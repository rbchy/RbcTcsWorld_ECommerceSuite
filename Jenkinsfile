// Jenkins pipeline with the same quality gates as .github/workflows/ci.yml.
// Jenkins runs on 8080, which is why the backend uses 8081.
//
// Agent needs: JDK 21, Maven, Docker with compose v2, python3, jq, Google Chrome (UI tests), curl.
// k6 is used when installed (brew install k6); otherwise the grafana/k6 image is used.
// Plugins: Pipeline, Git, JUnit, Timestamper (all in Jenkins' "suggested plugins") + HTML Publisher
// (optional: the "QA Reports" page with every report of the build; without it the reports are only archived).
// The syntax of this file is validated in GitHub Actions by a real Jenkins (job "Jenkinsfile lint").

pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()                       // one stack per agent: fixed ports 5432/8081/5173
    }

    parameters {
        booleanParam(name: 'RUN_UI', defaultValue: true, description: 'Selenium UI tests')
        choice(name: 'BROWSER', choices: ['chrome', 'firefox', 'edge', 'safari'], description: 'Browser for the UI and accessibility tests (must be installed on the agent; Safari: run "sudo safaridriver --enable" once)')
        booleanParam(name: 'RUN_MUTATION', defaultValue: true, description: 'PIT mutation testing of the business logic (about 4 minutes)')
        booleanParam(name: 'RUN_SECURITY', defaultValue: true, description: 'OSV dependency scan + API contract gate + OWASP ZAP API scan')
        choice(name: 'K6_EXTRA', choices: ['none', 'load', 'stress', 'spike', 'soak'], description: 'Extra (long) k6 test after the gates')
    }

    environment {
        // macOS agents: Homebrew (mvn, k6, jq) and Docker Desktop's CLI, which lives in ~/.docker/bin when
        // Docker Desktop was installed "per user" - Jenkins does not read the login shell's PATH.
        PATH = "/opt/homebrew/bin:/usr/local/bin:${env.HOME}/.docker/bin:/Applications/Docker.app/Contents/Resources/bin:${env.PATH}"
        BASE_URL = 'http://localhost:8081'
        UI_URL = 'http://localhost:5173'
        COMPOSE = 'docker compose --profile app'
        NETWORK = 'rbctcsworld_default'                       // compose network: containers reach "backend:8081"
    }

    stages {
        stage('Tools on the agent') {
            steps {
                // fail in seconds with a clear message instead of in the middle of the pipeline
                sh '''
                    for t in java mvn docker python3 jq curl; do
                      command -v "$t" >/dev/null || { echo "MISSING TOOL: $t (PATH=$PATH)"; exit 1; }
                    done
                    java -version 2>&1 | head -1
                    docker version --format 'Docker {{.Server.Version}}' || { echo "Docker Desktop is not running"; exit 1; }
                    docker compose version
                '''
            }
        }

        stage('Traceability matrix') {
            steps { sh 'python3 docs/qa/check_rtm.py' }
        }

        stage('Backend: unit + integration + coverage gate') {
            steps { sh 'mvn -B -f backend/pom.xml clean verify' }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml'
                    sh '.github/scripts/coverage-summary.sh backend/target/site/jacoco/jacoco.csv || true'
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'backend/target/site/jacoco/**'
                }
            }
        }

        stage('Mutation testing (PIT)') {
            when { expression { params.RUN_MUTATION } }
            steps {
                sh 'mvn -B -f backend/pom.xml -Pmutation -Djacoco.skip=true test-compile org.pitest:pitest-maven:mutationCoverage'
            }
            post {
                always {
                    sh '.github/scripts/mutation-summary.sh backend/target/pit-reports/mutations.xml 0 || true'
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'backend/target/pit-reports/**'
                }
            }
        }

        stage('Start the whole app in Docker') {
            steps {
                sh "${COMPOSE} down -v --remove-orphans || true"
                sh "${COMPOSE} up -d --build --wait --wait-timeout 300"
                sh "${COMPOSE} ps"
            }
        }

        stage('Automation: API + DB + BDD + UI') {
            steps {
                script {
                    def filter = params.RUN_UI ? '' : '-DexcludedGroups=ui'
                    sh "mvn -B -f automation/pom.xml clean test ${filter} -DbaseUrl=${BASE_URL} -DuiUrl=${UI_URL} -Dheadless=true -Dbrowser=${params.BROWSER}"
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'automation/target/surefire-reports/*.xml'
                    sh 'mvn -B -q -f automation/pom.xml allure:report || true'
                    archiveArtifacts allowEmptyArchive: true,
                            artifacts: 'automation/target/allure-report/**, automation/target/cucumber-report.html'
                }
            }
        }

        stage('Performance gates (k6)') {
            steps {
                sh '''
                    performance/k6run.sh -e DURATION=30s performance/tests/smoke.js
                    performance/k6run.sh -e BUYERS=60 -e STOCK=15 performance/tests/flash-sale.js
                    performance/k6run.sh -e DURATION=20s performance/tests/catalog.js
                '''
            }
            post {
                always { archiveArtifacts allowEmptyArchive: true, artifacts: 'performance/reports/**' }
            }
        }

        stage('Security: dependencies + API contract + OWASP ZAP') {
            when { expression { params.RUN_SECURITY } }
            steps {
                sh '''
                    # SBOM (Maven) + OSV-Scanner with completeness check and self-test (DEF-021);
                    # exceptions: backend/osv-scanner.toml
                    security/osv-scan.sh
                    security/osv-summary.sh osv-results.json

                    # API contract (provider side): no breaking change against the approved OpenAPI baseline
                    curl -fsS "$BASE_URL/v3/api-docs" -o security/zap/openapi.json
                    .github/scripts/openapi-breaking-changes.sh docs/api/openapi.json security/zap/openapi.json

                    # OWASP ZAP API scan from the OpenAPI spec, logged in as a customer, inside the compose network
                    TOKEN=$(curl -fsS -X POST "$BASE_URL/api/auth/register" -H 'Content-Type: application/json' \
                        -d "{\\"email\\":\\"zap-$BUILD_NUMBER@security.test\\",\\"password\\":\\"Password1!\\"}" | jq -r .token)
                    chmod -R 777 security/zap
                    set +e
                    docker run --rm --network "$NETWORK" -v "$PWD/security/zap:/zap/wrk:rw" \
                        -e ZAP_AUTH_HEADER=Authorization -e ZAP_AUTH_HEADER_VALUE="Bearer $TOKEN" \
                        ghcr.io/zaproxy/zaproxy:stable zap-api-scan.py \
                        -t http://backend:8081/v3/api-docs -f openapi -O http://backend:8081 \
                        -c zap-rules.tsv -I -r zap-report.html -J zap-report.json
                    code=$?
                    set -e
                    security/zap/summary.sh security/zap/zap-report.json
                    if [ "$code" -eq 1 ] || [ "$code" -eq 3 ]; then echo "OWASP ZAP failed (exit $code)"; exit 1; fi
                '''
            }
            post {
                always {
                    archiveArtifacts allowEmptyArchive: true,
                            artifacts: 'osv-results.json, security/zap/zap-report.html, security/zap/zap-report.json'
                }
            }
        }

        stage('Extra k6 test') {
            when { expression { params.K6_EXTRA != 'none' } }
            steps {
                sh "performance/k6run.sh performance/tests/${params.K6_EXTRA}.js"
            }
            post {
                always { archiveArtifacts allowEmptyArchive: true, artifacts: 'performance/reports/**' }
            }
        }
    }

    post {
        failure {
            sh "${COMPOSE} logs --no-color --tail=100 || true"
        }
        always {
            // ONE page with every report of this build: numbers of all gates + links to Allure, Cucumber, JaCoCo,
            // PIT, k6 and ZAP. Shown as "QA Reports" on the build page (HTML Publisher plugin).
            sh 'python3 ci/qa_dashboard.py qa-reports || true'
            archiveArtifacts allowEmptyArchive: true, artifacts: 'qa-reports/**'
            script {
                try {
                    publishHTML(target: [reportDir: 'qa-reports', reportFiles: 'index.html', reportName: 'QA Reports',
                                         keepAll: true, alwaysLinkToLastBuild: true, allowMissing: true])
                } catch (NoSuchMethodError ignored) {
                    echo 'Install the "HTML Publisher" plugin to get the QA Reports page (reports are archived anyway).'
                }
            }
            sh "${COMPOSE} down -v --remove-orphans || true"
        }
        success {
            echo 'All quality gates passed: tests, coverage, traceability, performance and security.'
        }
    }
}

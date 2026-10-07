// Jenkins pipeline with the same quality gates as .github/workflows/ci.yml.
// Jenkins runs on 8080, which is why the backend uses 8081.
//
// Agent needs: JDK 21, Maven, Docker with compose v2, python3, jq, Google Chrome (UI tests), curl.
// k6 is used when installed (brew install k6); otherwise the grafana/k6 image is used.
// Plugins: Pipeline, Git, JUnit, Timestamper (all in Jenkins' "suggested plugins").
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
        booleanParam(name: 'RUN_UI', defaultValue: true, description: 'Selenium UI tests (needs Chrome on the agent)')
        booleanParam(name: 'RUN_SECURITY', defaultValue: true, description: 'OSV dependency scan + OWASP ZAP API scan')
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
                    sh "mvn -B -f automation/pom.xml clean test ${filter} -DbaseUrl=${BASE_URL} -DuiUrl=${UI_URL} -Dheadless=true"
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

        stage('Security: dependencies + OWASP ZAP') {
            when { expression { params.RUN_SECURITY } }
            steps {
                sh '''
                    # OSV-Scanner: known CVEs in pom.xml / package-lock.json (exceptions: backend/osv-scanner.toml)
                    docker run --rm -v "$PWD:/src" ghcr.io/google/osv-scanner:v2.3.0 \
                        scan source --recursive --format json --output /src/osv-results.json /src || true
                    security/osv-summary.sh osv-results.json

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
            sh "${COMPOSE} down -v --remove-orphans || true"
        }
        success {
            echo 'All quality gates passed: tests, coverage, traceability, performance and security.'
        }
    }
}

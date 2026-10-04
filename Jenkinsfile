// Backend runs on 8081 so it does not clash with Jenkins (8080).
pipeline {
  agent any
  stages {
    stage('Backend tests') {
      steps { sh 'mvn -B -f backend/pom.xml clean verify' }
      post { always { junit 'backend/target/surefire-reports/*.xml' } }
    }
    stage('Automation compile') {
      steps { sh 'mvn -B -f automation/pom.xml test-compile' }
    }
  }
}

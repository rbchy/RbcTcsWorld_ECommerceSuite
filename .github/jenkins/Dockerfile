# Minimal Jenkins used only to validate the Jenkinsfile syntax in CI (declarative linter).
FROM jenkins/jenkins:lts-jdk21
RUN jenkins-plugin-cli --plugins workflow-aggregator git junit timestamper
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"

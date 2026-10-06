#!/usr/bin/env bash
# Starts a throw-away Jenkins with the pipeline plugins and asks its declarative linter to validate ./Jenkinsfile.
set -euo pipefail
docker build -q -t jenkins-lint .github/jenkins >/dev/null
docker run -d --name jenkins-lint -p 18080:8080 jenkins-lint >/dev/null
trap 'docker rm -f jenkins-lint >/dev/null 2>&1 || true' EXIT
for i in $(seq 1 90); do
  curl -fsS http://localhost:18080/login >/dev/null 2>&1 && break
  sleep 2
done
# Jenkins needs a CSRF crumb (and the session cookie that belongs to it) for POST requests
jar=$(mktemp)
crumb=$(curl -fsS -c "$jar" "http://localhost:18080/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,\":\",//crumb)" || true)
result=$(curl -sS -b "$jar" ${crumb:+-H "$crumb"} -X POST -F "jenkinsfile=<Jenkinsfile" \
  http://localhost:18080/pipeline-model-converter/validate)
echo "$result"
if echo "$result" | grep -q "Jenkinsfile successfully validated"; then
  echo "::notice title=Jenkinsfile::successfully validated by Jenkins $(curl -fsSI http://localhost:18080/login | grep -i '^x-jenkins:' | tr -d '\r' | cut -d' ' -f2)"
else
  echo "::error title=Jenkinsfile::$(echo "$result" | tr '\n' ' ' | cut -c1-500)"
  exit 1
fi

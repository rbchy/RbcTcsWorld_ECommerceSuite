#!/usr/bin/env bash
# Prints the newest version published on Maven Central for each pinned library, on the same release line
# as the version we use, so an upgrade can be planned before an advisory turns into an incident.
check() {  # groupId artifactId prefix current
  local path="${1//./\/}/$2"
  local latest
  latest=$(curl -sf "https://repo.maven.apache.org/maven2/$path/maven-metadata.xml" \
    | grep -o "<version>$3[^<]*</version>" | sed 's/<[^>]*>//g' | grep -Ev -- '-(M|RC|alpha|beta)' | sort -V | tail -1)
  if [ -z "$latest" ]; then echo "::notice title=Versions::$2: could not read Maven Central"; return; fi
  if [ "$latest" = "$4" ]; then echo "::notice title=Versions::$2 $4 is the newest $3x"
  else echo "::warning title=Versions::$2 $4 -> newer $latest is available"; fi
}
ev() { mvn -q -f backend/pom.xml help:evaluate -Dexpression="$1" -DforceStdout 2>/dev/null || echo "?"; }
BOOT=$(grep -o 'starter-parent</artifactId><version>[^<]*' backend/pom.xml | sed 's/.*<version>//')
check org.springframework.boot spring-boot-starter-parent "${BOOT%.*}." "$BOOT"
SPRING=$(ev spring-framework.version); check org.springframework spring-webmvc "${SPRING%.*}." "$SPRING"
TOMCAT=$(ev tomcat.version);          check org.apache.tomcat.embed tomcat-embed-core "${TOMCAT%.*}." "$TOMCAT"
PG=$(ev postgresql.version);          check org.postgresql postgresql "${PG%.*}." "$PG"

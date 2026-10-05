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
mvn_prop() { grep -o "<$1>[^<]*</$1>" backend/pom.xml | sed 's/<[^>]*>//g'; }
TOMCAT=$(mvn -q -f backend/pom.xml help:evaluate -Dexpression=tomcat.version -DforceStdout 2>/dev/null || echo "?")
check org.springframework.boot spring-boot-starter-parent 3.5. "$(grep -o 'starter-parent</artifactId><version>[^<]*' backend/pom.xml | sed 's/.*<version>//')"
check org.apache.tomcat.embed tomcat-embed-core 10.1. "$TOMCAT"
check com.fasterxml.jackson.core jackson-databind 2.21. "$(mvn_prop jackson-bom.version)"
check org.postgresql postgresql 42.7. "$(mvn_prop postgresql.version)"
check org.apache.logging.log4j log4j-api 2.25. "$(mvn_prop log4j2.version)"
check org.apache.commons commons-lang3 3. "$(mvn_prop commons-lang3.version)"

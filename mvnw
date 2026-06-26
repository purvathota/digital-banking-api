#!/bin/sh
# Maven Wrapper script

set -e

MAVEN_PROJECTBASEDIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

if [ -f "$WRAPPER_PROPERTIES" ]; then
  MAVEN_DIST_URL=$(grep "distributionUrl" "$WRAPPER_PROPERTIES" | cut -d'=' -f2- | tr -d '\r')
fi

MAVEN_DIST_URL=${MAVEN_DIST_URL:-"https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip"}

# Extract Maven version from URL
MAVEN_DIST_NAME=$(basename "$MAVEN_DIST_URL" .zip)
MAVEN_HOME="$HOME/.m2/wrapper/dists/$MAVEN_DIST_NAME"

if [ ! -d "$MAVEN_HOME" ]; then
  echo "Downloading Maven distribution..."
  mkdir -p "$HOME/.m2/wrapper/dists"
  
  if command -v curl > /dev/null 2>&1; then
    curl -fsSL "$MAVEN_DIST_URL" -o "$HOME/.m2/wrapper/dists/maven.zip"
  elif command -v wget > /dev/null 2>&1; then
    wget -q "$MAVEN_DIST_URL" -O "$HOME/.m2/wrapper/dists/maven.zip"
  else
    echo "Error: curl or wget is required to download Maven" >&2
    exit 1
  fi
  
  cd "$HOME/.m2/wrapper/dists"
  unzip -q -o maven.zip
  rm -f maven.zip
  cd "$MAVEN_PROJECTBASEDIR"
fi

# Find maven directory — may be nested inside the extracted archive
MVN_CMD=""
for dir in "$MAVEN_HOME" "$HOME/.m2/wrapper/dists"/apache-maven-*/; do
  if [ -f "$dir/bin/mvn" ]; then
    MVN_CMD="$dir/bin/mvn"
    break
  fi
done

if [ -z "$MVN_CMD" ]; then
  # Try direct find
  MVN_CMD=$(find "$HOME/.m2/wrapper/dists" -name "mvn" -path "*/bin/mvn" -type f 2>/dev/null | head -1)
fi

if [ -z "$MVN_CMD" ]; then
  echo "Error: Could not find Maven executable" >&2
  exit 1
fi

exec "$MVN_CMD" "$@"

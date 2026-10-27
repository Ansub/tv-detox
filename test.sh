#!/bin/sh
# Runs the logic tests on this computer (no TV needed). Needs Java 17.
#   sh test.sh
# Only Android-free classes are tested here; the rest is checked on a real TV.
set -e
cd "$(dirname "$0")"

[ -n "$JAVA_HOME" ] || for j in /opt/homebrew/opt/openjdk@17 /usr/local/opt/openjdk@17 /usr/lib/jvm/java-17-openjdk-amd64; do [ -d "$j" ] && JAVA_HOME=$j && break; done
[ -n "$JAVA_HOME" ] && export PATH="$JAVA_HOME/bin:$PATH"

# org.json is built into Android; tests use the same API from Maven Central.
JSON=test-lib/json-20240303.jar
if [ ! -f "$JSON" ]; then
  mkdir -p test-lib
  curl -fsSL -o "$JSON" https://repo1.maven.org/maven2/org/json/json/20240303/json-20240303.jar
fi

PURE=""
for c in Ease PhotoOrder LockState Greeting Scores BootRedirect KeyGate; do
  f=src/com/ansub/tvhome/$c.java
  [ -f "$f" ] && PURE="$PURE $f"
done

rm -rf out/test; mkdir -p out/test
# shellcheck disable=SC2046,SC2086 # one argument per file, on purpose
javac -nowarn --release 8 -encoding UTF-8 -cp "$JSON" -d out/test $PURE $(find test -name '*.java')
TESTS=$(find test -name '*Test.java' | sed 's|^test/||; s|\.java$||; s|/|.|g' | sort)
# shellcheck disable=SC2086 # the test class names are separate arguments
java -cp "out/test:$JSON" com.ansub.tvhome.T $TESTS

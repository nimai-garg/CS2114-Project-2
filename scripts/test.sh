#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
. ./scripts/java-env.sh
sh scripts/build.sh
mkdir -p .deps build/test-classes
# Dependencies are pinned to JUnit 4.13.2 and Hamcrest Core 1.3.
# Set JUNIT_JAR and HAMCREST_JAR to existing jars for an offline first run.
dispatch_junit=${JUNIT_JAR:-.deps/junit-4.13.2.jar}
dispatch_hamcrest=${HAMCREST_JAR:-.deps/hamcrest-core-1.3.jar}
fetch_jar() {
    if [ ! -f "$1" ]; then
        if ! command -v curl >/dev/null 2>&1; then
            echo "curl is required for the first download; alternatively set JUNIT_JAR and HAMCREST_JAR." >&2
            exit 1
        fi
        curl --fail --location --silent --show-error --connect-timeout 15 --max-time 90 "$2" -o "$1.tmp"
        mv "$1.tmp" "$1"
    fi
}
fetch_jar "$dispatch_junit" https://repo.maven.apache.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar
fetch_jar "$dispatch_hamcrest" https://repo.maven.apache.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar
dispatch_test_cp="build/classes:$dispatch_junit:$dispatch_hamcrest"
"$dispatch_java_bin/javac" --release 17 -encoding UTF-8 -Xlint:all -Werror -cp "$dispatch_test_cp" -d build/test-classes test/emergencyDispatch/*.java
# Discover every test file rather than maintaining a list that can miss new tests.
set --
for dispatch_test in test/emergencyDispatch/*Test.java; do
    dispatch_test_name=$(basename "$dispatch_test" .java)
    set -- "$@" "emergencyDispatch.$dispatch_test_name"
done
exec "$dispatch_java_bin/java" -cp "build/test-classes:$dispatch_test_cp" org.junit.runner.JUnitCore "$@"

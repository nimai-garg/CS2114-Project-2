#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
. ./scripts/java-env.sh
mkdir -p build/classes
"$dispatch_java_bin/javac" --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/classes src/emergencyDispatch/*.java
echo "Build passed."

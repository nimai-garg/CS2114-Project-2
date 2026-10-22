#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
. ./scripts/java-env.sh
sh scripts/build.sh
exec "$dispatch_java_bin/java" -cp build/classes emergencyDispatch.Main "$@"

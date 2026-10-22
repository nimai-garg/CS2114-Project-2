#!/bin/sh
# Shared by the launch scripts. JAVA_HOME takes precedence when supplied.
if [ -n "${JAVA_HOME:-}" ]; then
    dispatch_java_bin="$JAVA_HOME/bin"
    if ! "$dispatch_java_bin/javac" -version >/dev/null 2>&1; then
        echo "JAVA_HOME must point to a JDK (Java 17 or newer)." >&2
        exit 1
    fi
elif javac -version >/dev/null 2>&1; then
    dispatch_java_bin=$(dirname "$(command -v javac)")
else
    dispatch_java_bin=""
    for dispatch_candidate in /opt/homebrew/opt/openjdk@21/bin /opt/homebrew/opt/openjdk/bin /usr/local/opt/openjdk/bin; do
        if [ -x "$dispatch_candidate/javac" ]; then
            dispatch_java_bin="$dispatch_candidate"
            break
        fi
    done
    if [ -z "$dispatch_java_bin" ]; then
        echo "Install a JDK (Java 17 or newer), then set JAVA_HOME or add its bin folder to PATH." >&2
        exit 1
    fi
fi

#!/usr/bin/env bash
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_BIN="${JAVA_HOME:-}/bin/java"
if [ ! -x "$JAVA_BIN" ]; then JAVA_BIN="java"; fi
exec "$JAVA_BIN" -cp "$DIR/app-core.jar:$DIR/lib/*" com.dbsyncstudio.core.DbSyncStudioApplication "$@"
#!/usr/bin/env bash
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -x "$DIR/runtime/bin/java" ]; then
  JAVA_BIN="$DIR/runtime/bin/java"
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA_BIN="$JAVA_HOME/bin/java"
else
  JAVA_BIN="java"
fi
exec "$JAVA_BIN" -cp "$DIR/app-core.jar:$DIR/lib/*" com.dbsyncstudio.core.DbSyncStudioApplication "$@"
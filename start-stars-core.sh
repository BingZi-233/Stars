#!/bin/sh

set -eu

WORK_DIR="${WORK_DIR:-$(pwd)}"
APP_NAME="stars-core"
ENV_FILE="${ENV_FILE:-$WORK_DIR/.env}"
LOG_DIR="${LOG_DIR:-$WORK_DIR/logs}"
LOG_FILE="${LOG_FILE:-$LOG_DIR/latest.log}"
PID_FILE="${PID_FILE:-$WORK_DIR/${APP_NAME}.pid}"
JAVA_BIN="${JAVA_BIN:-java}"
JAVA_OPTS="${JAVA_OPTS:-}"

if [ -n "${JAR_PATH:-}" ]; then
    RESOLVED_JAR_PATH="$JAR_PATH"
else
    RESOLVED_JAR_PATH=$(
        find "$WORK_DIR" -maxdepth 1 -type f -name 'stars-core-*.jar' ! -name '*-plain.jar' | sort | tail -n 1
    )
fi

if [ ! -f "$ENV_FILE" ]; then
    echo "missing env file: $ENV_FILE" >&2
    exit 1
fi

if [ -z "${RESOLVED_JAR_PATH:-}" ] || [ ! -f "$RESOLVED_JAR_PATH" ]; then
    echo "missing jar file in current directory: $WORK_DIR/stars-core-*.jar" >&2
    exit 1
fi

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE" 2>/dev/null || true)
    if [ -n "${PID:-}" ] && kill -0 "$PID" 2>/dev/null; then
        echo "$APP_NAME is already running with pid $PID"
        exit 0
    fi
    rm -f "$PID_FILE"
fi

mkdir -p "$LOG_DIR"

set -a
. "$ENV_FILE"
set +a

nohup "$JAVA_BIN" -Dlogging.file.name="$LOG_FILE" $JAVA_OPTS -jar "$RESOLVED_JAR_PATH" >/dev/null 2>&1 &
PID=$!
echo "$PID" >"$PID_FILE"

echo "$APP_NAME started"
echo "pid: $PID"
echo "jar: $RESOLVED_JAR_PATH"
echo "log: $LOG_FILE"

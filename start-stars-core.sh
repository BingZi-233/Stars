#!/bin/sh

set -eu

COMMAND="${1:-start}"
WORK_DIR="${WORK_DIR:-$(pwd)}"
APP_NAME="stars-core"
ENV_FILE="${ENV_FILE:-$WORK_DIR/.env}"
LOG_DIR="${LOG_DIR:-$WORK_DIR/logs}"
LOG_FILE="${LOG_FILE:-$LOG_DIR/latest.log}"
PID_FILE="${PID_FILE:-$WORK_DIR/${APP_NAME}.pid}"
JAVA_BIN="${JAVA_BIN:-java}"
JAVA_OPTS="${JAVA_OPTS:-}"

read_pid() {
    if [ ! -f "$PID_FILE" ]; then
        return 1
    fi

    PID=$(cat "$PID_FILE" 2>/dev/null || true)
    if [ -z "${PID:-}" ]; then
        return 1
    fi

    return 0
}

is_running() {
    read_pid || return 1
    kill -0 "$PID" 2>/dev/null
}

cleanup_stale_pid() {
    if [ -f "$PID_FILE" ] && ! is_running; then
        rm -f "$PID_FILE"
    fi
}

resolve_jar_path() {
    if [ -n "${JAR_PATH:-}" ]; then
        RESOLVED_JAR_PATH="$JAR_PATH"
    else
        RESOLVED_JAR_PATH=$(
            find "$WORK_DIR" -maxdepth 1 -type f -name 'stars-core-*.jar' ! -name '*-plain.jar' | sort | tail -n 1
        )
    fi

    if [ -z "${RESOLVED_JAR_PATH:-}" ] || [ ! -f "$RESOLVED_JAR_PATH" ]; then
        echo "missing jar file in current directory: $WORK_DIR/stars-core-*.jar" >&2
        exit 1
    fi
}

start_app() {
    cleanup_stale_pid
    if is_running; then
        echo "$APP_NAME is already running with pid $PID"
        return 0
    fi

    if [ ! -f "$ENV_FILE" ]; then
        echo "missing env file: $ENV_FILE" >&2
        exit 1
    fi

    resolve_jar_path
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
}

stop_app() {
    cleanup_stale_pid
    if ! is_running; then
        echo "$APP_NAME is not running"
        return 0
    fi

    kill "$PID"

    i=0
    while kill -0 "$PID" 2>/dev/null; do
        i=$((i + 1))
        if [ "$i" -ge 30 ]; then
            echo "failed to stop $APP_NAME within 30 seconds" >&2
            return 1
        fi
        sleep 1
    done

    rm -f "$PID_FILE"
    echo "$APP_NAME stopped"
    return 0
}

status_app() {
    cleanup_stale_pid
    if is_running; then
        echo "$APP_NAME is running with pid $PID"
        return 0
    fi

    echo "$APP_NAME is not running"
    return 1
}

case "$COMMAND" in
    start)
        start_app
        ;;
    stop)
        stop_app
        ;;
    restart)
        stop_app
        start_app
        ;;
    status)
        status_app
        ;;
    *)
        echo "usage: $0 {start|stop|restart|status}" >&2
        exit 1
        ;;
esac

#!/usr/bin/env bash
# Switch the backend serving the frontend on http://localhost:8000.
#
# Usage:
#   ./switch-backend.sh spring   # Spring Boot (Java 21, requires JDK 21 on host)
#   ./switch-backend.sh python   # FastAPI (uses ./backend/.venv)
#   ./switch-backend.sh status   # Show what's currently bound to :8000
#   ./switch-backend.sh stop     # Stop the active native backend
#
# Frontend stays untouched. If the frontend is running in Docker,
# set BACKEND_URL=http://host.docker.internal:8000 on the frontend
# service so it can reach the host-native backend.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PORT=8000
PID_FILE="$ROOT/.backend.pid"
MODE_FILE="$ROOT/.backend.mode"
LOG_FILE="$ROOT/backend.log"
ENV_FILE="$ROOT/.env"

log()  { printf '\033[1;34m[switch-backend]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[switch-backend]\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31m[switch-backend]\033[0m %s\n' "$*" >&2; exit 1; }

usage() {
  sed -n '2,14p' "$0" | sed 's/^# \{0,1\}//'
  exit "${1:-1}"
}

load_env() {
  [ -f "$ENV_FILE" ] || return 0
  set -a
  # shellcheck disable=SC1090
  . "$ENV_FILE"
  set +a
}

port_listener_pid() {
  lsof -nP -iTCP:"$PORT" -sTCP:LISTEN -t 2>/dev/null | head -n1 || true
}

stop_docker_backend() {
  if command -v docker >/dev/null 2>&1 \
     && docker compose -f "$ROOT/docker-compose.yml" ps --services --filter status=running 2>/dev/null \
        | grep -qx backend; then
    log "Stopping docker compose 'backend' service (frees :$PORT)..."
    (cd "$ROOT" && docker compose stop backend >/dev/null)
  fi
}

stop_tracked_backend() {
  if [ -f "$PID_FILE" ]; then
    local pid; pid="$(cat "$PID_FILE" 2>/dev/null || true)"
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
      log "Stopping previous backend (pid $pid)..."
      kill "$pid" 2>/dev/null || true
      for _ in 1 2 3 4 5 6 7 8 9 10; do
        kill -0 "$pid" 2>/dev/null || break
        sleep 0.5
      done
      kill -9 "$pid" 2>/dev/null || true
    fi
    rm -f "$PID_FILE" "$MODE_FILE"
  fi
}

ensure_port_free() {
  local pid; pid="$(port_listener_pid)"
  [ -z "$pid" ] && return 0
  die "Port $PORT is held by pid $pid that this script didn't start. Free it and retry."
}

wait_until_ready() {
  local mode="$1" pid="$2"
  for _ in $(seq 1 60); do
    kill -0 "$pid" 2>/dev/null || die "Backend ($mode) exited during startup — see $LOG_FILE"
    if curl -fsS "http://localhost:$PORT/api/airport-code?city=London" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  die "Backend ($mode) did not become ready on :$PORT within 60s — see $LOG_FILE"
}

start_python() {
  [ -d "$ROOT/backend" ] || die "backend/ directory not found"
  if [ ! -d "$ROOT/backend/.venv" ]; then
    log "Creating Python venv..."
    (cd "$ROOT/backend" && python3 -m venv .venv && .venv/bin/pip install -q -r requirements.txt)
  fi
  log "Starting Python backend (FastAPI on :$PORT)..."
  : > "$LOG_FILE"
  (
    cd "$ROOT/backend"
    nohup .venv/bin/uvicorn src.api:app --host 0.0.0.0 --port "$PORT" \
      >>"$LOG_FILE" 2>&1 &
    echo $! > "$PID_FILE"
  )
  echo python > "$MODE_FILE"
  wait_until_ready python "$(cat "$PID_FILE")"
}

resolve_jdk21() {
  if [ -n "${JAVA_HOME_21:-}" ] && [ -x "$JAVA_HOME_21/bin/java" ]; then
    echo "$JAVA_HOME_21"; return
  fi
  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    /usr/libexec/java_home -v 21 2>/dev/null && return
  fi
  if [ -n "${JAVA_HOME:-}" ] && "$JAVA_HOME/bin/java" -version 2>&1 | grep -q 'version "21'; then
    echo "$JAVA_HOME"; return
  fi
  return 1
}

start_spring() {
  local app_dir="$ROOT/springboot-backend-api"
  [ -d "$app_dir" ] || die "springboot-backend-api/ not found"

  local jdk21
  jdk21="$(resolve_jdk21 || true)"
  [ -n "$jdk21" ] || die "JDK 21 not found. Install it (e.g. brew install openjdk@21) or set JAVA_HOME_21."

  local jar
  jar="$(ls -1t "$app_dir"/target/flight-agent-backend-*.jar 2>/dev/null | head -n1 || true)"
  if [ -z "$jar" ]; then
    command -v mvn >/dev/null 2>&1 || die "Maven not on PATH and no prebuilt jar in target/"
    log "Building Spring Boot jar (mvn -DskipTests package)..."
    (cd "$app_dir" && JAVA_HOME="$jdk21" PATH="$jdk21/bin:$PATH" \
       mvn -B -ntp -q -DskipTests package)
    jar="$(ls -1t "$app_dir"/target/flight-agent-backend-*.jar | head -n1)"
  fi

  log "Starting Spring Boot backend ($(basename "$jar")) on :$PORT..."
  : > "$LOG_FILE"
  (
    cd "$app_dir"
    JAVA_HOME="$jdk21" PATH="$jdk21/bin:$PATH" \
      nohup "$jdk21/bin/java" -jar "$jar" >>"$LOG_FILE" 2>&1 &
    echo $! > "$PID_FILE"
  )
  echo spring > "$MODE_FILE"
  wait_until_ready spring "$(cat "$PID_FILE")"
}

current_mode() {
  [ -f "$MODE_FILE" ] && cat "$MODE_FILE" || echo unknown
}

cmd_status() {
  local listener; listener="$(port_listener_pid)"
  printf 'Tracked mode : %s\n' "$(current_mode)"
  if [ -f "$PID_FILE" ]; then
    local pid; pid="$(cat "$PID_FILE")"
    if kill -0 "$pid" 2>/dev/null; then
      printf 'Tracked pid  : %s (running)\n' "$pid"
    else
      printf 'Tracked pid  : %s (not running — stale pid file)\n' "$pid"
    fi
  else
    printf 'Tracked pid  : none\n'
  fi
  printf 'Port %s     : %s\n' "$PORT" "${listener:-free}"
  if [ -n "$listener" ]; then
    ps -o pid=,command= -p "$listener" 2>/dev/null | sed 's/^/             /'
  fi
}

cmd_stop() {
  stop_tracked_backend
  log "Done. (docker compose backend, if any, is not touched — use 'docker compose stop backend' for that)"
}

cmd_switch() {
  local target="$1"
  if [ "$(current_mode)" = "$target" ] && [ -f "$PID_FILE" ] \
     && kill -0 "$(cat "$PID_FILE")" 2>/dev/null \
     && [ -n "$(port_listener_pid)" ]; then
    log "Already running '$target' on :$PORT (pid $(cat "$PID_FILE")). Nothing to do."
    return 0
  fi

  load_env
  stop_tracked_backend
  stop_docker_backend
  ensure_port_free

  case "$target" in
    spring) start_spring ;;
    python) start_python ;;
  esac

  log "✅ '$target' backend is live on http://localhost:$PORT  (logs: $LOG_FILE)"
  log "Frontend on host? http://localhost:3000 already proxies /api → :$PORT."
  log "Frontend in docker? Set BACKEND_URL=http://host.docker.internal:$PORT and recreate it."
}

main() {
  [ $# -ge 1 ] || usage 1
  case "$1" in
    spring|python) cmd_switch "$1" ;;
    status)        cmd_status ;;
    stop)          cmd_stop ;;
    -h|--help|help) usage 0 ;;
    *) usage 1 ;;
  esac
}

main "$@"

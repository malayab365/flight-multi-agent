#!/usr/bin/env bash
# Switch the docker compose 'backend' service between Spring Boot and Python (FastAPI).
# Frontend + postgres in the same stack are left running across switches.
#
# Usage:
#   ./switch-backend-docker.sh spring   # Spring Boot (uses docker-compose.yml)
#   ./switch-backend-docker.sh python   # FastAPI (overlays docker-compose.python.yml)
#   ./switch-backend-docker.sh status   # Show docker stack state + which flavor is up
#   ./switch-backend-docker.sh stop     # Stop the backend service only (keep stack)
#   ./switch-backend-docker.sh down     # Tear down the full compose stack
#
# Requires: docker compose v2, docker-compose.yml, docker-compose.python.yml.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PORT=8000
MODE_FILE="$ROOT/.backend-docker.mode"
COMPOSE_FILE="$ROOT/docker-compose.yml"
PYTHON_OVERRIDE="$ROOT/docker-compose.python.yml"

log()  { printf '\033[1;34m[switch-backend-docker]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[switch-backend-docker]\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31m[switch-backend-docker]\033[0m %s\n' "$*" >&2; exit 1; }

usage() {
  sed -n '2,13p' "$0" | sed 's/^# \{0,1\}//'
  exit "${1:-1}"
}

require_docker() {
  command -v docker >/dev/null 2>&1 || die "docker not found on PATH"
  docker info >/dev/null 2>&1 || die "docker daemon not reachable — is Docker Desktop running?"
}

compose_spring() {
  (cd "$ROOT" && docker compose -f "$COMPOSE_FILE" "$@")
}

compose_python() {
  [ -f "$PYTHON_OVERRIDE" ] || die "missing $PYTHON_OVERRIDE"
  (cd "$ROOT" && docker compose -f "$COMPOSE_FILE" -f "$PYTHON_OVERRIDE" "$@")
}

# Which compose form should we use for management calls? Pick the one
# matching the tracked mode so 'down/stop/ps' see the right project view.
compose_for_current() {
  case "$(current_mode)" in
    python) compose_python "$@" ;;
    *)      compose_spring "$@" ;;
  esac
}

current_mode() {
  [ -f "$MODE_FILE" ] && cat "$MODE_FILE" || echo unknown
}

backend_running() {
  compose_for_current ps --services --filter status=running 2>/dev/null | grep -qx backend
}

wait_until_ready() {
  local mode="$1"
  for _ in $(seq 1 90); do
    if curl -fsS "http://localhost:$PORT/api/airport-code?city=London" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  die "Backend ($mode) did not respond on :$PORT within 90s — try 'docker compose logs backend'"
}

cmd_switch() {
  require_docker
  local target="$1"

  if [ "$(current_mode)" = "$target" ] && backend_running; then
    log "Already running docker '$target' on :$PORT. Nothing to do."
    return 0
  fi

  # Stop the previously running backend (in whichever compose view tracked it).
  if backend_running; then
    log "Stopping current docker backend ($(current_mode))..."
    compose_for_current stop backend >/dev/null
    # Remove the container so the next 'up' rebuilds cleanly under the new compose view.
    compose_for_current rm -f backend >/dev/null 2>&1 || true
  fi

  case "$target" in
    spring)
      log "Bringing up docker backend (Spring Boot)..."
      compose_spring up -d --build backend
      ;;
    python)
      log "Bringing up docker backend (Python FastAPI)..."
      compose_python up -d --build backend
      ;;
  esac

  echo "$target" > "$MODE_FILE"
  wait_until_ready "$target"

  log "✅ docker '$target' backend live on http://localhost:$PORT"
  log "Frontend (docker) reaches it via the compose 'backend' service — no extra config."
  log "Logs: docker compose logs -f backend"
}

cmd_status() {
  require_docker
  printf 'Tracked mode : %s\n' "$(current_mode)"
  printf '\n-- docker compose ps (current view) --\n'
  compose_for_current ps 2>/dev/null || true
  printf '\n-- :%s ready? --\n' "$PORT"
  if curl -fsS -o /dev/null -w 'HTTP %{http_code}\n' "http://localhost:$PORT/api/airport-code?city=London" 2>/dev/null; then
    :
  else
    printf 'not responding\n'
  fi
}

cmd_stop() {
  require_docker
  if backend_running; then
    log "Stopping docker backend ($(current_mode))..."
    compose_for_current stop backend >/dev/null
  else
    log "No docker backend running."
  fi
}

cmd_down() {
  require_docker
  log "Tearing down the full compose stack..."
  compose_for_current down
  rm -f "$MODE_FILE"
}

main() {
  [ $# -ge 1 ] || usage 1
  case "$1" in
    spring|python) cmd_switch "$1" ;;
    status)        cmd_status ;;
    stop)          cmd_stop ;;
    down)          cmd_down ;;
    -h|--help|help) usage 0 ;;
    *) usage 1 ;;
  esac
}

main "$@"

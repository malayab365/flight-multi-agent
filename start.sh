#!/usr/bin/env bash
set -e

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_PORT=8000
FRONTEND_PORT=3000

cleanup() {
  echo ""
  echo "Shutting down..."
  kill "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null
  wait "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null
  exit 0
}
trap cleanup INT TERM

# --- Backend ---
echo "Starting backend (FastAPI on :$BACKEND_PORT)..."
cd "$ROOT/backend"
if [ ! -d ".venv" ]; then
  python3 -m venv .venv
  .venv/bin/pip install -q -r requirements.txt
fi
.venv/bin/uvicorn src.api:app --host 0.0.0.0 --port "$BACKEND_PORT" --reload \
  > "$ROOT/backend.log" 2>&1 &
BACKEND_PID=$!

# --- Frontend ---
echo "Starting frontend (Next.js on :$FRONTEND_PORT)..."
cd "$ROOT/frontend"
if [ ! -d "node_modules" ]; then
  npm install --silent
fi
npm run dev -- --port "$FRONTEND_PORT" > "$ROOT/frontend.log" 2>&1 &
FRONTEND_PID=$!

# Wait for frontend to be ready
echo "Waiting for servers to start..."
for i in $(seq 1 30); do
  if curl -s "http://localhost:$FRONTEND_PORT" > /dev/null 2>&1; then
    break
  fi
  sleep 1
done

echo ""
echo "========================================="
echo "  UI:      http://localhost:$FRONTEND_PORT"
echo "  API:     http://localhost:$BACKEND_PORT"
echo "  API docs: http://localhost:$BACKEND_PORT/docs"
echo "========================================="
echo "Logs: backend.log  |  frontend.log"
echo "Press Ctrl+C to stop."
echo ""

wait "$BACKEND_PID" "$FRONTEND_PID"

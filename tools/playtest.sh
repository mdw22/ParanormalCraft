#!/usr/bin/env bash
# One-click playtest: starts the NeoForge dev server, waits until it's ready, then starts the NeoForge client and
# joins it. When you close the game, the server is stopped (over RCON) and the results are in
# neoforge/runs/server/paranormalcraft-playtests/latest.md.
# Used by the VS Code task "Playtest"; run it from the repo root or anywhere.
set -u
cd "$(dirname "$0")/.."
LOG_DIR=build/playtest
mkdir -p "$LOG_DIR"
SERVER_LOG="$LOG_DIR/server.log"
PROPS=neoforge/runs/server/server.properties

stop_server() {
  # RCON "stop" with the dev server's password; falls back to killing the Gradle task.
  python3 - "$PROPS" <<'PY' || kill "$SERVER_PID" 2>/dev/null
import socket, struct, sys
props = dict(l.strip().split("=", 1) for l in open(sys.argv[1]) if "=" in l and not l.startswith("#"))
s = socket.create_connection(("127.0.0.1", int(props.get("rcon.port", "25575"))), timeout=5)
def send(i, kind, body):
    data = struct.pack("<ii", i, kind) + body.encode() + b"\x00\x00"
    s.sendall(struct.pack("<i", len(data)) + data)
    size = struct.unpack("<i", s.recv(4))[0]
    s.recv(size)
send(1, 3, props.get("rcon.password", ""))
send(2, 2, "stop")
PY
}

SERVER_PID=""
if lsof -nP -iTCP:25565 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "A server is already running on port 25565: joining it."
else
  echo "Starting the NeoForge server (log: $SERVER_LOG)..."
  ./gradlew :neoforge:runServer > "$SERVER_LOG" 2>&1 &
  SERVER_PID=$!
  until grep -qE "Done \(" "$SERVER_LOG" 2>/dev/null; do
    if ! kill -0 "$SERVER_PID" 2>/dev/null || grep -qE "BUILD FAILED|FAILURE:" "$SERVER_LOG"; then
      echo "The server didn't start. Last lines of $SERVER_LOG:"
      tail -n 40 "$SERVER_LOG"
      exit 1
    fi
    sleep 1
  done
  echo "Server ready."
fi

echo "Starting the NeoForge client; it joins the server by itself."
./gradlew :neoforge:runClient -PjoinServer=localhost
git checkout -- .vscode/launch.json 2>/dev/null || true

if [ -n "$SERVER_PID" ]; then
  echo "Game closed: stopping the server."
  stop_server
  wait "$SERVER_PID" 2>/dev/null
fi
echo "Playtest results: neoforge/runs/server/paranormalcraft-playtests/latest.md"

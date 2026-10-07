#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
OUT="$ROOT/app/build/core-test"
mkdir -p "$OUT"
javac -d "$OUT" app/src/main/java/com/chesscoach/app/Chess.java app/src/main/java/com/chesscoach/app/Stockfish.java app/src/main/java/com/chesscoach/app/Pgn.java app/src/main/java/com/chesscoach/app/Highlights.java app/src/main/java/com/chesscoach/app/Difficulty.java tests/CoreTest.java
java -Xmx768m -cp "$OUT" com.chesscoach.app.CoreTest "$@"
node --test companion/test/*.test.mjs

#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
python3 "$ROOT/scripts/stockfish-assets.py"
BUILD="$ROOT/app/build/stockfish/host"
if test -x "$BUILD/src/stockfish" && ! find "$ROOT/vendor/stockfish/src" -type f \( -name "*.cpp" -o -name "*.h" -o -name "*.nnue" -o -name Makefile \) -newer "$BUILD/src/stockfish" -print -quit | read -r _; then
    echo "Reusing host Stockfish";exit 0
fi
mkdir -p "$BUILD/src" "$BUILD/scripts"
cp -r "$ROOT/vendor/stockfish/src/." "$BUILD/src/"
cp -r "$ROOT/vendor/stockfish/scripts/." "$BUILD/scripts/"
make -C "$BUILD/src" -j"${BUILD_JOBS:-2}" build ARCH=x86-64 COMP=gcc

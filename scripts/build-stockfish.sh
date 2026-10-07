#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
SDK=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/workspace/toolchains/android-sdk}}
NDK="$SDK/ndk/27.2.12479018/toolchains/llvm/prebuilt/linux-x86_64/bin"
test -x "$NDK/aarch64-linux-android26-clang++"
cd "$ROOT/vendor/stockfish/src"
python3 "$ROOT/scripts/stockfish-assets.py"
# Portable ARM64 NEON and x86-64 SSE2 targets embed the verified NNUE.
# Android API 26 and 16KB segments support older phones and modern page sizes.
for abi in arm64-v8a x86_64; do
    dest="$ROOT/app/src/main/jniLibs/$abi/libstockfish.so"
    stamp="$(dirname "$dest")/build-v19-api26-16k-neon.stamp"
    if test -f "$dest" && test -f "$stamp" && ! find "$ROOT/vendor/stockfish/src" -type f \( -name '*.cpp' -o -name '*.h' -o -name '*.nnue' -o -name Makefile \) -newer "$dest" -print -quit | read -r _; then
        python3 "$ROOT/scripts/verify-embedded-net.py" "$dest"
        echo "Reusing $abi Stockfish";continue
    fi
    build="$ROOT/app/build/stockfish/$abi"
    mkdir -p "$build/src" "$build/scripts" "$(dirname "$dest")"
    cp -r "$ROOT/vendor/stockfish/src/." "$build/src/"
    cp -r "$ROOT/vendor/stockfish/scripts/." "$build/scripts/"
    cd "$build/src";make clean >/dev/null
    if test "$abi" = arm64-v8a; then arch=armv8;compiler=aarch64-linux-android26-clang++;else arch=x86-64;compiler=x86_64-linux-android26-clang++;fi
    PATH="$NDK:$PATH" make -j"${BUILD_JOBS:-2}" build ARCH="$arch" COMP=ndk COMPCXX="$NDK/$compiler" EXTRALDFLAGS='-Wl,-z,max-page-size=16384' UNIVERSAL_FINAL_FLAGS='-fno-exceptions -Os -static-libstdc++ -fPIE -pie -Wl,-z,max-page-size=16384 -lm -latomic'
    cp stockfish "$dest";"$NDK/llvm-strip" "$dest"
    python3 "$ROOT/scripts/verify-embedded-net.py" "$dest"
    touch "$stamp"
    cd "$ROOT/vendor/stockfish/src"
done

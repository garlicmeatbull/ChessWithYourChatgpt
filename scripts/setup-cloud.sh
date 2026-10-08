#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
python3 scripts/bootstrap-cloud.py
export JAVA_HOME=/workspace/toolchains/jdk-21.0.8+9
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_SDK_ROOT=/workspace/toolchains/android-sdk
export npm_config_cache=/workspace/toolchains/npm-cache
if ! test -f .agents/skills/caveman/SKILL.md; then
    npx skills@latest add JuliusBrussee/caveman -a codex --yes
fi
bash scripts/build-stockfish.sh
bash scripts/build-host-stockfish.sh
bash scripts/test-core.sh app/build/stockfish/host/src/stockfish
python3 scripts/gradle-cloud.py testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug

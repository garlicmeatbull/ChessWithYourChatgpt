# Chess Coach

체스를 두면서 Stockfish의 추천 수와 그 근거를 복기하는 **네이티브 Android 앱**입니다. Java Activity/View/Canvas를 사용하며 Android 8.1(API 27) 이상 ARM64 기기를 우선 지원합니다. iOS 구현은 포함하지 않습니다. 현재 버전은 화면과 탐색 흐름을 재설계한 0.2.0입니다.

## 사용할 수 있는 기능

- 홈 → 상대 난이도 → 대국, 홈 → 기록 → 기록 추가로 분리한 화면. 상단 AI 코치와 대국 중 모델 선택, 채움 벡터 기물, 판 위 색상·기호·추천 화살표를 지원합니다.
- 오프라인 Stockfish 19 대국, 시스템을 따르는 화이트/다크 모드.
- 상대 난이도: 입문 Skill 0, 목표 Elo 1320·1600·2000·2400·2800·3190, 제한 없는 최강. 분석 엔진은 별도 프로세스로 항상 Skill 20과 실력 제한 해제를 유지합니다. 목표 Elo는 다른 서비스의 사용자 레이팅과 직접 비교할 수 없습니다.
- 매 반수의 추천 수, 상위 후보 3개, 평가·메이트·탐색 깊이·PV를 계산하고 저장합니다. 실력 제한을 해제하더라도 탐색은 기기 성능과 시간의 영향을 받습니다.
- 본인 PC/서버의 **공식 Codex ChatGPT OAuth**를 통한 한국어 해설과 모델 선택. 폰에 ChatGPT OAuth 토큰을 넣지 않습니다.
- 직접 둔 대국과 가져온 대국을 동일한 **대국 기록** 화면에서 보관합니다. 수순, 엔진 분석, 실제 AI 해설·모델·사용량을 SQLite에 저장합니다.
- PGN/SAN 또는 UCI 좌표 수순을 붙여넣고 수마다 이동할 수 있습니다. 예: `1. e4 e5 2. Nf3 Nc6 *` 또는 `e2e4 e7e5 g1f3 b8c6`. 한 번에 한 대국의 메인 라인을 가져오며 주석·가지 변화는 제외합니다.
- 가져온 대국은 전체 분석 후 **학습 하이라이트**를 생성합니다. 직접 둔 대국도 복기 화면의 같은 버튼으로 생성할 수 있습니다. 하이라이트는 후보 비교·방어·좋은 선택처럼 발전에 도움이 될 국면을 골라 이유를 표시하고 해당 수로 이동합니다. 평가 차이와 PV에 기반한 휴리스틱이며 개인 맞춤 실력 진단은 아닙니다.
- 앱 설정과 대국 기록은 로컬에 보관하며 기기 백업에서 제외합니다. 앱을 삭제하면 기록도 삭제되므로 필요하면 PGN을 복사해 보관하세요.

수 평가는 초록 ★ 최선수, 연두 ✓ 좋은 수, 노랑 ?! 아쉬운 수, 주황 ? 실수, 빨강 ?? 블런더로 표시합니다. 깊이 12 이상에서 합법적인 PV 4반수 동안 기물 희생과 평가 보상이 확인되면 청록 !! **탁월 후보**로 표시합니다. 이는 공개한 휴리스틱으로, Chess.com의 독점 판정과 같지 않으며 확정적인 탁월수 판정도 아닙니다. 추천수 보기는 착수 전 국면에 초록 추천 화살표와 회색 실전 화살표를 표시합니다.

## Android 빌드

Linux x86_64, JDK 17 이상(검증한 버전 21), Android SDK API 35/build-tools 35.0.0/NDK 27.2.12479018, Python 3, GNU make, Node.js 20 이상이 필요합니다. Android Studio의 SDK Manager로 도구를 설치한 경우 다음을 실행하세요.

```sh
export ANDROID_SDK_ROOT=/path/to/Android/Sdk
bash scripts/build-stockfish.sh
./gradlew assembleDebug assembleDebugAndroidTest lintDebug
```

`build-stockfish.sh`는 SHA-256이 고정된 공식 Stockfish 릴리스에서 NNUE를 복원하고 ARM64 NEON 및 x86_64 SSE2 실행 파일을 소스로 빌드합니다. 전체 NNUE가 실행 파일에 포함되었는지도 검사합니다. 두 ABI 모두 API 26 및 16KB ELF 페이지 정렬로 빌드합니다. 큰 NNUE가 포함되므로 APK 크기와 최초 빌드 시간이 큽니다.

GitHub에 push하면 `Native Android` Actions가 APK와 대응 소스를 같은 다운로드 artifact로 만들도록 설정했습니다. 성공한 실행의 `ChessCoach-Android-and-source` artifact에서 APK와 대응 소스를 함께 받을 수 있습니다. 빌드 상태는 저장소의 Actions에서 확인하세요.

Actions 다운로드에는 `ChessCoach-0.2.0.apk`와 대응 소스 ZIP을 평평한 구조로 제공합니다. 개발 APK의 키는 각 CI 실행에서 생성하므로 이전 APK와 서명 불일치 시 앱 삭제 후 재설치가 필요합니다. 삭제 전 필요한 대국의 PGN을 복사하세요. PGN에는 AI 해설과 분석 캐시가 포함되지 않습니다. 동일한 개발 키로 업데이트하려면 Android 기본 개발 키 형식의 keystore를 base64로 인코딩해 저장소의 `CHESS_DEBUG_KEYSTORE_BASE64` Actions secret에 설정할 수 있습니다. 서명 키는 저장소·artifact·공개 캐시에 포함하지 않습니다. 정식 배포에는 보관하는 릴리스 키를 사용하세요.

설치 가능한 개발 APK: `app/build/outputs/apk/debug/app-debug.apk`. `adb install -r` 또는 Android Studio로 설치합니다. `assembleRelease`도 지원하지만 실제 배포에는 **본인이 보관하는 릴리스 서명 키**가 필요합니다. 개발용 서명 키는 저장소에 넣지 않습니다.

Codex 클라우드에서는 체크섬을 고정한 도구를 `/workspace/toolchains`에 준비하는 다음 스크립트를 사용할 수 있습니다. 설치된 도구와 빌드 결과는 재사용합니다. 시스템 HOME이나 CODEX_HOME을 바꾸지 않습니다.

```sh
bash scripts/setup-cloud.sh
```

프로젝트 범위 Caveman 스킬은 `.agents/skills`와 `skills-lock.json`에 포함됩니다. global 설치를 사용하지 않았습니다.

## OAuth 해설 연결

현재 구현은 안드로이드 앱이 본인 PC/서버의 companion에 연결하는 방식입니다. **폰 단독 ChatGPT 로그인은 구현하지 않았습니다.** 공개되지 않은 소비자 OAuth API를 직접 호출하는 대신 공식 Codex CLI/SDK가 계정 인증을 처리합니다. ChatGPT/Codex 계정, 이용 가능한 모델과 계정 한도가 필요합니다.

```sh
cd companion
npm ci
npx --no-install codex login
# 브라우저 사용이 어려운 경우 공식 CLI의 codex login --device-auth 사용
npx --no-install codex login status
export COACH_PAIRING_TOKEN='본인이-만든-32자-이상의-별도-랜덤-페어링-토큰'
export COACH_MODELS='gpt-5.4,gpt-5.3-codex'
export COACH_DEFAULT_MODEL='gpt-5.4'
npm start
```

위 페어링 토큰은 ChatGPT OAuth 토큰과 다른 값입니다. 서버는 OAuth 로그인 상태를 확인하고 API 키 인증으로 자동 전환하지 않습니다. OAuth 자격 증명은 공식 CLI가 서버 쪽에서 관리하며 앱의 페어링 토큰은 Android Keystore로 암호화합니다. 모델 목록은 서버 설정에 따르므로 계정에서 지원하는 모델 이름으로 조정하세요.

기본 서버는 `127.0.0.1:8787`에서 대기합니다. 실제 폰에는 유효한 TLS 인증서가 있는 HTTPS 주소를 제공하세요. 로컬 서버 앞에 HTTPS reverse proxy를 두거나 `COACH_TLS_CERT`, `COACH_TLS_KEY`, `COACH_HOST`, `COACH_PORT`를 설정할 수 있습니다. 원격 plaintext 리스너는 거부합니다. 디버그 APK의 Android 에뮬레이터만 `http://10.0.2.2:8787`을 사용할 수 있으며 릴리스 APK는 HTTPS를 요구합니다.

홈의 **AI 코치 연결**에서 서비스 주소·별도 연결 코드를 입력하세요. **ChatGPT로 로그인 · 브라우저 열기**는 서버에서 공식 `codex login --device-auth`를 시작하고 휴대폰 기본 브라우저에서 공식 기기 인증 페이지를 엽니다. 표시된 일회용 코드를 입력하고 앱으로 돌아오면 인증 상태를 확인합니다. 계정 설정에서 기기 인증을 허용해야 할 수 있습니다. 이미 서버에서 로그인했다면 추가 로그인 없이 연결됩니다. 대국 화면 상단 모델 버튼은 즉시 선택을 반영하고 해당 모델의 해설을 요청합니다. 진행 중인 요청은 마친 뒤 새 요청을 실행하며 모델별 해설을 모두 보존합니다. 연결 실패 시 엔진 분석은 계속 사용할 수 있고, 복기 화면에서 다시 분석해 누락된 해설을 요청할 수 있습니다. 복기를 닫으면 미완료 작업을 중단하며 완료된 결과는 저장됩니다.

검증 환경의 Codex 로그인 상태 조회는 성공했지만, 실제 해설 생성은 플랫폼의 읽기 전용 Codex 홈 때문에 공식 CLI가 `Read-only file system`으로 시작하지 못했습니다. **실제 OAuth 모델 응답까지의 검증은 미완료**입니다. 일반 PC/서버의 정상적으로 쓰기 가능한 공식 CLI 환경에서 추가 확인해야 합니다. 서버의 HTTP 요청·검증·캐시·모델 분리·사용량 저장은 자동 테스트로 검사했습니다.

## 검증과 설계

```sh
bash scripts/build-host-stockfish.sh
bash scripts/test-core.sh app/build/stockfish/host/src/stockfish
./gradlew assembleDebug assembleDebugAndroidTest assembleRelease lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.chesscoach.app.test/com.chesscoach.app.NativeSmoke
```

코어 검사는 실제 Stockfish와 합법 수/perft, SAN·PGN, 난이도 분리, MultiPV, 하이라이트를 확인합니다. Android instrumentation은 실제 SQLite 기록 병합·보존·무르기 및 APK의 네이티브 엔진을 확인합니다. 최종 실행 결과와 환경 제한은 [검증 기록](docs/validation.md)에 정리합니다.

[구조와 기록 형식](docs/architecture.md), [토큰 절약 및 참고 사례](docs/token-design.md)를 확인하세요. 전체 PGN 대신 FEN 두 개·상위 후보·짧은 PV·최근 평가만 보내고, 저장된 같은 모델의 해설과 서버 캐시를 재사용합니다. OAuth 경로의 토큰 할인이나 API 비용과의 동등성을 가정하지 않습니다.

## 라이선스와 공개

자체 코드는 GPL-3.0-or-later입니다. Stockfish·NNUE는 GPL v3 이상, Codex/Caveman/Gradle은 Apache 2.0, incbin은 Unlicense, 정적으로 링크한 LLVM 런타임은 관련 예외와 구성요소 고지를 따릅니다. 원문은 `licenses/`, 앱에서도 **오픈소스 라이선스**를 눌러 확인할 수 있습니다. 자세한 조건은 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)에 있습니다. OAuth 및 계정 이용은 OpenAI 이용약관과 계정 한도가 별도로 적용됩니다.

APK를 공유할 때 대응 소스도 함께 제공하세요. 다음 명령은 GitHub에 올리지 않는 NNUE까지 포함한 대응 소스 ZIP을 생성합니다.

```sh
python3 scripts/package-source.py
```

출력은 `artifacts/ChessCoach-0.2.0-source.zip`입니다. 생성한 바이너리·가중치·캐시·자격 증명은 Git에 포함하지 않습니다.

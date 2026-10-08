# Chess Coach

체스를 두면서 Stockfish의 추천 수와 그 근거를 복기하는 **네이티브 Android 앱**입니다. Java Activity/View/Canvas를 사용하며 Android 8.1(API 27) 이상 ARM64 기기를 우선 지원합니다. iOS 구현은 포함하지 않습니다. 현재 버전은 AI 핵심 이유와 짧은 설명을 위아래로 나누는 0.4.1입니다.

## 사용할 수 있는 기능

- AI 조언은 생성되는 대로 표시합니다. 상단에서는 **핵심 원인 한 줄**을 먼저 읽고, 바로 아래에서 위협·대응을 짧게 확인한 뒤, **자세히 보기**를 누르면 같은 Stockfish 근거로 상세 설명을 생성합니다. 요약과 상세 설명은 모델별로 함께 저장해 다시 열 때 재사용합니다. 상세 생성은 추가 구독 사용량을 소비합니다. 제목·굵게·기울임·목록·수 표기용 인라인 코드의 마크다운 서식을 네이티브 텍스트로 표시합니다.

- 처음 홈을 열면 AI 코칭을 위한 계정·모델 연결 안내가 나타납니다. 연결 버튼은 ChatGPT 연결 화면을 열고, 작은 회색 **괜찮아요, 연결 없이 사용할게요**를 누르면 이 설치에서는 안내가 다시 나타나지 않습니다. 나중에도 홈의 **AI 코치 연결 · 모델**에서 연결할 수 있습니다. 계정과 모델 연결을 마친 사용자에게도 안내를 반복하지 않습니다.

- 직전 수의 출발·도착 칸을 같은 노란색으로 표시하고 평가 배지를 함께 보여줍니다. 복기·추천 미리보기에서도 표시 중인 국면의 마지막 실전 이동을 따라갑니다.
- 홈 → 상대 난이도 → 대국, 홈 → 기록 → 기록 추가로 분리한 화면. 상단 AI 코치와 대국 중 모델 선택, 채움 벡터 기물, 판 위 색상·기호·추천 화살표를 지원합니다.
- 오프라인 Stockfish 19 대국, 시스템을 따르는 화이트/다크 모드.
- 상대 난이도: 입문 Skill 0, 목표 Elo 1320·1600·2000·2400·2800·3190, 제한 없는 최강. 분석 엔진은 별도 프로세스로 항상 Skill 20과 실력 제한 해제를 유지합니다. 목표 Elo는 다른 서비스의 사용자 레이팅과 직접 비교할 수 없습니다.
- 매 반수의 추천 수, 상위 후보 3개, 평가·메이트·탐색 깊이·PV를 계산하고 저장합니다. 실력 제한을 해제하더라도 탐색은 기기 성능과 시간의 영향을 받습니다.
- **Sign in with ChatGPT + Responses API 직접 연결**을 통한 한국어 해설. 서버·Codex CLI·API 키 없이 사용자 자신의 계정으로 연결하며, 계정별 모델 조회와 사용량 관리를 지원합니다.
- 직접 둔 대국과 가져온 대국을 동일한 **대국 기록** 화면에서 보관합니다. 수순, 엔진 분석, 실제 AI 해설·모델·사용량을 SQLite에 저장합니다.
- PGN/SAN 또는 UCI 좌표 수순을 붙여넣고 수마다 이동할 수 있습니다. 예: `1. e4 e5 2. Nf3 Nc6 *` 또는 `e2e4 e7e5 g1f3 b8c6`. 한 번에 한 대국의 메인 라인을 가져오며 주석·가지 변화는 제외합니다.
- 가져온 대국은 전체 분석 후 **학습 하이라이트**를 생성합니다. 직접 둔 대국도 복기 화면의 같은 버튼으로 생성할 수 있습니다. 하이라이트는 후보 비교·방어·좋은 선택처럼 발전에 도움이 될 국면을 골라 이유를 표시하고 해당 수로 이동합니다. 평가 차이와 PV에 기반한 휴리스틱이며 개인 맞춤 실력 진단은 아닙니다.
- 앱 설정과 대국 기록은 로컬에 보관하며 기기 백업에서 제외합니다. 앱을 삭제하면 기록도 삭제되므로 필요하면 PGN을 복사해 보관하세요.

수 평가는 초록 ★ 최선수, 연두 ✓ 좋은 수, 노랑 ?! 아쉬운 수, 주황 ? 실수, 빨강 ?? 블런더로 표시합니다. 깊이 12 이상에서 합법적인 PV 4반수 동안 기물 희생과 평가 보상이 확인되면 청록 !! **탁월 후보**로 표시합니다. 이는 공개한 휴리스틱으로, Chess.com의 독점 판정과 같지 않으며 확정적인 탁월수 판정도 아닙니다. 추천수 보기는 착수 전 국면에 초록 추천 화살표와 회색 실전 화살표를 표시합니다.

## Android 빌드

Linux x86_64, JDK 17 이상(검증한 버전 21), Android SDK API 35/build-tools 35.0.0/NDK 27.2.12479018, Python 3, GNU make가 필요합니다. Node.js는 Caveman 개발 스킬 설치에만 사용합니다. Android Studio의 SDK Manager로 도구를 설치한 경우 다음을 실행하세요.

```sh
export ANDROID_SDK_ROOT=/path/to/Android/Sdk
bash scripts/build-stockfish.sh
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
```

`build-stockfish.sh`는 SHA-256이 고정된 공식 Stockfish 릴리스에서 NNUE를 복원하고 ARM64 NEON 및 x86_64 SSE2 실행 파일을 소스로 빌드합니다. 전체 NNUE가 실행 파일에 포함되었는지도 검사합니다. 두 ABI 모두 API 26 및 16KB ELF 페이지 정렬로 빌드합니다. 큰 NNUE가 포함되므로 APK 크기와 최초 빌드 시간이 큽니다.

GitHub에 push하면 `Native Android` Actions가 APK와 대응 소스를 같은 다운로드 artifact로 만들도록 설정했습니다. 성공한 실행의 `ChessCoach-Android-and-source` artifact에서 APK와 대응 소스를 함께 받을 수 있습니다. 빌드 상태는 저장소의 Actions에서 확인하세요.

Actions 다운로드에는 `ChessCoach-0.4.1.apk`와 대응 소스 ZIP을 평평한 구조로 제공합니다. 개발 APK의 키는 각 CI 실행에서 생성하므로 이전 APK와 서명 불일치 시 앱 삭제 후 재설치가 필요합니다. 삭제 전 필요한 대국의 PGN을 복사하세요. PGN에는 AI 해설과 분석 캐시가 포함되지 않습니다. 동일한 개발 키로 업데이트하려면 Android 기본 개발 키 형식의 keystore를 base64로 인코딩해 저장소의 `CHESS_DEBUG_KEYSTORE_BASE64` Actions secret에 설정할 수 있습니다. 서명 키는 저장소·artifact·공개 캐시에 포함하지 않습니다. 정식 배포에는 보관하는 릴리스 키를 사용하세요.

설치 가능한 개발 APK: `app/build/outputs/apk/debug/app-debug.apk`. `adb install -r` 또는 Android Studio로 설치합니다. `assembleRelease`도 지원하지만 실제 배포에는 **본인이 보관하는 릴리스 서명 키**가 필요합니다. 개발용 서명 키는 저장소에 넣지 않습니다.

Codex 클라우드에서는 체크섬을 고정한 도구를 `/workspace/toolchains`에 준비하는 다음 스크립트를 사용할 수 있습니다. 설치된 도구와 빌드 결과는 재사용합니다. 시스템 HOME이나 CODEX_HOME을 바꾸지 않습니다.

```sh
bash scripts/setup-cloud.sh
```

프로젝트 범위 Caveman 스킬은 `.agents/skills`와 `skills-lock.json`에 포함됩니다. global 설치를 사용하지 않았습니다.

## 서버 없는 ChatGPT 연결

1. 홈의 **AI 코치 연결** → **Continue with ChatGPT**를 누릅니다.
2. 시스템 브라우저에서 자신의 ChatGPT 계정으로 로그인하고 워크스페이스와 구독 사용 권한을 선택합니다.
3. 브라우저가 휴대폰의 `127.0.0.1` 콜백으로 돌아오면 앱으로 전환합니다. 로그인 알림을 눌러 돌아와도 됩니다.
4. 앱이 ID 토큰 서명·발급자·대상·nonce·만료와 실제 허용된 scope를 검증하고, 계정 모델 목록을 조회합니다.
5. **연결 테스트 · AI 응답 받기**를 눌러 `Token sharing works.` 응답을 확인합니다. `response.completed`를 받아야 성공으로 표시합니다.
6. 대국 또는 복기 화면에서 모델을 선택하면 로컬 Stockfish 근거를 해당 모델로 직접 전송합니다.

공식 [OSS OAuth 문서](https://developers.openai.com/siwc/token-sharing-open-source)와 [구현 예제](https://developers.openai.com/cookbook/articles/sign-in-with-chatgpt)를 따릅니다. 최초 로그인은 `dynamic_agent_client`와 설치별 host ID로 동적 등록하고, 발급된 client ID는 다음 로그인에 재사용합니다. 로그인 중에만 foreground service가 IPv4 loopback 수신기를 유지하며, 임의 포트와 `/auth/callback`을 인증 요청·토큰 교환에서 동일하게 사용합니다. 브라우저 로그인은 기본 브라우저에서 진행하고 앱은 비밀번호를 받지 않습니다.

각 계정 등록은 분리해 보관합니다. access/refresh/ID 토큰과 계정 정보 전체를 Android Keystore AES-GCM으로 암호화하고 파일을 원자적으로 교체합니다. 백업은 제외하며, 상태/nonce/PKCE는 로그인 시도 동안 메모리에만 유지합니다. 프로세스가 종료되면 새 로그인이 필요합니다. 모델 선택 목록은 `/v1/models`의 `visibility: list` 항목을 서버 순서와 `display_name`으로 표시하고 요청에는 `slug`를 보냅니다. 계정 변경 시 목록을 다시 조회합니다.

이 앱은 무료 GPL 오픈소스 앱이며 API 키 기반 별도 과금으로 전환하지 않습니다. 공식 안내상 대상 ChatGPT Plus/Pro 계정 및 워크스페이스 정책·사용 한도가 적용됩니다. 요청은 사용자가 허용한 구독 사용량 또는 계정에서 허용한 크레딧을 사용할 수 있습니다. **Manage usage**는 [ChatGPT 사용량 설정](https://chatgpt.com/settings/usage)을 엽니다. 한도 초과 응답 이후 새 AI 요청은 중단하고, 설정을 확인한 뒤 연결 테스트 버튼으로 명시적으로 재시도합니다. 일시적인 사용량 조회 불가(503)는 한도 소진으로 단정하지 않습니다.

로그아웃은 선택 계정의 갱신 세션 해제를 시도하고 로컬 토큰을 지웁니다. 네트워크 때문에 원격 해제를 확인하지 못하면 이를 안내합니다. 계정 등록과 host ID는 이후 재로그인을 위해 유지합니다. 이전 버전의 서버·연결 코드는 사용하지 않습니다. 대국 기록과 저장된 해설은 같은 로컬 DB에 남습니다.

**실제 사용자 계정의 브라우저 동의부터 Responses 응답까지는 자동화된 클라우드 테스트에서 완료하지 못했습니다.** 위 연결 테스트로 실제 Android 기기에서 확인해야 합니다. 자동 프로토콜 검사와 모의 토큰은 실계정 성공을 의미하지 않습니다. Android 14/15 이상의 브라우저 전환·foreground service 동작도 실제 기기 확인 대상입니다.

## 검증과 설계

```sh
bash scripts/build-host-stockfish.sh
bash scripts/test-core.sh app/build/stockfish/host/src/stockfish
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest assembleRelease lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.chesscoach.app.test/com.chesscoach.app.NativeSmoke
```

코어 검사는 실제 Stockfish와 합법 수/perft, SAN·PGN, 난이도 분리, MultiPV, 하이라이트를 확인합니다. Android instrumentation은 실제 SQLite 기록 병합·보존·무르기 및 APK의 네이티브 엔진을 확인합니다. 최종 실행 결과와 환경 제한은 [검증 기록](docs/validation.md)에 정리합니다.

[구조와 기록 형식](docs/architecture.md), [토큰 절약 및 참고 사례](docs/token-design.md)를 확인하세요. 전체 PGN 대신 FEN 두 개·상위 후보·짧은 PV·최근 평가만 보내고, 저장된 같은 모델의 로컬 해설 캐시를 재사용합니다. OAuth 경로의 토큰 할인이나 API 비용과의 동등성을 가정하지 않습니다.

## 라이선스와 공개

자체 코드는 GPL-3.0-or-later입니다. Stockfish·NNUE는 GPL v3 이상, Nimbus JOSE+JWT/Caveman/Gradle은 Apache 2.0, incbin은 Unlicense, 정적으로 링크한 LLVM 런타임은 관련 예외와 구성요소 고지를 따릅니다. 원문은 `licenses/`, 앱에서도 **오픈소스 라이선스**를 눌러 확인할 수 있습니다. 자세한 조건은 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)에 있습니다. OAuth 및 계정 이용은 OpenAI 이용약관과 계정 한도가 별도로 적용됩니다.

APK를 공유할 때 대응 소스도 함께 제공하세요. 다음 명령은 GitHub에 올리지 않는 NNUE까지 포함한 대응 소스 ZIP을 생성합니다.

```sh
python3 scripts/package-source.py
```

출력은 `artifacts/ChessCoach-0.4.1-source.zip`입니다. 생성한 바이너리·가중치·캐시·자격 증명은 Git에 포함하지 않습니다.

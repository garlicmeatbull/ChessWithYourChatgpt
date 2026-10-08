# 0.4.1 핵심 원인·짧은 설명 검증 (2026-10-08)

- `testDebugUnitTest assembleDebug assembleRelease lintDebug`: 성공. JVM 29개(프로토콜 17개 + 코칭 12개), 실패/오류/스킵 0.
- 새 검사: 모든 스트리밍 경계와 반대 필드 생성 순서에서도 핵심·본문을 분리, 상세 병합 후 핵심 유지, 이전 요약에 핵심을 임의로 만들지 않는 호환성.
- 새 조언은 `headline`, `summary`를 한 요청에서 받습니다. 핵심은 굵은 별도 텍스트로 표시하고, 실제 앱 수 평가를 근거 자료로 전달합니다.
- 이번 변경의 실기기 표시·문장 품질과 응답 시간은 추가 확인 대상입니다. Android instrumentation은 이번에는 재실행하지 않았으며 아래 0.4.0 결과는 이전 버전 결과입니다.

# 0.4.0 요약·상세 스트리밍 검증 (2026-10-08)

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest assembleRelease lintDebug`: 성공. JVM 27개(기존 프로토콜 17개 + 새 코칭 10개), 실패/오류/스킵 0. Lint 오류 0, 경고 20.
- 새 코칭 검사: SSE delta 전달과 최종 텍스트 우선, 미완료/취소 응답 미저장, 부분 JSON의 모든 문자열 경계·유니코드/이스케이프, 요약·상세 및 두 usage의 병합, 이전 캐시 호환, 선택한 수의 대기 요청 우선순위.
- Android API 30 x86_64에서 `am instrument -w -e coachOnly true com.chesscoach.app.test/com.chesscoach.app.NativeSmoke`: **6개 PASS**, `INSTRUMENTATION_CODE: -1`. 실제 네이티브 마크다운 span 및 SQLite에 요약·상세와 두 요청의 usage 저장/재열기를 확인했습니다. 코칭 외 기존 instrumentation 검사는 이번에는 재실행하지 않았습니다.
- 대응 소스 ZIP 생성 및 Java 의존성 소스 체크섬 확인 성공. 새 외부 마크다운 의존성은 추가하지 않았습니다.
- 실제 서비스의 첫 토큰/완료 응답 시간과 품질 개선 비율은 측정하지 않았습니다. 실기기의 스트리밍·팝업 외관·큰 글꼴·모델 변경/오류 동작은 `docs/siwc-android.md` 절차로 추가 확인해야 합니다. 아래 이전 버전 결과는 과거 검증입니다.

# 0.3.0 직접 ChatGPT 연결 검증 (2026-10-08)

- 공식 OSS OAuth·모델·Responses·preview 문서와 production OIDC discovery를 읽어 동적 등록과 공개 API 직접 호출을 확인했습니다.
- Gradle JVM OAuth/Responses 검사 17개 통과. RSA 서명·issuer/audience/nonce/시간·계정 혼합·PKCE·callback·동의 범위·스트림 완료·중간 한도 오류·503 구분·갱신 응답을 확인했습니다. 테스트 키와 모의 이벤트를 사용하며 실계정 성공을 의미하지 않습니다.
- 체스 코어/실제 호스트 Stockfish 검사 54개 통과.
- Android 11/API 30 x86_64 instrumentation 20개 통과. 실제 SQLite 기록 보존·Keystore 암호화/원자 저장·APK Stockfish·foreground loopback 준비와 위조 콜백 거부를 확인했습니다. 결과 `checks=20`, `result=PASS`, `INSTRUMENTATION_CODE=-1`. 서명 검증용 RSA 및 실제 OpenAI 계정은 JVM/수동 검증 범위를 따릅니다.
- debug APK, Android test APK, release APK 빌드 및 Android lint 통과(오류 0). 릴리스 APK는 배포용 서명 검증 대상입니다.
- 대응 소스 ZIP 생성과 NNUE·Java 의존성 소스 3개·원본 POM·고정 해시를 확인했습니다. APK/서명 키/이전 서버의 node_modules가 포함되지 않았습니다.
- 연결 화면의 로그인·계정·모델 조회·응답 테스트·사용량 메뉴를 UI dump로 확인했습니다. 로그인 전 모델/응답 버튼은 disabled입니다. 소프트웨어 에뮬레이터에서 Home→연결 화면 전환 중 FocusEvent(hasFocus=false) 입력 시간 초과 ANR이 발생했습니다. 수집한 앱 UI 스레드는 MessageQueue.nativePollOnce 대기 상태였고 system_server 과부하도 관측됐습니다. 이 관측만으로 실기기에서 ANR이 없다고 보장하지 않으며 화면 전환 안정성은 실기기 확인 대상입니다.
- 실제 사용자 OAuth 로그인·구독 응답은 미검증입니다. 사용자 자신의 계정으로 [Android 최소 기능 절차](siwc-android.md)를 수행해야 합니다. Android 14/15 이상의 실기기 브라우저 전환도 확인 대상입니다.
- 앱의 서버 연결 구조와 설치 단계는 제거했습니다. 아래 0.1/0.2의 서버 검사 및 read-only Codex 홈 기록은 과거 구조의 기록입니다.

# 과거 검증 기록

2026-10-07 클라우드 Linux x86_64에서 실행했습니다. 기기 검사는 Android 11/API 30 x86_64 소프트웨어 에뮬레이터를 사용했습니다. 물리 Android ARM64 기기와 iOS 검증은 수행하지 않았습니다.

| 검사 | 결과 | 확인 범위 |
| --- | --- | --- |
| `bash scripts/setup-cloud.sh` | 통과 | 고정 JDK/SDK 초기화, `npm ci`, 두 Android ABI와 호스트 엔진, 코어/서버 검사, APK와 lint |
| `bash scripts/test-core.sh app/build/stockfish/host/src/stockfish` | 코어 54개, 서버 10개 통과 | perft·합법 수·캐슬링·앙파상·SAN/PGN·하이라이트, 실제 Stockfish 19 MultiPV·강제 검색·독립 난이도, 백/흑 수 평가·희생 후보, HTTP 검증·캐시·모델 분리·공식 기기 인증 출력 파싱·인증된 기기 로그인 경로 |
| `assembleDebug assembleDebugAndroidTest assembleRelease lintDebug` | 통과 | 설치 가능한 개발 APK, instrumentation APK, 미서명 release APK, Android API 호환성 검사 |
| Android `NativeSmoke` | **16개 통과**, `INSTRUMENTATION_CODE: -1` | 실제 SQLite 저장·병합·모델별 해설 병합·보존·두 출처 하이라이트·무르기·재개, Keystore 암호화/복호화, HTTPS 검증, 패키징된 네이티브 Stockfish MultiPV·합법 응수·Elo 설정 |
| 두 ABI 전체 NNUE SHA-256 검사 | 통과 | ARM64와 x86_64 실행 파일에 전체 검증된 98,511,183바이트 NNUE 포함 |
| ELF 헤더 검사 | 통과 | 두 ABI는 Android PIE 실행 파일, LOAD 세그먼트 모두 16KB 정렬 |
| companion 실제 HTTP 시작·모델 목록·OAuth 상태 | 통과 | 로컬 서버 요청과 공식 CLI의 ChatGPT 로그인 상태 조회 |
| 실제 OAuth 모델 해설 | **차단** | 공식 Codex CLI가 플랫폼의 읽기 전용 Codex 홈에서 `Read-only file system`으로 초기화 실패. 실제 모델 응답을 검증했다고 주장하지 않음 |
| 네이티브 화면 터치·시스템 테마 | 통과 | e2-e4 착수 후 Stockfish의 e7-e5 응수, 백 차례 복귀, 시스템 라이트/다크 전환, 직접 대국의 기록 화면 표시 |
| GitHub Actions 실행 | 0.1.0 성공 확인, 0.2.0 실행은 Actions에서 확인 | 0.1.0 최종 실행 [37567537551](https://github.com/garlicmeatbull/ChessWithYourChatgpt/actions/runs/37567537551)이 성공하여 APK와 소스 artifact를 생성함. 최초 실행은 Android toolchain 단계에서 exit 127로 실패. SDK 설치 액션도 실패하여 체크섬을 고정한 자체 installer로 교체. 새 SDK 디렉터리 설치와 Android 빌드를 클라우드에서 재검증. 최신 GitHub 실행·artifact 결과는 저장소 Actions에서 확인 |

Android lint는 오류 0개, 경고 26개로 종료했습니다. 주로 한국어 UI 문자열의 번역 리소스화, 아이콘 구성 및 backup 설정 관련 경고입니다. 검사를 비활성화하거나 lint baseline으로 실패를 숨기지 않았습니다. Windows용 원본 Gradle wrapper는 CRLF를 유지하고 Stockfish 원본의 공백도 변경하지 않았습니다.

처음 instrumentation 실행은 테스트 APK의 별도 앱 데이터 경로 권한 때문에 실패했습니다. 테스트 DB를 실제 target 앱 영역의 별도 이름으로 생성하고 테스트 전용 SharedPreferences를 사용하도록 수정한 뒤 14개 검사가 통과했습니다. 실제 사용자 DB와 페어링 설정은 테스트가 삭제하지 않습니다.

하드웨어 가속(`/dev/kvm`)이 없어 소프트웨어 에뮬레이션의 부팅·설치가 느리고 Android System UI 자체의 ANR이 발생했습니다. 해당 System UI를 테스트 기기에서만 비활성화하고 앱의 독립적인 instrumentation을 실행했습니다. 앱의 엔진 시작/초기 검색은 느린 기기를 고려해 비동기 작업과 제한된 timeout을 사용합니다. 실제 휴대폰 성능·배터리·긴 대국·모델 계정별 호출 한도는 추가 확인 대상입니다.

배포용 release 키로 서명하거나 Play Store에 게시하지 않았습니다. 제공하는 APK는 개발용 서명입니다. 저장소와 대응 소스 ZIP에는 인증 정보·서명 키·캐시를 넣지 않습니다.

0.2.0은 홈/대국 설정/기보 추가 화면, 채움 벡터 기물, 판의 색상·기호·화살표, 상단 AI 카드와 모델 선택을 추가했습니다. 실제 브라우저 계정 로그인과 OAuth 모델 응답은 위의 클라우드 제한으로 검증하지 못했으며, 기기 인증 테스트는 공식 출력 형식에 맞춘 모의 프로세스를 사용합니다.

0.2.0 에뮬레이터에서 홈의 대국/기록 진입, 난이도 목록과 고정 시작 버튼, 상단 코치 카드와 모델 버튼, 흰 기물 내부 채움을 화면 덤프와 스크린샷으로 확인했습니다. 480×800 화면에서 대국 판은 남은 공간에 맞춰 정사각형으로 배치됩니다.

0.2.0 대국 화면에서 e2-e4 입력 후 e7-e5 응수, 백 차례 복귀, 상단의 ★ 최선수 배지와 저장된 평가를 확인했습니다.

최종 UI 검증 중 소프트웨어 에뮬레이터의 엔진 초기화와 화면 전환이 겹칠 때 FocusEvent 시간 초과로 앱 ANR이 한 번 발생했습니다. 수집 시점 UI 스레드는 MessageQueue 대기 상태였습니다. 엔진 실행 스레드는 background 우선순위로 내리고 종료도 UI 밖에서 수행하도록 보완했습니다. 기록 화면으로 재진입은 성공했습니다. 물리 기기에서 같은 문제가 발생하지 않는다는 보장은 하지 않습니다.

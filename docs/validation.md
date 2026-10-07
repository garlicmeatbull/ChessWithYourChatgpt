# 검증 기록

2026-10-07 클라우드 Linux x86_64에서 실행했습니다. 기기 검사는 Android 11/API 30 x86_64 소프트웨어 에뮬레이터를 사용했습니다. 물리 Android ARM64 기기와 iOS 검증은 수행하지 않았습니다.

| 검사 | 결과 | 확인 범위 |
| --- | --- | --- |
| `bash scripts/setup-cloud.sh` | 통과 | 고정 JDK/SDK 초기화, `npm ci`, 두 Android ABI와 호스트 엔진, 코어/서버 검사, APK와 lint |
| `bash scripts/test-core.sh app/build/stockfish/host/src/stockfish` | 코어 47개, 서버 7개 통과 | perft·합법 수·캐슬링·앙파상·SAN/PGN·하이라이트, 실제 Stockfish 19 MultiPV·강제 검색·독립 난이도, HTTP 검증·캐시·모델 분리 |
| `assembleDebug assembleDebugAndroidTest assembleRelease lintDebug` | 통과 | 설치 가능한 개발 APK, instrumentation APK, 미서명 release APK, Android API 호환성 검사 |
| Android `NativeSmoke` | **14개 통과**, `INSTRUMENTATION_CODE: -1` | 실제 SQLite 저장·병합·모델 보존·두 출처 하이라이트·무르기·재개, Keystore 암호화/복호화, HTTPS 검증, 패키징된 네이티브 Stockfish MultiPV·합법 응수·Elo 설정 |
| 두 ABI 전체 NNUE SHA-256 검사 | 통과 | ARM64와 x86_64 실행 파일에 전체 검증된 98,511,183바이트 NNUE 포함 |
| ELF 헤더 검사 | 통과 | 두 ABI는 Android PIE 실행 파일, LOAD 세그먼트 모두 16KB 정렬 |
| companion 실제 HTTP 시작·모델 목록·OAuth 상태 | 통과 | 로컬 서버 요청과 공식 CLI의 ChatGPT 로그인 상태 조회 |
| 실제 OAuth 모델 해설 | **차단** | 공식 Codex CLI가 플랫폼의 읽기 전용 Codex 홈에서 `Read-only file system`으로 초기화 실패. 실제 모델 응답을 검증했다고 주장하지 않음 |
| 네이티브 화면 터치·시스템 테마 | 통과 | e2-e4 착수 후 Stockfish의 e7-e5 응수, 백 차례 복귀, 시스템 라이트/다크 전환, 직접 대국의 기록 화면 표시 |
| GitHub Actions 실행 | 미실행 | workflow를 추가했고 같은 빌드 명령은 클라우드에서 실행. GitHub의 실제 실행 결과는 push 후 확인 필요 |

Android lint는 오류 0개, 경고 16개로 종료했습니다. 주로 한국어 UI 문자열의 번역 리소스화, 아이콘 구성 및 backup 설정 관련 경고입니다. 검사를 비활성화하거나 lint baseline으로 실패를 숨기지 않았습니다. Windows용 원본 Gradle wrapper는 CRLF를 유지하고 Stockfish 원본의 공백도 변경하지 않았습니다.

처음 instrumentation 실행은 테스트 APK의 별도 앱 데이터 경로 권한 때문에 실패했습니다. 테스트 DB를 실제 target 앱 영역의 별도 이름으로 생성하고 테스트 전용 SharedPreferences를 사용하도록 수정한 뒤 14개 검사가 통과했습니다. 실제 사용자 DB와 페어링 설정은 테스트가 삭제하지 않습니다.

하드웨어 가속(`/dev/kvm`)이 없어 소프트웨어 에뮬레이션의 부팅·설치가 느리고 Android System UI 자체의 ANR이 발생했습니다. 해당 System UI를 테스트 기기에서만 비활성화하고 앱의 독립적인 instrumentation을 실행했습니다. 앱의 엔진 시작/초기 검색은 느린 기기를 고려해 비동기 작업과 제한된 timeout을 사용합니다. 실제 휴대폰 성능·배터리·긴 대국·모델 계정별 호출 한도는 추가 확인 대상입니다.

배포용 release 키로 서명하거나 Play Store에 게시하지 않았습니다. 제공하는 APK는 개발용 서명입니다. 저장소와 대응 소스 ZIP에는 인증 정보·서명 키·캐시를 넣지 않습니다.

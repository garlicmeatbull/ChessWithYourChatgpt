# 사용한 구성요소와 배포 조건

이 앱의 자체 코드는 GPL-3.0-or-later로 제공합니다. GitHub에 소스와 바이너리를 공개할 때 아래 원문과 고지를 함께 유지하세요. 앱의 오픈소스 라이선스 화면에도 고지가 포함됩니다.

| 구성요소 | 용도 | 라이선스와 포함한 고지 |
| --- | --- | --- |
| Stockfish 19, 공식 NNUE | APK에 포함되는 대국·분석 엔진 | GPL-3.0-or-later. `vendor/stockfish/Copying.txt`, `AUTHORS`, 전체 소스, NNUE 복원 스크립트 포함 |
| incbin | Stockfish의 NNUE 임베딩 | Unlicense. `licenses/Incbin-Unlicense.txt` |
| Android NDK libc++, libc++abi, compiler runtime | APK 엔진에 정적으로 연결되는 런타임 | Apache-2.0 WITH LLVM-exception 및 해당 구성요소 고지. `licenses/LLVM-NOTICE.txt` 원문 포함 |
| Nimbus JOSE+JWT 10.5 | APK의 RS256/JWKS ID 토큰 검증 | Apache-2.0. `licenses/Nimbus-Apache-2.0.txt`. Copyright Connect2id Ltd. |
| Nimbus에 포함된 Gson 2.13.1 | JWT JSON 처리 | Apache-2.0. Copyright Google Inc. 동일 Apache 원문 포함 |
| Nimbus에 포함된 JCIP annotations 1.0-1 | 동시성 주석 | Apache-2.0. Copyright 2013 Stephen Connolly. clean-room 구현 |
| Caveman 프로젝트 스킬 | 개발 도구, `.agents/skills` | Apache-2.0. `licenses/Caveman-Apache-2.0.txt`. 스킬 파일의 기존 고지 유지 |
| Gradle Wrapper 8.13 | 소스 빌드 도구 | Apache-2.0. `licenses/Gradle-Apache-2.0.txt` |

Stockfish 원본은 `official-stockfish/Stockfish`, 태그 `sf_19`, 커밋 `edb0d9db6731067ec50ce619ff372b463bc4dd5d`입니다. vendored Stockfish 소스를 변경하지 않고 빌드 설정을 별도 스크립트에 두었습니다. ARM64 엔진은 모든 ARM64 기기에서 실행 가능한 NEON 빌드이며, 안드로이드용 두 ABI 모두 16KB ELF 페이지 정렬로 재빌드합니다.

NNUE 원본 파일은 `nn-1a298aa575a0.nnue`입니다. SHA-256은 `1a298aa575a085434d29027978dc36867fe9c5bcea9376654b7a8eba1e52dfc2`. `scripts/stockfish-assets.py`는 체크섬이 고정된 공식 릴리스에서 NNUE를 추출하고 전체 해시를 검증합니다. 다운로드한 실행 파일을 실행해 데이터를 추출하지 않습니다.

GPL에 따라 APK를 배포할 때 **그 APK에 대응하는 전체 소스와 빌드 방법도 같은 배포에서 제공**하세요. `scripts/package-source.py`가 NNUE를 포함한 대응 소스 ZIP을 만듭니다. Stockfish 이름·저작권·라이선스·원저자 고지를 유지하세요. 원문 GPL과 Apache 라이선스의 보증 부인 조항도 그대로 포함합니다. GNU GPL v3와 Apache 2.0의 호환성을 고려해 프로젝트의 자체 코드도 GPL v3 이상으로 정했습니다.

OAuth는 별도의 오픈소스 라이선스가 아닌 인증 방식입니다. Sign in with ChatGPT는 OpenAI의 공개 [OSS 구독 사용 문서](https://developers.openai.com/siwc/token-sharing-open-source)와 [이용약관](https://openai.com/policies/terms-of-use/)·정책·계정 한도를 따릅니다. 이 앱은 무료 GPL 오픈소스 앱이며 OpenAI 공식 앱이 아닙니다. 사용자 자신의 계정과 승인한 구독/크레딧만 사용하고 API 키 과금 경로로 전환하지 않습니다. 유료 또는 원격 호스팅 앱의 제공 조건은 별도이므로 해당 배포 방식으로 변경할 때 공식 접근 조건을 다시 확인해야 합니다. Codex CLI/SDK는 APK나 현재 연결 구조에 포함하지 않습니다.

Nimbus의 바이너리에는 Gson과 JCIP가 재배치된 패키지로 포함됩니다. `vendor/java/sources.json`이 각 원본 source JAR과 Nimbus 원본 Maven POM의 버전·URL·SHA-256을 고정합니다. `scripts/java-sources.py`가 검증하며 대응 소스 ZIP에 원문 소스와 POM을 함께 넣습니다. Nimbus POM의 Maven Shade 설정이 원본 패키지를 재배치합니다. 세 구성요소는 자체 수정 없이 사용합니다. `junit:junit:4.13.2`(EPL-1.0)와 `org.json:json:20250517`(public domain)은 JVM 테스트 전용이며 APK에 포함하지 않습니다.

체스 기물은 자체 제작한 채움 벡터를 Canvas로 그립니다. Chess.com의 기물 이미지·아이콘·소스는 사용하지 않습니다. UI 폰트는 Android 시스템 폰트이며 별도 폰트 파일을 APK에 복사하지 않습니다. Android SDK/JDK는 빌드 전제이며 APK에 번들하지 않습니다. Node.js는 Caveman 개발 스킬 설치 때만 선택적으로 필요합니다.

# 사용한 구성요소와 배포 조건

이 앱과 설명 서버의 자체 코드는 GPL-3.0-or-later로 제공합니다. GitHub에 소스와 바이너리를 공개할 때 아래 원문과 고지를 함께 유지하세요. 앱의 오픈소스 라이선스 화면에도 고지가 포함됩니다.

| 구성요소 | 용도 | 라이선스와 포함한 고지 |
| --- | --- | --- |
| Stockfish 19, 공식 NNUE | APK에 포함되는 대국·분석 엔진 | GPL-3.0-or-later. `vendor/stockfish/Copying.txt`, `AUTHORS`, 전체 소스, NNUE 복원 스크립트 포함 |
| incbin | Stockfish의 NNUE 임베딩 | Unlicense. `licenses/Incbin-Unlicense.txt` |
| Android NDK libc++, libc++abi, compiler runtime | APK 엔진에 정적으로 연결되는 런타임 | Apache-2.0 WITH LLVM-exception 및 해당 구성요소 고지. `licenses/LLVM-NOTICE.txt` 원문 포함 |
| OpenAI Codex CLI / TypeScript SDK 0.160.1 | PC/서버의 OAuth 인증·해설 | Apache-2.0. `licenses/Codex-Apache-2.0.txt`, 패키지 lockfile 포함. APK에는 CLI를 넣지 않음 |
| Caveman 프로젝트 스킬 | 개발 도구, `.agents/skills` | Apache-2.0. `licenses/Caveman-Apache-2.0.txt`. 스킬 파일의 기존 고지 유지 |
| Gradle Wrapper 8.13 | 소스 빌드 도구 | Apache-2.0. `licenses/Gradle-Apache-2.0.txt` |

Stockfish 원본은 `official-stockfish/Stockfish`, 태그 `sf_19`, 커밋 `edb0d9db6731067ec50ce619ff372b463bc4dd5d`입니다. vendored Stockfish 소스를 변경하지 않고 빌드 설정을 별도 스크립트에 두었습니다. ARM64 엔진은 모든 ARM64 기기에서 실행 가능한 NEON 빌드이며, 안드로이드용 두 ABI 모두 16KB ELF 페이지 정렬로 재빌드합니다.

NNUE 원본 파일은 `nn-1a298aa575a0.nnue`입니다. SHA-256은 `1a298aa575a085434d29027978dc36867fe9c5bcea9376654b7a8eba1e52dfc2`. `scripts/stockfish-assets.py`는 체크섬이 고정된 공식 릴리스에서 NNUE를 추출하고 전체 해시를 검증합니다. 다운로드한 실행 파일을 실행해 데이터를 추출하지 않습니다.

GPL에 따라 APK를 배포할 때 **그 APK에 대응하는 전체 소스와 빌드 방법도 같은 배포에서 제공**하세요. `scripts/package-source.py`가 NNUE를 포함한 대응 소스 ZIP을 만듭니다. Stockfish 이름·저작권·라이선스·원저자 고지를 유지하세요. 원문 GPL과 Apache 라이선스의 보증 부인 조항도 그대로 포함합니다. GNU GPL v3와 Apache 2.0의 호환성을 고려해 프로젝트의 자체 코드도 GPL v3 이상으로 정했습니다.

OAuth는 별도의 오픈소스 라이선스가 아닌 인증 방식입니다. ChatGPT/Codex 계정과 모델 사용에는 [OpenAI 이용약관](https://openai.com/policies/terms-of-use/) 및 해당 서비스 정책·계정 이용 한도가 별도로 적용됩니다. 이 앱은 OpenAI 공식 앱이 아니며, 공식 Codex CLI와 SDK를 이용합니다. OAuth 자격 증명은 본인 PC/서버에 남고 앱에는 설명 서버의 페어링 토큰만 저장합니다.

Android 시스템의 폰트·체스 유니코드 글리프는 OS에서 렌더링하며 별도 폰트 파일을 APK에 복사하지 않습니다. Android SDK/JDK와 Node.js는 개발·서버 실행 전제이며 APK에 번들하지 않습니다. 서버 실행 시 배포하는 런타임이나 추가하는 패키지가 생기면 그 고지를 함께 갱신하세요.

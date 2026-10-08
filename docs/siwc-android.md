# Android 직접 인증과 최소 기능 검증

공식 규격 확인: 2026-10-08. [등록·로그인](https://developers.openai.com/siwc/token-sharing-open-source/sign-in), [계정·세션](https://developers.openai.com/siwc/token-sharing-open-source/profiles-and-sessions), [모델·추론](https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference), [Preview 제한](https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations), [UI 지침](https://developers.openai.com/siwc/ui-ux-guidelines), [공식 사례](https://developers.openai.com/cookbook/articles/sign-in-with-chatgpt)를 사용했습니다.

## 앱에서 확인할 순서

1. 대상 ChatGPT 계정이 있는 Android 기기에 0.3.0 APK를 설치합니다. 기존 APK와 서명이 다르면 삭제 전 필요한 PGN을 백업합니다. PGN에는 AI 해설 캐시가 포함되지 않습니다.
2. 홈 → AI 코치 연결 → Continue with ChatGPT. 알림 권한을 허용하면 브라우저 사용 중 로그인 알림을 볼 수 있습니다. 알림 권한 거절 자체를 로그인 실패로 처리하지는 않습니다.
3. 시스템 브라우저에서 자신의 계정으로 인증하고 워크스페이스와 구독 사용 권한을 승인합니다. 앱은 비밀번호·클라이언트 비밀·API 키를 받지 않습니다.
4. 브라우저의 로컬 콜백 완료 안내를 보고 앱으로 돌아옵니다. `계정 연결 및 모델 조회 완료`와 계정 표시를 확인합니다. 단순히 브라우저가 닫힌 것만으로 성공이라고 판단하지 않습니다.
5. 모델 조회·선택에서 자신의 계정이 지원하는 모델을 고릅니다. Continue with ChatGPT에서 구독 권한을 거절했다면 AI 요청이 차단돼야 합니다.
6. `연결 테스트 · AI 응답 받기` → `Responses 응답 완료` 및 `Token sharing works.`와 선택 모델을 확인합니다. 이 테스트도 구독 사용량을 사용합니다. 스트림 중간의 텍스트는 완료 증거가 아닙니다.
7. Stockfish와 e4 같은 한 수를 두고, 수의 평가와 AI 해설을 확인합니다. 모델을 변경한 뒤 같은 수의 해설을 요청해 선택 모델이 기록에 저장되는지 확인합니다.
8. 기록으로 이동해 해당 수의 엔진 분석·추천 화살표·모델별 해설을 확인합니다. 앱 종료 후 다시 열어 계정과 기록의 복원을 확인합니다.
9. 다른 계정을 추가하거나 기존 등록을 선택하면 모델 목록을 다시 조회해야 합니다. 같은 이메일이어도 등록(client ID)을 별도 항목으로 유지합니다.
10. Manage usage로 앱 사용 한도를 관리합니다. 한도 초과 시 새 요청을 중단해야 하며, 제한 조정 후 연결 테스트로 명시적으로 재시도합니다. 로그아웃 후에는 새 요청을 실행하지 않습니다.

## 구현에서 검증하는 경계

- 127.0.0.1만 listen하고 `/auth/callback`, 정확한 포트, state, 만료, 단일 시도를 검사합니다. 새 등록의 issued client ID를 토큰 교환 전에 보존하며 재인증은 기존 ID를 바꾸지 않습니다.
- S256 PKCE와 fresh nonce를 사용하고 Nimbus로 ID 토큰의 RS256/JWKS 서명·issuer·audience·시간·nonce·sub를 검증합니다. callback scope가 아닌 실제 token response scope로 구독 사용을 결정합니다.
- 토큰 전체는 Keystore AES-GCM + AtomicFile로 저장합니다. OS 백업, 로그, URL의 access/refresh token, Git, artifact에 넣지 않습니다. 선택 계정에서 갱신이 겹치지 않도록 직렬화하고 실패한 갱신으로 다른 등록을 덮어쓰지 않습니다.
- Responses는 공개 API만 사용합니다. store=false, stream=true, input 배열과 짧은 엔진 근거를 보내고 도구·온도·최대 토큰·서버 대화 상태 등 지원하지 않는 옵션을 추가하지 않습니다.
- `response.failed`, `response.incomplete`, 끊긴 스트림은 저장하지 않습니다. 한도 초과와 사용량 조회의 일시적인 실패는 구분하며 별도 과금으로 전환하지 않습니다.

## 검증의 범위

JVM 테스트는 임의 RSA 키로 유효·잘못된 서명을 만들고, PKCE·콜백·동의·SSE 완료·사용량 오류·갱신 응답을 검사합니다. Android instrumentation은 실제 Keystore/암호화 파일, SQLite/Stockfish와 foreground loopback의 위조 콜백 거부를 확인합니다. 모의 ID/토큰이나 위조 콜백은 실제 OpenAI 로그인 성공을 증명하지 않습니다.

클라우드 에뮬레이터는 Android 11/API 30 x86_64이며 하드웨어 가속이 없습니다. Android 14/15 이상의 foreground-service 정책과 실제 ARM64 기기의 Chrome·계정 동의·Responses 응답은 위 수동 절차로 확인해야 합니다. 개인 계정 로그인을 자동화하거나 개발 환경의 다른 계정 토큰을 이 앱으로 가져오지 않습니다. 앱은 OAuth 토큰 입력란을 제공하지 않습니다.

## 요약·상세 스트리밍 실기기 확인 (0.4.0)

1. 대국 또는 복기에서 미캐시 수를 선택하고, 완료 전에 짧은 조언의 문장이 점차 나타나는지 확인합니다. JSON 괄호·키·이스케이프가 그대로 노출되지 않아야 합니다.
2. 요약 아래 **자세히 보기**를 누릅니다. 같은 배경의 제목·본문·닫기 버튼과 마크다운 서식을 확인하고 상세 문장이 생성 중에도 표시되는지 확인합니다. 라이트/다크 모드 및 큰 글꼴에서 스크롤과 닫기 버튼을 확인합니다.
3. 상세를 다시 열거나 앱을 다시 시작하면 저장된 같은 수·모델의 요약과 상세를 추가 생성 없이 보여야 합니다. 모델을 바꾸면 다른 모델의 해설이 섞이지 않아야 합니다.
4. 생성 중 네트워크를 끊습니다. 부분 응답이 사라지고 실패 안내가 나와야 하며, 요약이 이미 저장돼 있었다면 그대로 남아야 합니다. 상세는 재시도할 수 있어야 합니다.
5. 생성 중 다른 수 선택·모델 변경·무르기를 실행합니다. 이전 수의 스트리밍 문장이 현재 수에 덮어써지지 않아야 합니다. 전체 복기 중 선택한 수의 미실행 요청은 대기 중인 일괄 요청보다 먼저 실행해야 합니다.

모의 SSE/캐시 검사는 실제 서비스의 응답 속도 측정을 대신하지 않습니다.

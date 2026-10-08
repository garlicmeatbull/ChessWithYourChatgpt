# 엔진 근거 중심의 해설과 토큰 절약

유사 사례는 Lichess의 [AccuracyCP](https://github.com/lichess-org/lila/blob/51263cc9a52f784f159f815cafab852fae1a542f/modules/analyse/src/main/AccuracyCP.scala)와 [AccuracyPercent](https://github.com/lichess-org/lila/blob/51263cc9a52f784f159f815cafab852fae1a542f/modules/analyse/src/main/AccuracyPercent.scala)를 확인했습니다. 이 구현은 엔진 평가를 착수한 쪽 관점으로 정렬하고 평가 변화로 수의 품질을 계산합니다. 이 프로젝트는 해당 코드를 복사하지 않고, Stockfish 점수를 항상 백 기준으로 정규화한 뒤 착수한 쪽의 손실을 계산하는 구조를 사용합니다. 고정 centipawn 경계에 따른 현재 라벨은 간단한 추정이며 Lichess 정확도와 같은 지표가 아닙니다.

모델에 체스 검색을 맡기지 않습니다. 기기에서 합법 수·Stockfish MultiPV·실전 수의 강제 검색을 계산하고 모델은 근거를 설명합니다. 한 요청은 착수 전/후 FEN, 실전 수, 상위 후보 최대 3개, 후보당 PV 최대 6반수, 평가·메이트·깊이, 최근 centipawn 추세 최대 6개로 제한합니다. PGN 전체·이미지·이전 해설·스킬 내용을 매번 전송하지 않습니다.

앱은 생성된 해설을 대국/반수/모델별로 SQLite에 저장해 같은 모델의 해설을 다시 볼 때 재호출하지 않습니다. 모델을 바꾸면 새 해설을 생성하고 이전 해설도 유지합니다. 직접 대국은 매 반수에, 가져온 대국은 전체 분석을 선택했을 때 수별 요청을 순서대로 보냅니다. 중단 시 완료된 분석·해설은 남으며 재실행은 저장된 결과를 재사용합니다.

Responses 요청은 `store:false`, `stream:true`이며 전체 PGN 대신 FEN 두 개·상위 후보 3개·6반수 이하 PV·짧은 평가 추세를 전송합니다. 기본 요청은 핵심 판단과 추천 행동 두 문장, 140자 이내의 `summary` JSON을 요구합니다. **자세히 보기**를 누른 경우에만 같은 분석 근거로 650자 이내의 `flow`, `bestMoveReason`, `plan` JSON을 추가 생성합니다. 기존 상세 설명은 추가 요청 없이 표시합니다. 요약과 상세를 모델별로 병합해 저장하고 두 요청의 usage도 각각 보존합니다. 에이전트 런타임·파일·셸·도구·이전 코딩 세션을 해설에 포함하지 않습니다. preview에서 허용하지 않는 `max_output_tokens`, `temperature`, `previous_response_id`도 전송하지 않습니다. 실제 완료 응답의 usage 객체를 기록에 남깁니다.

[공식 Sign in with ChatGPT 사례](https://developers.openai.com/cookbook/articles/sign-in-with-chatgpt)는 직접 모델 조회·Responses 스트리밍을 사용합니다. [Prompt Caching 101](https://github.com/openai/openai-cookbook/blob/main/examples/Prompt_Caching101.ipynb)을 참고해 고정 지침을 가변 엔진 자료 앞에 두되, 캐시를 맞추려고 짧은 요청을 인위적으로 늘리지 않습니다. 일반 API의 비용 할인과 ChatGPT 구독 사용량 절감을 동일시하지 않습니다.

## 응답 지연을 줄이는 방식 (0.4.0)

- SSE `response.output_text.delta`를 앱까지 전달합니다. JSON 구문과 미완성 이스케이프 대신 필드 안의 문장을 표시하고 화면 갱신은 최대 약 12.5회/초로 제한합니다. 완료를 기다리는 체감 지연을 줄이지만 서버의 첫 토큰 지연 자체를 제거하지는 않습니다.
- 짧은 요약을 먼저 요청하고 상세는 필요할 때 생성합니다. 초기 출력량을 줄이면서 Stockfish 후보·PV·FEN 자료와 사용자가 선택한 모델은 그대로 유지합니다. 상세 보기에는 별도의 요청 사용량과 대기가 발생합니다.
- 한 번에 한 AI 요청을 보내고, 사용자가 선택한 수의 요청을 대기 중인 전체 복기 요청보다 우선합니다. 이미 진행 중인 다른 수의 요청을 중간에 버리거나 병렬 요청으로 구독 사용량을 늘리지는 않습니다.
- 같은 수·모델의 완료된 요약과 상세는 SQLite 캐시로 재사용합니다. 생성 중 미완료 텍스트는 화면 미리보기이며 DB에 저장하지 않습니다. 모델 변경·무르기·화면 종료 후 도착한 오래된 미리보기는 현재 화면에 반영하지 않습니다.

모델을 더 작은 것으로 강제 교체하거나 Stockfish 탐색을 줄이지 않습니다. 지원 여부가 확인되지 않은 reasoning/출력 토큰 옵션도 추가하지 않습니다. 전체 대기 시간 및 설명 품질의 개선 비율은 실제 계정과 기기에서 동일한 국면·모델로 측정해야 하며 현재 수치로 보장하지 않습니다.

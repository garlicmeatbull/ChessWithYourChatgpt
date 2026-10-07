# 엔진 근거 중심의 해설과 토큰 절약

유사 사례는 Lichess의 [AccuracyCP](https://github.com/lichess-org/lila/blob/51263cc9a52f784f159f815cafab852fae1a542f/modules/analyse/src/main/AccuracyCP.scala)와 [AccuracyPercent](https://github.com/lichess-org/lila/blob/51263cc9a52f784f159f815cafab852fae1a542f/modules/analyse/src/main/AccuracyPercent.scala)를 확인했습니다. 이 구현은 엔진 평가를 착수한 쪽 관점으로 정렬하고 평가 변화로 수의 품질을 계산합니다. 이 프로젝트는 해당 코드를 복사하지 않고, Stockfish 점수를 항상 백 기준으로 정규화한 뒤 착수한 쪽의 손실을 계산하는 구조를 사용합니다. 고정 centipawn 경계에 따른 현재 라벨은 간단한 추정이며 Lichess 정확도와 같은 지표가 아닙니다.

모델에 체스 검색을 맡기지 않습니다. 기기에서 합법 수·Stockfish MultiPV·실전 수의 강제 검색을 계산하고 모델은 근거를 설명합니다. 한 요청은 착수 전/후 FEN, 실전 수, 상위 후보 최대 3개, 후보당 PV 최대 6반수, 평가·메이트·깊이, 최근 centipawn 추세 최대 6개로 제한합니다. PGN 전체·이미지·이전 해설·스킬 내용을 매번 전송하지 않습니다.

설명 서버는 모델+정규화된 근거+프롬프트 버전의 SHA-256을 키로 24시간/최대 128개 메모리 캐시를 사용합니다. 앱도 생성된 해설을 대국/반수별로 SQLite에 저장해 같은 모델의 해설을 다시 조회할 때 재호출하지 않습니다. 모델을 바꾸면 새 해설을 생성할 수 있습니다. 직접 대국은 매 반수에 해설을 요청하고, 가져온 대국도 전체 분석을 실행하면 연결된 서버에 수별 요청을 순서대로 보냅니다. 중단 시 완료된 분석·해설은 남으며 재실행은 저장된 결과를 재사용합니다.

Codex 실행은 새로운 stateless turn이며 `--ephemeral`, `--ignore-user-config`, 빈 작업 디렉터리를 사용합니다. 저장소의 Caveman 지침·파일·이전 코딩 세션을 체스 해설 문맥으로 넣지 않습니다. 웹 검색과 셸 도구를 비활성화하고 구조화된 짧은 JSON 응답을 요구합니다. SDK 응답의 `input_tokens`, `cached_input_tokens`, `output_tokens`는 반환 값과 저장 기록에 남습니다.

[OpenAI Prompt Caching 101](https://github.com/openai/openai-cookbook/blob/main/examples/Prompt_Caching101.ipynb)은 고정 지침을 앞에, 가변 데이터를 뒤에 두도록 안내합니다. 여기에도 그 순서를 적용했습니다. 다만 그 문서는 API의 캐싱 사례이고, Codex OAuth 계정에 동일한 비용 절감률을 보장하지 않습니다. 캐시를 맞추려고 짧은 요청을 1024토큰 이상으로 늘리지 않습니다. Codex 자체 시스템 프롬프트/에이전트 프로토콜의 비용은 존재하며, 위 JSON 길이가 실제 총 입력 토큰 수와 같지는 않습니다.

하이라이트는 큰 실수 목록을 그대로 재사용하지 않습니다. 7번째 반수 이후 작은 평가 손실(25~180cp)의 개선 가능성, 불리한 국면의 방어 자원, 합법적으로 확인한 체크 후보, 2순위와 차이가 있는 좋은 실전 결정을 찾습니다. 인접한 국면은 묶고 최대 6개를 고릅니다. 선정 이유와 수치·깊이를 저장합니다. 개인의 장기 실력이나 심리 상태를 아는 것처럼 평가하지 않습니다.

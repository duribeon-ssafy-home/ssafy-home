# AI Intent Router Guide

## 목적

`/api/ai/chat`는 하나의 진입점에서 사용자 질문을 받아 다음 Flow 중 하나로 보낸다.

- `PROPERTY_COMPARE`: 선택 매물 비교 분석
- `CONTRACT_KNOWLEDGE`: 전세/계약/부동산 용어 지식 RAG 답변
- `PROPERTY_RISK_EXPLAIN`: 선택 매물 위험도 설명
- `GENERAL_REAL_ESTATE`: 일반 부동산 LLM 답변

라우터의 핵심 원칙은 다음과 같다.

> 선택 매물이 있어도 순수 개념 질문이면 RAG/일반 답변으로 보내고, 선택 매물이 있으며 비교 표현이 있을 때만 비교 분석으로 보낸다.

## 현재 요청 구조

프론트는 `/api/ai/chat`에 다음 형태로 요청한다.

```json
{
  "message": "반전세가 뭐야?",
  "propertyIds": [1, 2]
}
```

- `message`: 사용자 질문
- `propertyIds`: 선택된 비교 매물 ID 목록. 없을 수도 있고, 1개 또는 2~4개일 수 있다.

`propertyIds`는 의도 판단에 참고하지만, 이것만으로 비교 의도를 확정하지 않는다.

## 라우팅 우선순위

현재 구현 위치:

- `backend/src/main/java/com/ssafy/home/ai/router/AiIntentRouter.java`
- `backend/src/main/java/com/ssafy/home/ai/service/AiOrchestratorService.java`

라우팅 순서는 다음과 같다.

1. 순수 전세/계약/용어 지식 질문이면 `CONTRACT_KNOWLEDGE`
2. 선택 매물이 2개 이상 있고 비교 표현이 있으면 `PROPERTY_COMPARE`
3. 선택 매물이 1개 이상 있고 위험도 설명 표현이 있으면 `PROPERTY_RISK_EXPLAIN`
4. 전세/계약 키워드가 있으면 `CONTRACT_KNOWLEDGE`
5. 나머지는 `GENERAL_REAL_ESTATE`

## 의도별 예시

### CONTRACT_KNOWLEDGE

선택 매물이 있어도 다음 질문은 지식 질문으로 처리한다.

```text
반전세가 뭐야?
전세랑 월세 차이 알려줘
확정일자가 무슨 뜻이야?
등기부등본은 뭐야?
보증보험이 뭔가?
```

판단 기준:

- 전세/계약 키워드가 있음
- `뭐야`, `뜻`, `의미`, `개념`, `정의`, `차이`, `알려줘` 같은 개념 질문 표현이 있음
- `매물`, `어디`, `추천`, `순서`, `높은`, `낮은`, `싼` 같은 비교 맥락 표현은 없음

### PROPERTY_COMPARE

선택 매물이 2개 이상 있고, 질문이 선택 매물 사이의 판단을 요구하면 비교 분석으로 보낸다.

```text
이 매물 두 개 비교해서 추천해줘
내 생활패턴 기준으로 어디가 제일 나아?
전세사기 위험이 낮은 순서로 알려줘
보증금이 싼데 위험 점수가 높은 매물을 설명해줘
이 중 가장 안전한 매물이 뭐야?
```

판단 기준:

- `propertyIds`가 2개 이상
- 비교 표현이 있음: `비교`, `추천`, `어디`, `어느`, `나아`, `순서`, `랭킹`, `가장`, `이 중`, `둘 중`, `매물`, `낮은`, `높은`, `싼`, `비싼`

### PROPERTY_RISK_EXPLAIN

선택 매물이 있고 위험도 자체의 이유를 묻는 질문은 위험도 설명 Flow로 보낸다.

```text
이 매물 위험도가 왜 높아?
위험 점수 이유가 뭐야?
주의로 나온 사유를 설명해줘
```

현재 `AiOrchestratorService`에서는 `PROPERTY_RISK_EXPLAIN`도 RAG 답변 경로를 사용한다. 특정 매물의 실제 위험도 데이터까지 함께 쓰려면 별도 Tool 연결이 추가로 필요하다.

### GENERAL_REAL_ESTATE

전세/계약 RAG 문서 근거가 꼭 필요하지 않은 일반 질문은 일반 LLM Flow로 보낸다.

```text
원룸 구할 때 뭐부터 보면 좋아?
서울에서 자취방 볼 때 체크할 점 알려줘
역세권 집의 장단점은 뭐야?
```

## RAG Flow 담당자에게 전달할 내용

전세/계약 지식 RAG 담당자는 Intent Router를 직접 구현할 필요가 없다. Router는 질문을 `CONTRACT_KNOWLEDGE`로 분류하고, Orchestrator가 RAG 서비스로 넘긴다.

현재 백엔드 연결 지점은 다음이다.

```java
if (intent == AiIntent.CONTRACT_KNOWLEDGE || intent == AiIntent.PROPERTY_RISK_EXPLAIN) {
    RagKnowledgeService.RagAnswer ragAnswer = ragKnowledgeService.answer(request.message());
    return verifierAgent.verify(intent, ragAnswer.answer(), ragAnswer.sources(), ragAnswer.warnings());
}
```

Python 기반 RAG를 붙일 경우 백엔드의 `RagKnowledgeService.answer(message)` 내부 구현만 교체하거나, 그 안에서 Python RAG API를 호출하면 된다.

RAG 담당자가 맞춰야 하는 응답 계약은 다음과 같다.

```json
{
  "answer": "사용자에게 보여줄 한국어 답변",
  "sources": [
    {
      "type": "RAG",
      "id": "문서 또는 청크 ID",
      "title": "근거 문서 제목"
    }
  ],
  "warnings": [
    "근거 부족, 법률 자문 아님 등 사용자에게 보여줄 주의 문구"
  ]
}
```

백엔드 DTO로는 `RagKnowledgeService.RagAnswer`에 대응한다.

- `answer`: 최종 답변 본문
- `sources`: `AiCompareSourceResponse` 목록. RAG 문서는 `type`을 `"RAG"`로 둔다.
- `warnings`: 검증/주의 문구. 없으면 빈 배열

## RAG 답변 작성 원칙

RAG Flow는 다음 원칙을 지켜야 한다.

- 한국어로 답한다.
- 검색된 문서 근거가 있는 내용만 구체적으로 말한다.
- 근거가 부족하면 모른다고 말하고 추가 확인이 필요하다고 안내한다.
- 법률/계약 안정성을 단정하지 않는다.
- 매물 비교 결과를 만들지 않는다. 비교 분석은 `PROPERTY_COMPARE` Flow의 책임이다.
- `propertyIds`가 요청에 있어도, `CONTRACT_KNOWLEDGE`로 들어온 질문은 용어/계약 지식 답변으로 처리한다.

## 주의할 경계 사례

다음 두 질문은 비슷해 보여도 의도가 다르다.

```text
반전세가 뭐야?
```

순수 개념 질문이므로 `CONTRACT_KNOWLEDGE`다.

```text
이 중 반전세 매물은 어디가 더 나아?
```

선택 매물 비교 질문이므로 `PROPERTY_COMPARE`다.

```text
보증금이 뭐야?
```

개념 질문이므로 `CONTRACT_KNOWLEDGE`다.

```text
보증금이 싼데 위험 점수가 높은 매물을 설명해줘
```

선택 매물 비교 맥락이므로 `PROPERTY_COMPARE`다.

## 테스트 기준

현재 라우팅 테스트는 다음 파일에 있다.

- `backend/src/test/java/com/ssafy/home/ai/AiChatApiTests.java`

주요 검증:

- 비교 표현 + 선택 매물 2개 이상 -> `PROPERTY_COMPARE`
- 선택 매물 2개 이상 + `반전세가 뭐야?` -> `CONTRACT_KNOWLEDGE`
- 계약 지식 질문 -> `CONTRACT_KNOWLEDGE`

라우터 키워드를 수정하면 위 테스트를 같이 갱신해야 한다.

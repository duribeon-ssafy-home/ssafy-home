# 라이프스타일 설문 처리 흐름

## 목표

`/survey`는 로그인 없이 접근할 수 있는 public 설문으로 유지한다. 사용자가 설문을 완료하면 프론트엔드는 답변을 백엔드로 보내 생활 성향 유형과 매물 필터 프리셋을 즉시 받는다.

- 로그인 사용자: 설문 결과를 즉시 DB에 저장한다.
- 비로그인 사용자: 저장하지 않고 결과만 미리보기로 보여준다.
- 비로그인 사용자가 결과 저장을 선택하면 로그인 후 기존 임시 결과를 저장한다.

## 설문 답변 요청 형식

프론트엔드는 질문 목록과 선택 상태를 아래 형식으로 변환해 전송한다. 사용자가 월세나 보증금 기준을 직접 입력한 경우 `monthlyRentMax`, `depositMax`를 만원 단위로 함께 보낸다.

```json
{
  "answers": [
    { "questionId": 1, "selectedOption": "A" },
    { "questionId": 2, "selectedOption": "B" },
    { "questionId": 3, "selectedOption": "A" },
    { "questionId": 4, "selectedOption": "B" },
    { "questionId": 5, "selectedOption": "A" },
    { "questionId": 6, "selectedOption": "B" }
  ],
  "monthlyRentMax": 65,
  "depositMax": 2000
}
```

백엔드는 다음 조건을 검증한다.

- `answers`는 정확히 6개여야 한다.
- `questionId`는 기존 생활 성향 질문 ID여야 한다.
- 같은 `questionId`가 중복되면 안 된다.
- `selectedOption`은 `A` 또는 `B`만 허용한다.
- `monthlyRentMax`, `depositMax`는 선택 값이며, 있으면 1 이상의 숫자여야 한다.

## API 흐름

### 질문 조회

```http
GET /api/lifestyle/questions
```

public API다. 설문 질문 6개와 A/B 선택지를 반환한다.

### 비로그인 결과 미리보기

```http
POST /api/lifestyle/results/preview
```

public API다. 답변을 분석해 `lifestyleType`, `typeName`, `filterPreset`을 반환하지만 DB에 저장하지 않는다.

### 로그인 결과 저장

```http
POST /api/lifestyle/results
Authorization: Bearer {accessToken}
```

인증 API다. 미리보기와 같은 분석 로직을 사용한 뒤 현재 로그인 사용자의 최신 생활 성향 결과로 저장한다.

### 내 최근 결과 조회

```http
GET /api/lifestyle/results/me
Authorization: Bearer {accessToken}
```

마이페이지에서 저장된 선호 유형을 보여줄 때 사용한다. 저장된 결과가 없으면 `LIFESTYLE_RESULT_NOT_FOUND`가 반환된다.

## 백엔드 계산 절차

설문 질문은 세 카테고리로 나뉜다.

| 카테고리 | 질문 ID | 의미 |
| --- | --- | --- |
| `LIVING_CONVENIENCE` | 1, 2 | 생활권과 편의시설을 얼마나 중요하게 보는지 |
| `COST_SENSITIVITY` | 3, 4 | 월세와 보증금 부담을 얼마나 줄이고 싶은지 |
| `HOME_QUALITY` | 5, 6 | 면적과 건물 상태를 얼마나 중요하게 보는지 |

각 카테고리는 `A` 선택 개수를 점수로 가진다. 유형 결정에는 점수의 크기보다 “해당 카테고리에서 A가 하나 이상 있는지”가 사용된다.

```text
livingConvenienceImportant = livingConvenienceScore > 0
costSensitive = costSensitivityScore > 0
homeQualityImportant = homeQualityScore > 0
```

세 boolean 조합으로 8개 생활 성향 유형 중 하나가 결정된다. `typeName`은 프론트 표시명과 동일하게 관리한다.

| 생활권 | 비용 | 집 품질 | lifestyleType | typeName |
| --- | --- | --- | --- | --- |
| true | true | true | `LIVING_COST_HOME_BALANCED` | 균형이 |
| true | true | false | `LIVING_COST_COMPACT` | 알뜰이 |
| true | false | true | `LIVING_FLEXIBLE_HOME` | 포근이 |
| true | false | false | `LIVING_FLEXIBLE_COMPACT` | 실용이 |
| false | true | true | `LOCATION_FLEXIBLE_COST_HOME` | 꼼꼼이 |
| false | true | false | `LOCATION_FLEXIBLE_COST_COMPACT` | 절약이 |
| false | false | true | `LOCATION_FLEXIBLE_HOME` | 공간이 |
| false | false | false | `LOCATION_FLEXIBLE_COMPACT` | 유연이 |

## 필터 프리셋 생성 규칙

`A` 선택 여부와 사용자가 직접 입력한 예산에 따라 매물 필터 프리셋이 만들어진다.

| 질문 | 조건 | 생성 필드 |
| --- | --- | --- |
| 1 또는 2 | `A` 선택 | `facilityScoreMin = 70` |
| 2 | `A` 선택 | `facilityCountMin = 20` |
| 3 | `A` 선택 | `monthlyRentMax = 50` |
| 4 | `A` 선택 | `depositMax = 1000` |
| 5 | `A` 선택 | `areaMin = 25` |
| 6 | `A` 선택 | `buildYearMin = 2016` |

사용자가 `monthlyRentMax` 또는 `depositMax`를 직접 입력하면 질문 3, 4의 기본값보다 사용자 입력값을 우선한다. 예산을 잘 모르겠다고 선택한 경우에는 값을 보내지 않으며 기존 A/B 기반 기본값을 사용한다.

값이 없는 필드는 응답 JSON에서 생략될 수 있다.

## 프론트 저장 상태

프론트는 결과 화면 표시와 로그인 후 저장 연결을 위해 `sessionStorage.lifestyleResult`에 임시 스냅샷을 저장한다.

```json
{
  "lifestyleType": "LIVING_COST_HOME_BALANCED",
  "typeName": "균형이",
  "filterPreset": {
    "facilityScoreMin": 70,
    "monthlyRentMax": 50,
    "areaMin": 25
  },
  "answers": [
    { "questionId": 1, "selectedOption": "A" }
  ],
  "budget": {
    "monthlyRentMax": 65
  },
  "saved": false,
  "savedAt": null
}
```

`saved`가 `false`이면 결과 페이지에서 임시 결과임을 안내하고 로그인 저장 CTA를 보여준다. 로그인 후 저장이 완료되면 같은 스냅샷을 `saved: true`로 갱신한다.

## 화면 흐름

1. 사용자가 `/survey`에서 6개 선택 질문과 선택 예산 입력을 완료한다.
2. 로그인 상태면 `POST /api/lifestyle/results`를 호출한다.
3. 비로그인 상태면 `POST /api/lifestyle/results/preview`를 호출한다.
4. 응답 결과, 답변 원본, 직접 입력한 예산을 sessionStorage에 저장하고 `/result`로 이동한다.
5. `/result`는 생활 성향 유형, 필터 프리셋, 저장 상태를 보여준다.
6. 비로그인 사용자가 “로그인하고 결과 저장하기”를 누르면 `/login?redirect=/result?saveLifestyle=1`로 이동한다.
7. 로그인 성공 후 `/result?saveLifestyle=1`로 돌아오면 sessionStorage의 답변과 예산 입력값을 `POST /api/lifestyle/results`로 저장한다.
8. `/my-page`는 `GET /api/users/me`와 `GET /api/lifestyle/results/me`를 조합해 내 정보와 저장된 선호 유형을 보여준다.

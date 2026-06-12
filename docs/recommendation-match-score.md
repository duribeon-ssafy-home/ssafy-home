# 추천 matchScore 계산 로직

## 목표

라이프스타일 추천 검색은 매물을 강제로 제거하는 필터와, 더 잘 맞는 매물을 위로 올리는 정렬 기준을 분리한다.

- 사용자 입력 조건: 지역, 보증금, 월세, 방 타입. `WHERE` 필터로 적용한다.
- 설문 기반 조건: `LifestyleResult`에 저장된 `filterPreset`. `WHERE`에 넣지 않고 `matchScore` 계산에만 사용한다.
- 정렬 기준: `matchScore DESC`, 동점이면 `createdAt DESC`.

예를 들어 사용자가 월세 조건을 80만 이하로 넓히면 월세 70만 매물도 후보에 남는다. 다만 설문 기준의 `monthlyRentMax = 50`을 만족하지 못하므로 점수는 낮아진다.

## API 흐름

```text
GET /api/recommendations
-> CurrentUser로 사용자 식별
-> 최신 LifestyleResult 조회
-> PropertySearchCondition으로 사용자 입력 조건만 DB 조회
-> 후보 매물별 AreaFacilityCount 조회
-> RecommendationScorer로 matchScore 계산
-> matchScore DESC, createdAt DESC 정렬
-> Page<PropertyResponse> 반환
```

관련 구현 파일:

- `backend/src/main/java/com/ssafy/home/recommendation/controller/RecommendationController.java`
- `backend/src/main/java/com/ssafy/home/recommendation/service/RecommendationService.java`
- `backend/src/main/java/com/ssafy/home/recommendation/service/RecommendationScorer.java`
- `backend/src/main/java/com/ssafy/home/recommendation/service/LifestyleRecommendationPolicy.java`
- `backend/src/main/java/com/ssafy/home/property/entity/AreaFacilityCount.java`

## 배점

활성 조건만 점수 계산에 참여한다. 활성 조건이란 `LifestyleResult`에 값이 존재하는 `filterPreset` 조건과 유형별 대표 방 타입이다.

| 조건 | 만족 기준 | 배점 |
| --- | --- | ---: |
| `facilityScoreMin` | `AreaFacilityCount.coverageScore()`가 기준 이상 | 15 |
| `facilityCountMin` | 동 단위 편의시설 총합이 기준 이상 | 20 |
| `monthlyRentMax` | 매물 월세가 기준 이하 | 15 |
| `depositMax` | 매물 보증금이 기준 이하 | 15 |
| `areaMin` | 매물 면적이 기준 이상 | 10 |
| `buildYearMin` | 매물 건축년도가 기준 이상 | 10 |
| 대표 방 타입 | 유형별 대표 `roomType`과 일치 | 15 |

계산식:

```text
matchScore = round(matchedWeight / activeWeight * 100)
```

값이 없는 조건은 `activeWeight`에 포함하지 않는다. 그래서 설문 결과에 `depositMax`가 없으면 보증금 조건은 점수에 영향을 주지 않는다.

## facilityScoreMin 처리

현재 DB에는 `facilityScoreMin`과 직접 비교할 수 있는 시설 점수 컬럼이 없다. 1차 구현에서는 `area_facility_counts`의 7개 시설 카테고리 커버리지를 점수로 환산한다.

대상 카테고리:

- 지하철
- 마트
- 편의점
- 병원
- 약국
- 카페
- 음식점

계산식:

```text
coverageScore = round(값이 1 이상인 시설 카테고리 수 / 7 * 100)
```

예시:

| 존재하는 시설 카테고리 수 | coverageScore | `facilityScoreMin = 70` 만족 여부 |
| ---: | ---: | --- |
| 5개 | 71 | 만족 |
| 4개 | 57 | 불만족 |

이 방식은 시설의 절대 개수는 `facilityCountMin`이 담당하고, 시설 종류의 다양성은 `facilityScoreMin`이 담당하도록 역할을 나눈다.

## 방 타입 1차 정책

현재 백엔드와 프론트는 단일 `roomType` 필터만 지원한다. 따라서 1차 구현에서는 유형별 추천 방 타입 목록 중 대표 방 타입 하나만 필터 기본값과 점수 조건에 사용한다.

| lifestyleType | 대표 roomType |
| --- | --- |
| `LIVING_COST_HOME_BALANCED` | `ONE_ROOM` |
| `LIVING_COST_COMPACT` | `ONE_ROOM` |
| `LIVING_FLEXIBLE_HOME` | `TWO_ROOM` |
| `LIVING_FLEXIBLE_COMPACT` | `ONE_ROOM` |
| `LOCATION_FLEXIBLE_COST_HOME` | `TWO_ROOM` |
| `LOCATION_FLEXIBLE_COST_COMPACT` | `ONE_ROOM` |
| `LOCATION_FLEXIBLE_HOME` | `TWO_ROOM` |
| `LOCATION_FLEXIBLE_COMPACT` | `ONE_ROOM` |

복수 방 타입 검색이 필요해지면 `roomTypes=ONE_ROOM,OFFICETEL` 형태의 API와 다중 선택 UI를 별도 개선으로 추가한다.

## 예시

`균형 잡힌 생활권 실속형` 사용자의 조건이 아래와 같다고 가정한다.

```text
facilityScoreMin = 70
facilityCountMin = 20
monthlyRentMax = 50
depositMax = 1000
areaMin = 25
buildYearMin = 2016
대표 roomType = ONE_ROOM
```

활성 배점 총합은 100점이다.

| 매물 | 만족 조건 | matchScore |
| --- | --- | ---: |
| A | 모든 조건 만족 | 100 |
| B | 보증금, 대표 방 타입만 만족 | 30 |
| C | 사용자 WHERE 조건 불만족 | 후보 제외 |

B 매물은 설문 조건을 대부분 만족하지 못해도 사용자 검색 조건을 만족한다면 결과에 남는다. 다만 `matchScore`가 낮아 A 매물보다 아래에 정렬된다.

## 프론트 표시

홈 화면은 로그인 사용자에게 최신 라이프스타일 결과가 있으면 `/api/recommendations`를 호출한다. 설문 결과가 없거나 비로그인 상태면 기존 `/api/properties`를 사용한다.

추천 모드에서는 필터바 아래 또는 추천 매물 영역에 아래 형태의 안내를 표시한다.

```text
생활권 중심 실속형 기준으로 편의시설 20개 이상, 월세 50만 이하 조건을 우선 반영 중입니다.
```

화면에 노출되는 필터는 지역, 보증금, 월세, 방 타입만 유지한다. 내부 점수 조건은 안내 칩으로만 설명한다.

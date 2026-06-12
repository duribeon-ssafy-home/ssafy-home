# 라이프스타일 추천 검색 기준

## 표시명 관리

`lifestyleType` enum 값은 API와 DB에서 사용하는 안정적인 식별자다. 사용자에게 보여주는 이름은 `typeName`이며, 프론트의 생활 성향 메타데이터와 같은 문구를 사용한다.

| lifestyleType | typeName |
| --- | --- |
| `LIVING_COST_HOME_BALANCED` | 균형 잡힌 생활권 실속형 |
| `LIVING_COST_COMPACT` | 생활권 중심 실속형 |
| `LIVING_FLEXIBLE_HOME` | 편안한 공간 우선형 |
| `LIVING_FLEXIBLE_COMPACT` | 생활권 중심 실용형 |
| `LOCATION_FLEXIBLE_COST_HOME` | 조건 꼼꼼 공간형 |
| `LOCATION_FLEXIBLE_COST_COMPACT` | 비용 절약 실속형 |
| `LOCATION_FLEXIBLE_HOME` | 집 자체 만족형 |
| `LOCATION_FLEXIBLE_COMPACT` | 조건 유연 탐색형 |

새 화면이나 문서를 추가할 때는 위 이름을 기준으로 맞춘다. 백엔드 `LifestyleType.typeName`, 프론트 `lifestyleTypeMeta.typeName`, 문서 예시는 서로 다른 이름을 쓰면 안 된다.

## 추천 검색 전략

추천 검색은 사용자 입력 조건과 설문 기반 선호 조건을 분리한다.

- 사용자 입력 조건: 지역, 보증금, 월세, 방 타입처럼 사용자가 직접 고른 값이며 `WHERE` 필터로 적용한다.
- 설문 기반 조건: 저장된 `filterPreset`에서 나온 값이며 매물 제외 조건으로 쓰지 않고 `matchScore` 계산에만 사용한다.
- 정렬 기준: `matchScore DESC`, 동점이면 `createdAt DESC`.

초기 `matchScore`는 활성화된 `filterPreset` 조건과 유형별 대표 방 타입을 대상으로 계산한다. 예를 들어 `depositMax`가 없는 사용자에게 보증금 조건 불일치 페널티를 주지 않는다.

초기 배점 후보:

| 조건 | 기준 | 기본 배점 |
| --- | --- | --- |
| `facilityScoreMin` | 동 단위 시설 카테고리 커버리지 점수가 기준 이상 | 15 |
| `facilityCountMin` | 동 단위 편의시설 총합이 기준 이상 | 20 |
| `monthlyRentMax` | 월세가 기준 이하 | 15 |
| `depositMax` | 보증금이 기준 이하 | 15 |
| `areaMin` | 면적이 기준 이상 | 10 |
| `buildYearMin` | 건축년도가 기준 이상 | 10 |
| 대표 방 타입 | 유형별 대표 방 타입과 일치 | 15 |

`facilityScoreMin`은 현재 DB에 직접 점수 컬럼이 없다. 1차 구현에서는 `AreaFacilityCount`의 7개 시설 카테고리 중 값이 1개 이상인 카테고리 비율을 100점으로 환산한 `coverageScore`를 사용한다.

```text
coverageScore = round(값이 1 이상인 시설 카테고리 수 / 7 * 100)
```

예를 들어 7개 중 5개 카테고리에 시설이 있으면 `coverageScore = 71`이므로 `facilityScoreMin = 70`을 만족한다.

최종 점수는 활성 조건의 총 배점 대비 획득 점수를 100점 만점으로 정규화한다. 활성 조건은 값이 존재하는 `filterPreset` 조건과 유형별 대표 방 타입이다.

```text
matchScore = round(matchedWeight / activeWeight * 100)
```

활성 조건이 하나도 없으면 `matchScore = 0`으로 두고 최신순 정렬만 적용한다.

## 방 타입 1차 정책

1차 구현에서는 기존 단일 `roomType` 필터 구조를 유지한다. `roomTypes=ONE_ROOM,OFFICETEL` 같은 복수 파라미터와 다중 선택 UI는 이번 범위에 포함하지 않는다.

- 유형별 추천 방 타입이 여러 개일 수 있어도, 화면 필터 기본값과 `WHERE` 조건에는 대표 방 타입 하나만 사용한다.
- 사용자가 방 타입을 직접 변경하면 변경한 단일 값이 `WHERE` 필터로 적용된다.
- 설문 기반 내부 조건은 사용자가 선택한 방 타입 후보군 안에서 `matchScore` 계산에만 사용한다.
- 복수 추천 방 타입을 동시에 후보군으로 조회하는 기능은 후속 개선 과제로 둔다.

1차 대표 방 타입은 프론트 메타데이터의 `recommendedRoomTypes` 첫 번째 값을 기준으로 백엔드 정책에 명시한다.

## 후속 구현 규칙

- `filterPreset` 조건을 추천 API의 강제 필터로 사용하지 않는다.
- 가격 값은 프로젝트 공통 기준인 만원 단위로 비교한다.
- 대표 추천 방 타입 기준은 백엔드에도 명시적으로 둔다. 프론트 전용 메타데이터에만 의존하지 않는다.
- 카드별 추천 이유가 필요해지면 `matchedConditions` 같은 응답 필드를 별도로 추가한다.

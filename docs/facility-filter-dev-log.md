# 시설 필터 & 상세 조회 facilityInfo 개발 로그

## 1. 서비스 설계 고민

### Issue #17 분석 및 역할 분리

A담당(설문혁)이 작성한 Issue #17은 `GET /api/properties`에 시설 관련 필터(`facilityCountMin`, `facilityScoreMin`)와 정렬 옵션을 추가하는 내용이었다.
우리가 이미 구현한 추천 API(`GET /api/recommendations`)와 상당 부분 겹쳤지만, **사용자 의도**가 달랐다.

| API | 사용 목적 | 필터 주체 |
|---|---|---|
| `GET /api/recommendations` | 성향 테스트 결과 기반 자동 추천 | 시스템(LifestyleResult) |
| `GET /api/properties` | 사용자가 직접 조건을 설정하는 검색 | 사용자 |

두 API는 공존해야 하며, `area_facility_counts` 테이블을 공유하는 것이 DB 중복을 막는 방법이라 판단했다.
A담당이 원했던 `dong_facility_stats` 테이블 및 `facility_score` 가중 점수 방식은 채택하지 않았다.

### facility_score 제외 결정

가중 점수 방식을 제외한 이유:
- 어떤 가중치를 줄지 명확한 기준이 없음
- `COALESCE(subway_count_500m, 0) * 가중치` 식의 계산은 주관적이며 변경이 어렵다
- `facilityCountMin=20` (시설 20개 이상) 방식이 사용자 입장에서 더 직관적
- 개별 카테고리 컬럼이 이미 있어 필요 시 특정 시설 기준 필터로 확장 가능

### facilityInfo 응답 포함 위치 결정

처음에는 `GET /api/properties` 목록 응답에도 `facilityInfo`를 포함하는 방안을 검토했으나 아래 이유로 제외했다.

- 일반 검색은 "지역을 이미 아는 사용자"나 "예산/크기 조건 기반 검색" 용도
- 동 단위 데이터라 같은 동 내 매물은 facilityInfo가 전부 동일 → 목록에 노출 실익 없음
- 목록 카드 UI에 표시할 정보가 아님

결론:
- **`GET /api/properties` 목록**: `facilityCountMin` 파라미터가 있을 때만 facilityInfo 포함
- **`GET /api/properties/{id}` 상세**: 항상 facilityInfo 포함 (상세 페이지에서 "이 동네 지하철 3개, 마트 2개" 형태로 노출)

---

## 2. 트러블슈팅

### facilityCountMin 필터 동작 안 함

**증상**
`facilityCountMin=2`로 요청해도 `totalElements: 0` 반환. 응답 딜레이도 거의 없음.

**원인 추적**
`show-sql: true` 로그 확인:

```sql
-- Hibernate가 생성한 쿼리
SELECT ... FROM area_facility_counts afc1_0
WHERE (coalesce(afc1_0.subway_count500m, 0) + ...) >= ?

-- 이후 properties 쿼리
WHERE p1_0.status <> ? AND 1<>1
```

`1<>1`은 `PropertySpecification.inAreas()`에서 빈 리스트가 들어올 때 반환하는 `cb.disjunction()` (항상 false 조건)이다.
즉, `findWithMinTotalCount(2)`가 빈 리스트를 반환하고 있었다.

**근본 원인: Hibernate 네이밍 전략과 컬럼명 불일치**

entity 필드명 `subwayCount500m`을 Hibernate의 `SpringPhysicalNamingStrategy`가 변환하면:
- 변환 규칙: 대문자 앞에만 언더스코어 삽입
- `subwayCount500m` → `subway_count500m` (숫자 앞 언더스코어 없음)

실제 schema.sql의 컬럼명:
- `subway_count_500m` (숫자 앞 언더스코어 있음)

`ddl-auto: update`로 인해 앱 시작 시 `subway_count500m`이라는 빈 컬럼이 자동 생성됐고,
실제 데이터가 있는 `subway_count_500m`은 읽지 못해 COALESCE가 전부 0을 반환했다.

**수정**

`AreaFacilityCount` entity에 `@Column(name = "...")` 명시:

```java
@Column(name = "subway_count_500m")
private Integer subwayCount500m;

@Column(name = "mart_count_1km")
private Integer martCount1km;

// ... 나머지 6개 동일하게 적용
```

그리고 Hibernate가 만든 빈 컬럼들을 DB에서 제거:

```sql
ALTER TABLE area_facility_counts
    DROP COLUMN subway_count500m,
    DROP COLUMN mart_count1km,
    DROP COLUMN convenience_count500m,
    DROP COLUMN hospital_count1km,
    DROP COLUMN pharmacy_count500m,
    DROP COLUMN cafe_count500m,
    DROP COLUMN restaurant_count500m;
```

---

## 3. 구현 시퀀스

### STEP 1. `PropertySearchCondition.java` — 필터 파라미터 추가

```java
public record PropertySearchCondition(
        String sido,
        String gugun,
        String dong,
        RentType rentType,
        RoomType roomType,
        Long minDeposit,
        Long maxDeposit,
        Integer minMonthlyRent,
        Integer maxMonthlyRent,
        BigDecimal minArea,
        BigDecimal maxArea,
        Integer facilityCountMin   // 추가
) {}
```

`@ParameterObject`로 컨트롤러에 바인딩되기 때문에 컨트롤러 수정 없이 Swagger에 자동 노출된다.

---

### STEP 2. `PropertyService.java` — 필터 로직 및 상세 조회 수정

**`getProperties()` 수정**

`facilityCountMin`이 있을 때만 qualifying 동 목록으로 추가 필터링하고 facilityInfo를 응답에 포함한다.

```java
public Page<PropertyResponse> getProperties(PropertySearchCondition condition, Pageable pageable) {
    Specification<Property> spec = PropertySpecification.search(condition);

    if (condition.facilityCountMin() != null) {
        List<AreaFacilityCount> qualifyingAreas =
                areaFacilityCountRepository.findWithMinTotalCount(condition.facilityCountMin());
        spec = spec.and(PropertySpecification.inAreas(qualifyingAreas));
    }

    Page<Property> page = propertyRepository.findAll(spec, pageable);

    if (condition.facilityCountMin() != null) {
        Map<String, AreaFacilityCount> facilityMap = page.getContent().stream()
                .collect(Collectors.toMap(
                        p -> p.getSido() + "|" + p.getGugun() + "|" + p.getDong(),
                        p -> areaFacilityCountRepository
                                .findBySidoAndGugunAndDong(p.getSido(), p.getGugun(), p.getDong())
                                .orElse(null),
                        (existing, duplicate) -> existing
                ));
        return page.map(p -> PropertyResponse.from(p,
                facilityMap.get(p.getSido() + "|" + p.getGugun() + "|" + p.getDong())));
    }

    return page.map(PropertyResponse::from);
}
```

**`getProperty()` 수정**

상세 조회 시 항상 해당 동의 시설 정보를 붙인다. 데이터가 없으면 null → `@JsonInclude(NON_NULL)`에 의해 응답에서 제외된다.

```java
public PropertyResponse getProperty(Long id) {
    Property property = findActiveProperty(id);
    AreaFacilityCount area = areaFacilityCountRepository
            .findBySidoAndGugunAndDong(property.getSido(), property.getGugun(), property.getDong())
            .orElse(null);
    return PropertyResponse.from(property, area);
}
```

---

### STEP 3. `AreaFacilityCount.java` — @Column 명시 (버그 수정)

```java
@Column(name = "subway_count_500m")
private Integer subwayCount500m;

@Column(name = "mart_count_1km")
private Integer martCount1km;

@Column(name = "convenience_count_500m")
private Integer convenienceCount500m;

@Column(name = "hospital_count_1km")
private Integer hospitalCount1km;

@Column(name = "pharmacy_count_500m")
private Integer pharmacyCount500m;

@Column(name = "cafe_count_500m")
private Integer cafeCount500m;

@Column(name = "restaurant_count_500m")
private Integer restaurantCount500m;
```

`@Column` 없이 Hibernate 자동 변환에 맡기면 `subwayCount500m` → `subway_count500m`으로 변환되어 실제 컬럼명 `subway_count_500m`과 불일치한다. `ddl-auto: update`와 조합 시 빈 컬럼이 자동 생성되는 사이드 이펙트가 발생하므로 숫자가 포함된 컬럼명은 반드시 명시한다.

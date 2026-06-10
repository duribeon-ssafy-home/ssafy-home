# 추천 API 구현 가이드

자취 MBTI 기반 매물 추천 기능의 전체 흐름과 각 파일의 역할을 단계별로 설명합니다.

---

## 전체 흐름 한눈에 보기

```
[추천 API]  사용자가 GET /api/recommendations 호출
  │
  ▼
[RecommendationController]
  JWT 토큰에서 userId 추출 → RecommendationService 호출
  │
  ▼
[RecommendationService] ← 핵심 로직
  1. 사용자의 LifestyleResult 조회 (A담당이 이미 저장해둔 값)
  2. LifestyleResult에서 필터 조건 꺼내기
     - monthlyRentMax, depositMax, areaMin, buildYearMin (properties 테이블)
     - facilityCountMin (area_facility_counts 테이블)
  3. 시설 조건이 있으면 → 조건을 만족하는 동(dong) 목록 조회
  4. properties 테이블에서 필터 적용한 매물 조회
  5. 결과 매물 각각에 facilityInfo 붙이기
  │
  ▼
[PropertyResponse + FacilityInfo]
  JSON으로 변환 → 응답 반환

[일반 검색 API]  사용자가 GET /api/properties?facilityCountMin=20 호출
  │
  ▼
[PropertyController → PropertyService]
  1. PropertySearchCondition에서 facilityCountMin 꺼내기
  2. facilityCountMin 있으면 → 동일한 방식으로 동 목록 조회
  3. properties 테이블에서 필터 적용한 매물 조회
  4. facilityCountMin 있을 때만 facilityInfo 붙이기
  (facilityCountMin 없으면 facilityInfo = null → JSON에서 키 자체 제외)

[상세 조회]  GET /api/properties/{id}
  → PropertyService.getProperty()가 항상 facilityInfo를 포함해서 반환
```

---

## STEP 1. AreaFacilityCount 엔티티

**파일**: `property/entity/AreaFacilityCount.java`

### 이게 왜 필요한가?

전처리 스크립트(`add_facility_counts.py`)가 `area_facility_counts` 테이블에 동별 시설 수를 채워넣습니다.  
Java에서 이 테이블을 읽으려면 테이블과 1:1로 대응하는 **엔티티 클래스**가 필요합니다.

### 코드 뜯어보기

```java
@Entity                          // "이 클래스는 DB 테이블과 연결된다"
@Table(name = "area_facility_counts")  // 연결할 테이블 이름 명시
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA는 기본생성자가 필요 (외부에서 직접 new 못하게 protected)
public class AreaFacilityCount {
```

> **왜 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`인가?**  
> JPA는 DB에서 데이터를 꺼낼 때 내부적으로 기본 생성자를 사용합니다.  
> 그런데 외부에서 `new AreaFacilityCount()`로 빈 객체를 만드는 건 의미가 없으니 `PROTECTED`로 막습니다.  
> (이 패턴은 이미 `Property` 엔티티에서도 쓰고 있습니다.)

---

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long areaId;
```

> `@Id` → 이 필드가 PK(기본키)임을 선언  
> `@GeneratedValue(IDENTITY)` → DB의 AUTO_INCREMENT를 사용 (MySQL)

---

```java
private Integer subwayCount500m;
private Integer martCount1km;
// ... 나머지 6개
```

> `Integer`(대문자)를 쓰는 이유: DB 컬럼이 `NULL`일 수 있기 때문입니다.  
> `int`(소문자)는 null을 표현할 수 없고, `Integer`는 null 허용.

---

```java
public int totalCount() {
    return coalesce(subwayCount500m)
            + coalesce(martCount1km)
            + ... ;
}

private int coalesce(Integer value) {
    return value != null ? value : 0;
}
```

> 7개 카테고리 수를 전부 더하는 편의 메서드.  
> null인 항목은 0으로 처리 (SQL의 `COALESCE`와 같은 역할).  
> 나중에 "이 동네의 총 시설 수가 20개 이상인가?" 체크할 때 씁니다.

---

## STEP 2. AreaFacilityCountRepository

**파일**: `property/repository/AreaFacilityCountRepository.java`

### Repository란?

DB에 SQL을 보내고 결과를 받아오는 역할.  
Spring Data JPA는 인터페이스만 선언해도 **자동으로 구현체를 만들어줍니다.**

```java
public interface AreaFacilityCountRepository extends JpaRepository<AreaFacilityCount, Long> {
```

> `JpaRepository<엔티티타입, PK타입>`을 상속하면  
> `findById()`, `findAll()`, `save()`, `delete()` 등 기본 CRUD가 자동으로 생깁니다.

---

### 메서드 1: 동 이름으로 단건 조회

```java
Optional<AreaFacilityCount> findBySidoAndGugunAndDong(String sido, String gugun, String dong);
```

> **메서드 이름 규칙**: `findBy필드명And필드명And필드명`  
> Spring Data JPA가 이름을 분석해서 자동으로 아래 SQL을 만들어줍니다.
> ```sql
> SELECT * FROM area_facility_counts
> WHERE sido = ? AND gugun = ? AND dong = ?
> ```
> `Optional`로 감싸는 이유: 해당 동 데이터가 없을 수도 있어서 null 대신 Optional 사용.

---

### 메서드 2: 총 시설 수 기준으로 동 목록 조회

```java
@Query("""
        SELECT a FROM AreaFacilityCount a
        WHERE (COALESCE(a.subwayCount500m, 0)
             + COALESCE(a.martCount1km, 0)
             + ... ) >= :minCount
        """)
List<AreaFacilityCount> findWithMinTotalCount(@Param("minCount") int minCount);
```

> 이건 이름으로 자동 생성이 불가능한 복잡한 쿼리라 `@Query`로 직접 작성.  
> `COALESCE(값, 0)` → null이면 0으로 대체 (SQL 함수).  
> `:minCount` → `@Param("minCount")`로 전달된 값이 들어가는 자리.
>
> **언제 쓰이나?** 사용자가 "생활시설이 중요하다(Q2=A)"고 답했을 때,  
> `facilityCountMin = 20`이 LifestyleResult에 저장됩니다.  
> 이 메서드로 "총 시설 수 ≥ 20인 동 목록"을 가져와서 그 동에 있는 매물만 필터합니다.

---

## STEP 3. FacilityInfo DTO

**파일**: `property/dto/FacilityInfo.java`

### DTO란?

**D**ata **T**ransfer **O**bject — 계층 간 데이터를 전달하는 객체.  
여기서는 `AreaFacilityCount` 엔티티의 데이터를 **API 응답 JSON**으로 변환하는 역할.

### 왜 엔티티를 그대로 쓰지 않고 DTO를 만드나?

엔티티에는 `areaId`, `centerLat`, `centerLng`, `calculatedAt` 같이  
**사용자에게 보여줄 필요 없는 내부 데이터**가 있습니다.  
DTO는 **실제로 응답에 포함할 필드만** 골라서 담습니다.

```java
@JsonInclude(JsonInclude.Include.NON_NULL)  // null인 필드는 JSON에서 제외
public record FacilityInfo(
        Integer subwayCount500m,
        Integer martCount1km,
        // ...
) {
    public static FacilityInfo from(AreaFacilityCount area) {
        if (area == null) return null;  // 시설 데이터 없는 동이면 null 반환
        return new FacilityInfo(
                area.getSubwayCount500m(),
                // ...
        );
    }
}
```

> **`record`란?**  
> Java 16+에서 추가된 불변(immutable) 데이터 클래스.  
> 필드, 생성자, getter, equals, hashCode를 자동 생성.  
> DTO처럼 "데이터를 담는 그릇"에 딱 맞습니다.

---

## STEP 4. PropertyResponse 변경

**파일**: `property/dto/PropertyResponse.java`

### 변경 내용

기존 `PropertyResponse`에 `FacilityInfo facilityInfo` 필드를 추가하고,  
`from()` 메서드를 오버로드(같은 이름, 다른 파라미터)했습니다.

```java
// 기존 API (매물 목록/상세) — facilityInfo = null
public static PropertyResponse from(Property property) {
    return from(property, null);  // 아래 메서드 호출
}

// 추천 API — facilityInfo 있음
public static PropertyResponse from(Property property, AreaFacilityCount area) {
    return new PropertyResponse(
            // ... 기존 필드들 ...
            FacilityInfo.from(area)  // null이면 FacilityInfo도 null
    );
}
```

> **왜 오버로드를 썼나?**  
> 기존의 `from(Property)`를 쓰는 코드(`PropertyService`, `FavoriteService` 등)를  
> 전혀 건드리지 않아도 됩니다.  
> 추천 API에서만 두 번째 메서드를 사용하면 됩니다.
>
> `@JsonInclude(JsonInclude.Include.NON_NULL)` → facilityInfo가 null이면  
> JSON 응답에 `"facilityInfo"` 키 자체가 안 나옵니다.

---

## STEP 5. PropertySpecification

**파일**: `property/repository/PropertySpecification.java`

### Specification이란?

JPA에서 **동적 WHERE 조건**을 만드는 방법.  
"이 조건, 저 조건을 AND/OR로 조합해서 쿼리를 만들어줘"라고 선언적으로 작성할 수 있습니다.

이 클래스는 두 개의 static 메서드를 갖습니다.

---

### 메서드 1: search() — 일반 검색 조건

```java
public static Specification<Property> search(PropertySearchCondition condition) {
    return (root, query, cb) -> {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("status"), PropertyStatus.APPROVED));
        // sido, gugun, dong, rentType, roomType, 가격 범위, 면적 범위 등 조건 추가
        return cb.and(predicates.toArray(new Predicate[0]));
    };
}
```

> `GET /api/properties`의 일반 검색에서 사용합니다.  
> 조건이 null이면 해당 WHERE 절은 추가하지 않습니다 (동적 쿼리).

---

### 메서드 2: inAreas() — 동(dong) 범위 필터

```java
public static Specification<Property> inAreas(List<AreaFacilityCount> areas) {
    return (root, query, cb) -> {
        if (areas.isEmpty()) return cb.disjunction();  // 빈 리스트면 false → 결과 0건
        List<Predicate> predicates = areas.stream()
                .map(a -> cb.and(
                        cb.equal(root.get("sido"), a.getSido()),
                        cb.equal(root.get("gugun"), a.getGugun()),
                        cb.equal(root.get("dong"), a.getDong())
                ))
                .toList();
        return cb.or(predicates.toArray(new Predicate[0]));
    };
}
```

> **`(root, query, cb) -> { ... }`** 람다식이 실제 SQL WHERE절을 만드는 부분입니다.
> - `root` → FROM 절의 테이블 (properties)
> - `cb` → 조건 빌더 (CriteriaBuilder)
>
> **생성되는 SQL 예시** (areas에 하단동, 서면동이 있을 때):
> ```sql
> WHERE (sido='부산광역시' AND gugun='사하구' AND dong='하단동')
>    OR (sido='부산광역시' AND gugun='부산진구' AND dong='서면동')
> ```
>
> **`cb.disjunction()`** → 항상 false인 조건. areas가 비어있으면 매물을 하나도 반환하지 않습니다.  
> (시설 조건을 만족하는 동이 없다면 추천 결과도 없어야 하므로)
>
> 추천 API(`RecommendationService`)와 일반 검색(`PropertyService`) 두 곳에서 모두 사용합니다.

---

## STEP 6. RecommendationService (핵심)

**파일**: `recommendation/service/RecommendationService.java`

서비스는 비즈니스 로직의 중심입니다. 4단계로 동작합니다.

### 단계 1: LifestyleResult 조회

```java
LifestyleResult lifestyle = lifestyleResultRepository
        .findTopByUserIdOrderByCreatedAtDescIdDesc(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.LIFESTYLE_RESULT_NOT_FOUND));
```

> A담당이 이미 구현한 `LifestyleResultRepository`의 메서드를 그대로 씁니다.  
> 해당 사용자의 **가장 최근** 성향 테스트 결과를 가져옵니다.  
> 결과가 없으면 `BusinessException` → 클라이언트에 404 응답.

---

### 단계 2: 기본 필터 Specification 구성

```java
private Specification<Property> buildSpec(LifestyleResult lifestyle) {
    return (root, query, cb) -> {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(root.get("status"), PropertyStatus.APPROVED));

        if (lifestyle.getMonthlyRentMax() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("monthlyRent"), lifestyle.getMonthlyRentMax()));
        }
        // depositMax, areaMin, buildYearMin도 동일한 패턴
```

> LifestyleResult에 저장된 값들을 꺼내서 SQL WHERE 조건으로 변환합니다.
>
> | LifestyleResult 필드 | SQL 조건 |
> |---|---|
> | `monthlyRentMax = 50` | `monthly_rent <= 50` |
> | `depositMax = 1000` | `deposit <= 1000` |
> | `areaMin = 25.0` | `area >= 25.0` |
> | `buildYearMin = 2016` | `build_year >= 2016` |
>
> 값이 null이면 (사용자가 해당 조건을 중요하지 않다고 답했으면) 조건을 추가하지 않습니다.

---

### 단계 3: 시설 조건 추가

```java
if (lifestyle.getFacilityCountMin() != null) {
    List<AreaFacilityCount> qualifyingAreas =
            areaFacilityCountRepository.findWithMinTotalCount(lifestyle.getFacilityCountMin());
    spec = spec.and(PropertySpecification.inAreas(qualifyingAreas));
}
```

> `facilityCountMin`이 있다는 건 사용자가 "생활시설 중요(Q2=A)"를 선택했다는 뜻.
>
> ```
> findWithMinTotalCount(20)
>   → area_facility_counts에서 시설 합계 ≥ 20인 동 목록 조회
>   → [하단동, 서면동, 남포동, ...]
>
> inAreas([하단동, 서면동, ...])
>   → properties WHERE (sido='...' AND gugun='...' AND dong='하단동') OR ...
> ```
>
> `spec.and(...)` → 기존 조건에 AND로 추가.

---

### 단계 4: 매물 조회 + facilityInfo 붙이기

```java
Page<Property> page = propertyRepository.findAll(spec, pageable);

Map<String, AreaFacilityCount> facilityMap = buildFacilityMap(page.getContent());

return page.map(p -> PropertyResponse.from(p,
        facilityMap.get(areaKey(p.getSido(), p.getGugun(), p.getDong()))));
```

> **왜 facilityMap을 쓰나?**  
> 결과 매물 20개가 있을 때, 같은 동에 속한 매물들은 동일한 facilityInfo를 공유합니다.  
> 동마다 한 번씩만 DB를 조회하고 Map에 캐시해두는 방식입니다.
>
> ```
> areaKey("부산광역시", "사하구", "하단동") → "부산광역시|사하구|하단동"
>
> facilityMap = {
>   "부산광역시|사하구|하단동" → AreaFacilityCount(subway=2, mart=1, ...),
>   "부산광역시|부산진구|서면동" → AreaFacilityCount(subway=5, mart=3, ...),
> }
> ```
>
> `page.map(...)` → Page 안의 각 Property를 PropertyResponse로 변환.  
> 페이지네이션 정보(총 건수, 현재 페이지 등)는 자동으로 유지됩니다.

---

## STEP 7. RecommendationController

**파일**: `recommendation/controller/RecommendationController.java`

```java
@GetMapping
public ResponseEntity<ApiResponse<Page<PropertyResponse>>> recommend(
        @CurrentUser Long userId,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
) {
    Page<PropertyResponse> result = recommendationService.recommend(userId, pageable);
    return ResponseEntity.ok(ApiResponse.success("추천 매물 조회에 성공했습니다.", result));
}
```

> **`@CurrentUser Long userId`**  
> JWT 필터가 토큰을 파싱해서 SecurityContext에 저장해둔 userId를  
> `CurrentUserArgumentResolver`가 꺼내서 주입해줍니다.  
> 컨트롤러에서 직접 JWT를 파싱하지 않아도 됩니다.
>
> **`@PageableDefault`**  
> 클라이언트가 페이지 파라미터를 안 보내면 적용되는 기본값:
> - `size = 20` → 한 페이지에 20개
> - `sort = "createdAt", direction = DESC` → 최신순 정렬
>
> 클라이언트가 `?page=1&size=10&sort=monthlyRent,asc`처럼 직접 지정할 수도 있습니다.

---

## 최종 요청/응답 예시

**요청:**
```
GET /api/recommendations
Authorization: Bearer {JWT토큰}
```

**응답:**
```json
{
  "success": true,
  "message": "추천 매물 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "propertyId": 42,
        "address": "부산광역시 사하구 하단동 ...",
        "monthlyRent": 40,
        "deposit": 500,
        "area": 28.5,
        "buildYear": 2019,
        "facilityInfo": {
          "subwayCount500m": 2,
          "martCount1km": 1,
          "convenienceCount500m": 4,
          "hospitalCount1km": 3,
          "pharmacyCount500m": 2,
          "cafeCount500m": 7,
          "restaurantCount500m": 15
        }
      }
    ],
    "totalElements": 142,
    "totalPages": 8,
    "number": 0,
    "size": 20
  }
}
```

> - `GET /api/properties` 일반 목록 조회: `facilityCountMin` 파라미터가 **없으면** facilityInfo 키 자체가 JSON에 나오지 않음, **있으면** facilityInfo 포함
> - `GET /api/properties/{id}` 상세 조회: **항상** facilityInfo 포함
> - `GET /api/recommendations` 추천 API: **항상** facilityInfo 포함

---

## 파일 간 의존 관계 요약

```
[추천 API]
RecommendationController
  └── RecommendationService
        ├── LifestyleResultRepository   (A담당 코드 그대로 사용)
        ├── PropertyRepository          (기존 property 코드 그대로 사용)
        │     └── PropertySpecification.inAreas()  (신규 추가)
        ├── AreaFacilityCountRepository (신규)
        │     └── AreaFacilityCount     (신규 엔티티)
        └── PropertyResponse.from(property, area)  (오버로드 추가)
              └── FacilityInfo          (신규 DTO)

[일반 검색 API — facilityCountMin 지원]
PropertyController
  └── PropertyService
        ├── PropertyRepository
        │     ├── PropertySpecification.search()    (기존 조건 처리)
        │     └── PropertySpecification.inAreas()   (facilityCountMin 있을 때)
        └── AreaFacilityCountRepository             (facilityCountMin 있을 때)

[상세 조회 — 항상 facilityInfo 포함]
PropertyService.getProperty()
  └── AreaFacilityCountRepository.findBySidoAndGugunAndDong()
```

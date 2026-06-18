# 매물 비교 기능 개발 로그

## 진행 현황

| 단계 | 작업 | 상태 |
|------|------|------|
| 1 | 기능 설계 및 계획서 작성 | ✅ 완료 |
| 2 | Pinia store 구현 | ✅ 완료 |
| 3 | Vue Router 라우트 등록 | ✅ 완료 |
| 4 | PropertyCard 비교 버튼 연결 | ✅ 완료 |
| 5 | CompareBar.vue 구현 | ⬜ 미완료 |
| 6 | CompareView.vue 구현 | ⬜ 미완료 |
| 7 | MapPropertyCard 비교 버튼 연결 | ⬜ 미완료 |
| 8 | PropertyDetailView 비교 버튼 연결 | ⬜ 미완료 |

---

## 커밋 이력

### 2026-06-15 — 계획서 작성 (`43625ab`)

**파일**: `docs/compare-feature-plan.md` 신규 생성

기능 구현 전 설계 단계. 아래 항목을 문서화함:

- 비교 항목 12개 (가격, 면적, 건축연도, 위험 등급 등) 및 강조 기준 정의
- 신규 파일 3개 / 수정 파일 6개 목록 확정
- `stores/compare.js` 코드 스니펫 초안
- `CompareBar.vue` 레이아웃 스케치
- `CompareView.vue` 테이블 레이아웃 및 강조 표시 로직 초안
- 구현 순서 8단계 정의
- 고려 사항 (비로그인, 모바일, 데이터 누락, URL 공유)

---

### 2026-06-16 — 계획서 보완 (`404c4d7`)

**파일**: `docs/compare-feature-plan.md` 수정

초안 리뷰 후 발견한 누락/오류 6건 수정:

- **전세/월세 혼합 처리 추가**: 전세 매물은 월세 행에 "—" 표시, 강조 시 동일 거래유형끼리만 비교
- **`highlight` 함수 버그 수정**: 사용하지 않는 `field` 파라미터 제거, 모든 값이 동일할 때 강조하지 않는 조건 추가
- **`isComparing` computed 정의 추가**: PropertyCard 코드 스니펫에 선언부가 빠져 있던 것 보완
- **MapPropertyCard 수정 내용 추가**: 신규 파일 목록에만 있고 컴포넌트 수정 섹션에 빠져 있던 것 추가
- **CompareView 내 직접 제거 명시**: 비교 페이지 안에서 × 버튼으로 매물을 뺄 수 있어야 함
- **`watch` 기반 리페칭 추가**: `compareStore.ids` 변경 시 데이터를 다시 페칭하는 로직 명시

---

### 2026-06-17 — store 및 라우터 구현 (`3cc7196`)

**파일**:
- `frontend/src/stores/compare.js` 신규 생성
- `frontend/src/router/index.js` 수정

#### `stores/compare.js`

비교 대상 매물 ID 목록을 전역으로 관리하는 Pinia store.

```js
export const useCompareStore = defineStore('compare', () => {
  const ids = ref([])             // propertyId[]

  const count = computed(...)     // 현재 담긴 개수
  const isFull = computed(...)    // 4개 도달 여부

  function has(propertyId) { ... }     // 담겨있는지 확인
  function toggle(propertyId) { ... }  // 담기 / 제거 토글 (4개 초과 시 무시)
  function clear() { ... }             // 전체 초기화

  return { ids, count, isFull, has, toggle, clear }
})
```

#### `router/index.js`

```js
{
  path: '/compare',
  name: 'compare',
  component: () => import('@/views/CompareView.vue'),
  // requiresAuth 없음 — 비로그인도 접근 가능
}
```

---

### 2026-06-18 — PropertyCard 비교 버튼 연결 (`a605ae5`)

**파일**: `frontend/src/components/PropertyCard.vue` 수정

`useCompareStore`를 연결하고 이미지 우측 상단(찜 버튼 바로 아래)에 비교 버튼 추가.

**동작**
- 기본 상태: `+` 아이콘 (흰 배경)
- 비교함에 담긴 상태: `✓` 아이콘 (파란 배경 `--active`)
- 비교함이 4개 꽉 찬 경우: 담기지 않은 카드의 버튼 `disabled` 처리 (opacity 0.4)

**추가된 핵심 코드**

```js
const compareStore = useCompareStore()
const isComparing = computed(() => compareStore.has(props.property.propertyId))
```

```html
<button
  class="compare-button"
  :class="{ 'compare-button--active': isComparing }"
  :disabled="compareStore.isFull && !isComparing"
  @click.prevent="compareStore.toggle(property.propertyId)"
>
  {{ isComparing ? '✓' : '+' }}
</button>
```

---

## 남은 작업

### 5단계: `CompareBar.vue`

화면 하단에 고정되는 비교함 UI. 선택된 매물 썸네일을 보여주고 `/compare`로 이동.

- `compareStore.ids`가 1개 이상일 때 슬라이드업으로 등장
- 각 슬롯에 × 버튼으로 개별 제거
- "비교하기" 버튼은 2개 이상일 때만 활성화
- `App.vue`에 마운트

### 6단계: `CompareView.vue`

`/compare` 페이지. 매물을 열(column), 항목을 행(row)으로 하는 비교 테이블.

- `compareStore.ids` 기준으로 `getProperty` + `getPropertyRisk` 병렬 호출
- 최솟값/최댓값 자동 강조 (`highlight` 함수)
- 전세/월세 혼합 시 월세 행 "—" 처리
- 열 헤더에 × 버튼으로 개별 제거, `compareStore.ids` watch로 리페칭
- 2개 미만이면 안내 메시지 표시

### 7단계: `MapPropertyCard.vue` 비교 버튼

PropertyCard와 동일한 방식. 가로형 카드이므로 버튼 위치는 우측 하단.

### 8단계: `PropertyDetailView.vue` 비교 버튼

패널 액션 영역에 "비교 추가 / 비교함에서 제거" 버튼 추가.

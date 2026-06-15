# 매물 비교 기능 구현 계획

## 개요

찜한 매물 또는 검색 결과의 매물들을 최대 4개까지 선택해 핵심 스펙을 나란히 비교하는 페이지.  
비교 선택 상태는 Pinia store로 전역 관리하고, 어느 페이지에서든 "비교 추가" 버튼으로 매물을 담을 수 있다.

---

## 1. 비교 항목

PropertyDetailView와 Risk API에서 가져올 수 있는 항목 기준.

| 항목 | 필드 | 강조 기준 |
|------|------|-----------|
| 거래 유형 | `rentType` | — |
| 보증금 | `deposit` | 최솟값 |
| 월세 | `monthlyRent` | 최솟값 |
| 관리비 | `managementFee` | 최솟값 |
| 방 타입 | `roomType` | — |
| 면적 | `area` | 최댓값 |
| 층수 | `floor` | — |
| 건축연도 | `buildYear` | 최신 (최댓값) |
| 위치 | `sido gugun dong` | — |
| 위험 등급 | `risk.label` | SAFE 강조 |
| 위험 점수 | `risk.score` | 최솟값 |
| 시세 대비 | `risk.priceGapRate` | 음수(저렴) 강조 |

---

## 2. 신규 파일 목록

```
frontend/src/
  stores/compare.js              # 비교 목록 전역 상태 (Pinia)
  views/CompareView.vue          # /compare 페이지
  components/CompareBar.vue      # 화면 하단 고정 비교함 (선택된 매물 미리보기)
```

수정이 필요한 기존 파일:

```
frontend/src/
  router/index.js                # /compare 라우트 추가
  components/PropertyCard.vue    # "비교 추가" 버튼 추가
  components/MapPropertyCard.vue # "비교 추가" 버튼 추가
  views/PropertyDetailView.vue   # "비교 추가" 버튼 추가
  views/FavoritesView.vue        # "비교 추가" 버튼 (찜 목록에서 선택)
  App.vue                        # CompareBar 마운트
```

---

## 3. 상태 관리 — `stores/compare.js`

```js
// stores/compare.js
import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

const MAX = 4

export const useCompareStore = defineStore('compare', () => {
  const ids = ref([])   // propertyId[]

  const count = computed(() => ids.value.length)
  const isFull = computed(() => ids.value.length >= MAX)

  function has(propertyId) {
    return ids.value.includes(propertyId)
  }

  function toggle(propertyId) {
    if (has(propertyId)) {
      ids.value = ids.value.filter((id) => id !== propertyId)
    } else if (!isFull.value) {
      ids.value = [...ids.value, propertyId]
    }
  }

  function clear() {
    ids.value = []
  }

  return { ids, count, isFull, has, toggle, clear }
})
```

**설계 포인트**
- 최대 4개 제한 — 4개 초과 시 `toggle` 무시 (UI에서 안내 토스트 표시)
- 새로고침 시 초기화 허용 — sessionStorage 유지는 v2에서 검토
- store를 모듈로 분리해 CompareBar, PropertyCard, CompareView 모두에서 공유

---

## 4. 라우터 추가 — `router/index.js`

```js
{
  path: '/compare',
  name: 'compare',
  component: () => import('@/views/CompareView.vue'),
  // 비로그인도 허용 — 단, 찜 연동 기능은 로그인 필요
}
```

---

## 5. 비교함 바 — `components/CompareBar.vue`

화면 하단 고정. 선택된 매물의 썸네일과 제목을 보여주고 "비교하기" 버튼으로 `/compare`로 이동.

**동작**
- `compareStore.ids` 길이가 0이면 숨김 (v-show)
- 길이가 1 이상이면 슬라이드업 등장
- 각 썸네일에 × 버튼으로 개별 제거
- "비교하기" 버튼은 2개 이상일 때만 활성화

**레이아웃 스케치**
```
┌─────────────────────────────────────────────────────────────┐
│  [썸네일×] [썸네일×] [썸네일×] [   +   ]   [비교하기 →]  │
└─────────────────────────────────────────────────────────────┘
```
(4번째 슬롯은 비어있으면 점선 박스로 "추가")

**마운트 위치**: `App.vue`의 `<RouterView />` 아래에 항상 렌더링

---

## 6. 비교 페이지 — `views/CompareView.vue`

### 데이터 페칭

`compareStore.ids`를 기준으로 컴포넌트 마운트 시 병렬 호출:

```js
const items = await Promise.all(
  compareStore.ids.map(async (id) => {
    const [property, risk] = await Promise.allSettled([
      getProperty(id),
      getPropertyRisk(id),
    ])
    return {
      property: property.status === 'fulfilled' ? property.value : null,
      risk: risk.status === 'fulfilled' ? risk.value : null,
    }
  })
)
```

이미 존재하는 `getProperty`, `getPropertyRisk`를 그대로 재사용.

### 레이아웃

가로 스크롤 테이블형. 매물이 열(column), 비교 항목이 행(row).

```
             [ 매물 A ]   [ 매물 B ]   [ 매물 C ]
이미지        [사진]       [사진]       [사진]
제목          역삼 원룸    홍대 오피    성수 투룸
가격          월세 58만    월세 72만    월세 92만
보증금        1,000만      1,500만      3,000만
관리비        7만          9만          10만
방 타입       원룸         오피스텔     투룸
면적          23.4m²       28.6m²       38.1m²   ← 최대값 강조
층수          5층          9층          4층
건축연도      2019년       2018년       2016년
위치          역삼동       동교동       성수동2가
위험 등급     [SAFE]       [CAUTION]    [SAFE]
위험 점수     12점         45점         18점
시세 대비     -3.2%        +8.1%        -1.5%
```

### 강조 표시 로직

```js
function highlight(field, values, mode) {
  // mode: 'min' | 'max'
  const nums = values.map(Number).filter(isFinite)
  const target = mode === 'min' ? Math.min(...nums) : Math.max(...nums)
  return (v) => Number(v) === target
}

// 사용 예
const isLowestRent = highlight('monthlyRent', items.map(i => i.property?.monthlyRent), 'min')
```

강조 항목은 초록 배경(`#ecfdf3`) + 굵은 텍스트로 표시. 단, 모든 값이 동일하면 강조 없음.

### 빈 슬롯 처리

ids가 2개 미만이면 "2개 이상의 매물을 선택해주세요" 안내 + 찜 목록 / 홈으로 이동 버튼 표시.

---

## 7. 기존 컴포넌트 수정

### PropertyCard.vue

찜 버튼 옆에 비교 버튼 추가. 비교함이 꽉 찬 경우 비활성화.

```html
<button
  class="compare-button"
  :class="{ 'compare-button--active': isComparing }"
  :disabled="compareStore.isFull && !isComparing"
  type="button"
  @click.prevent="compareStore.toggle(property.propertyId)"
>
  {{ isComparing ? '✓' : '+' }}
</button>
```

### PropertyDetailView.vue

패널 액션 영역에 "비교 추가" 버튼 추가:

```html
<button type="button" @click="compareStore.toggle(property.propertyId)">
  {{ compareStore.has(property.propertyId) ? '비교함에서 제거' : '비교 추가' }}
</button>
```

### FavoritesView.vue

각 PropertyCard 위에 체크박스 오버레이 UI 추가하는 방식 대신,  
기존 PropertyCard의 비교 버튼이 자동으로 동작하므로 별도 수정 불필요.

---

## 8. 구현 순서

1. `stores/compare.js` 작성
2. `router/index.js`에 `/compare` 라우트 추가
3. `App.vue`에 `<CompareBar />` 마운트
4. `components/CompareBar.vue` 구현
5. `components/PropertyCard.vue`에 비교 버튼 추가
6. `views/CompareView.vue` 구현 (데이터 페칭 → 테이블 레이아웃 → 강조 로직)
7. `views/PropertyDetailView.vue`에 비교 버튼 추가
8. 수동 테스트: 2개 / 4개 / 빈 상태 / 비로그인 시나리오

---

## 9. 고려 사항

### 비로그인 접근
- 비교 기능 자체는 로그인 불필요
- 찜 버튼은 로그인 필요 (기존 동작 그대로)

### 모바일 대응
- CompareView는 가로 스크롤 허용 (`overflow-x: auto`)
- CompareBar는 모바일에서 높이 절약형 레이아웃 (썸네일 생략, 개수만 표시)

### 데이터 누락
- `risk`가 UNKNOWN이거나 API 실패 시 해당 셀에 "—" 표시, 강조 대상에서 제외

### 비교 결과 공유
- URL에 ids를 쿼리 파라미터로 포함하면 링크 공유 가능 (`/compare?ids=101,102,103`)
- v2 기능으로 분류

---

## 10. 변경이 없는 것

- 백엔드 API 추가 없음 — `getProperty`, `getPropertyRisk` 재사용
- FavoriteApi 변경 없음
- 기존 찜하기 동작 변경 없음

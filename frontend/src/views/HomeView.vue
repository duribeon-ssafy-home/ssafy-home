<script setup>
import { computed, nextTick, ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import FilterBar from '@/components/FilterBar.vue'
import PropertyCard from '@/components/PropertyCard.vue'
import { getMyLatestLifestyleResult } from '@/api/lifestyleApi'
import { getProperties } from '@/api/propertyApi'
import { getRecommendations } from '@/api/recommendationApi'
import {
  createLifestylePresetChips,
  lifestyleTypeMeta,
} from '@/data/lifestyle'
import { useFavorites } from '@/composables/useFavorites'
import { useAuthStore } from '@/stores/auth'
import { useSearchStore } from '@/stores/search'

const authStore = useAuthStore()
const searchStore = useSearchStore()
const { loadFavorites } = useFavorites()

const properties = ref([])
const totalPages = ref(0)
const totalElements = ref(0)
const currentPage = ref(0)
const pageSize = ref(6)
const sortOrder = ref('createdAt,desc')
const isLoading = ref(false)
const lifestyleResult = ref(null)
const initialFilters = ref({
  location: '',
  deposit: '',
  monthlyRent: '',
  roomType: 'ALL',
})

const isRecommendationMode = computed(
  () => sortOrder.value === 'recommendation' && Boolean(lifestyleResult.value),
)
const lifestyleMeta = computed(() =>
  lifestyleResult.value?.lifestyleType
    ? lifestyleTypeMeta[lifestyleResult.value.lifestyleType]
    : null,
)
const recommendationChips = computed(() =>
  createLifestylePresetChips(lifestyleResult.value?.filterPreset, []).slice(0, 4),
)
const recommendationNoticeDescription = computed(() => {
  const typeName = lifestyleResult.value?.typeName || lifestyleMeta.value?.typeName
  const conditionText = recommendationChips.value.join(', ')

  if (!typeName) {
    return ''
  }

  if (!conditionText) {
    return `${typeName} 기준으로 대표 방 타입과 최신 매물을 우선 반영 중입니다.`
  }

  return `${typeName} 기준으로 ${conditionText} 성향을 추천 점수에 반영 중입니다.`
})
const activeFilters = ref({})
const activeRentType = ref(null)

const SIDO_ALIASES = {
  '서울': '서울특별시', '서울시': '서울특별시',
  '부산': '부산광역시', '부산시': '부산광역시',
  '대구': '대구광역시', '대구시': '대구광역시',
  '인천': '인천광역시', '인천시': '인천광역시',
  '광주': '광주광역시', '광주시': '광주광역시',
  '대전': '대전광역시', '대전시': '대전광역시',
  '울산': '울산광역시', '울산시': '울산광역시',
  '세종': '세종특별자치시', '세종시': '세종특별자치시',
  '경기': '경기도',
  '강원': '강원특별자치도', '강원도': '강원특별자치도',
  '충북': '충청북도',
  '충남': '충청남도',
  '전남': '전라남도',
  '전북': '전북특별자치도',
  '경남': '경상남도',
  '경북': '경상북도',
  '제주': '제주특별자치도', '제주도': '제주특별자치도',
}

function resolveLocationParam(input) {
  const v = input.trim()
  if (!v) return {}

  const parts = v.split(/\s+/)

  if (parts.length === 1) {
    const part = parts[0]
    if (SIDO_ALIASES[part]) return { sido: SIDO_ALIASES[part] }
    if (/(특별시|광역시|특별자치시|특별자치도|도)$/.test(part)) return { sido: part }
    if (/[구군시]$/.test(part)) return { gugun: part }
    return { dong: part }
  }

  const result = {}
  let lastUnmatched = null
  for (const part of parts) {
    if (SIDO_ALIASES[part]) {
      result.sido = SIDO_ALIASES[part]
    } else if (/(특별시|광역시|특별자치시|특별자치도|도)$/.test(part)) {
      result.sido = part
    } else if (/[구군시]$/.test(part)) {
      result.gugun = part
    } else {
      lastUnmatched = part
    }
  }
  if (lastUnmatched) result.dong = lastUnmatched
  return Object.keys(result).length ? result : { dong: v }
}

const visiblePages = computed(() => {
  if (totalPages.value <= 1) return []
  const current = currentPage.value + 1
  const total = totalPages.value
  const pages = new Set([1, total])

  for (let i = Math.max(2, current - 2); i <= Math.min(total - 1, current + 2); i++) {
    pages.add(i)
  }

  const sorted = [...pages].sort((a, b) => a - b)
  const result = []

  for (let i = 0; i < sorted.length; i++) {
    if (i > 0 && sorted[i] - sorted[i - 1] > 1) result.push('...')
    result.push(sorted[i])
  }

  return result
})

async function fetchProperties(params = {}) {
  isLoading.value = true
  try {
    const requestParams = {
      ...(activeRentType.value ? { rentType: activeRentType.value } : {}),
      ...params,
      page: currentPage.value,
      size: pageSize.value,
      ...(isRecommendationMode.value ? {} : { sort: sortOrder.value }),
    }

    const result = isRecommendationMode.value
      ? await getRecommendations(requestParams)
      : await getProperties(requestParams)

    properties.value = result.content
    totalPages.value = result.totalPages
    totalElements.value = result.totalElements
  } catch {
    if (isRecommendationMode.value) {
      await fetchFallbackProperties(params)
      return
    }

    properties.value = []
    totalPages.value = 0
    totalElements.value = 0
  } finally {
    isLoading.value = false
  }
}

async function fetchFallbackProperties(params = {}) {
  const fallbackParams = {
    ...(activeRentType.value ? { rentType: activeRentType.value } : {}),
    ...params,
    page: currentPage.value,
    size: pageSize.value,
    sort: 'createdAt,desc',
  }
  const result = await getProperties(fallbackParams)

  properties.value = result.content
  totalPages.value = result.totalPages
  totalElements.value = result.totalElements
}

function buildSearchParams(filters) {
  const params = {}
  if (filters.locationParts) {
    if (filters.locationParts.sido) params.sido = filters.locationParts.sido
    if (filters.locationParts.gugun) params.gugun = filters.locationParts.gugun
    if (filters.locationParts.dong) params.dong = filters.locationParts.dong
  } else {
    Object.assign(params, resolveLocationParam(filters.location || ''))
  }
  if (filters.roomType !== 'ALL') params.roomType = filters.roomType
  if (filters.deposit) params.maxDeposit = Number(filters.deposit)
  if (filters.monthlyRent) params.maxMonthlyRent = Number(filters.monthlyRent)
  return params
}

async function handleSearch(filters) {
  const params = buildSearchParams(filters)
  activeFilters.value = params
  searchStore.setSearch(params, filters.location || '')
  currentPage.value = 0
  await fetchProperties(params)
  await nextTick()
  scrollToProperties()
}

function setRentType(type) {
  activeRentType.value = activeRentType.value === type ? null : type
  currentPage.value = 0
  fetchProperties(activeFilters.value)
}

function clearRentType() {
  activeRentType.value = null
  currentPage.value = 0
  fetchProperties(activeFilters.value)
}

function handleSortChange() {
  currentPage.value = 0
  fetchProperties(activeFilters.value)
}

function handlePageSizeChange() {
  currentPage.value = 0
  fetchProperties(activeFilters.value)
}

function goToPage(page) {
  currentPage.value = page
  fetchProperties(activeFilters.value)
  document.getElementById('featured-properties')?.scrollIntoView({ behavior: 'smooth' })
}

function scrollToProperties() {
  const target = document.getElementById('featured-properties')
  if (!target) return
  target.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function loadInitialProperties() {
  const defaultParams = {}

  if (authStore.isAuthenticated) {
    try {
      const result = await getMyLatestLifestyleResult()
      lifestyleResult.value = result
      sortOrder.value = 'recommendation'
      initialFilters.value = {
        location: '',
        deposit: '',
        monthlyRent: '',
        roomType: 'ALL',
      }

      activeFilters.value = defaultParams
      searchStore.setSearch(defaultParams, '')
      currentPage.value = 0
      await fetchProperties(defaultParams)
      return
    } catch {
      lifestyleResult.value = null
      sortOrder.value = 'createdAt,desc'
    }
  }

  activeFilters.value = defaultParams
  searchStore.setSearch(defaultParams, '')
  currentPage.value = 0
  await fetchProperties(defaultParams)
}

onMounted(() => {
  loadInitialProperties()
  loadFavorites()
})
</script>

<template>
  <main class="page home-page">
    <section class="hero">
      <div class="hero__overlay"></div>
      <div class="section-container hero__content">
        <p class="eyebrow">SSAFY HOME</p>
        <h1>내 예산과 조건에 맞는 집을 찾으세요</h1>
        <p>지역, 보증금, 월세, 방 타입부터 생활 패턴까지 고려해 더 잘 맞는 매물을 추천합니다.</p>
        <div class="hero__actions">
          <button class="primary-action" type="button" @click="scrollToProperties">매물 살펴보기</button>
          <RouterLink class="secondary-action" :to="{ name: 'survey' }">자취TI 해보기</RouterLink>
        </div>

        <div class="hero__search-panel" aria-label="매물 검색 필터">
          <div class="hero__search-heading">
            <strong>조건에 맞는 매물 찾기</strong>
            <span>지역과 예산을 입력하거나 주요 금액을 선택해 바로 검색하세요.</span>
          </div>
          <FilterBar :initial-filters="initialFilters" @search="handleSearch" />
        </div>

        <div class="hero__features" aria-label="주요 기능">
          <article>
            <strong>조건 검색</strong>
            <span>지역·가격·방 타입으로 빠르게 좁히기</span>
          </article>
          <article>
            <strong>자취TI 추천</strong>
            <span>자취 성향 결과를 반영한 추천 매물 보기</span>
          </article>
          <article>
            <strong>의심 매물 신고</strong>
            <span>신고 사유와 위험도 정보를 함께 확인</span>
          </article>
        </div>
      </div>
    </section>

    <section id="featured-properties" class="featured-section">
      <div class="section-container">
        <div class="section-heading">
          <div>
            <p class="eyebrow">Featured Properties</p>
            <h2 class="section-title">추천 매물</h2>
          </div>
          <p class="section-copy">
            밝고 현실적인 원룸/오피스텔 중심으로, 핵심 조건을 빠르게 비교할 수 있게 정리했습니다.
          </p>
        </div>

        <div
          v-if="isRecommendationMode"
          class="recommendation-notice"
          data-testid="recommendation-notice"
        >
          <div class="recommendation-notice__content">
            <strong>자취TI 기준 우선 정렬</strong>
            <span>{{ recommendationNoticeDescription }}</span>
          </div>
          <div v-if="recommendationChips.length" class="recommendation-notice__chips">
            <span v-for="chip in recommendationChips" :key="chip">{{ chip }}</span>
          </div>
        </div>

        <div class="rent-type-tabs">
          <button
            class="rent-tab"
            :class="{ 'rent-tab--active': activeRentType === null }"
            @click="clearRentType()"
          >전체</button>
          <button
            class="rent-tab"
            :class="{ 'rent-tab--active': activeRentType === 'JEONSE' }"
            @click="setRentType('JEONSE')"
          >전세</button>
          <button
            class="rent-tab"
            :class="{ 'rent-tab--active': activeRentType === 'MONTHLY' }"
            @click="setRentType('MONTHLY')"
          >월세</button>
          <button
            class="rent-tab"
            :class="{ 'rent-tab--active': activeRentType === 'SEMI_JEONSE' }"
            @click="setRentType('SEMI_JEONSE')"
          >반전세</button>
        </div>

        <div class="result-controls">
          <span class="result-count">총 {{ totalElements.toLocaleString() }}개</span>
          <div class="control-group">
            <select
              class="control-select"
              v-model="sortOrder"
              @change="handleSortChange"
            >
              <option v-if="lifestyleResult" value="recommendation">추천순</option>
              <option value="createdAt,desc">최신순</option>
              <option value="deposit,asc">가격 낮은순</option>
              <option value="deposit,desc">가격 높은순</option>
            </select>

            <select class="control-select" v-model="pageSize" @change="handlePageSizeChange">
              <option :value="6">6개씩 보기</option>
              <option :value="18">18개씩 보기</option>
              <option :value="30">30개씩 보기</option>
            </select>
          </div>
        </div>

        <div v-if="isLoading" class="empty-result">
          <strong>매물을 불러오는 중입니다...</strong>
        </div>

        <template v-else-if="properties.length">
          <div class="property-grid">
            <PropertyCard
              v-for="property in properties"
              :key="property.propertyId"
              :property="property"
            />
          </div>

          <div v-if="totalPages > 1" class="pagination">
            <button
              class="page-button"
              :disabled="currentPage === 0"
              @click="goToPage(currentPage - 1)"
            >
              이전
            </button>

            <template v-for="item in visiblePages" :key="item">
              <span v-if="item === '...'" class="page-ellipsis">...</span>
              <button
                v-else
                class="page-button"
                :class="{ 'page-button--active': currentPage === item - 1 }"
                @click="goToPage(item - 1)"
              >
                {{ item }}
              </button>
            </template>

            <button
              class="page-button"
              :disabled="currentPage === totalPages - 1"
              @click="goToPage(currentPage + 1)"
            >
              다음
            </button>
          </div>
        </template>

        <div v-else class="empty-result">
          <strong>조건에 맞는 매물이 없습니다</strong>
          <p>지역이나 가격 조건을 조금 넓혀 다시 찾아보세요.</p>
        </div>
      </div>
    </section>

    <section class="lifestyle-band">
      <div class="section-container lifestyle-band__inner">
        <div>
          <p class="eyebrow">자취TI</p>
          <h2>자취TI로 나에게 맞는 매물을 추천받아보세요</h2>
          <p>
            짧은 성향 테스트로 생활권, 예산, 집 컨디션 선호를 매물 조건에 반영합니다.
          </p>
        </div>
        <RouterLink class="band-action" :to="{ name: 'survey' }">자취TI 시작</RouterLink>
      </div>
    </section>
  </main>
</template>

<style lang="scss" scoped>
.home-page {
  padding-bottom: 0;
}

.hero {
  position: relative;
  min-height: min(820px, calc(100vh - 72px));
  display: grid;
  align-items: end;
  overflow: visible;
  background:
    linear-gradient(90deg, rgba(10, 17, 28, 0.7) 0%, rgba(10, 17, 28, 0.38) 48%, rgba(10, 17, 28, 0.18) 100%),
    url('https://images.unsplash.com/photo-1493809842364-78817add7ffb?auto=format&fit=crop&w=2000&q=80')
      center / cover;
}

.hero__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(15, 23, 42, 0.16));
}

.hero__content {
  position: relative;
  z-index: 1;
  color: var(--color-surface);
  padding: 210px 0 76px;

  .eyebrow {
    color: rgba(255, 255, 255, 0.78);
  }

  h1 {
    max-width: 680px;
    margin-top: 14px;
    color: var(--color-surface);
    font-size: 56px;
    font-weight: 900;
    letter-spacing: 0;
    line-height: 1.12;
  }

  p:not(.eyebrow) {
    max-width: 650px;
    margin-top: 18px;
    color: rgba(255, 255, 255, 0.82);
    font-size: 17px;
    font-weight: 600;
  }
}

.hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}

.primary-action,
.secondary-action,
.band-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 900;
  padding: 0 18px;
  transition:
    background-color var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast),
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}

.primary-action {
  background: var(--color-surface);
  color: var(--color-primary-dark);

  &:hover {
    box-shadow: 0 14px 28px rgba(0, 0, 0, 0.22);
    transform: translateY(-1px);
  }
}

.secondary-action {
  border: 1px solid rgba(255, 255, 255, 0.42);
  background: rgba(255, 255, 255, 0.12);
  color: var(--color-surface);

  &:hover {
    background: rgba(255, 255, 255, 0.2);
    transform: translateY(-1px);
  }
}

.hero__search-panel {
  position: relative;
  z-index: 30;
  width: min(100%, 1120px);
  margin-top: 32px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  padding: 18px;
  box-shadow: 0 28px 70px rgba(15, 23, 42, 0.26);

  :deep(.filter-bar) {
    border: 0;
    border-radius: 0;
    background: transparent;
    box-shadow: none;
    padding: 0;
  }
}

.hero__search-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 14px;

  strong {
    color: var(--color-heading);
    font-size: 20px;
    font-weight: 900;
  }

  span {
    max-width: 420px;
    color: var(--color-muted);
    font-size: 13px;
    font-weight: 700;
    line-height: 1.45;
    text-align: right;
  }
}

.hero__features {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  width: min(100%, 860px);
  margin-top: 20px;

  article {
    border-left: 1px solid rgba(255, 255, 255, 0.35);
    padding: 2px 0 2px 14px;
  }

  strong,
  span {
    display: block;
  }

  strong {
    color: var(--color-surface);
    font-size: 14px;
    font-weight: 900;
  }

  span {
    margin-top: 4px;
    color: rgba(255, 255, 255, 0.76);
    font-size: 13px;
    font-weight: 700;
    line-height: 1.45;
  }
}

.featured-section {
  padding: 54px 0;
  scroll-margin-top: calc(var(--header-height) + 16px);
}

.section-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 28px;
  margin-bottom: 24px;

  .section-copy {
    max-width: 460px;
  }
}

.recommendation-notice {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px 18px;
  margin-bottom: 24px;
  border: 1px solid rgba(54, 95, 145, 0.18);
  border-left: 4px solid var(--color-primary);
  border-radius: var(--radius-sm);
  background: #f8fbff;
  padding: 14px 16px;
}

.recommendation-notice__content {
  display: grid;
  gap: 4px;
  min-width: 260px;

  strong {
    color: var(--color-heading);
    font-size: 14px;
    font-weight: 900;
  }

  span {
    color: var(--color-muted);
    font-size: 13px;
    font-weight: 700;
    line-height: 1.45;
  }
}

.recommendation-notice__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  span {
    border: 1px solid rgba(54, 95, 145, 0.18);
    border-radius: var(--radius-sm);
    background: var(--color-surface);
    color: var(--color-primary-dark);
    font-size: 12px;
    font-weight: 900;
    padding: 7px 9px;
  }
}

.property-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.rent-type-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.rent-tab {
  height: 36px;
  padding: 0 18px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition:
    background-color var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast);

  &:hover {
    border-color: var(--color-primary);
    color: var(--color-primary);
  }
}

.rent-tab--active {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: var(--color-surface);

  &:hover {
    color: var(--color-surface);
  }
}

.result-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.result-count {
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 800;
}

.control-group {
  display: flex;
  gap: 8px;
}

.control-select {
  height: 34px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-heading);
  font-size: 13px;
  font-weight: 700;
  padding: 0 10px;
  cursor: pointer;
  outline: none;
  transition: border-color var(--transition-fast);

  &:hover,
  &:focus {
    border-color: var(--color-primary);
  }
}

.pagination {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 32px;
}

.page-button {
  min-width: 40px;
  height: 40px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-heading);
  font-size: 14px;
  font-weight: 800;
  padding: 0 12px;
  transition:
    background-color var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast);

  &:hover:not(:disabled) {
    border-color: var(--color-primary);
    color: var(--color-primary);
  }

  &:disabled {
    color: var(--color-subtle);
    cursor: not-allowed;
  }
}

.page-ellipsis {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  height: 40px;
  color: var(--color-muted);
  font-size: 14px;
  font-weight: 800;
}

.page-button--active {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: var(--color-surface);

  &:hover {
    border-color: var(--color-primary) !important;
    color: var(--color-surface) !important;
  }
}

.empty-result {
  display: grid;
  gap: 8px;
  place-items: center;
  min-height: 220px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-muted);
  text-align: center;

  strong {
    color: var(--color-heading);
    font-size: 18px;
    font-weight: 900;
  }
}

.lifestyle-band {
  padding: 52px 0;
  background: linear-gradient(135deg, #1f344f 0%, #365f91 100%);
  color: var(--color-surface);
}

.lifestyle-band__inner {
  display: grid;
  justify-items: center;
  gap: 22px;
  text-align: center;

  .eyebrow {
    color: rgba(255, 255, 255, 0.72);
  }

  h2 {
    max-width: 720px;
    margin-top: 8px;
    color: var(--color-surface);
    font-size: 32px;
    font-weight: 900;
    letter-spacing: 0;
    line-height: 1.25;
  }

  p:not(.eyebrow) {
    max-width: 620px;
    margin-top: 12px;
    color: rgba(255, 255, 255, 0.75);
    font-weight: 600;
  }
}

.band-action {
  background: var(--color-surface);
  color: var(--color-primary-dark);

  &:hover {
    box-shadow: 0 12px 28px rgba(0, 0, 0, 0.2);
    transform: translateY(-1px);
  }
}

@media (max-width: 940px) {
  .hero {
    min-height: auto;
  }

  .hero__content h1 {
    font-size: 42px;
  }

  .hero__search-heading {
    align-items: flex-start;
    flex-direction: column;

    span {
      max-width: none;
      text-align: left;
    }
  }

  .hero__features {
    grid-template-columns: 1fr;
  }

  .section-heading,
  .lifestyle-band__inner {
    align-items: flex-start;
    flex-direction: column;
  }

  .property-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .hero {
    min-height: auto;
    background:
      linear-gradient(180deg, rgba(10, 17, 28, 0.76) 0%, rgba(10, 17, 28, 0.42) 100%),
      url('https://images.unsplash.com/photo-1493809842364-78817add7ffb?auto=format&fit=crop&w=1400&q=80')
        center / cover;
  }

  .hero__content {
    padding: 56px 0 42px;

    h1 {
      font-size: 34px;
    }

    p:not(.eyebrow) {
      font-size: 15px;
    }
  }

  .hero__actions {
    margin-top: 24px;
  }

  .hero__search-panel {
    margin-top: 26px;
    border-radius: 14px;
    padding: 14px;
  }

  .hero__features {
    margin-top: 16px;
  }

  .property-grid {
    grid-template-columns: 1fr;
  }
}
</style>

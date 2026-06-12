<script setup>
import { computed, ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import FilterBar from '@/components/FilterBar.vue'
import PropertyCard from '@/components/PropertyCard.vue'
import { getProperties } from '@/api/propertyApi'
import { useFavorites } from '@/composables/useFavorites'

const { loadFavorites } = useFavorites()

const properties = ref([])
const totalPages = ref(0)
const totalElements = ref(0)
const currentPage = ref(0)
const pageSize = ref(6)
const sortOrder = ref('createdAt,desc')
const isLoading = ref(false)
const activeFilters = ref({})

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
  if (SIDO_ALIASES[v]) return { sido: SIDO_ALIASES[v] }
  if (/(특별시|광역시|특별자치시|특별자치도|도)$/.test(v)) return { sido: v }
  if (/[구군시]$/.test(v)) return { gugun: v }
  if (/[동읍면리]$/.test(v)) return { dong: v }
  return { dong: v }
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
    const result = await getProperties({ ...params, page: currentPage.value, size: pageSize.value, sort: sortOrder.value })
    properties.value = result.content
    totalPages.value = result.totalPages
    totalElements.value = result.totalElements
  } catch {
    properties.value = []
    totalPages.value = 0
  } finally {
    isLoading.value = false
  }
}

function handleSearch(filters) {
  const params = {}
  Object.assign(params, resolveLocationParam(filters.location))
  if (filters.roomType !== 'ALL') params.roomType = filters.roomType
  if (filters.deposit) params.maxDeposit = Number(filters.deposit)
  if (filters.monthlyRent) params.maxMonthlyRent = Number(filters.monthlyRent)
  activeFilters.value = params
  currentPage.value = 0
  fetchProperties(params)
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

onMounted(() => {
  fetchProperties()
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
          <a class="primary-action" href="#featured-properties">매물 검색하기</a>
          <RouterLink class="secondary-action" :to="{ name: 'survey' }">
            생활패턴으로 추천받기
          </RouterLink>
        </div>
        <dl class="hero__stats" aria-label="서비스 요약">
          <div>
            <dt>6</dt>
            <dd>추천 mock 매물</dd>
          </div>
          <div>
            <dt>3</dt>
            <dd>핵심 조건 필터</dd>
          </div>
          <div>
            <dt>6</dt>
            <dd>라이프스타일 질문</dd>
          </div>
        </dl>
      </div>
    </section>

    <section class="filter-section" aria-label="매물 검색 필터">
      <div class="section-container">
        <FilterBar @search="handleSearch" />
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

        <div class="result-controls">
          <span class="result-count">총 {{ totalElements.toLocaleString() }}개</span>
          <div class="control-group">
            <select class="control-select" v-model="sortOrder" @change="handleSortChange">
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
          <p class="eyebrow">Lifestyle Match</p>
          <h2>조건 검색 다음은 생활 방식까지 맞춰볼 차례입니다</h2>
          <p>
            간단한 A/B 선택으로 나에게 맞는 주거 타입을 확인하고, 추천 조건을 바로 매물 탐색에
            연결해보세요.
          </p>
        </div>
        <RouterLink class="band-action" :to="{ name: 'survey' }">설문 시작하기</RouterLink>
      </div>
    </section>
  </main>
</template>

<style lang="scss" scoped>
.home-page {
  padding-bottom: 72px;
}

.hero {
  position: relative;
  min-height: 470px;
  display: grid;
  align-items: center;
  overflow: hidden;
  background:
    linear-gradient(90deg, rgba(10, 17, 28, 0.72) 0%, rgba(10, 17, 28, 0.42) 45%, transparent 76%),
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
  padding: 76px 0 86px;

  .eyebrow {
    color: rgba(255, 255, 255, 0.78);
  }

  h1 {
    max-width: 610px;
    margin-top: 14px;
    color: var(--color-surface);
    font-size: 52px;
    font-weight: 900;
    letter-spacing: 0;
    line-height: 1.12;
  }

  p:not(.eyebrow) {
    max-width: 600px;
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
  margin-top: 30px;
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

.hero__stats {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 38px;

  div {
    min-width: 128px;
    border-left: 1px solid rgba(255, 255, 255, 0.28);
    padding-left: 14px;
  }

  dt {
    color: var(--color-surface);
    font-size: 24px;
    font-weight: 900;
  }

  dd {
    color: rgba(255, 255, 255, 0.72);
    font-size: 13px;
    font-weight: 700;
  }
}

.filter-section {
  position: relative;
  z-index: 2;
  margin-top: -34px;
}

.featured-section {
  padding: 66px 0 54px;
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

.property-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
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
  padding: 56px 0;
  background: linear-gradient(135deg, #1f344f 0%, #365f91 100%);
  color: var(--color-surface);
}

.lifestyle-band__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;

  .eyebrow {
    color: rgba(255, 255, 255, 0.72);
  }

  h2 {
    max-width: 620px;
    margin-top: 8px;
    color: var(--color-surface);
    font-size: 30px;
    font-weight: 900;
    letter-spacing: 0;
    line-height: 1.25;
  }

  p:not(.eyebrow) {
    max-width: 680px;
    margin-top: 12px;
    color: rgba(255, 255, 255, 0.75);
    font-weight: 600;
  }
}

.band-action {
  flex: 0 0 auto;
  background: var(--color-surface);
  color: var(--color-primary-dark);

  &:hover {
    box-shadow: 0 12px 28px rgba(0, 0, 0, 0.2);
    transform: translateY(-1px);
  }
}

@media (max-width: 940px) {
  .hero__content h1 {
    font-size: 42px;
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
    min-height: 520px;
    background:
      linear-gradient(180deg, rgba(10, 17, 28, 0.76) 0%, rgba(10, 17, 28, 0.42) 100%),
      url('https://images.unsplash.com/photo-1493809842364-78817add7ffb?auto=format&fit=crop&w=1400&q=80')
        center / cover;
  }

  .hero__content {
    padding: 60px 0 88px;

    h1 {
      font-size: 34px;
    }

    p:not(.eyebrow) {
      font-size: 15px;
    }
  }

  .hero__stats {
    display: grid;
    grid-template-columns: 1fr;
  }

  .property-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import DetailedFilterBar from '@/components/DetailedFilterBar.vue'
import MapPropertyCard from '@/components/MapPropertyCard.vue'
import KakaoMap from '@/components/KakaoMap.vue'
import { getProperties } from '@/api/propertyApi'
import { useFavorites } from '@/composables/useFavorites'

const router = useRouter()
const { loadFavorites } = useFavorites()

const listProperties = ref([])
const mapProperties = ref([])
const selectedId = ref(null)
const selectedProperty = computed(() =>
  mapProperties.value.find((p) => p.propertyId === selectedId.value) ?? null
)
const totalElements = ref(0)
const currentPage = ref(0)
const totalPages = ref(0)
const isLoading = ref(false)
const activeFilters = ref({})
const sortOrder = ref('createdAt,desc')
const showInfoWindow = ref(false)
const sentinel = ref(null)
let observer = null

const hasMore = computed(() => currentPage.value < totalPages.value - 1)

async function fetchMapMarkers(params) {
  const result = await getProperties({ ...params, page: 0, size: 200, sort: 'createdAt,desc' })
  mapProperties.value = result.content
}

async function fetchListPage(params, page) {
  isLoading.value = true
  try {
    const result = await getProperties({ ...params, page, size: 20, sort: sortOrder.value })
    if (page === 0) {
      listProperties.value = result.content
    } else {
      listProperties.value = [...listProperties.value, ...result.content]
    }
    totalElements.value = result.totalElements
    totalPages.value = result.totalPages
    currentPage.value = page
  } finally {
    isLoading.value = false
  }
}

async function handleSearch(filters) {
  activeFilters.value = filters
  currentPage.value = 0
  selectedId.value = null
  showInfoWindow.value = false
  await Promise.all([
    fetchMapMarkers(filters),
    fetchListPage(filters, 0),
  ])
}

async function loadMore() {
  if (!hasMore.value || isLoading.value) return
  await fetchListPage(activeFilters.value, currentPage.value + 1)
}

function handleCardSelect(id) {
  if (selectedId.value === id) {
    selectedId.value = null
    showInfoWindow.value = false
  } else {
    selectedId.value = id
    showInfoWindow.value = true
  }
}

function handleMarkerSelect(id) {
  if (id === null) {
    selectedId.value = null
    showInfoWindow.value = false
    return
  }
  selectedId.value = id
  showInfoWindow.value = true
  const el = document.getElementById(`map-card-${id}`)
  el?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
}

function goToDetail() {
  if (selectedId.value) router.push({ name: 'property-detail', params: { id: selectedId.value } })
}

onMounted(async () => {
  await Promise.all([
    fetchMapMarkers({}),
    fetchListPage({}, 0),
    loadFavorites(),
  ])

  observer = new IntersectionObserver(
    (entries) => { if (entries[0].isIntersecting) loadMore() },
    { threshold: 0.1 }
  )
  if (sentinel.value) observer.observe(sentinel.value)
})

onUnmounted(() => observer?.disconnect())
</script>

<template>
  <div class="map-view">
    <DetailedFilterBar @search="handleSearch" />

    <div class="split-layout">
      <aside class="list-panel">
        <div class="list-header">
          <span class="result-count">총 {{ totalElements.toLocaleString() }}개</span>
          <select v-model="sortOrder" class="sort-select" @change="handleSearch(activeFilters)">
            <option value="createdAt,desc">최신순</option>
            <option value="deposit,asc">가격 낮은순</option>
            <option value="deposit,desc">가격 높은순</option>
          </select>
        </div>

        <div class="list-scroll">
          <MapPropertyCard
            v-for="property in listProperties"
            :id="`map-card-${property.propertyId}`"
            :key="property.propertyId"
            :property="property"
            :selected="selectedId === property.propertyId"
            @select="handleCardSelect"
          />

          <div v-if="isLoading" class="list-loading">불러오는 중...</div>
          <div v-if="!isLoading && listProperties.length === 0" class="list-empty">
            조건에 맞는 매물이 없습니다
          </div>

          <div ref="sentinel" class="sentinel" />
        </div>
      </aside>

      <div class="map-panel">
        <KakaoMap
          :properties="mapProperties"
          :selected-id="selectedId"
          @select="handleMarkerSelect"
        />

        <Transition name="info-fade">
          <div v-if="showInfoWindow && selectedProperty" class="info-window">
            <button class="info-window__close" type="button" @click="handleMarkerSelect(null)">✕</button>
            <div class="info-window__img">
              <img
                v-if="selectedProperty.images?.[0]?.imageUrl"
                :src="selectedProperty.images[0].imageUrl"
                :alt="selectedProperty.title"
              />
              <div v-else class="info-window__img-placeholder" />
            </div>
            <div class="info-window__body">
              <p class="info-window__price">
                <template v-if="selectedProperty.rentType === 'JEONSE'">
                  전세 {{ Number(selectedProperty.deposit).toLocaleString() }}만
                </template>
                <template v-else>
                  월세 {{ selectedProperty.monthlyRent }}만 / 보증금 {{ Number(selectedProperty.deposit).toLocaleString() }}만
                </template>
              </p>
              <p class="info-window__spec">
                {{ selectedProperty.gugun }} {{ selectedProperty.dong }}
              </p>
              <p class="info-window__spec">
                {{ selectedProperty.area }}m² · {{ selectedProperty.floor }}층
              </p>
              <button class="info-window__link" type="button" @click="goToDetail">
                상세 보기 →
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.map-view {
  height: calc(100vh - var(--header-height));
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.split-layout {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.list-panel {
  width: 33%;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--color-border);
  background: var(--color-bg-soft);
}

.list-header {
  padding: 10px 14px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.result-count {
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 800;
}

.sort-select {
  height: 30px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-heading);
  font-size: 12px;
  font-weight: 700;
  padding: 0 8px;
  outline: none;
  cursor: pointer;
}

.list-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: var(--color-border); border-radius: 2px; }
}

.list-loading,
.list-empty {
  padding: 20px;
  text-align: center;
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 700;
}

.sentinel { height: 1px; }

.map-panel {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.info-window {
  position: absolute;
  bottom: 24px;
  left: 24px;
  width: 220px;
  background: var(--color-surface);
  border-radius: var(--radius-sm);
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.18);
  overflow: hidden;
  z-index: 10;
}

.info-window__close {
  position: absolute;
  top: 7px;
  right: 8px;
  width: 22px;
  height: 22px;
  background: rgba(0, 0, 0, 0.45);
  color: white;
  border-radius: 50%;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
  cursor: pointer;
}

.info-window__img {
  width: 100%;
  height: 110px;

  img { width: 100%; height: 100%; object-fit: cover; }
}

.info-window__img-placeholder {
  width: 100%;
  height: 100%;
  background: var(--color-bg-soft);
}

.info-window__body {
  padding: 10px 12px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-window__price {
  color: var(--color-heading);
  font-size: 13px;
  font-weight: 900;
}

.info-window__spec {
  color: var(--color-muted);
  font-size: 11px;
  font-weight: 700;
}

.info-window__link {
  margin-top: 6px;
  background: var(--color-primary);
  color: var(--color-surface);
  border-radius: var(--radius-sm);
  padding: 7px 0;
  text-align: center;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
  transition: background-color var(--transition-fast);

  &:hover { background: var(--color-primary-dark); }
}

.info-fade-enter-active,
.info-fade-leave-active { transition: opacity 0.2s, transform 0.2s; }
.info-fade-enter-from,
.info-fade-leave-to { opacity: 0; transform: translateY(8px); }
</style>

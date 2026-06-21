<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { getProperty, getPropertyRisk } from '@/api/propertyApi'
import { roomTypeLabels } from '@/data/mockProperties'
import { useFavorites } from '@/composables/useFavorites'
import { useAuthStore } from '@/stores/auth'
import { useCompareStore } from '@/stores/compare'
const props = defineProps({
  id: {
    type: String,
    required: true,
  },
})

const router = useRouter()
const authStore = useAuthStore()
const { loadFavorites, toggleFavorite: toggle, isFavorited } = useFavorites()
const compareStore = useCompareStore()

const property = ref(null)
const risk = ref(null)
const isLoading = ref(false)
const isError = ref(false)

const riskMeta = {
  SAFE:    { label: '안전', class: 'risk--safe' },
  CAUTION: { label: '주의', class: 'risk--caution' },
  DANGER:  { label: '위험', class: 'risk--danger' },
  UNKNOWN: { label: '분석 불가', class: 'risk--unknown' },
}
const isToggling = ref(false)

const isFavorite = computed(() => property.value ? isFavorited(property.value.propertyId) : false)

async function fetchProperty() {
  isLoading.value = true
  isError.value = false
  try {
    const [propertyData, riskData] = await Promise.allSettled([
      getProperty(props.id),
      getPropertyRisk(props.id),
    ])
    if (propertyData.status === 'fulfilled') property.value = propertyData.value
    else isError.value = true
    if (riskData.status === 'fulfilled') risk.value = riskData.value
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetchProperty()
  loadFavorites()
})
watch(() => props.id, fetchProperty)

async function handleFavorite() {
  if (!authStore.isAuthenticated) {
    router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  if (isToggling.value || !property.value) return
  isToggling.value = true
  try {
    await toggle(property.value.propertyId)
  } finally {
    isToggling.value = false
  }
}

const imageUrl = computed(() => property.value?.images?.[0]?.imageUrl)
const priceLabel = computed(() => {
  if (!property.value) return ''
  if (property.value.rentType === 'JEONSE') {
    return `전세 ${formatMoneyManwon(property.value.deposit)}`
  }
  return `월세 ${formatMoneyManwon(property.value.monthlyRent)} / 보증금 ${formatMoneyManwon(
    property.value.deposit,
  )}`
})

function formatMoneyManwon(value) {
  const amount = Number(value)
  return Number.isFinite(amount) ? `${amount.toLocaleString('ko-KR')}만` : '-'
}
</script>

<template>
  <main class="page detail-page">
    <section class="section-container">
      <RouterLink class="back-link" :to="{ name: 'home' }">매물 찾기로 돌아가기</RouterLink>

      <div v-if="isLoading" class="placeholder">
        <strong>매물 정보를 불러오는 중입니다...</strong>
      </div>

      <div v-else-if="isError" class="placeholder">
        <strong>매물을 불러올 수 없습니다</strong>
        <p>잠시 후 다시 시도해 주세요.</p>
      </div>

      <div v-else-if="property" class="detail-layout">
        <div class="gallery-shell">
          <img v-if="imageUrl" :src="imageUrl" :alt="property.title" />
          <div v-else class="gallery-placeholder">
            <span>등록된 이미지가 없습니다</span>
          </div>
        </div>

        <aside class="info-panel">
          <p class="eyebrow">Property Detail</p>
          <h1>{{ property.title }}</h1>
          <strong>{{ priceLabel }}</strong>
          <p>{{ property.roadAddress }}</p>

          <dl>
            <div>
              <dt>방 타입</dt>
              <dd>{{ roomTypeLabels[property.roomType] }}</dd>
            </div>
            <div>
              <dt>면적</dt>
              <dd>{{ Number(property.area).toFixed(1) }}m2</dd>
            </div>
            <div>
              <dt>층수</dt>
              <dd>{{ property.floor }}층</dd>
            </div>
            <div>
              <dt>연식</dt>
              <dd>{{ property.buildYear }}년</dd>
            </div>
          </dl>

          <div v-if="risk" class="risk-panel" :class="riskMeta[risk.label]?.class">
            <div class="risk-header">
              <span class="risk-badge">{{ riskMeta[risk.label]?.label }}</span>
              <span v-if="risk.label !== 'UNKNOWN'" class="risk-score">위험 점수 {{ risk.score }}점</span>
            </div>
            <dl class="risk-detail">
              <div v-if="risk.marketPriceAvg">
                <dt>주변 시세 평균</dt>
                <dd>{{ Number(risk.marketPriceAvg).toLocaleString('ko-KR') }}만원</dd>
              </div>
              <div v-if="risk.priceGapRate != null">
                <dt>시세 대비 차이</dt>
                <dd>{{ risk.priceGapRate > 0 ? '+' : '' }}{{ (risk.priceGapRate * 100).toFixed(1) }}%</dd>
              </div>
              <div>
                <dt>신고 건수</dt>
                <dd>{{ risk.reportCount }}건</dd>
              </div>
              <div>
                <dt>소유자 확인</dt>
                <dd>{{ risk.ownerVerified ? '확인됨' : '미확인' }}</dd>
              </div>
            </dl>
            <p v-if="risk.label === 'UNKNOWN'" class="risk-notice">
              비교 가능한 주변 매물이 부족해 분석이 어렵습니다.
            </p>
          </div>

          <div class="panel-actions">
            <button
              type="button"
              :class="{ active: isFavorite }"
              :disabled="isToggling"
              @click="handleFavorite"
            >
              {{ isFavorite ? '♥ 찜 해제' : '♡ 찜하기' }}
            </button>
            <button
              class="ghost"
              type="button"
              :class="{ 'compare-active': compareStore.has(property.propertyId) }"
              :disabled="compareStore.isFull && !compareStore.has(property.propertyId)"
              @click="compareStore.toggle(property)"
            >
              {{ compareStore.has(property.propertyId) ? '✓ 비교함에서 제거' : '+ 비교 추가' }}
            </button>
          </div>
        </aside>
      </div>

      <div v-else class="placeholder">
        <strong>존재하지 않는 매물입니다</strong>
        <p>목록에서 다른 매물을 선택해 주세요.</p>
      </div>
    </section>
  </main>
</template>

<style lang="scss" scoped>
.detail-page {
  padding: 44px 0 76px;
}

.back-link {
  display: inline-flex;
  margin-bottom: 18px;
  color: var(--color-primary);
  font-size: 14px;
  font-weight: 900;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.8fr);
  gap: 24px;
  align-items: start;
}

.gallery-shell,
.info-panel,
.placeholder {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.gallery-shell {
  position: relative;
  overflow: hidden;
  aspect-ratio: 16 / 10;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.gallery-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-soft);
  color: var(--color-muted);
  font-size: 14px;
  font-weight: 700;
}

.info-panel {
  display: grid;
  gap: 16px;
  padding: 26px;

  h1 {
    color: var(--color-heading);
    font-size: 26px;
    font-weight: 900;
    line-height: 1.28;
  }

  strong {
    color: var(--color-primary-dark);
    font-size: 24px;
    font-weight: 900;
  }

  p {
    color: var(--color-muted);
    font-weight: 700;
  }

  dl {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    padding-top: 8px;
  }

  dt {
    color: var(--color-muted);
    font-size: 12px;
    font-weight: 800;
  }

  dd {
    margin-top: 4px;
    color: var(--color-heading);
    font-weight: 900;
  }
}

.risk-panel {
  border-radius: var(--radius-sm);
  padding: 16px;
  display: grid;
  gap: 12px;
}

.risk--safe    { background: #ecfdf3; }
.risk--caution { background: #fffbeb; }
.risk--danger  { background: #fff1f0; }
.risk--unknown { background: var(--color-surface-muted); }

.risk-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.risk-badge {
  border-radius: var(--radius-xs);
  font-size: 12px;
  font-weight: 900;
  padding: 4px 9px;

  .risk--safe &    { background: #027a48; color: #fff; }
  .risk--caution & { background: #b45309; color: #fff; }
  .risk--danger &  { background: var(--color-danger); color: #fff; }
  .risk--unknown & { background: var(--color-muted); color: #fff; }
}

.risk-score {
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 800;
}

.risk-detail {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;

  dt {
    color: var(--color-muted);
    font-size: 12px;
    font-weight: 800;
  }

  dd {
    margin-top: 3px;
    color: var(--color-heading);
    font-size: 14px;
    font-weight: 900;
  }
}

.risk-notice {
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 700;
}

.panel-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;

  button {
    height: 46px;
    border-radius: var(--radius-sm);
    background: var(--color-primary);
    color: var(--color-surface);
    font-weight: 900;
  }

  .ghost {
    border: 1px solid var(--color-border);
    background: var(--color-surface);
    color: var(--color-heading);
  }

  .compare-active {
    border-color: var(--color-primary);
    background: var(--color-primary-soft);
    color: var(--color-primary-dark);
  }

  .active {
    background: var(--color-danger);
  }
}

.placeholder {
  display: grid;
  gap: 8px;
  place-items: center;
  min-height: 280px;
  color: var(--color-muted);
  text-align: center;

  strong {
    color: var(--color-heading);
    font-size: 20px;
    font-weight: 900;
  }
}

@media (max-width: 900px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
}
</style>

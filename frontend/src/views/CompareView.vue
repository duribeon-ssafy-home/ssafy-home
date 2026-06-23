<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useCompareStore } from '@/stores/compare'
import { getProperty, getPropertyRisk } from '@/api/propertyApi'

const compareStore = useCompareStore()

const items = ref([]) // { property, risk }[]
const isLoading = ref(false)

const riskMeta = {
  SAFE:    { label: '안전', class: 'risk--safe' },
  CAUTION: { label: '주의', class: 'risk--caution' },
  DANGER:  { label: '위험', class: 'risk--danger' },
  UNKNOWN: { label: '분석 불가', class: 'risk--unknown' },
}

const roomTypeLabels = {
  ONE_ROOM: '원룸', TWO_ROOM: '투룸', OFFICETEL: '오피스텔', APARTMENT: '아파트',
}

async function fetchAll() {
  if (compareStore.ids.length === 0) { items.value = []; return }
  isLoading.value = true
  try {
    items.value = await Promise.all(
      compareStore.ids.map(async (id) => {
        const [prop, risk] = await Promise.allSettled([getProperty(id), getPropertyRisk(id)])
        return {
          property: prop.status === 'fulfilled' ? prop.value : null,
          risk: risk.status === 'fulfilled' ? risk.value : null,
        }
      }),
    )
  } finally {
    isLoading.value = false
  }
}

function highlight(values, mode) {
  const nums = values.map(Number).filter(Number.isFinite)
  if (nums.length < 2) return () => false
  const target = mode === 'min' ? Math.min(...nums) : Math.max(...nums)
  if (nums.every((n) => n === target)) return () => false
  return (v) => Number(v) === target
}

const isLowestDeposit = computed(() =>
  highlight(items.value.map((i) => i.property?.deposit), 'min'),
)
const isLowestRent = computed(() =>
  highlight(
    items.value.filter((i) => i.property?.rentType === 'MONTHLY').map((i) => i.property?.monthlyRent),
    'min',
  ),
)
const isLowestMgmt = computed(() =>
  highlight(items.value.map((i) => i.property?.managementFee), 'min'),
)
const isLargestArea = computed(() =>
  highlight(items.value.map((i) => i.property?.area), 'max'),
)
const isNewest = computed(() =>
  highlight(items.value.map((i) => i.property?.buildYear), 'max'),
)
const isLowestRiskScore = computed(() =>
  highlight(
    items.value.map((i) => (i.risk?.label !== 'UNKNOWN' ? i.risk?.score : null)),
    'min',
  ),
)
const isBestGapRate = computed(() =>
  highlight(items.value.map((i) => i.risk?.priceGapRate), 'min'),
)

function formatMoney(v) {
  const n = Number(v)
  return Number.isFinite(n) ? `${n.toLocaleString('ko-KR')}만` : '—'
}

function formatGapRate(v) {
  if (v == null || !Number.isFinite(Number(v))) return '—'
  const pct = Number(v).toFixed(1)
  return `${Number(v) > 0 ? '+' : ''}${pct}%`
}

onMounted(fetchAll)
watch(() => compareStore.ids, fetchAll, { deep: true })
</script>

<template>
  <main class="page compare-page">
    <section class="section-container">
      <div class="compare-header">
        <div>
          <p class="eyebrow">Compare</p>
          <h1>매물 비교</h1>
        </div>
        <RouterLink class="back-link" :to="{ name: 'home' }">← 매물 목록으로</RouterLink>
      </div>

      <div v-if="compareStore.count < 2 && !isLoading" class="empty-state">
        <strong>비교할 매물을 2개 이상 선택해주세요</strong>
        <p>매물 카드의 <strong>+</strong> 버튼을 눌러 비교함에 담으세요. 최대 4개까지 가능합니다.</p>
        <div class="empty-actions">
          <RouterLink :to="{ name: 'home' }">매물 목록 보기</RouterLink>
          <RouterLink :to="{ name: 'map' }">지도로 보기</RouterLink>
        </div>
      </div>

      <div v-else-if="isLoading" class="loading-state">매물 정보를 불러오는 중...</div>

      <div v-else class="compare-table-wrap">
        <table class="compare-table">
          <colgroup>
            <col class="label-col" />
            <col v-for="item in items" :key="item.property?.propertyId" />
          </colgroup>

          <thead>
            <tr>
              <th class="row-label corner-cell" />
              <th v-for="item in items" :key="item.property?.propertyId" class="prop-col">
                <div class="prop-header">
                  <button
                    class="remove-btn"
                    type="button"
                    @click="compareStore.toggle(item.property?.propertyId)"
                  >✕</button>
                  <div class="prop-thumb">
                    <img
                      v-if="item.property?.images?.[0]?.imageUrl"
                      :src="item.property.images[0].imageUrl"
                      :alt="item.property.title"
                    />
                    <div v-else class="prop-thumb-placeholder" />
                  </div>
                  <RouterLink
                    class="prop-title"
                    :to="{ name: 'property-detail', params: { id: item.property?.propertyId } }"
                  >{{ item.property?.title ?? '—' }}</RouterLink>
                  <p class="prop-addr">{{ item.property?.gugun }} {{ item.property?.dong }}</p>
                </div>
              </th>
            </tr>
          </thead>

          <tbody>
            <tr>
              <td class="row-label">거래유형</td>
              <td v-for="item in items" :key="item.property?.propertyId">
                {{ item.property?.rentType === 'JEONSE' ? '전세' : item.property?.rentType === 'MONTHLY' ? '월세' : '—' }}
              </td>
            </tr>
            <tr>
              <td class="row-label">보증금 / 전세금</td>
              <td
                v-for="item in items"
                :key="item.property?.propertyId"
                :class="{ highlight: isLowestDeposit(item.property?.deposit) }"
              >{{ formatMoney(item.property?.deposit) }}</td>
            </tr>
            <tr>
              <td class="row-label">월세</td>
              <td
                v-for="item in items"
                :key="item.property?.propertyId"
                :class="{ highlight: item.property?.rentType === 'MONTHLY' && isLowestRent(item.property?.monthlyRent) }"
              >
                {{ item.property?.rentType === 'MONTHLY' ? formatMoney(item.property?.monthlyRent) : '—' }}
              </td>
            </tr>
            <tr>
              <td class="row-label">관리비</td>
              <td
                v-for="item in items"
                :key="item.property?.propertyId"
                :class="{ highlight: isLowestMgmt(item.property?.managementFee) }"
              >{{ item.property?.managementFee != null ? formatMoney(item.property?.managementFee) : '—' }}</td>
            </tr>
            <tr>
              <td class="row-label">방 타입</td>
              <td v-for="item in items" :key="item.property?.propertyId">
                {{ roomTypeLabels[item.property?.roomType] ?? '—' }}
              </td>
            </tr>
            <tr>
              <td class="row-label">면적</td>
              <td
                v-for="item in items"
                :key="item.property?.propertyId"
                :class="{ highlight: isLargestArea(item.property?.area) }"
              >{{ item.property?.area != null ? `${Number(item.property.area).toFixed(1)}m²` : '—' }}</td>
            </tr>
            <tr>
              <td class="row-label">층수</td>
              <td v-for="item in items" :key="item.property?.propertyId">
                {{ item.property?.floor != null ? `${item.property.floor}층` : '—' }}
              </td>
            </tr>
            <tr>
              <td class="row-label">건축연도</td>
              <td
                v-for="item in items"
                :key="item.property?.propertyId"
                :class="{ highlight: isNewest(item.property?.buildYear) }"
              >{{ item.property?.buildYear != null ? `${item.property.buildYear}년` : '—' }}</td>
            </tr>

            <tr class="section-divider">
              <td :colspan="1 + items.length">위험 분석</td>
            </tr>

            <tr>
              <td class="row-label">위험 등급</td>
              <td v-for="item in items" :key="item.property?.propertyId">
                <span v-if="item.risk" class="risk-badge" :class="riskMeta[item.risk.label]?.class">
                  {{ riskMeta[item.risk.label]?.label ?? '—' }}
                </span>
                <span v-else>—</span>
              </td>
            </tr>
            <tr>
              <td class="row-label">위험 점수</td>
              <td
                v-for="item in items"
                :key="item.property?.propertyId"
                :class="{ highlight: item.risk?.label !== 'UNKNOWN' && isLowestRiskScore(item.risk?.score) }"
              >{{ item.risk?.label !== 'UNKNOWN' && item.risk?.score != null ? `${item.risk.score}점` : '—' }}</td>
            </tr>
            <tr>
              <td class="row-label">주변 평균 시세</td>
              <td v-for="item in items" :key="item.property?.propertyId">
                <template v-if="item.property?.rentType === 'JEONSE'">
                  {{ item.risk?.marketPriceAvg != null ? `전세 ${Number(item.risk.marketPriceAvg).toLocaleString('ko-KR')}만원` : '—' }}
                </template>
                <template v-else-if="item.risk?.avgMonthlyRent != null">
                  {{ `월세 ${Number(item.risk.avgMonthlyRent).toLocaleString('ko-KR')}만 / 보증금 ${Number(item.risk.avgDeposit).toLocaleString('ko-KR')}만` }}
                </template>
                <template v-else>—</template>
              </td>
            </tr>
            <tr>
              <td class="row-label">시세 대비</td>
              <td
                v-for="item in items"
                :key="item.property?.propertyId"
                :class="{ highlight: isBestGapRate(item.risk?.priceGapRate) }"
              >{{ formatGapRate(item.risk?.priceGapRate) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </main>
</template>

<style lang="scss" scoped>
.compare-page {
  padding: 44px 0 80px;
}

.compare-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 28px;

  h1 {
    color: var(--color-heading);
    font-size: 36px;
    font-weight: 900;
    margin-top: 6px;
  }
}

.back-link {
  color: var(--color-primary);
  font-size: 14px;
  font-weight: 800;
}

.empty-state {
  display: grid;
  gap: 14px;
  place-items: center;
  min-height: 360px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  text-align: center;
  padding: 40px;

  strong { font-size: 22px; font-weight: 900; color: var(--color-heading); }
  p { color: var(--color-muted); font-weight: 700; line-height: 1.7; }
}

.empty-actions {
  display: flex;
  gap: 10px;
  margin-top: 4px;

  a {
    height: 42px;
    padding: 0 20px;
    border-radius: var(--radius-sm);
    background: var(--color-primary);
    color: var(--color-surface);
    font-weight: 900;
    font-size: 14px;
    display: flex;
    align-items: center;

    &:last-child {
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      color: var(--color-heading);
    }
  }
}

.loading-state {
  text-align: center;
  padding: 60px;
  color: var(--color-muted);
  font-weight: 700;
}

.compare-table-wrap { overflow-x: auto; }

.compare-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 560px;

  .label-col { width: 130px; }
}

.corner-cell { background: var(--color-bg-soft); }

.prop-col {
  border: 1px solid var(--color-border);
  border-bottom: none;
  min-width: 190px;
}

.prop-header {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 14px 14px 16px;
}

.remove-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--color-border);
  color: var(--color-heading);
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color var(--transition-fast);

  &:hover { background: var(--color-danger); color: white; }
}

.prop-thumb {
  width: 100%;
  height: 120px;
  border-radius: var(--radius-sm);
  overflow: hidden;

  img { width: 100%; height: 100%; object-fit: cover; }
}

.prop-thumb-placeholder {
  width: 100%;
  height: 100%;
  background: var(--color-bg-soft);
}

.prop-title {
  font-size: 14px;
  font-weight: 900;
  color: var(--color-heading);
  text-align: center;
  line-height: 1.35;

  &:hover { color: var(--color-primary); }
}

.prop-addr {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-muted);
}

tbody tr td {
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 700;
  color: var(--color-heading);
  border: 1px solid var(--color-border);
  text-align: center;
}

.row-label {
  padding: 12px 16px;
  font-size: 12px;
  font-weight: 800;
  color: var(--color-muted);
  background: var(--color-bg-soft);
  text-align: left;
  border: 1px solid var(--color-border);
  white-space: nowrap;
}

.section-divider td {
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
  font-size: 12px;
  font-weight: 900;
  text-align: left;
  padding: 8px 16px;
  border: 1px solid var(--color-border);
}

.highlight {
  background: #ecfdf3;
  font-weight: 900;
  color: #027a48;
}

.risk-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: var(--radius-xs);
  font-size: 12px;
  font-weight: 900;

  &.risk--safe    { background: #027a48; color: white; }
  &.risk--caution { background: #b45309; color: white; }
  &.risk--danger  { background: var(--color-danger); color: white; }
  &.risk--unknown { background: var(--color-muted); color: white; }
}
</style>

<script setup>
import { computed, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { rentTypeLabels, roomTypeLabels } from '@/data/mockProperties'
import { useFavorites } from '@/composables/useFavorites'
import { useAuthStore } from '@/stores/auth'
import { useCompareStore } from '@/stores/compare'

const props = defineProps({
  property: {
    type: Object,
    required: true,
  },
})

const router = useRouter()
const authStore = useAuthStore()
const { toggleFavorite: toggle, isFavorited } = useFavorites()
const compareStore = useCompareStore()
const isToggling = ref(false)

const isComparing = computed(() => compareStore.has(props.property.propertyId))

const isFavorite = computed(() => isFavorited(props.property.propertyId))
const primaryImage = computed(() => props.property.images?.[0]?.imageUrl || '')

const priceLabel = computed(() => {
  if (props.property.rentType === 'JEONSE') {
    return `전세 ${formatMoneyManwon(props.property.deposit)}`
  }
  if (props.property.rentType === 'SEMI_JEONSE') {
    return `반전세 ${formatMoneyManwon(props.property.monthlyRent)} / 보증금 ${formatMoneyManwon(
      props.property.deposit,
    )}`
  }
  return `월세 ${formatMoneyManwon(props.property.monthlyRent)} / 보증금 ${formatMoneyManwon(
    props.property.deposit,
  )}`
})

const specLabel = computed(() => {
  const roomType = roomTypeLabels[props.property.roomType] || props.property.roomType
  const area = Number(props.property.area).toFixed(1)
  const mgmt = props.property.managementFee != null ? ` · 관리비 ${props.property.managementFee}만` : ''
  return `${roomType} · ${area}m2 · ${props.property.floor}층${mgmt}`
})

function formatMoneyManwon(value) {
  const amount = Number(value)
  return Number.isFinite(amount) ? `${amount.toLocaleString('ko-KR')}만` : '-'
}

async function toggleFavorite(e) {
  e.preventDefault()
  if (!authStore.isAuthenticated) {
    router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  if (isToggling.value) return
  isToggling.value = true
  try {
    await toggle(props.property.propertyId)
  } finally {
    isToggling.value = false
  }
}
</script>

<template>
  <article class="property-card">
    <RouterLink
      class="property-link"
      :to="{ name: 'property-detail', params: { id: property.propertyId } }"
    >
      <div class="image-wrap">
        <img v-if="primaryImage" :src="primaryImage" :alt="property.title" />
        <div v-else class="img-placeholder" />
        <span class="rent-badge">{{ rentTypeLabels[property.rentType] }}</span>
        <span v-if="property.dataSource === 'AGENT'" class="unreviewed-badge">미검증</span>
      </div>

      <div class="content">
        <div class="price-row">
          <strong>{{ priceLabel }}</strong>
        </div>
        <h3>{{ property.title }}</h3>
        <p class="spec">{{ specLabel }}</p>
        <p class="address">{{ property.sido }} {{ property.gugun }} {{ property.dong }}</p>
        <div class="tags">
          <span v-for="tag in property.tags" :key="tag">{{ tag }}</span>
        </div>
      </div>
    </RouterLink>

    <button
      class="favorite-button"
      :class="{ 'favorite-button--active': isFavorite }"
      type="button"
      :aria-label="isFavorite ? '찜 해제' : '찜하기'"
      @click="toggleFavorite"
    >
      {{ isFavorite ? '♥' : '♡' }}
    </button>

    <button
      class="compare-button"
      :class="{ 'compare-button--active': isComparing }"
      :disabled="compareStore.isFull && !isComparing"
      type="button"
      :aria-label="isComparing ? '비교 제거' : '비교 추가'"
      @click.prevent="compareStore.toggle(property)"
    >
      {{ isComparing ? '✓' : '+' }}
    </button>
  </article>
</template>

<style lang="scss" scoped>
.property-card {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(229, 231, 235, 0.9);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
  transition:
    box-shadow var(--transition-fast),
    transform var(--transition-fast),
    border-color var(--transition-fast);

  &:hover {
    border-color: rgba(54, 95, 145, 0.22);
    box-shadow: var(--shadow-card-hover);
    transform: translateY(-4px);

    img {
      transform: scale(1.035);
    }
  }
}

.property-link {
  display: block;
  height: 100%;
}

.image-wrap {
  position: relative;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  background: var(--color-bg-soft);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 320ms ease;
  }
}

.img-placeholder {
  width: 100%;
  height: 100%;
  background: var(--color-bg-soft);
}

.rent-badge {
  position: absolute;
  left: 14px;
  bottom: 14px;
  border-radius: var(--radius-pill);
  background: rgba(17, 24, 39, 0.72);
  color: var(--color-surface);
  font-size: 12px;
  font-weight: 900;
  padding: 6px 9px;
}

.unreviewed-badge {
  position: absolute;
  right: 14px;
  bottom: 14px;
  border-radius: var(--radius-pill);
  background: rgba(217, 119, 6, 0.88);
  color: #fff;
  font-size: 11px;
  font-weight: 900;
  padding: 5px 8px;
}

.content {
  display: grid;
  gap: 8px;
  padding: 18px;
}

.price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;

  strong {
    color: var(--color-heading);
    font-size: 19px;
    font-weight: 900;
    letter-spacing: var(--ls-tight);
  }
}

h3 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: var(--color-heading);
  font-size: 16px;
  font-weight: 800;
  line-height: var(--lh-snug);
}

.spec,
.address {
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 700;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  padding-top: 4px;

  span {
    border: 1px solid var(--color-border);
    border-radius: var(--radius-pill);
    background: var(--color-surface-muted);
    color: var(--color-muted);
    font-size: 12px;
    font-weight: 800;
    padding: 5px 8px;
  }
}

.favorite-button {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 38px;
  height: 38px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.88);
  color: var(--color-heading);
  font-size: 19px;
  line-height: 1;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.16);
  transition:
    background-color var(--transition-fast),
    color var(--transition-fast),
    transform var(--transition-fast);

  &:hover {
    transform: translateY(-1px);
  }
}

.favorite-button--active {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.compare-button {
  position: absolute;
  top: 58px;
  right: 12px;
  width: 38px;
  height: 38px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.88);
  color: var(--color-heading);
  font-size: 18px;
  font-weight: 900;
  line-height: 1;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.16);
  transition:
    background-color var(--transition-fast),
    color var(--transition-fast),
    transform var(--transition-fast);

  &:hover:not(:disabled) {
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

.compare-button--active {
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
}
</style>

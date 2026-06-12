<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useFavorites } from '@/composables/useFavorites'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  property: { type: Object, required: true },
  selected: { type: Boolean, default: false },
})
const emit = defineEmits(['select'])

const router = useRouter()
const authStore = useAuthStore()
const { toggleFavorite: toggle, isFavorited } = useFavorites()
const isToggling = ref(false)

const rentTypeLabels = { JEONSE: '전세', MONTHLY: '월세' }
const roomTypeLabels = {
  ONE_ROOM: '원룸', TWO_ROOM: '투룸', OFFICETEL: '오피스텔', APARTMENT: '아파트',
}

const isFavorite = computed(() => isFavorited(props.property.propertyId))
const primaryImage = computed(() => props.property.images?.[0]?.imageUrl || '')

const priceLabel = computed(() => {
  if (props.property.rentType === 'JEONSE') {
    return `전세 ${Number(props.property.deposit).toLocaleString('ko-KR')}만`
  }
  return `월세 ${props.property.monthlyRent}만 / 보증금 ${Number(props.property.deposit).toLocaleString('ko-KR')}만`
})

const specLabel = computed(() => {
  const room = roomTypeLabels[props.property.roomType] || props.property.roomType
  const area = Number(props.property.area).toFixed(1)
  return `${room} · ${area}m² · ${props.property.floor}층`
})

async function onFavoriteClick(e) {
  e.stopPropagation()
  if (!authStore.isAuthenticated) {
    router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  if (isToggling.value) return
  isToggling.value = true
  try { await toggle(props.property.propertyId) }
  finally { isToggling.value = false }
}
</script>

<template>
  <article
    class="map-property-card"
    :class="{ 'map-property-card--selected': selected }"
    @click="emit('select', property.propertyId)"
  >
    <div class="thumb">
      <img v-if="primaryImage" :src="primaryImage" :alt="property.title" />
      <div v-else class="thumb-placeholder" />
      <span class="rent-badge">{{ rentTypeLabels[property.rentType] }}</span>
    </div>

    <div class="info">
      <p class="price">{{ priceLabel }}</p>
      <p class="title">{{ property.title }}</p>
      <p class="spec">{{ specLabel }}</p>
      <p class="addr">{{ property.gugun }} {{ property.dong }}</p>
    </div>

    <button
      class="fav-btn"
      :class="{ 'fav-btn--active': isFavorite }"
      type="button"
      :aria-label="isFavorite ? '찜 해제' : '찜하기'"
      @click="onFavoriteClick"
    >{{ isFavorite ? '♥' : '♡' }}</button>
  </article>
</template>

<style lang="scss" scoped>
.map-property-card {
  display: flex;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  cursor: pointer;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);

  &:hover { border-color: var(--color-primary); box-shadow: 0 4px 14px rgba(54, 95, 145, 0.12); }
}

.map-property-card--selected {
  border: 2px solid var(--color-primary);
  box-shadow: 0 0 0 3px rgba(54, 95, 145, 0.15);
}

.thumb {
  position: relative;
  width: 100px;
  height: 90px;
  flex-shrink: 0;

  img, .thumb-placeholder {
    width: 100%;
    height: 100%;
    object-fit: cover;
    background: var(--color-bg-soft);
  }
}

.rent-badge {
  position: absolute;
  bottom: 6px;
  left: 6px;
  background: rgba(15, 23, 42, 0.7);
  color: var(--color-surface);
  font-size: 10px;
  font-weight: 800;
  padding: 2px 6px;
  border-radius: var(--radius-xs);
}

.info {
  flex: 1;
  padding: 10px 10px 8px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
  overflow: hidden;
}

.price {
  color: var(--color-heading);
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.title {
  color: var(--color-text);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.spec { color: var(--color-muted); font-size: 11px; font-weight: 700; }
.addr { color: var(--color-subtle); font-size: 11px; font-weight: 700; }

.fav-btn {
  padding: 10px 10px 10px 4px;
  display: flex;
  align-items: flex-start;
  flex-shrink: 0;
  color: var(--color-subtle);
  font-size: 17px;
  line-height: 1;
  transition: color var(--transition-fast);

  &:hover { color: var(--color-danger); }
}

.fav-btn--active { color: var(--color-danger); }
</style>

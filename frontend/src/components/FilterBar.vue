<script setup>
import { nextTick, reactive, ref, watch } from 'vue'
import { searchLocations } from '@/api/locationApi'
import { roomTypeLabels } from '@/data/mockProperties'

const props = defineProps({
  initialFilters: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['search'])

const defaultFilters = {
  location: '',
  deposit: '',
  monthlyRent: '',
  roomType: 'ALL',
}

const filters = reactive({
  ...defaultFilters,
  ...props.initialFilters,
})

const roomTypes = [
  { value: 'ALL', label: '전체' },
  { value: 'ONE_ROOM', label: roomTypeLabels.ONE_ROOM },
  { value: 'TWO_ROOM', label: roomTypeLabels.TWO_ROOM },
  { value: 'OFFICETEL', label: roomTypeLabels.OFFICETEL },
]

const depositPresets = [
  { value: '300', label: '300만' },
  { value: '500', label: '500만' },
  { value: '1000', label: '1,000만' },
]

const rentPresets = [
  { value: '30', label: '30만' },
  { value: '50', label: '50만' },
  { value: '70', label: '70만' },
]

const openAmountMenu = ref('')
const locationSuggestions = ref([])
const isLocationMenuOpen = ref(false)
const isLocationLoading = ref(false)
const selectedLocation = ref(null)
const locationSearchTimer = ref(null)
const activeLocationIndex = ref(-1)
const locationMenuRef = ref(null)
let locationRequestId = 0

function selectPreset(field, value) {
  filters[field] = value
  openAmountMenu.value = ''
}

function selectLocation(location) {
  selectedLocation.value = location
  filters.location = location.fullName
  locationSuggestions.value = []
  isLocationMenuOpen.value = false
  isLocationLoading.value = false
  activeLocationIndex.value = -1
  if (locationSearchTimer.value) {
    window.clearTimeout(locationSearchTimer.value)
  }
  locationRequestId += 1
}

function closeLocationMenuSoon() {
  window.setTimeout(() => {
    isLocationMenuOpen.value = false
  }, 120)
}

function toggleAmountMenu(field) {
  openAmountMenu.value = openAmountMenu.value === field ? '' : field
}

function submitSearch() {
  openAmountMenu.value = ''
  isLocationMenuOpen.value = false
  activeLocationIndex.value = -1
  emit('search', {
    ...filters,
    locationParts: selectedLocation.value,
  })
}

function handleLocationInput(event) {
  const keyword = event.target.value
  if (filters.location !== keyword) {
    filters.location = keyword
  }
  scheduleLocationSearch(keyword)
}

function scheduleLocationSearch(keyword) {
  if (selectedLocation.value?.fullName === keyword) {
    locationSuggestions.value = []
    isLocationMenuOpen.value = false
    isLocationLoading.value = false
    activeLocationIndex.value = -1
    return
  }

  selectedLocation.value = null
  activeLocationIndex.value = -1

  if (locationSearchTimer.value) {
    window.clearTimeout(locationSearchTimer.value)
  }

  const normalizedKeyword = String(keyword || '').trim()
  if (normalizedKeyword.length < 2) {
    locationSuggestions.value = []
    isLocationMenuOpen.value = false
    isLocationLoading.value = false
    activeLocationIndex.value = -1
    return
  }

  isLocationMenuOpen.value = true
  isLocationLoading.value = true
  const requestId = ++locationRequestId
  locationSearchTimer.value = window.setTimeout(async () => {
    try {
      const suggestions = await searchLocations(normalizedKeyword)
      if (requestId !== locationRequestId) return
      locationSuggestions.value = suggestions
      isLocationMenuOpen.value = true
      isLocationLoading.value = false
      activeLocationIndex.value = suggestions.length ? 0 : -1
    } catch {
      if (requestId !== locationRequestId) return
      locationSuggestions.value = []
      isLocationMenuOpen.value = true
      isLocationLoading.value = false
      activeLocationIndex.value = -1
    }
  }, 220)
}

function handleLocationKeydown(event) {
  if (!isLocationMenuOpen.value) return

  const lastIndex = locationSuggestions.value.length - 1
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    if (lastIndex < 0) return
    activeLocationIndex.value =
      activeLocationIndex.value >= lastIndex ? 0 : activeLocationIndex.value + 1
    scrollActiveLocationIntoView()
  }

  if (event.key === 'ArrowUp') {
    event.preventDefault()
    if (lastIndex < 0) return
    activeLocationIndex.value =
      activeLocationIndex.value <= 0 ? lastIndex : activeLocationIndex.value - 1
    scrollActiveLocationIntoView()
  }

  if (event.key === 'Enter' && activeLocationIndex.value >= 0) {
    event.preventDefault()
    selectLocation(locationSuggestions.value[activeLocationIndex.value])
  }

  if (event.key === 'Escape') {
    isLocationMenuOpen.value = false
    activeLocationIndex.value = -1
  }
}

async function scrollActiveLocationIntoView() {
  await nextTick()
  locationMenuRef.value
    ?.querySelector(`[data-location-index="${activeLocationIndex.value}"]`)
    ?.scrollIntoView({ block: 'nearest' })
}

watch(
  () => props.initialFilters,
  (nextFilters) => {
    Object.assign(filters, defaultFilters, nextFilters || {})
    if (selectedLocation.value?.fullName !== filters.location) {
      selectedLocation.value = null
    }
  },
  { deep: true },
)

</script>

<template>
  <form class="filter-bar" @submit.prevent="submitSearch">
    <label class="field location-field">
      <span>지역</span>
      <div class="location-control">
        <input
          v-model="filters.location"
          data-testid="location-input"
          type="search"
          autocomplete="off"
          placeholder="동, 구, 시 이름으로 검색"
          @input="handleLocationInput"
          @focus="isLocationMenuOpen = filters.location.trim().length >= 2"
          @keydown="handleLocationKeydown"
          @blur="closeLocationMenuSoon"
        />
        <div
          v-if="isLocationMenuOpen"
          ref="locationMenuRef"
          class="location-menu"
          data-testid="location-suggestions"
        >
          <p v-if="isLocationLoading">지역 후보를 찾고 있습니다</p>
          <button
            v-for="(location, index) in locationSuggestions"
            :key="location.code"
            v-show="!isLocationLoading"
            type="button"
            :class="{ 'location-menu__item--active': activeLocationIndex === index }"
            :data-location-index="index"
            @mousedown.prevent="selectLocation(location)"
          >
            <strong>{{ location.fullName }}</strong>
          </button>
          <p v-if="!isLocationLoading && !locationSuggestions.length">입력한 지역명으로 검색</p>
        </div>
      </div>
    </label>

    <div class="field">
      <span>보증금 이하 <em>만원</em></span>
      <div class="amount-control">
        <input
          v-model="filters.deposit"
          data-testid="deposit-select"
          type="number"
          min="0"
          placeholder="예: 1000"
        />
        <span class="amount-unit">만원</span>
        <button
          class="amount-menu-button"
          type="button"
          aria-label="보증금 주요 금액 선택"
          :aria-expanded="openAmountMenu === 'deposit'"
          @click="toggleAmountMenu('deposit')"
        >
          ▾
        </button>
        <div v-if="openAmountMenu === 'deposit'" class="amount-menu">
          <button
            v-for="p in depositPresets"
            :key="p.value"
            type="button"
            :class="{ 'amount-menu__item--active': filters.deposit === p.value }"
            @click="selectPreset('deposit', p.value)"
          >
            {{ p.label }}
          </button>
        </div>
      </div>
    </div>

    <div class="field">
      <span>월세 이하 <em>만원</em></span>
      <div class="amount-control">
        <input
          v-model="filters.monthlyRent"
          data-testid="monthly-rent-select"
          type="number"
          min="0"
          placeholder="예: 50"
        />
        <span class="amount-unit">만원</span>
        <button
          class="amount-menu-button"
          type="button"
          aria-label="월세 주요 금액 선택"
          :aria-expanded="openAmountMenu === 'monthlyRent'"
          @click="toggleAmountMenu('monthlyRent')"
        >
          ▾
        </button>
        <div v-if="openAmountMenu === 'monthlyRent'" class="amount-menu">
          <button
            v-for="p in rentPresets"
            :key="p.value"
            type="button"
            :class="{ 'amount-menu__item--active': filters.monthlyRent === p.value }"
            @click="selectPreset('monthlyRent', p.value)"
          >
            {{ p.label }}
          </button>
        </div>
      </div>
    </div>

    <div class="room-type" role="group" aria-label="방 타입">
      <span>방 타입</span>
      <div class="chips">
        <button
          v-for="roomType in roomTypes"
          :key="roomType.value"
          class="filter-chip"
          :data-testid="`room-type-${roomType.value}`"
          :class="{ 'filter-chip--active': filters.roomType === roomType.value }"
          type="button"
          @click="filters.roomType = roomType.value"
        >
          {{ roomType.label }}
        </button>
      </div>
    </div>

    <button class="search-button" data-testid="search-button" type="submit">검색하기</button>
  </form>
</template>

<style lang="scss" scoped>
.filter-bar {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr 1.45fr auto;
  gap: 14px;
  align-items: stretch;
  padding: 18px;
  border: 1px solid rgba(208, 213, 221, 0.9);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-panel);
}

.field,
.room-type {
  display: flex;
  flex-direction: column;
  gap: 7px;

  > span {
    color: var(--color-muted);
    font-size: 12px;
    font-weight: 800;
    flex-shrink: 0;

    em {
      color: var(--color-primary-dark);
      font-style: normal;
      font-weight: 900;
    }
  }

  > input,
  > .location-control,
  > .chips {
    margin-top: auto;
  }
}

input[type='search'],
input[type='number'] {
  width: 100%;
  height: 40px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-heading);
  font-size: 14px;
  font-weight: 700;
  padding: 0 13px;
  outline: none;
  transition:
    border-color var(--transition-fast),
    box-shadow var(--transition-fast);

  &:focus {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 4px rgba(54, 95, 145, 0.12);
  }

  &::-webkit-inner-spin-button,
  &::-webkit-outer-spin-button {
    opacity: 0.5;
  }
}

.amount-control {
  position: relative;
  display: flex;
  margin-top: auto;

  input[type='number'] {
    padding-right: 82px;
  }
}

.location-control {
  position: relative;
}

.location-menu {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  z-index: 120;
  display: grid;
  width: min(340px, 100%);
  max-height: 260px;
  overflow-y: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.14);
  overscroll-behavior: contain;

  button {
    display: flex;
    align-items: center;
    min-height: 46px;
    background: var(--color-surface);
    color: var(--color-heading);
    text-align: left;
    padding: 0 13px;
    transition:
      background-color var(--transition-fast),
      color var(--transition-fast);

    &:hover {
      background: var(--color-primary-soft);
      color: var(--color-primary-dark);
    }
  }

  strong {
    font-size: 14px;
    font-weight: 900;
  }

  p {
    color: var(--color-muted);
    font-size: 12px;
    font-weight: 800;
  }

  p {
    margin: 0;
    padding: 14px;
  }
}

.location-menu__item--active {
  background: var(--color-primary-soft) !important;
  color: var(--color-primary-dark) !important;
}

.amount-unit {
  position: absolute;
  top: 7px;
  right: 42px;
  display: inline-grid;
  place-items: center;
  height: 26px;
  min-width: 34px;
  border-radius: var(--radius-xs);
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
  font-size: 12px;
  font-weight: 900;
  pointer-events: none;
}

.amount-menu-button {
  position: absolute;
  top: 5px;
  right: 5px;
  width: 30px;
  height: 30px;
  border-radius: var(--radius-xs);
  background: var(--color-bg-soft);
  color: var(--color-heading);
  font-size: 13px;
  font-weight: 900;
  transition:
    background-color var(--transition-fast),
    color var(--transition-fast);

  &:hover {
    background: var(--color-primary-soft);
    color: var(--color-primary-dark);
  }
}

.amount-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 10;
  display: grid;
  width: min(160px, 100%);
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.14);

  button {
    height: 38px;
    background: var(--color-surface);
    color: var(--color-heading);
    font-size: 13px;
    font-weight: 800;
    text-align: left;
    padding: 0 12px;

    &:hover {
      background: var(--color-primary-soft);
      color: var(--color-primary-dark);
    }
  }
}

.amount-menu__item--active {
  background: var(--color-primary-soft) !important;
  color: var(--color-primary-dark) !important;
}

.chips {
  display: flex;
  gap: 8px;
}

.filter-chip {
  height: 40px;
  flex: 0 0 auto;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 800;
  padding: 0 13px;
  transition:
    background-color var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast),
    transform var(--transition-fast);

  &:hover {
    transform: translateY(-1px);
  }
}

.filter-chip--active {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
}

.search-button {
  align-self: end;
  height: 46px;
  min-width: 112px;
  border-radius: var(--radius-sm);
  background: var(--color-primary);
  color: var(--color-surface);
  font-size: 14px;
  font-weight: 900;
  padding: 0 18px;
  transition:
    background-color var(--transition-fast),
    transform var(--transition-fast),
    box-shadow var(--transition-fast);

  &:hover {
    background: var(--color-primary-dark);
    box-shadow: 0 12px 24px rgba(54, 95, 145, 0.2);
    transform: translateY(-1px);
  }
}

@media (max-width: 1040px) {
  .filter-bar {
    grid-template-columns: 1fr 1fr;
  }

  .room-type,
  .search-button {
    grid-column: 1 / -1;
  }
}

@media (max-width: 640px) {
  .filter-bar {
    grid-template-columns: 1fr;
    padding: 14px;
  }

  .field,
  .room-type,
  .search-button {
    grid-column: auto;
  }
}
</style>

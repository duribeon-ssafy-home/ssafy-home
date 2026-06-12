<script setup>
import { reactive, ref } from 'vue'
import RangeSlider from '@/components/RangeSlider.vue'

const emit = defineEmits(['search'])

const SIDO_ALIASES = {
  '서울': '서울특별시', '서울시': '서울특별시',
  '부산': '부산광역시', '부산시': '부산광역시',
  '대구': '대구광역시', '인천': '인천광역시',
  '광주': '광주광역시', '대전': '대전광역시',
  '울산': '울산광역시', '세종': '세종특별자치시',
  '경기': '경기도', '강원': '강원특별자치도',
  '충북': '충청북도', '충남': '충청남도',
  '전남': '전라남도', '전북': '전북특별자치도',
  '경남': '경상남도', '경북': '경상북도',
  '제주': '제주특별자치도',
}

function resolveLocation(input) {
  const v = input.trim()
  if (!v) return {}

  const parts = v.split(/\s+/)

  if (parts.length === 1) {
    const p = parts[0]
    if (SIDO_ALIASES[p]) return { sido: SIDO_ALIASES[p] }
    if (/(특별시|광역시|특별자치시|특별자치도|도)$/.test(p)) return { sido: p }
    if (/[구군시]$/.test(p)) return { gugun: p }
    return { dong: p }
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

const location = ref('')
const rentType = ref(null)
const roomType = ref(null)
const depositRange = ref([0, 100000])
const rentRange = ref([0, 500])
const areaRange = ref([0, 200])
const DEPOSIT_MAX = 100000
const RENT_MAX = 150
const AREA_MAX = 200

const depositInputs = reactive({ min: '', max: '' })
const rentInputs = reactive({ min: '', max: '' })
const areaInputs = reactive({ min: '', max: '' })

function syncDepositSlider([min, max]) {
  depositRange.value = [min, max]
  depositInputs.min = min > 0 ? String(min) : ''
  depositInputs.max = max < DEPOSIT_MAX ? String(max) : ''
}

function syncRentSlider([min, max]) {
  rentRange.value = [min, max]
  rentInputs.min = min > 0 ? String(min) : ''
  rentInputs.max = max < RENT_MAX ? String(max) : ''
}

function syncAreaSlider([min, max]) {
  areaRange.value = [min, max]
  areaInputs.min = min > 0 ? String(min) : ''
  areaInputs.max = max < AREA_MAX ? String(max) : ''
}

function onDepositInput() {
  const min = Number(depositInputs.min) || 0
  const max = Number(depositInputs.max) || DEPOSIT_MAX
  depositRange.value = [Math.min(min, max), Math.max(min, max)]
}

function onRentInput() {
  const min = Number(rentInputs.min) || 0
  const max = Number(rentInputs.max) || RENT_MAX
  rentRange.value = [Math.min(min, max), Math.max(min, max)]
}

function onAreaInput() {
  const min = Number(areaInputs.min) || 0
  const max = Number(areaInputs.max) || AREA_MAX
  areaRange.value = [Math.min(min, max), Math.max(min, max)]
}

function setRentType(value) {
  rentType.value = rentType.value === value ? null : value
  handleSearch()
}

function setRoomType(value) {
  roomType.value = roomType.value === value ? null : value
  handleSearch()
}

function handleSearch() {
  const params = {}
  Object.assign(params, resolveLocation(location.value))
  if (rentType.value) params.rentType = rentType.value
  if (roomType.value) params.roomType = roomType.value
  if (depositRange.value[0] > 0) params.minDeposit = depositRange.value[0]
  if (depositRange.value[1] < DEPOSIT_MAX) params.maxDeposit = depositRange.value[1]
  if (rentRange.value[0] > 0) params.minMonthlyRent = rentRange.value[0]
  if (rentRange.value[1] < RENT_MAX) params.maxMonthlyRent = rentRange.value[1]
  if (areaRange.value[0] > 0) params.minArea = areaRange.value[0]
  if (areaRange.value[1] < AREA_MAX) params.maxArea = areaRange.value[1]
  emit('search', params)
}
</script>

<template>
  <form class="detailed-filter-bar" @submit.prevent="handleSearch">
    <div class="filter-row">

      <div class="filter-group">
        <span class="filter-label">지역</span>
        <input v-model="location" class="filter-input" placeholder="서울 강남구, 역삼동..." />
      </div>

      <div class="filter-group">
        <span class="filter-label">거래유형</span>
        <div class="chips">
          <button type="button" class="chip" :class="{ 'chip--active': rentType === null }" @click="rentType = null; handleSearch()">전체</button>
          <button type="button" class="chip" :class="{ 'chip--active': rentType === 'JEONSE' }" data-testid="rent-JEONSE" @click="setRentType('JEONSE')">전세</button>
          <button type="button" class="chip" :class="{ 'chip--active': rentType === 'MONTHLY' }" data-testid="rent-MONTHLY" @click="setRentType('MONTHLY')">월세</button>
        </div>
      </div>

      <div class="filter-group">
        <span class="filter-label">방 타입</span>
        <div class="chips">
          <button
            v-for="rt in [
              { value: 'ONE_ROOM', label: '원룸' },
              { value: 'TWO_ROOM', label: '투룸' },
              { value: 'OFFICETEL', label: '오피스텔' },
              { value: 'APARTMENT', label: '아파트' },
            ]"
            :key="rt.value"
            type="button"
            class="chip"
            :class="{ 'chip--active': roomType === rt.value }"
            :data-testid="`room-${rt.value}`"
            @click="setRoomType(rt.value)"
          >{{ rt.label }}</button>
        </div>
      </div>

    </div>

    <div class="filter-row filter-row--ranges">

      <div class="filter-group filter-group--range">
        <span class="filter-label">보증금 (만원)</span>
        <div class="range-inputs">
          <input v-model="depositInputs.min" class="range-text" placeholder="최소" data-testid="deposit-min" @input="onDepositInput" />
          <span class="range-sep">~</span>
          <input v-model="depositInputs.max" class="range-text" placeholder="최대" data-testid="deposit-max" @input="onDepositInput" />
        </div>
        <div @pointerup="handleSearch" @touchend="handleSearch">
          <RangeSlider :min="0" :max="DEPOSIT_MAX" :step="100" :model-value="depositRange" @update:model-value="syncDepositSlider" />
        </div>
      </div>

      <div class="filter-group filter-group--range">
        <span class="filter-label">월세 (만원)</span>
        <div class="range-inputs">
          <input v-model="rentInputs.min" class="range-text" placeholder="최소" @input="onRentInput" />
          <span class="range-sep">~</span>
          <input v-model="rentInputs.max" class="range-text" placeholder="최대" @input="onRentInput" />
        </div>
        <div @pointerup="handleSearch" @touchend="handleSearch">
          <RangeSlider :min="0" :max="RENT_MAX" :step="5" :model-value="rentRange" @update:model-value="syncRentSlider" />
        </div>
      </div>

      <div class="filter-group filter-group--range">
        <span class="filter-label">면적 (m²)</span>
        <div class="range-inputs">
          <input v-model="areaInputs.min" class="range-text" placeholder="최소" @input="onAreaInput" />
          <span class="range-sep">~</span>
          <input v-model="areaInputs.max" class="range-text" placeholder="최대" @input="onAreaInput" />
        </div>
        <div @pointerup="handleSearch" @touchend="handleSearch">
          <RangeSlider :min="0" :max="AREA_MAX" :step="5" :model-value="areaRange" @update:model-value="syncAreaSlider" />
        </div>
      </div>

      <button class="search-btn" type="submit">검색</button>
    </div>
  </form>
</template>

<style lang="scss" scoped>
.detailed-filter-bar {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 24px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  align-items: flex-end;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-group--range { flex: 0 0 200px; }

.filter-label {
  color: var(--color-muted);
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.filter-input {
  height: 34px;
  min-width: 200px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-heading);
  font-size: 13px;
  font-weight: 700;
  padding: 0 12px;
  outline: none;
  transition: border-color var(--transition-fast);
  &:focus { border-color: var(--color-primary); }
}

.chips { display: flex; gap: 5px; }

.chip {
  height: 34px;
  padding: 0 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-muted);
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
  white-space: nowrap;
  transition: background-color var(--transition-fast), border-color var(--transition-fast), color var(--transition-fast);
  &:hover { border-color: var(--color-primary); color: var(--color-primary-dark); }
}

.chip--active {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
}

.range-inputs { display: flex; gap: 6px; align-items: center; }

.range-text {
  flex: 1;
  min-width: 0;
  height: 34px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-heading);
  font-size: 12px;
  font-weight: 700;
  padding: 0 8px;
  text-align: center;
  outline: none;
  transition: border-color var(--transition-fast);
  &:focus { border-color: var(--color-primary); }
}

.range-sep { color: var(--color-subtle); font-size: 13px; }

.search-btn {
  height: 34px;
  margin-bottom: 26px;
  padding: 0 20px;
  background: var(--color-primary);
  color: var(--color-surface);
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 900;
  cursor: pointer;
  transition: background-color var(--transition-fast), transform var(--transition-fast);
  white-space: nowrap;
  &:hover { background: var(--color-primary-dark); transform: translateY(-1px); }
}
</style>

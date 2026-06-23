<script setup>
import { reactive, ref, watch } from 'vue'
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

function selectPreset(field, value) {
  filters[field] = value
  openAmountMenu.value = ''
}

function toggleAmountMenu(field) {
  openAmountMenu.value = openAmountMenu.value === field ? '' : field
}

function submitSearch() {
  openAmountMenu.value = ''
  emit('search', { ...filters })
}

watch(
  () => props.initialFilters,
  (nextFilters) => {
    Object.assign(filters, defaultFilters, nextFilters || {})
  },
  { deep: true },
)
</script>

<template>
  <form class="filter-bar" @submit.prevent="submitSearch">
    <label class="field">
      <span>지역</span>
      <input
        v-model="filters.location"
        data-testid="location-input"
        type="search"
        placeholder="부산광역시, 사하구, 하단동"
      />
    </label>

    <div class="field">
      <span>보증금 이하</span>
      <div class="amount-control">
        <input
          v-model="filters.deposit"
          data-testid="deposit-select"
          type="number"
          min="0"
          placeholder="직접 입력"
        />
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
      <span>월세 이하</span>
      <div class="amount-control">
        <input
          v-model="filters.monthlyRent"
          data-testid="monthly-rent-select"
          type="number"
          min="0"
          placeholder="직접 입력"
        />
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
  }

  > input,
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
    padding-right: 43px;
  }
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

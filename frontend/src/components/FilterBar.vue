<script setup>
import { reactive } from 'vue'
import { roomTypeLabels } from '@/data/mockProperties'

const emit = defineEmits(['search'])

const filters = reactive({
  location: '',
  deposit: '',
  monthlyRent: '',
  roomType: 'ALL',
})

const roomTypes = [
  { value: 'ALL', label: '전체' },
  { value: 'ONE_ROOM', label: roomTypeLabels.ONE_ROOM },
  { value: 'TWO_ROOM', label: roomTypeLabels.TWO_ROOM },
  { value: 'OFFICETEL', label: roomTypeLabels.OFFICETEL },
]

function submitSearch() {
  emit('search', { ...filters })
}
</script>

<template>
  <form class="filter-bar" @submit.prevent="submitSearch">
    <label class="field">
      <span>지역</span>
      <input v-model="filters.location" type="search" placeholder="서울, 강남구, 역삼동" />
    </label>

    <label class="field">
      <span>보증금</span>
      <select v-model="filters.deposit">
        <option value="">전체</option>
        <option value="1000">1,000만 이하</option>
        <option value="3000">3,000만 이하</option>
        <option value="5000">5,000만 이하</option>
      </select>
    </label>

    <label class="field">
      <span>월세</span>
      <select v-model="filters.monthlyRent">
        <option value="">전체</option>
        <option value="50">50만 이하</option>
        <option value="80">80만 이하</option>
        <option value="110">110만 이하</option>
      </select>
    </label>

    <div class="room-type" role="group" aria-label="방 타입">
      <span>방 타입</span>
      <div class="chips">
        <button
          v-for="roomType in roomTypes"
          :key="roomType.value"
          class="filter-chip"
          :class="{ 'filter-chip--active': filters.roomType === roomType.value }"
          type="button"
          @click="filters.roomType = roomType.value"
        >
          {{ roomType.label }}
        </button>
      </div>
    </div>

    <button class="search-button" type="submit">검색하기</button>
  </form>
</template>

<style lang="scss" scoped>
.filter-bar {
  display: grid;
  grid-template-columns: 1.4fr 0.9fr 0.9fr 1.45fr auto;
  gap: 14px;
  align-items: end;
  padding: 18px;
  border: 1px solid rgba(208, 213, 221, 0.9);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-panel);
}

.field,
.room-type {
  display: grid;
  gap: 8px;

  span {
    color: var(--color-muted);
    font-size: 12px;
    font-weight: 800;
  }
}

input,
select {
  width: 100%;
  height: 46px;
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
}

.chips {
  display: flex;
  gap: 8px;
}

.filter-chip {
  height: 46px;
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

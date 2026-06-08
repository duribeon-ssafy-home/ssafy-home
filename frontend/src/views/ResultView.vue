<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import PropertyCard from '@/components/PropertyCard.vue'
import { lifestyleTypeMeta } from '@/data/lifestyle'
import { mockProperties } from '@/data/mockProperties'

const result = ref(null)

const defaultResult = {
  lifestyleType: 'LIVING_COST_COMPACT',
  typeName: lifestyleTypeMeta.LIVING_COST_COMPACT.typeName,
  filterPreset: {
    facilityScoreMin: 70,
    facilityCountMin: 20,
    monthlyRentMax: 50,
    depositMax: 1000,
    areaMin: null,
    buildYearMin: null,
  },
}

const displayResult = computed(() => result.value || defaultResult)
const meta = computed(() => lifestyleTypeMeta[displayResult.value.lifestyleType])
const presetChips = computed(() => createPresetChips(displayResult.value.filterPreset))
const recommendedProperties = computed(() => {
  const roomTypes = meta.value.recommendedRoomTypes
  return mockProperties.filter((property) => roomTypes.includes(property.roomType)).slice(0, 3)
})

onMounted(() => {
  const savedResult = sessionStorage.getItem('lifestyleResult')

  if (!savedResult) {
    return
  }

  try {
    result.value = JSON.parse(savedResult)
  } catch {
    result.value = null
  }
})

function createPresetChips(filterPreset = {}) {
  const chips = []

  if (filterPreset.facilityScoreMin) {
    chips.push(`생활 편의 점수 ${filterPreset.facilityScoreMin}+`)
  }

  if (filterPreset.facilityCountMin) {
    chips.push(`편의시설 ${filterPreset.facilityCountMin}개 이상`)
  }

  if (filterPreset.monthlyRentMax) {
    chips.push(`월세 ${filterPreset.monthlyRentMax}만 이하`)
  }

  if (filterPreset.depositMax) {
    chips.push(`보증금 ${Number(filterPreset.depositMax).toLocaleString('ko-KR')}만 이하`)
  }

  if (filterPreset.areaMin) {
    chips.push(`${filterPreset.areaMin}m2 이상`)
  }

  if (filterPreset.buildYearMin) {
    chips.push(`${filterPreset.buildYearMin}년 이후`)
  }

  return chips.length ? chips : meta.value.chips
}
</script>

<template>
  <main class="page result-page">
    <section class="result-hero">
      <div class="section-container result-hero__inner">
        <div class="result-copy">
          <p class="eyebrow">Lifestyle Result</p>
          <span>당신의 주거 타입은</span>
          <h1>{{ meta.typeName }}</h1>
          <p>{{ meta.headline }}</p>
        </div>

        <div class="summary-panel">
          <strong>추천 조건</strong>
          <div class="chip-list">
            <span
              v-for="(chip, index) in presetChips"
              :key="chip"
              :style="{ '--delay': `${index * 70}ms` }"
            >
              {{ chip }}
            </span>
          </div>
          <p>{{ meta.summary }}</p>
        </div>
      </div>
    </section>

    <section class="recommend-section">
      <div class="section-container">
        <div class="section-heading">
          <div>
            <p class="eyebrow">Matched Homes</p>
            <h2 class="section-title">이 타입에 맞는 추천 매물</h2>
          </div>
          <div class="result-actions">
            <RouterLink class="secondary-link" :to="{ name: 'survey' }">다시 설문하기</RouterLink>
            <RouterLink class="primary-link" :to="{ name: 'home' }">메인으로 이동</RouterLink>
          </div>
        </div>

        <div class="property-grid">
          <PropertyCard
            v-for="property in recommendedProperties"
            :key="property.propertyId"
            :property="property"
          />
        </div>
      </div>
    </section>
  </main>
</template>

<style lang="scss" scoped>
.result-page {
  padding-bottom: 76px;
  background:
    linear-gradient(180deg, rgba(238, 244, 255, 0.9) 0%, rgba(246, 247, 249, 0) 420px),
    var(--color-bg);
}

.result-hero {
  padding: 72px 0 46px;
}

.result-hero__inner {
  display: grid;
  grid-template-columns: minmax(0, 1.06fr) minmax(320px, 0.74fr);
  gap: 28px;
  align-items: stretch;
}

.result-copy,
.summary-panel {
  border: 1px solid rgba(208, 213, 221, 0.9);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-panel);
}

.result-copy {
  display: grid;
  align-content: center;
  min-height: 370px;
  padding: 42px;
  animation: fadeUp 520ms ease both;

  span {
    margin-top: 22px;
    color: var(--color-muted);
    font-size: 17px;
    font-weight: 800;
  }

  h1 {
    margin-top: 12px;
    color: var(--color-heading);
    font-size: 52px;
    font-weight: 900;
    letter-spacing: 0;
    line-height: 1.12;
  }

  p:not(.eyebrow) {
    max-width: 620px;
    margin-top: 18px;
    color: var(--color-muted);
    font-size: 18px;
    font-weight: 700;
    line-height: 1.7;
  }
}

.summary-panel {
  display: grid;
  align-content: center;
  gap: 20px;
  padding: 32px;
  animation: fadeUp 520ms ease 120ms both;

  strong {
    color: var(--color-heading);
    font-size: 18px;
    font-weight: 900;
  }

  p {
    color: var(--color-muted);
    font-weight: 700;
    line-height: 1.7;
  }
}

.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;

  span {
    border: 1px solid rgba(54, 95, 145, 0.2);
    border-radius: var(--radius-sm);
    background: var(--color-primary-soft);
    color: var(--color-primary-dark);
    font-size: 13px;
    font-weight: 900;
    padding: 9px 11px;
    animation: chipIn 360ms ease both;
    animation-delay: var(--delay);
  }
}

.recommend-section {
  padding-top: 24px;
}

.section-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.result-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.primary-link,
.secondary-link {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 900;
  padding: 0 16px;
  transition:
    background-color var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast),
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}

.primary-link {
  background: var(--color-primary);
  color: var(--color-surface);

  &:hover {
    background: var(--color-primary-dark);
    box-shadow: 0 12px 24px rgba(54, 95, 145, 0.2);
    transform: translateY(-1px);
  }
}

.secondary-link {
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-heading);

  &:hover {
    border-color: var(--color-primary);
    transform: translateY(-1px);
  }
}

.property-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

@keyframes fadeUp {
  from {
    opacity: 0;
    transform: translateY(18px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes chipIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 940px) {
  .result-hero__inner,
  .property-grid {
    grid-template-columns: 1fr;
  }

  .section-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .result-copy h1 {
    font-size: 38px;
  }
}
</style>

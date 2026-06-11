<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { getProperty } from '@/api/propertyApi'
import { roomTypeLabels } from '@/data/mockProperties'

const props = defineProps({
  id: {
    type: String,
    required: true,
  },
})

const property = ref(null)
const isLoading = ref(false)
const isError = ref(false)

async function fetchProperty() {
  isLoading.value = true
  isError.value = false
  try {
    property.value = await getProperty(props.id)
  } catch {
    isError.value = true
  } finally {
    isLoading.value = false
  }
}

onMounted(fetchProperty)
watch(() => props.id, fetchProperty)

const imageUrl = computed(() => property.value?.images?.[0]?.imageUrl)
const priceLabel = computed(() => {
  if (!property.value) return ''
  if (property.value.rentType === 'JEONSE') {
    return `전세 ${Number(property.value.deposit).toLocaleString('ko-KR')}`
  }
  return `월세 ${property.value.monthlyRent} / 보증금 ${Number(property.value.deposit).toLocaleString('ko-KR')}`
})
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
          <img :src="imageUrl" :alt="property.title" />
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

          <div class="panel-actions">
            <button type="button">찜하기</button>
            <button class="ghost" type="button">신고</button>
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
  overflow: hidden;
  aspect-ratio: 16 / 10;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
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

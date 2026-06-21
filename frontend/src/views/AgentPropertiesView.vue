<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyProperties, deleteProperty } from '@/api/propertyApi'

const router = useRouter()

const properties = ref([])
const isLoading = ref(false)
const isError = ref(false)
const viewMode = ref('card')

const statusMeta = {
  PENDING:  { label: '승인 대기', class: 'badge--pending' },
  APPROVED: { label: '승인됨',   class: 'badge--approved' },
  REJECTED: { label: '거부됨',   class: 'badge--rejected' },
  HIDDEN:   { label: '숨김',     class: 'badge--hidden' },
  DELETED:  { label: '삭제됨',   class: 'badge--hidden' },
}

const rentTypeLabels = { JEONSE: '전세', MONTHLY: '월세' }
const roomTypeLabels = { ONE_ROOM: '원룸', TWO_ROOM: '투룸', OFFICETEL: '오피스텔', APARTMENT: '아파트' }

function priceLabel(p) {
  if (p.rentType === 'JEONSE') return `전세 ${Number(p.deposit).toLocaleString('ko-KR')}만`
  return `월세 ${p.monthlyRent}만 / 보증금 ${Number(p.deposit).toLocaleString('ko-KR')}만`
}

function thumbnail(p) {
  return p.images?.[0]?.imageUrl ?? null
}

async function load() {
  isLoading.value = true
  isError.value = false
  try {
    properties.value = await getMyProperties()
  } catch {
    isError.value = true
  } finally {
    isLoading.value = false
  }
}

async function handleDelete(property) {
  if (!confirm(`"${property.title || '(제목 없음)'}" 매물을 삭제하시겠습니까?`)) return
  try {
    await deleteProperty(property.propertyId)
    await load()
  } catch {
    alert('삭제에 실패했습니다.')
  }
}

onMounted(load)
</script>

<template>
  <main class="page agent-page">
    <div class="section-container">
      <div class="page-header">
        <div>
          <p class="eyebrow">Agent Dashboard</p>
          <h1>내 매물 관리</h1>
        </div>
        <div class="header-right">
          <div class="view-toggle">
            <button
              class="toggle-btn"
              :class="{ 'toggle-btn--active': viewMode === 'card' }"
              type="button"
              title="카드 보기"
              @click="viewMode = 'card'"
            >
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <rect x="1" y="1" width="6" height="6" rx="1" fill="currentColor"/>
                <rect x="9" y="1" width="6" height="6" rx="1" fill="currentColor"/>
                <rect x="1" y="9" width="6" height="6" rx="1" fill="currentColor"/>
                <rect x="9" y="9" width="6" height="6" rx="1" fill="currentColor"/>
              </svg>
              카드 보기
            </button>
            <button
              class="toggle-btn"
              :class="{ 'toggle-btn--active': viewMode === 'list' }"
              type="button"
              title="목록 보기"
              @click="viewMode = 'list'"
            >
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <rect x="1" y="2" width="14" height="2" rx="1" fill="currentColor"/>
                <rect x="1" y="7" width="14" height="2" rx="1" fill="currentColor"/>
                <rect x="1" y="12" width="14" height="2" rx="1" fill="currentColor"/>
              </svg>
              목록 보기
            </button>
          </div>
          <button class="primary-btn" type="button" @click="router.push({ name: 'agent-property-new' })">
            + 새 매물 등록
          </button>
        </div>
      </div>

      <div v-if="isLoading" class="state-box">
        <p>매물 목록을 불러오는 중...</p>
      </div>

      <div v-else-if="isError" class="state-box">
        <p>목록을 불러올 수 없습니다. 잠시 후 다시 시도해주세요.</p>
      </div>

      <div v-else-if="properties.length === 0" class="state-box">
        <p>등록된 매물이 없습니다.</p>
        <button class="primary-btn" type="button" @click="router.push({ name: 'agent-property-new' })">
          첫 매물 등록하기
        </button>
      </div>

      <!-- 카드 보기 -->
      <div v-else-if="viewMode === 'card'" class="card-grid">
        <div v-for="p in properties" :key="p.propertyId" class="prop-card">
          <div class="prop-card__thumb">
            <img v-if="thumbnail(p)" :src="thumbnail(p)" :alt="p.title || '매물 이미지'" />
            <div v-else class="prop-card__no-img">이미지 없음</div>
            <span class="prop-card__status badge" :class="statusMeta[p.status]?.class">
              {{ statusMeta[p.status]?.label ?? p.status }}
            </span>
          </div>

          <div class="prop-card__body">
            <div class="prop-card__tags">
              <span class="tag">{{ rentTypeLabels[p.rentType] ?? p.rentType }}</span>
              <span class="tag tag--sub">{{ roomTypeLabels[p.roomType] ?? p.roomType }}</span>
            </div>
            <p class="prop-card__title">{{ p.title || '(제목 없음)' }}</p>
            <p class="prop-card__price">{{ priceLabel(p) }}</p>

            <div class="prop-card__meta">
              <span v-if="p.area">{{ p.area }}m²</span>
              <span v-if="p.area && p.floor">·</span>
              <span v-if="p.floor">{{ p.floor }}층</span>
              <span v-if="(p.area || p.floor) && p.createdAt">·</span>
              <span v-if="p.createdAt">{{ p.createdAt.slice(0, 10) }}</span>
            </div>
          </div>

          <div class="prop-card__actions">
            <button
              class="action-btn"
              type="button"
              @click="router.push({ name: 'agent-property-edit', params: { id: p.propertyId } })"
            >수정</button>
            <button
              class="action-btn action-btn--danger"
              type="button"
              @click="handleDelete(p)"
            >삭제</button>
          </div>
        </div>
      </div>

      <!-- 목록 보기 -->
      <div v-else class="table-wrap">
        <table class="prop-table">
          <thead>
            <tr>
              <th>제목</th>
              <th>거래유형</th>
              <th>방 타입</th>
              <th>가격</th>
              <th>상태</th>
              <th>등록일</th>
              <th>관리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in properties" :key="p.propertyId">
              <td class="title-cell">{{ p.title || '(제목 없음)' }}</td>
              <td>{{ rentTypeLabels[p.rentType] }}</td>
              <td>{{ roomTypeLabels[p.roomType] }}</td>
              <td class="price-cell">{{ priceLabel(p) }}</td>
              <td>
                <span class="badge" :class="statusMeta[p.status]?.class">
                  {{ statusMeta[p.status]?.label ?? p.status }}
                </span>
              </td>
              <td class="date-cell">{{ p.createdAt?.slice(0, 10) ?? '-' }}</td>
              <td class="action-cell">
                <button
                  class="action-btn"
                  type="button"
                  @click="router.push({ name: 'agent-property-edit', params: { id: p.propertyId } })"
                >수정</button>
                <button
                  class="action-btn action-btn--danger"
                  type="button"
                  @click="handleDelete(p)"
                >삭제</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </main>
</template>

<style lang="scss" scoped>
.agent-page {
  padding: 44px 0 80px;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 32px;

  h1 {
    margin-top: 6px;
    color: var(--color-heading);
    font-size: 28px;
    font-weight: 900;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.view-toggle {
  display: flex;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.toggle-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 14px;
  background: var(--color-surface);
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition: background-color var(--transition-fast), color var(--transition-fast);
  border-right: 1px solid var(--color-border);

  &:last-child { border-right: none; }
  &:hover { background: var(--color-bg-soft); color: var(--color-heading); }

  &--active {
    background: var(--color-primary-soft);
    color: var(--color-primary-dark);
  }
}

.primary-btn {
  height: 42px;
  padding: 0 20px;
  background: var(--color-primary);
  color: var(--color-surface);
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 900;
  white-space: nowrap;
  transition: background-color var(--transition-fast);
  &:hover { background: var(--color-primary-dark); }
}

.eyebrow {
  color: var(--color-muted);
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  min-height: 240px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-muted);
  font-size: 15px;
  font-weight: 700;
}

/* ── 카드 그리드 ── */
.card-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.prop-card {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  overflow: hidden;
  box-shadow: var(--shadow-card);
  transition: box-shadow var(--transition-fast), transform var(--transition-fast);

  &:hover {
    box-shadow: 0 4px 16px rgba(0,0,0,0.10);
    transform: translateY(-2px);
  }
}

.prop-card__thumb {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  background: var(--color-bg-soft);
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.prop-card__no-img {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-subtle);
  font-size: 13px;
  font-weight: 700;
}

.prop-card__status {
  position: absolute;
  top: 10px;
  left: 10px;
}

.prop-card__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px 10px;
}

.prop-card__tags {
  display: flex;
  gap: 6px;
}

.tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--radius-xs);
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
  font-size: 11px;
  font-weight: 900;

  &--sub {
    background: var(--color-bg-soft);
    color: var(--color-muted);
  }
}

.prop-card__title {
  color: var(--color-heading);
  font-size: 15px;
  font-weight: 900;
  line-height: 1.3;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.prop-card__price {
  color: var(--color-primary-dark);
  font-size: 14px;
  font-weight: 900;
}

.prop-card__meta {
  display: flex;
  gap: 5px;
  color: var(--color-muted);
  font-size: 12px;
  font-weight: 700;
  margin-top: 2px;
}

.prop-card__actions {
  display: flex;
  gap: 8px;
  padding: 10px 16px 14px;
  border-top: 1px solid var(--color-border);
}

/* ── 테이블 ── */
.table-wrap {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  overflow: hidden;
  background: var(--color-surface);
}

.prop-table {
  width: 100%;
  border-collapse: collapse;

  th, td {
    padding: 12px 16px;
    text-align: left;
    font-size: 13px;
    border-bottom: 1px solid var(--color-border);
  }

  th {
    background: var(--color-bg-soft);
    color: var(--color-muted);
    font-weight: 800;
    font-size: 12px;
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }

  tr:last-child td { border-bottom: none; }
  tr:hover td { background: var(--color-bg-soft); }
}

.title-cell {
  font-weight: 700;
  color: var(--color-heading);
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price-cell {
  font-weight: 800;
  color: var(--color-heading);
  white-space: nowrap;
}

.date-cell {
  color: var(--color-muted);
  white-space: nowrap;
}

/* ── 공통 ── */
.badge {
  display: inline-block;
  padding: 3px 9px;
  border-radius: var(--radius-xs);
  font-size: 11px;
  font-weight: 900;
}

.badge--pending  { background: #fefce8; color: #854d0e; }
.badge--approved { background: #ecfdf5; color: #065f46; }
.badge--rejected { background: #fff1f2; color: #9f1239; }
.badge--hidden   { background: var(--color-bg-soft); color: var(--color-muted); }

.action-cell {
  display: flex;
  gap: 6px;
  white-space: nowrap;
}

.action-btn {
  flex: 1;
  height: 32px;
  padding: 0 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-heading);
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
  transition: border-color var(--transition-fast), color var(--transition-fast);
  &:hover { border-color: var(--color-primary); color: var(--color-primary); }
}

.action-btn--danger {
  &:hover { border-color: var(--color-danger); color: var(--color-danger); }
}

@media (max-width: 900px) {
  .card-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 600px) {
  .card-grid { grid-template-columns: 1fr; }
  .toggle-btn span { display: none; }
  .page-header { flex-direction: column; align-items: flex-start; }
  .header-right { width: 100%; justify-content: space-between; }
}
</style>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useCompareStore } from '@/stores/compare'

const compareStore = useCompareStore()
const router = useRouter()
const route = useRoute()

const canCompare = computed(() => compareStore.count >= 2)
const visible = computed(() => compareStore.count > 0 && route.name !== 'compare')

function goCompare() {
  router.push({ name: 'compare' })
}
</script>

<template>
  <Transition name="bar-slide">
    <div v-if="visible" class="compare-bar">
      <div class="compare-bar__slots">
        <div v-for="item in compareStore.items" :key="item.propertyId" class="slot">
          <div class="slot__thumb">
            <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.title" />
            <div v-else class="slot__placeholder" />
          </div>
          <p class="slot__title">{{ item.title ?? `매물 #${item.propertyId}` }}</p>
          <button class="slot__remove" type="button" @click="compareStore.remove(item.propertyId)">✕</button>
        </div>

        <div v-for="n in 4 - compareStore.count" :key="`empty-${n}`" class="slot slot--empty">
          <span>+</span>
          <p>추가</p>
        </div>
      </div>

      <div class="compare-bar__actions">
        <span class="compare-bar__count">{{ compareStore.count }}개 선택됨</span>
        <button
          class="compare-bar__btn"
          type="button"
          :disabled="!canCompare"
          @click="goCompare"
        >비교하기 →</button>
        <button class="compare-bar__clear" type="button" @click="compareStore.clear()">전체 지우기</button>
      </div>
    </div>
  </Transition>
</template>

<style lang="scss" scoped>
.compare-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
  box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 12px 24px;
}

.compare-bar__slots {
  display: flex;
  gap: 10px;
  flex: 1;
}

.slot {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px 6px 6px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg-soft);
  width: 170px;
  flex-shrink: 0;
}

.slot--empty {
  border-style: dashed;
  justify-content: center;
  color: var(--color-subtle);
  font-size: 13px;
  font-weight: 700;
  gap: 4px;

  span { font-size: 16px; line-height: 1; }
  p { margin: 0; }
}

.slot__thumb {
  width: 44px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 4px;
  overflow: hidden;

  img { width: 100%; height: 100%; object-fit: cover; }
}

.slot__placeholder {
  width: 100%;
  height: 100%;
  background: var(--color-border);
  border-radius: 4px;
}

.slot__title {
  flex: 1;
  font-size: 11px;
  font-weight: 700;
  color: var(--color-heading);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.slot__remove {
  position: absolute;
  top: -7px;
  right: -7px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--color-heading);
  color: var(--color-surface);
  font-size: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color var(--transition-fast);

  &:hover { background: var(--color-danger); }
}

.compare-bar__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.compare-bar__count {
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.compare-bar__btn {
  height: 40px;
  padding: 0 22px;
  background: var(--color-primary);
  color: white;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 900;
  cursor: pointer;
  white-space: nowrap;
  transition: background-color var(--transition-fast);

  &:hover:not(:disabled) { background: var(--color-primary-dark); }
  &:disabled { opacity: 0.4; cursor: not-allowed; }
}

.compare-bar__clear {
  color: var(--color-muted);
  font-size: 12px;
  font-weight: 700;
  text-decoration: underline;
  cursor: pointer;
  white-space: nowrap;

  &:hover { color: var(--color-danger); }
}

.bar-slide-enter-active,
.bar-slide-leave-active { transition: transform 0.25s ease, opacity 0.25s; }
.bar-slide-enter-from,
.bar-slide-leave-to { transform: translateY(100%); opacity: 0; }
</style>

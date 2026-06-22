<script setup>
import { computed, ref } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import CompareBar from '@/components/CompareBar.vue'
import AiCompareFloatingButton from '@/components/compare/AiCompareFloatingButton.vue'
import AiComparePanel from '@/components/compare/AiComparePanel.vue'
import { useCompareStore } from '@/stores/compare'

const DRAWER_MIN = 350
const DRAWER_MAX = 700

const compareStore = useCompareStore()
const route = useRoute()
const isAiPanelOpen = ref(false)
const drawerWidth = ref(420)

const aiButtonBottom = computed(() => (
  compareStore.count > 0 && route.name !== 'compare' ? 108 : 28
))

const effectiveDrawerWidth = computed(() => {
  if (!isAiPanelOpen.value) return 0
  return Math.min(Math.max(drawerWidth.value, DRAWER_MIN), DRAWER_MAX)
})

function openAiPanel() {
  isAiPanelOpen.value = true
}

function closeAiPanel() {
  isAiPanelOpen.value = false
}

function resizeAiPanel(width) {
  drawerWidth.value = Math.min(Math.max(width, DRAWER_MIN), DRAWER_MAX)
}
</script>

<template>
  <div
    class="app-shell"
    :class="{ 'app-shell--drawer-open': isAiPanelOpen }"
    :style="{ '--ai-drawer-width': `${effectiveDrawerWidth}px` }"
  >
    <div class="app-main">
      <AppHeader />
      <RouterView v-slot="{ Component }">
        <Transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
      <CompareBar />
    </div>

    <AiComparePanel
      :open="isAiPanelOpen"
      :property-ids="compareStore.ids"
      :width="effectiveDrawerWidth"
      @close="closeAiPanel"
      @resize="resizeAiPanel"
    />

    <AiCompareFloatingButton
      v-if="!isAiPanelOpen"
      :bottom="aiButtonBottom"
      @open="openAiPanel"
    />
  </div>
</template>

<style lang="scss" scoped>
.app-shell {
  display: flex;
  align-items: stretch;
  min-height: 100vh;
}

.app-main {
  flex: 1 1 auto;
  min-width: 0;
  transition: flex-basis var(--transition-base);
}

.app-shell--drawer-open {
  min-width: calc(320px + var(--ai-drawer-width));

  .app-main {
    flex-basis: calc(100% - var(--ai-drawer-width));
    min-width: 320px;
  }

  :deep(.compare-bar) {
    right: var(--ai-drawer-width);
  }
}
</style>

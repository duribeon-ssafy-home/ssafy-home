<script setup>
import { RouterView } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import CompareBar from '@/components/CompareBar.vue'
</script>

<template>
  <div class="app-shell">
    <AppHeader />
    <RouterView v-slot="{ Component }">
      <Transition name="page-fade" mode="out-in">
        <component :is="Component" />
      </Transition>
    </RouterView>
    <CompareBar />
  </div>
</template>

<style lang="scss" scoped>
.app-shell {
  min-height: 100vh;
}
</style>

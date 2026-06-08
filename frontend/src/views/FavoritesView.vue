<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <main class="page favorites-page">
    <section class="section-container">
      <div class="page-heading">
        <p class="eyebrow">Saved Homes</p>
        <h1>찜한 매물</h1>
        <p>관심 있는 매물을 저장하고 나중에 다시 확인할 수 있는 공간입니다.</p>
      </div>

      <div class="empty-state">
        <strong>아직 찜한 매물이 없습니다</strong>
        <p>마음에 드는 매물을 저장해두면 여기에서 다시 볼 수 있어요.</p>
        <RouterLink :to="{ name: 'home' }">매물 둘러보기</RouterLink>
      </div>
    </section>
  </main>
</template>

<style lang="scss" scoped>
.favorites-page {
  padding: 56px 0 76px;
}

.page-heading {
  display: grid;
  gap: 10px;
  margin-bottom: 24px;

  h1 {
    color: var(--color-heading);
    font-size: 36px;
    font-weight: 900;
  }

  p:not(.eyebrow) {
    color: var(--color-muted);
    font-weight: 700;
  }
}

.empty-state {
  display: grid;
  gap: 12px;
  place-items: center;
  min-height: 360px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
  color: var(--color-muted);
  text-align: center;
  padding: 32px;

  strong {
    color: var(--color-heading);
    font-size: 22px;
    font-weight: 900;
  }

  a {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 44px;
    border-radius: var(--radius-sm);
    background: var(--color-primary);
    color: var(--color-surface);
    font-weight: 900;
    padding: 0 16px;
  }
}
</style>

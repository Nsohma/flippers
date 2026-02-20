<script setup>
import { RouterLink } from "vue-router";
</script>

<template>
  <section class="mode-switch">
    <RouterLink class="mode-link" to="/pos">レジキー</RouterLink>
    <RouterLink class="mode-link" to="/handy">ハンディ</RouterLink>
  </section>
</template>

<style scoped>
.mode-switch {
  display: flex;
  gap: 8px;
  margin: 8px 0 12px;
}
.mode-link {
  text-decoration: none;
  color: #2a3c57;
  border: 1px solid #d4dcea;
  background: #f6f8fc;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 13px;
}
.mode-link.router-link-active {
  border-color: #7692c8;
  background: #e9f0ff;
  color: #203455;
  font-weight: 700;
}
</style>

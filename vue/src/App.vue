<script setup>
import { computed, provide, ref, watch } from "vue";
import { RouterLink, RouterView, useRoute } from "vue-router";

const route = useRoute();
const lastEditorPath = ref("/pos");
const sidebarCollapsed = ref(false);

watch(
  () => route.path,
  (path) => {
    if (path === "/pos" || path === "/handy") {
      lastEditorPath.value = path;
    }
  },
  { immediate: true },
);

const editorPath = computed(() => lastEditorPath.value || "/pos");
const isTopActive = computed(() => route.path === "/");
const isEditorActive = computed(() => route.path === "/pos" || route.path === "/handy");
const isCatalogActive = computed(() => route.path === "/catalog");

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value;
}

provide("toggleSidebar", toggleSidebar);
</script>

<template>
  <div class="app-shell" :class="{ collapsed: sidebarCollapsed }">
    <aside class="side-nav">
      <RouterLink class="brand" :class="{ active: isTopActive }" to="/">
        <div class="brand-mark">F</div>
        <div class="brand-text" v-if="!sidebarCollapsed">
          <div class="brand-title">flippers</div>
          <div class="brand-sub">editor</div>
        </div>
      </RouterLink>

      <nav class="side-menu" aria-label="画面切り替え">
        <RouterLink class="side-link" :class="{ active: isEditorActive }" :to="editorPath">
          <span class="side-link-icon" aria-hidden="true">🛠</span>
          <span class="side-link-main">編集画面</span>
        </RouterLink>
        <RouterLink class="side-link" :class="{ active: isCatalogActive }" to="/catalog">
          <span class="side-link-icon" aria-hidden="true">📚</span>
          <span class="side-link-main">商品カタログ</span>
        </RouterLink>
      </nav>
    </aside>

    <div class="main-column">
      <main class="content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100dvh;
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  background: #eef1f6;
  transition: grid-template-columns 0.2s ease;
}

.app-shell.collapsed {
  grid-template-columns: 76px minmax(0, 1fr);
}

.side-nav {
  background: linear-gradient(180deg, #04182f 0%, #061427 100%);
  color: #dce8ff;
  border-right: 1px solid #0c2c4d;
  padding: 12px 10px;
  overflow: hidden;
}

.brand {
  text-decoration: none;
  color: inherit;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 8px 14px;
  border-radius: 10px;
}

.brand.active {
  background: #113154;
}

.brand-mark {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: #0f3157;
  color: #dfeaff;
  font-weight: 800;
  font-size: 18px;
}

.brand-text {
  line-height: 1.1;
}

.brand-title {
  font-weight: 700;
  font-size: 18px;
  letter-spacing: 0.02em;
}

.brand-sub {
  opacity: 0.8;
  font-size: 12px;
}

.side-menu {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.side-link {
  text-decoration: none;
  color: #d6e5ff;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid transparent;
  font-size: 14px;
  background: transparent;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.side-link-main {
  display: inline-block;
}

.side-link-icon {
  width: 18px;
  text-align: center;
  flex: 0 0 18px;
}

.side-link:hover {
  background: #113154;
  border-color: #24486f;
}

.side-link.active {
  background: #3f87f5;
  border-color: #67a4ff;
  color: #fff;
  font-weight: 700;
}

.main-column {
  min-width: 0;
}

.content {
  min-width: 0;
}

.app-shell.collapsed .brand {
  justify-content: center;
}

.app-shell.collapsed .side-link {
  justify-content: center;
  padding: 10px 6px;
}

.app-shell.collapsed .side-link-main {
  display: none;
}

@media (max-width: 900px) {
  .app-shell {
    display: block;
  }

  .app-shell.collapsed {
    grid-template-columns: none;
  }

  .side-nav {
    border-right: none;
    border-bottom: 1px solid #0c2c4d;
  }

  .side-menu {
    flex-direction: row;
  }

  .side-link {
    flex: 1 1 0;
    text-align: center;
    justify-content: center;
  }
}
</style>

<script setup>
import { computed, ref } from "vue";

const props = defineProps({
  entries: {
    type: Array,
    default: () => [],
  },
  currentIndex: {
    type: Number,
    default: -1,
  },
  loading: {
    type: Boolean,
    default: false,
  },
});
const emit = defineEmits(["jump", "clear"]);

const expanded = ref(false);
const reversedEntries = computed(() =>
  [...props.entries]
    .map((entry) => {
      const action = String(entry?.action ?? "").trim();
      const match = action.match(/^(.*?)(\s*\([^()]+\))$/);
      if (!match) {
        return {
          ...entry,
          actionLabel: action,
          actionTarget: "",
        };
      }
      return {
        ...entry,
        actionLabel: match[1].trim(),
        actionTarget: match[2].trim(),
      };
    })
    .reverse(),
);
const currentEntry = computed(() => {
  if (!Number.isInteger(props.currentIndex) || props.currentIndex < 0) return null;
  return reversedEntries.value.find((entry) => entry.index === props.currentIndex) ?? null;
});
const canClear = computed(() => props.entries.length > 1 && !props.loading);

function toggleExpanded() {
  expanded.value = !expanded.value;
}

function formatTimestamp(value) {
  const raw = String(value ?? "").trim();
  if (!raw) return "";

  const date = new Date(raw);
  if (Number.isNaN(date.getTime())) {
    return raw;
  }

  return date.toLocaleString("ja-JP", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}
</script>

<template>
  <section class="panel" v-if="entries.length">
    <div class="head">
      <div class="title-wrap">
        <h2>編集履歴</h2>
        <div v-if="!expanded" class="summary">
          <span>全 {{ entries.length }} 件</span>
          <span v-if="currentEntry">現在: #{{ currentEntry.index }} {{ currentEntry.actionLabel }}</span>
        </div>
      </div>

      <div class="head-actions">
        <button
          class="clear-btn"
          type="button"
          :disabled="!canClear"
          title="編集履歴を削除"
          @click="emit('clear')"
        >
          履歴削除
        </button>

        <button
          class="expand-btn"
          type="button"
          :aria-expanded="expanded"
          :title="expanded ? '履歴を折りたたむ' : '履歴を展開する'"
          @click="toggleExpanded"
        >
          <span class="arrow" :class="{ open: expanded }">▼</span>
        </button>
      </div>
    </div>

    <ol v-if="expanded" class="history-list">
      <li
        v-for="entry in reversedEntries"
        :key="entry.index"
        class="history-item"
        :class="{ current: entry.index === currentIndex }"
        @click="emit('jump', entry.index)"
      >
        <span class="index">#{{ entry.index }}</span>
        <span class="action">{{ entry.actionLabel }}</span>
        <span class="target">{{ entry.actionTarget }}</span>
        <span class="time">{{ formatTimestamp(entry.timestamp) }}</span>
      </li>
    </ol>
  </section>
</template>

<style scoped>
.panel {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 12px;
  margin: 12px 0;
}
.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}
.head-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.title-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.title-wrap h2 {
  margin: 0;
  font-size: 16px;
}
.summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: #555;
}
.expand-btn {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  border: 1px solid #ccc;
  background: #fff;
  color: #333;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.clear-btn {
  height: 28px;
  border-radius: 999px;
  border: 1px solid #d1d8e5;
  background: #fff;
  color: #234;
  padding: 0 10px;
  font-size: 12px;
  cursor: pointer;
}
.clear-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.arrow {
  display: inline-block;
  transform: rotate(-90deg);
  transition: transform 0.16s ease;
  font-size: 11px;
  line-height: 1;
}
.arrow.open {
  transform: rotate(0deg);
}
.history-list {
  margin: 8px 0 0;
  padding: 0;
  list-style: none;
  max-height: 320px;
  overflow: auto;
  border: 1px solid #eee;
  border-radius: 8px;
}
.history-item {
  display: grid;
  grid-template-columns: 56px minmax(120px, auto) minmax(120px, 1fr) auto;
  gap: 8px;
  align-items: center;
  padding: 8px 10px;
  border-bottom: 1px solid #f3f3f3;
  font-size: 13px;
  cursor: pointer;
}
.history-item:last-child {
  border-bottom: none;
}
.history-item.current {
  background: #eef4ff;
}
.index {
  color: #666;
  font-variant-numeric: tabular-nums;
}
.action {
  font-weight: 600;
}
.target {
  color: #245;
  text-align: right;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.time {
  color: #666;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

@media (max-width: 680px) {
  .history-item {
    grid-template-columns: 1fr 1fr;
    gap: 2px;
  }
  .index,
  .time {
    grid-column: 1 / -1;
  }
  .target {
    text-align: left;
  }
  .time {
    white-space: normal;
  }
}
</style>

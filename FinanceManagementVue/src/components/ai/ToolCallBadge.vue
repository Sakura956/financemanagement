<script setup lang="ts">
import { computed } from 'vue'
import { Loading, CircleCheckFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import type { ToolCallEvent } from '@/types'

const props = defineProps<{ event: ToolCallEvent }>()

const statusIcon = computed(() => {
  if (props.event.status === 'start') return Loading
  if (props.event.status === 'error') return CircleCloseFilled
  return CircleCheckFilled
})

const statusText = computed(() => {
  if (props.event.status === 'start') return '执行中'
  if (props.event.status === 'error') return '失败'
  return '完成'
})
</script>

<template>
  <span class="tool-badge" :class="`tool-${event.status}`" :title="event.arguments">
    <el-icon :size="12" :class="{ spinning: event.status === 'start' }">
      <component :is="statusIcon" />
    </el-icon>
    <span class="tool-name">{{ event.toolLabel }}</span>
    <span class="tool-status">{{ statusText }}</span>
  </span>
</template>

<style scoped>
.tool-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  line-height: 18px;
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}

.tool-badge.tool-success {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #15803d;
}

.tool-badge.tool-error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.tool-name {
  font-weight: 500;
}

.tool-status {
  font-size: 11px;
  opacity: 0.75;
}

.spinning {
  animation: tool-spin 1s linear infinite;
}

@keyframes tool-spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}
</style>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { statisticsApi } from '@/api/modules/statistics'// 统计接口
import type { StatisticsOverview } from '@/types'
import { formatCurrency } from '@/utils'// 金额格式化工具
import { Money, TrendCharts, PieChart, Notebook, MagicStick } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const router = useRouter()
// 统计数据：本月收入、支出、结余
const overview = ref<StatisticsOverview>({ month: '', income: 0, expense: 0, balance: 0 })
//快捷入口菜单
const quickLinks = [
  { path: '/bills', title: '记一笔', desc: '记录收入和支出', icon: Money, color: '#2563eb' },
  { path: '/finance-plans', title: '理财计划', desc: '管理投资组合', icon: TrendCharts, color: '#16a34a' },
  { path: '/statistics', title: '统计分析', desc: '查看财务图表', icon: PieChart, color: '#9333ea' },
  { path: '/memos', title: '备忘录', desc: '管理待办事项', icon: Notebook, color: '#ea580c' },
  { path: '/ai', title: 'AI 助手', desc: '智能财务建议', icon: MagicStick, color: '#0891b2' },
]

onMounted(async () => {
  try {
    overview.value = await statisticsApi.getOverview()
    // console.log('overview:',overview.value)
  } catch {}
})
</script>

<template>
  <div class="dashboard">
    <h3 class="page-title">欢迎回来，{{ authStore.userInfo?.nickname || '用户' }}</h3>

    <el-row :gutter="20" class="overview-row">
      <el-col :span="8">
        <el-card shadow="never" class="stat-card income">
          <div class="stat-label">本月收入</div>
          <div class="stat-value income-value">{{ formatCurrency(overview.income) }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="stat-card expense">
          <div class="stat-label">本月支出</div>
          <div class="stat-value expense-value">{{ formatCurrency(overview.expense) }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="stat-card balance">
          <div class="stat-label">本月结余</div>
          <div class="stat-value" :class="overview.balance >= 0 ? 'balance-positive' : 'balance-negative'">
            {{ formatCurrency(overview.balance) }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <h4 class="section-title">快捷入口</h4>
    <el-row :gutter="16">
      <el-col v-for="link in quickLinks" :key="link.path" :span="8" style="margin-bottom: 16px">
        <el-card shadow="never" class="quick-card" @click="router.push(link.path)">
          <div class="quick-content">
            <div class="quick-icon" :style="{ background: link.color }">
              <el-icon :size="22" color="#fff">
                <component :is="link.icon" />
              </el-icon>
            </div>
            <div class="quick-info">
              <div class="quick-title">{{ link.title }}</div>
              <div class="quick-desc">{{ link.desc }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.page-title {
  font-size: 20px;
  color: #1e293b;
  margin: 0 0 20px;
}

.overview-row {
  margin-bottom: 32px;
}

.stat-card {
  text-align: center;
  padding: 8px 0;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
}

.income-value {
  color: #16a34a;
}

.expense-value {
  color: #dc2626;
}

.balance-positive {
  color: #2563eb;
}

.balance-negative {
  color: #dc2626;
}

.section-title {
  font-size: 16px;
  color: #1e293b;
  margin: 0 0 16px;
}

.quick-card {
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.quick-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.quick-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.quick-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.quick-title {
  font-size: 15px;
  font-weight: 600;
  color: #334155;
}

.quick-desc {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 2px;
}
</style>

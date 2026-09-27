<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { statisticsApi } from '@/api/modules/statistics'
import type { StatisticsOverview, CategoryPieData, TrendItem, YearlySummary } from '@/types'
import { formatCurrency } from '@/utils'
import * as echarts from 'echarts'//百度图表库

const month = ref('')
const year = ref(new Date().getFullYear())
const trendMonths = ref(12)//趋势图显示近 12 个月数据（可切换 6/12/24）

const overview = ref<StatisticsOverview>({ month: '', income: 0, expense: 0, balance: 0 })//月度概览数据
const pieData = ref<CategoryPieData>({ totalAmount: 0, items: [] })//饼图数据支出（分类名称 + 金额）
const incomePieData = ref<CategoryPieData>({ totalAmount: 0, items: [] })//收入饼图数据
const trendData = ref<TrendItem[]>([])//折线图数据（每月收入、支出、结余）
const yearlyData = ref<YearlySummary | null>(null)//年度统计数据

//绑定页面上的图表容器
const pieChartRef = ref<HTMLDivElement>()
const incomePieChartRef = ref<HTMLDivElement>()
const trendChartRef = ref<HTMLDivElement>()
//图表实例（用来渲染、更新）
let pieChart: echarts.ECharts | null = null
let incomePieChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

//一次性加载所有统计数据
async function fetchAll() {
  try {
    const [ov, pie, incomePie, trend, yearly] = await Promise.all([
      statisticsApi.getOverview(month.value || undefined),
      statisticsApi.getCategoryPie({ month: month.value || undefined, type: 0 }),
      statisticsApi.getCategoryPie({ month: month.value || undefined, type: 1 }),
      statisticsApi.getTrend(trendMonths.value),
      statisticsApi.getYearly(year.value),
    ])
    overview.value = ov
    pieData.value = pie
    incomePieData.value = incomePie
    trendData.value = trend
    yearlyData.value = yearly
    //等待 DOM 更新 → 渲染图表
    await Promise.resolve()
    renderPieChart()
    renderIncomePieChart()
    renderTrendChart()
    //窗口缩放时图表自适应
    window.addEventListener('resize', () => {
      pieChart?.resize()
      incomePieChart?.resize()
      trendChart?.resize()
    })
  } catch { }
}
//画支出饼图
function renderPieChart() {
  if (!pieChartRef.value) return
  if (!pieChart) pieChart = echarts.init(pieChartRef.value)

  //获取 DOM → 初始化图表
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    color: ['#2563eb', '#16a34a', '#ea580c', '#9333ea', '#0891b2', '#eab308', '#dc2626', '#6366f1', '#ec4899', '#84cc16'],
    series: [{
      type: 'pie',
      radius: ['45%', '72%'],
      center: ['50%', '50%'],
      label: { formatter: '{b}\n{d}%' },
      data: pieData.value.items.map((i) => ({ name: i.categoryName, value: i.amount })),
    }],
  })
}
// 收入饼图
function renderIncomePieChart() {
  if (!incomePieChartRef.value) return
  if (!incomePieChart) incomePieChart = echarts.init(incomePieChartRef.value)

  incomePieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    color: ['#16a34a', '#22c55e', '#4ade80', '#86efac', '#bbf7d0'],
    series: [{
      type: 'pie',
      radius: ['45%', '72%'],
      center: ['50%', '50%'],
      label: { formatter: '{b}\n{d}%' },
      data: incomePieData.value.items.map((i) => ({ name: i.categoryName, value: i.amount })),
    }],
  })
}
//画折线图
function renderTrendChart() {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)

  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['收入', '支出', '结余'], bottom: 0 },
    grid: { left: 20, right: 20, top: 20, bottom: 50 },
    xAxis: { type: 'category', data: trendData.value.map((t) => t.month) },
    yAxis: { type: 'value' },
    series: [
      { name: '收入', type: 'line', data: trendData.value.map((t) => t.income), smooth: true, color: '#16a34a', lineStyle: { width: 2 } },
      { name: '支出', type: 'line', data: trendData.value.map((t) => t.expense), smooth: true, color: '#dc2626', lineStyle: { width: 2 } },
      { name: '结余', type: 'line', data: trendData.value.map((t) => t.balance), smooth: true, color: '#2563eb', lineStyle: { width: 2 } },
    ],
  })
}

//监听 & 初始化
watch(month, () => fetchAll())

onMounted(fetchAll)
//页面切换 / 销毁时必须销毁图表
onUnmounted(() => {
  pieChart?.dispose()
  incomePieChart?.dispose()
  trendChart?.dispose()
  pieChart = null
  incomePieChart = null
  trendChart = null
})
</script>

<template>
  <div class="statistics-page">
    <div class="page-header">
      <h3 class="page-title">统计分析</h3>
      <el-date-picker v-model="month" type="month" placeholder="选择月份(默认当月)" value-format="YYYY-MM" clearable />
    </div>

    <!-- 月度概览卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="8">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">收入</div>
          <div class="stat-value income">{{ formatCurrency(overview.income) }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">支出</div>
          <div class="stat-value expense">{{ formatCurrency(overview.expense) }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">结余</div>
          <div class="stat-value" :class="overview.balance >= 0 ? 'positive' : 'negative'">
            {{ formatCurrency(overview.balance) }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 饼图 -->
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <span>支出分类占比</span>
            <span class="card-subtitle" v-if="pieData.totalAmount">
              合计: {{ formatCurrency(pieData.totalAmount) }}
            </span>
          </template>
          <div ref="pieChartRef" style="height: 350px" />
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <span>收入分类占比</span>
            <span class="card-subtitle" v-if="incomePieData.totalAmount">
              合计: {{ formatCurrency(incomePieData.totalAmount) }}
            </span>
          </template>
          <div ref="incomePieChartRef" style="height: 350px" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 折线图 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <span>月度趋势</span>
            <el-select v-model="trendMonths" style="width: 100px; margin-left: auto" @change="fetchAll">
              <el-option :value="6" label="6个月" />
              <el-option :value="12" label="12个月" />
              <el-option :value="24" label="24个月" />
            </el-select>
          </template>
          <div ref="trendChartRef" style="height: 340px" />
        </el-card>
      </el-col>
    </el-row>

    <!--年度总览 -->
    <el-card shadow="never" v-if="yearlyData">
      <template #header>
        <span>{{ yearlyData.year }} 年度总览</span>
        <span style="display: block; font-size: 10px; color: #94a3b8;">提示：月均支出: 总支出 / 有数据的月份数</span>
        <span style="display: block; font-size: 10px; color: #94a3b8;">提示：统计数据仅统计有数据月份</span>
      </template>
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="year-stat"><span class="ys-label">总收入</span><span class="ys-value income">{{
            formatCurrency(yearlyData.totalIncome) }}</span></div>
        </el-col>
        <el-col :span="6">
          <div class="year-stat"><span class="ys-label">总支出</span><span class="ys-value expense">{{
            formatCurrency(yearlyData.totalExpense) }}</span></div>
        </el-col>
        <el-col :span="6">
          <div class="year-stat"><span class="ys-label">结余</span><span class="ys-value positive">{{
            formatCurrency(yearlyData.balance) }}</span></div>
        </el-col>
        <el-col :span="6">
          <div class="year-stat"><span class="ys-label">月均支出</span><span class="ys-value">{{
            formatCurrency(yearlyData.monthlyAvgExpense) }}</span></div>
        </el-col>
        <el-col :span="6">
          <div class="year-stat"><span class="ys-label">最高支出月</span><span class="ys-value">{{
            yearlyData.highestExpenseMonth
              }} ({{ formatCurrency(yearlyData.highestExpenseAmount) }})</span></div>
        </el-col>
        <el-col :span="6">
          <div class="year-stat"><span class="ys-label">最低支出月</span>
            <span class="ys-value">{{ yearlyData.lowestExpenseMonth }} ({{
              formatCurrency(yearlyData.lowestExpenseAmount)
            }})</span>
          </div>
        </el-col>
        <el-col :span="6">

        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-title {
  font-size: 20px;
  color: #1e293b;
  margin: 0;
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

.stat-value.income {
  color: #16a34a;
}

.stat-value.expense {
  color: #dc2626;
}

.stat-value.positive {
  color: #2563eb;
}

.stat-value.negative {
  color: #dc2626;
}

.chart-card :deep(.el-card__header) {
  display: flex;
  align-items: center;
  font-weight: 600;
}

.card-subtitle {
  font-size: 13px;
  color: #94a3b8;
  margin-left: 12px;
  font-weight: 400;
}

.year-stat {
  text-align: center;
  padding: 8px 0;
}

.ys-label {
  display: block;
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 6px;
}

.ys-value {
  font-size: 16px;
  font-weight: 600;
  color: #334155;
  display: block;
}

.ys-value.income {
  color: #16a34a;
}

.ys-value.expense {
  color: #dc2626;
}

.ys-value.positive {
  color: #2563eb;
}
</style>

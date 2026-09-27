<template>
  <view class="plan-page">
    <!-- 汇总信息 -->
    <view class="summary-card">
      <view class="summary-grid">
        <view class="summary-item">
          <text class="s-label">总投入</text>
          <text class="s-value">{{ formatMoney(summary.totalInvested) }}</text>
        </view>
        <view class="summary-item">
          <text class="s-label">总市值</text>
          <text class="s-value">{{ formatMoney(summary.totalCurrentValue) }}</text>
        </view>
        <view class="summary-item">
          <text class="s-label">总收益</text>
          <text class="s-value" :class="summary.totalProfit >= 0 ? 'income' : 'expense'">
            {{ formatMoney(summary.totalProfit) }}
          </text>
        </view>
        <view class="summary-item">
          <text class="s-label">收益率</text>
          <text class="s-value" :class="summary.overallProfitRate >= 0 ? 'income' : 'expense'">
            {{ summary.overallProfitRate != null ? summary.overallProfitRate.toFixed(2) + '%' : '-' }}
          </text>
        </view>
      </view>
    </view>

    <!-- 筛选 -->
    <view class="filter-row">
      <view class="filter-tab" :class="{ active: filterStatus === '' }" @click="changeFilter('')">全部</view>
      <view class="filter-tab" :class="{ active: filterStatus === 0 }" @click="changeFilter(0)">持有中</view>
      <view class="filter-tab" :class="{ active: filterStatus === 1 }" @click="changeFilter(1)">已赎回</view>
    </view>

    <!-- 计划列表 -->
    <scroll-view scroll-y class="plan-scroll" @scrolltolower="loadMore">
      <view v-if="list.length > 0">
        <view v-for="item in list" :key="item.id" class="plan-card" @click="goDetail(item.id)">
          <view class="plan-header">
            <view class="plan-status" :class="item.status === 0 ? 'holding' : 'redeemed'">
              {{ item.status === 0 ? '持有中' : '已赎回' }}
            </view>
            <text class="plan-name">{{ item.name }}</text>
          </view>
          <view class="plan-body">
            <view class="plan-row">
              <text class="plan-label">投入金额</text>
              <text class="plan-value">{{ formatMoney(item.initialAmount) }}</text>
            </view>
            <view class="plan-row">
              <text class="plan-label">当前市值</text>
              <text class="plan-value">{{ formatMoney(item.currentValue) }}</text>
            </view>
            <view class="plan-row">
              <text class="plan-label">收益</text>
              <text class="plan-value" :class="item.profitAmount >= 0 ? 'income' : 'expense'">
                {{ item.profitAmount >= 0 ? '+' : '' }}{{ formatMoney(item.profitAmount) }}
                （{{ item.profitRate != null ? item.profitRate.toFixed(2) + '%' : '-' }}）
              </text>
            </view>
            <view class="plan-row">
              <text class="plan-label">开始日期</text>
              <text class="plan-value">{{ item.startDate }}</text>
            </view>
          </view>
        </view>
        <view v-if="loading" class="loading-tip">加载中...</view>
        <view v-if="noMore && list.length > 0" class="loading-tip">— 没有更多了 —</view>
      </view>
      <view v-if="!loading && list.length === 0" class="empty-block">
        <text class="empty-icon">📈</text>
        <text class="empty-text">暂无理财计划</text>
      </view>
    </scroll-view>

    <!-- 添加按钮 -->
    <view class="float-btn" @click="goAdd">
      <text style="font-size: 48rpx; color: #fff;">+</text>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive} from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { onShow } from '@dcloudio/uni-app'
import { getFinancePlans } from '@/api/index.js'
import { formatMoney } from '@/utils/index.js'

const filterStatus = ref('')
const list = ref([])
const page = ref(1)
const loading = ref(false)
const noMore = ref(false)

const summary = reactive({
  totalInvested: 0,
  totalCurrentValue: 0,
  totalProfit: 0,
  overallProfitRate: 0
})

function changeFilter(status) {
  filterStatus.value = status
  page.value = 1
  list.value = []
  loadData()
}

function goAdd() { uni.navigateTo({ url: '/pages/plan/add' }) }
function goDetail(id) { uni.navigateTo({ url: `/pages/plan/detail?id=${id}` }) }

async function loadData() {
  if (loading.value) return
  loading.value = true
  try {
    const params = { page: page.value, size: 10 }
    if (filterStatus.value !== '') params.status = filterStatus.value
    const res = await getFinancePlans(params)
    if (res.data) {
      if (page.value === 1) {
        list.value = res.data.records || []
      } else {
        list.value = list.value.concat(res.data.records || [])
      }
      if (res.data.summary) {
        summary.totalInvested = res.data.summary.totalInvested || 0
        summary.totalCurrentValue = res.data.summary.totalCurrentValue || 0
        summary.totalProfit = res.data.summary.totalProfit || 0
        summary.overallProfitRate = res.data.summary.overallProfitRate || 0
      }
      noMore.value = (res.data.records || []).length < 10
    }
  } catch (err) { console.error(err) }
  finally { loading.value = false }
}

function loadMore() {
  if (loading.value || noMore.value) return
  page.value++
  loadData()
}

onShow(() => {
  page.value = 1
  list.value = []
  loadData()
})
</script>

<style lang="scss" scoped>
.plan-page {
  min-height: 100vh;
  background: #F5F7FA;
  position: relative;
}

.summary-card {
  margin: 20rpx 24rpx;
  background: linear-gradient(135deg, #EEF2FF, #FFFFFF);
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03);
}

.summary-grid {
  display: flex;
  flex-wrap: wrap;

  .summary-item {
    width: 50%;
    padding: 12rpx 0;
    text-align: center;

    .s-label { display: block; font-size: 24rpx; color: #999; margin-bottom: 6rpx; }
    .s-value { display: block; font-size: 28rpx; font-weight: 600; color: #333;
      &.income { color: #10B981; }
      &.expense { color: #EF4444; }
    }
  }
}

.filter-row {
  display: flex;
  padding: 0 24rpx 16rpx;
  gap: 16rpx;

  .filter-tab {
    font-size: 26rpx;
    padding: 10rpx 28rpx;
    border-radius: 24rpx;
    color: #666;
    background: #FFF;

    &.active { background: #EEF2FF; color: #3B6FE8; font-weight: 500; }
  }
}

.plan-scroll { height: calc(100vh - 420rpx); }

.plan-card {
  margin: 0 24rpx 16rpx;
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);

  .plan-header {
    display: flex;
    align-items: center;
    gap: 12rpx;
    margin-bottom: 20rpx;
    padding-bottom: 16rpx;
    border-bottom: 1rpx solid #F0F2F5;

    .plan-status {
      font-size: 22rpx;
      padding: 4rpx 14rpx;
      border-radius: 12rpx;

      &.holding { background: #ECFDF5; color: #10B981; }
      &.redeemed { background: #F3F4F6; color: #999; }
    }

    .plan-name { font-size: 30rpx; font-weight: 600; color: #1A1A2E; flex: 1; }
  }

  .plan-body {
    .plan-row {
      display: flex;
      justify-content: space-between;
      padding: 8rpx 0;

      .plan-label { font-size: 26rpx; color: #999; }
      .plan-value {
        font-size: 26rpx; color: #333; font-weight: 500;
        &.income { color: #10B981; }
        &.expense { color: #EF4444; }
      }
    }
  }
}

.loading-tip { text-align: center; padding: 24rpx; font-size: 24rpx; color: #C0C0C0; }

.empty-block {
  display: flex; flex-direction: column; align-items: center; padding-top: 200rpx;
  .empty-icon { font-size: 96rpx; margin-bottom: 20rpx; }
  .empty-text { font-size: 28rpx; color: #999; }
}

.float-btn {
  position: fixed;
  bottom: 140rpx; right: 40rpx;
  width: 104rpx; height: 104rpx;
  background: linear-gradient(135deg, #4F8CFF, #3B6FE8);
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(59,111,232,0.4);
  z-index: 10;
}
</style>

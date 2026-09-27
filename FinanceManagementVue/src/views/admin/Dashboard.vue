<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/modules/admin'
import type { AdminDashboard } from '@/types'
import { UserFilled, User, DataLine, Coin, Notebook, TrendCharts } from '@element-plus/icons-vue'

const data = ref<AdminDashboard>({
  totalUsers: 0,//总用户
  activeUsersToday: 0,//今日在线用户
  newUsersThisWeek: 0,//本章新增
  newUsersThisMonth: 0,//本月新增
  totalBills: 0,//总账单数
  billsToday: 0,//今日账单
  totalPlans: 0,//理财计划
  totalMemos: 0,//备忘录数
})

async function fetchDashboard() {
  try {
    data.value = await adminApi.getDashboard()
  } catch {  }
}

onMounted(fetchDashboard)
</script>

<template>
  <div class="admin-dashboard">
    <h3 class="page-title">管理后台</h3>

    <h4 class="section-title">用户统计</h4>
    <el-row :gutter="16" style="margin-bottom: 24px">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-icon" style="background: #eff6ff"><el-icon :size="20" color="#2563eb">
              <UserFilled />
            </el-icon></div>
          <div class="stat-info"><span class="label">总用户数</span><span class="value">{{ data.totalUsers }}</span></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-icon" style="background: #f0fdf4"><el-icon :size="20" color="#16a34a">
              <User />
            </el-icon></div>
          <div class="stat-info"><span class="label">今日活跃</span><span class="value">{{ data.activeUsersToday }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-icon" style="background: #fefce8"><el-icon :size="20" color="#eab308">
              <User />
            </el-icon></div>
          <div class="stat-info"><span class="label">本周新增</span><span class="value">{{ data.newUsersThisWeek }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-icon" style="background: #fdf2f8"><el-icon :size="20" color="#ec4899">
              <User />
            </el-icon></div>
          <div class="stat-info"><span class="label">本月新增</span><span class="value">{{ data.newUsersThisMonth }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <h4 class="section-title">数据统计</h4>
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-icon" style="background: #eff6ff"><el-icon :size="20" color="#2563eb">
              <DataLine />
            </el-icon></div>
          <div class="stat-info"><span class="label">总账单</span><span class="value">{{ data.totalBills }}</span></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-icon" style="background: #f0fdf4"><el-icon :size="20" color="#16a34a">
              <Coin />
            </el-icon></div>
          <div class="stat-info"><span class="label">今日记账</span><span class="value">{{ data.billsToday }}</span></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-icon" style="background: #fefce8"><el-icon :size="20" color="#eab308">
              <TrendCharts />
            </el-icon></div>
          <div class="stat-info"><span class="label">理财计划</span><span class="value">{{ data.totalPlans }}</span></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-icon" style="background: #fdf2f8"><el-icon :size="20" color="#ec4899">
              <Notebook />
            </el-icon></div>
          <div class="stat-info"><span class="label">备忘录</span><span class="value">{{ data.totalMemos }}</span></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.page-title {
  font-size: 20px;
  color: #1e293b;
  margin: 0 0 4px;
}

.section-title {
  font-size: 15px;
  color: #64748b;
  margin: 0 0 12px;
  font-weight: 500;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 8px 0;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-info .label {
  font-size: 13px;
  color: #94a3b8;
}

.stat-info .value {
  font-size: 24px;
  font-weight: 700;
  color: #334155;
}
</style>

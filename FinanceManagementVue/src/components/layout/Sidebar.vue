<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import {
  HomeFilled, Money, TrendCharts, PieChart, Notebook, MagicStick,
  Setting, UserFilled, Grid, List, Fold, Expand,
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const appStore = useAppStore()

const currentPath = computed(() => route.path)

const menuItems = computed(() => {
  // 普通用户都能看见的菜单
  const items = [
    { path: '/dashboard', title: '首页', icon: HomeFilled },
    { path: '/bills', title: '收支账单', icon: Money },
    { path: '/finance-plans', title: '理财计划', icon: TrendCharts },
    { path: '/statistics', title: '统计分析', icon: PieChart },
    { path: '/memos', title: '备忘录', icon: Notebook },
    { path: '/ai', title: 'AI 助手', icon: MagicStick },
    { path: '/profile', title: '个人设置', icon: Setting },
  ]

  //只有管理员才显示下面 3 个菜单
  if (authStore.isAdmin) {
    items.push(
      { path: '/admin/dashboard', title: '管理后台', icon: Grid },
      { path: '/admin/users', title: '用户管理', icon: UserFilled },
      { path: '/admin/categories', title: '分类管理', icon: List },
    )
  }
  return items
})
//跳转方法
function navigate(path: string) {
  router.push(path)
}
</script>

<template>
  <!-- 左侧菜单 -->
  <div class="sidebar-wrapper">

    <div class="sidebar-logo">
      <span v-if="!appStore.sidebarCollapsed" class="logo-text">财务管理</span>
      <span v-else class="logo-short">FM</span>
    </div>

    <el-menu
      :default-active="currentPath"
      :collapse="appStore.sidebarCollapsed"
      background-color="#1e293b"
      text-color="#94a3b8"
      active-text-color="#60a5fa"
      router
      class="sidebar-menu"
    >
      <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path" @click="navigate(item.path)">
        <el-icon><component :is="item.icon" /></el-icon>
        <template #title>{{ item.title }}</template>
      </el-menu-item>
      
    </el-menu>
  </div>
</template>

<style scoped>
.sidebar-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.sidebar-logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  border-bottom: 1px solid #334155;
}

.logo-short {
  font-size: 16px;
  letter-spacing: 1px;
}

.sidebar-menu {
  border-right: none;
  flex: 1;
}
</style>

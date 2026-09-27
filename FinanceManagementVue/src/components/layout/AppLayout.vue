<script setup lang="ts">
import Sidebar from './Sidebar.vue'//导入侧边栏
import AppHeader from './AppHeader.vue'//顶部头部
import { useAppStore } from '@/stores/app'//拿折叠状态

const appStore = useAppStore()
</script>

<template>
  <!-- 骨架布局 -->
  <el-container class="app-container">

    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '220px'" class="app-aside">
      <Sidebar />
    </el-aside>

    <el-container>

      <el-header class="app-header">
        <AppHeader />
      </el-header>

      <el-main class="app-main">
        <router-view />
      </el-main>

    </el-container>
  </el-container>

</template>

<style scoped>
.app-container {
  height: 100vh;
}

.app-aside {
  background: #1e293b;
  transition: width 0.3s;
  overflow-x: hidden;
}

.app-header {
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  padding: 0 20px;
  display: flex;
  align-items: center;
  height: 56px;
}

.app-main {
  background: #f1f5f9;
  padding: 20px;
  overflow-y: auto;
}
</style>

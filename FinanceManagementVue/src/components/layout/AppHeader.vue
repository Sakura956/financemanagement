<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { Fold, Expand, SwitchButton, User } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

//退出登录
function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <!-- 页面顶部导航栏,固定在所有页面最上方 -->
  <div class="header-wrapper">
    <div class="header-left">
      <!-- 控制侧边栏的折叠 -->
      <el-button :icon="appStore.sidebarCollapsed ? Expand : Fold" text @click="appStore.toggleSidebar()" />
      <span class="header-title">个人财务管理系统</span>
    </div>

    <div class="header-right">
      <!-- 下拉菜单 -->
      <el-dropdown trigger="click">
        <span class="user-info">
          <!-- <el-avatar :size="32" icon="UserFilled" /> -->
          <el-avatar :size="32" :src="authStore.userInfo?.avatarUrl" icon="UserFilled" />
          <span class="nickname">{{ authStore.userInfo?.nickname || '用户' }}</span>
          <el-tag v-if="authStore.isAdmin" size="small" type="danger" effect="dark">管理员</el-tag>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item :icon="User" @click="router.push('/profile')">个人设置</el-dropdown-item>
            <el-dropdown-item :icon="SwitchButton" divided @click="handleLogout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<style scoped>
.header-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-title {
  font-size: 15px;
  font-weight: 500;
  color: #1e293b;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.nickname {
  font-size: 14px;
  color: #334155;
}
</style>

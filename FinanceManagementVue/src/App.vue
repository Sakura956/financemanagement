<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { onMounted } from 'vue'

const authStore = useAuthStore()
const router = useRouter()

onMounted(async () => {
  // 用户已经登录（有 token），但是还没有获取用户信息
  if (authStore.isLoggedIn && !authStore.userInfo) {
    try {
      //就去后台拉取用户信息
      await authStore.fetchProfile()
    } catch {
      //如果失败（token 过期 / 无效） → 清空登录状态 → 跳回登录页
      authStore.clearAuth()
      router.push('/login')
    }
  }
})
</script>

<template>
  <router-view />
</template>

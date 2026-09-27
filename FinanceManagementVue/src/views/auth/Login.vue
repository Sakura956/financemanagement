<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()// 表单DOM引用（用于校验）
const loading = ref(false)

const form = reactive({
  phone: '',
  password: '',
})

const rules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' },
  ],
}

//登录提交
async function handleLogin() {
  if (!formRef.value) return
  //校验表单
  //.catch(() => false),如果校验不通过，.validate() 会报错,保证报错时不会崩溃，而是返回 false
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    //调用 Pinia 的登录方法
    await authStore.login({ phone: form.phone, password: form.password })
    router.push('/dashboard')
  } catch {
    // 错误由 axios 拦截器统一提示
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2 class="auth-title">登录</h2>
      <p class="auth-subtitle">个人财务管理系统</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" class="auth-btn" size="large">
          登 录
        </el-button>
      </el-form>

      <p class="auth-link">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
}

.auth-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

.auth-title {
  text-align: center;
  font-size: 24px;
  color: #1e293b;
  margin: 0 0 4px;
}

.auth-subtitle {
  text-align: center;
  color: #94a3b8;
  margin: 0 0 32px;
  font-size: 14px;
}

.auth-btn {
  width: 100%;
  margin-top: 8px;
}

.auth-link {
  text-align: center;
  margin: 20px 0 0;
  font-size: 14px;
  color: #64748b;
}

.auth-link a {
  color: #2563eb;
  text-decoration: none;
}
</style>

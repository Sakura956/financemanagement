<template>
  <view class="pwd-page">
    <view class="form-card">
      <view class="form-item">
        <text class="f-label">旧密码</text>
        <input class="f-input" v-model="form.oldPassword" type="password" placeholder="请输入旧密码" placeholder-style="color:#C0C0C0" maxlength="20" />
      </view>
      <view class="form-item">
        <text class="f-label">新密码</text>
        <input class="f-input" v-model="form.newPassword" type="password" placeholder="6-20位，含字母和数字" placeholder-style="color:#C0C0C0" maxlength="20" />
      </view>
      <view class="form-item">
        <text class="f-label">确认密码</text>
        <input class="f-input" v-model="confirmPwd" type="password" placeholder="再次输入新密码" placeholder-style="color:#C0C0C0" maxlength="20" />
      </view>
    </view>

    <!-- 密码规则 -->
    <view class="tips-card">
      <text class="tip-title">密码要求：</text>
      <text class="tip-item" :class="{ valid: pwdValid }">✓ 6-20位字符</text>
      <text class="tip-item" :class="{ valid: pwdHasLetter }">✓ 包含字母</text>
      <text class="tip-item" :class="{ valid: pwdHasNumber }">✓ 包含数字</text>
    </view>

    <view class="submit-area">
      <button class="btn-primary" :loading="submitting" @click="handleSave">修改密码</button>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { onShow } from '@dcloudio/uni-app'
import { useAuthStore } from '@/store/auth.js'
import { changePassword as changePwdApi } from '@/api/index.js'
import { validatePassword } from '@/utils/index.js'

const authStore = useAuthStore()
const submitting = ref(false)
const confirmPwd = ref('')

const form = reactive({
  oldPassword: '',
  newPassword: ''
})

const pwdValid = computed(() => form.newPassword.length >= 6 && form.newPassword.length <= 20)
const pwdHasLetter = computed(() => /[a-zA-Z]/.test(form.newPassword))
const pwdHasNumber = computed(() => /\d/.test(form.newPassword))

async function handleSave() {
  if (!form.oldPassword) {
    uni.showToast({ title: '请输入旧密码', icon: 'none' })
    return
  }

  if (!validatePassword(form.newPassword)) {
    uni.showToast({ title: '新密码需6-20位，含字母和数字', icon: 'none' })
    return
  }

  if (form.newPassword !== confirmPwd.value) {
    uni.showToast({ title: '两次密码输入不一致', icon: 'none' })
    return
  }

  if (form.oldPassword === form.newPassword) {
    uni.showToast({ title: '新密码不能与旧密码相同', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    await changePwdApi({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })
    uni.showToast({ title: '密码修改成功，请重新登录', icon: 'success' })
    setTimeout(() => {
      authStore.logout()
    }, 1500)
  } catch (err) { console.error(err) }
  finally { submitting.value = false }
}
</script>

<style lang="scss" scoped>
.pwd-page {
  min-height: 100vh;
  background: #F5F7FA;
}

.form-card {
  margin: 20rpx 24rpx;
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 0 28rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03);
}

.form-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 0;
  border-bottom: 1rpx solid #F0F2F5;

  &:last-child { border-bottom: none; }

  .f-label {
    font-size: 28rpx;
    color: #666;
    width: 140rpx;
    flex-shrink: 0;
  }

  .f-input {
    flex: 1;
    text-align: right;
    font-size: 28rpx;
    color: #333;
    height: 60rpx;
  }
}

.tips-card {
  margin: 20rpx 32rpx 0;
  padding: 24rpx;
  background: #FFFFFF;
  border-radius: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);

  .tip-title {
    display: block;
    font-size: 26rpx;
    color: #666;
    margin-bottom: 12rpx;
  }

  .tip-item {
    display: block;
    font-size: 24rpx;
    color: #C0C0C0;
    padding: 4rpx 0;

    &.valid { color: #10B981; }
  }
}

.submit-area {
  padding: 48rpx 32rpx;
}
</style>

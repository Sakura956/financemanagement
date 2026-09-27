<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadRawFile } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const { userInfo } = storeToRefs(authStore)
const router = useRouter()
//获取两个表单实例，用于手动校验
const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
//按钮加载状态，防止重复提交
const profileLoading = ref(false)
const passwordLoading = ref(false)

const profileForm = reactive({
  nickname: authStore.userInfo?.nickname || '',
  avatarUrl: authStore.userInfo?.avatarUrl || '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const profileRules: FormRules = {
  nickname: [{ min: 1, max: 20, message: '昵称长度为 1-20 位', trigger: 'blur' }],
}

//密码校验
const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次密码输入不一致'))
  } else {
    callback()
  }
}

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' },
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d)/, message: '密码需包含字母和数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}
//更新资料方法
async function handleUpdateProfile() {
  if (!profileFormRef.value) return
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return

  profileLoading.value = true
  try {
    await authStore.updateProfile({
      nickname: profileForm.nickname || undefined,
      avatarUrl: profileForm.avatarUrl || undefined,
    })
    ElMessage.success('个人信息更新成功')
  } catch { } finally {
    profileLoading.value = false
  }
}


// 上传头像前校验
// 符合后端要求：5MB + 图片格式
const beforeAvatarUpload = (rawFile: UploadRawFile) => {
  // 校验文件类型（只允许图片）
  const isImage = rawFile.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件！')
    return false
  }

  //校验文件大小（≤5MB）
  const maxSize = 5 * 1024 * 1024 // 5MB
  const isLt5M = rawFile.size < maxSize
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB！')
    return false
  }
  // 校验通过
  return true
}

// 自定义上传：调用 Pinia 仓库方法
const uploadAvatar = async (options: any) => {
  try {
    // options.file 就是选中的图片文件
    const file = options.file
    // 调用 pinia 里的上传方法
    await authStore.updateAvatar(file)

    //上传成功后，同步更新页面显示的头像
    profileForm.avatarUrl = authStore.userInfo?.avatarUrl || ''
    // 成功提示
    ElMessage.success('头像更新成功！')
  } catch (err) {
    ElMessage.error('头像更新失败，请重试')
    console.error(err)
  }
}


//修改密码方法
async function handleChangePassword() {
  if (!passwordFormRef.value) return
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  passwordLoading.value = true
  try {
    await authStore.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    router.push('/login')
  } catch { } finally {
    passwordLoading.value = false
  }
}
</script>

<template>
  <div class="profile-page">
    <h3 class="page-title">个人设置</h3>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span>基本信息</span>
          </template>
          <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="80px">

            <el-form-item label="头像">
              <!-- Element Plus 上传组件 -->
              <el-upload class="avatar-uploader" :key="userInfo?.avatarUrl" :http-request="uploadAvatar"
                :show-file-list="false" :before-upload="beforeAvatarUpload">
                <!-- 显示头像：优先取接口返回的头像，没有则显示加号 -->
                <img v-if="userInfo?.avatarUrl" :src="userInfo.avatarUrl" class="avatar"
                  style="width: 80px; height: 80px; object-fit: cover; border-radius: 8px" />
                <el-icon v-else class="avatar-uploader-icon">
                  <Plus />
                </el-icon>
              </el-upload>
            </el-form-item>
            <el-form-item label="手机号">
              <el-input :model-value="authStore.userInfo?.phone" disabled />
            </el-form-item>
            <el-form-item label="角色">
              <el-tag :type="authStore.isAdmin ? 'danger' : 'primary'" effect="light">
                {{ authStore.isAdmin ? '管理员' : '普通用户' }}
              </el-tag>
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="profileForm.nickname" maxlength="20" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="profileLoading" @click="handleUpdateProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span>修改密码</span>
          </template>
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px">
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="6-20位，需包含字母和数字" />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
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
</style>

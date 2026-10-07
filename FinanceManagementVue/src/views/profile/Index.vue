<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadRawFile } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { reportApi } from '@/api/modules/report'
import type { WeeklyReportItem } from '@/types'

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

// ===== AI 周报邮件推送（Resend） =====
const pushFormRef = ref<FormInstance>()
const pushLoading = ref(false)
const testLoading = ref(false)
const previewLoading = ref(false)
const sendNowLoading = ref(false)
//是否已保存过邮箱（未保存时测试邮件/立即发送按钮不可用）
const savedEmail = ref('')

const pushForm = reactive({
  email: '',
  weeklyEnabled: false,
  frequency: 'WEEKLY' as 'DAILY' | 'WEEKLY',
  dayOfWeek: 7,
  sendHour: 20,
})

//周几选项（1-7 对应周一到周日）
const dayOptions = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 },
]
//发送时间选项（0-23 点）
const hourOptions = Array.from({ length: 24 }, (_, h) => ({
  label: `${String(h).padStart(2, '0')}:00`,
  value: h,
}))

const pushRules: FormRules = {
  email: [
    { required: true, message: '请输入接收邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] },
  ],
}

//最近存档报告
const reports = ref<WeeklyReportItem[]>([])
//报告内容弹窗（预览 / 历史报告共用）
const reportDialog = reactive({
  visible: false,
  title: '',
  content: '',
})

//加载推送配置 + 最近报告
async function loadPushData() {
  try {
    const config = await reportApi.getPushConfig()
    pushForm.email = config.email || ''
    pushForm.weeklyEnabled = !!config.weeklyEnabled
    pushForm.frequency = config.frequency === 'DAILY' ? 'DAILY' : 'WEEKLY'
    pushForm.dayOfWeek = config.dayOfWeek || 7
    pushForm.sendHour = config.sendHour ?? 20
    savedEmail.value = config.email || ''
  } catch { }
  try {
    reports.value = await reportApi.listReports()
  } catch { }
}

//保存推送配置
async function handleSavePushConfig() {
  if (!pushFormRef.value) return
  const valid = await pushFormRef.value.validate().catch(() => false)
  if (!valid) return

  pushLoading.value = true
  try {
    await reportApi.updatePushConfig({
      email: pushForm.email.trim(),
      weeklyEnabled: pushForm.weeklyEnabled,
      frequency: pushForm.frequency,
      dayOfWeek: pushForm.dayOfWeek,
      sendHour: pushForm.sendHour,
    })
    savedEmail.value = pushForm.email.trim()
    ElMessage.success('推送配置保存成功')
  } catch { } finally {
    pushLoading.value = false
  }
}

//发送测试邮件
async function handleSendTest() {
  testLoading.value = true
  try {
    await reportApi.sendTestEmail()
    ElMessage.success('测试邮件已发送，请查收')
  } catch { } finally {
    testLoading.value = false
  }
}

//生成并预览本周报告（不发送）
async function handlePreviewReport() {
  previewLoading.value = true
  try {
    const preview = await reportApi.previewReport()
    reportDialog.title = preview.title
    reportDialog.content = preview.content
    reportDialog.visible = true
  } catch { } finally {
    previewLoading.value = false
  }
}

//立即生成并发送一份报告
async function handleSendNow() {
  sendNowLoading.value = true
  try {
    await reportApi.sendNow()
    ElMessage.success('报告已生成并发送到你的邮箱')
    //刷新最近报告列表（发送成功后新增一条记录）
    try {
      reports.value = await reportApi.listReports()
    } catch { }
  } catch { } finally {
    sendNowLoading.value = false
  }
}

//查看历史报告
function openReport(item: WeeklyReportItem) {
  reportDialog.title = item.title
  reportDialog.content = item.content
  reportDialog.visible = true
}

//发送状态显示
function sendStatusText(status: number): string {
  if (status === 1) return '已发送'
  if (status === 2) return '发送失败'
  return '未发送'
}
function sendStatusType(status: number): 'success' | 'danger' | 'info' {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'info'
}

onMounted(loadPushData)


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

    <!-- AI 周报邮件推送（Resend） -->
    <el-card shadow="never" class="push-card">
      <template #header>
        <div class="push-header">
          <span>AI 报告邮件推送</span>
          <span class="push-tip">开启后按你设置的发送计划，由 AI 汇总账单数据生成报告并发送到邮箱</span>
        </div>
      </template>
      <el-form ref="pushFormRef" :model="pushForm" :rules="pushRules" label-width="100px">
        <el-form-item label="接收邮箱" prop="email">
          <el-input v-model="pushForm.email" placeholder="example@mail.com" maxlength="100" style="max-width: 360px" />
        </el-form-item>
        <el-form-item label="推送开关">
          <el-switch v-model="pushForm.weeklyEnabled" active-text="开启" inactive-text="关闭" />
        </el-form-item>
        <el-form-item label="发送频率">
          <el-radio-group v-model="pushForm.frequency">
            <el-radio-button value="WEEKLY">每周</el-radio-button>
            <el-radio-button value="DAILY">每日</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="pushForm.frequency === 'WEEKLY'" label="发送日">
          <el-select v-model="pushForm.dayOfWeek" style="width: 140px">
            <el-option v-for="d in dayOptions" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="发送时间">
          <el-select v-model="pushForm.sendHour" style="width: 140px">
            <el-option v-for="h in hourOptions" :key="h.value" :label="h.label" :value="h.value" />
          </el-select>
          <span class="push-tip" style="margin-left: 12px">（报告会统计发送前 7 天的数据）</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="pushLoading" @click="handleSavePushConfig">保存设置</el-button>
          <el-button type="success" :loading="sendNowLoading" :disabled="!savedEmail" @click="handleSendNow">
            {{ sendNowLoading ? 'AI 生成并发送中…' : '立即发送一次' }}
          </el-button>
          <el-button :loading="testLoading" :disabled="!savedEmail" @click="handleSendTest">发送测试邮件</el-button>
          <el-button :loading="previewLoading" @click="handlePreviewReport">
            {{ previewLoading ? 'AI 生成中…' : '预览报告' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 最近报告 -->
      <div v-if="reports.length" class="recent-reports">
        <div class="recent-title">最近报告</div>
        <el-table :data="reports" size="small" @row-click="openReport" class="report-table">
          <el-table-column prop="createTime" label="生成时间" width="180" />
          <el-table-column prop="title" label="报告" min-width="200" show-overflow-tooltip />
          <el-table-column label="发送状态" width="100">
            <template #default="{ row }">
              <el-tag :type="sendStatusType(row.sendStatus)" size="small">{{ sendStatusText(row.sendStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default>
              <el-button type="primary" link size="small">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 报告内容弹窗（预览 / 历史共用） -->
    <el-dialog v-model="reportDialog.visible" :title="reportDialog.title" width="680px" top="8vh">
      <pre class="report-content">{{ reportDialog.content }}</pre>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-title {
  font-size: 20px;
  color: #1e293b;
  margin: 0 0 20px;
}

.push-card {
  margin-top: 20px;
}

.push-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.push-tip {
  font-size: 12px;
  color: #94a3b8;
}

.recent-reports {
  margin-top: 8px;
}

.recent-title {
  font-size: 14px;
  color: #1e293b;
  margin-bottom: 10px;
}

.report-table {
  cursor: pointer;
}

.report-content {
  margin: 0;
  max-height: 60vh;
  overflow-y: auto;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.8;
  color: #1f2937;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { aiApi } from '@/api/modules/ai'
import type { AISession, AIMessage } from '@/types'
import { MagicStick, Delete, Plus, Service } from '@element-plus/icons-vue'
import { formatDateTimeDeleteT } from '@/utils'

const sessions = ref<AISession[]>([])// 会话列表
const messages = ref<AIMessage[]>([])// 当前对话消息
const currentSessionId = ref<string | null>(null)// 当前选中的会话ID
const inputMessage = ref('')// 输入框内容
const loading = ref(false)// 发送中
const messagesContainer = ref<HTMLDivElement>()// 消息容器 DOM（用于自动滚动到底部）
const sessionsLoading = ref(false)// 会话列表加载状态

//获取会话列表
async function fetchSessions() {
  // 打开加载动画
  sessionsLoading.value = true
  try {
    // 请求后端接口，获取会话列表（最多50条）
    const result = await aiApi.getSessions({ page: 1, size: 50 })
    sessions.value = result.records
  } finally {
    sessionsLoading.value = false
  }
}
//获取某会话的消息
async function fetchMessages(sessionId: string) {
  try {
    // 根据会话ID获取聊天记录
    const result = await aiApi.getSessionMessages(sessionId, { page: 1, size: 100 })
    // console.log('获取会话消息', result)
    messages.value = result.records
    // 滚动到底部
    scrollToBottom()
  } catch { }
}
//选择会话
function selectSession(session: AISession) {
  // 记录当前选中的会话ID
  currentSessionId.value = session.sessionId
  fetchMessages(session.sessionId)
}
//新建对话
async function newChat() {
  // 清空当前会话
  currentSessionId.value = null
  // 清空消息
  messages.value = []
}
//删除会话
async function handleDeleteSession(sessionId: string, event: Event) {
  // 阻止事件冒泡（不触发点击会话项）
  event.stopPropagation()
  await ElMessageBox.confirm('确定要删除该会话吗？', '确认删除', { type: 'warning' })

  try {
    await aiApi.deleteSession(sessionId)
    // 如果删除的是当前打开的会话，则清空聊天界面
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = null
      messages.value = []
    }
    // 前端同步删除列表项
    sessions.value = sessions.value.filter((s) => s.sessionId !== sessionId)
    ElMessage.success('已删除')
  } catch { }
}

// 发送消息
async function sendMessage() {
  // 1. 获取输入框内容，并去掉首尾空格
  const msg = inputMessage.value.trim()
  // 2. 如果内容为空 或 正在发送中，则直接返回
  if (!msg || loading.value) return

  // 3. 把用户消息添加到界面
  messages.value.push({
    id: Date.now(),
    role: 'user',
    content: msg,
    tokensUsed: 0,
    createTime: new Date().toISOString(),
  })
  // 4. 清空输入框
  inputMessage.value = ''
  // 5. 等待DOM更新
  await nextTick()
  // 6. 滚动到底部
  scrollToBottom()

  // 7. 打开发送状态
  loading.value = true

  try {
    // 10. 请求后端流式接口
    const res = await aiApi.chat({
      sessionId: currentSessionId.value || undefined,
      message: msg,
      includeHistory: true,
    })

    // ✅ 关键修复：把返回的 sessionId 保存起来，下次对话会带上
    if (res.sessionId) {
      currentSessionId.value = res.sessionId
    }

    //直接添加AI回复
    messages.value.push({
      id: Date.now(),
      role: 'assistant',
      content: res.message,
      tokensUsed: res.tokensUsed || 0,
      createTime: new Date().toISOString(),
    })

    // 刷新会话列表
    fetchSessions()

  } catch (err) {
    ElMessage.error('AI 服务暂不可用')
  } finally {
    loading.value = false
    scrollToBottom()
  }

}
//自动滚动到底部
function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      // 让滚动条等于最大高度 = 自动到底部
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

onMounted(fetchSessions)
</script>

<template>
  <div class="ai-page">
    <div class="ai-layout">
      <!-- 左侧：会话列表栏 -->
      <div class="ai-sidebar">
        <div class="sidebar-header">
          <el-button type="primary" :icon="Plus" size="small" @click="newChat">新对话</el-button>
        </div>

        <div class="session-list" v-loading="sessionsLoading">
          <div v-for="s in sessions" :key="s.sessionId" class="session-item"
            :class="{ active: s.sessionId === currentSessionId }" @click="selectSession(s)">
            <div class="session-info">
              <div class="session-title">{{ s.title }}</div>
              <div class="session-meta">{{ s.messageCount }} 条消息</div>
            </div>
            <el-button type="danger" :icon="Delete" text size="small"
              @click="handleDeleteSession(s.sessionId, $event)" />
          </div>
          <el-empty v-if="sessions.length === 0 && !sessionsLoading" description="暂无对话" :image-size="60" />
        </div>
      </div>

      <!-- 右侧：聊天区域 -->
      <div class="ai-chat">
        <!-- 消息滚动容器 -->
        <div ref="messagesContainer" class="messages-container">
          <!-- 欢迎界面（没有消息时显示） -->
          <div v-if="messages.length === 0" class="chat-welcome">
            <el-icon :size="48" color="#93c5fd">
              <MagicStick />
            </el-icon>
            <h3>AI 财务助手</h3>
            <p>您可以询问财务分析、理财建议、消费优化等问题</p>
          </div>

          <!-- 循环渲染聊天消息 -->
          <div v-for="m in messages" :key="m.id" class="message-row" :class="m.role">
            <!-- user → 用户消息（右侧，蓝色气泡）| assistant → AI 消息（左侧，灰色气泡） -->
            <div class="message-avatar">
              <el-avatar v-if="m.role === 'user'" :size="32" icon="User" />
              <el-avatar v-else :size="32" :icon="Service" style="background: #2563eb" />
            </div>
            <div class="message-bubble" :class="m.role">
              <div class="message-text" v-text="m.content" />
              <div class="message-time">{{ formatDateTimeDeleteT(m.createTime) }}</div>
            </div>
          </div>

           <!--  AI 思考中（加载状态）-->
          <div v-if="loading" class="message-row assistant thinking">
            <div class="message-avatar">
              <el-avatar :size="32" :icon="Service" style="background: #2563eb" />
            </div>
            <div class="message-bubble assistant">
              <div class="message-text">
                <span class="dot-flash">思考中</span>
              </div>
            </div>
          </div>

          <!-- 底部输入框 -->
          <div class="chat-input">
            <el-input v-model="inputMessage" type="textarea" :rows="2" placeholder="输入您的问题..." :disabled="loading"
              resize="none" @keyup.enter.exact.prevent="sendMessage" />
            <el-button type="primary" :loading="loading" :disabled="!inputMessage.trim() || loading" @click="sendMessage"
              style="margin-left: 8px">
              {{ loading ? '思考中...' : '发送' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ai-page {
  height: calc(100vh - 96px);
}

.ai-layout {
  display: flex;
  height: 100%;
  gap: 16px;
}

.ai-sidebar {
  width: 240px;
  background: #fff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

.sidebar-header {
  padding: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.15s;
}

.session-item:hover {
  background: #f1f5f9;
}

.session-item.active {
  background: #eff6ff;
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: 13px;
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-meta {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

.ai-chat {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  min-width: 0;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.chat-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #64748b;
}

.chat-welcome h3 {
  margin: 16px 0 4px;
  color: #334155;
}

.chat-welcome p {
  margin: 0 0 20px;
  font-size: 14px;
}

.quick-prompts {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
}

.message-bubble.user {
  background: #2563eb;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-bubble.assistant {
  background: #f1f5f9;
  color: #334155;
  border-bottom-left-radius: 4px;
}

.message-text {
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-time {
  font-size: 11px;
  margin-top: 4px;
  opacity: 0.6;
}

.cursor-blink {
  animation: blink 1s infinite;
}

@keyframes blink {

  0%,
  100% {
    opacity: 1;
  }

  50% {
    opacity: 0;
  }
}
/* ai思考中动画 */
.dot-flash {
  animation: flash 1.4s infinite linear;
}
@keyframes flash {
  0% { opacity: 0.4; }
  50% { opacity: 1; }
  100% { opacity: 0.4; }
}
.chat-input {
  padding: 16px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  align-items: flex-end;
}
</style>

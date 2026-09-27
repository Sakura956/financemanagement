<template>
  <view class="chat-page">
    <!-- 消息列表 -->
    <scroll-view
      class="chat-scroll"
      scroll-y
      :scroll-into-view="scrollToId"
      :style="{ height: 'calc(100vh - 120rpx)' }"
    >
      <view v-if="messages.length === 0" class="welcome-area">
        <text class="welcome-icon">🤖</text>
        <text class="welcome-text">财务 AI 助手</text>
        <text class="welcome-hint">可以问我关于理财、预算、支出分析等问题</text>
        <view class="quick-questions">
          <text class="quick-item" @click="sendQuick('我本月的收支情况如何？')">本月收支情况</text>
          <text class="quick-item" @click="sendQuick('帮我分析一下支出结构')">支出结构分析</text>
          <text class="quick-item" @click="sendQuick('有什么省钱建议？')">省钱建议</text>
        </view>
      </view>

      <view v-for="(msg, index) in messages" :key="index">
        <view class="msg-item" :class="msg.role === 'user' ? 'msg-user' : 'msg-ai'">
          <view class="msg-avatar" :class="msg.role === 'user' ? 'avatar-user' : 'avatar-ai'">
            <text style="font-size: 32rpx;">{{ msg.role === 'user' ? '👤' : '🤖' }}</text>
          </view>
          <view class="msg-bubble" :class="msg.role === 'user' ? 'bubble-user' : 'bubble-ai'">
            <text class="msg-content">{{ msg.content }}</text>
          </view>
        </view>
      </view>

      <view v-if="aiTyping" class="msg-item msg-ai">
        <view class="msg-avatar avatar-ai">
          <text style="font-size: 32rpx;">🤖</text>
        </view>
        <view class="msg-bubble bubble-ai">
          <view class="typing-dots">
            <view class="dot"></view>
            <view class="dot"></view>
            <view class="dot"></view>
          </view>
        </view>
      </view>

      <view id="msg-bottom" style="height: 20rpx;"></view>
    </scroll-view>

    <!-- 输入区域 -->
    <view class="input-bar">
      <input
        class="chat-input"
        v-model="inputText"
        placeholder="输入您的问题..."
        placeholder-style="color: #C0C0C0"
        :disabled="aiTyping"
        confirm-type="send"
        @confirm="sendMessage"
      />
      <button
        class="send-icon-btn"
        :disabled="!inputText.trim() || aiTyping"
        @click="sendMessage"
      >
        <text style="font-size: 36rpx;" :style="{ opacity: inputText.trim() && !aiTyping ? 1 : 0.3 }">➤</text>
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { onShow,onLoad } from '@dcloudio/uni-app'
import { sendAIChat, getAISessionMessages } from '@/api/index.js'

const sessionId = ref(null)
const messages = ref([])
const inputText = ref('')
const aiTyping = ref(false)
const scrollToId = ref('')

function scrollToBottom() {
  nextTick(() => {
    scrollToId.value = 'msg-bottom'
  })
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || aiTyping.value) return

  // 添加用户消息
  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  scrollToBottom()

  // 显示 AI 输入中
  aiTyping.value = true

  try {
    const data = { message: text, includeHistory: true }
    if (sessionId.value) {
      data.sessionId = sessionId.value
    }

    const res = await sendAIChat(data)
    if (res.data) {
      // 保存 sessionId
      if (res.data.sessionId) {
        sessionId.value = res.data.sessionId
      }
      // 添加 AI 回复
      messages.value.push({ role: 'assistant', content: res.data.message })
      scrollToBottom()
    }
  } catch (err) {
    console.error('AI 请求失败:', err)
    messages.value.push({ role: 'assistant', content: '抱歉，AI 服务暂时不可用，请稍后重试。' })
  } finally {
    aiTyping.value = false
    scrollToBottom()
  }
}

function sendQuick(text) {
  inputText.value = text
  sendMessage()
}

// 加载历史消息
async function loadMessages(sid) {
  try {
    const res = await getAISessionMessages(sid, { page: 1, size: 50 })
    if (res.data && res.data.records) {
      messages.value = res.data.records.map((m) => ({
        role: m.role,
        content: m.content
      }))
      scrollToBottom()
    }
  } catch (err) { console.error(err) }
}

onLoad((options) => {
  if (options && options.sessionId) {
    sessionId.value = options.sessionId
    loadMessages(options.sessionId)
  }
})
</script>

<style lang="scss" scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #F5F7FA;
}

.chat-scroll {
  flex: 1;
  padding: 16rpx 0;
}

.welcome-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 120rpx;

  .welcome-icon { font-size: 100rpx; margin-bottom: 20rpx; }
  .welcome-text { font-size: 36rpx; font-weight: 600; color: #1A1A2E; margin-bottom: 12rpx; }
  .welcome-hint { font-size: 26rpx; color: #999; margin-bottom: 40rpx; }

  .quick-questions {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 16rpx;
    padding: 0 32rpx;

    .quick-item {
      padding: 14rpx 28rpx;
      background: #FFFFFF;
      border-radius: 24rpx;
      font-size: 26rpx;
      color: #3B6FE8;
      box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04);
    }
  }
}

.msg-item {
  display: flex;
  padding: 16rpx 24rpx;
  gap: 16rpx;

  &.msg-user {
    flex-direction: row-reverse;
  }

  .msg-avatar {
    width: 64rpx;
    height: 64rpx;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    &.avatar-user { background: #EEF2FF; }
    &.avatar-ai { background: #ECFDF5; }
  }

  .msg-bubble {
    max-width: 75%;
    padding: 18rpx 24rpx;
    border-radius: 20rpx;
    font-size: 28rpx;
    line-height: 1.6;
    word-break: break-all;

    &.bubble-user {
      background: #3B6FE8;
      color: #FFFFFF;
      border-top-right-radius: 6rpx;
    }

    &.bubble-ai {
      background: #FFFFFF;
      color: #333333;
      border-top-left-radius: 6rpx;
      box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04);
      white-space: pre-wrap;
    }
  }
}

.typing-dots {
  display: flex;
  gap: 8rpx;
  padding: 10rpx 0;

  .dot {
    width: 14rpx;
    height: 14rpx;
    background: #C0C0C0;
    border-radius: 50%;
    animation: blink 1.4s infinite ease-in-out both;

    &:nth-child(1) { animation-delay: 0s; }
    &:nth-child(2) { animation-delay: 0.16s; }
    &:nth-child(3) { animation-delay: 0.32s; }
  }
}

@keyframes blink {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

.input-bar {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 20rpx;
  background: #FFFFFF;
  border-top: 1rpx solid #F0F2F5;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));

  .chat-input {
    flex: 1;
    height: 72rpx;
    background: #F5F7FA;
    border-radius: 36rpx;
    padding: 0 24rpx;
    font-size: 28rpx;
    color: #333;
  }

  .send-icon-btn {
    width: 72rpx;
    height: 72rpx;
    background: none;
    border: none;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0;
    color: #3B6FE8;
  }
}
</style>

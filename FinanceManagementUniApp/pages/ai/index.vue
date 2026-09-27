<template>
  <view class="ai-page">
    <!-- 新建对话入口 -->
    <view class="new-chat-bar" @click="goChat()">
      <view class="new-chat-input">
        <text style="font-size: 36rpx;">🤖</text>
        <text class="input-hint">向 AI 助手提问财务问题...</text>
      </view>
      <text class="send-btn">发送</text>
    </view>

    <!-- 会话列表 -->
    <scroll-view scroll-y class="session-scroll" @scrolltolower="loadMore">
      <view v-if="list.length > 0">
        <view v-for="item in list" :key="item.sessionId" class="session-card" @click="goChat(item.sessionId)">
          <view class="session-header">
            <text class="session-title">{{ item.title || '新对话' }}</text>
            <text class="session-time">{{ formatDate(item.lastActiveTime, 'MM-dd HH:mm') }}</text>
          </view>
          <view class="session-footer">
            <text class="session-preview">{{ item.lastMessage || '' }}</text>
            <view class="session-meta">
              <text class="msg-count">{{ item.messageCount }} 条消息</text>
              <text class="delete-btn" @click.stop="handleDelete(item)">删除</text>
            </view>
          </view>
        </view>
        <view v-if="loading" class="loading-tip">加载中...</view>
        <view v-if="noMore && list.length > 0" class="loading-tip">— 没有更多了 —</view>
      </view>
      <view v-if="!loading && list.length === 0" class="empty-block">
        <text class="empty-icon">🤖</text>
        <text class="empty-text">还没有对话记录</text>
        <text class="empty-hint">点击上方输入框开始 AI 对话</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow,onLoad } from '@dcloudio/uni-app'
import { getAISessions, deleteAISession } from '@/api/index.js'
import { formatDate } from '@/utils/index.js'

const list = ref([])
const page = ref(1)
const loading = ref(false)
const noMore = ref(false)

function goChat(sessionId) {
  if (sessionId) {
    uni.navigateTo({ url: `/pages/ai/chat?sessionId=${sessionId}` })
  } else {
    uni.navigateTo({ url: '/pages/ai/chat' })
  }
}

async function handleDelete(item) {
  uni.showModal({
    title: '确认删除',
    content: '确定要删除该对话吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteAISession(item.sessionId)
          list.value = list.value.filter((s) => s.sessionId !== item.sessionId)
          uni.showToast({ title: '删除成功', icon: 'success' })
        } catch (err) { console.error(err) }
      }
    }
  })
}

async function loadData() {
  if (loading.value) return
  loading.value = true
  try {
    const res = await getAISessions({ page: page.value, size: 15 })
    if (res.data) {
      if (page.value === 1) {
        list.value = res.data.records || []
      } else {
        list.value = list.value.concat(res.data.records || [])
      }
      noMore.value = (res.data.records || []).length < 15
    }
  } catch (err) { console.error(err) }
  finally { loading.value = false }
}

function loadMore() {
  if (loading.value || noMore.value) return
  page.value++
  loadData()
}

onShow(() => {
  page.value = 1
  list.value = []
  loadData()
})
</script>

<style lang="scss" scoped>
.ai-page {
  min-height: 100vh;
  background: #F5F7FA;
}

.new-chat-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx 24rpx;
  background: #FFFFFF;
  border-bottom: 1rpx solid #F0F2F5;

  .new-chat-input {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 12rpx;
    background: #F5F7FA;
    border-radius: 40rpx;
    padding: 16rpx 24rpx;

    .input-hint { font-size: 28rpx; color: #C0C0C0; }
  }

  .send-btn {
    font-size: 28rpx;
    color: #FFFFFF;
    background: #3B6FE8;
    padding: 14rpx 28rpx;
    border-radius: 32rpx;
    font-weight: 500;
  }
}

.session-scroll { height: calc(100vh - 120rpx); }

.session-card {
  margin: 12rpx 24rpx;
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);

  .session-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12rpx;

    .session-title {
      font-size: 28rpx;
      font-weight: 500;
      color: #1A1A2E;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      flex: 1;
    }

    .session-time { font-size: 22rpx; color: #C0C0C0; margin-left: 16rpx; }
  }

  .session-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .session-preview {
      font-size: 26rpx;
      color: #999;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      flex: 1;
    }

    .session-meta {
      display: flex;
      align-items: center;
      gap: 16rpx;
      margin-left: 16rpx;

      .msg-count { font-size: 22rpx; color: #C0C0C0; }
      .delete-btn { font-size: 22rpx; color: #EF4444; }
    }
  }
}

.loading-tip { text-align: center; padding: 24rpx; font-size: 24rpx; color: #C0C0C0; }

.empty-block {
  display: flex; flex-direction: column; align-items: center; padding-top: 200rpx;
  .empty-icon { font-size: 96rpx; margin-bottom: 20rpx; }
  .empty-text { font-size: 28rpx; color: #999; }
  .empty-hint { font-size: 24rpx; color: #C0C0C0; margin-top: 8rpx; }
}
</style>

import request from '@/api/request'
import type { AISession, AIMessage, ChatRequest, ChatResult, AgentSseEvent, PageResult } from '@/types'

export const aiApi = {
  /**  普通聊天（非流式，旧版 RestTemplate 实现，保留给兼容场景） */
  chat(data: ChatRequest): Promise<ChatResult> {
    return request.post('/user/ai/chat', data)
  },

  //获取会话列表
  getSessions(params?: { page?: number; size?: number }): Promise<PageResult<AISession>> {
    return request.get('/user/ai/sessions', { params })
  },

  //获取某个会话的聊天记录
  getSessionMessages(sessionId: string, params?: { page?: number; size?: number }): Promise<PageResult<AIMessage>> {
    return request.get(`/user/ai/sessions/${sessionId}/messages`, { params })
  },

  //删除会话
  deleteSession(sessionId: string): Promise<null> {
    return request.delete(`/user/ai/sessions/${sessionId}`)
  },

  /**
   * Agent 流式对话（SSE）：后端事件序列 session → tool* / content*（交错）→ sources → done
   * fetch + ReadableStream 逐事件解析（axios 不支持流式读取，故单独实现）
   */
  async agentChatStream(
    data: { sessionId?: string; message: string },
    onEvent: (event: AgentSseEvent) => void,
    signal?: AbortSignal,
  ): Promise<void> {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'
    const token = localStorage.getItem('token')

    const response = await fetch(`${baseUrl}/user/agent/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(data),
      signal,
    })

    if (!response.ok || !response.body) {
      if (response.status === 401) {
        throw new Error('登录已过期，请重新登录')
      }
      throw new Error(`AI 服务暂时不可用(${response.status})`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    // SSE 以空行分隔事件，每行 data: 开头为 JSON 载荷
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      const chunks = buffer.split('\n\n')
      buffer = chunks.pop() ?? ''
      for (const chunk of chunks) {
        for (const line of chunk.split('\n')) {
          if (!line.startsWith('data:')) continue
          const payload = line.slice(5).trim()
          if (!payload) continue
          try {
            onEvent(JSON.parse(payload) as AgentSseEvent)
          } catch {
            // 忽略无法解析的行（如心跳注释）
          }
        }
      }
    }
  },

  /** Agent 删除会话（同时清理 MySQL 历史 + Redis 对话记忆） */
  agentDeleteSession(sessionId: string): Promise<null> {
    return request.delete(`/user/agent/session/${sessionId}`)
  },
}

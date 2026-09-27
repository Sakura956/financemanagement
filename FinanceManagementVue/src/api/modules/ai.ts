import request from '@/api/request'
import type { AISession, AIMessage, ChatRequest, ChatResult, FinanceDiagnosisRequest, FinanceDiagnosisResult, InvestmentAdviceResult, PageResult } from '@/types'

export const aiApi = {
  /**  普通聊天（非流式） */
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
}

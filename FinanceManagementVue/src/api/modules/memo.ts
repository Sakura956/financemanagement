import request from '@/api/request'
import type { Memo, MemoForm, PageResult } from '@/types'

export const memoApi = {
  getMemos(params?: {
    isCompleted?: 0 | 1
    page?: number
    size?: number
    keyword?: string
  }): Promise<PageResult<Memo>> {
    return request.get('/user/memos', { params })
  },

  getMemoDetail(id: number): Promise<Memo> {
    return request.get(`/user/memos/${id}`)
  },

  createMemo(data: MemoForm): Promise<Memo> {
    return request.post('/user/memos', data)
  },

  updateMemo(id: number, data: Partial<MemoForm>): Promise<Memo> {
    return request.put(`/user/memos/${id}`, data)
  },

  toggleMemo(id: number): Promise<{ id: number; isCompleted: 0 | 1; updateTime: string }> {
    return request.put(`/user/memos/${id}/toggle`)
  },

  deleteMemo(id: number): Promise<null> {
    return request.delete(`/user/memos/${id}`)
  },
}

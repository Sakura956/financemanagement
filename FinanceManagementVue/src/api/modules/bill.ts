import request from '@/api/request'
import type { Bill, BillQuery, BillForm, BillUpdateForm, BillListData, Category } from '@/types'

export const billApi = {
  /**
   * 新增一条收入或支出记录
   * @param data 
   * @returns 
   */
  createBill(data: BillForm): Promise<Bill> {
    return request.post('/user/bills', data)
  },

  /**
   * 查询当前用户的账单记录，支持多条件筛选
   * @param params 
   * @returns 
   */
  getBills(params: BillQuery): Promise<BillListData> {
    return request.get('/user/bills', { params })
  },

  getBillDetail(id: number): Promise<Bill> {
    return request.get(`/user/bills/${id}`)
  },

  /**
   * 修改账单信息，支持部分更新
   * @param id 
   * @param data 
   * @returns 
   */
  updateBill(id: number, data: BillUpdateForm): Promise<Bill> {
    return request.put(`/user/bills/${id}`, data)
  },

  deleteBill(id: number): Promise<null> {
    return request.delete(`/user/bills/${id}`)
  },

  /**
   * 批量删除账单
   * @param ids 
   * @returns 
   */
  batchDeleteBills(ids: number[]): Promise<{ deletedCount: number }> {
    return request.delete('/user/bills/batch', { data: { ids } })
  },

  /**
   * 用户端获取账单的分类
   * @param type 
   * @returns 
   */
  getCategories(type?: 0 | 1): Promise<Category[]> {
    return request.get('/user/categories', { params: type !== undefined ? { type } : {} })
  },
}

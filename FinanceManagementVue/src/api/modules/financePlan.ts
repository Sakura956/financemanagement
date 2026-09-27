import request from '@/api/request'
import type { FinancePlan, FinancePlanForm, FinancePlanListData, ValuationForm, PlanStatusForm } from '@/types'

export const financePlanApi = {
  /**
   * 获取理财计划列表
   * @param params 
   * @returns 
   */
  getPlans(params?: { status?: 0 | 1; page?: number; size?: number }): Promise<FinancePlanListData> {
    return request.get('/user/finance-plans', { params })
  },

  getPlanDetail(id: number): Promise<FinancePlan> {
    return request.get(`/user/finance-plans/${id}`)
  },

  /**
   * 新增理财计划
   * @param data 
   * @returns 
   */
  createPlan(data: FinancePlanForm): Promise<FinancePlan> {
    return request.post('/user/finance-plans', data)
  },

  /**
   * 更新理财计划
   * @param id 
   * @param data 
   * @returns 
   */
  updatePlan(id: number, data: Partial<FinancePlanForm>): Promise<FinancePlan> {
    return request.put(`/user/finance-plans/${id}`, data)
  },

  updateValuation(id: number, data: ValuationForm): Promise<FinancePlan> {
    return request.put(`/user/finance-plans/${id}/valuation`, data)
  },

  updatePlanStatus(id: number, data: PlanStatusForm): Promise<null> {
    return request.put(`/user/finance-plans/${id}/status`, data)
  },

  deletePlan(id: number): Promise<null> {
    return request.delete(`/user/finance-plans/${id}`)
  },
}

import request from '@/api/request'
import type { StatisticsOverview, CategoryPieData, TrendItem, YearlySummary } from '@/types'

export const statisticsApi = {
  /**
   * 获取指定月份的收支总览数据,不传默认当前月
   * @param month 
   * @returns 
   */
  getOverview(month?: string): Promise<StatisticsOverview> {
    return request.get('/user/statistics/overview', { params: { month } })
  },

  /**
   * 获取指定月份各支出分类的占比数据
   * @param params 
   * @returns 
   */
  getCategoryPie(params?: { month?: string; type?: 0 | 1 }): Promise<CategoryPieData> {
    return request.get('/user/statistics/category-pie', { params })
  },

  /**
   * 获取近 N 个月的收支趋势数据
   * @param months 
   * @returns 
   */
  getTrend(months?: number): Promise<TrendItem[]> {
    return request.get('/user/statistics/trend', { params: { months } })
  },

  /**
   * 获取指定年份的年度汇总数据
   * @param year 
   * @returns 
   */
  getYearly(year?: number): Promise<YearlySummary> {
    return request.get('/user/statistics/yearly', { params: { year } })
  },
}

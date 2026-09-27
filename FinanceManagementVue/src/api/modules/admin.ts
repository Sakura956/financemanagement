import request from '@/api/request'
import type { PageResult, UserInfo, UserQuery, Category, CategoryForm, AdminDashboard } from '@/types'

export const adminApi = {
  //分页查询所有注册用户
  getUsers(params: UserQuery): Promise<PageResult<UserInfo>> {
    return request.get('/admin/users', { params })
  },

  //查看指定用户的详细信息及统计数据
  getUserDetail(userId: number): Promise<UserInfo> {
    return request.get(`/admin/users/${userId}`)
  },

  //封禁/解封用户
  updateUserStatus(userId: number, status: 0 | 1): Promise<null> {
    return request.put(`/admin/users/${userId}/status`, { status })
  },

  //获取账单分类列表
  getCategories(params?: { type?: 0 | 1; includeDisabled?: boolean }, silent = false): Promise<Category[]> {
    return request.get('/admin/categories', {
      params,
      ...(silent ? { skipErrorToast: true } : {}),
    })
  },

  //新增自定义账单分类
  createCategory(data: CategoryForm): Promise<Category> {
    return request.post('/admin/categories', data)
  },

  //修改账单分类
  updateCategory(id: number, data: Partial<CategoryForm>): Promise<Category> {
    return request.put(`/admin/categories/${id}`, data)
  },

  //删除/禁用账单分类
  deleteCategory(id: number): Promise<null> {
    return request.delete(`/admin/categories/${id}`)
  },

  //启用/禁用分类
  updateCategoryStatus(id: number, status: 0 | 1): Promise<null> {
    return request.put(`/admin/categories/${id}/status`, { status })
  },

  //获取管理端首页统计数据
  getDashboard(): Promise<AdminDashboard> {
    return request.get('/admin/dashboard')
  },
}

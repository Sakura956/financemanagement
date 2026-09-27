// API 接口定义 - 仅包含用户端接口

import request from '@/utils/request.js'

// ==================== 认证模块 ====================

/** 用户注册 */
export function register(data) {
	return request.post('/auth/register', data)
}

/** 用户登录 */
export function login(data) {
	return request.post('/auth/login', data)
}

/** 修改密码 */
export function changePassword(data) {
	return request.put('/auth/change-password', data)
}

/** 获取当前用户信息 */
export function getCurrentUser() {
	return request.get('/auth/me')
}

/** 更新个人信息 */
export function updateProfile(data) {
	return request.put('/auth/profile', data)
}

// ==================== 账单管理 ====================

/** 用户端账单分类列表 */
export function getCategories(type) {
	const params = {}
	if (type !== undefined && type !== null && type !== '') {
		params.type = type
	}
	return request.get('/user/categories', params)
}

/** 创建账单 */
export function createBill(data) {
	return request.post('/user/bills', data)
}

/** 账单分页查询 */
export function getBills(params) {
	return request.get('/user/bills', params)
}

/** 账单详情 */
export function getBillDetail(id) {
	return request.get(`/user/bills/${id}`)
}

/** 修改账单 */
export function updateBill(id, data) {
	return request.put(`/user/bills/${id}`, data)
}

/** 删除账单 */
export function deleteBill(id) {
	return request.del(`/user/bills/${id}`)
}

/** 批量删除账单 */
export function batchDeleteBills(ids) {
	return request.del('/user/bills/batch', {
		ids
	})
}

// ==================== 理财计划管理 ====================

/** 理财计划列表 */
export function getFinancePlans(params) {
	return request.get('/user/finance-plans', params)
}

/** 理财计划详情 */
export function getFinancePlanDetail(id) {
	return request.get(`/user/finance-plans/${id}`)
}

/** 创建理财计划 */
export function createFinancePlan(data) {
	return request.post('/user/finance-plans', data)
}

/** 修改理财计划基本信息 */
export function updateFinancePlan(id, data) {
	return request.put(`/user/finance-plans/${id}`, data)
}

/** 更新理财计划市值 */
export function updateFinancePlanValuation(id, data) {
	return request.put(`/user/finance-plans/${id}/valuation`, data)
}

/** 理财计划状态变更（赎回） */
export function updateFinancePlanStatus(id, data) {
	return request.put(`/user/finance-plans/${id}/status`, data)
}

/** 删除理财计划 */
export function deleteFinancePlan(id) {
	return request.del(`/user/finance-plans/${id}`)
}

// ==================== 统计分析 ====================

/** 月度收支总览 */
export function getStatisticsOverview(month) {
	return request.get('/user/statistics/overview', {
		month
	})
}

/** 支出分类统计（饼图数据） */
export function getCategoryPie(params) {
	return request.get('/user/statistics/category-pie', params)
}

/** 月度趋势（折线图数据） */
export function getTrend(months = 12) {
	return request.get('/user/statistics/trend', {
		months
	})
}

/** 年度总览 */
export function getYearlyStatistics(year) {
	return request.get('/user/statistics/yearly', {
		year
	})
}

// ==================== 备忘录管理 ====================

/** 备忘录列表 */
export function getMemos(params) {
	return request.get('/user/memos', params)
}

/** 备忘录详情 */
export function getMemoDetail(id) {
	return request.get(`/user/memos/${id}`)
}

/** 新增备忘录 */
export function createMemo(data) {
	return request.post('/user/memos', data)
}

/** 修改备忘录 */
export function updateMemo(id, data) {
	return request.put(`/user/memos/${id}`, data)
}

/** 切换备忘录完成状态 */
export function toggleMemo(id) {
	return request.put(`/user/memos/${id}/toggle`)
}

/** 删除备忘录 */
export function deleteMemo(id) {
	return request.del(`/user/memos/${id}`)
}

// ==================== AI 智能助手 ====================

/** 发送对话消息 */
export function sendAIChat(data) {
	return request.post('/user/ai/chat', data)
}

/** 获取会话列表 */
export function getAISessions(params) {
	return request.get('/user/ai/sessions', params)
}

/** 获取会话消息历史 */
export function getAISessionMessages(sessionId, params) {
	return request.get(`/user/ai/sessions/${sessionId}/messages`, params)
}

/** 删除会话 */
export function deleteAISession(sessionId) {
	return request.del(`/user/ai/sessions/${sessionId}`)
}
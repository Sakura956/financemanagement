// 网络请求封装 - 统一处理 Token 注入和错误拦截

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

// 请求超时时间（毫秒）
const TIMEOUT = 30000

/**
 * 获取存储的 Token
 */
function getToken() {
	try {
		return uni.getStorageSync('token') || ''
	} catch (e) {
		return ''
	}
}

/**
 * 清除登录信息
 */
function clearAuth() {
	try {
		uni.removeStorageSync('token')
		uni.removeStorageSync('userInfo')
	} catch (e) {
		// ignore
	}
}

/**
 * 显示错误提示
 */
function showError(message) {
	uni.showToast({
		title: message || '请求失败',
		icon: 'none',
		duration: 2000
	})
}

/**
 * 统一请求方法
 * @param {Object} options - 请求配置
 * @returns {Promise}
 */
function request(options) {
	return new Promise((resolve, reject) => {
		// 获取token
		const token = getToken()

		// 构建请求头
		const header = {
			'Content-Type': 'application/json',
			...options.header
		}

		// 注入 Token
		if (token) {
			header['Authorization'] = `Bearer ${token}`
		}

		//发起请求
		uni.request({
			url: BASE_URL + options.url,
			method: options.method || 'GET',
			data: options.data || {},
			header,
			timeout: options.timeout || TIMEOUT,
			success(res) {
				const {
					statusCode,
					data
				} = res

				//统一处理返回结果
				// HTTP 200 且业务码 200 表示成功
				if (statusCode === 200 && data.code === 200) {
					resolve(data)
					return
				}

				// 业务错误码处理
				if (data.code === 401) {
					clearAuth()
					showError('登录已过期，请重新登录')
					// 跳转到登录页
					setTimeout(() => {
						uni.reLaunch({
							url: '/pages/auth/login'
						})
					}, 1500)
					reject(data)
					return
				}

				if (data.code === 403) {
					showError(data.message || '无权限访问')
					reject(data)
					return
				}

				// 其他业务错误
				showError(data.message || '请求失败')
				reject(data)
			},
			fail(err) {
				// 网络错误
				if (err.errMsg && err.errMsg.includes('timeout')) {
					showError('请求超时，请稍后重试')
				} else {
					showError('网络异常，请检查网络连接')
				}
				reject(err)
			}
		})
	})
}

/**
 * GET 请求
 */
function get(url, params = {}) {
	// 过滤空值参数
	const filteredParams = {}
	Object.keys(params).forEach((key) => {
		if (params[key] !== '' && params[key] !== null && params[key] !== undefined) {
			filteredParams[key] = params[key]
		}
	})

	// 拼接 query string
	const queryString = Object.keys(filteredParams)
		.map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(filteredParams[key])}`)
		.join('&')

	const fullUrl = queryString ? `${url}?${queryString}` : url

	return request({
		url: fullUrl,
		method: 'GET'
	})
}

/**
 * POST 请求
 */
function post(url, data = {}) {
	return request({
		url,
		method: 'POST',
		data
	})
}

/**
 * PUT 请求
 */
function put(url, data = {}) {
	return request({
		url,
		method: 'PUT',
		data
	})
}

/**
 * DELETE 请求
 */
function del(url, data = {}) {
	return request({
		url,
		method: 'DELETE',
		data
	})
}

export default {
	get,
	post,
	put,
	del,
	BASE_URL
}
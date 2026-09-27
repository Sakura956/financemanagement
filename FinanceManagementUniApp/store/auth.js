// Pinia 用户认证状态管理

import {
	defineStore
} from 'pinia'
//loginApi：登录接口，getCurrentUserApi：获取当前用户信息接口
import {
	login as loginApi,
	getCurrentUser as getCurrentUserApi
} from '@/api/index.js'

export const useAuthStore = defineStore('auth', {
	//选项式写法，uni-app 官方推荐的最标准、最正确的 Pinia 写法
	
	//state：存数据
	state: () => ({
		// Token
		token: uni.getStorageSync('token') || '',
		// 用户信息
		userInfo: uni.getStorageSync('userInfo') ? JSON.parse(uni.getStorageSync('userInfo')) : null,
		// 是否已登录
		isLogin: !!uni.getStorageSync('token')
	}),

	// getters：计算属性（快速拿数据）
	getters: {
		/** 用户ID */
		userId: (state) => state.userInfo?.id || null,
		/** 用户昵称 */
		nickname: (state) => state.userInfo?.nickname || '用户',
		/** 用户头像 */
		avatarUrl: (state) => state.userInfo?.avatarUrl || '',
		/** 用户手机号 */
		phone: (state) => state.userInfo?.phone || ''
	},

	actions: {
		/**
		 * 登录成功后保存信息
		 */
		setLoginData(token, userInfo) {
			this.token = token
			this.userInfo = userInfo
			this.isLogin = true

			//保存到本地缓存
			uni.setStorageSync('token', token)
			uni.setStorageSync('userInfo', JSON.stringify(userInfo))
		},

		/**
		 * 用户登录
		 */
		async login(phone, password) {
			const res = await loginApi({
				phone,
				password
			})
			const {
				token,
				userInfo
			} = res.data
			this.setLoginData(token, userInfo)
			return res
		},

		/**
		 * 刷新用户信息
		 */
		async fetchUserInfo() {
			const res = await getCurrentUserApi()
			this.userInfo = res.data
			this.isLogin = true
			uni.setStorageSync('userInfo', JSON.stringify(res.data))
			return res.data
		},

		/**
		 * 更新本地用户信息
		 */
		updateUserInfo(data) {
			this.userInfo = {
				...this.userInfo,
				...data
			}
			uni.setStorageSync('userInfo', JSON.stringify(this.userInfo))
		},

		/**
		 * 退出登录
		 */
		logout() {
			this.token = ''
			this.userInfo = null
			this.isLogin = false

			uni.removeStorageSync('token')
			uni.removeStorageSync('userInfo')

			uni.reLaunch({
				url: '/pages/auth/login'
			})
		}
	}
})
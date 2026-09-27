<template>
	<view class="login-page">
		<!-- 顶部装饰 -->
		<view class="login-header">
			<view class="header-circle circle-1"></view>
			<view class="header-circle circle-2"></view>
			<view class="header-content">
				<text class="app-name">财务管家</text>
				<text class="app-slogan">轻松管理每一笔收支</text>
			</view>
		</view>

		<!-- 登录表单 -->
		<view class="login-form">
			<view class="form-title">欢迎回来</view>
			<view class="form-subtitle">请使用手机号登录您的账户</view>

			<view class="input-group">
				<view class="input-icon">
					<text class="iconfont icon-phone">📱</text>
				</view>
				<input class="form-input" v-model="form.phone" type="number" placeholder="请输入手机号" maxlength="11"
					placeholder-style="color: #C0C0C0" />
			</view>

			<view class="input-group">
				<view class="input-icon">
					<text class="iconfont icon-lock">🔒</text>
				</view>
				<input class="form-input" v-model="form.password" type="password" placeholder="请输入密码" maxlength="20"
					placeholder-style="color: #C0C0C0" />
			</view>

			<button class="btn-primary login-btn" :loading="loading" @click="handleLogin">
				登 录
			</button>

			<view class="form-footer">
				<text>还没有账户？</text>
				<text class="link" @click="goRegister">立即注册</text>
			</view>
		</view>

		<!-- 底部版权 -->
		<view class="login-footer">
			<text class="footer-text">财务管家 v1.0.0</text>
		</view>
	</view>
</template>

<script setup>
	import {
		ref,
		reactive
	} from 'vue'
	import {
		useAuthStore
	} from '@/store/auth.js'
	import {
		validatePhone
	} from '@/utils/index.js'

	const authStore = useAuthStore()
	const loading = ref(false)

	const form = reactive({
		phone: '',
		password: ''
	})

	// 跳转到注册页
	function goRegister() {
		uni.navigateTo({
			url: '/pages/auth/register'
		})
	}

	// 处理登录
	async function handleLogin() {
		// 校验手机号
		if (!validatePhone(form.phone)) {
			uni.showToast({
				title: '请输入正确的手机号',
				icon: 'none'
			})
			return
		}

		// 校验密码
		if (!form.password || form.password.length < 6) {
			uni.showToast({
				title: '请输入密码（6-20位）',
				icon: 'none'
			})
			return
		}

		loading.value = true
		try {
			await authStore.login(form.phone, form.password)
			uni.showToast({
				title: '登录成功',
				icon: 'success'
			})
			setTimeout(() => {
				uni.switchTab({
					url: '/pages/index/index'
				})
			}, 500)
		} catch (err) {
			console.error('登录失败:', err)
		} finally {
			loading.value = false
		}
	}
</script>

<style lang="scss" scoped>
	.login-page {
		min-height: 100vh;
		background: linear-gradient(180deg, #EEF2FF 0%, #F5F7FA 40%);
		display: flex;
		flex-direction: column;
	}

	.login-header {
		position: relative;
		height: 360rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		overflow: hidden;

		.header-circle {
			position: absolute;
			border-radius: 50%;
			background: rgba(59, 111, 232, 0.08);

			&.circle-1 {
				width: 500rpx;
				height: 500rpx;
				top: -180rpx;
				right: -100rpx;
			}

			&.circle-2 {
				width: 300rpx;
				height: 300rpx;
				bottom: -60rpx;
				left: -80rpx;
			}
		}

		.header-content {
			position: relative;
			z-index: 1;
			text-align: center;

			.app-name {
				display: block;
				font-size: 52rpx;
				font-weight: 700;
				color: #1A1A2E;
				letter-spacing: 4rpx;
			}

			.app-slogan {
				display: block;
				font-size: 26rpx;
				color: #666666;
				margin-top: 12rpx;
			}
		}
	}

	.login-form {
		flex: 1;
		padding: 60rpx 48rpx 0;

		.form-title {
			font-size: 40rpx;
			font-weight: 700;
			color: #1A1A2E;
			margin-bottom: 8rpx;
		}

		.form-subtitle {
			font-size: 26rpx;
			color: #999999;
			margin-bottom: 48rpx;
		}
	}

	.input-group {
		display: flex;
		align-items: center;
		background: #FFFFFF;
		border-radius: 16rpx;
		padding: 0 24rpx;
		margin-bottom: 24rpx;
		height: 100rpx;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.03);

		.input-icon {
			width: 60rpx;
			font-size: 36rpx;
			text-align: center;
		}

		.form-input {
			flex: 1;
			height: 100%;
			font-size: 30rpx;
			color: #333333;
		}
	}

	.login-btn {
		margin-top: 20rpx;
		height: 96rpx;
		font-size: 34rpx;
		letter-spacing: 8rpx;
	}

	.form-footer {
		display: flex;
		justify-content: center;
		align-items: center;
		margin-top: 40rpx;
		font-size: 28rpx;
		color: #999999;

		.link {
			color: #3B6FE8;
			font-weight: 500;
			margin-left: 8rpx;
		}
	}

	.login-footer {
		padding: 40rpx 0;
		text-align: center;

		.footer-text {
			font-size: 24rpx;
			color: #C0C0C0;
		}
	}
</style>
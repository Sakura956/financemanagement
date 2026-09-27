<template>
	<view class="register-page">
		<!-- 顶部 -->
		<view class="register-header">
			<view class="header-circle circle-1"></view>
			<view class="header-circle circle-2"></view>
			<view class="back-btn" @click="goBack">
				<text style="font-size: 36rpx;">←</text>
			</view>
			<view class="header-content">
				<text class="app-name">创建账户</text>
				<text class="app-slogan">注册后即可开始管理您的财务</text>
			</view>
		</view>

		<!-- 注册表单 -->
		<view class="register-form">
			<view class="input-group">
				<view class="input-icon">📱</view>
				<input class="form-input" v-model="form.phone" type="number" placeholder="请输入手机号" maxlength="11"
					placeholder-style="color: #C0C0C0" />
			</view>

			<view class="input-group">
				<view class="input-icon">👤</view>
				<input class="form-input" v-model="form.nickname" type="text" placeholder="请输入昵称（选填）" maxlength="20"
					placeholder-style="color: #C0C0C0" />
			</view>

			<view class="input-group">
				<view class="input-icon">🔒</view>
				<input class="form-input" v-model="form.password" type="password" placeholder="请设置密码（6-20位，含字母和数字）"
					maxlength="20" placeholder-style="color: #C0C0C0" />
			</view>

			<view class="input-group">
				<view class="input-icon">🔒</view>
				<input class="form-input" v-model="form.confirmPassword" type="password" placeholder="请确认密码"
					maxlength="20" placeholder-style="color: #C0C0C0" />
			</view>

			<!-- 密码规则提示 -->
			<view class="pwd-tips">
				<text class="tip-item" :class="{ valid: pwdHasLength }">✓ 6-20位字符</text>
				<text class="tip-item" :class="{ valid: pwdHasLetter }">✓ 包含字母</text>
				<text class="tip-item" :class="{ valid: pwdHasNumber }">✓ 包含数字</text>
			</view>

			<button class="btn-primary register-btn" :loading="loading" @click="handleRegister">
				注 册
			</button>

			<view class="form-footer">
				<text>已有账户？</text>
				<text class="link" @click="goBack">立即登录</text>
			</view>
		</view>
	</view>
</template>

<script setup>
	import {
		ref,
		reactive,
		computed
	} from 'vue'
	import {
		register as registerApi
	} from '@/api/index.js'
	import {
		validatePhone,
		validatePassword
	} from '@/utils/index.js'

	const loading = ref(false)

	const form = reactive({
		phone: '',
		nickname: '',
		password: '',
		confirmPassword: ''
	})

	// 密码规则校验
	const pwdHasLength = computed(() => form.password.length >= 6 && form.password.length <= 20)
	const pwdHasLetter = computed(() => /[a-zA-Z]/.test(form.password))
	const pwdHasNumber = computed(() => /\d/.test(form.password))

	function goBack() {
		uni.navigateBack()
	}

	async function handleRegister() {
		// 校验手机号
		if (!validatePhone(form.phone)) {
			uni.showToast({
				title: '请输入正确的手机号',
				icon: 'none'
			})
			return
		}

		// 校验密码
		if (!validatePassword(form.password)) {
			uni.showToast({
				title: '密码需6-20位，且包含字母和数字',
				icon: 'none'
			})
			return
		}

		// 确认密码
		if (form.password !== form.confirmPassword) {
			uni.showToast({
				title: '两次密码输入不一致',
				icon: 'none'
			})
			return
		}

		loading.value = true
		try {
			const data = {
				phone: form.phone,
				password: form.password
			}
			if (form.nickname.trim()) {
				data.nickname = form.nickname.trim()
			}
			await registerApi(data)
			uni.showToast({
				title: '注册成功，请登录',
				icon: 'success'
			})
			setTimeout(() => {
				uni.navigateBack()
			}, 1500)
		} catch (err) {
			console.error('注册失败:', err)
		} finally {
			loading.value = false
		}
	}
</script>

<style lang="scss" scoped>
	.register-page {
		min-height: 100vh;
		background: linear-gradient(180deg, #EEF2FF 0%, #F5F7FA 40%);
	}

	.register-header {
		position: relative;
		height: 280rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		overflow: hidden;

		.header-circle {
			position: absolute;
			border-radius: 50%;
			background: rgba(59, 111, 232, 0.08);

			&.circle-1 {
				width: 400rpx;
				height: 400rpx;
				top: -150rpx;
				right: -80rpx;
			}

			&.circle-2 {
				width: 200rpx;
				height: 200rpx;
				bottom: -40rpx;
				left: -60rpx;
			}
		}

		.back-btn {
			position: absolute;
			top: 60rpx;
			left: 32rpx;
			z-index: 10;
			width: 64rpx;
			height: 64rpx;
			display: flex;
			align-items: center;
			justify-content: center;
			color: #333333;
		}

		.header-content {
			position: relative;
			z-index: 1;
			text-align: center;

			.app-name {
				display: block;
				font-size: 44rpx;
				font-weight: 700;
				color: #1A1A2E;
			}

			.app-slogan {
				display: block;
				font-size: 26rpx;
				color: #666666;
				margin-top: 8rpx;
			}
		}
	}

	.register-form {
		padding: 40rpx 48rpx 0;
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

	.pwd-tips {
		display: flex;
		gap: 24rpx;
		margin-bottom: 24rpx;
		padding: 0 8rpx;

		.tip-item {
			font-size: 24rpx;
			color: #C0C0C0;

			&.valid {
				color: #10B981;
			}
		}
	}

	.register-btn {
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
</style>
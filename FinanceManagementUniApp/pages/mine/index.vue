<template>
	<view class="mine-page">
		<!-- 用户信息卡片 -->
		<view class="user-card">
			<view class="user-bg"></view>
			<view class="user-content">
				<view class="avatar">
					<image v-if="authStore.avatarUrl" :src="authStore.avatarUrl" class="avatar-img" />
					<text v-else style="font-size: 56rpx;">👤</text>
				</view>
				<view class="user-info">
					<text class="nickname">{{ authStore.nickname }}</text>
					<text class="phone">{{ maskedPhone }}</text>
				</view>
				<text class="edit-btn" @click="goProfile">编辑资料 →</text>
			</view>
		</view>

		<!-- 功能入口 -->
		<view class="menu-section">
			<view class="section-title">功能</view>
			<view class="menu-list">
				<view class="menu-item" @click="goPlans">
					<view class="menu-left">
						<view class="menu-icon plan-icon">📈</view>
						<text class="menu-text">理财计划</text>
					</view>
					<text class="menu-arrow">→</text>
				</view>
				<view class="menu-item" @click="goMemos">
					<view class="menu-left">
						<view class="menu-icon memo-icon">📝</view>
						<text class="menu-text">备忘录</text>
					</view>
					<text class="menu-arrow">→</text>
				</view>
				<view class="menu-item" @click="goAI">
					<view class="menu-left">
						<view class="menu-icon ai-icon">🤖</view>
						<text class="menu-text">AI 智能助手</text>
					</view>
					<text class="menu-arrow">→</text>
				</view>
			</view>
		</view>

		<!-- 设置 -->
		<view class="menu-section">
			<view class="section-title">设置</view>
			<view class="menu-list">
				<view class="menu-item" @click="goProfile">
					<view class="menu-left">
						<view class="menu-icon set-icon">👤</view>
						<text class="menu-text">个人资料</text>
					</view>
					<text class="menu-arrow">→</text>
				</view>
				<view class="menu-item" @click="goChangePwd">
					<view class="menu-left">
						<view class="menu-icon set-icon">🔒</view>
						<text class="menu-text">修改密码</text>
					</view>
					<text class="menu-arrow">→</text>
				</view>
				<view class="menu-item" @click="handleLogout">
					<view class="menu-left">
						<view class="menu-icon logout-icon">🚪</view>
						<text class="menu-text logout-text">退出登录</text>
					</view>
					<text class="menu-arrow">→</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
	import {
		computed
	} from 'vue'
	import {
		onLoad
	} from '@dcloudio/uni-app'
	import {
		onShow
	} from '@dcloudio/uni-app'
	import {
		useAuthStore
	} from '@/store/auth.js'

	const authStore = useAuthStore()

	// 脱敏手机号
	const maskedPhone = computed(() => {
		const phone = authStore.phone
		if (!phone || phone.length < 11) return ''
		return phone.substring(0, 3) + '****' + phone.substring(7)
	})

	function goPlans() {
		uni.navigateTo({
			url: '/pages/plan/index'
		})
	}

	function goMemos() {
		uni.navigateTo({
			url: '/pages/memo/index'
		})
	}

	function goAI() {
		uni.navigateTo({
			url: '/pages/ai/index'
		})
	}

	function goProfile() {
		uni.navigateTo({
			url: '/pages/mine/profile'
		})
	}

	function goChangePwd() {
		uni.navigateTo({
			url: '/pages/mine/changePassword'
		})
	}

	function handleLogout() {
		uni.showModal({
			title: '退出登录',
			content: '确定要退出登录吗？',
			success: (res) => {
				if (res.confirm) {
					authStore.logout()
				}
			}
		})
	}
</script>

<style lang="scss" scoped>
	.mine-page {
		min-height: 100vh;
		background: #F5F7FA;
	}

	.user-card {
		position: relative;

		.user-bg {
			position: absolute;
			top: 0;
			left: 0;
			right: 0;
			height: 260rpx;
			background: linear-gradient(135deg, #4F8CFF, #3B6FE8);
			border-radius: 0 0 32rpx 32rpx;
		}

		.user-content {
			position: relative;
			z-index: 1;
			display: flex;
			align-items: center;
			padding: 40rpx 32rpx 32rpx;

			.avatar-img {
				width: 100%;
				height: 100%;
				border-radius: 50%;
				object-fit: cover;
			}

			.avatar {
				width: 112rpx;
				height: 112rpx;
				background: rgba(255, 255, 255, 0.25);
				border-radius: 50%;
				display: flex;
				align-items: center;
				justify-content: center;
				margin-right: 24rpx;
				border: 4rpx solid rgba(255, 255, 255, 0.4);
			}

			.user-info {
				flex: 1;

				.nickname {
					display: block;
					font-size: 36rpx;
					font-weight: 600;
					color: #FFFFFF;
					margin-bottom: 8rpx;
				}

				.phone {
					display: block;
					font-size: 26rpx;
					color: rgba(255, 255, 255, 0.75);
				}
			}

			.edit-btn {
				font-size: 26rpx;
				color: rgba(255, 255, 255, 0.9);
				padding: 8rpx 16rpx;
				background: rgba(255, 255, 255, 0.15);
				border-radius: 24rpx;
			}
		}
	}

	.menu-section {
		margin: 24rpx 24rpx 0;

		.section-title {
			font-size: 26rpx;
			color: #999;
			margin-bottom: 12rpx;
			padding-left: 8rpx;
		}

		.menu-list {
			background: #FFFFFF;
			border-radius: 16rpx;
			overflow: hidden;
			box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
		}

		.menu-item {
			display: flex;
			justify-content: space-between;
			align-items: center;
			padding: 28rpx 24rpx;
			border-bottom: 1rpx solid #F0F2F5;

			&:last-child {
				border-bottom: none;
			}

			.menu-left {
				display: flex;
				align-items: center;
				gap: 16rpx;

				.menu-icon {
					width: 56rpx;
					height: 56rpx;
					border-radius: 14rpx;
					display: flex;
					align-items: center;
					justify-content: center;
					font-size: 30rpx;

					&.plan-icon {
						background: #EEF2FF;
					}

					&.memo-icon {
						background: #FFFBEB;
					}

					&.ai-icon {
						background: #ECFDF5;
					}

					&.set-icon {
						background: #F5F7FA;
					}

					&.logout-icon {
						background: #FEF2F2;
					}
				}

				.menu-text {
					font-size: 28rpx;
					color: #333;
				}

				.logout-text {
					color: #EF4444;
				}
			}

			.menu-arrow {
				font-size: 26rpx;
				color: #C0C0C0;
			}
		}
	}
</style>
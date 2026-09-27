<template>
	<view class="home-page">
		<!-- 顶部用户信息 -->
		<view class="home-header">
			<view class="header-bg"></view>
			<view class="header-content">
				<view class="user-row">
					<view class="avatar">
						<image v-if="authStore.avatarUrl" :src="authStore.avatarUrl" class="avatar-img" />
						<text v-else style="font-size: 48rpx;">👤</text>
					</view>
					<view class="user-info">
						<text class="greeting">{{ greeting }}，{{ authStore.nickname }}</text>
						<text class="date-text">{{ todayDate }}</text>
					</view>
					<view class="header-actions">
						<view class="icon-btn" @click="goAIChat">
							<text style="font-size: 40rpx;">🤖</text>
						</view>
					</view>
				</view>
				<!-- 本月收支概览 -->
				<view class="overview-row">
					<view class="overview-item">
						<text class="overview-label">本月收入</text>
						<text class="overview-value income">+{{ formatMoney(overview.income) }}</text>
					</view>
					<view class="overview-divider"></view>
					<view class="overview-item">
						<text class="overview-label">本月支出</text>
						<text class="overview-value expense">-{{ formatMoney(overview.expense) }}</text>
					</view>
					<view class="overview-divider"></view>
					<view class="overview-item">
						<text class="overview-label">本月结余</text>
						<text class="overview-value" :class="overview.balance >= 0 ? 'income' : 'expense'">
							{{ formatMoney(overview.balance) }}
						</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 快捷操作 -->
		<view class="quick-actions">
			<view class="action-item" @click="goAddBill(1)">
				<view class="action-icon income-bg">💰</view>
				<text class="action-text">记收入</text>
			</view>
			<view class="action-item" @click="goAddBill(0)">
				<view class="action-icon expense-bg">💳</view>
				<text class="action-text">记支出</text>
			</view>
			<view class="action-item" @click="goPlans">
				<view class="action-icon plan-bg">📈</view>
				<text class="action-text">理财计划</text>
			</view>
			<view class="action-item" @click="goMemos">
				<view class="action-icon memo-bg">📝</view>
				<text class="action-text">备忘录</text>
			</view>
		</view>

		<!-- 最近账单 -->
		<view class="section">
			<view class="section-header">
				<text class="section-title">最近账单</text>
				<text class="section-more" @click="goBills">查看全部 →</text>
			</view>
			<view v-if="recentBills.length > 0" class="bill-list">
				<view v-for="item in recentBills" :key="item.id" class="bill-item" @click="goBillDetail(item.id)">
					<view class="bill-left">
						<view class="bill-icon" :class="item.type === 1 ? 'icon-income' : 'icon-expense'">
							<text style="font-size: 32rpx;">{{ getCategoryIcon(item.categoryIcon) }}</text>
						</view>
						<view class="bill-info">
							<text class="bill-category">{{ item.categoryName }}</text>
							<text class="bill-time">{{ formatDate(item.recordTime, 'MM-dd HH:mm') }}</text>
						</view>
					</view>
					<view class="bill-right">
						<text class="bill-amount" :class="item.type === 1 ? 'income' : 'expense'">
							{{ item.type === 1 ? '+' : '-' }}{{ formatMoney(item.amount) }}
						</text>
					</view>
				</view>
			</view>
			<view v-else class="empty-block">
				<text class="empty-icon">📋</text>
				<text class="empty-text">还没有账单记录</text>
				<text class="empty-hint">点击上方按钮开始记账吧</text>
			</view>
		</view>

		<!-- 待处理备忘录 -->
		<view v-if="pendingMemos.length > 0" class="section">
			<view class="section-header">
				<text class="section-title">待办提醒</text>
				<text class="section-more" @click="goMemos">查看全部 →</text>
			</view>
			<view class="memo-list">
				<view v-for="item in pendingMemos" :key="item.id" class="memo-item" @click="handleToggleMemo(item)">
					<view class="memo-left">
						<view class="memo-check" :class="{ checked: item.isCompleted }">
							<text v-if="item.isCompleted" style="color: #fff; font-size: 22rpx;">✓</text>
						</view>
						<view class="memo-info">
							<text class="memo-title" :class="{ completed: item.isCompleted }">{{ item.title }}</text>
							<text v-if="item.remindTime" class="memo-time">⏰ {{ item.remindTime }}</text>
						</view>
					</view>
				</view>
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
		onShow
	} from '@dcloudio/uni-app'
	import {
		useAuthStore
	} from '@/store/auth.js'
	import {
		getStatisticsOverview,
		getBills,
		getMemos,
		toggleMemo
	} from '@/api/index.js'
	import {
		formatMoney,
		formatDate,
		getCurrentMonth
	} from '@/utils/index.js'

	const authStore = useAuthStore()

	// 问候语
	const greeting = computed(() => {
		const hour = new Date().getHours()
		if (hour < 6) return '夜深了'
		if (hour < 9) return '早上好'
		if (hour < 12) return '上午好'
		if (hour < 14) return '中午好'
		if (hour < 18) return '下午好'
		return '晚上好'
	})

	// 今天日期
	const todayDate = computed(() => formatDate(new Date(), 'yyyy年MM月dd日'))

	// 本月收支概览
	const overview = reactive({
		income: 0,
		expense: 0,
		balance: 0
	})

	// 最近账单
	const recentBills = ref([])

	// 待处理备忘录
	const pendingMemos = ref([])

	// 分类图标映射
	const iconMap = {
		food: '🍔',
		car: '🚗',
		shopping: '🛒',
		salary: '💵',
		travel: '✈️',
		house: '🏠',
		health: '💊',
		education: '📚',
		entertainment: '🎮',
		transport: '🚌',
		phone: '📱',
		clothes: '👔',
		gift: '🎁',
		invest: '📊',
		other: '📌'
	}

	function getCategoryIcon(icon) {
		return iconMap[icon] || '📌'
	}

	function goAddBill(type) {
		uni.navigateTo({
			url: `/pages/bill/add?type=${type}`
		})
	}

	function goBills() {
		uni.switchTab({
			url: '/pages/bill/index'
		})
	}

	function goBillDetail(id) {
		uni.navigateTo({
			url: `/pages/bill/detail?id=${id}`
		})
	}

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

	function goAIChat() {
		uni.navigateTo({
			url: '/pages/ai/index'
		})
	}

	async function handleToggleMemo(item) {
		try {
			await toggleMemo(item.id)
			item.isCompleted = item.isCompleted === 1 ? 0 : 1
			uni.showToast({
				title: item.isCompleted ? '已完成' : '已取消',
				icon: 'success'
			})
		} catch (err) {
			console.error(err)
		}
	}

	async function loadData() {
		const month = getCurrentMonth()
		try {
			const overviewRes = await getStatisticsOverview(month)
			if (overviewRes.data) {
				overview.income = overviewRes.data.income || 0
				overview.expense = overviewRes.data.expense || 0
				overview.balance = overviewRes.data.balance || 0
			}
		} catch (err) {
			console.error('加载概览失败:', err)
		}

		try {
			const billRes = await getBills({
				page: 1,
				size: 5,
				sortBy: 'recordTime',
				order: 'desc'
			})
			if (billRes.data && billRes.data.records) {
				recentBills.value = billRes.data.records
			}
		} catch (err) {
			console.error('加载账单失败:', err)
		}

		try {
			const memoRes = await getMemos({
				isCompleted: 0,
				page: 1,
				size: 5
			})
			if (memoRes.data && memoRes.data.records) {
				pendingMemos.value = memoRes.data.records
			}
		} catch (err) {
			console.error('加载备忘录失败:', err)
		}
	}

	onShow(() => {
		loadData()
	})
</script>

<style lang="scss" scoped>
	.home-page {
		min-height: 100vh;
		background: #F5F7FA;
		padding-bottom: 24rpx;
	}

	.home-header {
		position: relative;

		.header-bg {
			position: absolute;
			top: 0;
			left: 0;
			right: 0;
			height: 380rpx;
			background: linear-gradient(135deg, #4F8CFF, #3B6FE8);
			border-radius: 0 0 40rpx 40rpx;
		}

		.header-content {
			position: relative;
			z-index: 1;
			padding: 20rpx 32rpx 32rpx;
		}
	}

	.user-row {
		display: flex;
		align-items: center;
		padding-top: 16rpx;

		.avatar-img {
			width: 100%;
			height: 100%;
			border-radius: 50%;
			object-fit: cover;
		}

		.avatar {
			width: 80rpx;
			height: 80rpx;
			background: rgba(255, 255, 255, 0.25);
			border-radius: 50%;
			display: flex;
			align-items: center;
			justify-content: center;
			margin-right: 20rpx;
		}

		.user-info {
			flex: 1;

			.greeting {
				display: block;
				font-size: 32rpx;
				font-weight: 600;
				color: #FFFFFF;
			}

			.date-text {
				display: block;
				font-size: 24rpx;
				color: rgba(255, 255, 255, 0.7);
				margin-top: 4rpx;
			}
		}

		.header-actions .icon-btn {
			width: 72rpx;
			height: 72rpx;
			background: rgba(255, 255, 255, 0.2);
			border-radius: 50%;
			display: flex;
			align-items: center;
			justify-content: center;
		}
	}

	.overview-row {
		display: flex;
		align-items: center;
		background: #FFFFFF;
		border-radius: 20rpx;
		margin-top: 28rpx;
		padding: 28rpx 16rpx;
		box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);

		.overview-item {
			flex: 1;
			text-align: center;

			.overview-label {
				display: block;
				font-size: 24rpx;
				color: #999999;
				margin-bottom: 8rpx;
			}

			.overview-value {
				display: block;
				font-size: 32rpx;
				font-weight: 700;
				color: #333333;

				&.income {
					color: #10B981;
				}

				&.expense {
					color: #EF4444;
				}
			}
		}

		.overview-divider {
			width: 2rpx;
			height: 52rpx;
			background: #E8ECF1;
		}
	}

	.quick-actions {
		display: flex;
		justify-content: space-around;
		padding: 32rpx 24rpx;
		margin-top: 60rpx;

		.action-item {
			display: flex;
			flex-direction: column;
			align-items: center;

			.action-icon {
				width: 96rpx;
				height: 96rpx;
				border-radius: 24rpx;
				display: flex;
				align-items: center;
				justify-content: center;
				font-size: 44rpx;
				margin-bottom: 12rpx;

				&.income-bg {
					background: #ECFDF5;
				}

				&.expense-bg {
					background: #FEF2F2;
				}

				&.plan-bg {
					background: #EEF2FF;
				}

				&.memo-bg {
					background: #FFFBEB;
				}
			}

			.action-text {
				font-size: 24rpx;
				color: #666666;
			}
		}
	}

	.section {
		margin: 0 24rpx 20rpx;

		.section-header {
			display: flex;
			justify-content: space-between;
			align-items: center;
			margin-bottom: 16rpx;
			padding: 0 4rpx;

			.section-title {
				font-size: 32rpx;
				font-weight: 600;
				color: #1A1A2E;
			}

			.section-more {
				font-size: 26rpx;
				color: #3B6FE8;
			}
		}
	}

	.bill-list {
		background: #FFFFFF;
		border-radius: 16rpx;
		overflow: hidden;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.03);

		.bill-item {
			display: flex;
			justify-content: space-between;
			align-items: center;
			padding: 24rpx;
			border-bottom: 1rpx solid #F0F2F5;

			&:last-child {
				border-bottom: none;
			}

			.bill-left {
				display: flex;
				align-items: center;
				flex: 1;

				.bill-icon {
					width: 72rpx;
					height: 72rpx;
					border-radius: 16rpx;
					display: flex;
					align-items: center;
					justify-content: center;
					margin-right: 16rpx;

					&.icon-income {
						background: #ECFDF5;
					}

					&.icon-expense {
						background: #FEF2F2;
					}
				}

				.bill-info {
					.bill-category {
						display: block;
						font-size: 28rpx;
						font-weight: 500;
						color: #333333;
					}

					.bill-time {
						display: block;
						font-size: 24rpx;
						color: #999999;
						margin-top: 4rpx;
					}
				}
			}

			.bill-right .bill-amount {
				font-size: 30rpx;
				font-weight: 600;

				&.income {
					color: #10B981;
				}

				&.expense {
					color: #EF4444;
				}
			}
		}
	}

	.empty-block {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 60rpx 0;
		background: #FFFFFF;
		border-radius: 16rpx;

		.empty-icon {
			font-size: 72rpx;
			margin-bottom: 16rpx;
		}

		.empty-text {
			font-size: 28rpx;
			color: #999999;
		}

		.empty-hint {
			font-size: 24rpx;
			color: #C0C0C0;
			margin-top: 8rpx;
		}
	}

	.memo-list {
		background: #FFFFFF;
		border-radius: 16rpx;
		overflow: hidden;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.03);

		.memo-item {
			padding: 24rpx;
			border-bottom: 1rpx solid #F0F2F5;

			&:last-child {
				border-bottom: none;
			}

			.memo-left {
				display: flex;
				align-items: center;

				.memo-check {
					width: 44rpx;
					height: 44rpx;
					border-radius: 50%;
					border: 3rpx solid #D1D5DB;
					display: flex;
					align-items: center;
					justify-content: center;
					margin-right: 16rpx;
					flex-shrink: 0;

					&.checked {
						background: #10B981;
						border-color: #10B981;
					}
				}

				.memo-info {
					flex: 1;

					.memo-title {
						display: block;
						font-size: 28rpx;
						color: #333333;

						&.completed {
							color: #C0C0C0;
							text-decoration: line-through;
						}
					}

					.memo-time {
						display: block;
						font-size: 24rpx;
						color: #F59E0B;
						margin-top: 4rpx;
					}
				}
			}
		}
	}
</style>
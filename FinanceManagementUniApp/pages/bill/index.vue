<template>
	<view class="bill-page">
		<!-- 顶部筛选栏 -->
		<view class="filter-bar">
			<view class="filter-tabs">
				<view class="filter-tab" :class="{ active: filterType === '' }" @click="changeType('')">全部</view>
				<view class="filter-tab" :class="{ active: filterType === 1 }" @click="changeType(1)">收入</view>
				<view class="filter-tab" :class="{ active: filterType === 0 }" @click="changeType(0)">支出</view>
			</view>
			<view class="filter-date" @click="showDatePicker = true">
				<text style="font-size: 26rpx;">📅</text>
				<text class="date-text">{{ dateLabel }}</text>
				<text style="font-size: 22rpx; color: #999;">▼</text>
			</view>
		</view>

		<!-- 汇总信息 -->
		<view v-if="summary" class="summary-bar">
			<text class="summary-item income">收入 {{ formatMoney(summary.totalIncome) }}</text>
			<text class="summary-item expense">支出 {{ formatMoney(summary.totalExpense) }}</text>
		</view>

		<!-- 账单列表 -->
		<scroll-view class="bill-scroll" scroll-y @scrolltolower="loadMore"
			:style="{ height: 'calc(100vh - ' + (summary ? '260rpx' : '200rpx') + ')' }">
			<view v-if="list.length > 0">
				<!-- 按日期分组显示 -->
				<view v-for="(group, gIndex) in groupedList" :key="gIndex">
					<view class="date-header">
						<text class="date-title">{{ group.date }}</text>
						<text class="date-summary">
							收入 {{ formatMoney(group.income) }} 支出 {{ formatMoney(group.expense) }}
						</text>
					</view>
					<view class="bill-list">
						<view v-for="item in group.items" :key="item.id" class="bill-item" @click="goDetail(item.id)">
							<view class="bill-left">
								<view class="bill-icon" :class="item.type === 1 ? 'icon-income' : 'icon-expense'">
									<text style="font-size: 32rpx;">{{ getCategoryIcon(item.categoryIcon) }}</text>
								</view>
								<view class="bill-info">
									<text class="bill-category">{{ item.categoryName }}</text>
									<text v-if="item.description" class="bill-desc">{{ item.description }}</text>
								</view>
							</view>
							<text class="bill-amount" :class="item.type === 1 ? 'income' : 'expense'">
								{{ item.type === 1 ? '+' : '-' }}{{ formatMoney(item.amount) }}
							</text>
						</view>
					</view>
				</view>

				<view v-if="loading" class="loading-tip">加载中...</view>
				<view v-if="noMore && list.length > 0" class="loading-tip">— 没有更多了 —</view>
			</view>

			<view v-if="!loading && list.length === 0" class="empty-block">
				<text class="empty-icon">📋</text>
				<text class="empty-text">还没有账单记录</text>
			</view>
		</scroll-view>

		<!-- 添加按钮 -->
		<view class="float-btn" @click="showAddPopup">
			<text style="font-size: 48rpx; color: #fff; line-height: 1;">+</text>
		</view>

		<!-- 快速记账弹窗 -->
		<view class="popup-mask" v-if="showPopup" @click="showPopup = false"></view>
		<view class="popup-panel" :class="{ show: showPopup }">
			<view class="popup-header">
				<text class="popup-title">选择记账类型</text>
				<text class="popup-close" @click="showPopup = false">✕</text>
			</view>
			<view class="popup-actions">
				<view class="popup-action" @click="goAdd(1)">
					<view class="popup-icon income-bg">💰</view>
					<text class="popup-text">记收入</text>
				</view>
				<view class="popup-action" @click="goAdd(0)">
					<view class="popup-icon expense-bg">💳</view>
					<text class="popup-text">记支出</text>
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
		getBills
	} from '@/api/index.js'
	import {
		formatMoney,
		formatDate,
		getCurrentMonth
	} from '@/utils/index.js'

	// 筛选状态
	const filterType = ref('')
	const dateRange = reactive({
		startDate: '',
		endDate: ''
	})
	const dateLabel = ref('本月')
	const showPopup = ref(false)
	const showDatePicker = ref(false)

	// 列表状态
	const list = ref([])
	const page = ref(1)
	const loading = ref(false)
	const noMore = ref(false)
	const summary = ref(null)

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

	// 按日期分组的列表
	const groupedList = computed(() => {
		const groups = []
		let currentDate = ''
		let currentGroup = null

		list.value.forEach((item) => {
			const date = formatDate(item.recordTime, 'MM月dd日')
			if (date !== currentDate) {
				if (currentGroup) groups.push(currentGroup)
				currentGroup = {
					date,
					income: 0,
					expense: 0,
					items: []
				}
				currentDate = date
			}
			if (item.type === 1) {
				currentGroup.income += item.amount
			} else {
				currentGroup.expense += item.amount
			}
			currentGroup.items.push(item)
		})
		if (currentGroup) groups.push(currentGroup)
		return groups
	})

	function changeType(type) {
		filterType.value = type
		page.value = 1
		list.value = []
		noMore.value = false
		loadData()
	}

	function showAddPopup() {
		showPopup.value = true
	}

	function goAdd(type) {
		showPopup.value = false
		uni.navigateTo({
			url: `/pages/bill/add?type=${type}`
		})
	}

	function goDetail(id) {
		uni.navigateTo({
			url: `/pages/bill/detail?id=${id}`
		})
	}

	async function loadData() {
		if (loading.value) return
		loading.value = true

		try {
			const params = {
				page: page.value,
				size: 20,
				sortBy: 'recordTime',
				order: 'desc'
			}
			if (filterType.value !== '') {
				params.type = filterType.value
			}
			if (dateRange.startDate) params.startDate = dateRange.startDate
			if (dateRange.endDate) params.endDate = dateRange.endDate

			const res = await getBills(params)
			if (res.data) {
				if (page.value === 1) {
					list.value = res.data.records || []
				} else {
					list.value = list.value.concat(res.data.records || [])
				}
				summary.value = res.data.summary || null
				noMore.value = (res.data.records || []).length < 20
			}
		} catch (err) {
			console.error('加载账单失败:', err)
		} finally {
			loading.value = false
		}
	}

	function loadMore() {
		if (loading.value || noMore.value) return
		page.value++
		loadData()
	}

	onShow(() => {
		page.value = 1
		list.value = []
		noMore.value = false
		loadData()
	})
</script>

<style lang="scss" scoped>
	.bill-page {
		min-height: 100vh;
		background: #F5F7FA;
		position: relative;
	}

	.filter-bar {
		display: flex;
		justify-content: space-between;
		align-items: center;
		background: #FFFFFF;
		padding: 16rpx 24rpx;
		border-bottom: 1rpx solid #F0F2F5;

		.filter-tabs {
			display: flex;

			.filter-tab {
				padding: 10rpx 28rpx;
				font-size: 28rpx;
				color: #666666;
				border-radius: 32rpx;
				margin-right: 12rpx;
				background: #F5F7FA;

				&.active {
					background: #EEF2FF;
					color: #3B6FE8;
					font-weight: 600;
				}
			}
		}

		.filter-date {
			display: flex;
			align-items: center;
			gap: 8rpx;
			padding: 10rpx 16rpx;
			background: #F5F7FA;
			border-radius: 32rpx;

			.date-text {
				font-size: 26rpx;
				color: #666;
			}
		}
	}

	.summary-bar {
		display: flex;
		justify-content: center;
		gap: 48rpx;
		padding: 16rpx 24rpx;
		background: #FFFFFF;
		border-bottom: 1rpx solid #F0F2F5;

		.summary-item {
			font-size: 26rpx;
			font-weight: 500;

			&.income {
				color: #10B981;
			}

			&.expense {
				color: #EF4444;
			}
		}
	}

	.date-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 20rpx 32rpx 12rpx;

		.date-title {
			font-size: 28rpx;
			font-weight: 600;
			color: #1A1A2E;
		}

		.date-summary {
			font-size: 22rpx;
			color: #999999;
		}
	}

	.bill-list {
		margin: 0 24rpx;
		background: #FFFFFF;
		border-radius: 16rpx;
		overflow: hidden;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.03);

		.bill-item {
			display: flex;
			justify-content: space-between;
			align-items: center;
			padding: 22rpx 24rpx;
			border-bottom: 1rpx solid #F0F2F5;

			&:last-child {
				border-bottom: none;
			}

			.bill-left {
				display: flex;
				align-items: center;
				flex: 1;
				overflow: hidden;

				.bill-icon {
					width: 72rpx;
					height: 72rpx;
					border-radius: 16rpx;
					display: flex;
					align-items: center;
					justify-content: center;
					margin-right: 16rpx;
					flex-shrink: 0;

					&.icon-income {
						background: #ECFDF5;
					}

					&.icon-expense {
						background: #FEF2F2;
					}
				}

				.bill-info {
					overflow: hidden;

					.bill-category {
						display: block;
						font-size: 28rpx;
						font-weight: 500;
						color: #333;
					}

					.bill-desc {
						display: block;
						font-size: 24rpx;
						color: #999;
						margin-top: 4rpx;
						overflow: hidden;
						text-overflow: ellipsis;
						white-space: nowrap;
					}
				}
			}

			.bill-amount {
				font-size: 30rpx;
				font-weight: 600;
				flex-shrink: 0;
				margin-left: 16rpx;

				&.income {
					color: #10B981;
				}

				&.expense {
					color: #EF4444;
				}
			}
		}
	}

	.loading-tip {
		text-align: center;
		padding: 24rpx;
		font-size: 24rpx;
		color: #C0C0C0;
	}

	.empty-block {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding-top: 200rpx;

		.empty-icon {
			font-size: 96rpx;
			margin-bottom: 20rpx;
		}

		.empty-text {
			font-size: 28rpx;
			color: #999;
		}
	}

	.float-btn {
		position: fixed;
		bottom: 140rpx;
		right: 40rpx;
		width: 104rpx;
		height: 104rpx;
		background: linear-gradient(135deg, #4F8CFF, #3B6FE8);
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 8rpx 24rpx rgba(59, 111, 232, 0.4);
		z-index: 10;
	}

	.popup-mask {
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		bottom: 0;
		background: rgba(0, 0, 0, 0.5);
		z-index: 100;
	}

	.popup-panel {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		background: #FFFFFF;
		border-radius: 32rpx 32rpx 0 0;
		z-index: 101;
		padding: 32rpx 32rpx 60rpx;
		transform: translateY(100%);
		transition: transform 0.3s;

		&.show {
			transform: translateY(0);
		}

		.popup-header {
			display: flex;
			justify-content: space-between;
			align-items: center;
			margin-bottom: 40rpx;

			.popup-title {
				font-size: 32rpx;
				font-weight: 600;
				color: #1A1A2E;
			}

			.popup-close {
				font-size: 36rpx;
				color: #999;
				padding: 8rpx;
			}
		}

		.popup-actions {
			display: flex;
			justify-content: space-around;

			.popup-action {
				display: flex;
				flex-direction: column;
				align-items: center;

				.popup-icon {
					width: 120rpx;
					height: 120rpx;
					border-radius: 28rpx;
					display: flex;
					align-items: center;
					justify-content: center;
					font-size: 56rpx;
					margin-bottom: 16rpx;

					&.income-bg {
						background: #ECFDF5;
					}

					&.expense-bg {
						background: #FEF2F2;
					}
				}

				.popup-text {
					font-size: 28rpx;
					color: #333;
					font-weight: 500;
				}
			}
		}
	}
</style>
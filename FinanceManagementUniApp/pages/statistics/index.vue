<template>
	<view class="statistics-page">
		<!-- 月份切换 -->
		<view class="month-bar">
			<view class="month-arrow" @click="changeMonth(-1)">◀</view>
			<text class="month-text">{{ currentMonth }}</text>
			<view class="month-arrow" @click="changeMonth(1)">▶</view>
		</view>

		<!-- 收支总览 -->
		<view class="overview-card">
			<view class="overview-row">
				<view class="overview-item">
					<text class="ov-label">收入</text>
					<text class="ov-value income">{{ formatMoney(overview.income) }}</text>
				</view>
				<view class="ov-divider"></view>
				<view class="overview-item">
					<text class="ov-label">支出</text>
					<text class="ov-value expense">{{ formatMoney(overview.expense) }}</text>
				</view>
				<view class="ov-divider"></view>
				<view class="overview-item">
					<text class="ov-label">结余</text>
					<text class="ov-value" :class="overview.balance >= 0 ? 'income' : 'expense'">
						{{ formatMoney(overview.balance) }}
					</text>
				</view>
			</view>
		</view>

		<!-- 分类占比饼图 -->
		<view class="chart-card">
			<view class="chart-header">
				<text class="chart-title">支出分类占比</text>
				<view class="chart-tabs">
					<text class="chart-tab" :class="{ active: pieType === 0 }"
						@click="pieType = 0; loadPieData()">支出</text>
					<text class="chart-tab" :class="{ active: pieType === 1 }"
						@click="pieType = 1; loadPieData()">收入</text>
				</view>
			</view>

			<!-- 简易饼图（使用进度条模拟） -->
			<view class="pie-chart">
				<view v-if="pieData.length === 0" class="pie-empty">暂无数据</view>
				<view v-for="(item, index) in pieData" :key="index" class="pie-item">
					<view class="pie-info">
						<view class="pie-icon" :style="{ backgroundColor: pieColors[index % pieColors.length] }">
							<text style="font-size: 24rpx;">{{ getCategoryIcon(item.categoryIcon) }}</text>
						</view>
						<view class="pie-text">
							<text class="pie-name">{{ item.categoryName }}</text>
							<text class="pie-amount">{{ formatMoney(item.amount) }}</text>
						</view>
					</view>
					<view class="pie-bar-wrap">
						<view class="pie-bar"
							:style="{ width: item.percent, backgroundColor: pieColors[index % pieColors.length] }">
						</view>
					</view>
					<text class="pie-percent">{{ item.percent }}</text>
				</view>
			</view>
		</view>

		<!-- 月度趋势 -->
		<view class="chart-card">
			<view class="chart-header">
				<text class="chart-title">月度趋势</text>
				<text class="chart-sub">近{{ trendMonths }}个月</text>
			</view>
			<view v-if="trendData.length > 0" class="trend-chart">
				<!-- 简易柱状图 -->
				<scroll-view scroll-x class="trend-scroll">
					<view class="trend-bars">
						<view v-for="(item, index) in trendData" :key="index" class="trend-bar-group">
							<view class="bar-stack">
								<view class="bar income-bar" :style="{ height: getBarHeight(item.income, 'income') }">
								</view>
								<view class="bar expense-bar"
									:style="{ height: getBarHeight(item.expense, 'expense') }"></view>
							</view>
							<text class="bar-label">{{ item.month.substring(5) }}月</text>
						</view>
					</view>
				</scroll-view>
				<view class="trend-legend">
					<view class="legend-item">
						<view class="legend-dot income-dot"></view>收入
					</view>
					<view class="legend-item">
						<view class="legend-dot expense-dot"></view>支出
					</view>
				</view>
			</view>
			<view v-else class="pie-empty">暂无数据</view>
		</view>

		<!-- 年度总览 -->
		<view class="chart-card">
			<view class="chart-header">
				<text class="chart-title">年度总览</text>
				<text class="chart-sub">{{ currentYear }}年</text>
			</view>
			<view v-if="yearlyData" class="yearly-grid">
				<view class="yearly-item">
					<text class="yearly-label">年总收入</text>
					<text class="yearly-value income">{{ formatMoney(yearlyData.totalIncome) }}</text>
				</view>
				<view class="yearly-item">
					<text class="yearly-label">年总支出</text>
					<text class="yearly-value expense">{{ formatMoney(yearlyData.totalExpense) }}</text>
				</view>
				<view class="yearly-item">
					<text class="yearly-label">月均支出</text>
					<text class="yearly-value">{{ formatMoney(yearlyData.monthlyAvgExpense) }}</text>
				</view>
				<view class="yearly-item">
					<text class="yearly-label">最高支出月</text>
					<text class="yearly-value expense">
						{{ yearlyData.highestExpenseMonth ? yearlyData.highestExpenseMonth.substring(5) + '月' : '-' }}
					</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
	import {
		ref,
		reactive
	} from 'vue'
	import {
		onLoad
	} from '@dcloudio/uni-app'
	import {
		onShow
	} from '@dcloudio/uni-app'
	import {
		getStatisticsOverview,
		getCategoryPie,
		getTrend,
		getYearlyStatistics
	} from '@/api/index.js'
	import {
		formatMoney,
		getCurrentMonth,
		getCurrentYear
	} from '@/utils/index.js'

	// 当前月份
	const currentDate = new Date()
	const currentMonth = ref(getCurrentMonth())
	const currentYear = ref(getCurrentYear())
	const trendMonths = ref(12)
	const pieType = ref(0)

	// 数据
	const overview = reactive({
		income: 0,
		expense: 0,
		balance: 0
	})
	const pieData = ref([])
	const trendData = ref([])
	const yearlyData = ref(null)

	// 饼图颜色
	const pieColors = ['#3B6FE8', '#F59E0B', '#10B981', '#8B5CF6', '#EC4899', '#06B6D4', '#F97316', '#6366F1', '#14B8A6',
		'#E11D48'
	]

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

	function changeMonth(delta) {
		const [y, m] = currentMonth.value.split('-').map(Number)
		const d = new Date(y, m - 1 + delta, 1)
		const year = d.getFullYear()
		const month = String(d.getMonth() + 1).padStart(2, '0')
		currentMonth.value = `${year}-${month}`
		loadOverview()
		loadPieData()
	}

	// 柱状图高度计算
	function getBarHeight(amount, type) {
		const maxVal = Math.max(
			...trendData.value.map((d) => Math.max(d.income || 0, d.expense || 0)),
			1
		)
		const pct = (amount || 0) / maxVal * 100
		return Math.max(pct, 4) + '%'
	}

	async function loadOverview() {
		try {
			const res = await getStatisticsOverview(currentMonth.value)
			if (res.data) {
				overview.income = res.data.income || 0
				overview.expense = res.data.expense || 0
				overview.balance = res.data.balance || 0
			}
		} catch (err) {
			console.error(err)
		}
	}

	async function loadPieData() {
		try {
			const res = await getCategoryPie({
				month: currentMonth.value,
				type: pieType.value
			})
			if (res.data && res.data.items) {
				pieData.value = res.data.items
			} else {
				pieData.value = []
			}
		} catch (err) {
			pieData.value = []
		}
	}

	async function loadTrend() {
		try {
			const res = await getTrend(trendMonths.value)
			if (res.data) {
				trendData.value = res.data
			}
		} catch (err) {
			console.error(err)
		}
	}

	async function loadYearly() {
		try {
			const res = await getYearlyStatistics(currentYear.value)
			if (res.data) {
				yearlyData.value = res.data
			}
		} catch (err) {
			console.error(err)
		}
	}

	onShow(() => {
		loadOverview()
		loadPieData()
		loadTrend()
		loadYearly()
	})
</script>

<style lang="scss" scoped>
	.statistics-page {
		min-height: 100vh;
		background: #F5F7FA;
		padding-bottom: 32rpx;
	}

	.month-bar {
		display: flex;
		align-items: center;
		justify-content: center;
		background: #FFFFFF;
		padding: 20rpx 0;
		gap: 40rpx;
		border-bottom: 1rpx solid #F0F2F5;

		.month-arrow {
			font-size: 28rpx;
			color: #3B6FE8;
			padding: 8rpx 16rpx;
		}

		.month-text {
			font-size: 32rpx;
			font-weight: 600;
			color: #1A1A2E;
			min-width: 180rpx;
			text-align: center;
		}
	}

	.overview-card {
		margin: 20rpx 24rpx;
		background: #FFFFFF;
		border-radius: 16rpx;
		padding: 28rpx 24rpx;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.03);
	}

	.overview-row {
		display: flex;
		align-items: center;

		.overview-item {
			flex: 1;
			text-align: center;

			.ov-label {
				display: block;
				font-size: 24rpx;
				color: #999;
				margin-bottom: 8rpx;
			}

			.ov-value {
				display: block;
				font-size: 30rpx;
				font-weight: 700;

				&.income {
					color: #10B981;
				}

				&.expense {
					color: #EF4444;
				}
			}
		}

		.ov-divider {
			width: 2rpx;
			height: 48rpx;
			background: #E8ECF1;
		}
	}

	.chart-card {
		margin: 0 24rpx 20rpx;
		background: #FFFFFF;
		border-radius: 16rpx;
		padding: 24rpx;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.03);

		.chart-header {
			display: flex;
			justify-content: space-between;
			align-items: center;
			margin-bottom: 24rpx;

			.chart-title {
				font-size: 30rpx;
				font-weight: 600;
				color: #1A1A2E;
			}

			.chart-sub {
				font-size: 24rpx;
				color: #999;
			}
		}

		.chart-tabs {
			display: flex;
			gap: 8rpx;

			.chart-tab {
				font-size: 24rpx;
				padding: 6rpx 20rpx;
				border-radius: 20rpx;
				color: #999;
				background: #F5F7FA;

				&.active {
					background: #EEF2FF;
					color: #3B6FE8;
				}
			}
		}
	}

	.pie-empty {
		text-align: center;
		padding: 60rpx 0;
		color: #C0C0C0;
		font-size: 28rpx;
	}

	.pie-item {
		display: flex;
		align-items: center;
		margin-bottom: 16rpx;
		gap: 16rpx;

		.pie-info {
			display: flex;
			align-items: center;
			width: 260rpx;
			flex-shrink: 0;

			.pie-icon {
				width: 48rpx;
				height: 48rpx;
				border-radius: 12rpx;
				display: flex;
				align-items: center;
				justify-content: center;
				margin-right: 12rpx;
			}

			.pie-text {
				overflow: hidden;

				.pie-name {
					display: block;
					font-size: 26rpx;
					color: #333;
				}

				.pie-amount {
					display: block;
					font-size: 22rpx;
					color: #999;
				}
			}
		}

		.pie-bar-wrap {
			flex: 1;
			height: 12rpx;
			background: #F0F2F5;
			border-radius: 6rpx;
			overflow: hidden;

			.pie-bar {
				height: 100%;
				border-radius: 6rpx;
				min-width: 4rpx;
			}
		}

		.pie-percent {
			font-size: 24rpx;
			color: #666;
			width: 80rpx;
			text-align: right;
			flex-shrink: 0;
		}
	}

	.trend-scroll {
		width: 100%;
	}

	.trend-bars {
		display: flex;
		align-items: flex-end;
		gap: 16rpx;
		padding: 0 8rpx;
		min-width: max-content;
		height: 300rpx;

		.trend-bar-group {
			display: flex;
			flex-direction: column;
			align-items: center;
			width: 60rpx;

			.bar-stack {
				display: flex;
				align-items: flex-end;
				gap: 4rpx;
				height: 220rpx;
				width: 100%;
				justify-content: center;

				.bar {
					width: 24rpx;
					border-radius: 4rpx 4rpx 0 0;

					&.income-bar {
						background: #10B981;
					}

					&.expense-bar {
						background: #EF4444;
					}
				}
			}

			.bar-label {
				font-size: 22rpx;
				color: #999;
				margin-top: 8rpx;
			}
		}
	}

	.trend-legend {
		display: flex;
		justify-content: center;
		gap: 40rpx;
		margin-top: 20rpx;

		.legend-item {
			display: flex;
			align-items: center;
			font-size: 24rpx;
			color: #666;
			gap: 8rpx;

			.legend-dot {
				width: 16rpx;
				height: 16rpx;
				border-radius: 4rpx;

				&.income-dot {
					background: #10B981;
				}

				&.expense-dot {
					background: #EF4444;
				}
			}
		}
	}

	.yearly-grid {
		display: flex;
		flex-wrap: wrap;

		.yearly-item {
			width: 50%;
			padding: 16rpx 0;
			text-align: center;

			.yearly-label {
				display: block;
				font-size: 24rpx;
				color: #999;
				margin-bottom: 8rpx;
			}

			.yearly-value {
				display: block;
				font-size: 30rpx;
				font-weight: 600;
				color: #333;

				&.income {
					color: #10B981;
				}

				&.expense {
					color: #EF4444;
				}
			}
		}
	}
</style>
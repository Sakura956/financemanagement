<template>
	<view class="plan-detail-page">
		<view v-if="plan" class="detail-content">
			<!-- 收益头部 -->
			<view class="profit-header">
				<text class="profit-label">累计收益</text>
				<text class="profit-amount" :class="plan.profitAmount >= 0 ? 'income' : 'expense'">
					{{ plan.profitAmount >= 0 ? '+' : '' }}{{ formatMoney(plan.profitAmount) }}
				</text>
				<text class="profit-rate" :class="plan.profitRate >= 0 ? 'income' : 'expense'">
					收益率 {{ plan.profitRate != null ? plan.profitRate.toFixed(2) + '%' : '-' }}
				</text>
			</view>

			<!-- 详情卡片 -->
			<view class="detail-card">
				<view class="detail-item">
					<text class="d-label">计划名称</text>
					<text class="d-value">{{ plan.name }}</text>
				</view>
				<view class="detail-item">
					<text class="d-label">状态</text>
					<text class="d-value" :class="plan.status === 0 ? 'income' : ''">
						{{ plan.status === 0 ? '持有中' : '已赎回' }}
					</text>
				</view>
				<view class="detail-item">
					<text class="d-label">初始投入</text>
					<text class="d-value">{{ formatMoney(plan.initialAmount) }}</text>
				</view>
				<view class="detail-item">
					<text class="d-label">当前市值</text>
					<text class="d-value">{{ formatMoney(plan.currentValue) }}</text>
				</view>
				<view class="detail-item">
					<text class="d-label">预期年化</text>
					<text class="d-value">{{ plan.expectedRoi != null ? plan.expectedRoi + '%' : '-' }}</text>
				</view>
				<view class="detail-item">
					<text class="d-label">开始日期</text>
					<text class="d-value">{{ plan.startDate }}</text>
				</view>
				<view class="detail-item" v-if="plan.endDate">
					<text class="d-label">结束日期</text>
					<text class="d-value">{{ plan.endDate }}</text>
				</view>
				<view class="detail-item" v-if="plan.remark">
					<text class="d-label">备注</text>
					<text class="d-value">{{ plan.remark }}</text>
				</view>
			</view>

			<!-- 操作 -->
			<view class="action-section">
				<view v-if="plan.status === 0" class="action-row">
					<button class="btn-outline action-half" @click="showValuePopup = true">更新市值</button>
					<button class="btn-warning action-half" @click="handleRedeem">标记赎回</button>
				</view>
				<view class="action-row">
					<button class="btn-outline action-half" @click="goEdit">修改信息</button>
					<button class="btn-danger action-half" @click="handleDelete">删除计划</button>
				</view>
			</view>
		</view>

		<!-- 更新市值弹窗 -->
		<view class="popup-mask" v-if="showValuePopup" @click="showValuePopup = false"></view>
		<view class="popup-panel" v-if="showValuePopup">
			<text class="popup-title">更新当前市值</text>
			<input class="popup-input" v-model="newValueStr" type="digit" placeholder="请输入最新市值"
				placeholder-style="color:#C0C0C0" />
			<view class="popup-btns">
				<button class="popup-btn cancel" @click="showValuePopup = false">取消</button>
				<button class="popup-btn confirm" @click="handleUpdateValue">确认</button>
			</view>
		</view>

		<view v-if="!plan" class="loading-block"><text>加载中...</text></view>
	</view>
</template>

<script setup>
	import {
		ref
	} from 'vue'
	import {
		onLoad
	} from '@dcloudio/uni-app'
	import {
		onShow
	} from '@dcloudio/uni-app'
	import {
		getFinancePlanDetail,
		updateFinancePlanValuation,
		updateFinancePlanStatus,
		deleteFinancePlan
	} from '@/api/index.js'
	import {
		formatMoney,
		formatDate
	} from '@/utils/index.js'

	const plan = ref(null)
	const showValuePopup = ref(false)
	const newValueStr = ref('')

	function goEdit() {
		if (plan.value) {
			uni.navigateTo({
				url: `/pages/plan/add?id=${plan.value.id}`
			})
		}
	}

	async function handleUpdateValue() {
		const val = parseFloat(newValueStr.value)
		if (isNaN(val) || val < 0) {
			uni.showToast({
				title: '请输入有效的市值',
				icon: 'none'
			});
			return
		}
		try {
			const res = await updateFinancePlanValuation(plan.value.id, {
				currentValue: val
			})
			if (res.data) {
				plan.value.currentValue = res.data.currentValue
				plan.value.profitAmount = res.data.profitAmount
				plan.value.profitRate = res.data.profitRate
			}
			showValuePopup.value = false
			newValueStr.value = ''
			uni.showToast({
				title: '市值更新成功',
				icon: 'success'
			})
		} catch (err) {
			console.error(err)
		}
	}

	async function handleRedeem() {
		uni.showModal({
			title: '确认赎回',
			content: '确定将该理财计划标记为已赎回状态吗？',
			success: async (res) => {
				if (res.confirm && plan.value) {
					try {
						await updateFinancePlanStatus(plan.value.id, {
							status: 1,
							endDate: formatDate(new Date(), 'yyyy-MM-dd')
						})
						plan.value.status = 1
						uni.showToast({
							title: '已标记为已赎回',
							icon: 'success'
						})
					} catch (err) {
						console.error(err)
					}
				}
			}
		})
	}

	async function handleDelete() {
		uni.showModal({
			title: '确认删除',
			content: '确定要删除该理财计划吗？删除后不可恢复。',
			success: async (res) => {
				if (res.confirm && plan.value) {
					try {
						await deleteFinancePlan(plan.value.id)
						uni.showToast({
							title: '删除成功',
							icon: 'success'
						})
						setTimeout(() => {
							uni.navigateBack()
						}, 1000)
					} catch (err) {
						console.error(err)
					}
				}
			}
		})
	}

	onLoad((options) => {
		if (options && options.id) {
			loadDetail(options.id)
		}
	})

	async function loadDetail(id) {
		try {
			const res = await getFinancePlanDetail(id)
			plan.value = res.data
		} catch (err) {
			console.error(err)
			uni.showToast({
				title: '计划不存在',
				icon: 'none'
			})
			setTimeout(() => {
				uni.navigateBack()
			}, 1500)
		}
	}
</script>

<style lang="scss" scoped>
	.plan-detail-page {
		min-height: 100vh;
		background: #F5F7FA;
	}

	.profit-header {
		background: linear-gradient(135deg, #EEF2FF, #FFFFFF);
		padding: 48rpx 32rpx;
		text-align: center;

		.profit-label {
			display: block;
			font-size: 26rpx;
			color: #999;
			margin-bottom: 12rpx;
		}

		.profit-amount {
			display: block;
			font-size: 60rpx;
			font-weight: 700;

			&.income {
				color: #10B981;
			}

			&.expense {
				color: #EF4444;
			}
		}

		.profit-rate {
			display: block;
			font-size: 28rpx;
			margin-top: 8rpx;

			&.income {
				color: #10B981;
			}

			&.expense {
				color: #EF4444;
			}
		}
	}

	.detail-card {
		margin: 24rpx;
		background: #FFFFFF;
		border-radius: 16rpx;
		padding: 0 28rpx;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.03);
	}

	.detail-item {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 24rpx 0;
		border-bottom: 1rpx solid #F0F2F5;

		&:last-child {
			border-bottom: none;
		}

		.d-label {
			font-size: 28rpx;
			color: #999;
			width: 160rpx;
			flex-shrink: 0;
		}

		.d-value {
			font-size: 28rpx;
			color: #333;
			text-align: right;
			flex: 1;

			&.income {
				color: #10B981;
			}
		}
	}

	.action-section {
		padding: 20rpx 32rpx;
	}

	.action-row {
		display: flex;
		gap: 16rpx;
		margin-bottom: 16rpx;
	}

	.action-half {
		flex: 1;
		height: 80rpx;
		font-size: 28rpx;
		border-radius: 40rpx;
	}

	.btn-warning {
		flex: 1;
		height: 80rpx;
		background: #FFF7ED;
		color: #F59E0B;
		font-size: 28rpx;
		border-radius: 40rpx;
		border: 2rpx solid #FED7AA;
	}

	.btn-danger {
		flex: 1;
		height: 80rpx;
		background: #FFFFFF;
		color: #EF4444;
		font-size: 28rpx;
		border-radius: 40rpx;
		border: 2rpx solid #FECACA;
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
		top: 40%;
		left: 48rpx;
		right: 48rpx;
		background: #FFFFFF;
		border-radius: 24rpx;
		padding: 40rpx 32rpx;
		z-index: 101;

		.popup-title {
			display: block;
			font-size: 32rpx;
			font-weight: 600;
			text-align: center;
			margin-bottom: 32rpx;
		}

		.popup-input {
			height: 88rpx;
			background: #F5F7FA;
			border-radius: 12rpx;
			padding: 0 20rpx;
			font-size: 32rpx;
			text-align: center;
			margin-bottom: 28rpx;
		}

		.popup-btns {
			display: flex;
			gap: 16rpx;

			.popup-btn {
				flex: 1;
				height: 80rpx;
				font-size: 28rpx;
				border-radius: 40rpx;

				&.cancel {
					background: #F5F7FA;
					color: #666;
				}

				&.confirm {
					background: #3B6FE8;
					color: #FFFFFF;
				}
			}
		}
	}

	.loading-block {
		display: flex;
		align-items: center;
		justify-content: center;
		padding-top: 200rpx;
		color: #999;
	}
</style>
<template>
	<view class="detail-page">
		<view v-if="bill" class="detail-content">
			<!-- 金额展示 -->
			<view class="amount-header" :class="bill.type === 1 ? 'income-bg' : 'expense-bg'">
				<text class="amount-type">{{ bill.type === 1 ? '收入' : '支出' }}</text>
				<text class="amount-value" :class="bill.type === 1 ? 'income' : 'expense'">
					{{ bill.type === 1 ? '+' : '-' }}{{ formatMoney(bill.amount) }}
				</text>
			</view>

			<!-- 详情卡片 -->
			<view class="detail-card">
				<view class="detail-item">
					<text class="detail-label">分类</text>
					<view class="detail-value-row">
						<view class="cat-icon" :class="bill.type === 1 ? 'icon-income' : 'icon-expense'">
							<text style="font-size: 28rpx;">{{ getCategoryIcon(bill.categoryIcon) }}</text>
						</view>
						<text class="detail-value">{{ bill.categoryName }}</text>
					</view>
				</view>

				<view class="detail-item">
					<text class="detail-label">时间</text>
					<text class="detail-value">{{ bill.recordTime }}</text>
				</view>

				<view v-if="bill.description" class="detail-item">
					<text class="detail-label">备注</text>
					<text class="detail-value">{{ bill.description }}</text>
				</view>

				<view class="detail-item">
					<text class="detail-label">创建时间</text>
					<text class="detail-value">{{ bill.createTime }}</text>
				</view>

				<view v-if="bill.updateTime && bill.updateTime !== bill.createTime" class="detail-item">
					<text class="detail-label">修改时间</text>
					<text class="detail-value">{{ bill.updateTime }}</text>
				</view>
			</view>

			<!-- 操作按钮 -->
			<view class="action-btns">
				<button class="btn-outline" @click="goEdit">修改账单</button>
				<button class="btn-danger" @click="handleDelete">删除账单</button>
			</view>
		</view>

		<view v-else class="loading-block">
			<text>加载中...</text>
		</view>
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
		getBillDetail,
		deleteBill
	} from '@/api/index.js'
	import {
		formatMoney
	} from '@/utils/index.js'

	const bill = ref(null)

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

	function goEdit() {
		if (bill.value) {
			uni.navigateTo({
				url: `/pages/bill/add?id=${bill.value.id}`
			})
		}
	}

	async function handleDelete() {
		uni.showModal({
			title: '确认删除',
			content: '确定要删除这笔账单吗？删除后不可恢复。',
			success: async (res) => {
				if (res.confirm && bill.value) {
					try {
						await deleteBill(bill.value.id)
						uni.showToast({
							title: '删除成功',
							icon: 'success'
						})
						setTimeout(() => {
							uni.navigateBack()
						}, 1000)
					} catch (err) {
						console.error('删除失败:', err)
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
			const res = await getBillDetail(id)
			bill.value = res.data
		} catch (err) {
			console.error('加载账单详情失败:', err)
			uni.showToast({
				title: '账单不存在',
				icon: 'none'
			})
			setTimeout(() => {
				uni.navigateBack()
			}, 1500)
		}
	}
</script>

<style lang="scss" scoped>
	.detail-page {
		min-height: 100vh;
		background: #F5F7FA;
	}

	.amount-header {
		padding: 48rpx 32rpx;
		text-align: center;

		&.income-bg {
			background: linear-gradient(135deg, #10B981, #059669);
		}

		&.expense-bg {
			background: linear-gradient(135deg, #EF4444, #DC2626);
		}

		.amount-type {
			display: block;
			font-size: 26rpx;
			color: rgba(255, 255, 255, 0.8);
			margin-bottom: 12rpx;
		}

		.amount-value {
			display: block;
			font-size: 60rpx;
			font-weight: 700;
			color: #FFFFFF;

			&.income {
				color: #FFFFFF;
			}

			&.expense {
				color: #FFFFFF;
			}
		}
	}

	.detail-card {
		margin: 24rpx;
		background: #FFFFFF;
		border-radius: 16rpx;
		overflow: hidden;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.03);
	}

	.detail-item {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 24rpx 28rpx;
		border-bottom: 1rpx solid #F0F2F5;

		&:last-child {
			border-bottom: none;
		}

		.detail-label {
			font-size: 28rpx;
			color: #999;
			flex-shrink: 0;
			width: 160rpx;
		}

		.detail-value {
			font-size: 28rpx;
			color: #333;
			text-align: right;
			flex: 1;
		}

		.detail-value-row {
			display: flex;
			align-items: center;
			gap: 12rpx;

			.cat-icon {
				width: 48rpx;
				height: 48rpx;
				border-radius: 12rpx;
				display: flex;
				align-items: center;
				justify-content: center;

				&.icon-income {
					background: #ECFDF5;
				}

				&.icon-expense {
					background: #FEF2F2;
				}
			}
		}
	}

	.action-btns {
		padding: 40rpx 32rpx;
		display: flex;
		flex-direction: column;
		gap: 20rpx;

		.btn-danger {
			width: 100%;
			height: 88rpx;
			background: #FFFFFF;
			color: #EF4444;
			font-size: 32rpx;
			border-radius: 44rpx;
			border: 2rpx solid #FECACA;

			&:active {
				background: #FEF2F2;
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
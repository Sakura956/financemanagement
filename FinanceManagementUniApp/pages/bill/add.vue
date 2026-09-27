<template>
	<view class="add-bill-page">
		<!-- 类型切换 -->
		<view class="type-switch">
			<view class="switch-item" :class="{ active: form.type === 1 }" @click="switchType(1)">
				<text class="switch-text">收入</text>
			</view>
			<view class="switch-item" :class="{ active: form.type === 0 }" @click="switchType(0)">
				<text class="switch-text">支出</text>
			</view>
		</view>

		<!-- 金额输入 -->
		<view class="amount-section">
			<text class="amount-label">金额</text>
			<view class="amount-input-row">
				<text class="amount-symbol">¥</text>
				<input class="amount-input" v-model="amountStr" type="digit" placeholder="0.00"
					placeholder-style="color: #D1D5DB" :focus="amountFocused" @blur="amountFocused = false" />
			</view>
		</view>

		<!-- 分类选择 -->
		<view class="form-section">
			<view class="section-title">选择分类</view>
			<view class="category-grid">
				<view v-for="cat in categories" :key="cat.id" class="category-item"
					:class="{ selected: form.categoryId === cat.id }" @click="form.categoryId = cat.id">
					<view class="cat-icon" :class="form.type === 1 ? 'income-bg' : 'expense-bg'">
						<text style="font-size: 40rpx;">{{ getCategoryIcon(cat.icon) }}</text>
					</view>
					<text class="cat-name">{{ cat.name }}</text>
				</view>
			</view>
		</view>

		<!-- 表单信息 -->
		<view class="form-section">
			<view class="section-title">详细信息</view>

			<view class="form-item">
				<text class="form-label">时间</text>
				<picker mode="date" :value="form.recordDate" @change="onDateChange">
					<text class="form-value">{{ form.recordDate }}</text>
				</picker>
			</view>

			<view class="form-item">
				<text class="form-label">备注</text>
				<input class="form-input-right" v-model="form.description" placeholder="添加备注说明（选填）"
					placeholder-style="color: #C0C0C0" maxlength="500" />
			</view>
		</view>

		<!-- 提交按钮 -->
		<view class="submit-area">
			<button class="btn-primary" :loading="submitting" @click="handleSubmit">
				{{ isEdit ? '保存修改' : '确认记账' }}
			</button>
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
		getCategories,
		createBill,
		updateBill,
		getBillDetail
	} from '@/api/index.js'
	import {
		formatDate
	} from '@/utils/index.js'

	const isEdit = ref(false)
	const editId = ref(null)
	const submitting = ref(false)
	const amountFocused = ref(true)

	const amountStr = ref('')
	const categories = ref([])

	const form = reactive({
		type: 0, // 0-支出 1-收入
		amount: 0,
		categoryId: null,
		description: '',
		recordTime: '',
		recordDate: formatDate(new Date(), 'yyyy-MM-dd')
	})

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

	function switchType(type) {
		if (form.type === type) return
		form.type = type
		form.categoryId = null
		loadCategories()
	}

	async function loadCategories() {
		try {
			const res = await getCategories(form.type)
			if (res.data && res.data.length > 0) {
				categories.value = res.data
			}
		} catch (err) {
			console.error('加载分类失败:', err)
		}
	}

	function onDateChange(e) {
		form.recordDate = e.detail.value
	}

	async function handleSubmit() {
		// 校验金额
		const amount = parseFloat(amountStr.value)
		if (isNaN(amount) || amount <= 0) {
			uni.showToast({
				title: '请输入有效的金额',
				icon: 'none'
			})
			return
		}
		if (!/^\d+(\.\d{1,2})?$/.test(amountStr.value)) {
			uni.showToast({
				title: '金额最多保留两位小数',
				icon: 'none'
			})
			return
		}

		// 校验分类
		if (!form.categoryId) {
			uni.showToast({
				title: '请选择分类',
				icon: 'none'
			})
			return
		}

		submitting.value = true
		try {
			const data = {
				type: form.type,
				amount: amount,
				categoryId: form.categoryId,
				description: form.description.trim(),
				recordTime: form.recordDate + ' ' + formatDate(new Date(), 'HH:mm:ss')
			}

			if (isEdit.value) {
				await updateBill(editId.value, data)
				uni.showToast({
					title: '修改成功',
					icon: 'success'
				})
			} else {
				await createBill(data)
				uni.showToast({
					title: '记账成功',
					icon: 'success'
				})
			}

			setTimeout(() => {
				uni.navigateBack()
			}, 1000)
		} catch (err) {
			console.error('提交失败:', err)
		} finally {
			submitting.value = false
		}
	}

	onLoad((options) => {
		// 从URL参数获取记账类型
		if (options && options.type !== undefined) {
			form.type = parseInt(options.type)
		}
		if (options && options.id) {
			isEdit.value = true
			editId.value = parseInt(options.id)
			loadBillDetail(options.id)
		}
		loadCategories()
	})

	async function loadBillDetail(id) {
		try {
			const res = await getBillDetail(id)
			if (res.data) {
				const d = res.data
				form.type = d.type
				form.categoryId = d.categoryId
				form.description = d.description || ''
				form.recordDate = formatDate(d.recordTime, 'yyyy-MM-dd')
				amountStr.value = String(d.amount)
			}
		} catch (err) {
			console.error('加载账单详情失败:', err)
		}
	}
</script>

<style lang="scss" scoped>
	.add-bill-page {
		min-height: 100vh;
		background: #F5F7FA;
		padding-bottom: 40rpx;
	}

	.type-switch {
		display: flex;
		background: #FFFFFF;
		padding: 20rpx 48rpx;
		gap: 24rpx;

		.switch-item {
			flex: 1;
			text-align: center;
			padding: 16rpx 0;
			border-radius: 32rpx;
			background: #F5F7FA;
			font-size: 28rpx;
			color: #666;
			font-weight: 500;

			&.active {
				background: #EEF2FF;
				color: #3B6FE8;
			}
		}
	}

	.amount-section {
		background: #FFFFFF;
		padding: 40rpx 32rpx;
		margin-top: 16rpx;

		.amount-label {
			font-size: 26rpx;
			color: #999;
			margin-bottom: 16rpx;
			display: block;
		}

		.amount-input-row {
			display: flex;
			align-items: baseline;
			border-bottom: 4rpx solid #3B6FE8;
			padding-bottom: 12rpx;

			.amount-symbol {
				font-size: 52rpx;
				font-weight: 700;
				color: #1A1A2E;
				margin-right: 8rpx;
			}

			.amount-input {
				flex: 1;
				font-size: 64rpx;
				font-weight: 700;
				color: #1A1A2E;
				height: 88rpx;
			}
		}
	}

	.form-section {
		background: #FFFFFF;
		margin-top: 16rpx;
		padding: 24rpx 32rpx;

		.section-title {
			font-size: 28rpx;
			font-weight: 600;
			color: #1A1A2E;
			margin-bottom: 20rpx;
		}
	}

	.category-grid {
		display: flex;
		flex-wrap: wrap;
		gap: 20rpx;

		.category-item {
			display: flex;
			flex-direction: column;
			align-items: center;
			width: calc(25% - 15rpx);
			padding: 16rpx 0;

			.cat-icon {
				width: 80rpx;
				height: 80rpx;
				border-radius: 20rpx;
				display: flex;
				align-items: center;
				justify-content: center;
				margin-bottom: 8rpx;

				&.income-bg {
					background: #ECFDF5;
				}

				&.expense-bg {
					background: #FEF2F2;
				}
			}

			.cat-name {
				font-size: 24rpx;
				color: #666;
			}

			&.selected {
				.cat-icon {
					border: 3rpx solid #3B6FE8;
				}

				.cat-name {
					color: #3B6FE8;
					font-weight: 600;
				}
			}
		}
	}

	.form-item {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 24rpx 0;
		border-bottom: 1rpx solid #F0F2F5;

		.form-label {
			font-size: 28rpx;
			color: #333;
			flex-shrink: 0;
			width: 120rpx;
		}

		.form-value {
			font-size: 28rpx;
			color: #3B6FE8;
			text-align: right;
		}

		.form-input-right {
			flex: 1;
			font-size: 28rpx;
			color: #333;
			text-align: right;
		}
	}

	.submit-area {
		padding: 40rpx 32rpx;
	}
</style>
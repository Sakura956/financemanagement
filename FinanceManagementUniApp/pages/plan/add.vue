<template>
	<view class="add-plan-page">
		<view class="form-card">
			<view class="form-item">
				<text class="form-label">计划名称 <text class="required">*</text></text>
				<input class="form-input" v-model="form.name" placeholder="请输入理财计划名称" placeholder-style="color:#C0C0C0"
					maxlength="50" />
			</view>

			<view class="form-item">
				<text class="form-label">初始投入 <text class="required">*</text></text>
				<input class="form-input" v-model="initialAmountStr" type="digit" placeholder="请输入初始投入金额"
					placeholder-style="color:#C0C0C0" />
			</view>

			<view class="form-item">
				<text class="form-label">当前市值 <text class="required">*</text></text>
				<input class="form-input" v-model="currentValueStr" type="digit" placeholder="请输入当前市值"
					placeholder-style="color:#C0C0C0" />
			</view>

			<view class="form-item">
				<text class="form-label">预期收益率</text>
				<view class="form-input-row">
					<input class="form-input short-input" v-model="expectedRoiStr" type="digit" placeholder="预期年化"
						placeholder-style="color:#C0C0C0" />
					<text class="input-suffix">%</text>
				</view>
			</view>

			<view class="form-item">
				<text class="form-label">开始日期 <text class="required">*</text></text>
				<picker mode="date" :value="form.startDate" @change="onStartDateChange">
					<text class="form-picker">{{ form.startDate || '请选择' }}</text>
				</picker>
			</view>

			<view class="form-item">
				<text class="form-label">结束日期</text>
				<picker mode="date" :value="form.endDate" @change="onEndDateChange">
					<text class="form-picker">{{ form.endDate || '请选择（选填）' }}</text>
				</picker>
			</view>

			<view class="form-item">
				<text class="form-label">备注</text>
				<textarea class="form-textarea" v-model="form.remark" placeholder="添加备注说明（选填）"
					placeholder-style="color:#C0C0C0" maxlength="500" />
			</view>
		</view>

		<view class="submit-area">
			<button class="btn-primary" :loading="submitting" @click="handleSubmit">
				{{ isEdit ? '保存修改' : '创建计划' }}
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
		onShow
	} from '@dcloudio/uni-app'
	import {
		createFinancePlan,
		updateFinancePlan,
		getFinancePlanDetail
	} from '@/api/index.js'
	import {
		formatDate
	} from '@/utils/index.js'

	const isEdit = ref(false)
	const editId = ref(null)
	const submitting = ref(false)
	const initialAmountStr = ref('')
	const currentValueStr = ref('')
	const expectedRoiStr = ref('')

	const form = reactive({
		name: '',
		initialAmount: 0,
		currentValue: 0,
		expectedRoi: null,
		startDate: formatDate(new Date(), 'yyyy-MM-dd'),
		endDate: '',
		remark: ''
	})

	function onStartDateChange(e) {
		form.startDate = e.detail.value
	}

	function onEndDateChange(e) {
		form.endDate = e.detail.value
	}

	async function handleSubmit() {
		if (!form.name.trim()) {
			uni.showToast({
				title: '请输入计划名称',
				icon: 'none'
			});
			return
		}

		const initialAmount = parseFloat(initialAmountStr.value)
		if (isNaN(initialAmount) || initialAmount <= 0) {
			uni.showToast({
				title: '请输入有效的初始投入金额',
				icon: 'none'
			});
			return
		}

		const currentValue = parseFloat(currentValueStr.value)
		if (isNaN(currentValue) || currentValue < 0) {
			uni.showToast({
				title: '请输入有效的当前市值',
				icon: 'none'
			});
			return
		}

		if (!form.startDate) {
			uni.showToast({
				title: '请选择开始日期',
				icon: 'none'
			});
			return
		}

		submitting.value = true
		try {
			const data = {
				name: form.name.trim(),
				initialAmount,
				currentValue,
				startDate: form.startDate,
				remark: form.remark.trim()
			}
			if (form.endDate) data.endDate = form.endDate
			if (expectedRoiStr.value) data.expectedRoi = parseFloat(expectedRoiStr.value)

			if (isEdit.value) {
				await updateFinancePlan(editId.value, data)
				uni.showToast({
					title: '修改成功',
					icon: 'success'
				})
			} else {
				await createFinancePlan(data)
				uni.showToast({
					title: '创建成功',
					icon: 'success'
				})
			}
			setTimeout(() => {
				uni.navigateBack()
			}, 1000)
		} catch (err) {
			console.error(err)
		} finally {
			submitting.value = false
		}
	}

	onLoad((options) => {
		if (options && options.id) {
			isEdit.value = true
			editId.value = parseInt(options.id)
			loadDetail(options.id)
		}
	})

	async function loadDetail(id) {
		try {
			const res = await getFinancePlanDetail(id)
			if (res.data) {
				const d = res.data
				form.name = d.name
				form.startDate = d.startDate || ''
				form.endDate = d.endDate || ''
				form.remark = d.remark || ''
				initialAmountStr.value = String(d.initialAmount || '')
				currentValueStr.value = String(d.currentValue || '')
				if (d.expectedRoi != null) expectedRoiStr.value = String(d.expectedRoi)
			}
		} catch (err) {
			console.error(err)
		}
	}
</script>

<style lang="scss" scoped>
	.add-plan-page {
		min-height: 100vh;
		background: #F5F7FA;
	}

	.form-card {
		margin: 20rpx 24rpx;
		background: #FFFFFF;
		border-radius: 16rpx;
		padding: 0 28rpx;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.03);
	}

	.form-item {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 28rpx 0;
		border-bottom: 1rpx solid #F0F2F5;

		.form-label {
			font-size: 28rpx;
			color: #333;
			flex-shrink: 0;
			width: 180rpx;

			.required {
				color: #EF4444;
			}
		}

		.form-input {
			flex: 1;
			text-align: right;
			font-size: 28rpx;
			color: #333;
			height: 60rpx;
		}

		.form-input-row {
			display: flex;
			align-items: center;
			flex: 1;
			justify-content: flex-end;

			.short-input {
				flex: 1;
				text-align: right;
			}

			.input-suffix {
				font-size: 28rpx;
				color: #999;
				margin-left: 8rpx;
			}
		}

		.form-picker {
			font-size: 28rpx;
			color: #3B6FE8;
			text-align: right;
			flex: 1;
		}

		.form-textarea {
			flex: 1;
			text-align: right;
			font-size: 28rpx;
			color: #333;
			height: 120rpx;
			padding: 8rpx 0;
		}
	}

	.submit-area {
		padding: 40rpx 32rpx;
	}
</style>
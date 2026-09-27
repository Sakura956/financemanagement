<template>
	<view class="add-memo-page">
		<view class="form-card">
			<view class="form-item">
				<text class="form-label">标题 <text class="required">*</text></text>
				<input class="form-input" v-model="form.title" placeholder="请输入备忘录标题" placeholder-style="color:#C0C0C0"
					maxlength="100" />
			</view>

			<view class="form-item">
				<text class="form-label">内容</text>
				<textarea class="form-textarea" v-model="form.content" placeholder="输入详细内容（选填）"
					placeholder-style="color:#C0C0C0" maxlength="2000" />
			</view>

			<view class="form-item">
				<text class="form-label">提醒时间</text>
				<picker mode="multiSelector" :range="remindRange" :value="remindIndex" @change="onRemindChange">
					<text class="form-picker">{{ form.remindTime || '请选择（选填）' }}</text>
				</picker>
			</view>
		</view>

		<view class="submit-area">
			<button class="btn-primary" :loading="submitting" @click="handleSubmit">
				{{ isEdit ? '保存修改' : '创建备忘' }}
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
		createMemo,
		updateMemo,
		getMemoDetail
	} from '@/api/index.js'
	import {
		formatDate
	} from '@/utils/index.js'

	const isEdit = ref(false)
	const editId = ref(null)
	const submitting = ref(false)

	const form = reactive({
		title: '',
		content: '',
		remindTime: ''
	})

	// 提醒时间选择器（简化版：日期 + 时间）
	const remindIndex = ref([0, 0])
	const remindRange = ref([
		[],
		[]
	])

	// 生成日期选项
	function buildRemindRange() {
		const dates = []
		const times = ['09:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00']
		const today = new Date()
		for (let i = 0; i < 30; i++) {
			const d = new Date(today)
			d.setDate(d.getDate() + i)
			dates.push(formatDate(d, 'yyyy-MM-dd'))
		}
		remindRange.value = [dates, times]
	}
	buildRemindRange()

	function onRemindChange(e) {
		const [di, ti] = e.detail.value
		remindIndex.value = [di, ti]
		form.remindTime = remindRange.value[0][di] + ' ' + remindRange.value[1][ti] + ':00'
	}

	async function handleSubmit() {
		if (!form.title.trim()) {
			uni.showToast({
				title: '请输入标题',
				icon: 'none'
			})
			return
		}

		submitting.value = true
		try {
			const data = {
				title: form.title.trim(),
				content: form.content.trim()
			}
			if (form.remindTime) data.remindTime = form.remindTime

			if (isEdit.value) {
				await updateMemo(editId.value, data)
				uni.showToast({
					title: '修改成功',
					icon: 'success'
				})
			} else {
				await createMemo(data)
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
			const res = await getMemoDetail(id)
			if (res.data) {
				form.title = res.data.title || ''
				form.content = res.data.content || ''
				form.remindTime = res.data.remindTime || ''
			}
		} catch (err) {
			console.error(err)
		}
	}
</script>

<style lang="scss" scoped>
	.add-memo-page {
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
		padding: 28rpx 0;
		border-bottom: 1rpx solid #F0F2F5;

		.form-label {
			display: block;
			font-size: 28rpx;
			color: #333;
			margin-bottom: 16rpx;

			.required {
				color: #EF4444;
			}
		}

		.form-input {
			width: 100%;
			font-size: 28rpx;
			color: #333;
			height: 64rpx;
			background: #F9FAFB;
			border-radius: 10rpx;
			padding: 0 16rpx;
		}

		.form-textarea {
			width: 100%;
			font-size: 28rpx;
			color: #333;
			height: 200rpx;
			background: #F9FAFB;
			border-radius: 10rpx;
			padding: 16rpx;
		}

		.form-picker {
			font-size: 28rpx;
			color: #3B6FE8;
		}
	}

	.submit-area {
		padding: 40rpx 32rpx;
	}
</style>
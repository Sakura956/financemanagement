<template>
	<view class="profile-page">
		<view class="form-card">
			<view class="avatar-row" @click="chooseAvatar">
				<view class="avatar">
					<image v-if="authStore.avatarUrl" :src="authStore.avatarUrl" class="avatar-img" />
					<text v-else style="font-size: 64rpx;">👤</text>
				</view>
				<text class="avatar-hint">点击更换头像</text>
			</view>

			<view class="form-item">
				<text class="f-label">手机号</text>
				<text class="f-value">{{ authStore.phone }}</text>
			</view>

			<view class="form-item">
				<text class="f-label">昵称</text>
				<input class="f-input" v-model="form.nickname" placeholder="请输入昵称" placeholder-style="color:#C0C0C0"
					maxlength="20" />
			</view>
		</view>

		<view class="submit-area">
			<button class="btn-primary" :loading="submitting" @click="handleSave">保存修改</button>
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
		useAuthStore
	} from '@/store/auth.js'
	import {
		updateProfile
	} from '@/api/index.js'

	const authStore = useAuthStore()
	const submitting = ref(false)

	const form = reactive({
		nickname: authStore.userInfo?.nickname || ''
	})

	async function chooseAvatar() {
		uni.chooseImage({
			count: 1,
			sizeType: ['compressed'],
			sourceType: ['album', 'camera'],
			success: async (res) => {
				const tempFilePath = res.tempFilePaths[0]
				// 上传头像到后端
				uploadAvatarApi(tempFilePath)
			}
		})
	}

	async function handleSave() {
		if (!form.nickname.trim()) {
			uni.showToast({
				title: '昵称不能为空',
				icon: 'none'
			})
			return
		}

		submitting.value = true
		try {
			const data = {
				nickname: form.nickname.trim()
			}
			const res = await updateProfile(data)
			if (res.data) {
				authStore.updateUserInfo(res.data)
				uni.showToast({
					title: '修改成功',
					icon: 'success'
				})
				setTimeout(() => {
					uni.navigateBack()
				}, 1000)
			}
		} catch (err) {
			console.error(err)
		} finally {
			submitting.value = false
		}
	}
</script>

<style lang="scss" scoped>
	.profile-page {
		min-height: 100vh;
		background: #F5F7FA;
	}

	.avatar-row {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 48rpx 0 32rpx;

		.avatar {
			width: 140rpx;
			height: 140rpx;
			background: #EEF2FF;
			border-radius: 50%;
			display: flex;
			align-items: center;
			justify-content: center;
			margin-bottom: 16rpx;
			border: 4rpx dashed #C7D2FE;
		}

		.avatar-img {
			width: 100%;
			height: 100%;
			object-fit: cover;
		}

		.avatar-hint {
			font-size: 24rpx;
			color: #3B6FE8;
		}
	}

	.form-card {
		margin: 0 24rpx;
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

		&:last-child {
			border-bottom: none;
		}

		.f-label {
			font-size: 28rpx;
			color: #666;
			width: 120rpx;
			flex-shrink: 0;
		}

		.f-value {
			font-size: 28rpx;
			color: #999;
			text-align: right;
			flex: 1;
		}

		.f-input {
			flex: 1;
			text-align: right;
			font-size: 28rpx;
			color: #333;
			height: 60rpx;
		}
	}

	.submit-area {
		padding: 48rpx 32rpx;
	}
</style>
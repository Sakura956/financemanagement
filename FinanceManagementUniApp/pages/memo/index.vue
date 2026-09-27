<template>
	<view class="memo-page">
		<view class="filter-row">
			<view class="filter-tab" :class="{ active: filterStatus === '' }" @click="changeFilter('')">全部</view>
			<view class="filter-tab" :class="{ active: filterStatus === 0 }" @click="changeFilter(0)">未完成</view>
			<view class="filter-tab" :class="{ active: filterStatus === 1 }" @click="changeFilter(1)">已完成</view>
		</view>

		<scroll-view scroll-y class="memo-scroll" @scrolltolower="loadMore">
			<view v-if="list.length > 0">
				<view v-for="item in list" :key="item.id" class="memo-card">
					<view class="memo-header">
						<view class="memo-check" :class="{ checked: item.isCompleted === 1 }"
							@click.stop="handleToggle(item)">
							<text v-if="item.isCompleted === 1" style="color: #fff; font-size: 24rpx;">✓</text>
						</view>
						<view class="memo-title-wrap" @click="goEdit(item.id)">
							<text class="memo-title"
								:class="{ completed: item.isCompleted === 1 }">{{ item.title }}</text>
							<text v-if="item.content" class="memo-preview">{{ item.content }}</text>
						</view>
						<view class="memo-actions">
							<text class="action-icon" @click.stop="handleDelete(item)">🗑</text>
						</view>
					</view>
					<view v-if="item.remindTime" class="memo-footer">
						<text class="memo-remind">⏰ {{ item.remindTime }}</text>
					</view>
				</view>
				<view v-if="loading" class="loading-tip">加载中...</view>
				<view v-if="noMore && list.length > 0" class="loading-tip">— 没有更多了 —</view>
			</view>
			<view v-if="!loading && list.length === 0" class="empty-block">
				<text class="empty-icon">📝</text>
				<text class="empty-text">暂无备忘录</text>
			</view>
		</scroll-view>

		<view class="float-btn" @click="goAdd">
			<text style="font-size: 48rpx; color: #fff;">+</text>
		</view>
	</view>
</template>

<script setup>
	import {
		ref
	} from 'vue'
	import {
		onShow
	} from '@dcloudio/uni-app'
	import {
		getMemos,
		toggleMemo,
		deleteMemo
	} from '@/api/index.js'

	const filterStatus = ref('')
	const list = ref([])
	const page = ref(1)
	const loading = ref(false)
	const noMore = ref(false)

	function changeFilter(status) {
		filterStatus.value = status
		page.value = 1
		list.value = []
		loadData()
	}

	function goAdd() {
		uni.navigateTo({
			url: '/pages/memo/add'
		})
	}

	function goEdit(id) {
		uni.navigateTo({
			url: `/pages/memo/add?id=${id}`
		})
	}

	async function handleToggle(item) {
		try {
			await toggleMemo(item.id)
			item.isCompleted = item.isCompleted === 1 ? 0 : 1
			uni.showToast({
				title: item.isCompleted === 1 ? '已完成' : '已取消',
				icon: 'success'
			})
		} catch (err) {
			console.error(err)
		}
	}

	async function handleDelete(item) {
		uni.showModal({
			title: '确认删除',
			content: '确定要删除该备忘录吗？',
			success: async (res) => {
				if (res.confirm) {
					try {
						await deleteMemo(item.id)
						list.value = list.value.filter((m) => m.id !== item.id)
						uni.showToast({
							title: '删除成功',
							icon: 'success'
						})
					} catch (err) {
						console.error(err)
					}
				}
			}
		})
	}

	async function loadData() {
		if (loading.value) return
		loading.value = true
		try {
			const params = {
				page: page.value,
				size: 15
			}
			if (filterStatus.value !== '') params.isCompleted = filterStatus.value
			const res = await getMemos(params)
			if (res.data) {
				if (page.value === 1) {
					list.value = res.data.records || []
				} else {
					list.value = list.value.concat(res.data.records || [])
				}
				noMore.value = (res.data.records || []).length < 15
			}
		} catch (err) {
			console.error(err)
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
		loadData()
	})
</script>

<style lang="scss" scoped>
	.memo-page {
		min-height: 100vh;
		background: #F5F7FA;
		position: relative;
	}

	.filter-row {
		display: flex;
		padding: 16rpx 24rpx;
		gap: 16rpx;
		background: #FFF;
		border-bottom: 1rpx solid #F0F2F5;

		.filter-tab {
			font-size: 26rpx;
			padding: 10rpx 28rpx;
			border-radius: 24rpx;
			color: #666;
			background: #F5F7FA;

			&.active {
				background: #EEF2FF;
				color: #3B6FE8;
				font-weight: 500;
			}
		}
	}

	.memo-scroll {
		height: calc(100vh - 120rpx);
	}

	.memo-card {
		margin: 16rpx 24rpx;
		background: #FFFFFF;
		border-radius: 16rpx;
		padding: 24rpx;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
	}

	.memo-header {
		display: flex;
		align-items: flex-start;
		gap: 16rpx;

		.memo-check {
			width: 48rpx;
			height: 48rpx;
			border-radius: 50%;
			border: 3rpx solid #D1D5DB;
			display: flex;
			align-items: center;
			justify-content: center;
			flex-shrink: 0;
			margin-top: 2rpx;

			&.checked {
				background: #10B981;
				border-color: #10B981;
			}
		}

		.memo-title-wrap {
			flex: 1;
			overflow: hidden;

			.memo-title {
				display: block;
				font-size: 30rpx;
				font-weight: 500;
				color: #333;

				&.completed {
					color: #C0C0C0;
					text-decoration: line-through;
				}
			}

			.memo-preview {
				display: block;
				font-size: 26rpx;
				color: #999;
				margin-top: 8rpx;
				overflow: hidden;
				text-overflow: ellipsis;
				white-space: nowrap;
			}
		}

		.memo-actions {
			flex-shrink: 0;

			.action-icon {
				font-size: 32rpx;
				padding: 8rpx;
			}
		}
	}

	.memo-footer {
		margin-top: 16rpx;
		padding-top: 12rpx;
		border-top: 1rpx solid #F0F2F5;

		.memo-remind {
			font-size: 24rpx;
			color: #F59E0B;
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
</style>
<script>
	// Vue /uni-app 的规则：所有 .vue 文件的 script 必须导出一个组件配置对象，框架才能识别并运行它！
	//setup语法糖可以自动帮你导出了，所以不用写 export default
	export default {
		//小程序第一次启动时执行，且只执行一次！
		onLaunch() {
			console.log('财务管理小程序启动')
			// 检查登录状态
			const token = uni.getStorageSync('token')
			if (!token) {
				// 未登录则跳转到登录页
				uni.reLaunch({ url: '/pages/auth/login' })
			}
		},
		//小程序从后台切回前台时执行
		onShow() {
			console.log('App Show')
		},
		//小程序从前台切到后台时执行
		onHide() {
			console.log('App Hide')
		}
	}
</script>

<style lang="scss">
	/* 全局公共样式 */
	@import '@/uni.scss';

	page {
		background-color: #F5F7FA;
		font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
		font-size: 28rpx;
		color: #333333;
		box-sizing: border-box;
	}

	view,
	text,
	image,
	scroll-view,
	input,
	button {
		box-sizing: border-box;
	}

	/* 通用按钮 */
	.btn-primary {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 100%;
		height: 88rpx;
		background: linear-gradient(135deg, #4F8CFF, #3B6FE8);
		color: #FFFFFF;
		font-size: 32rpx;
		font-weight: 500;
		border-radius: 44rpx;
		border: none;

		&:active {
			opacity: 0.9;
		}
	}

	.btn-outline {
		display: flex;
		align-items: center;
		justify-content: center;
		height: 88rpx;
		background: #FFFFFF;
		color: #3B6FE8;
		font-size: 32rpx;
		font-weight: 500;
		border-radius: 44rpx;
		border: 2rpx solid #3B6FE8;
	}

	/* 通用卡片 */
	.card {
		background: #FFFFFF;
		border-radius: 16rpx;
		padding: 24rpx;
		margin: 0 24rpx 20rpx;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
	}

	/* 安全区域底部 */
	.safe-bottom {
		padding-bottom: constant(safe-area-inset-bottom);
		padding-bottom: env(safe-area-inset-bottom);
	}

	/* 文字颜色 */
	.text-income {
		color: #10B981;
	}

	.text-expense {
		color: #EF4444;
	}

	.text-primary {
		color: #3B6FE8;
	}

	.text-muted {
		color: #999999;
	}

	.text-bold {
		font-weight: 600;
	}

	/* Flex 布局 */
	.flex-center {
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.flex-between {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.flex-start {
		display: flex;
		align-items: center;
		justify-content: flex-start;
	}

	.flex-column {
		display: flex;
		flex-direction: column;
	}
</style>
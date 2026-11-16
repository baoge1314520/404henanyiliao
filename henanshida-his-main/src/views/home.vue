<template>
	<el-container class="shell">
		<!-- 侧边栏 -->
		<el-aside class="sidebar" :width="collapse ? '72px' : '232px'">
			<div class="side-logo" :class="{ mini: collapse }">
				<div class="badge"><el-icon :size="24"><FirstAidKit /></el-icon></div>
				<div v-show="!collapse" class="txt">
					<span class="cn">河南师大 HIS</span>
					<span class="en">医院信息系统</span>
				</div>
			</div>
			<div class="side-menu">
				<wy-menu :collapse="collapse" />
			</div>
			<div class="side-foot" v-show="!collapse">
				<span class="dot"></span> 系统运行中
			</div>
		</el-aside>

		<el-container class="main-wrap">
			<!-- 顶栏 -->
			<el-header class="topbar">
				<div class="tb-left">
					<el-icon class="fold" @click="collapse = !collapse">
						<Expand v-if="collapse" />
						<Fold v-else />
					</el-icon>
					<el-breadcrumb separator="/">
						<el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
						<el-breadcrumb-item v-if="route.meta.group">{{ route.meta.group }}</el-breadcrumb-item>
						<el-breadcrumb-item>{{ route.meta.title || '工作台' }}</el-breadcrumb-item>
					</el-breadcrumb>
					<el-tag type="warning" effect="plain" size="small" round style="margin-left: 10px">演示数据</el-tag>
				</div>
				<div class="tb-right">
					<el-tooltip content="刷新页面" placement="bottom">
						<el-icon class="tb-icon" @click="reload"><Refresh /></el-icon>
					</el-tooltip>
					<el-dropdown trigger="click" @command="onCommand">
						<div class="user-chip">
							<div class="avatar">{{ userStore.avatarText }}</div>
							<span class="name">{{ userStore.realName }}</span>
							<el-icon class="caret"><ArrowDown /></el-icon>
						</div>
						<template #dropdown>
							<el-dropdown-menu>
								<el-dropdown-item command="dashboard" :icon="Odometer">工作台</el-dropdown-item>
								<el-dropdown-item command="ai" :icon="ChatDotRound">天使宝宝ai</el-dropdown-item>
								<el-dropdown-item command="logout" divided :icon="SwitchButton">退出登录</el-dropdown-item>
							</el-dropdown-menu>
						</template>
					</el-dropdown>
				</div>
			</el-header>

			<!-- 内容 -->
			<el-main class="content">
				<router-view v-slot="{ Component }">
					<transition name="fade" mode="out-in">
						<component :is="Component" />
					</transition>
				</router-view>
			</el-main>
		</el-container>

		<!-- 全局悬浮「天使宝宝ai」 -->
		<AiAngelWidget />
	</el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Expand, Fold, Refresh, ArrowDown, Odometer, ChatDotRound, SwitchButton, FirstAidKit } from '@element-plus/icons-vue'
import WyMenu from '@@/menu'
import AiAngelWidget from '@@/angel/AiAngelWidget.vue'
import { useUserStore } from '@/stores/user'
import { useAiWidgetStore } from '@/stores/aiWidget'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const aiWidget = useAiWidgetStore()
const collapse = ref(false)

onMounted(() => {
	// 拉取最新用户信息（失败不阻塞页面）
	userStore.fetchInfo().catch(() => {})
})

function reload() {
	location.reload()
}

function onCommand(cmd) {
	if (cmd === 'logout') {
		ElMessageBox.confirm('确定退出登录吗？', '提示', {
			type: 'warning', confirmButtonText: '退出', cancelButtonText: '取消'
		}).then(async () => {
			await userStore.logout()
			ElMessage.success('已退出登录')
			router.replace('/login')
		}).catch(() => {})
	} else if (cmd === 'dashboard') {
		router.push('/dashboard')
	} else if (cmd === 'ai') {
		aiWidget.open()
	}
}
</script>

<style scoped lang="scss">
.shell {
	height: 100vh;
}

/* 侧边栏 */
.sidebar {
	background: linear-gradient(180deg, var(--his-side-top), var(--his-side-bottom));
	display: flex;
	flex-direction: column;
	transition: width .26s ease;
	box-shadow: 2px 0 16px rgba(15, 23, 42, .12);
	overflow: hidden;
}

.side-logo {
	display: flex;
	align-items: center;
	gap: 12px;
	height: 64px;
	padding: 0 18px;
	flex-shrink: 0;
	border-bottom: 1px solid rgba(255, 255, 255, .08);

	&.mini {
		justify-content: center;
		padding: 0;
	}

	.badge {
		width: 40px;
		height: 40px;
		border-radius: 11px;
		background: linear-gradient(135deg, #3b82f6, #2563eb);
		display: flex;
		align-items: center;
		justify-content: center;
		color: #fff;
		box-shadow: 0 6px 16px rgba(37, 99, 235, .45);
		flex-shrink: 0;
	}

	.txt {
		display: flex;
		flex-direction: column;
		white-space: nowrap;

		.cn {
			color: #fff;
			font-size: 16.5px;
			font-weight: 700;
			letter-spacing: .5px;
		}

		.en {
			color: #93a4c8;
			font-size: 11px;
			margin-top: 2px;
		}
	}
}

.side-menu {
	flex: 1;
	overflow-y: auto;
	overflow-x: hidden;
	padding-top: 6px;

	&::-webkit-scrollbar {
		width: 6px;
	}

	&::-webkit-scrollbar-thumb {
		background: rgba(255, 255, 255, .18);
	}
}

.side-foot {
	flex-shrink: 0;
	padding: 14px 20px;
	font-size: 12px;
	color: #8fa1c4;
	border-top: 1px solid rgba(255, 255, 255, .08);
	display: flex;
	align-items: center;
	gap: 8px;

	.dot {
		width: 7px;
		height: 7px;
		border-radius: 50%;
		background: #34d399;
		box-shadow: 0 0 0 3px rgba(52, 211, 153, .2);
	}
}

/* 顶栏 */
.main-wrap {
	background: var(--his-bg);
}

.topbar {
	height: 64px;
	background: #fff;
	border-bottom: 1px solid var(--his-border);
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 0 22px;
	box-shadow: 0 1px 3px rgba(15, 23, 42, .04);
}

.tb-left {
	display: flex;
	align-items: center;
	gap: 16px;

	.fold {
		font-size: 20px;
		color: #64748b;
		cursor: pointer;
		transition: color .2s;

		&:hover {
			color: var(--his-primary);
		}
	}
}

.tb-right {
	display: flex;
	align-items: center;
	gap: 18px;

	.tb-icon {
		font-size: 19px;
		color: #64748b;
		cursor: pointer;

		&:hover {
			color: var(--his-primary);
		}
	}
}

.user-chip {
	display: flex;
	align-items: center;
	gap: 9px;
	cursor: pointer;
	padding: 5px 10px 5px 5px;
	border-radius: 30px;
	transition: background .2s;

	&:hover {
		background: #f1f5f9;
	}

	.avatar {
		width: 34px;
		height: 34px;
		border-radius: 50%;
		background: linear-gradient(135deg, #2563eb, #60a5fa);
		color: #fff;
		font-size: 15px;
		font-weight: 600;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.name {
		font-size: 14px;
		font-weight: 500;
		color: #334155;
	}

	.caret {
		font-size: 12px;
		color: #94a3b8;
	}
}

/* 内容区 */
.content {
	padding: 0;
	overflow-y: auto;
}

.fade-enter-active,
.fade-leave-active {
	transition: opacity .2s ease;
}

.fade-enter-from,
.fade-leave-to {
	opacity: 0;
}
</style>
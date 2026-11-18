<template>
	<div class="d-home">
		<!-- 欢迎横幅 -->
		<div class="welcome">
			<div class="w-left">
				<div class="w-hi">{{ greeting }}，{{ userStore.realName }} 医生</div>
				<div class="w-sub">
					{{ todayText }}
					<template v-if="userStore.deptName"> · {{ userStore.deptName }}</template>
					<template v-if="userStore.titleName"> · {{ userStore.titleName }}</template>
					· 这里只展示您本人的接诊数据
				</div>
				<div class="w-actions">
					<el-button type="primary" round :icon="Notebook" @click="$router.push('/doc/today')">开始接诊</el-button>
					<el-button round :icon="Calendar" @click="$router.push('/doc/schedule')">我的排班</el-button>
				</div>
			</div>
			<div class="w-art">
				<el-icon :size="86"><FirstAidKit /></el-icon>
			</div>
		</div>

		<!-- 今日数据概览（只读） -->
		<div class="stat-row">
			<div v-for="s in statCards" :key="s.label" class="stat-card">
				<div class="s-num" :style="{ color: s.color }">{{ s.value }}</div>
				<div class="s-label">{{ s.label }}</div>
			</div>
		</div>

		<!-- 今日患者预览 -->
		<div class="panel">
			<div class="panel-head">
				<div class="panel-title">今日就诊患者</div>
				<el-button type="primary" link @click="$router.push('/doc/today')">
					去叫号 / 就诊 <el-icon><Right /></el-icon>
				</el-button>
			</div>
			<el-table v-loading="loading" :data="patients" stripe style="width: 100%">
				<el-table-column type="index" label="#" width="52" />
				<el-table-column prop="patientName" label="患者" min-width="100" />
				<el-table-column label="性别" width="70">
					<template #default="{ row }">{{ genderText(row.gender) }}</template>
				</el-table-column>
				<el-table-column prop="regNo" label="挂号单号" min-width="160" show-overflow-tooltip />
				<el-table-column label="时段" width="80">
					<template #default="{ row }">{{ row.regPeriod === '1' ? '上午' : '下午' }}</template>
				</el-table-column>
				<el-table-column label="状态" width="100">
					<template #default="{ row }">
						<el-tag v-if="row.status === '2'" type="success" effect="light" size="small">已就诊</el-tag>
						<el-tag v-else-if="row.callTime" type="warning" effect="light" size="small">已叫号</el-tag>
						<el-tag v-else type="info" effect="light" size="small">候诊中</el-tag>
					</template>
				</el-table-column>
				<el-table-column label="叫号时间" min-width="150">
					<template #default="{ row }">{{ formatTime(row.callTime) }}</template>
				</el-table-column>
				<template #empty>
					<el-empty description="今天暂无挂号患者" :image-size="70" />
				</template>
			</el-table>
		</div>
	</div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { FirstAidKit, Notebook, Calendar, Right } from '@element-plus/icons-vue'
import { _axios } from '@/util/axios'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const stats = ref({ todayTotal: 0, waiting: 0, called: 0, done: 0, shifts: 0 })
const patients = ref([])
const loading = ref(false)
let timer = null

const WEEK = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
const todayText = computed(() => {
	const d = new Date()
	return `${d.getFullYear()} 年 ${d.getMonth() + 1} 月 ${d.getDate()} 日 ${WEEK[d.getDay()]}`
})
const greeting = computed(() => {
	const h = new Date().getHours()
	return h < 6 ? '夜深了' : h < 12 ? '早上好' : h < 14 ? '中午好' : h < 18 ? '下午好' : '晚上好'
})

const statCards = computed(() => [
	{ label: '今日挂号', value: stats.value.todayTotal, color: '#2563eb' },
	{ label: '候诊中', value: stats.value.waiting, color: '#64748b' },
	{ label: '已叫号', value: stats.value.called, color: '#f59e0b' },
	{ label: '已就诊', value: stats.value.done, color: '#16a34a' },
	{ label: '今日排班时段', value: stats.value.shifts, color: '#0ea5e9' }
])

function genderText(g) {
	return g === '1' ? '男' : g === '2' ? '女' : '未知'
}
function formatTime(t) {
	if (!t) return '—'
	return String(t).replace('T', ' ').slice(0, 19)
}

async function load(silent = false) {
	if (!silent) loading.value = true
	try {
		const [s, p] = await Promise.all([
			_axios.request({ url: '/doctorAuth/stats', method: 'GET' }),
			_axios.request({ url: '/doctorAuth/today', method: 'GET' })
		])
		if (s.data.code === 200 && s.data.content) stats.value = s.data.content
		if (p.data.code === 200) patients.value = p.data.content || []
	} catch {
		// 静默刷新失败不打扰医生
	} finally {
		loading.value = false
	}
}

onMounted(() => {
	load()
	// 工作台数据 15 秒静默刷新
	timer = setInterval(() => load(true), 15000)
})
onBeforeUnmount(() => {
	if (timer) clearInterval(timer)
})
</script>

<style scoped lang="scss">
.d-home {
	padding: 22px 26px 40px;
	display: flex;
	flex-direction: column;
	gap: 16px;
}

/* 欢迎横幅 */
.welcome {
	display: flex;
	align-items: center;
	justify-content: space-between;
	border-radius: 18px;
	padding: 26px 30px;
	background: linear-gradient(120deg, #1d4ed8 0%, #2563eb 46%, #0ea5e9 100%);
	color: #fff;
	box-shadow: 0 14px 30px rgba(37, 99, 235, .28);
}

.w-hi {
	font-size: 24px;
	font-weight: 800;
	letter-spacing: .5px;
}

.w-sub {
	font-size: 13px;
	opacity: .85;
	margin: 8px 0 16px;
}

.w-actions {
	display: flex;
	gap: 10px;

	.el-button {
		border: none;
	}

	.el-button--primary {
		background: #fff;
		color: #1d4ed8;
		font-weight: 700;
	}

	.el-button:not(.el-button--primary) {
		background: rgba(255, 255, 255, .18);
		color: #fff;
	}
}

.w-art {
	opacity: .3;
}

/* 概览 */
.stat-row {
	display: grid;
	grid-template-columns: repeat(5, 1fr);
	gap: 14px;
}

.stat-card {
	background: #fff;
	border: 1px solid #e8eef6;
	border-radius: 15px;
	padding: 18px 20px;
	transition: all .2s;

	&:hover {
		transform: translateY(-2px);
		box-shadow: 0 10px 22px rgba(30, 64, 175, .1);
	}

	.s-num {
		font-size: 28px;
		font-weight: 800;
	}

	.s-label {
		font-size: 13px;
		color: #94a3b8;
		margin-top: 4px;
	}
}

/* 面板 */
.panel {
	background: #fff;
	border: 1px solid #e8eef6;
	border-radius: 15px;
	padding: 18px 20px;
}

.panel-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 12px;
}

.panel-title {
	font-size: 15.5px;
	font-weight: 800;
	color: #1e293b;
}

@media (max-width: 1100px) {
	.stat-row {
		grid-template-columns: repeat(3, 1fr);
	}
}

@media (max-width: 900px) {
	.stat-row {
		grid-template-columns: repeat(2, 1fr);
	}

	.w-art {
		display: none;
	}
}
</style>
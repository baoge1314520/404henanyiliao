<template>
	<div class="p-home">
		<!-- 欢迎横幅 -->
		<div class="welcome">
			<div class="w-left">
				<div class="w-hi">你好，{{ patientStore.name }}</div>
				<div class="w-sub">{{ todayText }} · 欢迎使用智慧院区患者服务门户，在线挂号、支付、查记录一站完成</div>
				<div class="w-actions">
					<el-button type="primary" round :icon="OfficeBuilding" @click="$router.push('/patient/hall')">立即挂号</el-button>
					<el-button round :icon="Tickets" @click="$router.push('/patient/records')">我的挂号</el-button>
				</div>
			</div>
			<div class="w-art">
				<el-icon :size="86"><FirstAidKit /></el-icon>
			</div>
		</div>

		<!-- 我的数据概览 -->
		<div class="stat-row">
			<div v-for="s in stats" :key="s.label" class="stat-card" @click="$router.push('/patient/records')">
				<div class="s-num" :style="{ color: s.color }">{{ s.value }}</div>
				<div class="s-label">{{ s.label }}</div>
			</div>
		</div>

		<div class="two-col">
			<!-- 就诊流程 -->
			<div class="panel">
				<div class="panel-title">就诊流程</div>
				<div class="steps">
					<div v-for="(st, i) in steps" :key="st.title" class="step">
						<div class="s-badge">{{ i + 1 }}</div>
						<div class="s-body">
							<div class="s-title">{{ st.title }}</div>
							<div class="s-desc">{{ st.desc }}</div>
						</div>
					</div>
				</div>
			</div>

			<!-- 科室导航 -->
			<div class="panel">
				<div class="panel-title">科室导航</div>
				<div v-loading="deptLoading" class="dept-chips">
					<button v-for="d in depts" :key="d.id" type="button" class="dept-chip"
						@click="$router.push({ path: '/patient/hall', query: { deptId: d.id } })">
						{{ d.deptName }}
					</button>
					<el-empty v-if="!deptLoading && depts.length === 0" description="暂无科室数据" :image-size="70" />
				</div>
			</div>
		</div>

		<!-- 温馨提示 -->
		<div class="notice">
			<el-icon class="n-icon"><InfoFilled /></el-icon>
			<div>
				<b>温馨提示（演示环境）</b>
				<p>登录密码为身份证号后 6 位；挂号后请在支付宝沙箱完成支付，支付成功后状态自动变为「待就诊」；就诊日期当天及之前可在「我的挂号」自助退号。本系统为教学实训演示平台，数据仅供学习使用。</p>
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { FirstAidKit, OfficeBuilding, Tickets, InfoFilled } from '@element-plus/icons-vue'
import { _axios } from '@/util/axios'
import { usePatientStore } from '@/stores/patient'

const patientStore = usePatientStore()
const records = ref([])
const depts = ref([])
const deptLoading = ref(false)

const WEEK = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
const todayText = computed(() => {
	const d = new Date()
	return `${d.getFullYear()} 年 ${d.getMonth() + 1} 月 ${d.getDate()} 日 ${WEEK[d.getDay()]}`
})

const stats = computed(() => [
	{ label: '累计挂号', value: records.value.length, color: '#2563eb' },
	{ label: '待支付', value: records.value.filter(r => r.status === '0').length, color: '#f59e0b' },
	{ label: '待就诊', value: records.value.filter(r => r.status === '1').length, color: '#0ea5e9' },
	{ label: '已就诊', value: records.value.filter(r => r.status === '2').length, color: '#16a34a' }
])

const steps = [
	{ title: '在线挂号', desc: '在挂号大厅选择日期、科室与医生，确认号源后提交' },
	{ title: '支付宝支付', desc: '跳转支付宝沙箱收银台完成挂号费支付' },
	{ title: '候诊叫号', desc: '支付成功后进入待就诊队列，按叫号顺序就诊' },
	{ title: '就诊取药', desc: '就诊完成后可在「我的挂号」查看就诊记录' }
]

async function loadRecords() {
	try {
		const { data } = await _axios.request({ url: '/patientAuth/registrations', method: 'GET' })
		if (data.code === 200) records.value = data.content?.records || []
	} catch {
		// 概览加载失败不阻塞首页
	}
}

async function loadDepts() {
	deptLoading.value = true
	try {
		const { data } = await _axios.request({ url: '/sysDept/listAll', method: 'GET' })
		if (data.code === 200) depts.value = data.content || []
	} catch {
		ElMessage.error('科室数据加载失败')
	} finally {
		deptLoading.value = false
	}
}

onMounted(() => {
	loadRecords()
	loadDepts()
})
</script>

<style scoped lang="scss">
.p-home {
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
	grid-template-columns: repeat(4, 1fr);
	gap: 14px;
}

.stat-card {
	background: #fff;
	border: 1px solid #e8eef6;
	border-radius: 15px;
	padding: 18px 20px;
	cursor: pointer;
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

/* 双栏 */
.two-col {
	display: grid;
	grid-template-columns: 1fr 1fr;
	gap: 14px;
}

.panel {
	background: #fff;
	border: 1px solid #e8eef6;
	border-radius: 15px;
	padding: 18px 20px;
}

.panel-title {
	font-size: 15.5px;
	font-weight: 800;
	color: #1e293b;
	margin-bottom: 14px;
}

.steps {
	display: flex;
	flex-direction: column;
	gap: 14px;
}

.step {
	display: flex;
	gap: 12px;

	.s-badge {
		width: 26px;
		height: 26px;
		border-radius: 50%;
		background: linear-gradient(135deg, #2563eb, #60a5fa);
		color: #fff;
		font-size: 13px;
		font-weight: 700;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.s-title {
		font-size: 14px;
		font-weight: 700;
		color: #334155;
	}

	.s-desc {
		font-size: 12.5px;
		color: #94a3b8;
		margin-top: 2px;
		line-height: 1.6;
	}
}

.dept-chips {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;
	min-height: 80px;
}

.dept-chip {
	border: 1px solid #dbe6f5;
	background: #f8fafc;
	color: #334155;
	font-size: 13px;
	border-radius: 30px;
	padding: 7px 15px;
	cursor: pointer;
	transition: all .18s;

	&:hover {
		background: #2563eb;
		border-color: #2563eb;
		color: #fff;
	}
}

/* 提示 */
.notice {
	display: flex;
	gap: 12px;
	background: #fffbeb;
	border: 1px solid #fde68a;
	border-radius: 15px;
	padding: 15px 18px;

	.n-icon {
		color: #f59e0b;
		font-size: 20px;
		margin-top: 2px;
	}

	b {
		font-size: 13.5px;
		color: #92400e;
	}

	p {
		font-size: 12.5px;
		color: #a16207;
		line-height: 1.8;
		margin-top: 4px;
	}
}

@media (max-width: 900px) {
	.stat-row {
		grid-template-columns: repeat(2, 1fr);
	}

	.two-col {
		grid-template-columns: 1fr;
	}

	.w-art {
		display: none;
	}
}
</style>
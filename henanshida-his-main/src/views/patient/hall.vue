<template>
	<div class="hall-page">
		<!-- 页头 -->
		<div class="page-head">
			<div>
				<h2>挂号大厅</h2>
				<p>选择日期与科室，查看医生出诊号源，在线挂号后跳转支付宝支付</p>
			</div>
			<el-button :icon="Refresh" :loading="loading" @click="loadAll()">刷新号源</el-button>
		</div>

		<!-- 7 天日期条 -->
		<div class="date-strip">
			<button v-for="d in days" :key="d.date" type="button" class="date-cell"
				:class="{ on: d.date === selectedDate, today: d.isToday }" @click="selectedDate = d.date">
				<span class="wk">{{ d.isToday ? '今天' : d.week }}</span>
				<span class="md">{{ d.md }}</span>
				<span class="left" :class="{ none: dayTotal(d.date) <= 0 }">
					{{ dayTotal(d.date) > 0 ? `余 ${dayTotal(d.date)}` : '无号' }}
				</span>
			</button>
		</div>

		<!-- 科室 / 时段筛选 + 汇总 -->
		<div class="filter-bar">
			<el-select v-model="deptId" placeholder="选择科室" clearable style="width: 200px">
				<el-option v-for="d in depts" :key="d.id" :label="d.deptName" :value="d.id" />
			</el-select>
			<div class="period-switch">
				<button type="button" :class="{ on: periodFilter === '' }" @click="periodFilter = ''">全部时段</button>
				<button type="button" :class="{ on: periodFilter === '1' }" @click="periodFilter = '1'">
					<el-icon><Sunny /></el-icon> 上午
				</button>
				<button type="button" :class="{ on: periodFilter === '2' }" @click="periodFilter = '2'">
					<el-icon><Moon /></el-icon> 下午
				</button>
			</div>
			<span class="sum">{{ deptLabel }} · {{ selectedDate }} 共 {{ groups.length }} 位医生 · {{ sessionCount }} 个出诊时段</span>
		</div>

		<!-- 医生卡片：一位医生一张卡，卡内按上午/下午分时段 -->
		<div v-loading="loading" class="card-wrap">
			<el-empty v-if="!loading && groups.length === 0"
				description="该日期暂无排班号源，请切换日期或关注排班管理" />
			<div v-else class="card-grid">
				<div v-for="g in groups" :key="g.doctorId" class="doc-card">
					<div class="doc-top">
						<el-avatar :size="46" :src="g.avatar || undefined" class="doc-avatar">
							{{ (g.doctorName || '医').charAt(0) }}
						</el-avatar>
						<div class="doc-id">
							<div class="doc-name">
								{{ g.doctorName }}
								<el-tag v-if="g.titleName" size="small" effect="plain" type="primary">{{ g.titleName }}</el-tag>
							</div>
							<div class="doc-dept">{{ g.deptName }}</div>
						</div>
						<div class="doc-fee">¥{{ g.regFee != null ? g.regFee : '--' }}</div>
					</div>
					<div class="doc-intro">{{ g.intro || '医生简介待完善' }}</div>
					<div class="sessions">
						<div v-for="se in g.sessions" :key="se.id" class="session"
							:class="{ am: se.regPeriod === '1', pm: se.regPeriod === '2' }">
							<span class="s-period">
								<el-icon><Sunny v-if="se.regPeriod === '1'" /><Moon v-else /></el-icon>
								{{ se.regPeriod === '1' ? '上午门诊' : '下午门诊' }}
							</span>
							<span class="s-quota" :class="{ none: se.remaining <= 0 }">
								余号 <b>{{ se.remaining }}</b> / {{ se.quota }}
							</span>
							<el-button size="small" type="primary" round :disabled="se.remaining <= 0 || booking"
								@click="book(se)">
								{{ se.remaining <= 0 ? '已约满' : '立即挂号' }}
							</el-button>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Sunny, Moon } from '@element-plus/icons-vue'
import { _axios } from '@/util/axios'
import { usePatientStore } from '@/stores/patient'

const route = useRoute()
const router = useRouter()
const patientStore = usePatientStore()

const WEEK = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
const loading = ref(false)
const booking = ref(false)
const depts = ref([])
const deptId = ref(route.query.deptId ? Number(route.query.deptId) : null)
// 时段筛选：'' 全部 / '1' 上午 / '2' 下午
const periodFilter = ref('')
// 每天全量号源（不含科室过滤），日期条余号与卡片共用这份数据
const dayData = ref({})

function fmt(d) {
	const m = String(d.getMonth() + 1).padStart(2, '0')
	const day = String(d.getDate()).padStart(2, '0')
	return `${d.getFullYear()}-${m}-${day}`
}

const days = computed(() => {
	const arr = []
	const base = new Date()
	for (let i = 0; i < 7; i++) {
		const d = new Date(base.getFullYear(), base.getMonth(), base.getDate() + i)
		arr.push({
			date: fmt(d),
			md: `${d.getMonth() + 1}/${d.getDate()}`,
			week: WEEK[d.getDay()],
			isToday: i === 0
		})
	}
	return arr
})
const selectedDate = ref(fmt(new Date()))

const deptLabel = computed(() => {
	if (!deptId.value) return '全部科室'
	return depts.value.find(d => d.id === deptId.value)?.deptName || '全部科室'
})

const filtered = computed(() => {
	const list = dayData.value[selectedDate.value] || []
	return deptId.value ? list.filter(s => s.deptId === deptId.value) : list
})

// 按医生分组：一位医生一张卡片，卡内时段按上午→下午排序
const groups = computed(() => {
	const map = new Map()
	for (const s of filtered.value) {
		if (periodFilter.value && s.regPeriod !== periodFilter.value) continue
		if (!map.has(s.doctorId)) {
			map.set(s.doctorId, {
				doctorId: s.doctorId,
				doctorName: s.doctorName,
				avatar: s.avatar,
				intro: s.intro,
				deptName: s.deptName,
				titleName: s.titleName,
				regFee: s.regFee,
				sessions: []
			})
		}
		map.get(s.doctorId).sessions.push(s)
	}
	const arr = [...map.values()]
	arr.forEach(g => g.sessions.sort((a, b) => String(a.regPeriod).localeCompare(String(b.regPeriod))))
	return arr
})
const sessionCount = computed(() => groups.value.reduce((n, g) => n + g.sessions.length, 0))

function dayTotal(date) {
	return (dayData.value[date] || []).reduce((sum, s) => sum + (s.remaining || 0), 0)
}

async function fetchDay(date) {
	const { data } = await _axios.request({ url: '/patientAuth/schedules', method: 'GET', params: { date } })
	if (data.code === 200 && Array.isArray(data.content)) return data.content
	return []
}

// silent 为静默刷新：不转圈、失败不弹错，保留当前号源展示
async function loadAll(silent = false) {
	if (!silent) loading.value = true
	try {
		const results = await Promise.all(days.value.map(d => fetchDay(d.date)))
		const map = {}
		days.value.forEach((d, i) => { map[d.date] = results[i] })
		dayData.value = map
	} catch {
		if (!silent) ElMessage.error('号源加载失败，请检查后端服务')
	} finally {
		if (!silent) loading.value = false
	}
}

async function loadDepts() {
	try {
		const { data } = await _axios.request({ url: '/sysDept/listAll', method: 'GET' })
		if (data.code === 200) depts.value = data.content || []
	} catch {
		// 科室加载失败不阻塞号源展示
	}
}

async function book(s) {
	const period = s.regPeriod === '1' ? '上午' : '下午'
	try {
		await ElMessageBox.confirm(
			`就诊人：${patientStore.name}` +
			`<br/>科室：${s.deptName}<br/>医生：${s.doctorName}（${s.titleName || '医师'}）` +
			`<br/>时间：${s.scheduleDate} ${period}<br/>挂号费：¥${s.regFee != null ? s.regFee : '--'}`,
			'确认挂号',
			{ dangerouslyUseHTMLString: true, confirmButtonText: '确认并支付', cancelButtonText: '再想想', type: 'info' }
		)
	} catch {
		return
	}
	booking.value = true
	try {
		const fd = new FormData()
		fd.append('regDate', s.scheduleDate)
		fd.append('regPeriod', s.regPeriod)
		fd.append('doctorId', s.doctorId)
		fd.append('deptId', s.deptId)
		const { data } = await _axios.request({ url: '/patientAuth/book', method: 'POST', data: fd })
		if (data.code !== 200) {
			ElMessage.error(data.message || '挂号失败')
			loadAll()
			return
		}
		const reg = data.content
		ElMessage.success('挂号成功，正在跳转支付宝…')
		// 发起支付宝沙箱支付；支付地址拿不到时保留待支付单，稍后可在「我的挂号」继续支付
		try {
			const payFd = new FormData()
			payFd.append('regId', reg.id)
			const { data: payData } = await _axios.request({ url: '/alipay/pay', method: 'POST', data: payFd })
			if (payData.code === 200 && payData.content) {
				window.open(payData.content, '_blank')
			} else {
				ElMessage.warning('支付地址生成失败，可在「我的挂号」中重新发起支付')
			}
		} catch {
			ElMessage.warning('支付服务暂不可用，可在「我的挂号」中重新发起支付')
		}
		router.push('/patient/records')
	} catch {
		ElMessage.error('网络异常，挂号失败')
	} finally {
		booking.value = false
	}
}

// 管理端新增医生会自动带出 7 天排班：这里静默轮询 + 回到窗口即刷新，挂号按钮自动出现
function onWake() {
	if (document.visibilityState === 'visible') {
		loadDepts()
		loadAll(true)
	}
}

let timer = null
onMounted(() => {
	loadDepts()
	loadAll()
	timer = setInterval(() => {
		if (document.visibilityState === 'visible') loadAll(true)
	}, 15000)
	window.addEventListener('focus', onWake)
	document.addEventListener('visibilitychange', onWake)
})

onBeforeUnmount(() => {
	clearInterval(timer)
	window.removeEventListener('focus', onWake)
	document.removeEventListener('visibilitychange', onWake)
})
</script>

<style scoped lang="scss">
.hall-page {
	padding: 22px 26px 40px;
}

.page-head {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	margin-bottom: 18px;

	h2 {
		font-size: 21px;
		font-weight: 800;
		color: #1e293b;
		margin-bottom: 5px;
	}

	p {
		font-size: 13px;
		color: #94a3b8;
	}
}

/* 日期条 */
.date-strip {
	display: grid;
	grid-template-columns: repeat(7, 1fr);
	gap: 10px;
	margin-bottom: 16px;
}

.date-cell {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 3px;
	padding: 12px 6px;
	border: 1.5px solid #e2e8f0;
	border-radius: 13px;
	background: #fff;
	cursor: pointer;
	transition: all .2s;

	.wk {
		font-size: 12.5px;
		color: #64748b;
		font-weight: 600;
	}

	.md {
		font-size: 16px;
		font-weight: 800;
		color: #1e293b;
	}

	.left {
		font-size: 11.5px;
		color: #16a34a;
		font-weight: 600;

		&.none {
			color: #b0bccd;
		}
	}

	&:hover {
		border-color: #bfdbfe;
		transform: translateY(-1px);
	}

	&.on {
		border-color: #2563eb;
		background: linear-gradient(135deg, #eff6ff, #dbeafe);
		box-shadow: 0 6px 16px rgba(37, 99, 235, .16);

		.wk,
		.md {
			color: #1d4ed8;
		}
	}

	&.today .wk {
		color: #2563eb;
	}
}

/* 筛选条 */
.filter-bar {
	display: flex;
	align-items: center;
	gap: 14px;
	margin-bottom: 16px;
	flex-wrap: wrap;

	.sum {
		font-size: 13.5px;
		color: #64748b;
		font-weight: 600;
	}
}

.period-switch {
	display: flex;
	background: #f1f5f9;
	border-radius: 10px;
	padding: 3px;

	button {
		display: flex;
		align-items: center;
		gap: 4px;
		border: none;
		background: transparent;
		border-radius: 8px;
		padding: 6px 13px;
		font-size: 13px;
		font-weight: 600;
		color: #64748b;
		cursor: pointer;
		transition: all .2s;

		&.on {
			background: #fff;
			color: #1d4ed8;
			box-shadow: 0 2px 8px rgba(30, 64, 175, .14);
		}
	}
}

/* 号源卡片 */
.card-wrap {
	min-height: 220px;
}

.card-grid {
	display: grid;
	grid-template-columns: repeat(auto-fill, minmax(min(320px, 100%), 1fr));
	gap: 14px;
}

.doc-card {
	background: #fff;
	border: 1px solid #e8eef6;
	border-radius: 15px;
	padding: 16px 18px;
	display: flex;
	flex-direction: column;
	gap: 10px;
	transition: all .2s;

	&:hover {
		box-shadow: 0 10px 24px rgba(30, 64, 175, .1);
		transform: translateY(-2px);
	}
}

.doc-top {
	display: flex;
	align-items: center;
	gap: 12px;
}

.doc-avatar {
	background: linear-gradient(135deg, #2563eb, #60a5fa);
	color: #fff;
	font-weight: 700;
	flex-shrink: 0;
}

.doc-id {
	flex: 1;
	min-width: 0;
}

.doc-name {
	font-size: 15.5px;
	font-weight: 800;
	color: #1e293b;
	display: flex;
	align-items: center;
	gap: 8px;
}

.doc-dept {
	font-size: 12.5px;
	color: #64748b;
	margin-top: 3px;
}

.doc-fee {
	font-size: 18px;
	font-weight: 800;
	color: #f59e0b;
	flex-shrink: 0;
}

.doc-intro {
	font-size: 12.5px;
	color: #94a3b8;
	line-height: 1.6;
	display: -webkit-box;
	-webkit-line-clamp: 2;
	-webkit-box-orient: vertical;
	overflow: hidden;
}

/* 卡内时段行：上午 / 下午 */
.sessions {
	display: flex;
	flex-direction: column;
	gap: 8px;
	border-top: 1px dashed #e8eef6;
	padding-top: 11px;
}

.session {
	display: flex;
	align-items: center;
	gap: 10px;
	flex-wrap: wrap;
	border-radius: 11px;
	padding: 8px 12px;

	&.am {
		background: #fff7ed;

		.s-period {
			color: #c2410c;
		}
	}

	&.pm {
		background: #eef2ff;

		.s-period {
			color: #4338ca;
		}
	}

	.s-period {
		display: flex;
		align-items: center;
		gap: 5px;
		font-size: 13px;
		font-weight: 700;
		width: 92px;
		flex-shrink: 0;
	}

	.s-quota {
		flex: 1;
		font-size: 12.5px;
		color: #64748b;

		b {
			font-size: 16px;
			color: #16a34a;
		}

		&.none b {
			color: #b0bccd;
		}
	}
}

@media (max-width: 900px) {
	.date-strip {
		grid-template-columns: repeat(4, 1fr);
	}
}
</style>
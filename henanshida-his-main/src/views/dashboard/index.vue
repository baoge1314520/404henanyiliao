<template>
	<div class="page dash">
		<!-- 欢迎横幅 -->
		<div class="banner">
			<div class="banner-text">
				<div class="hello">{{ greeting }}，{{ userStore.realName }} 👋</div>
				<div class="today">{{ todayText }} · 欢迎使用河南师大医院信息系统</div>
			</div>
			<div class="banner-art">
				<el-icon :size="120"><FirstAidKit /></el-icon>
			</div>
		</div>

		<!-- 统计卡片 -->
		<div class="stats">
			<div v-for="s in stats" :key="s.key" class="stat-card his-card" @click="router.push(s.path)">
				<div class="stat-icon" :style="{ background: s.bg, color: s.color }">
					<el-icon :size="26"><component :is="s.icon" /></el-icon>
				</div>
				<div class="stat-body">
					<div class="stat-num">{{ s.value === null ? '—' : s.value }}</div>
					<div class="stat-label">{{ s.label }}</div>
				</div>
				<el-icon class="stat-arrow"><ArrowRight /></el-icon>
			</div>
		</div>

		<!-- 图表行 -->
		<div class="charts">
			<div class="his-card panel chart-panel">
				<div class="panel-head">
					<span class="panel-title"><el-icon><TrendCharts /></el-icon> 近 7 日挂号趋势</span>
					<span class="panel-extra">单位：人次</span>
				</div>
				<svg :viewBox="`0 0 ${chart.W} ${chart.H}`" class="area-chart" preserveAspectRatio="none">
					<defs>
						<linearGradient id="areaFill" x1="0" y1="0" x2="0" y2="1">
							<stop offset="0" stop-color="#2563eb" stop-opacity=".30" />
							<stop offset="1" stop-color="#2563eb" stop-opacity="0" />
						</linearGradient>
					</defs>
					<g class="grid">
						<line v-for="(t, i) in chart.ticks" :key="'g' + i" :x1="chart.L" :x2="chart.W - chart.R" :y1="t.y" :y2="t.y" />
						<text v-for="(t, i) in chart.ticks" :key="'gt' + i" :x="chart.L - 8" :y="t.y + 4" text-anchor="end">{{ t.v }}</text>
					</g>
					<path :d="chart.area" fill="url(#areaFill)" />
					<path :d="chart.line" fill="none" stroke="#2563eb" stroke-width="2.5" stroke-linejoin="round" stroke-linecap="round" />
					<circle v-for="(p, i) in chart.pts" :key="'p' + i" :cx="p[0]" :cy="p[1]" r="3.5" fill="#fff" stroke="#2563eb" stroke-width="2" />
					<text v-for="(lb, i) in chart.labels" :key="'x' + i" :x="chart.pts[i][0]" :y="chart.H - 8" text-anchor="middle" class="xlab">{{ lb }}</text>
				</svg>
			</div>

			<div class="his-card panel chart-panel">
				<div class="panel-head">
					<span class="panel-title"><el-icon><PieChart /></el-icon> 挂号状态分布</span>
				</div>
				<div class="donut-wrap">
					<svg viewBox="0 0 200 200" class="donut">
						<g transform="rotate(-90 100 100)">
							<circle v-if="donut.total === 0" cx="100" cy="100" r="70" fill="none" stroke="#e2e8f0" stroke-width="26" />
							<circle v-for="(s, i) in donut.segs" :key="i" cx="100" cy="100" r="70" fill="none" :stroke="s.color"
								stroke-width="26" :stroke-dasharray="s.dash" :stroke-dashoffset="s.offset" />
						</g>
						<text x="100" y="96" text-anchor="middle" class="donut-num">{{ donut.total }}</text>
						<text x="100" y="118" text-anchor="middle" class="donut-lab">总挂号</text>
					</svg>
					<ul class="legend">
						<li v-for="(s, i) in donut.segs" :key="i">
							<span class="dot" :style="{ background: s.color }"></span>
							<span class="lg-label">{{ s.label }}</span>
							<b>{{ s.value }}</b>
							<span class="lg-pct">{{ s.pct }}%</span>
						</li>
					</ul>
				</div>
			</div>
		</div>

		<div class="dash-grid">
			<!-- 最新挂号 -->
			<div class="his-card panel">
				<div class="panel-head">
					<span class="panel-title"><el-icon><Tickets /></el-icon> 最新挂号记录</span>
					<el-button link type="primary" @click="router.push('/registration')">查看全部 <el-icon><ArrowRight /></el-icon></el-button>
				</div>
				<el-table :data="recent" v-loading="loadingRecent" empty-text="暂无挂号记录" style="width: 100%">
					<el-table-column prop="regNo" label="挂号单号" min-width="180" show-overflow-tooltip />
					<el-table-column prop="patientName" label="患者" width="100" />
					<el-table-column prop="deptName" label="科室" min-width="110" show-overflow-tooltip />
					<el-table-column prop="doctorName" label="医生" width="100" />
					<el-table-column label="状态" width="100" align="center">
						<template #default="{ row }">
							<el-tag :type="statusTag(row.status).type" effect="light" round size="small">{{ statusTag(row.status).text }}</el-tag>
						</template>
					</el-table-column>
				</el-table>
			</div>

			<!-- 快捷操作 -->
			<div class="his-card panel">
				<div class="panel-head">
					<span class="panel-title"><el-icon><Grid /></el-icon> 快捷操作</span>
				</div>
				<div class="quick">
					<div v-for="q in quicks" :key="q.label" class="quick-item" @click="onQuick(q)">
						<div class="q-icon" :style="{ background: q.bg, color: q.color }"><el-icon :size="20"><component :is="q.icon" /></el-icon></div>
						<span>{{ q.label }}</span>
					</div>
				</div>
				<div class="demo-note">
					<el-icon><InfoFilled /></el-icon>
					当前系统展示为实训演示数据，仅供教学使用。
				</div>
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { _axios } from '@/util/axios'
import { useUserStore } from '@/stores/user'
import { useAiWidgetStore } from '@/stores/aiWidget'
import { FirstAidKit, ArrowRight, Tickets, Grid, InfoFilled, TrendCharts, PieChart } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const aiWidget = useAiWidgetStore()

function onQuick(q) {
	if (q.action === 'ai') aiWidget.open()
	else router.push(q.path)
}

const now = new Date()
const greeting = computed(() => {
	const h = now.getHours()
	if (h < 6) return '夜深了'
	if (h < 12) return '早上好'
	if (h < 14) return '中午好'
	if (h < 18) return '下午好'
	return '晚上好'
})
const week = ['日', '一', '二', '三', '四', '五', '六']
const pad = n => String(n).padStart(2, '0')
const fmt = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
const todayStr = fmt(now)
const todayText = `${now.getFullYear()} 年 ${now.getMonth() + 1} 月 ${now.getDate()} 日 · 星期${week[now.getDay()]}`

const stats = reactive([
	{ key: 'dept', label: '科室总数', value: null, icon: 'OfficeBuilding', path: '/dept', bg: '#eef3fe', color: '#2563eb' },
	{ key: 'doctor', label: '医生总数', value: null, icon: 'User', path: '/doctor', bg: '#e9f9f1', color: '#10b981' },
	{ key: 'today', label: '今日挂号', value: null, icon: 'Tickets', path: '/registration', bg: '#fff4e6', color: '#f59e0b' },
	{ key: 'waiting', label: '待就诊', value: null, icon: 'Bell', path: '/queue', bg: '#fdeef0', color: '#ef4444' }
])

const quicks = [
	{ label: '挂号', icon: 'Tickets', path: '/registration', bg: '#eef3fe', color: '#2563eb' },
	{ label: '叫号', icon: 'Bell', path: '/queue', bg: '#fdeef0', color: '#ef4444' },
	{ label: '分诊', icon: 'Guide', path: '/triage', bg: '#e9f9f1', color: '#10b981' },
	{ label: '排班', icon: 'Calendar', path: '/schedule', bg: '#fff4e6', color: '#f59e0b' },
	{ label: '患者', icon: 'UserFilled', path: '/patient', bg: '#f0eefe', color: '#8b5cf6' },
	{ label: '天使宝宝ai', icon: 'MagicStick', action: 'ai', bg: '#f3eefc', color: '#8b5cf6' }
]

// 近 7 日日期
const days = []
for (let i = 6; i >= 0; i--) {
	const d = new Date(now)
	d.setDate(now.getDate() - i)
	days.push(fmt(d))
}
const trend = reactive({ labels: days.map(d => d.slice(5)), values: [0, 0, 0, 0, 0, 0, 0] })
const statusDist = reactive([
	{ label: '待就诊', value: 0, color: '#f59e0b' },
	{ label: '已就诊', value: 0, color: '#10b981' },
	{ label: '已取消', value: 0, color: '#94a3b8' }
])

// 面积图几何
const chart = computed(() => {
	const W = 560, H = 220, L = 40, R = 14, T = 16, B = 30
	const vals = trend.values
	const max = Math.max(1, ...vals)
	const iw = W - L - R, ih = H - T - B
	const pts = vals.map((v, i) => {
		const x = L + (vals.length === 1 ? 0 : i * (iw / (vals.length - 1)))
		const y = T + ih - (v / max) * ih
		return [Number(x.toFixed(1)), Number(y.toFixed(1))]
	})
	const line = pts.map((p, i) => (i ? 'L' : 'M') + p[0] + ',' + p[1]).join(' ')
	const area = `${line} L ${pts[pts.length - 1][0]},${T + ih} L ${pts[0][0]},${T + ih} Z`
	const ticks = [0, 1, 2, 3].map(i => {
		const v = Math.round((max * i) / 3)
		return { v, y: Number((T + ih - (v / max) * ih).toFixed(1)) }
	})
	return { W, H, L, R, line, area, pts, ticks, labels: trend.labels }
})

// 环形图几何
const donut = computed(() => {
	const total = statusDist.reduce((s, x) => s + x.value, 0)
	const C = 2 * Math.PI * 70
	let acc = 0
	const segs = statusDist.map(s => {
		const f = total ? s.value / total : 0
		const seg = { ...s, dash: `${(f * C).toFixed(2)} ${C.toFixed(2)}`, offset: Number((-acc * C).toFixed(2)), pct: total ? Math.round(f * 100) : 0 }
		acc += f
		return seg
	})
	return { total, segs }
})

const recent = ref([])
const loadingRecent = ref(false)

function statusTag(s) {
	return ({ 1: { text: '待就诊', type: 'warning' }, 2: { text: '已就诊', type: 'success' }, 3: { text: '已取消', type: 'info' } })[s] || { text: '未知', type: 'info' }
}

async function count(url, params) {
	try {
		const { data } = await _axios.request({ url, method: 'GET', params: { pageNo: 1, pageSize: 1, ...params } })
		return data.code === 200 ? (data.content?.totalRow ?? 0) : null
	} catch {
		return null
	}
}

onMounted(async () => {
	const [dept, doctor, today, waiting, s2, s3, ...trendCounts] = await Promise.all([
		count('/sysDept/list'),
		count('/sysUser/list'),
		count('/sysRegistration/list', { regDate: todayStr }),
		count('/sysRegistration/list', { status: '1' }),
		count('/sysRegistration/list', { status: '2' }),
		count('/sysRegistration/list', { status: '3' }),
		...days.map(d => count('/sysRegistration/list', { regDate: d }))
	])
	stats[0].value = dept
	stats[1].value = doctor
	stats[2].value = today
	stats[3].value = waiting
	statusDist[0].value = waiting ?? 0
	statusDist[1].value = s2 ?? 0
	statusDist[2].value = s3 ?? 0
	trend.values = trendCounts.map(v => v ?? 0)

	loadingRecent.value = true
	try {
		const { data } = await _axios.request({ url: '/sysRegistration/list', method: 'GET', params: { pageNo: 1, pageSize: 6 } })
		if (data.code === 200) recent.value = data.content?.records || []
	} catch {
		recent.value = []
	} finally {
		loadingRecent.value = false
	}
})
</script>

<style scoped lang="scss">
.dash {
	display: flex;
	flex-direction: column;
	gap: 18px;
}

.banner {
	position: relative;
	border-radius: var(--his-radius);
	padding: 26px 30px;
	color: #fff;
	background: linear-gradient(120deg, #1d4ed8, #2563eb 50%, #0ea5e9);
	overflow: hidden;
	display: flex;
	align-items: center;
	justify-content: space-between;
	box-shadow: 0 12px 30px rgba(37, 99, 235, .28);

	.hello {
		font-size: 24px;
		font-weight: 800;
		margin-bottom: 8px;
	}

	.today {
		font-size: 13.5px;
		opacity: .9;
	}

	.banner-art {
		opacity: .18;
		transform: rotate(-8deg);
	}
}

.stats {
	display: grid;
	grid-template-columns: repeat(4, 1fr);
	gap: 16px;
}

.stat-card {
	display: flex;
	align-items: center;
	gap: 16px;
	padding: 20px 22px;
	cursor: pointer;
	transition: transform .2s, box-shadow .2s;

	&:hover {
		transform: translateY(-3px);
		box-shadow: var(--his-shadow-hover);
	}

	.stat-icon {
		width: 54px;
		height: 54px;
		border-radius: 14px;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.stat-num {
		font-size: 28px;
		font-weight: 800;
		line-height: 1.1;
		font-variant-numeric: tabular-nums;
	}

	.stat-label {
		font-size: 13px;
		color: var(--his-text-3);
		margin-top: 4px;
	}

	.stat-arrow {
		margin-left: auto;
		color: #cbd5e1;
		font-size: 18px;
	}
}

.charts {
	display: grid;
	grid-template-columns: 1.7fr 1fr;
	gap: 16px;
}

.chart-panel {
	padding: 18px 20px;
}

.panel-extra {
	font-size: 12px;
	color: var(--his-text-3);
}

.area-chart {
	width: 100%;
	height: 220px;
	display: block;

	.grid line {
		stroke: #eef2f7;
		stroke-width: 1;
	}

	.grid text,
	.xlab {
		fill: #94a3b8;
		font-size: 10px;
	}
}

.donut-wrap {
	display: flex;
	align-items: center;
	gap: 18px;

	.donut {
		width: 168px;
		height: 168px;
		flex-shrink: 0;
	}

	.donut-num {
		font-size: 28px;
		font-weight: 800;
		fill: #1e293b;
	}

	.donut-lab {
		font-size: 11px;
		fill: #94a3b8;
	}

	.legend {
		flex: 1;
		list-style: none;
		margin: 0;
		padding: 0;
		display: flex;
		flex-direction: column;
		gap: 12px;

		li {
			display: flex;
			align-items: center;
			gap: 8px;
			font-size: 13px;
			color: var(--his-text-2);
		}

		.dot {
			width: 10px;
			height: 10px;
			border-radius: 3px;
			flex-shrink: 0;
		}

		.lg-label {
			flex: 1;
		}

		b {
			font-variant-numeric: tabular-nums;
			color: var(--his-text-1);
		}

		.lg-pct {
			width: 42px;
			text-align: right;
			color: var(--his-text-3);
			font-size: 12px;
		}
	}
}

.dash-grid {
	display: grid;
	grid-template-columns: 1.9fr 1fr;
	gap: 16px;
}

.panel {
	padding: 18px 20px;
}

.panel-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 14px;

	.panel-title {
		display: flex;
		align-items: center;
		gap: 8px;
		font-size: 15.5px;
		font-weight: 700;

		.el-icon {
			color: var(--his-primary);
		}
	}
}

.quick {
	display: grid;
	grid-template-columns: repeat(3, 1fr);
	gap: 12px;
}

.quick-item {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 9px;
	padding: 16px 6px;
	border-radius: 12px;
	background: #f8fafc;
	border: 1px solid var(--his-border);
	cursor: pointer;
	font-size: 13px;
	color: var(--his-text-2);
	transition: all .2s;

	&:hover {
		background: #fff;
		border-color: #c7d7fa;
		transform: translateY(-2px);
		box-shadow: var(--his-shadow);
	}

	.q-icon {
		width: 42px;
		height: 42px;
		border-radius: 12px;
		display: flex;
		align-items: center;
		justify-content: center;
	}
}

.demo-note {
	margin-top: 16px;
	display: flex;
	align-items: center;
	gap: 8px;
	font-size: 12px;
	color: var(--his-text-3);
	background: #f8fafc;
	border: 1px dashed var(--his-border);
	border-radius: 10px;
	padding: 10px 12px;
	line-height: 1.6;
}

@media (max-width: 1100px) {
	.charts {
		grid-template-columns: 1fr;
	}

	.stats {
		grid-template-columns: repeat(2, 1fr);
	}

	.dash-grid {
		grid-template-columns: 1fr;
	}
}
</style>

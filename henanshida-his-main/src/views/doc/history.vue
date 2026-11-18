<template>
	<div class="d-history">
		<div class="page-head">
			<div>
				<div class="page-title">历史记录</div>
				<div class="page-sub">本人名下全部挂号与就诊记录 · 过去的就诊和未来的预约都在此，就诊日期倒序</div>
			</div>
			<el-button :icon="Refresh" round :loading="loading" @click="refresh">刷新</el-button>
		</div>

		<!-- 全时段概览 -->
		<div class="stat-row">
			<div v-for="s in statCards" :key="s.label" class="stat-card">
				<div class="s-num" :style="{ color: s.color }">{{ s.value }}</div>
				<div class="s-label">{{ s.label }}</div>
			</div>
		</div>

		<!-- 筛选 -->
		<div class="panel filter-panel">
			<div class="tab-bar">
				<div v-for="t in tabs" :key="t.value" class="tab" :class="{ on: statusTab === t.value }" @click="switchTab(t.value)">
					{{ t.label }}<span class="t-num">{{ t.count }}</span>
				</div>
			</div>
			<div class="filter-row">
				<el-input v-model="keyword" class="f-keyword" placeholder="按患者姓名搜索" clearable :prefix-icon="Search"
					@keyup.enter="search" @clear="search" />
				<el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" range-separator="至"
					start-placeholder="开始日期" end-placeholder="结束日期" class="f-range" @change="onRange" />
				<el-radio-group v-model="quick" size="small" @change="onQuick">
					<el-radio-button value="all">全部时间</el-radio-button>
					<el-radio-button value="past">仅过去</el-radio-button>
					<el-radio-button value="future">仅未来</el-radio-button>
				</el-radio-group>
				<el-button type="primary" plain :icon="Search" @click="search">查询</el-button>
			</div>
		</div>

		<!-- 记录列表 -->
		<div class="panel">
			<el-table v-loading="loading" :data="records" stripe style="width: 100%">
				<el-table-column label="就诊时间" min-width="150">
					<template #default="{ row }">
						<div class="p-name">{{ row.regDate }}</div>
						<div class="p-meta">
							{{ row.regPeriod === '1' ? '上午' : '下午' }}
							<el-tag v-if="row.regDate > todayStr" type="primary" effect="plain" size="small" class="mini-tag" disable-transitions>未来</el-tag>
							<el-tag v-else-if="row.regDate === todayStr" type="warning" effect="plain" size="small" class="mini-tag" disable-transitions>今天</el-tag>
						</div>
					</template>
				</el-table-column>
				<el-table-column label="患者" min-width="130">
					<template #default="{ row }">
						<div class="p-name">{{ row.patientName }}</div>
						<div class="p-meta">{{ genderText(row.gender) }}<template v-if="ageText(row.birthday)"> · {{ ageText(row.birthday) }}</template></div>
					</template>
				</el-table-column>
				<el-table-column prop="regNo" label="挂号单号" min-width="170" show-overflow-tooltip />
				<el-table-column label="科室 / 职称" min-width="120">
					<template #default="{ row }">
						<div class="p-name">{{ row.deptName }}</div>
						<div class="p-meta">{{ row.titleName || '—' }}</div>
					</template>
				</el-table-column>
				<el-table-column label="类型" width="80">
					<template #default="{ row }">{{ row.regType === '2' ? '预约' : '普通' }}</template>
				</el-table-column>
				<el-table-column label="状态" width="90">
					<template #default="{ row }">
						<el-tag :type="statusTag(row.status)" effect="light" size="small">{{ statusText(row.status) }}</el-tag>
					</template>
				</el-table-column>
				<el-table-column label="挂号费" width="90">
					<template #default="{ row }">{{ row.regFee != null ? `¥${row.regFee}` : '—' }}</template>
				</el-table-column>
				<el-table-column label="用药 / 处方" min-width="160" show-overflow-tooltip>
					<template #default="{ row }">{{ row.medication || '—' }}</template>
				</el-table-column>
				<el-table-column label="医嘱 / 建议" min-width="160" show-overflow-tooltip>
					<template #default="{ row }">{{ row.advice || '—' }}</template>
				</el-table-column>
				<el-table-column label="操作" width="90" fixed="right">
					<template #default="{ row }">
						<el-button size="small" :icon="View" @click="openDetail(row)">详情</el-button>
					</template>
				</el-table-column>
				<template #empty>
					<el-empty description="当前筛选条件下暂无挂号记录" :image-size="80" />
				</template>
			</el-table>
			<div class="pager">
				<el-pagination background layout="total, sizes, prev, pager, next" :total="total"
					v-model:current-page="pageNo" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]"
					@current-change="load" @size-change="onSize" />
			</div>
		</div>

		<!-- 挂号 / 就诊详情 -->
		<el-dialog v-model="dialogVisible" title="挂号 / 就诊详情" width="640px" destroy-on-close>
			<div v-if="current" class="visit-head">
				<div class="v-name">{{ current.patientName }}</div>
				<div class="v-meta">
					{{ genderText(current.gender) }}<template v-if="ageText(current.birthday)"> · {{ ageText(current.birthday) }}</template>
					· {{ current.phone || '无联系电话' }} · 单号 {{ current.regNo }}
				</div>
			</div>
			<el-descriptions v-if="current" :column="2" border size="small">
				<el-descriptions-item label="就诊日期">{{ current.regDate }} {{ current.regPeriod === '1' ? '上午' : '下午' }}</el-descriptions-item>
				<el-descriptions-item label="状态">
					<el-tag :type="statusTag(current.status)" effect="light" size="small">{{ statusText(current.status) }}</el-tag>
				</el-descriptions-item>
				<el-descriptions-item label="科室">{{ current.deptName || '—' }}</el-descriptions-item>
				<el-descriptions-item label="职称">{{ current.titleName || '—' }}</el-descriptions-item>
				<el-descriptions-item label="挂号类型">{{ current.regType === '2' ? '预约挂号' : '普通挂号' }}</el-descriptions-item>
				<el-descriptions-item label="挂号费">{{ current.regFee != null ? `¥${current.regFee}` : '—' }}</el-descriptions-item>
				<el-descriptions-item label="叫号时间">{{ formatTime(current.callTime) }}</el-descriptions-item>
				<el-descriptions-item label="登记时间">{{ formatTime(current.createDate) }} {{ current.createTime || '' }}</el-descriptions-item>
				<el-descriptions-item label="患者备注" :span="2">{{ current.remark || '—' }}</el-descriptions-item>
			</el-descriptions>
			<div v-if="current" class="visit-block">
				<div class="vb-title">用药 / 处方</div>
				<div class="vb-body" :class="{ empty: !current.medication }">{{ current.medication || '未开具用药' }}</div>
			</div>
			<div v-if="current" class="visit-block">
				<div class="vb-title">医嘱 / 建议</div>
				<div class="vb-body" :class="{ empty: !current.advice }">{{ current.advice || '未填写医嘱' }}</div>
			</div>
			<template #footer>
				<el-button type="primary" @click="dialogVisible = false">关闭</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh, Search, View } from '@element-plus/icons-vue'
import { _axios } from '@/util/axios'

const records = ref([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const statusTab = ref('')
const keyword = ref('')
const range = ref(null)
const quick = ref('all')
const stats = ref({ total: 0, unpaid: 0, waiting: 0, done: 0, cancelled: 0, future: 0 })
const dialogVisible = ref(false)
const current = ref(null)

function fmt(d) {
	const m = String(d.getMonth() + 1).padStart(2, '0')
	const day = String(d.getDate()).padStart(2, '0')
	return `${d.getFullYear()}-${m}-${day}`
}
const todayStr = fmt(new Date())
const yesterdayStr = fmt(new Date(Date.now() - 86400000))

const statCards = computed(() => [
	{ label: '累计挂号', value: stats.value.total, color: '#2563eb' },
	{ label: '未来预约', value: stats.value.future, color: '#0ea5e9' },
	{ label: '待就诊', value: stats.value.waiting, color: '#f59e0b' },
	{ label: '已就诊', value: stats.value.done, color: '#16a34a' },
	{ label: '已退号', value: stats.value.cancelled, color: '#94a3b8' }
])

const tabs = computed(() => [
	{ label: '全部', value: '', count: stats.value.total },
	{ label: '待支付', value: '0', count: stats.value.unpaid },
	{ label: '待就诊', value: '1', count: stats.value.waiting },
	{ label: '已就诊', value: '2', count: stats.value.done },
	{ label: '已退号', value: '3', count: stats.value.cancelled }
])

// 日期范围：自定义区间优先，否则按快捷选项（过去 / 未来）
const dateFrom = computed(() => (range.value && range.value[0]) || (quick.value === 'future' ? todayStr : null))
const dateTo = computed(() => (range.value && range.value[1]) || (quick.value === 'past' ? yesterdayStr : null))

function genderText(g) {
	return g === '1' ? '男' : g === '2' ? '女' : '未知'
}
function ageText(birthday) {
	if (!birthday) return ''
	const b = new Date(String(birthday).slice(0, 10) + 'T00:00:00')
	if (isNaN(b.getTime())) return ''
	const now = new Date()
	let age = now.getFullYear() - b.getFullYear()
	if (now.getMonth() < b.getMonth() || (now.getMonth() === b.getMonth() && now.getDate() < b.getDate())) age--
	return age >= 0 && age < 150 ? `${age} 岁` : ''
}
function formatTime(t) {
	if (!t) return '—'
	return String(t).replace('T', ' ').slice(0, 19)
}
function statusText(s) {
	return s === '0' ? '待支付' : s === '1' ? '待就诊' : s === '2' ? '已就诊' : '已退号'
}
function statusTag(s) {
	return s === '0' ? 'warning' : s === '1' ? 'primary' : s === '2' ? 'success' : 'info'
}

async function load() {
	loading.value = true
	try {
		const params = { pageNo: pageNo.value, pageSize: pageSize.value }
		if (keyword.value.trim()) params.patientName = keyword.value.trim()
		if (statusTab.value) params.status = statusTab.value
		if (dateFrom.value) params.dateFrom = dateFrom.value
		if (dateTo.value) params.dateTo = dateTo.value
		const { data } = await _axios.request({ url: '/doctorAuth/history', method: 'GET', params })
		if (data.code === 200 && data.content) {
			records.value = data.content.records || []
			total.value = Number(data.content.totalRow || 0)
		}
	} catch {
		// 加载失败保留上次数据，不打断操作
	} finally {
		loading.value = false
	}
}

async function loadStats() {
	try {
		const { data } = await _axios.request({ url: '/doctorAuth/historyStats', method: 'GET' })
		if (data.code === 200 && data.content) stats.value = data.content
	} catch {
		// 统计失败不阻塞列表
	}
}

function refresh() {
	load()
	loadStats()
}
function search() {
	pageNo.value = 1
	load()
}
function switchTab(v) {
	statusTab.value = v
	pageNo.value = 1
	load()
}
function onQuick() {
	range.value = null
	pageNo.value = 1
	load()
}
function onRange() {
	quick.value = 'all'
	pageNo.value = 1
	load()
}
function onSize() {
	pageNo.value = 1
	load()
}
function openDetail(row) {
	current.value = row
	dialogVisible.value = true
}

onMounted(() => {
	load()
	loadStats()
})
</script>

<style scoped lang="scss">
.d-history {
	padding: 22px 26px 40px;
	display: flex;
	flex-direction: column;
	gap: 16px;
}

.page-head {
	display: flex;
	align-items: flex-end;
	justify-content: space-between;
	gap: 16px;
}

.page-title {
	font-size: 20px;
	font-weight: 800;
	color: #1e293b;
}

.page-sub {
	font-size: 12.5px;
	color: #94a3b8;
	margin-top: 5px;
}

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

	.s-num {
		font-size: 26px;
		font-weight: 800;
		line-height: 1.1;
	}

	.s-label {
		font-size: 12.5px;
		color: #94a3b8;
		margin-top: 6px;
	}
}

.panel {
	background: #fff;
	border: 1px solid #e8eef6;
	border-radius: 15px;
	padding: 14px 16px;
}

.filter-panel {
	display: flex;
	flex-direction: column;
	gap: 12px;
}

.tab-bar {
	display: flex;
	gap: 8px;
	flex-wrap: wrap;
}

.tab {
	padding: 7px 14px;
	border-radius: 9px;
	background: #f1f5f9;
	color: #64748b;
	font-size: 13px;
	cursor: pointer;
	transition: all .2s;
	display: flex;
	align-items: center;
	gap: 6px;

	&:hover {
		background: #e2e8f0;
	}

	&.on {
		background: linear-gradient(135deg, #2563eb, #3b82f6);
		color: #fff;
		font-weight: 600;
	}

	.t-num {
		font-size: 12px;
		opacity: .85;
	}
}

.filter-row {
	display: flex;
	align-items: center;
	gap: 10px;
	flex-wrap: wrap;

	.f-keyword {
		width: 200px;
	}

	.f-range {
		width: 260px;
	}
}

.p-name {
	font-weight: 700;
	color: #334155;
}

.p-meta {
	font-size: 12px;
	color: #94a3b8;
	margin-top: 2px;
	display: flex;
	align-items: center;
	gap: 6px;
}

.mini-tag {
	height: 18px;
	padding: 0 5px;
}

.pager {
	display: flex;
	justify-content: flex-end;
	margin-top: 14px;
}

.visit-head {
	background: #f8fafc;
	border: 1px solid #e8eef6;
	border-radius: 12px;
	padding: 13px 16px;
	margin-bottom: 16px;

	.v-name {
		font-size: 16px;
		font-weight: 800;
		color: #1e293b;
	}

	.v-meta {
		font-size: 12.5px;
		color: #64748b;
		margin-top: 4px;
	}
}

.visit-block {
	margin-top: 14px;

	.vb-title {
		font-size: 13px;
		font-weight: 700;
		color: #475569;
		margin-bottom: 6px;
	}

	.vb-body {
		background: #f8fafc;
		border: 1px solid #e8eef6;
		border-radius: 10px;
		padding: 10px 12px;
		font-size: 13px;
		color: #334155;
		white-space: pre-wrap;
		line-height: 1.7;

		&.empty {
			color: #94a3b8;
		}
	}
}

@media (max-width: 1100px) {
	.stat-row {
		grid-template-columns: repeat(3, 1fr);
	}
}
</style>
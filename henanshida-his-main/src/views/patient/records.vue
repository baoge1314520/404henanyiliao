<template>
	<div class="records-page">
		<!-- 页头 -->
		<div class="page-head">
			<div>
				<h2>我的挂号</h2>
				<p>查看本人在线预约与就诊记录，就诊前一天可自助退号</p>
			</div>
			<el-button :icon="Refresh" :loading="loading" @click="fetchList()">刷新</el-button>
		</div>

		<!-- 状态筛选 -->
		<div class="tab-bar">
			<button v-for="t in tabs" :key="t.key" type="button" class="tab"
				:class="{ on: tab === t.key }" @click="tab = t.key">
				{{ t.label }}
				<span class="cnt">{{ countOf(t.key) }}</span>
			</button>
		</div>

		<!-- 记录表 -->
		<div class="table-card" v-loading="loading">
			<el-table :data="shown" stripe style="width: 100%">
				<el-table-column prop="regNo" label="挂号单号" min-width="190" show-overflow-tooltip />
				<el-table-column prop="deptName" label="科室" min-width="100" />
				<el-table-column prop="doctorName" label="医生" min-width="90" />
				<el-table-column label="就诊时间" min-width="150">
					<template #default="{ row }">{{ row.regDate }} {{ row.regPeriod === '1' ? '上午' : '下午' }}</template>
				</el-table-column>
				<el-table-column label="状态" width="96" align="center">
					<template #default="{ row }">
						<el-tag :type="statusOf(row.status).type" effect="light" size="small">{{ statusOf(row.status).label }}</el-tag>
					</template>
				</el-table-column>
				<el-table-column label="支付" width="90" align="center">
					<template #default="{ row }">
						<el-tag v-if="row.status === '0'" type="warning" effect="plain" size="small">未支付</el-tag>
						<el-tag v-else-if="row.status === '1' || row.status === '2'" type="success" effect="plain" size="small">已支付</el-tag>
						<span v-else class="muted">—</span>
					</template>
				</el-table-column>
				<el-table-column label="诊断结果" min-width="100">
					<template #default><span class="muted">—</span></template>
				</el-table-column>
				<el-table-column label="医嘱" min-width="90">
					<template #default><span class="muted">—</span></template>
				</el-table-column>
				<el-table-column label="操作" width="150" align="center" fixed="right">
					<template #default="{ row }">
						<el-button v-if="row.status === '0'" type="primary" link size="small" @click="pay(row)">去支付</el-button>
						<el-button v-if="canCancel(row)" type="danger" link size="small" @click="cancel(row)">退号</el-button>
						<span v-if="row.status !== '0' && !canCancel(row)" class="muted">—</span>
					</template>
				</el-table-column>
				<template #empty>
					<el-empty description="暂无挂号记录，去挂号大厅看看吧" :image-size="90" />
				</template>
			</el-table>
			<div class="table-foot">共 {{ shown.length }} 条记录</div>
		</div>
	</div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { _axios } from '@/util/axios'

const router = useRouter()
const loading = ref(false)
const records = ref([])
const tab = ref('all')

const tabs = [
	{ key: 'all', label: '全部' },
	{ key: '0', label: '待支付' },
	{ key: '1', label: '待就诊' },
	{ key: '2', label: '已就诊' },
	{ key: '3', label: '已退号' }
]

const STATUS = {
	'0': { label: '待支付', type: 'warning' },
	'1': { label: '待就诊', type: 'primary' },
	'2': { label: '已就诊', type: 'success' },
	'3': { label: '已退号', type: 'info' }
}
function statusOf(s) {
	return STATUS[s] || { label: s || '未知', type: 'info' }
}

const shown = computed(() => tab.value === 'all' ? records.value : records.value.filter(r => r.status === tab.value))
function countOf(key) {
	return key === 'all' ? records.value.length : records.value.filter(r => r.status === key).length
}

function todayStr() {
	const d = new Date()
	return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
function canCancel(row) {
	return (row.status === '0' || row.status === '1') && String(row.regDate) >= todayStr()
}

async function fetchList(silent) {
	if (!silent) loading.value = true
	try {
		const { data } = await _axios.request({ url: '/patientAuth/registrations', method: 'GET' })
		if (data.code === 200) {
			records.value = data.content?.records || []
		} else if (!silent) {
			ElMessage.error(data.message || '加载失败')
		}
	} catch {
		if (!silent) ElMessage.error('网络异常，无法加载挂号记录')
	} finally {
		if (!silent) loading.value = false
	}
}

async function pay(row) {
	try {
		const fd = new FormData()
		fd.append('regId', row.id)
		const { data } = await _axios.request({ url: '/alipay/pay', method: 'POST', data: fd })
		if (data.code === 200 && data.content) {
			window.open(data.content, '_blank')
			ElMessage.success('已打开支付宝收银台，支付完成后本页自动刷新')
		} else {
			ElMessage.error(data.message || '支付地址生成失败')
		}
	} catch {
		ElMessage.error('网络异常，无法发起支付')
	}
}

async function cancel(row) {
	try {
		await ElMessageBox.confirm(
			`确定退掉 ${row.regDate} ${row.regPeriod === '1' ? '上午' : '下午'} ${row.deptName} ${row.doctorName} 的号吗？`,
			'自助退号', { type: 'warning', confirmButtonText: '确定退号', cancelButtonText: '取消' }
		)
	} catch {
		return
	}
	try {
		const fd = new FormData()
		fd.append('id', row.id)
		const { data } = await _axios.request({ url: '/patientAuth/cancel', method: 'POST', data: fd })
		if (data.code === 200) {
			ElMessage.success('退号成功')
			fetchList()
		} else {
			ElMessage.error(data.message || '退号失败')
		}
	} catch {
		ElMessage.error('网络异常，退号失败')
	}
}

// 支付在另一页签完成：回到本页自动静默刷新；存在待支付单时每 5 秒轮询，全部支付后自动停止
let pollTimer = null
function syncPolling() {
	const hasUnpaid = records.value.some(r => r.status === '0')
	if (hasUnpaid && !pollTimer) {
		pollTimer = setInterval(() => { if (!document.hidden) fetchList(true) }, 5000)
	} else if (!hasUnpaid && pollTimer) {
		clearInterval(pollTimer)
		pollTimer = null
	}
}
watch(records, syncPolling)
function onReturn() {
	if (!document.hidden) fetchList(true)
}

onMounted(() => {
	fetchList()
	window.addEventListener('focus', onReturn)
	document.addEventListener('visibilitychange', onReturn)
})
onBeforeUnmount(() => {
	window.removeEventListener('focus', onReturn)
	document.removeEventListener('visibilitychange', onReturn)
	if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped lang="scss">
.records-page {
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

.tab-bar {
	display: flex;
	gap: 8px;
	margin-bottom: 14px;
	flex-wrap: wrap;
}

.tab {
	display: flex;
	align-items: center;
	gap: 6px;
	border: 1.5px solid #e2e8f0;
	background: #fff;
	border-radius: 30px;
	padding: 7px 16px;
	font-size: 13.5px;
	font-weight: 600;
	color: #64748b;
	cursor: pointer;
	transition: all .2s;

	.cnt {
		font-size: 11.5px;
		background: #f1f5f9;
		border-radius: 20px;
		padding: 1px 8px;
		color: #94a3b8;
	}

	&:hover {
		border-color: #bfdbfe;
	}

	&.on {
		border-color: #2563eb;
		background: linear-gradient(135deg, #eff6ff, #dbeafe);
		color: #1d4ed8;

		.cnt {
			background: #2563eb;
			color: #fff;
		}
	}
}

.table-card {
	background: #fff;
	border: 1px solid #e8eef6;
	border-radius: 15px;
	padding: 8px 12px 12px;
}

.table-foot {
	padding: 10px 6px 2px;
	font-size: 12.5px;
	color: #94a3b8;
	text-align: right;
}

.muted {
	color: #c3cdd9;
}
</style>
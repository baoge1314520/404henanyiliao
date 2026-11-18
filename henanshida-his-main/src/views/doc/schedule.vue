<template>
	<div class="d-schedule">
		<div class="page-head">
			<div>
				<div class="page-title">我的排班</div>
				<div class="page-sub">今天起的出诊安排，由管理员在排班管理中维护；已约与余号实时同步挂号数据</div>
			</div>
			<el-button :icon="Refresh" round :loading="loading" @click="load">刷新</el-button>
		</div>

		<div class="panel">
			<el-table v-loading="loading" :data="schedules" stripe style="width: 100%"
				:row-class-name="rowClass">
				<el-table-column label="日期" min-width="150">
					<template #default="{ row }">
						<span class="date-txt">{{ row.scheduleDate }}</span>
						<el-tag v-if="isToday(row.scheduleDate)" type="primary" effect="dark" size="small" round style="margin-left: 8px">今天</el-tag>
					</template>
				</el-table-column>
				<el-table-column label="星期" width="80">
					<template #default="{ row }">{{ weekText(row.scheduleDate) }}</template>
				</el-table-column>
				<el-table-column label="时段" width="90">
					<template #default="{ row }">
						<el-tag :type="row.regPeriod === '1' ? 'warning' : 'primary'" effect="light" size="small">
							{{ row.regPeriod === '1' ? '上午' : '下午' }}
						</el-tag>
					</template>
				</el-table-column>
				<el-table-column prop="deptName" label="科室" min-width="130" />
				<el-table-column prop="quota" label="号源数" width="90" align="center" />
				<el-table-column prop="booked" label="已约" width="90" align="center" />
				<el-table-column label="余号" width="110" align="center">
					<template #default="{ row }">
						<span :class="['left-num', { full: row.remaining === 0 }]">{{ row.remaining }}</span>
					</template>
				</el-table-column>
				<el-table-column label="状态" width="100">
					<template #default="{ row }">
						<el-tag v-if="row.enabled" type="success" effect="light" size="small">出诊</el-tag>
						<el-tag v-else type="info" effect="light" size="small">停诊</el-tag>
					</template>
				</el-table-column>
				<template #empty>
					<el-empty description="近期暂无排班，请联系管理员安排" :image-size="80" />
				</template>
			</el-table>
		</div>
	</div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { _axios } from '@/util/axios'

const schedules = ref([])
const loading = ref(false)

const WEEK = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

function localDateStr(d) {
	const m = String(d.getMonth() + 1).padStart(2, '0')
	const day = String(d.getDate()).padStart(2, '0')
	return `${d.getFullYear()}-${m}-${day}`
}
function isToday(dateStr) {
	return String(dateStr).slice(0, 10) === localDateStr(new Date())
}
function weekText(dateStr) {
	const d = new Date(String(dateStr).slice(0, 10) + 'T00:00:00')
	return isNaN(d.getTime()) ? '' : WEEK[d.getDay()]
}
function rowClass({ row }) {
	return isToday(row.scheduleDate) ? 'today-row' : ''
}

async function load() {
	loading.value = true
	try {
		const { data } = await _axios.request({ url: '/doctorAuth/schedules', method: 'GET' })
		if (data.code === 200) schedules.value = data.content || []
	} catch {
		// 加载失败保留旧数据
	} finally {
		loading.value = false
	}
}

onMounted(load)
</script>

<style scoped lang="scss">
.d-schedule {
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

.panel {
	background: #fff;
	border: 1px solid #e8eef6;
	border-radius: 15px;
	padding: 14px 16px;
}

.date-txt {
	font-weight: 600;
	color: #334155;
}

.left-num {
	font-weight: 800;
	color: #16a34a;

	&.full {
		color: #ef4444;
	}
}

:deep(.today-row) {
	background: #eff6ff !important;

	td {
		background: #eff6ff !important;
	}
}
</style>
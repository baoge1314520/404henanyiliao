<template>
	<div class="page">
		<div class="page-header">
			<div class="ph-left">
				<div class="ph-icon"><el-icon><Guide /></el-icon></div>
				<div>
					<h2>分诊管理</h2>
					<div class="ph-sub">调整患者就诊科室、医生与职称，挂号费随职称自动更新</div>
				</div>
			</div>
		</div>

		<div class="his-card search-bar">
			<el-input v-model="filters.patientName" placeholder="请输入患者姓名" clearable @keyup.enter="search" @clear="search" />
			<el-select v-model="filters.status" placeholder="挂号状态" clearable @change="search">
				<el-option label="待就诊" value="1" />
				<el-option label="已就诊" value="2" />
				<el-option label="已取消" value="3" />
			</el-select>
			<el-button type="primary" :icon="Search" @click="search">查询</el-button>
			<el-button :icon="RefreshLeft" @click="reset">重置</el-button>
		</div>

		<div class="his-card table-card" v-loading="loading">
			<el-table :data="records" stripe style="width: 100%" empty-text="暂无可分诊记录">
				<el-table-column prop="regNo" label="挂号单号" min-width="185" show-overflow-tooltip />
				<el-table-column prop="patientName" label="患者" width="95" />
				<el-table-column prop="deptName" label="当前科室" min-width="110" show-overflow-tooltip />
				<el-table-column prop="doctorName" label="当前医生" width="95" />
				<el-table-column prop="titleName" label="当前职称" min-width="105" show-overflow-tooltip />
				<el-table-column label="挂号费" width="95" align="right">
					<template #default="{ row }"><span class="money">¥{{ money(row.regFee) }}</span></template>
				</el-table-column>
				<el-table-column label="状态" width="95" align="center">
					<template #default="{ row }">
						<el-tag :type="statusTag(row.status).type" effect="light" round size="small">{{ statusTag(row.status).text }}</el-tag>
					</template>
				</el-table-column>
				<el-table-column label="操作" width="120" fixed="right" align="center">
					<template #default="{ row }">
						<el-button link type="primary" :icon="Sort" @click="openTriage(row)">分诊</el-button>
					</template>
				</el-table-column>
			</el-table>
			<div class="pager">
				<el-pagination background layout="total, sizes, prev, pager, next, jumper"
					:total="totalRow" :page-sizes="[10, 20, 50]"
					v-model:current-page="pageNo" v-model:page-size="pageSize"
					@current-change="fetchList" @size-change="onSize" />
			</div>
		</div>

		<!-- 分诊弹窗 -->
		<el-dialog v-model="visible" title="分诊调整" width="560px" :close-on-click-modal="false" destroy-on-close>
			<div class="triage-patient">
				<el-icon><UserFilled /></el-icon>
				<span>{{ current.patientName }}</span>
				<span class="muted">{{ current.regNo }}</span>
			</div>
			<el-form ref="formRef" :model="form" :rules="rules" label-width="92px" v-loading="saving">
				<el-form-item label="就诊科室" prop="deptId">
					<el-select v-model="form.deptId" placeholder="请选择科室" filterable clearable style="width: 100%" @change="onDeptChange">
						<el-option v-for="d in depts" :key="d.id" :label="d.deptName" :value="d.id" />
					</el-select>
				</el-form-item>
				<el-form-item label="就诊医生" prop="doctorId">
					<el-select v-model="form.doctorId" placeholder="请选择医生" filterable clearable style="width: 100%">
						<el-option v-for="d in doctorList" :key="d.id" :label="d.realName" :value="d.id" />
					</el-select>
				</el-form-item>
				<el-form-item label="挂号职称">
					<el-select v-model="form.titleId" placeholder="留空则按医生职称自动匹配" filterable clearable style="width: 100%">
						<el-option v-for="t in titles" :key="t.id" :label="`${t.titleName}（¥${money(t.regFee)}）`" :value="t.id" />
					</el-select>
				</el-form-item>
				<el-alert title="提示：未选择职称时，系统按所选医生的职称自动匹配并更新挂号费。" type="info" :closable="false" show-icon />
			</el-form>
			<template #footer>
				<el-button @click="visible = false">取消</el-button>
				<el-button type="primary" :loading="saving" @click="submit">确认分诊</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { _axios } from '@/util/axios'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft, Guide, Sort, UserFilled } from '@element-plus/icons-vue'

const loading = ref(false)
const records = ref([])
const totalRow = ref(0)
const pageNo = ref(1)
const pageSize = ref(10)
const filters = reactive({ patientName: '', status: '1' })

const depts = ref([])
const doctors = ref([])
const titles = ref([])

const visible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const current = reactive({ id: null, patientName: '', regNo: '' })
const form = reactive({ deptId: '', doctorId: '', titleId: '' })
const rules = {
	doctorId: [{ required: true, message: '请选择就诊医生', trigger: 'change' }]
}

const doctorList = computed(() => {
	if (!form.deptId) return doctors.value
	return doctors.value.filter(d => d.deptId === form.deptId)
})

function money(v) { const n = Number(v); return isNaN(n) ? '0.00' : n.toFixed(2) }
function statusTag(s) {
	return ({ 1: { text: '待就诊', type: 'warning' }, 2: { text: '已就诊', type: 'success' }, 3: { text: '已取消', type: 'info' } })[s] || { text: '未知', type: 'info' }
}

function fetchList() {
	loading.value = true
	const params = { pageNo: pageNo.value, pageSize: pageSize.value }
	for (const k in filters) if (filters[k]) params[k] = filters[k]
	_axios.request({ url: '/sysRegistration/list', method: 'GET', params }).then(({ data }) => {
		if (data.code === 200) { records.value = data.content?.records || []; totalRow.value = data.content?.totalRow || 0 }
		else ElMessage.error(data.message || '查询失败')
	}).catch(() => ElMessage.error('网络异常，无法加载数据')).finally(() => { loading.value = false })
}
function search() { pageNo.value = 1; fetchList() }
function reset() { filters.patientName = ''; filters.status = '1'; pageNo.value = 1; fetchList() }
function onSize() { pageNo.value = 1; fetchList() }

async function loadBase() {
	try {
		const [d, u, t] = await Promise.all([
			_axios.request({ url: '/sysDept/listAll', method: 'GET' }),
			_axios.request({ url: '/sysUser/list', method: 'GET', params: { pageNo: 1, pageSize: 500 } }),
			_axios.request({ url: '/sysTitle/listAll', method: 'GET' })
		])
		depts.value = d.data.content || []
		doctors.value = u.data.content?.records || []
		titles.value = t.data.content || []
	} catch { /* 忽略 */ }
}

function openTriage(row) {
	current.id = row.id
	current.patientName = row.patientName
	current.regNo = row.regNo
	form.deptId = row.deptId || ''
	form.doctorId = row.doctorId || ''
	form.titleId = row.titleId || ''
	visible.value = true
}
function onDeptChange() { form.doctorId = '' }

function submit() {
	formRef.value.validate(valid => {
		if (!valid) return
		saving.value = true
		const fd = new FormData()
		fd.append('id', current.id)
		if (form.deptId) fd.append('deptId', form.deptId)
		if (form.doctorId) fd.append('doctorId', form.doctorId)
		if (form.titleId) fd.append('titleId', form.titleId)
		_axios.request({ url: '/sysRegistration/triage', method: 'POST', data: fd }).then(({ data }) => {
			if (data.code === 200) { ElMessage.success(data.message || '分诊成功'); visible.value = false; fetchList() }
			else ElMessage.error(data.message || '分诊失败')
		}).catch(() => ElMessage.error('网络异常，分诊失败')).finally(() => { saving.value = false })
	})
}

onMounted(() => { loadBase(); fetchList() })
</script>

<style scoped lang="scss">
.triage-patient {
	display: flex;
	align-items: center;
	gap: 10px;
	padding: 12px 16px;
	margin-bottom: 18px;
	background: var(--his-primary-soft);
	border-radius: 10px;
	font-weight: 600;
	color: var(--his-primary-dark);

	.el-icon {
		font-size: 18px;
	}

	.muted {
		font-weight: 400;
		font-size: 12.5px;
		margin-left: auto;
	}
}
</style>
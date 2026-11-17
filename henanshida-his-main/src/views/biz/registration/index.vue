<template>
	<div class="page">
		<div class="page-header">
			<div class="ph-left">
				<div class="ph-icon"><el-icon><Tickets /></el-icon></div>
				<div>
					<h2>挂号管理</h2>
					<div class="ph-sub">患者挂号登记，身份证自动识别老患者，挂号费按医生职称快照</div>
				</div>
			</div>
			<el-button type="primary" :icon="Plus" round @click="openReg">挂号登记</el-button>
		</div>

		<!-- 搜索 -->
		<div class="his-card search-bar">
			<el-input v-model="filters.patientName" placeholder="请输入患者姓名" clearable @keyup.enter="search" @clear="search" />
			<el-date-picker v-model="filters.regDate" type="date" value-format="YYYY-MM-DD" placeholder="就诊日期" @change="search" />
			<el-select v-model="filters.status" placeholder="挂号状态" clearable @change="search">
				<el-option label="待支付" value="0" />
				<el-option label="待就诊" value="1" />
				<el-option label="已就诊" value="2" />
				<el-option label="已取消" value="3" />
			</el-select>
			<el-button type="primary" :icon="Search" @click="search">查询</el-button>
			<el-button :icon="RefreshLeft" @click="reset">重置</el-button>
		</div>

		<!-- 表格 -->
		<div class="his-card table-card" v-loading="loading">
			<el-table :data="records" stripe style="width: 100%" empty-text="暂无挂号记录">
				<el-table-column prop="regNo" label="挂号单号" min-width="185" show-overflow-tooltip />
				<el-table-column prop="patientName" label="患者" width="95" />
				<el-table-column prop="deptName" label="科室" min-width="110" show-overflow-tooltip />
				<el-table-column prop="doctorName" label="医生" width="95" />
				<el-table-column prop="titleName" label="职称" min-width="105" show-overflow-tooltip />
				<el-table-column label="挂号费" width="95" align="right">
					<template #default="{ row }"><span class="money">¥{{ money(row.regFee) }}</span></template>
				</el-table-column>
				<el-table-column prop="regDate" label="就诊日期" width="120" />
				<el-table-column label="时段" width="80" align="center">
					<template #default="{ row }">{{ REG_PERIOD[row.regPeriod] || '—' }}</template>
				</el-table-column>
				<el-table-column label="状态" width="95" align="center">
					<template #default="{ row }">
						<el-tag :type="statusTag(row.status).type" effect="light" round size="small">{{ statusTag(row.status).text }}</el-tag>
					</template>
				</el-table-column>
				<el-table-column label="操作" width="170" fixed="right" align="center">
					<template #default="{ row }">
						<template v-if="row.status === '0'">
							<el-button link type="primary" :icon="CreditCard" @click="pay(row)">去支付</el-button>
							<el-button link type="danger" :icon="CircleClose" @click="cancel(row)">取消</el-button>
						</template>
						<template v-else-if="row.status === '1'">
							<el-button link type="success" :icon="CircleCheck" @click="finish(row)">就诊</el-button>
							<el-button link type="danger" :icon="CircleClose" @click="cancel(row)">取消</el-button>
						</template>
						<span v-else class="muted">—</span>
					</template>
				</el-table-column>
			</el-table>
			<div class="pager">
				<el-pagination background layout="total, sizes, prev, pager, next, jumper"
					:total="totalRow" :page-sizes="[10, 20, 50]"
					v-model:current-page="pageNo" v-model:page-size="pageSize"
					@current-change="() => fetchList()" @size-change="onSize" />
			</div>
		</div>

		<!-- 挂号弹窗 -->
		<el-dialog v-model="regVisible" title="挂号登记" width="700px" :close-on-click-modal="false" destroy-on-close>
			<el-form ref="regFormRef" :model="regForm" :rules="regRules" label-width="92px" v-loading="saving">
				<div class="sec-title"><el-icon><UserFilled /></el-icon> 患者信息</div>
				<el-row :gutter="18">
					<el-col :span="12">
						<el-form-item label="身份证号" prop="idCard">
							<el-input v-model="regForm.idCard" placeholder="输入后点击识别" clearable>
								<template #append>
									<el-button :icon="Search" :loading="identifying" @click="identify">识别</el-button>
								</template>
							</el-input>
						</el-form-item>
					</el-col>
					<el-col :span="12">
						<el-form-item label="患者姓名" prop="patientName">
							<el-input v-model="regForm.patientName" placeholder="请输入患者姓名" clearable />
						</el-form-item>
					</el-col>
					<el-col :span="12">
						<el-form-item label="性别">
							<el-select v-model="regForm.gender" style="width: 100%">
								<el-option label="男" value="1" /><el-option label="女" value="2" /><el-option label="未知" value="0" />
							</el-select>
						</el-form-item>
					</el-col>
					<el-col :span="12">
						<el-form-item label="出生日期">
							<el-date-picker v-model="regForm.birthday" type="date" value-format="YYYY-MM-DD" placeholder="出生日期" style="width: 100%" />
						</el-form-item>
					</el-col>
					<el-col :span="12">
						<el-form-item label="联系电话">
							<el-input v-model="regForm.phone" placeholder="联系电话" clearable />
						</el-form-item>
					</el-col>
					<el-col :span="12">
						<el-form-item label="家庭住址">
							<el-input v-model="regForm.address" placeholder="家庭住址" clearable />
						</el-form-item>
					</el-col>
				</el-row>

				<div class="sec-title"><el-icon><FirstAidKit /></el-icon> 挂号信息</div>
				<el-row :gutter="18">
					<el-col :span="12">
						<el-form-item label="就诊科室" prop="deptId">
							<el-select v-model="regForm.deptId" placeholder="请选择科室" filterable clearable style="width: 100%" @change="onDeptChange">
								<el-option v-for="d in depts" :key="d.id" :label="d.deptName" :value="d.id" />
							</el-select>
						</el-form-item>
					</el-col>
					<el-col :span="12">
						<el-form-item label="就诊医生" prop="doctorId">
							<el-select v-model="regForm.doctorId" placeholder="请选择医生" filterable clearable style="width: 100%">
								<el-option v-for="d in doctorList" :key="d.id" :label="d.realName" :value="d.id" />
							</el-select>
							<div v-if="regForm.deptId && doctorList.length === 0" class="no-doc-hint">该科室暂无启用医生，请先在「医生管理」中为其添加医生</div>
						</el-form-item>
					</el-col>
					<el-col :span="12">
						<el-form-item label="就诊日期" prop="regDate">
							<el-date-picker v-model="regForm.regDate" type="date" value-format="YYYY-MM-DD" placeholder="就诊日期" style="width: 100%" />
						</el-form-item>
					</el-col>
					<el-col :span="12">
						<el-form-item label="就诊时段">
							<el-select v-model="regForm.regPeriod" style="width: 100%">
								<el-option label="上午" value="1" /><el-option label="下午" value="2" />
							</el-select>
						</el-form-item>
					</el-col>
					<el-col :span="12">
						<el-form-item label="挂号类型">
							<el-select v-model="regForm.regType" style="width: 100%">
								<el-option label="普通挂号" value="1" /><el-option label="预约挂号" value="2" />
							</el-select>
						</el-form-item>
					</el-col>
					<el-col :span="12">
						<el-form-item label="备注">
							<el-input v-model="regForm.remark" placeholder="备注" clearable />
						</el-form-item>
					</el-col>
				</el-row>
				<el-alert v-if="feeHint" :title="feeHint" type="info" :closable="false" show-icon style="margin-top: 4px" />
			</el-form>
			<template #footer>
				<el-button @click="regVisible = false">取消</el-button>
				<el-button type="primary" :loading="saving" @click="submitReg">确认挂号</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { _axios } from '@/util/axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshLeft, CircleCheck, CircleClose, UserFilled, FirstAidKit, Tickets, CreditCard } from '@element-plus/icons-vue'
import { REG_PERIOD } from '@/util/options'

const loading = ref(false)
const records = ref([])
const totalRow = ref(0)
const pageNo = ref(1)
const pageSize = ref(10)
const filters = reactive({ patientName: '', regDate: '', status: '' })

const depts = ref([])
const doctors = ref([])
const titles = ref([])

const regVisible = ref(false)
const saving = ref(false)
const identifying = ref(false)
const regFormRef = ref(null)
const todayStr = (() => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}` })()
const regForm = reactive({
	idCard: '', patientName: '', gender: '1', birthday: '', phone: '', address: '',
	deptId: '', doctorId: '', regDate: todayStr, regPeriod: '1', regType: '1', remark: ''
})
const regRules = {
	idCard: [{ required: true, message: '请输入身份证号（患者档案唯一标识）', trigger: 'blur' }],
	patientName: [{ required: true, message: '请输入患者姓名', trigger: 'blur' }],
	doctorId: [{ required: true, message: '请选择就诊医生', trigger: 'change' }],
	regDate: [{ required: true, message: '请选择就诊日期', trigger: 'change' }]
}

const doctorList = computed(() => {
	const on = doctors.value.filter(d => d.enabled !== false)
	if (!regForm.deptId) return on
	return on.filter(d => d.deptId === regForm.deptId)
})

const feeHint = computed(() => {
	const doc = doctors.value.find(d => d.id === regForm.doctorId)
	if (!doc) return ''
	const t = titles.value.find(t => t.id === doc.titleId)
	return t ? `该医生职称：${t.titleName}，挂号费 ¥${Number(t.regFee).toFixed(2)}（以医生档案为准）` : ''
})

function money(v) { const n = Number(v); return isNaN(n) ? '0.00' : n.toFixed(2) }
function statusTag(s) {
	return ({ 0: { text: '待支付', type: 'danger' }, 1: { text: '待就诊', type: 'warning' }, 2: { text: '已就诊', type: 'success' }, 3: { text: '已取消', type: 'info' } })[s] || { text: '未知', type: 'info' }
}

function fetchList(silent) {
	if (!silent) loading.value = true
	const params = { pageNo: pageNo.value, pageSize: pageSize.value }
	for (const k in filters) if (filters[k]) params[k] = filters[k]
	_axios.request({ url: '/sysRegistration/list', method: 'GET', params }).then(({ data }) => {
		if (data.code === 200) {
			records.value = data.content?.records || []
			totalRow.value = data.content?.totalRow || 0
		} else if (!silent) ElMessage.error(data.message || '查询失败')
	}).catch(() => { if (!silent) ElMessage.error('网络异常，无法加载数据') }).finally(() => { if (!silent) loading.value = false })
}
function search() { pageNo.value = 1; fetchList() }
function reset() { filters.patientName = ''; filters.regDate = ''; filters.status = ''; pageNo.value = 1; fetchList() }
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
	} catch { /* 基础数据加载失败不阻塞页面 */ }
}

function openReg() {
	Object.assign(regForm, {
		idCard: '', patientName: '', gender: '1', birthday: '', phone: '', address: '',
		deptId: '', doctorId: '', regDate: todayStr, regPeriod: '1', regType: '1', remark: ''
	})
	regVisible.value = true
}

function onDeptChange() { regForm.doctorId = '' }

function identify() {
	if (!regForm.idCard) { ElMessage.warning('请先输入身份证号'); return }
	identifying.value = true
	_axios.request({ url: '/sysRegistration/patient', method: 'GET', params: { idCard: regForm.idCard } }).then(({ data }) => {
		if (data.code === 200 && data.content) {
			const p = data.content
			regForm.patientName = p.patientName || ''
			regForm.gender = p.gender || '1'
			regForm.birthday = p.birthday || ''
			regForm.phone = p.phone || ''
			regForm.address = p.address || ''
			ElMessage.success('已识别老患者，自动回填档案')
		} else {
			ElMessage.info('未找到该身份证患者，将新建患者档案')
		}
	}).catch(() => ElMessage.error('识别失败')).finally(() => { identifying.value = false })
}

function submitReg() {
	regFormRef.value.validate(valid => {
		if (!valid) return
		saving.value = true
		const fd = new FormData()
		const keys = ['idCard', 'patientName', 'gender', 'birthday', 'phone', 'address', 'deptId', 'doctorId', 'regDate', 'regPeriod', 'regType', 'remark']
		for (const k of keys) {
			const v = regForm[k]
			if (v !== '' && v !== null && v !== undefined) fd.append(k, v)
		}
		_axios.request({ url: '/sysRegistration/add', method: 'POST', data: fd }).then(({ data }) => {
			if (data.code === 200) {
				ElMessage.success(data.message || '挂号成功')
				regVisible.value = false
				fetchList()
				// 挂号成功后拿记录 id 发起支付宝电脑网站支付，跳转沙箱收银台
				const regId = data.content && data.content.id
				if (regId) goAlipay(regId)
			} else ElMessage.error(data.message || '挂号失败')
		}).catch(() => ElMessage.error('网络异常，挂号失败')).finally(() => { saving.value = false })
	})
}

// 请求后端生成支付宝收银台 URL 并在新标签打开；支付失败不影响已完成的挂号
function goAlipay(regId) {
	const fd = new FormData()
	fd.append('regId', regId)
	_axios.request({ url: '/alipay/pay', method: 'POST', data: fd }).then(({ data }) => {
		if (data.code === 200 && data.content) {
			ElMessage.info('正在跳转支付宝收银台…')
			window.open(data.content, '_blank')
		} else {
			ElMessage.warning('挂号已完成，但生成支付地址失败：' + (data.message || '请检查后端 alipay 沙箱密钥配置'))
		}
	}).catch(() => ElMessage.warning('挂号已完成，但支付请求失败，请确认后端已重启并填好支付宝配置'))
}

function act(row, url, action) {
	ElMessageBox.confirm(`确定${action}患者「${row.patientName}」的挂号吗？`, '提示', {
		type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
	}).then(() => {
		const fd = new FormData(); fd.append('id', row.id)
		_axios.request({ url, method: 'POST', data: fd }).then(({ data }) => {
			if (data.code === 200) { ElMessage.success(data.message || `${action}成功`); fetchList() }
			else ElMessage.error(data.message || `${action}失败`)
		}).catch(() => ElMessage.error('网络异常，操作失败'))
	}).catch(() => {})
}
function finish(row) { act(row, '/sysRegistration/finish', '完成就诊') }
function cancel(row) { act(row, '/sysRegistration/cancel', '取消挂号') }
// 待支付单子重新打开支付宝收银台（沙箱偶发 502 时可在此重试）
function pay(row) { goAlipay(row.id) }

// 支付在别的标签完成：回到本页（focus/可见）立即静默刷新；仍有待支付单时轻量轮询兜底
let pollTimer = null
function syncPolling() {
	const hasUnpaid = records.value.some(r => r.status === '0')
	if (hasUnpaid && !pollTimer) {
		pollTimer = setInterval(() => { if (!document.hidden) fetchList(true) }, 5000)
	} else if (!hasUnpaid && pollTimer) {
		clearInterval(pollTimer); pollTimer = null
	}
}
watch(records, syncPolling)
function onReturn() { if (!document.hidden) fetchList(true) }

onMounted(() => {
	loadBase(); fetchList()
	window.addEventListener('focus', onReturn)
	document.addEventListener('visibilitychange', onReturn)
})
onBeforeUnmount(() => {
	window.removeEventListener('focus', onReturn)
	document.removeEventListener('visibilitychange', onReturn)
	if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
})
</script>

<style scoped lang="scss">
.sec-title {
	display: flex;
	align-items: center;
	gap: 8px;
	font-size: 14px;
	font-weight: 700;
	color: var(--his-primary-dark);
	margin: 6px 0 16px;
	padding-left: 10px;
	border-left: 3px solid var(--his-primary);

	.el-icon {
		font-size: 16px;
	}
}

.no-doc-hint {
	font-size: 12px;
	color: var(--el-color-danger);
	line-height: 1.5;
	margin-top: 4px;
}
</style>
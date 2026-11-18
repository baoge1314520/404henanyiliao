<template>
	<div class="d-today">
		<div class="page-head">
			<div>
				<div class="page-title">今日就诊</div>
				<div class="page-sub">{{ todayText }} · 候诊 {{ waitingCount }} 人 · 已叫号 {{ calledCount }} 人 · 已就诊 {{ doneCount }} 人</div>
			</div>
			<el-button :icon="Refresh" round :loading="loading" @click="load">刷新</el-button>
		</div>

		<div class="panel">
			<el-table v-loading="loading" :data="patients" stripe style="width: 100%">
				<el-table-column type="index" label="#" width="52" />
				<el-table-column label="患者" min-width="150">
					<template #default="{ row }">
						<div class="p-name">{{ row.patientName }}</div>
						<div class="p-meta">{{ genderText(row.gender) }}<template v-if="ageText(row.birthday)"> · {{ ageText(row.birthday) }}</template></div>
					</template>
				</el-table-column>
				<el-table-column prop="phone" label="联系电话" min-width="120" />
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
				<el-table-column label="操作" width="210" fixed="right">
					<template #default="{ row }">
						<template v-if="row.status === '1'">
							<el-button size="small" :icon="Bell" :loading="busyId === row.id" @click="call(row)">叫号</el-button>
							<el-button size="small" type="primary" :icon="EditPen" @click="openVisit(row)">就诊</el-button>
						</template>
						<el-button v-else size="small" type="success" plain :icon="EditPen" @click="openVisit(row)">查看 / 修改医嘱</el-button>
					</template>
				</el-table-column>
				<template #empty>
					<el-empty description="今天暂无挂号患者，可在「我的排班」查看出诊安排" :image-size="80" />
				</template>
			</el-table>
		</div>

		<!-- 就诊 / 医嘱弹窗 -->
		<el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" :close-on-click-modal="false" destroy-on-close>
			<div v-if="current" class="visit-head">
				<div class="v-name">{{ current.patientName }}</div>
				<div class="v-meta">
					{{ genderText(current.gender) }}<template v-if="ageText(current.birthday)"> · {{ ageText(current.birthday) }}</template>
					· {{ current.phone || '无联系电话' }} · 单号 {{ current.regNo }}
				</div>
				<div v-if="current.remark" class="v-remark">患者备注：{{ current.remark }}</div>
			</div>
			<el-form label-position="top">
				<el-form-item label="用药 / 处方">
					<el-input v-model="visitForm.medication" type="textarea" :rows="4" maxlength="1000" show-word-limit
						placeholder="例如：阿莫西林胶囊 0.5g 口服 每日3次 × 5天（演示数据，仅供学习）" />
				</el-form-item>
				<el-form-item label="医嘱 / 建议">
					<el-input v-model="visitForm.advice" type="textarea" :rows="4" maxlength="1000" show-word-limit
						placeholder="例如：多饮水，清淡饮食，三天后复诊（演示数据，仅供学习）" />
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="dialogVisible = false">取消</el-button>
				<el-button type="primary" :loading="saving" @click="save">
					{{ current && current.status === '2' ? '保存修改' : '完成就诊' }}
				</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Bell, EditPen } from '@element-plus/icons-vue'
import { _axios } from '@/util/axios'

const patients = ref([])
const loading = ref(false)
const busyId = ref(null)
const dialogVisible = ref(false)
const saving = ref(false)
const current = ref(null)
const visitForm = reactive({ medication: '', advice: '' })
let timer = null

const WEEK = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
const todayText = computed(() => {
	const d = new Date()
	return `${d.getFullYear()} 年 ${d.getMonth() + 1} 月 ${d.getDate()} 日 ${WEEK[d.getDay()]}`
})
const waitingCount = computed(() => patients.value.filter(p => p.status === '1' && !p.callTime).length)
const calledCount = computed(() => patients.value.filter(p => p.status === '1' && p.callTime).length)
const doneCount = computed(() => patients.value.filter(p => p.status === '2').length)
const dialogTitle = computed(() => current.value?.status === '2' ? '查看 / 修改医嘱' : '就诊 · 填写用药与建议')

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

async function load(silent = false) {
	if (!silent) loading.value = true
	try {
		const { data } = await _axios.request({ url: '/doctorAuth/today', method: 'GET' })
		if (data.code === 200) patients.value = data.content || []
	} catch {
		// 静默刷新失败不打扰医生
	} finally {
		loading.value = false
	}
}

async function call(row) {
	busyId.value = row.id
	try {
		const formData = new FormData()
		formData.append('id', row.id)
		const { data } = await _axios.request({ url: '/doctorAuth/call', method: 'POST', data: formData })
		if (data.code === 200) {
			ElMessage.success(`已呼叫 ${row.patientName}，请准备接诊`)
			await load(true)
		} else {
			ElMessage.error(data.message || '叫号失败')
		}
	} catch {
		ElMessage.error('网络异常，叫号失败')
	} finally {
		busyId.value = null
	}
}

function openVisit(row) {
	current.value = row
	visitForm.medication = row.medication || ''
	visitForm.advice = row.advice || ''
	dialogVisible.value = true
}

async function save() {
	if (!current.value) return
	saving.value = true
	try {
		const finished = current.value.status !== '2'
		const formData = new FormData()
		formData.append('id', current.value.id)
		formData.append('medication', visitForm.medication)
		formData.append('advice', visitForm.advice)
		const url = finished ? '/doctorAuth/finish' : '/doctorAuth/advice'
		const { data } = await _axios.request({ url, method: 'POST', data: formData })
		if (data.code === 200) {
			ElMessage.success(finished ? `已完成 ${current.value.patientName} 的就诊` : '医嘱已保存')
			dialogVisible.value = false
			await load(true)
		} else {
			ElMessage.error(data.message || '保存失败')
		}
	} catch {
		ElMessage.error('网络异常，保存失败')
	} finally {
		saving.value = false
	}
}

onMounted(() => {
	load()
	// 10 秒静默刷新；弹窗打开时暂停，避免打断填写
	timer = setInterval(() => {
		if (!dialogVisible.value) load(true)
	}, 10000)
})
onBeforeUnmount(() => {
	if (timer) clearInterval(timer)
})
</script>

<style scoped lang="scss">
.d-today {
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

.p-name {
	font-weight: 700;
	color: #334155;
}

.p-meta {
	font-size: 12px;
	color: #94a3b8;
	margin-top: 2px;
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

	.v-remark {
		font-size: 12.5px;
		color: #b45309;
		background: #fffbeb;
		border-radius: 8px;
		padding: 6px 10px;
		margin-top: 8px;
	}
}
</style>
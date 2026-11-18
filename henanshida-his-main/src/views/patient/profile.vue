<template>
	<div class="profile-page">
		<!-- 页头 -->
		<div class="page-head">
			<div>
				<h2>个人中心</h2>
				<p>管理本人档案与家人信息，可为家人代挂号；家人档案同步至管理员「患者管理」</p>
			</div>
			<el-button :icon="Refresh" :loading="loading" @click="refresh()">刷新</el-button>
		</div>

		<!-- 本人档案 -->
		<div class="self-card" v-loading="loading">
			<div class="self-top">
				<el-avatar :size="60" class="self-avatar">{{ patientStore.avatarText }}</el-avatar>
				<div class="self-id">
					<div class="self-name">
						{{ patientStore.name }}
						<el-tag size="small" effect="plain" type="primary">本人</el-tag>
					</div>
					<div class="self-sub">{{ genderLabel(self.gender) }} · {{ ageText(self.birthday) }} · {{ self.phone || '未填手机号' }}</div>
				</div>
				<el-button type="primary" round :icon="Edit" @click="openSelf">编辑个人信息</el-button>
			</div>
			<div class="field-grid">
				<div class="field"><span class="k">姓名</span><span class="v">{{ self.patientName || '—' }}</span></div>
				<div class="field"><span class="k">性别</span><span class="v">{{ genderLabel(self.gender) }}</span></div>
				<div class="field"><span class="k">出生日期</span><span class="v">{{ self.birthday || '—' }}</span></div>
				<div class="field"><span class="k">身份证号</span><span class="v">{{ maskCard(self.idCard) }}</span></div>
				<div class="field"><span class="k">手机号</span><span class="v">{{ self.phone || '—' }}</span></div>
				<div class="field wide"><span class="k">家庭住址</span><span class="v">{{ self.address || '—' }}</span></div>
			</div>
			<div class="self-tip">
				<el-icon><InfoFilled /></el-icon>
				身份证号、性别、出生日期由建档信息确定，不可修改；手机号为登录账号。
			</div>
		</div>

		<!-- 我的家人 -->
		<div class="family-head">
			<div class="fh-title">我的家人 <span class="cnt">{{ family.length }}</span></div>
			<el-button type="primary" round :icon="Plus" @click="openAdd">添加家人</el-button>
		</div>
		<div class="family-grid" v-loading="loading">
			<el-empty v-if="!loading && family.length === 0"
				description="还没有家人档案，添加后可为家人代挂号" :image-size="90" />
			<div v-for="m in family" :key="m.id" class="fam-card">
				<div class="fam-top">
					<el-avatar :size="44" class="fam-avatar" :class="{ female: m.gender === '2' }">
						{{ (m.patientName || '家').charAt(0) }}
					</el-avatar>
					<div class="fam-id">
						<div class="fam-name">{{ m.patientName }}</div>
						<div class="fam-sub">{{ genderLabel(m.gender) }} · {{ ageText(m.birthday) }}</div>
					</div>
				</div>
				<div class="fam-fields">
					<div><el-icon><Iphone /></el-icon> {{ m.phone || '未填手机号' }}</div>
					<div><el-icon><Postcard /></el-icon> {{ maskCard(m.idCard) }}</div>
					<div v-if="m.address"><el-icon><HomeFilled /></el-icon> {{ m.address }}</div>
				</div>
				<div class="fam-actions">
					<el-button size="small" type="primary" round :icon="OfficeBuilding" @click="bookFor(m)">为TA挂号</el-button>
					<el-button size="small" round :icon="Edit" @click="openEdit(m)">编辑</el-button>
					<el-button size="small" type="danger" plain round :icon="Delete" @click="removeFam(m)">删除</el-button>
				</div>
			</div>
		</div>

		<!-- 说明 -->
		<div class="notice">
			<el-icon class="n-icon"><InfoFilled /></el-icon>
			<div>
				<b>家人档案说明</b>
				<p>家人与本人共用同一份患者档案库，添加后管理员端「患者管理」自动可见；可在挂号大厅为家人代挂号，并在「我的挂号」统一查看与退号。已有挂号记录的家人档案不能删除，以保留历史就诊信息。</p>
			</div>
		</div>

		<!-- 编辑本人信息 -->
		<el-dialog v-model="selfDialog" title="编辑个人信息" width="460px" :close-on-click-modal="false">
			<el-form :model="selfForm" label-width="86px">
				<el-form-item label="姓名" required>
					<el-input v-model="selfForm.patientName" maxlength="20" placeholder="请输入姓名" />
				</el-form-item>
				<el-form-item label="手机号" required>
					<el-input v-model="selfForm.phone" maxlength="11" placeholder="登录账号，11 位手机号" />
				</el-form-item>
				<el-form-item label="家庭住址">
					<el-input v-model="selfForm.address" type="textarea" :rows="2" maxlength="100" placeholder="选填" />
				</el-form-item>
				<el-form-item label="身份证号">
					<el-input :model-value="maskCard(self.idCard)" disabled />
				</el-form-item>
				<el-form-item label="性别 / 生日">
					<el-input :model-value="genderLabel(self.gender) + ' · ' + (self.birthday || '—')" disabled />
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="selfDialog = false">取消</el-button>
				<el-button type="primary" :loading="submitting" @click="submitSelf">保存</el-button>
			</template>
		</el-dialog>

		<!-- 添加家人 -->
		<el-dialog v-model="addDialog" title="添加家人档案" width="460px" :close-on-click-modal="false">
			<el-form :model="addForm" label-width="86px">
				<el-form-item label="姓名" required>
					<el-input v-model="addForm.patientName" maxlength="20" placeholder="家人姓名" />
				</el-form-item>
				<el-form-item label="身份证号" required>
					<el-input v-model="addForm.idCard" maxlength="18" placeholder="18 位身份证号，末位可为 X" />
				</el-form-item>
				<el-form-item label="手机号">
					<el-input v-model="addForm.phone" maxlength="11" placeholder="选填，家人可自行登录的手机号" />
				</el-form-item>
				<el-form-item v-if="addPreview" label="自动识别">
					<el-tag type="success" effect="plain">{{ addPreview.gender }} · {{ addPreview.birthday }}</el-tag>
					<span class="auto-tip">性别与出生日期按身份证规则自动解析</span>
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="addDialog = false">取消</el-button>
				<el-button type="primary" :loading="submitting" @click="submitAdd">添加</el-button>
			</template>
		</el-dialog>

		<!-- 编辑家人 -->
		<el-dialog v-model="editDialog" title="编辑家人信息" width="460px" :close-on-click-modal="false">
			<el-form :model="editForm" label-width="86px">
				<el-form-item label="姓名" required>
					<el-input v-model="editForm.patientName" maxlength="20" placeholder="家人姓名" />
				</el-form-item>
				<el-form-item label="手机号">
					<el-input v-model="editForm.phone" maxlength="11" placeholder="选填" />
				</el-form-item>
				<el-form-item label="家庭住址">
					<el-input v-model="editForm.address" type="textarea" :rows="2" maxlength="100" placeholder="选填" />
				</el-form-item>
				<el-form-item label="身份证号">
					<el-input :model-value="maskCard(editForm.idCard)" disabled />
				</el-form-item>
				<el-form-item label="性别 / 生日">
					<el-input :model-value="genderLabel(editForm.gender) + ' · ' + (editForm.birthday || '—')" disabled />
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="editDialog = false">取消</el-button>
				<el-button type="primary" :loading="submitting" @click="submitEdit">保存</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Edit, Plus, Delete, Iphone, Postcard, HomeFilled, OfficeBuilding, InfoFilled } from '@element-plus/icons-vue'
import { _axios } from '@/util/axios'
import { usePatientStore } from '@/stores/patient'

const router = useRouter()
const patientStore = usePatientStore()

const loading = ref(false)
const submitting = ref(false)
const selfDialog = ref(false)
const addDialog = ref(false)
const editDialog = ref(false)

const self = computed(() => patientStore.patient || {})
const family = computed(() => patientStore.family || [])

const selfForm = reactive({ patientName: '', phone: '', address: '' })
const addForm = reactive({ patientName: '', phone: '', idCard: '' })
const editForm = reactive({ id: null, patientName: '', phone: '', address: '', idCard: '', gender: '', birthday: '' })

function genderLabel(g) {
	return g === '1' ? '男' : g === '2' ? '女' : '未知'
}
function ageOf(birthday) {
	if (!birthday) return null
	const b = new Date(birthday)
	if (isNaN(b.getTime())) return null
	const now = new Date()
	let age = now.getFullYear() - b.getFullYear()
	const m = now.getMonth() - b.getMonth()
	if (m < 0 || (m === 0 && now.getDate() < b.getDate())) age--
	return age >= 0 && age < 150 ? age : null
}
function ageText(birthday) {
	const a = ageOf(birthday)
	return a == null ? '年龄未知' : `${a} 岁`
}
function maskCard(card) {
	if (!card) return '—'
	const c = String(card)
	return c.length >= 15 ? `${c.slice(0, 6)}********${c.slice(-4)}` : c
}

// 添加家人时按身份证实时预览性别 / 生日
const addPreview = computed(() => {
	const card = addForm.idCard.trim().toUpperCase()
	if (!/^\d{17}[\dX]$/.test(card)) return null
	const gender = (card.charAt(16) - '0') % 2 === 1 ? '男' : '女'
	const birthday = `${card.substring(6, 10)}-${card.substring(10, 12)}-${card.substring(12, 14)}`
	if (isNaN(new Date(birthday).getTime())) return null
	return { gender, birthday }
})

async function refresh() {
	loading.value = true
	try {
		await Promise.all([patientStore.fetchInfo(), patientStore.loadFamily()])
	} catch {
		// 刷新失败保留现有展示
	} finally {
		loading.value = false
	}
}

function openSelf() {
	selfForm.patientName = self.value.patientName || ''
	selfForm.phone = self.value.phone || ''
	selfForm.address = self.value.address || ''
	selfDialog.value = true
}

async function submitSelf() {
	const name = selfForm.patientName.trim()
	const phone = selfForm.phone.trim()
	if (!name) return ElMessage.warning('请填写姓名')
	if (!/^1[3-9]\d{9}$/.test(phone)) return ElMessage.warning('手机号格式不正确')
	submitting.value = true
	try {
		const fd = new FormData()
		fd.append('patientName', name)
		fd.append('phone', phone)
		fd.append('address', selfForm.address.trim())
		const { data } = await _axios.request({ url: '/patientAuth/updateProfile', method: 'POST', data: fd })
		if (data.code === 200) {
			ElMessage.success(data.message || '个人信息已更新')
			selfDialog.value = false
			await patientStore.fetchInfo()
		} else {
			ElMessage.error(data.message || '更新失败')
		}
	} catch {
		ElMessage.error('网络异常，更新失败')
	} finally {
		submitting.value = false
	}
}

function openAdd() {
	addForm.patientName = ''
	addForm.phone = ''
	addForm.idCard = ''
	addDialog.value = true
}

async function submitAdd() {
	const name = addForm.patientName.trim()
	const card = addForm.idCard.trim().toUpperCase()
	const phone = addForm.phone.trim()
	if (!name) return ElMessage.warning('请填写家人姓名')
	if (!/^\d{17}[\dX]$/.test(card)) return ElMessage.warning('身份证号需为 18 位（末位可为 X）')
	if (phone && !/^1[3-9]\d{9}$/.test(phone)) return ElMessage.warning('手机号格式不正确')
	submitting.value = true
	try {
		const fd = new FormData()
		fd.append('patientName', name)
		fd.append('idCard', card)
		fd.append('phone', phone)
		const { data } = await _axios.request({ url: '/patientAuth/addFamily', method: 'POST', data: fd })
		if (data.code === 200) {
			ElMessage.success(data.message || '家人档案已添加')
			addDialog.value = false
			await patientStore.loadFamily()
		} else {
			ElMessage.error(data.message || '添加失败')
		}
	} catch {
		ElMessage.error('网络异常，添加失败')
	} finally {
		submitting.value = false
	}
}

function openEdit(m) {
	editForm.id = m.id
	editForm.patientName = m.patientName || ''
	editForm.phone = m.phone || ''
	editForm.address = m.address || ''
	editForm.idCard = m.idCard || ''
	editForm.gender = m.gender || ''
	editForm.birthday = m.birthday || ''
	editDialog.value = true
}

async function submitEdit() {
	const name = editForm.patientName.trim()
	const phone = editForm.phone.trim()
	if (!name) return ElMessage.warning('请填写姓名')
	if (phone && !/^1[3-9]\d{9}$/.test(phone)) return ElMessage.warning('手机号格式不正确')
	submitting.value = true
	try {
		const fd = new FormData()
		fd.append('id', editForm.id)
		fd.append('patientName', name)
		fd.append('phone', phone)
		fd.append('address', editForm.address.trim())
		const { data } = await _axios.request({ url: '/patientAuth/updateFamily', method: 'POST', data: fd })
		if (data.code === 200) {
			ElMessage.success(data.message || '家人信息已更新')
			editDialog.value = false
			await patientStore.loadFamily()
		} else {
			ElMessage.error(data.message || '更新失败')
		}
	} catch {
		ElMessage.error('网络异常，更新失败')
	} finally {
		submitting.value = false
	}
}

async function removeFam(m) {
	try {
		await ElMessageBox.confirm(
			`确定删除家人「${m.patientName}」的档案吗？删除后管理员「患者管理」中也将移除。`,
			'删除家人', { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
		)
	} catch {
		return
	}
	try {
		const fd = new FormData()
		fd.append('id', m.id)
		const { data } = await _axios.request({ url: '/patientAuth/removeFamily', method: 'POST', data: fd })
		if (data.code === 200) {
			ElMessage.success(data.message || '家人档案已删除')
			await patientStore.loadFamily()
		} else {
			ElMessage.error(data.message || '删除失败')
		}
	} catch {
		ElMessage.error('网络异常，删除失败')
	}
}

function bookFor(m) {
	router.push({ path: '/patient/hall', query: { patientId: m.id } })
}

onMounted(refresh)
</script>

<style scoped lang="scss">
.profile-page {
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

/* 本人档案卡 */
.self-card {
	background: #fff;
	border: 1px solid #e8eef6;
	border-radius: 16px;
	padding: 20px 22px;
	margin-bottom: 22px;
}

.self-top {
	display: flex;
	align-items: center;
	gap: 16px;
}

.self-avatar {
	background: linear-gradient(135deg, #2563eb, #60a5fa);
	color: #fff;
	font-size: 24px;
	font-weight: 700;
	flex-shrink: 0;
}

.self-id {
	flex: 1;
	min-width: 0;
}

.self-name {
	font-size: 19px;
	font-weight: 800;
	color: #1e293b;
	display: flex;
	align-items: center;
	gap: 9px;
}

.self-sub {
	font-size: 13px;
	color: #64748b;
	margin-top: 5px;
}

.field-grid {
	display: grid;
	grid-template-columns: repeat(3, 1fr);
	gap: 12px 20px;
	margin-top: 18px;
	padding-top: 18px;
	border-top: 1px dashed #e8eef6;
}

.field {
	display: flex;
	flex-direction: column;
	gap: 4px;

	&.wide {
		grid-column: 1 / -1;
	}

	.k {
		font-size: 12px;
		color: #94a3b8;
	}

	.v {
		font-size: 14px;
		color: #334155;
		font-weight: 600;
		word-break: break-all;
	}
}

.self-tip {
	display: flex;
	align-items: center;
	gap: 6px;
	margin-top: 16px;
	font-size: 12.5px;
	color: #94a3b8;

	.el-icon {
		color: #cbd5e1;
	}
}

/* 家人区 */
.family-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 14px;
}

.fh-title {
	font-size: 16.5px;
	font-weight: 800;
	color: #1e293b;
	display: flex;
	align-items: center;
	gap: 9px;

	.cnt {
		font-size: 12px;
		background: #eff6ff;
		color: #2563eb;
		border-radius: 20px;
		padding: 2px 10px;
		font-weight: 700;
	}
}

.family-grid {
	display: grid;
	grid-template-columns: repeat(auto-fill, minmax(min(300px, 100%), 1fr));
	gap: 14px;
	min-height: 120px;
	margin-bottom: 22px;
}

.fam-card {
	background: #fff;
	border: 1px solid #e8eef6;
	border-radius: 15px;
	padding: 16px 18px;
	display: flex;
	flex-direction: column;
	gap: 12px;
	transition: all .2s;

	&:hover {
		box-shadow: 0 10px 24px rgba(30, 64, 175, .1);
		transform: translateY(-2px);
	}
}

.fam-top {
	display: flex;
	align-items: center;
	gap: 12px;
}

.fam-avatar {
	background: linear-gradient(135deg, #0ea5e9, #38bdf8);
	color: #fff;
	font-weight: 700;
	flex-shrink: 0;

	&.female {
		background: linear-gradient(135deg, #ec4899, #f472b6);
	}
}

.fam-id {
	flex: 1;
	min-width: 0;
}

.fam-name {
	font-size: 15.5px;
	font-weight: 800;
	color: #1e293b;
}

.fam-sub {
	font-size: 12.5px;
	color: #64748b;
	margin-top: 3px;
}

.fam-fields {
	display: flex;
	flex-direction: column;
	gap: 7px;
	border-top: 1px dashed #e8eef6;
	padding-top: 12px;

	div {
		display: flex;
		align-items: center;
		gap: 7px;
		font-size: 12.5px;
		color: #64748b;
		word-break: break-all;

		.el-icon {
			color: #94a3b8;
			flex-shrink: 0;
		}
	}
}

.fam-actions {
	display: flex;
	gap: 8px;
	flex-wrap: wrap;
}

.auto-tip {
	font-size: 12px;
	color: #94a3b8;
	margin-left: 10px;
}

/* 说明 */
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
	.field-grid {
		grid-template-columns: repeat(2, 1fr);
	}
}
</style>
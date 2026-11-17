<template>
	<div class="page">
		<!-- 页头 -->
		<div class="page-header">
			<div class="ph-left">
				<div class="ph-icon"><el-icon><component :is="icon" /></el-icon></div>
				<div>
					<h2>{{ title }}管理</h2>
					<div class="ph-sub">{{ subtitle || `维护${title}信息，支持查询、新增、修改与启用 / 禁用` }}</div>
				</div>
			</div>
			<el-button type="primary" :icon="Plus" round @click="openAdd">新增{{ title }}</el-button>
		</div>

		<!-- 搜索栏 -->
		<div v-if="searchFields.length" class="his-card search-bar">
			<template v-for="f in searchFields" :key="f.prop">
				<el-select
					v-if="f.type === 'select'"
					v-model="searchValues[f.prop]"
					:placeholder="f.placeholder || `请选择${f.label}`"
					clearable filterable
					@change="search">
					<el-option v-for="o in opts(f.prop)" :key="o.value" :label="o.label" :value="o.value" />
				</el-select>
				<el-date-picker
					v-else-if="f.type === 'date'"
					v-model="searchValues[f.prop]"
					type="date" value-format="YYYY-MM-DD"
					:placeholder="f.placeholder || `请选择${f.label}`"
					@change="search" />
				<el-input
					v-else
					v-model="searchValues[f.prop]"
					:placeholder="f.placeholder || `请输入${f.label}`"
					clearable
					@keyup.enter="search"
					@clear="search" />
			</template>
			<el-button type="primary" :icon="Search" @click="search">查询</el-button>
			<el-button :icon="RefreshLeft" @click="reset">重置</el-button>
			<div class="spacer"></div>
			<slot name="toolbar"></slot>
		</div>

		<!-- 表格 -->
		<div class="his-card table-card" v-loading="loading">
			<el-table :data="records" stripe style="width: 100%" :row-key="rowKey" empty-text="暂无数据">
				<el-table-column v-if="showIndex" type="index" label="序号" width="70" align="center" :index="rowIndex" />
				<template v-for="col in columns" :key="col.prop">
					<el-table-column
						:prop="col.prop" :label="col.label"
						:width="col.width" :min-width="col.minWidth"
						:align="col.align || 'left'" :fixed="col.fixed"
						:show-overflow-tooltip="col.tooltip !== false">
						<template #default="{ row }">
							<!-- 自定义插槽列 -->
							<slot v-if="col.slot" :name="col.slot" :row="row"></slot>
							<!-- 启用状态 -->
							<el-tag v-else-if="col.type === 'enabled'" :type="row[col.prop] ? 'success' : 'danger'" effect="light" round>
								{{ row[col.prop] ? '启用' : '禁用' }}
							</el-tag>
							<!-- 标签映射 -->
							<el-tag v-else-if="col.type === 'tag'" :type="tagOf(col, row[col.prop]).type" effect="light" round>
								{{ tagOf(col, row[col.prop]).text }}
							</el-tag>
							<!-- 字典映射 -->
							<span v-else-if="col.type === 'dict'">{{ (col.dict && col.dict[row[col.prop]]) || row[col.prop] || '—' }}</span>
							<!-- 金额 -->
							<span v-else-if="col.type === 'money'" class="money">¥{{ fmtMoney(row[col.prop]) }}</span>
							<!-- 普通 -->
							<span v-else>{{ row[col.prop] ?? '—' }}</span>
						</template>
					</el-table-column>
				</template>
				<el-table-column label="操作" :width="actionWidth" fixed="right" align="center">
					<template #default="{ row }">
						<slot name="row-actions" :row="row"></slot>
						<el-button link type="primary" :icon="Edit" @click="openEdit(row)">修改</el-button>
						<el-button link :type="row.enabled ? 'danger' : 'success'" :icon="row.enabled ? Delete : CircleCheck" @click="toggle(row)">
							{{ row.enabled ? '禁用' : '启用' }}
						</el-button>
					</template>
				</el-table-column>
			</el-table>
			<div class="pager">
				<el-pagination
					background layout="total, sizes, prev, pager, next, jumper"
					:total="totalRow" :page-sizes="[10, 20, 50, 100]"
					v-model:current-page="pageNo" v-model:page-size="pageSizeRef"
					@current-change="onPageChange" @size-change="onSizeChange" />
			</div>
		</div>

		<!-- 新增 / 修改 弹窗 -->
		<el-dialog v-model="dialogVisible" :title="(mode === 'add' ? '新增' : '修改') + title" width="600px" :close-on-click-modal="false" destroy-on-close>
			<el-form ref="formRef" :model="form" :rules="rules" label-width="96px" v-loading="saving">
				<el-row :gutter="18">
					<template v-for="f in visibleFields" :key="f.prop">
						<el-col :span="f.span || 24">
							<el-form-item :label="f.label" :prop="f.prop">
								<el-switch v-if="f.type === 'switch'" v-model="form[f.prop]" />
								<el-input-number
									v-else-if="f.type === 'number'"
									v-model="form[f.prop]" :min="f.min ?? 0" :max="f.max ?? 999999"
									:precision="f.precision" :step="f.step || 1" controls-position="right" style="width: 100%" />
								<el-select
									v-else-if="f.type === 'select'"
									v-model="form[f.prop]" :placeholder="f.placeholder || `请选择${f.label}`"
									clearable filterable style="width: 100%">
									<el-option v-for="o in opts(f.prop)" :key="o.value" :label="o.label" :value="o.value" />
								</el-select>
								<el-date-picker
									v-else-if="f.type === 'date'"
									v-model="form[f.prop]" type="date" value-format="YYYY-MM-DD"
									:placeholder="f.placeholder || `请选择${f.label}`" style="width: 100%" />
								<el-upload
									v-else-if="f.type === 'image'"
									class="his-uploader"
									action="#"
									list-type="picture-card"
									accept="image/*"
									:limit="f.limit || 1"
									:multiple="false"
									v-model:file-list="uploadFiles[f.prop]"
									:http-request="(opt) => doUpload(opt, f)"
									:before-upload="(file) => beforeImageUpload(file, f)"
									:on-preview="previewImage"
									:on-remove="(file) => removeImage(file, f)"
									:on-exceed="onImageExceed">
									<el-icon><Plus /></el-icon>
								</el-upload>
								<el-input
									v-else-if="f.type === 'textarea'"
									v-model="form[f.prop]" type="textarea" :rows="f.rows || 3"
									:placeholder="f.placeholder || `请输入${f.label}`" />
								<el-input
									v-else-if="f.type === 'password'"
									v-model="form[f.prop]" type="password" show-password
									:placeholder="f.placeholder || `请输入${f.label}`" />
								<el-input v-else v-model="form[f.prop]" :placeholder="f.placeholder || `请输入${f.label}`" clearable />
							</el-form-item>
						</el-col>
					</template>
				</el-row>
			</el-form>
			<template #footer>
				<el-button @click="dialogVisible = false">取消</el-button>
				<el-button type="primary" :loading="saving" @click="save">保存</el-button>
			</template>
		</el-dialog>

		<!-- 头像预览 -->
		<el-dialog v-model="previewVisible" title="头像预览" width="420px" append-to-body>
			<img :src="previewUrl" alt="头像预览" class="preview-img" />
		</el-dialog>
	</div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { _axios } from '@/util/axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshLeft, Edit, Delete, CircleCheck } from '@element-plus/icons-vue'

const props = defineProps({
	api: { type: String, required: true },
	title: { type: String, default: '记录' },
	icon: { type: String, default: 'Document' },
	subtitle: { type: String, default: '' },
	searchFields: { type: Array, default: () => [] },
	columns: { type: Array, default: () => [] },
	fields: { type: Array, default: () => [] },
	rowKey: { type: String, default: 'id' },
	showIndex: { type: Boolean, default: true },
	pageSize: { type: Number, default: 10 },
	actionWidth: { type: [String, Number], default: 160 },
	// 列表查询额外固定参数
	extraParams: { type: Object, default: () => ({}) },
	// 静默自动刷新间隔（毫秒），0 表示关闭；开启后定时 + 回到窗口自动同步列表
	autoRefresh: { type: Number, default: 0 }
})

const loading = ref(false)
const saving = ref(false)
const records = ref([])
const totalRow = ref(0)
const pageNo = ref(1)
const pageSizeRef = ref(props.pageSize)
const searchValues = reactive({})
const optionsMap = reactive({})

const dialogVisible = ref(false)
const mode = ref('add')
const formRef = ref(null)
const form = reactive({})
// 图片字段（picture-card）的文件列表，按字段名分别维护
const uploadFiles = reactive({})
const previewVisible = ref(false)
const previewUrl = ref('')

const visibleFields = computed(() => props.fields.filter(f => {
	if (f.addOnly && mode.value === 'edit') return false
	if (f.editOnly && mode.value === 'add') return false
	return true
}))

const rules = computed(() => {
	const r = {}
	for (const f of props.fields) {
		if (f.required) {
			r[f.prop] = [{
				required: true,
				message: `${f.type === 'select' || f.type === 'date' ? '请选择' : '请输入'}${f.label}`,
				trigger: f.type === 'select' || f.type === 'date' || f.type === 'switch' ? 'change' : 'blur'
			}]
		}
	}
	return r
})

function opts(prop) {
	return optionsMap[prop] || []
}

function tagOf(col, val) {
	const m = (col.tagMap && col.tagMap[val]) || {}
	return { text: m.text ?? (val ?? '—'), type: m.type || 'info' }
}

function fmtMoney(v) {
	const n = Number(v)
	return isNaN(n) ? '0.00' : n.toFixed(2)
}

// 序号列：跨分页连续编号（第 2 页从 pageSize+1 开始），不依赖数据库主键
function rowIndex(i) {
	return (pageNo.value - 1) * pageSizeRef.value + i + 1
}

// 解析下拉选项：静态 options / 远程 optionsApi / 自定义 loadOptions
async function resolveOptions() {
	const all = [...props.searchFields, ...props.fields]
	const seen = new Set()
	const tasks = []
	for (const f of all) {
		if (!f.prop || seen.has(f.prop)) continue
		seen.add(f.prop)
		if (f.options) {
			optionsMap[f.prop] = f.options
		} else if (f.loadOptions) {
			tasks.push(Promise.resolve(f.loadOptions()).then(list => { optionsMap[f.prop] = list || [] }).catch(() => { optionsMap[f.prop] = [] }))
		} else if (f.optionsApi) {
			tasks.push(_axios.request({ url: f.optionsApi, method: 'GET' }).then(({ data }) => {
				const arr = Array.isArray(data.content) ? data.content : (data.content?.records || [])
				optionsMap[f.prop] = arr.map(it => ({
					label: it[f.labelKey || 'label'],
					value: it[f.valueKey || 'value']
				}))
			}).catch(() => { optionsMap[f.prop] = [] }))
		}
	}
	await Promise.all(tasks)
}

// silent 为静默刷新：不转圈、失败不弹错，避免自动同步打扰当前操作
function fetchList(silent = false) {
	if (!silent) loading.value = true
	const params = { pageNo: pageNo.value, pageSize: pageSizeRef.value, ...props.extraParams }
	for (const k in searchValues) {
		if (searchValues[k] !== '' && searchValues[k] !== null && searchValues[k] !== undefined) {
			params[k] = searchValues[k]
		}
	}
	_axios.request({ url: `${props.api}/list`, method: 'GET', params }).then(({ data }) => {
		if (data.code === 200) {
			records.value = data.content?.records || []
			totalRow.value = data.content?.totalRow || 0
		} else if (!silent) {
			ElMessage.error(data.message || '查询失败')
		}
	}).catch(() => {
		if (!silent) ElMessage.error('网络异常，无法加载数据')
	}).finally(() => { if (!silent) loading.value = false })
}

function onPageChange() {
	fetchList()
}

function search() {
	pageNo.value = 1
	fetchList()
}

function reset() {
	for (const k in searchValues) searchValues[k] = ''
	pageNo.value = 1
	fetchList()
}

function onSizeChange() {
	pageNo.value = 1
	fetchList()
}

function buildForm(row) {
	for (const k in form) delete form[k]
	form.id = row ? row.id : null
	for (const f of props.fields) {
		if (row) {
			form[f.prop] = row[f.prop] ?? (f.type === 'switch' ? false : '')
		} else {
			form[f.prop] = f.defaultValue !== undefined ? f.defaultValue : (f.type === 'switch' ? true : (f.type === 'number' ? (f.min ?? 0) : ''))
		}
		if (f.type === 'image') {
			const url = form[f.prop]
			uploadFiles[f.prop] = url
				? [{ name: String(url).split('/').pop() || '头像', url, uid: Date.now() }]
				: []
		}
	}
}

function openAdd() {
	mode.value = 'add'
	buildForm(null)
	dialogVisible.value = true
}

function openEdit(row) {
	mode.value = 'edit'
	buildForm(row)
	dialogVisible.value = true
}

// ===== 图片上传（picture-card，默认限 1 张）=====
function beforeImageUpload(file, f) {
	const isImage = (file.type || '').startsWith('image/')
	const maxMB = f.maxSize || 5
	if (!isImage) { ElMessage.error('只能上传图片文件'); return false }
	if (file.size / 1024 / 1024 > maxMB) { ElMessage.error(`图片大小不能超过 ${maxMB}MB`); return false }
	return true
}

// 自定义上传：走 _axios 自动携带登录 token，成功后把服务端 URL 写回表单与缩略图
async function doUpload(opt, f) {
	const fd = new FormData()
	fd.append('file', opt.file)
	try {
		const { data } = await _axios.request({ url: '/upload/image', method: 'POST', data: fd })
		if (data.code === 200) {
			const url = data.content
			form[f.prop] = url
			const item = (uploadFiles[f.prop] || []).find(x => x.uid === opt.file.uid)
			if (item) { item.url = url; item.name = opt.file.name }
			opt.onSuccess(data)
			ElMessage.success('头像上传成功')
		} else {
			uploadFiles[f.prop] = (uploadFiles[f.prop] || []).filter(x => x.uid !== opt.file.uid)
			form[f.prop] = ''
			opt.onError(new Error(data.message || '上传失败'))
			ElMessage.error(data.message || '上传失败')
		}
	} catch (e) {
		uploadFiles[f.prop] = (uploadFiles[f.prop] || []).filter(x => x.uid !== opt.file.uid)
		form[f.prop] = ''
		opt.onError(e)
		ElMessage.error('网络异常，头像上传失败')
	}
}

function removeImage(file, f) {
	form[f.prop] = ''
}

function previewImage(file) {
	previewUrl.value = file.url
	previewVisible.value = true
}

function onImageExceed() {
	ElMessage.warning('最多只能上传 1 张头像，请先删除已有头像')
}

function save() {
	formRef.value.validate(valid => {
		if (!valid) return
		saving.value = true
		const fd = new FormData()
		if (mode.value === 'edit' && form.id != null) fd.append('id', form.id)
		for (const f of props.fields) {
			if (f.addOnly && mode.value === 'edit') continue
			if (f.editOnly && mode.value === 'add') continue
			const v = form[f.prop]
			// 图片字段始终提交（含空串），这样删除头像后也能把清空同步到后端
			if (f.type === 'image') { fd.append(f.prop, v || ''); continue }
			if (v === null || v === undefined || v === '') continue
			fd.append(f.prop, v)
		}
		const url = mode.value === 'add' ? `${props.api}/add` : `${props.api}/update`
		_axios.request({ url, method: 'POST', data: fd }).then(({ data }) => {
			if (data.code === 200) {
				ElMessage.success(data.message || '保存成功')
				dialogVisible.value = false
				fetchList()
			} else {
				ElMessage.error(data.message || '保存失败')
			}
		}).catch(() => {
			ElMessage.error('网络异常，保存失败')
		}).finally(() => { saving.value = false })
	})
}

function toggle(row) {
	const action = row.enabled ? '禁用' : '启用'
	ElMessageBox.confirm(
		`确定${action}该${props.title}吗？` + (row.enabled ? '禁用后数据仍保留，可随时恢复。' : ''),
		'提示', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
	).then(() => {
		loading.value = true
		const fd = new FormData()
		fd.append('id', row.id)
		fd.append('enabled', !row.enabled)
		_axios.request({ url: `${props.api}/del`, method: 'POST', data: fd }).then(({ data }) => {
			if (data.code === 200) {
				ElMessage.success(data.message || `${action}成功`)
				fetchList()
			} else {
				ElMessage.error(data.message || `${action}失败`)
			}
		}).catch(() => {
			ElMessage.error('网络异常，操作失败')
		}).finally(() => { loading.value = false })
	}).catch(() => {})
}

// 自动刷新：弹窗编辑期间暂停，避免列表变动干扰正在填写的表单
function silentSync() {
	if (props.autoRefresh > 0 && document.visibilityState === 'visible' && !dialogVisible.value) {
		fetchList(true)
	}
}

let autoTimer = null
onMounted(async () => {
	await resolveOptions()
	fetchList()
	if (props.autoRefresh > 0) {
		autoTimer = setInterval(silentSync, props.autoRefresh)
		window.addEventListener('focus', silentSync)
		document.addEventListener('visibilitychange', silentSync)
	}
})

onBeforeUnmount(() => {
	if (autoTimer) clearInterval(autoTimer)
	window.removeEventListener('focus', silentSync)
	document.removeEventListener('visibilitychange', silentSync)
})

defineExpose({ refresh: fetchList })
</script>

<style scoped>
.his-uploader :deep(.el-upload--picture-card) {
	width: 100px;
	height: 100px;
	border-radius: 10px;
}

.his-uploader :deep(.el-upload-list--picture-card .el-upload-list__item) {
	width: 100px;
	height: 100px;
	border-radius: 10px;
}

.preview-img {
	width: 100%;
	display: block;
	border-radius: 8px;
}
</style>
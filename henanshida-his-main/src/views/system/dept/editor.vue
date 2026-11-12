<template>
	<el-form v-loading.fullscreen.lock="loading" :model="wyform" label-width="80px">
		<el-form-item label="科室名称">
			<el-input v-model="wyform.deptName" placeholder="请输入科室名称"></el-input>
		</el-form-item>
		<el-form-item label="科室编码">
			<el-input v-model="wyform.deptCode" placeholder="请输入科室编码"></el-input>
		</el-form-item>
		<el-form-item label="类别">
			<el-select v-model="wyform.deptType" placeholder="请选择科室类别">
				<el-option label="临床科室" value="1"></el-option>
				<el-option label="行政科室" value="2"></el-option>
				<el-option label="医技科室" value="3"></el-option>
			</el-select>
		</el-form-item>
		<el-form-item>
			<el-button type="primary" plain :icon="Save" @click="save">保存</el-button>
		</el-form-item>
	</el-form>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import Save from '@@/icons/save' 
import { _axios } from '@/util/axios'
import { ElMessage } from 'element-plus'
const props = defineProps({
	row: { type: Object, default: null }
})
const emit = defineEmits(['saved'])
const wyform = reactive({
	id: null,
	deptName: '',
	deptCode: '',
	deptType: ''
})
// 父组件传了 row 就是修改，没传就是添加
watch(() => props.row, row => {
	wyform.id = row ? row.id : null
	wyform.deptName = row ? row.deptName : ''
	wyform.deptCode = row ? row.deptCode : ''
	wyform.deptType = row ? row.deptType : ''
}, { immediate: true })
const loading = ref(false)
function save () {
	if (!wyform.deptName || !wyform.deptCode || !wyform.deptType) {
		ElMessage.warning('科室名称、编码、类别都要填')
		return
	}
	loading.value = true
	const formData = new FormData()
	for (const key in wyform) {
		if (wyform[key] !== null && wyform[key] !== '') {
			formData.append(key, wyform[key])
		}
	}
	_axios.request({
		url: wyform.id ? '/sysDept/update' : '/sysDept/add',
		method: 'POST',
		data: formData
	}).then(response => {
		console.dir(response.data)
		if (response.data.code === 200) {
			if (response.data.message) {
				ElMessage.success(response.data.message)
			}
			emit('saved')
		} else {
			ElMessage.error(response.data.message)
		}
	}).catch(error => {
		
	}).finally(() => {
		loading.value = false
	})
}
</script>

<style scoped lang="scss">
</style>
<template>
	<crud-table
		api="/sysUser" title="医生" icon="User"
		subtitle="维护医护人员档案，新增时设置登录密码，修改时留空则保持原密码"
		:search-fields="searchFields" :columns="columns" :fields="fields" :action-width="170" />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import CrudTable from '@@/CrudTable'
import { GENDER, deptOptions, titleOptions, toMap } from '@/util/options'

const deptOpts = ref([])
const titleOpts = ref([])
const deptMap = computed(() => toMap(deptOpts.value))
const titleMap = computed(() => toMap(titleOpts.value))

onMounted(async () => {
	deptOpts.value = await deptOptions()
	titleOpts.value = await titleOptions()
})

const searchFields = [
	{ prop: 'realName', label: '医生姓名', placeholder: '请输入医生姓名' }
]

const columns = computed(() => [
	{ prop: 'realName', label: '姓名', minWidth: 100 },
	{ prop: 'gender', label: '性别', width: 70, align: 'center', type: 'dict', dict: GENDER },
	{ prop: 'employeeNo', label: '工号', width: 110 },
	{ prop: 'phone', label: '手机号', minWidth: 130 },
	{ prop: 'deptId', label: '所属科室', minWidth: 120, type: 'dict', dict: deptMap.value },
	{ prop: 'titleId', label: '职称', minWidth: 110, type: 'dict', dict: titleMap.value },
	{ prop: 'enabled', label: '状态', width: 90, align: 'center', type: 'enabled' }
])

const genderOptions = [
	{ label: '男', value: '1' },
	{ label: '女', value: '2' },
	{ label: '未知', value: '0' }
]

const fields = [
	{ prop: 'realName', label: '姓名', required: true, span: 12 },
	{ prop: 'password', label: '登录密码', type: 'password', required: true, span: 12, addOnly: true, placeholder: '新增时必填' },
	{ prop: 'gender', label: '性别', type: 'select', span: 12, options: genderOptions, defaultValue: '1' },
	{ prop: 'employeeNo', label: '工号', span: 12 },
	{ prop: 'phone', label: '手机号', span: 12 },
	{ prop: 'email', label: '邮箱', span: 12 },
	{ prop: 'deptId', label: '所属科室', type: 'select', span: 12, loadOptions: deptOptions },
	{ prop: 'titleId', label: '职称', type: 'select', span: 12, loadOptions: titleOptions },
	{ prop: 'birthday', label: '出生日期', type: 'date', span: 12 },
	{ prop: 'enabled', label: '是否启用', type: 'switch', span: 12, defaultValue: true },
	{ prop: 'address', label: '家庭住址', type: 'textarea', rows: 2 },
	{ prop: 'intro', label: '医生简介', type: 'textarea', rows: 3, placeholder: '擅长治疗的疾病和症状' }
]
</script>
<template>
	<crud-table
		api="/sysDict" title="字典" icon="Collection"
		subtitle="维护系统数据字典，按字典类型分组管理标签与取值"
		:search-fields="searchFields" :columns="columns" :fields="fields" />
</template>

<script setup>
import CrudTable from '@@/CrudTable'

const dictTypes = [
	{ label: '性别 (sys_gender)', value: 'sys_gender' },
	{ label: '科室类别 (sys_dept_type)', value: 'sys_dept_type' },
	{ label: '就诊时段 (reg_period)', value: 'reg_period' },
	{ label: '挂号类型 (reg_type)', value: 'reg_type' },
	{ label: '挂号状态 (reg_status)', value: 'reg_status' }
]

const searchFields = [
	{ prop: 'dictType', label: '字典类型', type: 'select', options: dictTypes, placeholder: '请选择字典类型' },
	{ prop: 'dictLabel', label: '字典标签', placeholder: '请输入字典标签' }
]

const columns = [
	{ prop: 'dictType', label: '字典类型', minWidth: 150 },
	{ prop: 'dictLabel', label: '字典标签', minWidth: 130 },
	{ prop: 'dictValue', label: '字典值', width: 110 },
	{ prop: 'sort', label: '排序', width: 80, align: 'center' },
	{ prop: 'enabled', label: '状态', width: 100, align: 'center', type: 'enabled' }
]

const fields = [
	{ prop: 'dictType', label: '字典类型', type: 'select', required: true, span: 24, options: dictTypes },
	{ prop: 'dictLabel', label: '字典标签', required: true, span: 12 },
	{ prop: 'dictValue', label: '字典值', required: true, span: 12 },
	{ prop: 'sort', label: '排序', type: 'number', span: 12, defaultValue: 0, min: 0 },
	{ prop: 'enabled', label: '是否启用', type: 'switch', span: 12, defaultValue: true }
]
</script>
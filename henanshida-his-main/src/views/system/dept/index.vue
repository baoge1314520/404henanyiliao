<template>
	<crud-table
		api="/sysDept" title="科室" icon="OfficeBuilding"
		subtitle="维护医院科室档案，科室编码唯一，支持启用 / 禁用"
		:search-fields="searchFields" :columns="columns" :fields="fields" :show-index="false" />
</template>

<script setup>
import CrudTable from '@@/CrudTable'
import { DEPT_TYPE } from '@/util/options'

const searchFields = [
	{ prop: 'name', label: '科室名称', placeholder: '请输入科室名称' }
]

const columns = [
	{ prop: 'id', label: '编号', width: 80, align: 'center' },
	{ prop: 'deptName', label: '科室名称', minWidth: 140 },
	{ prop: 'deptCode', label: '科室编码', minWidth: 130 },
	{ prop: 'deptType', label: '科室类别', width: 120, type: 'dict', dict: DEPT_TYPE },
	{ prop: 'enabled', label: '状态', width: 100, align: 'center', type: 'enabled' },
	{ prop: 'createDate', label: '创建日期', width: 130 }
]

const fields = [
	{ prop: 'deptName', label: '科室名称', required: true, span: 12 },
	{ prop: 'deptCode', label: '科室编码', required: true, span: 12 },
	{
		prop: 'deptType', label: '科室类别', type: 'select', required: true, span: 12,
		options: [
			{ label: '临床科室', value: '1' },
			{ label: '行政科室', value: '2' },
			{ label: '医技科室', value: '3' }
		]
	},
	{ prop: 'enabled', label: '是否启用', type: 'switch', span: 12, defaultValue: true }
]
</script>
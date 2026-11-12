<template>
	<crud-table
		api="/sysTitle" title="职称" icon="Medal"
		subtitle="维护医生职称与对应挂号级别、挂号费"
		:search-fields="searchFields" :columns="columns" :fields="fields" :show-index="false" />
</template>

<script setup>
import CrudTable from '@@/CrudTable'

const searchFields = [
	{ prop: 'titleName', label: '职称名称', placeholder: '请输入职称名称' }
]

const columns = [
	{ prop: 'id', label: '编号', width: 80, align: 'center' },
	{ prop: 'titleName', label: '职称名称', minWidth: 140 },
	{ prop: 'regLevel', label: '挂号级别', minWidth: 140 },
	{ prop: 'regFee', label: '挂号费', width: 110, align: 'right', type: 'money' },
	{ prop: 'description', label: '说明', minWidth: 180 },
	{ prop: 'enabled', label: '状态', width: 100, align: 'center', type: 'enabled' }
]

const fields = [
	{ prop: 'titleName', label: '职称名称', required: true, span: 12 },
	{
		prop: 'regLevel', label: '挂号级别', type: 'select', required: true, span: 12,
		options: [
			{ label: '专家号', value: '专家号' },
			{ label: '副主任医师号', value: '副主任医师号' },
			{ label: '主治医师号', value: '主治医师号' },
			{ label: '普通号', value: '普通号' }
		]
	},
	{ prop: 'regFee', label: '挂号费(元)', type: 'number', required: true, precision: 2, step: 1, min: 0, span: 12 },
	{ prop: 'enabled', label: '是否启用', type: 'switch', span: 12, defaultValue: true },
	{ prop: 'description', label: '说明', type: 'textarea', rows: 3 }
]
</script>
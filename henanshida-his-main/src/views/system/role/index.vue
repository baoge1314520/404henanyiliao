<template>
	<crud-table
		api="/sysRole" title="角色" icon="Avatar"
		subtitle="维护系统角色与权限编码，用于多角色访问控制"
		:search-fields="searchFields" :columns="columns" :fields="fields" :show-index="false" />
</template>

<script setup>
import CrudTable from '@@/CrudTable'

const searchFields = [
	{ prop: 'roleName', label: '角色名称', placeholder: '请输入角色名称' }
]

const columns = [
	{ prop: 'id', label: '编号', width: 80, align: 'center' },
	{ prop: 'roleName', label: '角色名称', minWidth: 140 },
	{ prop: 'roleCode', label: '角色编码', minWidth: 140 },
	{ prop: 'remark', label: '备注', minWidth: 200 },
	{ prop: 'enabled', label: '状态', width: 100, align: 'center', type: 'enabled' }
]

const fields = [
	{ prop: 'roleName', label: '角色名称', required: true, span: 12 },
	{ prop: 'roleCode', label: '角色编码', required: true, span: 12, placeholder: '如 admin / doctor' },
	{ prop: 'remark', label: '备注', type: 'textarea', rows: 3 },
	{ prop: 'enabled', label: '是否启用', type: 'switch', defaultValue: true }
]
</script>
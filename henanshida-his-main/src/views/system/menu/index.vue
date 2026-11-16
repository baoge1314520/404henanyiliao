<template>
	<crud-table
		api="/sysMenu" title="菜单" icon="Menu"
		subtitle="维护系统导航菜单与路由路径，父菜单 ID 为 0 表示顶级"
		:search-fields="searchFields" :columns="columns" :fields="fields" />
</template>

<script setup>
import CrudTable from '@@/CrudTable'
import { menuOptions } from '@/util/options'

const searchFields = [
	{ prop: 'menuName', label: '菜单名称', placeholder: '请输入菜单名称' }
]

const columns = [
	{ prop: 'menuName', label: '菜单名称', minWidth: 140 },
	{ prop: 'parentId', label: '父菜单ID', width: 100, align: 'center' },
	{ prop: 'path', label: '路由路径', minWidth: 140 },
	{ prop: 'icon', label: '图标', width: 130 },
	{ prop: 'sort', label: '排序', width: 80, align: 'center' },
	{ prop: 'enabled', label: '状态', width: 100, align: 'center', type: 'enabled' }
]

const fields = [
	{ prop: 'menuName', label: '菜单名称', required: true, span: 12 },
	{ prop: 'parentId', label: '父菜单', type: 'select', span: 12, loadOptions: menuOptions, defaultValue: 0 },
	{ prop: 'path', label: '路由路径', span: 12, placeholder: '如 /dept' },
	{ prop: 'icon', label: '图标名称', span: 12, placeholder: 'Element Plus 图标名' },
	{ prop: 'sort', label: '排序', type: 'number', span: 12, defaultValue: 0, min: 0 },
	{ prop: 'enabled', label: '是否启用', type: 'switch', span: 12, defaultValue: true }
]
</script>
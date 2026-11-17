<template>
	<crud-table
		api="/sysPatient" title="患者" icon="UserFilled"
		subtitle="维护患者档案，身份证号唯一，挂号时可自动复用；患者端新注册自动同步"
		:auto-refresh="8000"
		:search-fields="searchFields" :columns="columns" :fields="fields" />
</template>

<script setup>
import CrudTable from '@@/CrudTable'
import { GENDER } from '@/util/options'

const searchFields = [
	{ prop: 'patientName', label: '患者姓名', placeholder: '请输入患者姓名' }
]

const columns = [
	{ prop: 'patientName', label: '姓名', minWidth: 110 },
	{ prop: 'gender', label: '性别', width: 80, align: 'center', type: 'dict', dict: GENDER },
	{ prop: 'birthday', label: '出生日期', width: 130 },
	{ prop: 'phone', label: '联系电话', minWidth: 130 },
	{ prop: 'idCard', label: '身份证号', minWidth: 180 },
	{ prop: 'enabled', label: '状态', width: 100, align: 'center', type: 'enabled' }
]

const genderOptions = [
	{ label: '男', value: '1' },
	{ label: '女', value: '2' },
	{ label: '未知', value: '0' }
]

const fields = [
	{ prop: 'patientName', label: '患者姓名', required: true, span: 12 },
	{ prop: 'gender', label: '性别', type: 'select', span: 12, options: genderOptions, defaultValue: '1' },
	{ prop: 'birthday', label: '出生日期', type: 'date', span: 12 },
	{ prop: 'phone', label: '联系电话', span: 12 },
	{ prop: 'idCard', label: '身份证号', span: 24, placeholder: '18 位身份证号' },
	{ prop: 'address', label: '家庭住址', type: 'textarea', rows: 2 },
	{ prop: 'enabled', label: '是否启用', type: 'switch', defaultValue: true }
]
</script>
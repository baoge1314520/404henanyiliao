<template>
	<crud-table
		api="/sysSchedule" title="排班" icon="Calendar"
		subtitle="安排医生出诊日期、时段与号源数量"
		:search-fields="searchFields" :columns="columns" :fields="fields" />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import CrudTable from '@@/CrudTable'
import { REG_PERIOD, deptOptions, doctorOptions, toMap } from '@/util/options'

const deptOpts = ref([])
const doctorOpts = ref([])
const deptMap = computed(() => toMap(deptOpts.value))
const doctorMap = computed(() => toMap(doctorOpts.value))

onMounted(async () => {
	deptOpts.value = await deptOptions()
	doctorOpts.value = await doctorOptions()
})

const searchFields = [
	{ prop: 'deptId', label: '科室', type: 'select', loadOptions: deptOptions, placeholder: '请选择科室' },
	{ prop: 'doctorId', label: '医生', type: 'select', loadOptions: doctorOptions, placeholder: '请选择医生' },
	{ prop: 'scheduleDate', label: '排班日期', type: 'date' }
]

const columns = computed(() => [
	{ prop: 'doctorId', label: '医生', minWidth: 110, type: 'dict', dict: doctorMap.value },
	{ prop: 'deptId', label: '科室', minWidth: 120, type: 'dict', dict: deptMap.value },
	{ prop: 'scheduleDate', label: '排班日期', width: 130 },
	{ prop: 'regPeriod', label: '时段', width: 90, align: 'center', type: 'dict', dict: REG_PERIOD },
	{ prop: 'quota', label: '号源数', width: 90, align: 'center' },
	{ prop: 'enabled', label: '状态', width: 90, align: 'center', type: 'enabled' }
])

const fields = [
	{ prop: 'doctorId', label: '医生', type: 'select', required: true, span: 12, loadOptions: doctorOptions },
	{ prop: 'deptId', label: '科室', type: 'select', required: true, span: 12, loadOptions: deptOptions },
	{ prop: 'scheduleDate', label: '排班日期', type: 'date', required: true, span: 12 },
	{
		prop: 'regPeriod', label: '时段', type: 'select', required: true, span: 12, defaultValue: '1',
		options: [{ label: '上午', value: '1' }, { label: '下午', value: '2' }]
	},
	{ prop: 'quota', label: '号源数', type: 'number', span: 12, defaultValue: 30, min: 0, max: 999 },
	{ prop: 'enabled', label: '是否启用', type: 'switch', span: 12, defaultValue: true }
]
</script>
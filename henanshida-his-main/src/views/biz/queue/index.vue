<template>
	<div class="page">
		<div class="page-header">
			<div class="ph-left">
				<div class="ph-icon"><el-icon><Bell /></el-icon></div>
				<div>
					<h2>叫号队列</h2>
					<div class="ph-sub">待就诊患者排队叫号，未叫号优先，叫号后记录叫号时间</div>
				</div>
			</div>
			<div class="head-actions">
				<el-switch v-model="autoRefresh" active-text="自动刷新" inline-prompt style="margin-right: 14px" />
				<el-button :icon="Refresh" round @click="load">刷新队列</el-button>
			</div>
		</div>

		<!-- 统计 -->
		<div class="q-stats">
			<div class="q-stat his-card"><span class="n">{{ list.length }}</span><span class="l">待就诊总数</span></div>
			<div class="q-stat his-card warn"><span class="n">{{ uncalledCount }}</span><span class="l">尚未叫号</span></div>
			<div class="q-stat his-card ok"><span class="n">{{ calledCount }}</span><span class="l">已叫号</span></div>
		</div>

		<div v-loading="loading" class="queue-wrap">
			<div v-if="!list.length && !loading" class="his-card his-empty">
				<el-icon :size="46"><CircleCheck /></el-icon>
				<p>当前没有待就诊患者，队列已清空</p>
			</div>
			<div class="q-grid">
				<div v-for="(p, i) in list" :key="p.id" class="q-card his-card" :class="{ called: !!p.callTime }">
					<div class="q-no" :class="{ called: !!p.callTime }">{{ i + 1 }}</div>
					<div class="q-main">
						<div class="q-name">
							{{ p.patientName }}
							<el-tag size="small" effect="plain" round>{{ GENDER[p.gender] || '未知' }}</el-tag>
							<el-tag v-if="p.callTime" size="small" type="success" effect="light" round>已叫号 {{ time(p.callTime) }}</el-tag>
							<el-tag v-else size="small" type="warning" effect="light" round>待叫号</el-tag>
						</div>
						<div class="q-meta">{{ p.deptName || '—' }} · {{ p.doctorName || '—' }} · {{ p.titleName || '' }}</div>
						<div class="q-sub">{{ p.regNo }} · {{ p.regDate }} {{ REG_PERIOD[p.regPeriod] || '' }}</div>
					</div>
					<div class="q-actions">
						<el-button :type="p.callTime ? 'default' : 'primary'" :icon="Bell" round @click="call(p)">
							{{ p.callTime ? '重新叫号' : '叫号' }}
						</el-button>
						<el-button type="success" :icon="CircleCheck" round plain @click="finish(p)">就诊</el-button>
					</div>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { _axios } from '@/util/axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, Refresh, CircleCheck } from '@element-plus/icons-vue'
import { GENDER, REG_PERIOD } from '@/util/options'

const loading = ref(false)
const list = ref([])
const autoRefresh = ref(false)
let timer = null

const uncalledCount = computed(() => list.value.filter(p => !p.callTime).length)
const calledCount = computed(() => list.value.filter(p => !!p.callTime).length)

function time(t) {
	if (!t) return ''
	const s = String(t).replace('T', ' ')
	return s.length >= 19 ? s.slice(11, 19) : s
}

function load() {
	loading.value = true
	_axios.request({ url: '/sysRegistration/queue', method: 'GET' }).then(({ data }) => {
		if (data.code === 200) list.value = data.content || []
		else ElMessage.error(data.message || '加载队列失败')
	}).catch(() => ElMessage.error('网络异常，无法加载队列')).finally(() => { loading.value = false })
}

function call(p) {
	const fd = new FormData(); fd.append('id', p.id)
	_axios.request({ url: '/sysRegistration/call', method: 'POST', data: fd }).then(({ data }) => {
		if (data.code === 200) {
			ElMessage.success(`请 ${p.patientName} 到 ${p.deptName || ''} ${p.doctorName || ''} 诊室就诊`)
			load()
		} else ElMessage.error(data.message || '叫号失败')
	}).catch(() => ElMessage.error('网络异常，叫号失败'))
}

function finish(p) {
	ElMessageBox.confirm(`确认患者「${p.patientName}」已完成就诊？`, '提示', {
		type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消'
	}).then(() => {
		const fd = new FormData(); fd.append('id', p.id)
		_axios.request({ url: '/sysRegistration/finish', method: 'POST', data: fd }).then(({ data }) => {
			if (data.code === 200) { ElMessage.success('已完成就诊'); load() }
			else ElMessage.error(data.message || '操作失败')
		}).catch(() => ElMessage.error('网络异常，操作失败'))
	}).catch(() => {})
}

watch(autoRefresh, on => {
	clearInterval(timer)
	if (on) timer = setInterval(load, 10000)
})

onMounted(load)
onUnmounted(() => clearInterval(timer))
</script>

<style scoped lang="scss">
.head-actions {
	display: flex;
	align-items: center;
}

.q-stats {
	display: grid;
	grid-template-columns: repeat(3, 1fr);
	gap: 16px;
	margin-bottom: 18px;
}

.q-stat {
	padding: 18px 22px;
	display: flex;
	flex-direction: column;
	gap: 4px;
	border-left: 4px solid var(--his-primary);

	.n {
		font-size: 30px;
		font-weight: 800;
		font-variant-numeric: tabular-nums;
	}

	.l {
		font-size: 13px;
		color: var(--his-text-3);
	}

	&.warn {
		border-left-color: var(--his-warning);
	}

	&.ok {
		border-left-color: var(--his-success);
	}
}

.queue-wrap {
	min-height: 200px;
}

.q-grid {
	display: grid;
	grid-template-columns: repeat(2, 1fr);
	gap: 14px;
}

.q-card {
	display: flex;
	align-items: center;
	gap: 16px;
	padding: 16px 18px;
	transition: box-shadow .2s, transform .2s;

	&:hover {
		box-shadow: var(--his-shadow-hover);
		transform: translateY(-2px);
	}

	&.called {
		background: linear-gradient(180deg, #f6fefb, #fff);
	}
}

.q-no {
	width: 46px;
	height: 46px;
	border-radius: 13px;
	background: linear-gradient(135deg, #f59e0b, #fbbf24);
	color: #fff;
	font-size: 20px;
	font-weight: 800;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;

	&.called {
		background: linear-gradient(135deg, #10b981, #34d399);
	}
}

.q-main {
	flex: 1;
	min-width: 0;
}

.q-name {
	display: flex;
	align-items: center;
	gap: 8px;
	font-size: 16px;
	font-weight: 700;
	margin-bottom: 5px;
}

.q-meta {
	font-size: 13px;
	color: var(--his-text-2);
	margin-bottom: 3px;
}

.q-sub {
	font-size: 12px;
	color: var(--his-text-3);
}

.q-actions {
	display: flex;
	flex-direction: column;
	gap: 8px;
	flex-shrink: 0;
}

.his-empty {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 12px;
	color: var(--his-success);

	p {
		color: var(--his-text-3);
	}
}
</style>
<template>
	<div class="page ai-page">
		<div class="page-header">
			<div class="ph-left">
				<div class="ph-icon"><el-icon><ChatDotRound /></el-icon></div>
				<div>
					<h2>AI 问答</h2>
					<div class="ph-sub">基于大模型的智能助手，支持上下文连续对话</div>
				</div>
			</div>
			<el-button type="danger" plain :icon="Delete" round :disabled="loading" @click="clear">清空对话</el-button>
		</div>

		<div class="chat-card his-card">
			<div class="chat-body" ref="bodyRef">
				<div v-if="messages.length === 0" class="chat-empty">
					<div class="empty-badge"><el-icon :size="40"><ChatDotRound /></el-icon></div>
					<h3>你好，我是 HIS 智能助手</h3>
					<p>可以问我科室导航、挂号流程、就诊须知等问题，试试下面的示例：</p>
					<div class="chips">
						<div v-for="s in suggestions" :key="s" class="chip" @click="useSuggestion(s)">{{ s }}</div>
					</div>
				</div>

				<div v-for="(msg, i) in messages" :key="i" class="msg" :class="msg.role">
					<div class="avatar" :class="msg.role">
						<el-icon v-if="msg.role === 'user'"><User /></el-icon>
						<el-icon v-else-if="msg.role === 'error'"><WarningFilled /></el-icon>
						<el-icon v-else><MagicStick /></el-icon>
					</div>
					<div class="bubble">
						<div class="text">{{ msg.text }}</div>
					</div>
				</div>

				<div v-if="loading" class="msg assistant">
					<div class="avatar assistant"><el-icon><MagicStick /></el-icon></div>
					<div class="bubble">
						<div class="typing"><span></span><span></span><span></span></div>
					</div>
				</div>
			</div>

			<div class="chat-footer">
				<el-input
					v-model="input" type="textarea" :autosize="{ minRows: 1, maxRows: 4 }" resize="none"
					placeholder="请输入问题，Enter 发送，Shift+Enter 换行" :disabled="loading"
					@keydown.enter.exact.prevent="send" />
				<el-button class="send-btn" type="primary" :icon="Promotion" :loading="loading" :disabled="!input.trim()" @click="send">发送</el-button>
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { _axios } from '@/util/axios'
import { ElMessage } from 'element-plus'
import { Promotion, Delete, ChatDotRound, User, MagicStick, WarningFilled } from '@element-plus/icons-vue'

const input = ref('')
const loading = ref(false)
const messages = ref([])
const bodyRef = ref(null)
const memoryId = ref('')

const suggestions = ['医院有哪些科室？', '挂号的流程是怎样的？', '就诊前需要准备什么？', '如何取消挂号？']

onMounted(() => {
	let id = sessionStorage.getItem('ai-memory-id')
	if (!id) {
		id = 'ai-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8)
		sessionStorage.setItem('ai-memory-id', id)
	}
	memoryId.value = id
})

function scrollBottom() {
	nextTick(() => { if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight })
}

function useSuggestion(s) { input.value = s; send() }

function send() {
	const text = input.value.trim()
	if (!text || loading.value) return
	messages.value.push({ role: 'user', text })
	input.value = ''
	loading.value = true
	scrollBottom()

	const formData = new FormData()
	formData.append('memoryId', memoryId.value)
	formData.append('message', text)
	_axios.request({ url: '/ai/chat', method: 'POST', data: formData }).then(response => {
		if (response.data.code === 200) {
			const c = response.data.content
			const answer = typeof c === 'string' ? c : ((c && c.answer) || '')
			messages.value.push({ role: 'assistant', text: answer })
		} else {
			messages.value.push({ role: 'error', text: response.data.message || 'AI 服务返回错误' })
		}
	}).catch(error => {
		messages.value.push({ role: 'error', text: '请求失败：' + (error.message || '网络异常') })
	}).finally(() => { loading.value = false; scrollBottom() })
}

function clear() {
	const formData = new FormData()
	formData.append('memoryId', memoryId.value)
	_axios.request({ url: '/ai/clear', method: 'POST', data: formData }).then(response => {
		if (response.data.code === 200) ElMessage.success(response.data.message || '已清空对话')
	}).catch(() => {})
	messages.value = []
	const id = 'ai-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8)
	sessionStorage.setItem('ai-memory-id', id)
	memoryId.value = id
}
</script>

<style scoped lang="scss">
.ai-page {
	display: flex;
	flex-direction: column;
	height: calc(100vh - 64px);
}

.chat-card {
	flex: 1;
	display: flex;
	flex-direction: column;
	overflow: hidden;
	min-height: 0;
}

.chat-body {
	flex: 1;
	overflow-y: auto;
	padding: 24px;
	background: linear-gradient(180deg, #f8fafc, #f1f5f9);
}

.chat-empty {
	height: 100%;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	text-align: center;
	gap: 10px;

	.empty-badge {
		width: 84px;
		height: 84px;
		border-radius: 24px;
		background: linear-gradient(135deg, #2563eb, #0ea5e9);
		color: #fff;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 14px 34px rgba(37, 99, 235, .3);
		margin-bottom: 8px;
	}

	h3 {
		font-size: 20px;
		font-weight: 700;
	}

	p {
		font-size: 13.5px;
		color: var(--his-text-3);
	}

	.chips {
		display: flex;
		flex-wrap: wrap;
		gap: 10px;
		justify-content: center;
		margin-top: 14px;
		max-width: 560px;
	}

	.chip {
		padding: 9px 16px;
		background: #fff;
		border: 1px solid var(--his-border);
		border-radius: 30px;
		font-size: 13px;
		color: var(--his-text-2);
		cursor: pointer;
		transition: all .2s;

		&:hover {
			border-color: var(--his-primary);
			color: var(--his-primary);
			box-shadow: var(--his-shadow);
		}
	}
}

.msg {
	display: flex;
	gap: 12px;
	margin-bottom: 18px;
	align-items: flex-start;

	&.user {
		flex-direction: row-reverse;

		.bubble {
			background: linear-gradient(135deg, #2563eb, #3b82f6);
			color: #fff;
			border: none;
			border-radius: 14px 4px 14px 14px;
		}
	}

	&.error .bubble {
		background: #fef2f2;
		border-color: #fecaca;
		color: #b91c1c;
	}
}

.avatar {
	width: 38px;
	height: 38px;
	border-radius: 11px;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	font-size: 19px;
	color: #fff;

	&.assistant {
		background: linear-gradient(135deg, #2563eb, #0ea5e9);
	}

	&.user {
		background: linear-gradient(135deg, #64748b, #94a3b8);
	}

	&.error {
		background: linear-gradient(135deg, #ef4444, #f87171);
	}
}

.bubble {
	max-width: 72%;
	padding: 12px 16px;
	background: #fff;
	border: 1px solid var(--his-border);
	border-radius: 4px 14px 14px 14px;
	box-shadow: 0 2px 8px rgba(15, 23, 42, .04);

	.text {
		white-space: pre-wrap;
		word-break: break-word;
		line-height: 1.7;
		font-size: 14px;
	}
}

.typing {
	display: flex;
	gap: 5px;
	padding: 4px 0;

	span {
		width: 7px;
		height: 7px;
		border-radius: 50%;
		background: #94a3b8;
		animation: blink 1.3s infinite ease-in-out;

		&:nth-child(2) {
			animation-delay: .2s;
		}

		&:nth-child(3) {
			animation-delay: .4s;
		}
	}
}

@keyframes blink {

	0%,
	60%,
	100% {
		opacity: .3;
		transform: translateY(0);
	}

	30% {
		opacity: 1;
		transform: translateY(-4px);
	}
}

.chat-footer {
	display: flex;
	align-items: flex-end;
	gap: 12px;
	padding: 16px 20px;
	border-top: 1px solid var(--his-border);
	background: #fff;

	.el-textarea {
		flex: 1;
	}

	.send-btn {
		height: 42px;
		padding: 0 24px;
		border-radius: 10px;
	}
}

:deep(.el-textarea__inner) {
	border-radius: 10px;
	padding: 11px 14px;
}
</style>

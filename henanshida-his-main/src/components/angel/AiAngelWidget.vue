<template>
	<div class="angel-ai-root">
		<!-- 聊天面板 -->
		<section
			class="ab-panel" :class="{ open: store.visible }" :style="panelStyle"
			role="dialog" aria-label="天使宝宝ai 智能助手" :aria-hidden="!store.visible">
			<header class="ab-head">
				<div class="ab-head-bg"></div>
				<div class="ab-head-id">
					<div class="ab-head-avatar"><AngelBaby :size="34" animated /></div>
					<div class="ab-head-meta">
						<div class="ab-head-name">天使宝宝ai</div>
						<div class="ab-head-sub"><span class="dot"></span> 在线 · 智能导诊助手</div>
					</div>
				</div>
				<div class="ab-head-actions">
					<button class="ab-ico" type="button" :title="store.muted ? '开启提示音' : '关闭提示音'"
						:aria-label="store.muted ? '开启提示音' : '关闭提示音'" @click="onToggleMute">
						<el-icon><MuteNotification v-if="store.muted" /><Bell v-else /></el-icon>
					</button>
					<button class="ab-ico" type="button" title="清空对话" aria-label="清空对话"
						:disabled="loading || messages.length === 0" @click="clear">
						<el-icon><Delete /></el-icon>
					</button>
					<button class="ab-ico" type="button" title="收起" aria-label="收起" @click="closePanel">
						<el-icon><Close /></el-icon>
					</button>
				</div>
			</header>

			<div class="ab-body" ref="bodyRef">
				<!-- 空状态 -->
				<div v-if="messages.length === 0 && !loading" class="ab-empty">
					<div class="ab-empty-halo">
						<AngelBaby :size="92" animated />
						<span class="spark s1"></span><span class="spark s2"></span><span class="spark s3"></span>
					</div>
					<h3>你好，我是天使宝宝ai</h3>
					<p>医院里的智能小天使，科室导航、挂号流程、就诊须知都可以问我。</p>
					<div class="ab-chips">
						<button v-for="s in suggestions" :key="s" type="button" class="ab-chip" @click="useSuggestion(s)">{{ s }}</button>
					</div>
				</div>

				<!-- 消息列表 -->
				<div v-for="(m, i) in messages" :key="i" class="ab-msg" :class="m.role">
					<div class="ab-avatar" :class="m.role">
						<AngelBaby v-if="m.role === 'assistant'" :size="26" />
						<el-icon v-else-if="m.role === 'error'"><WarningFilled /></el-icon>
						<el-icon v-else><User /></el-icon>
					</div>
					<div class="ab-bubble-wrap">
						<div class="ab-bubble">
							<div class="ab-text">{{ m.text }}</div>
						</div>
						<div class="ab-meta">
							<span class="ab-time">{{ m.time }}</span>
							<button v-if="m.role === 'assistant'" class="ab-copy" type="button" title="复制回答"
								aria-label="复制回答" @click="copy(m.text)">
								<el-icon><CopyDocument /></el-icon>
							</button>
						</div>
					</div>
				</div>

				<!-- 输入中 -->
				<div v-if="loading" class="ab-msg assistant">
					<div class="ab-avatar assistant"><AngelBaby :size="26" animated /></div>
					<div class="ab-bubble-wrap">
						<div class="ab-bubble"><div class="ab-typing"><span></span><span></span><span></span></div></div>
					</div>
				</div>
			</div>

			<footer class="ab-foot">
				<div class="ab-input-row">
					<el-input
						v-model="input" ref="inputRef" type="textarea" :autosize="{ minRows: 1, maxRows: 3 }" resize="none"
						placeholder="问点什么… Enter 发送" :disabled="loading" @keydown.enter.exact.prevent="send" />
					<button class="ab-send" type="button" :disabled="!input.trim() || loading" aria-label="发送" @click="send">
						<el-icon v-if="!loading"><Promotion /></el-icon>
						<span v-else class="ab-send-spin"></span>
					</button>
				</div>
				<div class="ab-foot-hint">
					<span>Enter 发送 · Shift+Enter 换行</span>
					<span class="ab-foot-brand">天使宝宝ai · 演示助手</span>
				</div>
			</footer>
		</section>

		<!-- 悬浮图标 -->
		<div
			class="ab-launcher" :class="{ active: store.visible, dragging, 'tip-right': tipRight }" :style="launcherStyle"
			role="button" tabindex="0" aria-label="天使宝宝ai 智能助手"
			@pointerdown="onPointerDown" @pointermove="onPointerMove" @pointerup="onPointerUp" @pointercancel="onPointerUp"
			@keydown.enter.prevent="togglePanel" @keydown.space.prevent="togglePanel">
			<span class="ab-aura"></span>
			<span class="ab-orbit o1"></span><span class="ab-orbit o2"></span><span class="ab-orbit o3"></span>
			<span class="ab-tip" role="tooltip">我是天使宝宝，有什么困难随时找我哦~</span>
			<span class="ab-badge"><AngelBaby :size="52" viewBox="30 24 140 140" animated /></span>
			<span v-if="store.unread > 0" class="ab-unread">{{ store.unread > 9 ? '9+' : store.unread }}</span>
		</div>
	</div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, Delete, Bell, MuteNotification, User, WarningFilled, CopyDocument, Promotion } from '@element-plus/icons-vue'
import AngelBaby from './AngelBaby.vue'
import { _axios } from '@/util/axios'
import { useAiWidgetStore } from '@/stores/aiWidget'

const store = useAiWidgetStore()

const BADGE = 64
const PANEL_W = 380
const MSG_KEY = 'angel-ai-msgs'
const POS_KEY = 'angel-ai-pos'

const vw = ref(window.innerWidth)
const vh = ref(window.innerHeight)
const pos = reactive({ x: 0, y: 0 })
const dragging = ref(false)
let dragState = null

const input = ref('')
const loading = ref(false)
const messages = ref([])
const bodyRef = ref(null)
const inputRef = ref(null)
const memoryId = ref('')

const suggestions = ['医院有哪些科室？', '挂号的流程是怎样的？', '就诊前需要准备什么？', '如何取消挂号？']

const clamp = (v, a, b) => Math.min(Math.max(v, a), b)
const nowTime = () => {
	const d = new Date()
	return String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0')
}
const minX = () => (vw.value < 640 ? 8 : 120)

const launcherStyle = computed(() => ({ left: pos.x + 'px', top: pos.y + 'px' }))
// 图标靠屏幕左侧时把 hover 气泡翻到右边，避免溢出视口
const tipRight = computed(() => pos.x < 280)

const panelStyle = computed(() => {
	const W = Math.min(PANEL_W, vw.value - 24)
	if (vw.value < 640) {
		return {
			left: '12px', right: '12px', width: 'auto', top: 'auto',
			bottom: (BADGE + 30) + 'px',
			height: Math.max(320, Math.min(560, vh.value - BADGE - 60)) + 'px'
		}
	}
	// 选上/下中空间更大的一侧，并按可用空间收缩高度，避免面板压住悬浮图标
	const gap = 14, margin = 12
	const aboveSpace = pos.y - gap - margin
	const belowSpace = vh.value - margin - (pos.y + BADGE + gap)
	const H = Math.max(300, Math.min(560, Math.max(aboveSpace, belowSpace)))
	const left = clamp(pos.x + BADGE - W, margin, vw.value - W - margin)
	const top = aboveSpace >= belowSpace
		? clamp(pos.y - gap - H, margin, Math.max(margin, vh.value - H - margin))
		: clamp(pos.y + BADGE + gap, margin, Math.max(margin, vh.value - H - margin))
	return { left: left + 'px', top: top + 'px', width: W + 'px', height: H + 'px' }
})

/* ---------- 位置 / 拖拽 ---------- */
function clampPos() {
	pos.x = clamp(pos.x, minX(), Math.max(minX(), vw.value - BADGE - 8))
	pos.y = clamp(pos.y, 8, Math.max(8, vh.value - BADGE - 8))
}
function loadPos() {
	try {
		const raw = localStorage.getItem(POS_KEY)
		if (raw) {
			const p = JSON.parse(raw)
			if (Number.isFinite(p.x) && Number.isFinite(p.y)) {
				pos.x = p.x; pos.y = p.y; clampPos(); return
			}
		}
	} catch { /* 忽略损坏的本地位置 */ }
	pos.x = clamp(vw.value - BADGE - 24, minX(), vw.value - BADGE - 8)
	pos.y = clamp(vh.value - BADGE - 24, 8, vh.value - BADGE - 8)
}
function savePos() {
	localStorage.setItem(POS_KEY, JSON.stringify({ x: Math.round(pos.x), y: Math.round(pos.y) }))
}
function onPointerDown(e) {
	if (e.pointerType === 'mouse' && e.button !== 0) return
	dragState = { sx: e.clientX, sy: e.clientY, ox: pos.x, oy: pos.y, moved: false }
	try { e.currentTarget.setPointerCapture(e.pointerId) } catch { /* 某些浏览器不支持 */ }
}
function onPointerMove(e) {
	if (!dragState) return
	const dx = e.clientX - dragState.sx
	const dy = e.clientY - dragState.sy
	if (!dragState.moved && (Math.abs(dx) > 5 || Math.abs(dy) > 5)) {
		dragState.moved = true
		dragging.value = true
	}
	if (dragState.moved) {
		pos.x = clamp(dragState.ox + dx, minX(), vw.value - BADGE - 8)
		pos.y = clamp(dragState.oy + dy, 8, vh.value - BADGE - 8)
	}
}
function onPointerUp(e) {
	if (!dragState) return
	const moved = dragState.moved
	dragState = null
	dragging.value = false
	try { e.currentTarget.releasePointerCapture(e.pointerId) } catch { /* noop */ }
	if (moved) savePos()
	else togglePanel()
}

/* ---------- 面板开关 ---------- */
function openPanel() {
	store.open()
	ensureAudio()
	playPop()
	nextTick(() => { scrollBottom(); inputRef.value?.focus?.() })
}
function closePanel() { store.close() }
function togglePanel() { if (store.visible) closePanel(); else openPanel() }

/* ---------- 对话 ---------- */
function scrollBottom() {
	nextTick(() => { const el = bodyRef.value; if (el) el.scrollTop = el.scrollHeight })
}
function persist() {
	try { sessionStorage.setItem(MSG_KEY, JSON.stringify(messages.value.slice(-60))) } catch { /* 存储满忽略 */ }
}
function useSuggestion(s) { input.value = s; send() }
function send() {
	const text = input.value.trim()
	if (!text || loading.value) return
	messages.value.push({ role: 'user', text, time: nowTime() })
	input.value = ''
	loading.value = true
	persist(); scrollBottom()

	const fd = new FormData()
	fd.append('memoryId', memoryId.value)
	fd.append('message', text)
	_axios.request({ url: '/ai/chat', method: 'POST', data: fd }).then(res => {
		if (res.data.code === 200) {
			messages.value.push({ role: 'assistant', text: res.data.content, time: nowTime() })
			if (store.visible) playChime(); else store.bump()
		} else {
			messages.value.push({ role: 'error', text: res.data.message || 'AI 服务返回错误', time: nowTime() })
			if (!store.visible) store.bump()
		}
	}).catch(err => {
		messages.value.push({ role: 'error', text: '请求失败：' + (err.message || '网络异常'), time: nowTime() })
		if (!store.visible) store.bump()
	}).finally(() => { loading.value = false; persist(); scrollBottom() })
}
function clear() {
	const fd = new FormData()
	fd.append('memoryId', memoryId.value)
	_axios.request({ url: '/ai/clear', method: 'POST', data: fd }).catch(() => {})
	messages.value = []
	const id = 'ai-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8)
	sessionStorage.setItem('ai-memory-id', id)
	memoryId.value = id
	persist()
	ElMessage.success('已清空对话')
}
async function copy(text) {
	try {
		await navigator.clipboard.writeText(text)
		ElMessage.success('已复制')
	} catch {
		const ta = document.createElement('textarea')
		ta.value = text
		ta.style.position = 'fixed'
		ta.style.opacity = '0'
		document.body.appendChild(ta)
		ta.select()
		try { document.execCommand('copy'); ElMessage.success('已复制') } catch { ElMessage.error('复制失败') }
		document.body.removeChild(ta)
	}
}

/* ---------- 提示音（WebAudio，无外部资源） ---------- */
let actx = null
function ensureAudio() {
	const AC = window.AudioContext || window.webkitAudioContext
	if (!AC) return null
	if (!actx) actx = new AC()
	if (actx.state === 'suspended') actx.resume().catch(() => {})
	return actx
}
function tone(freq, delay, dur, peak) {
	if (!actx) return
	const o = actx.createOscillator()
	const g = actx.createGain()
	o.type = 'sine'
	o.frequency.value = freq
	o.connect(g); g.connect(actx.destination)
	const t = actx.currentTime + delay
	g.gain.setValueAtTime(0.0001, t)
	g.gain.exponentialRampToValueAtTime(peak, t + 0.02)
	g.gain.exponentialRampToValueAtTime(0.0001, t + dur)
	o.start(t); o.stop(t + dur + 0.03)
}
function playPop() { if (store.muted || !actx) return; tone(540, 0, .12, .05); tone(810, .05, .12, .04) }
function playChime() { if (store.muted || !actx) return; tone(880, 0, .16, .05); tone(1318, .09, .2, .045) }
function onToggleMute() {
	store.toggleMuted()
	if (!store.muted) { ensureAudio(); playPop() }
}

/* ---------- 生命周期 ---------- */
function onResize() { vw.value = window.innerWidth; vh.value = window.innerHeight; clampPos() }
function onKeydown(e) { if (e.key === 'Escape' && store.visible) closePanel() }

onMounted(() => {
	loadPos()
	let id = sessionStorage.getItem('ai-memory-id')
	if (!id) {
		id = 'ai-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8)
		sessionStorage.setItem('ai-memory-id', id)
	}
	memoryId.value = id
	try {
		const raw = sessionStorage.getItem(MSG_KEY)
		if (raw) { const arr = JSON.parse(raw); if (Array.isArray(arr)) messages.value = arr }
	} catch { /* 忽略损坏的会话缓存 */ }
	window.addEventListener('resize', onResize)
	window.addEventListener('keydown', onKeydown)
})
onBeforeUnmount(() => {
	window.removeEventListener('resize', onResize)
	window.removeEventListener('keydown', onKeydown)
	if (actx) { actx.close().catch(() => {}); actx = null }
})
watch(messages, persist, { deep: true })
</script>

<style scoped lang="scss">
.angel-ai-root {
	position: fixed;
	inset: 0;
	pointer-events: none;
	z-index: 1900;

	/* 天使宝宝专属淡紫色板 */
	--ab-lav: #a189c9;
	--ab-lav-deep: #7e5fb5;
	--ab-violet: #8b5cf6;
	--ab-violet-deep: #6d43c8;
	--ab-pink: #e9a8c9;
	--ab-sky: #7cc4f0;
	--ab-mint: #45d0a2;
	--ab-amber: #f6c667;
	--ab-danger: #e5484d;
	--ab-ink: #463a60;
	--ab-ink-2: #6f6390;
	--ab-ink-3: #a79cc4;
	--ab-bg: #faf8ff;
	--ab-surface: #ffffff;
	--ab-line: #ece5f8;
}

.angel-ai-root > * {
	pointer-events: auto;
}

/* ---------- 悬浮图标 ---------- */
.ab-launcher {
	position: fixed;
	width: 64px;
	height: 64px;
	cursor: grab;
	touch-action: none;
	user-select: none;
	-webkit-user-select: none;
	outline: none;

	&.dragging {
		cursor: grabbing;
	}

	&:focus-visible .ab-badge {
		box-shadow: 0 0 0 3px #fff, 0 0 0 6px var(--ab-violet), 0 10px 26px rgba(126, 95, 181, .35);
	}
}

.ab-badge {
	position: absolute;
	inset: 0;
	border-radius: 50%;
	background: radial-gradient(circle at 32% 28%, #ffffff, #f4eefc 62%, #e7dcf7);
	border: 1px solid #e3d7f5;
	box-shadow: 0 10px 26px rgba(126, 95, 181, .35), 0 2px 6px rgba(126, 95, 181, .22);
	display: flex;
	align-items: center;
	justify-content: center;
	animation: ab-bob 3.6s ease-in-out infinite;
	transition: box-shadow .25s;

	:deep(svg) {
		transition: transform .25s cubic-bezier(.34, 1.5, .5, 1);
	}
}

.ab-launcher:hover .ab-badge {
	box-shadow: 0 14px 32px rgba(126, 95, 181, .45), 0 3px 8px rgba(126, 95, 181, .26);

	:deep(svg) {
		transform: scale(1.09);
	}
}

.ab-launcher.active .ab-badge :deep(svg) {
	transform: scale(.9);
}

.ab-launcher.dragging .ab-badge {
	animation: none;

	:deep(svg) {
		transform: scale(1.05);
	}
}

@keyframes ab-bob {

	0%,
	100% {
		transform: translateY(0);
	}

	50% {
		transform: translateY(-5px);
	}
}

.ab-aura {
	position: absolute;
	inset: -6px;
	border-radius: 50%;
	border: 2px solid rgba(161, 137, 201, .45);
	animation: ab-aura 2.8s ease-out infinite;
	pointer-events: none;
}

@keyframes ab-aura {
	0% {
		transform: scale(.85);
		opacity: .7;
	}

	100% {
		transform: scale(1.35);
		opacity: 0;
	}
}

.ab-orbit {
	position: absolute;
	border-radius: 50%;
	pointer-events: none;
}

.o1 {
	width: 8px;
	height: 8px;
	background: var(--ab-pink);
	top: -4px;
	right: 6px;
	animation: ab-tw 2.2s ease-in-out infinite;
}

.o2 {
	width: 6px;
	height: 6px;
	background: var(--ab-sky);
	bottom: 2px;
	left: -6px;
	animation: ab-tw 2.2s .5s ease-in-out infinite;
}

.o3 {
	width: 5px;
	height: 5px;
	background: var(--ab-amber);
	top: 10px;
	left: -10px;
	animation: ab-tw 2.2s 1s ease-in-out infinite;
}

@keyframes ab-tw {

	0%,
	100% {
		transform: scale(.6);
		opacity: .35;
	}

	50% {
		transform: scale(1.15);
		opacity: 1;
	}
}

/* hover 才出现的问候对话框（默认隐藏） */
.ab-tip {
	position: absolute;
	right: calc(100% + 14px);
	top: 50%;
	width: max-content;
	max-width: 190px;
	background: #fff;
	color: var(--ab-ink);
	border: 1px solid var(--ab-line);
	border-radius: 14px;
	padding: 10px 13px;
	font-size: 12.5px;
	font-weight: 600;
	line-height: 1.55;
	text-align: left;
	box-shadow: 0 12px 30px rgba(70, 58, 96, .18);
	opacity: 0;
	visibility: hidden;
	pointer-events: none;
	transform: translateY(-50%) translateX(8px);
	transition: opacity .18s ease, transform .24s cubic-bezier(.34, 1.4, .5, 1), visibility 0s linear .24s;
}

.ab-tip::after {
	content: '';
	position: absolute;
	top: 50%;
	right: -6px;
	margin-top: -6px;
	width: 12px;
	height: 12px;
	background: #fff;
	border-right: 1px solid var(--ab-line);
	border-top: 1px solid var(--ab-line);
	transform: rotate(45deg);
}

.ab-launcher:hover .ab-tip,
.ab-launcher:focus-visible .ab-tip {
	opacity: 1;
	visibility: visible;
	pointer-events: auto;
	transform: translateY(-50%) translateX(0);
	transition: opacity .18s ease, transform .24s cubic-bezier(.34, 1.4, .5, 1), visibility 0s;
}

.ab-launcher.active .ab-tip,
.ab-launcher.dragging .ab-tip {
	opacity: 0;
	visibility: hidden;
	pointer-events: none;
}

.ab-launcher.tip-right .ab-tip {
	right: auto;
	left: calc(100% + 14px);
	transform: translateY(-50%) translateX(-8px);
}

.ab-launcher.tip-right:hover .ab-tip,
.ab-launcher.tip-right:focus-visible .ab-tip {
	transform: translateY(-50%) translateX(0);
}

.ab-launcher.tip-right .ab-tip::after {
	right: auto;
	left: -6px;
	border-right: none;
	border-top: none;
	border-left: 1px solid var(--ab-line);
	border-bottom: 1px solid var(--ab-line);
}

.ab-unread {
	position: absolute;
	top: -4px;
	right: -4px;
	min-width: 20px;
	height: 20px;
	padding: 0 5px;
	border-radius: 999px;
	background: var(--ab-danger);
	color: #fff;
	font-size: 11px;
	font-weight: 700;
	display: flex;
	align-items: center;
	justify-content: center;
	border: 2px solid #fff;
	box-shadow: 0 3px 8px rgba(229, 72, 77, .4);
}

/* ---------- 聊天面板 ---------- */
.ab-panel {
	position: fixed;
	display: flex;
	flex-direction: column;
	overflow: hidden;
	background: var(--ab-surface);
	border: 1px solid var(--ab-line);
	border-radius: 20px;
	box-shadow: 0 24px 64px rgba(70, 58, 96, .26), 0 4px 14px rgba(70, 58, 96, .12);
	opacity: 0;
	visibility: hidden;
	transform: translateY(14px) scale(.96);
	transform-origin: bottom right;
	transition: opacity .2s ease, transform .26s cubic-bezier(.34, 1.3, .4, 1), visibility 0s linear .26s;

	&.open {
		opacity: 1;
		visibility: visible;
		transform: none;
		transition: opacity .2s ease, transform .26s cubic-bezier(.34, 1.3, .4, 1), visibility 0s;
	}
}

.ab-head {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 10px;
	padding: 14px 14px 14px 16px;
	color: #fff;
	background: linear-gradient(120deg, #a189c9, #8b5cf6 52%, #6d43c8);
	overflow: hidden;
	flex-shrink: 0;
}

.ab-head-bg {
	position: absolute;
	inset: 0;
	background:
		radial-gradient(120px 60px at 88% -10%, rgba(255, 255, 255, .35), transparent 70%),
		radial-gradient(90px 50px at 10% 120%, rgba(233, 168, 201, .4), transparent 70%);
	pointer-events: none;
}

.ab-head-id {
	position: relative;
	display: flex;
	align-items: center;
	gap: 10px;
	min-width: 0;
}

.ab-head-avatar {
	width: 42px;
	height: 42px;
	border-radius: 13px;
	background: rgba(255, 255, 255, .92);
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 4px 12px rgba(70, 58, 96, .22);
	flex-shrink: 0;
}

.ab-head-name {
	font-size: 15.5px;
	font-weight: 800;
	letter-spacing: .4px;
}

.ab-head-sub {
	display: flex;
	align-items: center;
	gap: 6px;
	font-size: 11.5px;
	opacity: .92;
	margin-top: 2px;

	.dot {
		width: 7px;
		height: 7px;
		border-radius: 50%;
		background: var(--ab-mint);
		box-shadow: 0 0 0 3px rgba(69, 208, 162, .28);
	}
}

.ab-head-actions {
	position: relative;
	display: flex;
	gap: 4px;
}

.ab-ico {
	width: 30px;
	height: 30px;
	border-radius: 9px;
	border: none;
	background: rgba(255, 255, 255, .14);
	color: #fff;
	cursor: pointer;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 15px;
	transition: background .18s, transform .18s;

	&:hover {
		background: rgba(255, 255, 255, .28);
		transform: translateY(-1px);
	}

	&:disabled {
		opacity: .45;
		cursor: not-allowed;
		transform: none;
	}
}

.ab-body {
	flex: 1;
	overflow-y: auto;
	padding: 16px 14px;
	background: linear-gradient(180deg, #fbf9ff, #f5f1fc);
	min-height: 0;

	&::-webkit-scrollbar {
		width: 8px;
	}

	&::-webkit-scrollbar-track {
		background: transparent;
	}

	&::-webkit-scrollbar-thumb {
		background: #d9cbee;
		border-radius: 8px;
		border: 2px solid transparent;
		background-clip: content-box;
	}

	&::-webkit-scrollbar-thumb:hover {
		background: var(--ab-lav);
		background-clip: content-box;
	}
}

.ab-empty {
	height: 100%;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	text-align: center;
	gap: 8px;
	padding: 8px 6px;

	h3 {
		font-size: 17px;
		font-weight: 800;
		color: var(--ab-ink);
	}

	p {
		font-size: 12.5px;
		color: var(--ab-ink-2);
		line-height: 1.6;
		max-width: 270px;
	}
}

.ab-empty-halo {
	position: relative;
	width: 120px;
	height: 120px;
	border-radius: 50%;
	background: radial-gradient(circle, #efe6fb, #e2d4f6 60%, transparent 72%);
	display: flex;
	align-items: center;
	justify-content: center;
	margin-bottom: 4px;
}

.spark {
	position: absolute;
	border-radius: 50%;
}

.spark.s1 {
	width: 8px;
	height: 8px;
	background: var(--ab-pink);
	top: 6px;
	right: 10px;
	animation: ab-tw 2.4s infinite;
}

.spark.s2 {
	width: 6px;
	height: 6px;
	background: var(--ab-sky);
	bottom: 12px;
	left: 4px;
	animation: ab-tw 2.4s .6s infinite;
}

.spark.s3 {
	width: 5px;
	height: 5px;
	background: var(--ab-amber);
	top: 24px;
	left: -4px;
	animation: ab-tw 2.4s 1.1s infinite;
}

.ab-chips {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;
	justify-content: center;
	margin-top: 10px;
}

.ab-chip {
	border: 1px solid #e2d6f4;
	background: #fff;
	color: var(--ab-lav-deep);
	font-size: 12px;
	padding: 7px 12px;
	border-radius: 999px;
	cursor: pointer;
	transition: all .18s;

	&:hover {
		background: linear-gradient(135deg, #8b5cf6, #6d43c8);
		color: #fff;
		border-color: transparent;
		box-shadow: 0 6px 14px rgba(109, 67, 200, .28);
		transform: translateY(-1px);
	}
}

.ab-msg {
	display: flex;
	gap: 9px;
	margin-bottom: 14px;
	align-items: flex-start;

	&.user {
		flex-direction: row-reverse;

		.ab-bubble-wrap {
			align-items: flex-end;
		}

		.ab-bubble {
			background: linear-gradient(135deg, #a189c9, #8b5cf6);
			border: none;
			color: #fff;
			border-radius: 14px 4px 14px 14px;
		}
	}

	&.error .ab-bubble {
		background: #fef2f2;
		border-color: #fecaca;
		color: #b91c1c;
	}

	&:hover .ab-copy {
		opacity: 1;
	}
}

.ab-avatar {
	width: 32px;
	height: 32px;
	border-radius: 10px;
	flex-shrink: 0;
	display: flex;
	align-items: center;
	justify-content: center;
	color: #fff;
	font-size: 16px;

	&.assistant {
		background: linear-gradient(135deg, #f3edfc, #e5d8f6);
		color: var(--ab-lav);
		border: 1px solid #e3d7f5;
	}

	&.user {
		background: linear-gradient(135deg, #8b5cf6, #6d43c8);
	}

	&.error {
		background: linear-gradient(135deg, #e5484d, #f0716f);
	}
}

.ab-bubble-wrap {
	max-width: 76%;
	display: flex;
	flex-direction: column;
	gap: 4px;
}

.ab-bubble {
	position: relative;
	padding: 10px 13px;
	background: #fff;
	border: 1px solid var(--ab-line);
	border-radius: 4px 14px 14px 14px;
	box-shadow: 0 2px 8px rgba(70, 58, 96, .06);
}

.ab-text {
	white-space: pre-wrap;
	word-break: break-word;
	line-height: 1.65;
	font-size: 13px;
}

.ab-meta {
	display: flex;
	align-items: center;
	gap: 6px;
	padding: 0 4px;
}

.ab-time {
	font-size: 10.5px;
	color: var(--ab-ink-3);
}

.ab-copy {
	width: 22px;
	height: 22px;
	border-radius: 7px;
	border: 1px solid var(--ab-line);
	background: #fff;
	color: var(--ab-ink-3);
	cursor: pointer;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 12px;
	opacity: 0;
	transition: opacity .15s, color .15s;

	&:hover {
		color: var(--ab-violet);
		border-color: #d9cbee;
	}
}

.ab-typing {
	display: flex;
	gap: 5px;
	padding: 3px 2px;

	span {
		width: 7px;
		height: 7px;
		border-radius: 50%;
		background: var(--ab-lav);
		animation: ab-blinkdot 1.3s infinite ease-in-out;

		&:nth-child(2) {
			animation-delay: .2s;
		}

		&:nth-child(3) {
			animation-delay: .4s;
		}
	}
}

@keyframes ab-blinkdot {

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

.ab-foot {
	flex-shrink: 0;
	border-top: 1px solid var(--ab-line);
	background: #fff;
	padding: 10px 12px 8px;
}

.ab-input-row {
	display: flex;
	gap: 8px;
	align-items: flex-end;

	:deep(.el-textarea__inner) {
		border-radius: 12px;
		padding: 9px 12px;
		font-size: 13px;
		box-shadow: 0 0 0 1px #e6ddf5 inset;
		transition: box-shadow .18s;
		background: #fbf9ff;
	}

	:deep(.el-textarea__inner:focus) {
		box-shadow: 0 0 0 1.5px var(--ab-violet) inset;
		background: #fff;
	}
}

.ab-send {
	flex-shrink: 0;
	width: 40px;
	height: 40px;
	border-radius: 12px;
	border: none;
	cursor: pointer;
	background: linear-gradient(135deg, #a189c9, #8b5cf6 60%, #6d43c8);
	color: #fff;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 17px;
	box-shadow: 0 6px 14px rgba(109, 67, 200, .3);
	transition: transform .15s, box-shadow .2s, opacity .2s;

	&:hover:not(:disabled) {
		transform: translateY(-1px);
		box-shadow: 0 9px 18px rgba(109, 67, 200, .4);
	}

	&:disabled {
		opacity: .45;
		cursor: not-allowed;
		box-shadow: none;
	}
}

.ab-send-spin {
	width: 16px;
	height: 16px;
	border-radius: 50%;
	border: 2px solid rgba(255, 255, 255, .4);
	border-top-color: #fff;
	animation: ab-spin .8s linear infinite;
}

@keyframes ab-spin {
	to {
		transform: rotate(360deg);
	}
}

.ab-foot-hint {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-top: 7px;
	font-size: 10.5px;
	color: var(--ab-ink-3);
	padding: 0 2px;
}

.ab-foot-brand {
	font-weight: 600;
}

@media (prefers-reduced-motion: reduce) {

	.ab-badge,
	.ab-aura,
	.ab-orbit,
	.spark {
		animation: none;
	}
}
</style>
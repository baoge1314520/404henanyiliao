<template>
	<div class="login-page">
		<!-- 左侧：品牌 + 医院插画 -->
		<div class="brand">
			<div class="sky-glow"></div>
			<div class="brand-top">
				<div class="logo-badge">
					<el-icon :size="24"><FirstAidKit /></el-icon>
				</div>
				<div class="logo-text">
					<span class="cn">河南师大医院</span>
					<span class="en">HOSPITAL INFORMATION SYSTEM</span>
				</div>
			</div>

			<!-- 医院插画 -->
			<div class="illustration">
				<svg viewBox="0 0 520 420" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
					<defs>
						<linearGradient id="bld" x1="0" y1="0" x2="0" y2="1">
							<stop offset="0" stop-color="#ffffff" stop-opacity=".85" />
							<stop offset="1" stop-color="#ffffff" stop-opacity=".45" />
						</linearGradient>
						<linearGradient id="tower" x1="0" y1="0" x2="0" y2="1">
							<stop offset="0" stop-color="#ffffff" stop-opacity=".95" />
							<stop offset="1" stop-color="#ffffff" stop-opacity=".6" />
						</linearGradient>
					</defs>
					<!-- 地面波浪 -->
					<path d="M0,362 C120,342 200,382 300,364 C400,348 460,380 520,362 L520,420 L0,420 Z" fill="#ffffff" opacity=".28" />
					<path d="M0,378 C140,360 240,396 340,380 C430,366 480,392 520,378 L520,420 L0,420 Z" fill="#ffffff" opacity=".22" />
					<!-- 树 -->
					<g fill="#1e40af" opacity=".32">
						<rect x="63" y="322" width="13" height="66" rx="4" />
						<circle cx="69" cy="316" r="46" />
						<rect x="443" y="318" width="13" height="70" rx="4" />
						<circle cx="449" cy="312" r="52" />
					</g>
					<!-- 主楼 -->
					<rect x="110" y="152" width="300" height="210" rx="12" fill="url(#bld)" />
					<!-- 中央塔楼 -->
					<rect x="210" y="88" width="100" height="130" rx="12" fill="url(#tower)" />
					<!-- 十字 -->
					<g fill="#2563eb">
						<rect x="248" y="106" width="24" height="66" rx="5" />
						<rect x="227" y="127" width="66" height="24" rx="5" />
					</g>
					<!-- 窗户 -->
					<g fill="#8ab8e8">
						<rect x="138" y="182" width="36" height="36" rx="5" />
						<rect x="186" y="182" width="36" height="36" rx="5" />
						<rect x="298" y="182" width="36" height="36" rx="5" />
						<rect x="346" y="182" width="36" height="36" rx="5" />
						<rect x="138" y="238" width="36" height="36" rx="5" />
						<rect x="186" y="238" width="36" height="36" rx="5" />
						<rect x="298" y="238" width="36" height="36" rx="5" />
						<rect x="346" y="238" width="36" height="36" rx="5" />
					</g>
					<!-- 门 -->
					<rect x="242" y="292" width="36" height="70" rx="6" fill="#5f93c8" />
					<rect x="258" y="292" width="4" height="70" fill="#ffffff" opacity=".5" />
				</svg>
				<div class="pulse-ring r1"></div>
				<div class="pulse-ring r2"></div>
			</div>

			<div class="brand-copy">
				<div class="eyebrow">SMART MEDICAL</div>
				<h1>用科技守护每一份健康</h1>
				<p>简洁、高效、安全的一体化医院信息管理平台</p>
			</div>
			<div class="brand-foot">© 2026 河南师范大学 · 医院信息系统实训项目</div>
		</div>

		<!-- 右侧：登录 / 注册 -->
		<div class="form-side">
			<div class="form-card">
				<div class="form-logo">
					<el-icon :size="26"><FirstAidKit /></el-icon>
				</div>
				<h2 class="form-title">{{ mode === 'login' ? '欢迎登录' : '创建账号' }}</h2>
				<p class="form-sub">{{ mode === 'login' ? '河南师大医院管理系统' : '注册后即可登录使用全部功能' }}</p>

				<!-- 模式切换 -->
				<div class="mode-switch">
					<button :class="{ on: mode === 'login' }" @click="switchMode('login')">登 录</button>
					<button :class="{ on: mode === 'register' }" @click="switchMode('register')">注 册</button>
					<span class="thumb" :style="{ transform: mode === 'login' ? 'translateX(0)' : 'translateX(100%)' }"></span>
				</div>

				<!-- 登录表单 -->
					<el-form v-if="mode === 'login'" key="login" ref="loginRef" :model="loginForm" :rules="loginRules"
						size="large" @keyup.enter="doLogin">
						<el-form-item prop="username">
							<el-input v-model="loginForm.username" placeholder="手机号 / 邮箱 / 工号" :prefix-icon="User" clearable />
						</el-form-item>
						<el-form-item prop="password">
							<el-input v-model="loginForm.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password clearable />
						</el-form-item>
						<div class="row-between">
							<el-checkbox v-model="remember">记住登录状态</el-checkbox>
							<button class="link-btn" type="button" @click="forgot">忘记密码?</button>
						</div>
						<el-button class="submit-btn" type="primary" :loading="loading" @click="doLogin">
							{{ loading ? '登录中…' : '立即登录' }} <el-icon v-if="!loading"><Right /></el-icon>
						</el-button>
						<div class="demo-card">
							<div class="demo-title"><el-icon><InfoFilled /></el-icon> 演示账号（密码均为 123456）</div>
							<div class="demo-accounts">
								<button v-for="a in demoAccounts" :key="a" type="button" @click="fillDemo(a)">{{ a }}</button>
							</div>
						</div>
					</el-form>

					<!-- 注册表单 -->
					<el-form v-else key="register" ref="regRef" :model="regForm" :rules="regRules" size="large" @keyup.enter="doRegister">
						<el-form-item prop="realName">
							<el-input v-model="regForm.realName" placeholder="真实姓名" :prefix-icon="Postcard" clearable />
						</el-form-item>
						<el-form-item prop="phone">
							<el-input v-model="regForm.phone" placeholder="手机号（用于登录）" :prefix-icon="Iphone" maxlength="11" clearable />
						</el-form-item>
						<el-form-item prop="password">
							<el-input v-model="regForm.password" type="password" placeholder="设置密码（6-32 位）" :prefix-icon="Lock" show-password clearable />
						</el-form-item>
						<el-form-item prop="confirm">
							<el-input v-model="regForm.confirm" type="password" placeholder="确认密码" :prefix-icon="Lock" show-password clearable />
						</el-form-item>
						<el-form-item prop="agree" class="agree-item">
							<el-checkbox v-model="regForm.agree">我已阅读并同意</el-checkbox>
							<button class="link-btn" type="button" @click="terms">《系统使用规范》</button>
						</el-form-item>
						<el-button class="submit-btn" type="primary" :loading="loading" @click="doRegister">
							{{ loading ? '注册中…' : '注册并登录' }} <el-icon v-if="!loading"><Right /></el-icon>
						</el-button>
						<div class="form-tip">注册即创建一个启用状态的医护账号，手机号将作为登录账号。</div>
					</el-form>
			</div>
			<div class="side-foot">© 2026 河南师大医院 · 毕业设计演示系统</div>
		</div>
	</div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Lock, FirstAidKit, InfoFilled, Right, Iphone, Postcard } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const mode = ref('login')
const loading = ref(false)
const remember = ref(true)
const demoAccounts = ['13800000001', '13800000002', '13800000003']

const loginRef = ref(null)
const loginForm = reactive({ username: '', password: '' })
const loginRules = {
	username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
	password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const regRef = ref(null)
const regForm = reactive({ realName: '', phone: '', password: '', confirm: '', agree: false })
const regRules = {
	realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
	phone: [
		{ required: true, message: '请输入手机号', trigger: 'blur' },
		{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
	],
	password: [
		{ required: true, message: '请设置密码', trigger: 'blur' },
		{ min: 6, max: 32, message: '密码长度需为 6-32 位', trigger: 'blur' }
	],
	confirm: [
		{ required: true, message: '请再次输入密码', trigger: 'blur' },
		{
			validator: (rule, value, cb) => (value === regForm.password ? cb() : cb(new Error('两次输入的密码不一致'))),
			trigger: 'blur'
		}
	],
	agree: [
		{
			validator: (rule, value, cb) => (value ? cb() : cb(new Error('请先同意系统使用规范'))),
			trigger: 'change'
		}
	]
}

function switchMode(m) {
	mode.value = m
}
function fillDemo(a) {
	loginForm.username = a
	loginForm.password = '123456'
}
function forgot() {
	ElMessageBox.alert('演示环境暂不开放自助重置，请联系系统管理员重置密码。', '忘记密码', { confirmButtonText: '知道了' })
}
function terms() {
	ElMessageBox.alert('本系统为教学实训演示平台，数据仅供学习使用；请妥善保管账号，勿用于真实医疗场景。', '系统使用规范', { confirmButtonText: '我已了解' })
}

function redirectAfterAuth() {
	const redirect = route.query.redirect || '/dashboard'
	router.replace(redirect)
}

async function doLogin() {
	if (!loginRef.value) return
	await loginRef.value.validate(async valid => {
		if (!valid) return
		loading.value = true
		try {
			const res = await userStore.login(loginForm.username.trim(), loginForm.password)
			if (res.ok) {
				if (remember.value) localStorage.setItem('his_remember_user', loginForm.username.trim())
				else localStorage.removeItem('his_remember_user')
				ElMessage.success('登录成功，欢迎回来')
				redirectAfterAuth()
			} else {
				ElMessage.error(res.message)
			}
		} catch {
			ElMessage.error('网络异常，无法连接服务器')
		} finally {
			loading.value = false
		}
	})
}

async function doRegister() {
	if (!regRef.value) return
	await regRef.value.validate(async valid => {
		if (!valid) return
		loading.value = true
		try {
			const res = await userStore.register({
				realName: regForm.realName.trim(),
				phone: regForm.phone.trim(),
				password: regForm.password,
				confirm: regForm.confirm
			})
			if (res.ok) {
				ElMessage.success('注册成功，已自动登录')
				redirectAfterAuth()
			} else {
				ElMessage.error(res.message)
			}
		} catch {
			ElMessage.error('网络异常，无法连接服务器')
		} finally {
			loading.value = false
		}
	})
}

onMounted(() => {
	const saved = localStorage.getItem('his_remember_user')
	if (saved) loginForm.username = saved
})
</script>

<style scoped lang="scss">
.login-page {
	display: flex;
	height: 100vh;
	overflow: hidden;
	background: #fff;
}

/* ---------- 左侧品牌 ---------- */
.brand {
	position: relative;
	width: 50%;
	min-width: 480px;
	display: flex;
	flex-direction: column;
	justify-content: space-between;
	padding: 44px 52px 30px;
	overflow: hidden;
	background: linear-gradient(180deg, #e8f2fd 0%, #cfe3f8 42%, #a9cbec 100%);
}

.sky-glow {
	position: absolute;
	width: 420px;
	height: 420px;
	left: -120px;
	top: -140px;
	border-radius: 50%;
	background: radial-gradient(circle, rgba(255, 255, 255, .8), transparent 68%);
	pointer-events: none;
}

.brand-top {
	position: relative;
	z-index: 2;
	display: flex;
	align-items: center;
	gap: 12px;
}

.logo-badge {
	width: 46px;
	height: 46px;
	border-radius: 13px;
	background: linear-gradient(135deg, #2563eb, #1d4ed8);
	color: #fff;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 8px 18px rgba(37, 99, 235, .32);
}

.logo-text {
	display: flex;
	flex-direction: column;

	.cn {
		font-size: 18px;
		font-weight: 800;
		color: #1e3a8a;
		letter-spacing: .5px;
	}

	.en {
		font-size: 9.5px;
		letter-spacing: 2px;
		color: #5b7ba6;
		margin-top: 2px;
	}
}

.illustration {
	position: relative;
	z-index: 1;
	flex: 1;
	display: flex;
	align-items: center;
	justify-content: center;
	min-height: 0;

	svg {
		width: min(100%, 520px);
		height: auto;
		filter: drop-shadow(0 18px 30px rgba(30, 64, 175, .16));
	}
}

.pulse-ring {
	position: absolute;
	border-radius: 50%;
	border: 2px solid rgba(37, 99, 235, .25);
	animation: pulse 3.4s ease-out infinite;
	pointer-events: none;
}

.r1 {
	width: 120px;
	height: 120px;
	left: 12%;
	top: 24%;
}

.r2 {
	width: 80px;
	height: 80px;
	right: 14%;
	top: 40%;
	animation-delay: 1.4s;
}

@keyframes pulse {
	0% {
		transform: scale(.6);
		opacity: .8;
	}

	100% {
		transform: scale(1.5);
		opacity: 0;
	}
}

.brand-copy {
	position: relative;
	z-index: 2;

	.eyebrow {
		font-size: 11px;
		letter-spacing: 4px;
		color: #4a6da0;
		margin-bottom: 10px;
	}

	h1 {
		font-size: 32px;
		font-weight: 800;
		color: #16305c;
		letter-spacing: 1px;
		margin-bottom: 10px;
	}

	p {
		font-size: 14px;
		color: #47618c;
	}
}

.brand-foot {
	position: relative;
	z-index: 2;
	margin-top: 18px;
	font-size: 11.5px;
	color: #6c86ad;
}

/* ---------- 右侧表单 ---------- */
.form-side {
	flex: 1;
	position: relative;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 40px;
	background: #fff;
}

.form-card {
	width: 100%;
	max-width: 400px;
}

.form-logo {
	width: 52px;
	height: 52px;
	border-radius: 15px;
	background: linear-gradient(135deg, #2563eb, #1d4ed8);
	color: #fff;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 10px 22px rgba(37, 99, 235, .3);
	margin-bottom: 22px;
}

.form-title {
	font-size: 27px;
	font-weight: 800;
	color: #1e293b;
	margin-bottom: 6px;
}

.form-sub {
	font-size: 13.5px;
	color: #94a3b8;
	margin-bottom: 24px;
}

.mode-switch {
	position: relative;
	display: grid;
	grid-template-columns: 1fr 1fr;
	background: #f1f5f9;
	border-radius: 12px;
	padding: 4px;
	margin-bottom: 24px;

	button {
		position: relative;
		z-index: 2;
		border: none;
		background: transparent;
		height: 38px;
		font-size: 14.5px;
		font-weight: 600;
		color: #64748b;
		cursor: pointer;
		border-radius: 9px;
		transition: color .25s;
		letter-spacing: 2px;

		&.on {
			color: #1d4ed8;
		}
	}

	.thumb {
		position: absolute;
		z-index: 1;
		top: 4px;
		left: 4px;
		width: calc(50% - 4px);
		height: 38px;
		background: #fff;
		border-radius: 9px;
		box-shadow: 0 3px 10px rgba(30, 64, 175, .14);
		transition: transform .28s cubic-bezier(.4, 0, .2, 1);
	}
}

.row-between {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin: -4px 0 18px;
}

.link-btn {
	border: none;
	background: none;
	color: #2563eb;
	font-size: 13px;
	cursor: pointer;
	padding: 0;

	&:hover {
		text-decoration: underline;
	}
}

.agree-item {
	margin-bottom: 14px;

	:deep(.el-form-item__content) {
		display: flex;
		align-items: center;
		gap: 4px;
	}
}

.submit-btn {
	width: 100%;
	height: 47px;
	font-size: 15.5px;
	font-weight: 700;
	letter-spacing: 2px;
	border-radius: 11px;
	background: linear-gradient(135deg, #2563eb, #1d4ed8);
	border: none;
	box-shadow: 0 10px 22px rgba(37, 99, 235, .3);
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 6px;
	transition: transform .15s, box-shadow .2s;

	&:hover {
		transform: translateY(-1px);
		box-shadow: 0 14px 26px rgba(37, 99, 235, .38);
	}
}

.demo-card {
	margin-top: 22px;
	background: #f8fafc;
	border: 1px solid #e8eef6;
	border-radius: 12px;
	padding: 13px 16px;

	.demo-title {
		display: flex;
		align-items: center;
		gap: 6px;
		font-size: 12px;
		color: #7c8ba1;
		margin-bottom: 10px;
	}

	.demo-accounts {
		display: flex;
		flex-wrap: wrap;
		gap: 8px;

		button {
			border: 1px solid #dbe6f5;
			background: #fff;
			color: #2563eb;
			font-size: 12px;
			font-family: Consolas, monospace;
			border-radius: 8px;
			padding: 4px 10px;
			cursor: pointer;
			transition: all .18s;

			&:hover {
				background: #2563eb;
				color: #fff;
				border-color: #2563eb;
			}
		}
	}
}

.form-tip {
	margin-top: 16px;
	font-size: 12px;
	color: #b0bccd;
	text-align: center;
	line-height: 1.7;
}

.side-foot {
	position: absolute;
	bottom: 20px;
	left: 0;
	right: 0;
	text-align: center;
	font-size: 11.5px;
	color: #c3cdd9;
}

:deep(.el-input__wrapper) {
	border-radius: 11px;
	padding: 4px 14px;
	box-shadow: 0 0 0 1px #e2e8f0 inset;
	transition: box-shadow .2s;

	&:hover,
	&.is-focus {
		box-shadow: 0 0 0 1px #2563eb inset;
	}
}

:deep(.el-form-item) {
	margin-bottom: 20px;
}

/* 窄屏：隐藏插画列，表单居中 */
@media (max-width: 900px) {
	.brand {
		display: none;
	}

	.form-side {
		background: linear-gradient(180deg, #eef9ff, #fff 40%);
	}
}
</style>

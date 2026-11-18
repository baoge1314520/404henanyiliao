import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getRole } from '@/util/auth'

const router = createRouter({
	history: createWebHistory(import.meta.env.BASE_URL),
	routes: [
		{
			path: '/login',
			name: 'login',
			component: () => import('@/views/login'),
			meta: { public: true, title: '登录' }
		},
		{
			path: '/',
			redirect: '/dashboard'
		},
		{
			path: '/home',
			component: () => import('@/views/home'),
			redirect: '/dashboard',
			children: [
				{ path: '/dashboard', name: 'dashboard', component: () => import('@/views/dashboard'), meta: { title: '工作台', group: '概览' } },
				{ path: '/dept', name: 'dept', component: () => import('@/views/system/dept'), meta: { title: '科室管理', group: '系统管理' } },
				{ path: '/doctor', name: 'doctor', component: () => import('@/views/system/doctor'), meta: { title: '医生管理', group: '系统管理' } },
				{ path: '/title', name: 'title', component: () => import('@/views/system/title'), meta: { title: '职称管理', group: '系统管理' } },
				{ path: '/role', name: 'role', component: () => import('@/views/system/role'), meta: { title: '角色管理', group: '系统管理' } },
				{ path: '/menu', name: 'menu', component: () => import('@/views/system/menu'), meta: { title: '菜单管理', group: '系统管理' } },
				{ path: '/dict', name: 'dict', component: () => import('@/views/system/dict'), meta: { title: '字典管理', group: '系统管理' } },
				{ path: '/patient', name: 'patient', component: () => import('@/views/biz/patient'), meta: { title: '患者管理', group: '挂号就诊' } },
				{ path: '/registration', name: 'registration', component: () => import('@/views/biz/registration'), meta: { title: '挂号管理', group: '挂号就诊' } },
				{ path: '/schedule', name: 'schedule', component: () => import('@/views/biz/schedule'), meta: { title: '排班管理', group: '挂号就诊' } },
				{ path: '/queue', name: 'queue', component: () => import('@/views/biz/queue'), meta: { title: '叫号队列', group: '挂号就诊' } },
				{ path: '/triage', name: 'triage', component: () => import('@/views/biz/triage'), meta: { title: '分诊管理', group: '挂号就诊' } }
			]
		},
		{
			path: '/patient',
			component: () => import('@/views/patient/shell.vue'),
			redirect: '/patient/home',
			children: [
				{ path: '/patient/home', name: 'patientHome', component: () => import('@/views/patient/home.vue'), meta: { title: '首页', patient: true } },
				{ path: '/patient/hall', name: 'patientHall', component: () => import('@/views/patient/hall.vue'), meta: { title: '挂号大厅', patient: true } },
				{ path: '/patient/records', name: 'patientRecords', component: () => import('@/views/patient/records.vue'), meta: { title: '我的挂号', patient: true } },
				{ path: '/patient/profile', name: 'patientProfile', component: () => import('@/views/patient/profile.vue'), meta: { title: '个人中心', patient: true } }
			]
		},
		{
			path: '/doc',
			component: () => import('@/views/doc/shell.vue'),
			redirect: '/doc/home',
			children: [
				{ path: '/doc/home', name: 'docHome', component: () => import('@/views/doc/home.vue'), meta: { title: '工作台', doctor: true } },
				{ path: '/doc/schedule', name: 'docSchedule', component: () => import('@/views/doc/schedule.vue'), meta: { title: '我的排班', doctor: true } },
				{ path: '/doc/today', name: 'docToday', component: () => import('@/views/doc/today.vue'), meta: { title: '今日就诊', doctor: true } },
				{ path: '/doc/history', name: 'docHistory', component: () => import('@/views/doc/history.vue'), meta: { title: '历史记录', doctor: true } }
			]
		},
		{
			path: '/:pathMatch(.*)*',
			redirect: '/dashboard'
		}
	]
})

// 全局登录守卫：管理端、医生端与患者端按角色分流
router.beforeEach((to) => {
	const hasToken = !!getToken()
	const role = getRole()
	if (to.meta.public) {
		if (hasToken && to.path === '/login') {
			const home = role === 'patient' ? '/patient/home' : role === 'doctor' ? '/doc/home' : '/dashboard'
			return { path: home }
		}
		return true
	}
	if (to.meta.patient) {
		if (!hasToken || role !== 'patient') {
			return { path: '/login', query: { redirect: to.fullPath, role: 'patient' } }
		}
		return true
	}
	if (to.meta.doctor) {
		if (!hasToken || role !== 'doctor') {
			return { path: '/login', query: { redirect: to.fullPath, role: 'doctor' } }
		}
		return true
	}
	if (!hasToken) {
		return { path: '/login', query: { redirect: to.fullPath } }
	}
	// 患者/医生会话不允许进入管理端页面
	if (role === 'patient') {
		return { path: '/patient/home' }
	}
	if (role === 'doctor') {
		return { path: '/doc/home' }
	}
	return true
})

router.afterEach((to) => {
	const brand = to.meta.doctor ? '医生工作站' : to.meta.patient ? '智慧院区 HIS' : '河南师大 HIS'
	const title = to.meta.title ? `${to.meta.title} · ${brand}` : brand
	document.title = title
})

export default router
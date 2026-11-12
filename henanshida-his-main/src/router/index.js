import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/util/auth'

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
			path: '/:pathMatch(.*)*',
			redirect: '/dashboard'
		}
	]
})

// 全局登录守卫
router.beforeEach((to) => {
	const hasToken = !!getToken()
	if (to.meta.public) {
		if (hasToken && to.path === '/login') {
			return { path: '/dashboard' }
		}
		return true
	}
	if (!hasToken) {
		return { path: '/login', query: { redirect: to.fullPath } }
	}
	return true
})

router.afterEach((to) => {
	const title = to.meta.title ? `${to.meta.title} · 河南师大 HIS` : '河南师大 HIS'
	document.title = title
})

export default router
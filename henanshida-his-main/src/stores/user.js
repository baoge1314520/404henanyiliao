import { defineStore } from 'pinia'
import { _axios } from '@/util/axios'
import { getToken, setToken, getUser, setUser, getRole, setRole, clearAuth } from '@/util/auth'

// 管理端与医生端共用 /auth/login，后端按 sys_user.role 发放不同前缀的令牌；
// 前端按返回的角色落 his_role，路由守卫据此分流到管理端或医生工作站。
export const useUserStore = defineStore('user', {
	state: () => ({
		token: getToken(),
		user: getUser(),
		deptName: '',
		titleName: ''
	}),
	getters: {
		isLogin: state => !!state.token,
		realName: state => state.user?.realName || '管理员',
		avatarText: state => (state.user?.realName || '管').slice(0, 1)
	},
	actions: {
		async login(username, password) {
			const formData = new FormData()
			formData.append('username', username)
			formData.append('password', password)
			const { data } = await _axios.request({ url: '/auth/login', method: 'POST', data: formData })
			if (data.code === 200) {
				this.token = data.content.token
				this.user = data.content.user
				const role = this.user?.role === 'doctor' ? 'doctor' : 'admin'
				setToken(this.token)
				setUser(this.user)
				setRole(role)
				return { ok: true, role }
			}
			return { ok: false, message: data.message || '登录失败' }
		},
		async register(payload) {
			const formData = new FormData()
			Object.keys(payload).forEach(k => formData.append(k, payload[k]))
			const { data } = await _axios.request({ url: '/auth/register', method: 'POST', data: formData })
			if (data.code === 200) {
				this.token = data.content.token
				this.user = data.content.user
				// 注册统一创建医生账号（管理员账号由系统内置）
				setToken(this.token)
				setUser(this.user)
				setRole('doctor')
				return { ok: true, role: 'doctor' }
			}
			return { ok: false, message: data.message || '注册失败' }
		},
		async fetchInfo() {
			if (getRole() === 'doctor') {
				const { data } = await _axios.request({ url: '/doctorAuth/info', method: 'GET' })
				if (data.code === 200 && data.content) {
					this.user = data.content.user
					this.deptName = data.content.deptName || ''
					this.titleName = data.content.titleName || ''
					setUser(this.user)
				}
				return data
			}
			const { data } = await _axios.request({ url: '/auth/info', method: 'GET' })
			if (data.code === 200 && data.content) {
				this.user = data.content
				setUser(this.user)
			}
			return data
		},
		async logout() {
			try {
				const url = getRole() === 'doctor' ? '/doctorAuth/logout' : '/auth/logout'
				await _axios.request({ url, method: 'POST' })
			} catch {
				// 忽略退出接口异常，前端照样清理登录态
			}
			this.token = ''
			this.user = null
			this.deptName = ''
			this.titleName = ''
			clearAuth()
		}
	}
})
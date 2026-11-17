import { defineStore } from 'pinia'
import { _axios } from '@/util/axios'
import { getToken, setToken, getUser, setUser, setRole, clearAuth } from '@/util/auth'

export const useUserStore = defineStore('user', {
	state: () => ({
		token: getToken(),
		user: getUser()
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
				setToken(this.token)
				setUser(this.user)
				setRole('admin')
				return { ok: true }
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
				setToken(this.token)
				setUser(this.user)
				setRole('admin')
				return { ok: true }
			}
			return { ok: false, message: data.message || '注册失败' }
		},
		async fetchInfo() {
			const { data } = await _axios.request({ url: '/auth/info', method: 'GET' })
			if (data.code === 200 && data.content) {
				this.user = data.content
				setUser(this.user)
			}
			return data
		},
		async logout() {
			try {
				await _axios.request({ url: '/auth/logout', method: 'POST' })
			} catch {
				// 忽略退出接口异常，前端照样清理登录态
			}
			this.token = ''
			this.user = null
			clearAuth()
		}
	}
})
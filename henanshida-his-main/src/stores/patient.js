import { defineStore } from 'pinia'
import { _axios } from '@/util/axios'
import { getToken, setToken, getUser, setUser, setRole, clearAuth } from '@/util/auth'

// 患者端登录态：与管理端共用同一份 token 存储（同一浏览器同时只保留一个会话），
// 角色标记为 patient，路由守卫据此放行患者门户、拦截管理端页面。
export const usePatientStore = defineStore('patient', {
	state: () => ({
		token: getToken(),
		patient: getUser()
	}),
	getters: {
		isLogin: state => !!state.token,
		name: state => state.patient?.patientName || '患者',
		avatarText: state => (state.patient?.patientName || '患').slice(0, 1)
	},
	actions: {
		async login(phone, password) {
			const formData = new FormData()
			formData.append('phone', phone)
			formData.append('password', password)
			const { data } = await _axios.request({ url: '/patientAuth/login', method: 'POST', data: formData })
			if (data.code === 200) {
				this.token = data.content.token
				this.patient = data.content.patient
				setToken(this.token)
				setUser(this.patient)
				setRole('patient')
				return { ok: true }
			}
			return { ok: false, message: data.message || '登录失败' }
		},
		async register(payload) {
			const formData = new FormData()
			Object.keys(payload).forEach(k => formData.append(k, payload[k]))
			const { data } = await _axios.request({ url: '/patientAuth/register', method: 'POST', data: formData })
			if (data.code === 200) {
				this.token = data.content.token
				this.patient = data.content.patient
				setToken(this.token)
				setUser(this.patient)
				setRole('patient')
				return { ok: true }
			}
			return { ok: false, message: data.message || '注册失败' }
		},
		async fetchInfo() {
			const { data } = await _axios.request({ url: '/patientAuth/info', method: 'GET' })
			if (data.code === 200 && data.content) {
				this.patient = data.content
				setUser(this.patient)
			}
			return data
		},
		async logout() {
			try {
				await _axios.request({ url: '/patientAuth/logout', method: 'POST' })
			} catch {
				// 忽略退出接口异常，前端照样清理登录态
			}
			this.token = ''
			this.patient = null
			clearAuth()
		}
	}
})
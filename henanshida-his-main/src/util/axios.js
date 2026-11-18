import axios from 'axios'
import { getToken, getRole, clearAuth } from '@/util/auth'

export const _axios = axios.create({
	baseURL: 'http://127.0.0.1:8081',
	timeout: 30000
})

// 请求拦截：自动携带登录令牌
_axios.interceptors.request.use(config => {
	const token = getToken()
	if (token) {
		config.headers = config.headers || {}
		config.headers['Authorization'] = token
	}
	return config
})

// 响应拦截：统一处理登录失效（后端约定 HTTP 200 + code 401）
let redirecting = false
_axios.interceptors.response.use(
	response => {
		const data = response.data
		if (data && data.code === 401 && !redirecting) {
			redirecting = true
			const role = getRole()
			clearAuth()
			const back = encodeURIComponent(location.pathname + location.search)
			location.replace('/login?redirect=' + back + (role !== 'admin' ? '&role=' + role : ''))
			setTimeout(() => { redirecting = false }, 1500)
		}
		return response
	},
	error => Promise.reject(error)
)
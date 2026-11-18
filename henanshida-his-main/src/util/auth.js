// 登录态本地持久化：token + 用户信息 + 当前角色（admin 管理端 / doctor 医生端 / patient 患者端）
const TOKEN_KEY = 'his_token'
const USER_KEY = 'his_user'
const ROLE_KEY = 'his_role'

export function getToken() {
	return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token) {
	localStorage.setItem(TOKEN_KEY, token || '')
}

export function getUser() {
	try {
		return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
	} catch {
		return null
	}
}

export function setUser(user) {
	localStorage.setItem(USER_KEY, JSON.stringify(user || null))
}

export function getRole() {
	const role = localStorage.getItem(ROLE_KEY)
	return role === 'patient' || role === 'doctor' ? role : 'admin'
}

export function setRole(role) {
	localStorage.setItem(ROLE_KEY, role === 'patient' || role === 'doctor' ? role : 'admin')
}

export function clearAuth() {
	localStorage.removeItem(TOKEN_KEY)
	localStorage.removeItem(USER_KEY)
	localStorage.removeItem(ROLE_KEY)
}
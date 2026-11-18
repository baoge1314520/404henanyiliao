import { _axios } from '@/util/axios'

// 缓存远程下拉数据，避免重复请求；失败时清空缓存以便下次重试
function cached(loader) {
	let p = null
	return () => {
		if (!p) {
			p = loader().catch(() => { p = null; return [] })
		}
		return p
	}
}

async function getArray(url, params) {
	const { data } = await _axios.request({ url, method: 'GET', params })
	if (data.code !== 200) return []
	return Array.isArray(data.content) ? data.content : (data.content?.records || [])
}

export const deptOptions = cached(async () => {
	const arr = await getArray('/sysDept/listAll')
	return arr.map(d => ({ label: d.deptName, value: d.id }))
})

export const titleOptions = cached(async () => {
	const arr = await getArray('/sysTitle/listAll')
	return arr.map(t => ({ label: `${t.titleName}（¥${t.regFee}）`, value: t.id, raw: t.titleName }))
})

export const doctorOptions = cached(async () => {
	const arr = await getArray('/sysUser/list', { pageNo: 1, pageSize: 500 })
	// 保留 deptId，供排班等页面做医生-科室双向联动
	return arr.map(u => ({ label: u.realName, value: u.id, deptId: u.deptId }))
})

export const menuOptions = cached(async () => {
	const arr = await getArray('/sysMenu/listAll')
	return [{ label: '顶级菜单', value: 0 }, ...arr.map(m => ({ label: m.menuName, value: m.id }))]
})

// 选项数组 → { value: label } 映射，供表格字典列使用
export function toMap(options) {
	const m = {}
	for (const o of options || []) m[o.value] = o.raw || o.label
	return m
}

export const GENDER = { 0: '未知', 1: '男', 2: '女' }
export const DEPT_TYPE = { 1: '临床科室', 2: '行政科室', 3: '医技科室' }
export const REG_PERIOD = { 1: '上午', 2: '下午' }
export const REG_TYPE = { 1: '普通挂号', 2: '预约挂号' }
export const REG_STATUS = { 1: '待就诊', 2: '已就诊', 3: '已取消' }
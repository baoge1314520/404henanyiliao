<template>
	<el-menu
		router
		:collapse="collapse"
		:default-active="activePath"
		:collapse-transition="false"
		class="his-menu">
		<template v-for="group in groups" :key="group.title">
			<!-- 单级 -->
			<el-menu-item v-if="group.single" :index="group.path">
				<el-icon><component :is="group.icon" /></el-icon>
				<template #title>{{ group.title }}</template>
			</el-menu-item>
			<!-- 分组 -->
			<el-sub-menu v-else :index="group.title">
				<template #title>
					<el-icon><component :is="group.icon" /></el-icon>
					<span>{{ group.title }}</span>
				</template>
				<el-menu-item v-for="item in group.children" :key="item.path" :index="item.path">
					<el-icon><component :is="item.icon" /></el-icon>
					<template #title>{{ item.title }}</template>
				</el-menu-item>
			</el-sub-menu>
		</template>
	</el-menu>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

defineProps({
	collapse: { type: Boolean, default: false }
})

const route = useRoute()
const activePath = computed(() => route.path)

const groups = [
	{ single: true, title: '工作台', path: '/dashboard', icon: 'Odometer' },
	{
		title: '系统管理', icon: 'Setting', children: [
			{ title: '科室管理', path: '/dept', icon: 'OfficeBuilding' },
			{ title: '医生管理', path: '/doctor', icon: 'User' },
			{ title: '职称管理', path: '/title', icon: 'Medal' },
			{ title: '角色管理', path: '/role', icon: 'Avatar' },
			{ title: '菜单管理', path: '/menu', icon: 'Menu' },
			{ title: '字典管理', path: '/dict', icon: 'Collection' }
		]
	},
	{
		title: '挂号就诊', icon: 'FirstAidKit', children: [
			{ title: '患者管理', path: '/patient', icon: 'UserFilled' },
			{ title: '挂号管理', path: '/registration', icon: 'Tickets' },
			{ title: '排班管理', path: '/schedule', icon: 'Calendar' },
			{ title: '叫号队列', path: '/queue', icon: 'Bell' },
			{ title: '分诊管理', path: '/triage', icon: 'Guide' }
		]
	}
]
</script>

<style scoped lang="scss">
.his-menu {
	border-right: none;
	background: transparent;
	padding: 8px 10px;
	--el-menu-bg-color: transparent;
	--el-menu-text-color: #c3cfe6;
	--el-menu-hover-bg-color: rgba(255, 255, 255, .08);
	--el-menu-active-color: #ffffff;
	--el-menu-item-height: 46px;
	--el-menu-sub-item-height: 42px;
}

.his-menu:not(.el-menu--collapse) {
	width: 100%;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
	border-radius: 10px;
	margin-bottom: 4px;
	font-size: 14px;
}

:deep(.el-menu-item .el-icon),
:deep(.el-sub-menu__title .el-icon) {
	font-size: 18px;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
	background: rgba(255, 255, 255, .09);
	color: #fff;
}

:deep(.el-menu-item.is-active) {
	background: linear-gradient(135deg, #2563eb, #3b82f6);
	color: #fff;
	font-weight: 600;
	box-shadow: 0 6px 16px rgba(37, 99, 235, .4);
}

:deep(.el-sub-menu .el-menu) {
	background: rgba(0, 0, 0, .16);
	border-radius: 10px;
	padding: 4px;
	margin: 2px 0 6px;
}

:deep(.el-sub-menu .el-menu .el-menu-item) {
	min-width: auto;
	height: 40px;
	line-height: 40px;
	font-size: 13.5px;
}

:deep(.el-sub-menu.is-active > .el-sub-menu__title) {
	color: #fff;
}
</style>
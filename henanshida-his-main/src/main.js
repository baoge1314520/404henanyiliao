import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import '@/css/main'
import router from './router'

const app = createApp(App)

// 全局注册 Element Plus 图标，模板里可直接 <el-icon><Search /></el-icon>
for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
	app.component(name, component)
}

app.use(createPinia())
app.use(router)

app.mount('#app')
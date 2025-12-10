// 为浏览器环境提供Node.js global对象的兼容
if (typeof global === 'undefined') {
  window.global = window;
}

import './assets/main.css'

// 引入Element Plus样式
import 'element-plus/dist/index.css'

import { createApp } from 'vue'
// 引入Element Plus
import ElementPlus from 'element-plus'
// 引入Pinia
import { createPinia } from 'pinia'
// 引入路由配置
import router from './router'

import App from './App.vue'

// 创建应用实例
const app = createApp(App)

// 使用Pinia状态管理
const pinia = createPinia()
app.use(pinia)

// 使用路由
app.use(router)

// 使用Element Plus组件库
app.use(ElementPlus)

// 挂载应用
app.mount('#app')

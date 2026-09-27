const originalWarn = console.warn
console.warn = (...args) => {
  // 屏蔽 ElementPlus 所有警告 + 废弃API + el-radio/el-tag 等
  const msg = args.join(' ')
  if (
    msg.includes('ElementPlusError') ||
    msg.includes('deprecated') ||
    msg.includes('el-radio') ||
    msg.includes('el-tag') ||
    msg.includes('Invalid prop') ||
    msg.includes('custom validator check failed')
  ) {
    return // 屏蔽
  }
  // 其他正常输出
  originalWarn(...args)
}
// 错误（error）完全不动，保证安全！
// console.error 保持原样

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
//导入 Element Plus 的所有图标
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'echarts'

import App from './App.vue'
import router from './router'
import './assets/styles/global.scss'

const app = createApp(App)


// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')

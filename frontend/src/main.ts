import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 设计 token 与主题换肤：必须在 element-plus 样式之后引入才能覆盖其变量
import './styles/tokens.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(ElementPlus, { locale: zhCn })
app.use(router)
app.mount('#app')

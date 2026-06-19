import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/app.css'
import App from './App.vue'
import router from './router'
import { bootstrapBackend } from './services/backend'

async function bootstrap() {
  await bootstrapBackend()
  createApp(App).use(router).use(ElementPlus).mount('#app')
}

bootstrap()

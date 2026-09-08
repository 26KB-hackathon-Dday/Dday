import 'vant/lib/index.css'
import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Vant from 'vant'

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)
// 해커톤 기간에는 전체 등록으로 간다 — 필요한 컴포넌트를 그때그때 골라 쓰기 위해서다.
// 번들 크기가 문제가 되면 쓰는 컴포넌트만 개별 import로 바꾼다.
app.use(Vant)

app.mount('#app')

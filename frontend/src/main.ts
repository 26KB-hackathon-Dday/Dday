import 'vant/lib/index.css'
import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Vant, { Locale } from 'vant'
import koKR from 'vant/es/locale/lang/ko-KR'

import App from './App.vue'
import router from './router'

// 기본 로케일이 중국어라 DatePicker 등의 확인/취소 버튼이 한자로 뜬다.
Locale.use('ko-KR', koKR)

const app = createApp(App)

app.use(createPinia())
app.use(router)
// 해커톤 기간에는 전체 등록으로 간다 — 필요한 컴포넌트를 그때그때 골라 쓰기 위해서다.
// 번들 크기가 문제가 되면 쓰는 컴포넌트만 개별 import로 바꾼다.
app.use(Vant)

app.mount('#app')

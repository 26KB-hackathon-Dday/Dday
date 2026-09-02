<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { pocketApi, type Pocket } from '@/api/pocket'
import { ApiError } from '@/api/types'

const pockets = ref<Pocket[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

/** 금액은 원 단위 정수로 보여준다. 소수점은 화면에서 의미가 없다. */
const won = (value: number) => `${Math.round(value).toLocaleString('ko-KR')}원`

async function load() {
  loading.value = true
  error.value = null
  try {
    pockets.value = await pocketApi.findAll()
  } catch (e) {
    // 백엔드 ErrorCode의 message가 문구의 정본이라 그대로 띄운다 (AGENTS.md §2).
    error.value = e instanceof ApiError ? e.message : '알 수 없는 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <main class="page">
    <header class="head">
      <h1>내 포켓</h1>
      <p>정착금과 지원금을 용도별로 나눠 관리합니다.</p>
    </header>

    <p v-if="loading" class="state">불러오는 중…</p>

    <div v-else-if="error" class="state error">
      <p>{{ error }}</p>
      <button type="button" @click="load">다시 시도</button>
    </div>

    <ul v-else class="list">
      <li v-for="pocket in pockets" :key="pocket.pocketId" class="card">
        <div class="card-head">
          <div>
            <h2>{{ pocket.label }}</h2>
            <p class="desc">{{ pocket.description }}</p>
          </div>
          <!-- 예산을 넘긴 포켓은 눈에 띄어야 한다. 서버가 remaining을 음수로 주는 이유다. -->
          <span class="rate" :class="{ over: pocket.remaining < 0 }">
            {{ pocket.usageRate }}%
          </span>
        </div>

        <div class="bar">
          <div
            class="fill"
            :class="{ over: pocket.remaining < 0 }"
            :style="{ width: `${Math.min(pocket.usageRate, 100)}%` }"
          />
        </div>

        <dl class="figures">
          <div>
            <dt>배분액</dt>
            <dd>{{ won(pocket.monthlyBudget) }}</dd>
          </div>
          <div>
            <dt>사용</dt>
            <dd>{{ won(pocket.spentThisMonth) }}</dd>
          </div>
          <div>
            <dt>{{ pocket.remaining < 0 ? '초과' : '남음' }}</dt>
            <dd :class="{ over: pocket.remaining < 0 }">
              {{ won(Math.abs(pocket.remaining)) }}
            </dd>
          </div>
        </dl>
      </li>
    </ul>
  </main>
</template>

<style scoped>
.page {
  max-width: 640px;
  margin: 0 auto;
  padding: 2rem 1rem 4rem;
}
.head h1 {
  font-size: 1.5rem;
  font-weight: 700;
}
.head p {
  margin-top: 0.25rem;
  color: var(--color-text-light, #888);
  font-size: 0.9rem;
}
.state {
  margin-top: 2rem;
  text-align: center;
  color: var(--color-text-light, #888);
}
.state.error {
  color: #c0392b;
}
.state button {
  margin-top: 0.75rem;
  padding: 0.4rem 1rem;
  cursor: pointer;
}
.list {
  list-style: none;
  padding: 0;
  margin: 1.5rem 0 0;
  display: grid;
  gap: 0.75rem;
}
.card {
  border: 1px solid var(--color-border, #e0e0e0);
  border-radius: 12px;
  padding: 1rem;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}
.card-head h2 {
  font-size: 1.05rem;
  font-weight: 600;
}
.desc {
  margin-top: 0.15rem;
  font-size: 0.8rem;
  color: var(--color-text-light, #888);
}
.rate {
  font-variant-numeric: tabular-nums;
  font-weight: 700;
  white-space: nowrap;
}
.rate.over {
  color: #c0392b;
}
.bar {
  margin-top: 0.75rem;
  height: 6px;
  border-radius: 3px;
  background: var(--color-background-mute, #f1f1f1);
  overflow: hidden;
}
.fill {
  height: 100%;
  background: #00857a;
  transition: width 0.3s;
}
.fill.over {
  background: #c0392b;
}
.figures {
  display: flex;
  justify-content: space-between;
  margin: 0.75rem 0 0;
}
.figures div {
  text-align: center;
  flex: 1;
}
.figures dt {
  font-size: 0.75rem;
  color: var(--color-text-light, #888);
}
.figures dd {
  margin: 0.15rem 0 0;
  font-size: 0.9rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.figures dd.over {
  color: #c0392b;
}
</style>

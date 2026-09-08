<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { grantApi, type GrantListItem } from '@/api/grant'
import { ApiError } from '@/api/types'
import AppIcon from '@/components/AppIcon.vue'

const router = useRouter()

const items = ref<GrantListItem[]>([])
const total = ref(0)
const loading = ref(false)
const errorText = ref('')

/** 백엔드 category 정확일치 값. '전체'는 필터 없음. */
const CATEGORIES = ['전체', '주거', '생활', '자산형성', '기타']
const activeCategory = ref('전체')
const query = ref('')

/** category(파스텔 배지 색) — 백엔드가 색을 안 주므로 여기서 매핑한다. */
const CATEGORY_TONE: Record<string, string> = {
  주거: 'blue',
  생활: 'teal',
  자산형성: 'orange',
  기타: 'neutral',
}
function toneOf(category: string | null): string {
  return (category && CATEGORY_TONE[category]) || 'neutral'
}

async function load() {
  loading.value = true
  errorText.value = ''
  try {
    const page = await grantApi.fetchList({
      q: query.value.trim() || undefined,
      category: activeCategory.value === '전체' ? undefined : activeCategory.value,
    })
    items.value = page.content
    total.value = page.totalElements
  } catch (e) {
    items.value = []
    total.value = 0
    errorText.value = e instanceof ApiError ? e.message : '목록을 불러오지 못했어요.'
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(activeCategory, load)

// 검색어는 입력이 멈춘 뒤에 호출한다.
let debounce: ReturnType<typeof setTimeout> | undefined
watch(query, () => {
  clearTimeout(debounce)
  debounce = setTimeout(load, 300)
})

function openDetail(programId: string) {
  router.push(`/grants/${programId}`)
}
</script>

<template>
  <div class="list">
    <!-- 검색 -->
    <section class="search">
      <h2 class="search__title">전체 지원제도 찾기</h2>
      <div class="search__field">
        <AppIcon name="search" :size="18" class="search__icon" />
        <input v-model="query" type="search" placeholder="지원제도를 검색해보세요" />
      </div>
    </section>

    <!-- 필터 -->
    <section class="filters">
      <div class="chips">
        <button
          v-for="cat in CATEGORIES"
          :key="cat"
          type="button"
          class="chip"
          :class="{ 'chip--on': cat === activeCategory }"
          @click="activeCategory = cat"
        >
          {{ cat }}
        </button>
      </div>

      <p class="filters__count">총 {{ total }}건</p>
    </section>

    <!-- 결과 -->
    <section class="results">
      <button
        v-for="item in items"
        :key="item.programId"
        type="button"
        class="gcard"
        :class="{ 'gcard--closed': item.status === 'CLOSED' }"
        @click="openDetail(item.programId)"
      >
        <div class="gcard__top">
          <div class="gcard__lead">
            <span v-if="item.category" class="cat" :class="`cat--${toneOf(item.category)}`">
              {{ item.category }}
            </span>
            <span class="gcard__title">{{ item.name }}</span>
          </div>
          <span class="status" :class="`status--${item.status.toLowerCase()}`">
            {{ item.status === 'OPEN' ? '신청가능' : '마감' }}
          </span>
        </div>
        <div class="gcard__meta">
          <span v-if="item.benefitText" class="metarow">
            <AppIcon name="benefit" :size="15" />
            {{ item.benefitText }}
          </span>
          <span v-if="item.periodText" class="metarow">
            <AppIcon name="calendar" :size="13" />
            {{ item.periodText }}
          </span>
        </div>
      </button>

      <p v-if="errorText" class="results__empty">{{ errorText }}</p>
      <p v-else-if="!loading && !items.length" class="results__empty">
        조건에 맞는 지원제도가 없어요.
      </p>
    </section>
  </div>
</template>

<style scoped>
.list {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 12px 24px 40px;
}

/* ── 검색 ── */
.search {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.search__title {
  font-size: 20px;
  font-weight: 500;
  line-height: 28px;
  letter-spacing: -0.2px;
  color: #000;
}
.search__field {
  position: relative;
  display: flex;
  align-items: center;
}
.search__icon {
  position: absolute;
  left: 16px;
  color: var(--c-text-2);
  pointer-events: none;
}
.search__field input {
  width: 100%;
  height: 56px;
  padding: 0 16px 0 46px;
  border: 1px solid var(--c-border);
  border-radius: 8px;
  background: var(--c-bg);
  font-size: 16px;
  color: #000;
}
.search__field input::placeholder {
  color: var(--c-text-3);
}

/* ── 필터 ── */
.filters {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.chips {
  display: flex;
  justify-content: center;
  gap: 8px;
  overflow-x: auto;
  padding: 0 20px 8px;
  margin: 0 -20px;
  scrollbar-width: none;
}
.chips::-webkit-scrollbar {
  display: none;
}
.chip {
  flex-shrink: 0;
  height: 32px;
  padding: 0 16px;
  border: 1px solid var(--c-border);
  border-radius: 9999px;
  background: var(--c-bg);
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.14px;
  color: var(--c-text-2);
}
.chip--on {
  background: #000;
  border-color: #000;
  color: #fff;
}

.filters__count {
  text-align: right;
  font-size: 14px;
  line-height: 20px;
  color: var(--c-text-3);
  font-variant-numeric: tabular-nums;
}

/* ── 결과 카드 ── */
.results {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.gcard {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 17px;
  border: 1px solid var(--c-border);
  border-radius: 12px;
  background: var(--c-bg);
  text-align: left;
}
.gcard--closed {
  opacity: 0.6;
}
.gcard__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}
.gcard__lead {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}
.cat {
  padding: 4px 8px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
}
.cat--blue {
  background: rgba(57, 123, 199, 0.1);
  color: var(--c-blue);
}
.cat--teal {
  background: rgba(39, 184, 184, 0.1);
  color: var(--c-teal);
}
.cat--orange {
  background: rgba(242, 154, 74, 0.1);
  color: #f29a4a;
}
.cat--neutral {
  background: var(--c-surface);
  color: var(--c-text-2);
}
.gcard__title {
  margin-top: 4px;
  font-size: 20px;
  font-weight: 500;
  line-height: 28px;
  letter-spacing: -0.2px;
  color: #000;
}
.status {
  flex-shrink: 0;
  padding: 4px 8px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
}
.status--open {
  background: #e2e2e2;
  color: #000;
}
.status--closed {
  background: #eee;
  color: var(--c-text-3);
}
.gcard__meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.metarow {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
  color: var(--c-text-2);
}
.metarow :deep(.app-icon) {
  color: var(--c-text-2);
}

.results__empty {
  padding: 32px 0;
  text-align: center;
  font-size: 14px;
  color: var(--c-text-3);
}
</style>

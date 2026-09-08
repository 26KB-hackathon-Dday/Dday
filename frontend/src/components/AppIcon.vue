<script setup lang="ts">
/**
 * Figma에서 내보낸 SVG(`src/assets/icons/*.svg`)를 CSS mask로 그린다.
 * 원본 파일은 그대로 두고 색만 `currentColor`로 바꿔, 활성/비활성·강조 색을
 * 부모의 `color`로 제어한다.
 */
import { computed } from 'vue'

const props = withDefaults(defineProps<{ name: string; size?: number }>(), { size: 24 })

const urls = import.meta.glob<string>('@/assets/icons/*.svg', {
  eager: true,
  query: '?url',
  import: 'default',
})

/** glob 키('/src/assets/icons/back.svg')를 파일명으로 다시 색인한다. */
const byName: Record<string, string> = Object.fromEntries(
  Object.entries(urls).map(([path, url]) => [path.split('/').pop()!.replace('.svg', ''), url]),
)

const style = computed(() => {
  const url = byName[props.name]
  const px = `${props.size}px`
  if (!url) return { width: px, height: px }
  // data: URI로 인라인되면 작은따옴표가 들어 있어 큰따옴표로 감싼다.
  const mask = `url("${url}")`
  return { width: px, height: px, maskImage: mask, WebkitMaskImage: mask }
})
</script>

<template>
  <span class="app-icon" :style="style" />
</template>

<style scoped>
.app-icon {
  display: inline-block;
  flex-shrink: 0;
  background-color: currentColor;
  mask-repeat: no-repeat;
  mask-position: center;
  mask-size: contain;
  -webkit-mask-repeat: no-repeat;
  -webkit-mask-position: center;
  -webkit-mask-size: contain;
}
</style>

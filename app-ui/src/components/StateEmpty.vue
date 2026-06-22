<template>
  <div class="state-empty glass-panel" :class="['state-empty--' + size, { 'state-empty--wide': wide }]">
    <div class="state-empty__icon">
      <span>◎</span>
    </div>
    <div class="state-empty__eyebrow">{{ eyebrow }}</div>
    <div class="state-empty__title">{{ title }}</div>
    <div class="state-empty__desc">{{ description }}</div>
    <div class="state-empty__hint">{{ hint }}</div>
    <div v-if="footer" class="state-empty__footer">{{ footer }}</div>
    <div v-if="buttonText" class="state-empty__actions">
      <el-button type="primary" round @click="$emit('action')">{{ buttonText }}</el-button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  eyebrow: {
    type: String,
    default: '当前无内容'
  },
  title: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  },
  hint: {
    type: String,
    default: ''
  },
  footer: {
    type: String,
    default: ''
  },
  buttonText: {
    type: String,
    default: ''
  },
  wide: {
    type: Boolean,
    default: false
  },
  size: {
    type: String,
    default: 'compact'
  }
})

defineEmits(['action'])
</script>

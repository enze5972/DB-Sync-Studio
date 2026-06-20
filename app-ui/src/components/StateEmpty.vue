<template>
  <div class="state-empty glass-panel" :class="{ 'state-empty--wide': wide }">
    <div class="state-empty__icon">
      <span>◎</span>
    </div>
    <div class="state-empty__eyebrow">准备好开始下一步</div>
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
  }
})

defineEmits(['action'])
</script>

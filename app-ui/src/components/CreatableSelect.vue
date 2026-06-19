<template>
  <el-select
    :model-value="modelValue"
    :placeholder="placeholder"
    :filterable="filterable"
    :allow-create="allowCreate"
    :default-first-option="defaultFirstOption"
    :clearable="clearable"
    :disabled="disabled"
    v-bind="$attrs"
    @update:model-value="handleUpdate"
    @change="handleChange"
  >
    <el-option
      v-for="option in normalizedOptions"
      :key="String(option.value) + ':' + option.label"
      :label="option.label"
      :value="option.value"
    />
  </el-select>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  options: {
    type: Array,
    default: function () {
      return []
    }
  },
  placeholder: {
    type: String,
    default: ''
  },
  filterable: {
    type: Boolean,
    default: true
  },
  allowCreate: {
    type: Boolean,
    default: true
  },
  defaultFirstOption: {
    type: Boolean,
    default: true
  },
  clearable: {
    type: Boolean,
    default: true
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const normalizedOptions = computed(function () {
  return (props.options || []).map(function (option) {
    if (option && typeof option === 'object') {
      const label = option.label || option.schemaName || option.tableName || option.value || ''
      const value = option.value !== undefined && option.value !== null
        ? option.value
        : (option.schemaName || option.tableName || option.label || '')
      return {
        label: String(label),
        value: value
      }
    }
    return {
      label: String(option),
      value: option
    }
  })
})

function handleUpdate(value) {
  emit('update:modelValue', value)
}

function handleChange(value) {
  emit('change', value)
}
</script>

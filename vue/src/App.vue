<script setup>
import { ref } from 'vue'
import axios from 'axios'

const file = ref(null)
const result = ref(null)
const error = ref(null)
const loading = ref(false)

function onFileChange(e) {
  file.value = e.target.files?.[0] ?? null
}

async function upload() {
  error.value = null
  result.value = null
  if (!file.value) {
    error.value = 'Excelファイルを選んでください'
    return
  }
  loading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file.value)

    // Spring側: POST /api/pos/import を想定
    const res = await axios.post('/api/pos/import', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    result.value = res.data
  } catch (e) {
    error.value = String(e?.response?.data?.message ?? e?.message ?? e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div style="max-width: 900px; margin: 40px auto; font-family: sans-serif;">
    <h2>Flippers POS Editor (dev)</h2>

    <input type="file" accept=".xlsx" @change="onFileChange" />
    <button @click="upload" :disabled="loading" style="margin-left: 10px;">
      {{ loading ? 'Uploading...' : 'Upload' }}
    </button>

    <p v-if="error" style="color: red;">{{ error }}</p>

    <pre v-if="result" style="margin-top: 20px; background: #f6f6f6; padding: 12px; overflow:auto;">
{{ JSON.stringify(result, null, 2) }}
    </pre>
  </div>
</template>

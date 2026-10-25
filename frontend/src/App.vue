<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
const devices = ref({}), messages = ref([]), live = ref(false)
let ws, retryTimer, closed = false

function connect() {
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  ws = new WebSocket(`${proto}://${location.host}/ws`)
  ws.onopen = () => (live.value = true)
  ws.onclose = () => {                                  // 斷線自動重連(WebSocket 不像 SSE 內建重連)
    live.value = false
    if (!closed) retryTimer = setTimeout(connect, 2000)
  }
  ws.onmessage = e => {
    const { type, data } = JSON.parse(e.data)
    if (type === 'snapshot') {
      devices.value = Object.fromEntries(data.devices.map(x => [x.id, x]))
      messages.value = data.messages
    } else if (type === 'device') {
      devices.value[data.id] = { ...devices.value[data.id], ...data }
    } else if (type === 'message') {
      devices.value[data.deviceId] = { ...devices.value[data.deviceId], id: data.deviceId, online: true, lastData: data.payload }
      messages.value = [data, ...messages.value].slice(0, 20)
    }
  }
}
// 透過同一條 WebSocket 下發指令
function ping(id) {
  if (ws && ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify({ type: 'send', deviceId: id, body: { cmd: 'ping' } }))
}
const fmt = t => new Date(t).toLocaleTimeString()
onMounted(connect)
onUnmounted(() => { closed = true; clearTimeout(retryTimer); ws && ws.close() })
</script>

<template>
  <main>
    <h1>設備接入監控 (MQTT) <small :class="live ? 'on' : 'off'">● {{ live ? '即時連線中' : '重新連線中…' }}</small></h1>
    <section class="cards">
      <div v-for="d in devices" :key="d.id" class="card">
        <b>{{ d.id }}</b>
        <span :class="d.online ? 'on' : 'off'">{{ d.online ? '在線' : '離線' }}</span>
        <div v-if="d.lastData">🌡 {{ d.lastData.temperature }} ℃ &nbsp; 💧 {{ d.lastData.humidity }} %</div>
        <button @click="ping(d.id)">下發 ping</button>
      </div>
    </section>
    <h2>最新上行消息</h2>
    <table>
      <thead><tr><th>時間</th><th>設備</th><th>Topic</th><th>內容</th></tr></thead>
      <tbody><tr v-for="m in messages" :key="m.deviceId + m.ts">
        <td>{{ fmt(m.ts) }}</td><td>{{ m.deviceId }}</td><td>{{ m.topic }}</td><td>{{ JSON.stringify(m.payload) }}</td>
      </tr></tbody>
    </table>
  </main>
</template>

<style>
body { font-family: system-ui, sans-serif; margin: 0; background: #f5f6fa; color: #222 }
main { max-width: 960px; margin: 0 auto; padding: 24px }
h1 small { font-size: 14px; margin-left: 12px }
.cards { display: flex; gap: 16px; flex-wrap: wrap }
.card { background: #fff; padding: 16px; border-radius: 8px; box-shadow: 0 1px 4px #0002; min-width: 200px; display: grid; gap: 8px }
.on { color: #16a34a } .off { color: #dc2626 }
table { width: 100%; border-collapse: collapse; background: #fff } th, td { padding: 8px; border-bottom: 1px solid #eee; text-align: left; font-size: 13px }
button { cursor: pointer }
</style>

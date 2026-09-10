<script setup>
import { ref, reactive, nextTick, watch, onMounted } from 'vue'

const isOpen = ref(false)
const input = ref('')
const isLoading = ref(false)
const currentTool = ref(null)
const messagesContainer = ref(null)
const textareaRef = ref(null)

const quickPrompts = [
  '🩺 老年人高血压与低压的标准范围是多少？日常如何调理？',
  '⌚ 智能腕表心率监测低于 50 次/分正常吗？有哪些注意事项？',
  '🍲 适合高血糖老人的日常低升糖（GI）食谱建议',
  '🏃 预防居家老年人跌倒的核心环境改造措施有哪些？'
]

const messages = reactive([
  {
    role: 'assistant',
    content: '您好！我是您的**智慧医养 AI 守护顾问（小养）**。\n\n我已接入 **DeepSeek-V3.2 大模型** 并具备**互联网实时检索能力**，同时开启了 **LangSmith 全链路可观测性监控**。\n\n您可以随时向我咨询长者健康体征、慢性病调理、智能硬件告警分析或日常康养急救常识！',
    time: formatTime(new Date()),
    toolUsed: false
  }
])

function formatTime(d) {
  return d.toTimeString().slice(0, 5)
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

function toggleOpen() {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    scrollToBottom()
    nextTick(() => {
      textareaRef.value?.focus()
    })
  }
}

function clearHistory() {
  messages.splice(1) // 保留首条欢迎语
  currentTool.value = null
}

function usePrompt(p) {
  input.value = p
  sendMessage()
}

async function sendMessage() {
  const text = input.value.trim()
  if (!text || isLoading.value) return

  messages.push({
    role: 'user',
    content: text,
    time: formatTime(new Date())
  })
  input.value = ''
  isLoading.value = true
  currentTool.value = null
  scrollToBottom()

  // 创建助手占位消息
  const assistantMsgIndex = messages.length
  messages.push({
    role: 'assistant',
    content: '',
    time: formatTime(new Date()),
    toolUsed: false,
    toolQueries: []
  })

  try {
    const historyPayload = messages.slice(0, assistantMsgIndex).map(m => ({
      role: m.role,
      content: m.content
    }))

    const response = await fetch('/api/ai/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ messages: historyPayload })
    })

    if (!response.ok) {
      throw new Error(`服务响应异常 (HTTP ${response.status})`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      // SSE 事件块由双换行符分隔
      const blocks = buffer.split(/\r?\n\r?\n/)
      buffer = blocks.pop() || ''

      for (const block of blocks) {
        if (!block.trim()) continue
        const lines = block.split(/\r?\n/)
        let eventType = 'message'
        let dataStr = ''

        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventType = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            dataStr = line.slice(5).trim()
          }
        }

        if (!dataStr) continue

        try {
          const payload = JSON.parse(dataStr)
          if (eventType === 'delta' && payload.text) {
            messages[assistantMsgIndex].content += payload.text
            scrollToBottom()
          } else if (eventType === 'tool_start') {
            currentTool.value = payload.query || '正在全网检索'
            messages[assistantMsgIndex].toolUsed = true
            if (payload.query && !messages[assistantMsgIndex].toolQueries.includes(payload.query)) {
              messages[assistantMsgIndex].toolQueries.push(payload.query)
            }
            scrollToBottom()
          } else if (eventType === 'tool_done') {
            currentTool.value = null
          } else if (eventType === 'done') {
            currentTool.value = null
          } else if (eventType === 'error') {
            messages[assistantMsgIndex].content += `\n\n*(提示: ${payload.message || '服务异常'})*`
          }
        } catch (err) {
          // ignore json parse error
        }
      }
    }
  } catch (e) {
    messages[assistantMsgIndex].content += `\n\n*(连接遇到波动: ${e.message}，请确保本地 AI 微服务已启动)*`
  } finally {
    isLoading.value = false
    currentTool.value = null
    scrollToBottom()
  }
}

// 简易 Markdown 解析器
function renderMarkdown(content) {
  if (!content) return ''
  let html = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  // 粗体
  html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
  // 标题
  html = html.replace(/^### (.*$)/gim, '<h4 class="ai-md-h4">$1</h4>')
  html = html.replace(/^## (.*$)/gim, '<h3 class="ai-md-h3">$1</h3>')
  html = html.replace(/^# (.*$)/gim, '<h2 class="ai-md-h2">$1</h2>')
  // 无序列表
  html = html.replace(/^\s*[-*]\s+(.*$)/gim, '<li class="ai-md-li">$1</li>')
  // 换行转段落
  html = html.replace(/\n\n/g, '<br><br>')
  html = html.replace(/\n/g, '<br>')
  return html
}

function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}
</script>

<template>
  <div class="ai-assistant-root">
    <!-- 悬浮 3D 呼吸微光球体按钮 -->
    <div
      class="ai-orb-trigger"
      :class="{ 'is-active': isOpen, 'is-loading': isLoading }"
      @click="toggleOpen"
      title="智慧医养 AI 健康顾问"
    >
      <div class="orb-halo"></div>
      <div class="orb-core">
        <svg class="orb-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 4a6 6 0 1 1-6 6 6 6 0 0 1 6-6zm0 2a4 4 0 1 0 4 4 4 4 0 0 0-4-4z"
          />
          <path
            stroke-linecap="round"
            stroke-width="2"
            d="M12 8v8M8 12h8"
          />
        </svg>
        <span class="orb-sparkle">✦</span>
      </div>
      <div class="orb-badge">
        <span class="dot"></span>
        <span class="badge-text">AI</span>
      </div>
    </div>

    <!-- 弹出的毛玻璃对话抽屉 -->
    <transition name="ai-pop">
      <div v-if="isOpen" class="ai-chat-window">
        <!-- 头部 -->
        <header class="ai-header">
          <div class="ai-profile">
            <div class="mini-orb">
              <span class="mini-sparkle">✦</span>
            </div>
            <div>
              <div class="ai-title-row">
                <h3>智慧小养 · 健康顾问</h3>
              </div>
              <p class="ai-subtitle">老年慢病管理 · 腕表体征初筛 · 联网实时检索</p>
            </div>
          </div>
          <div class="ai-actions">
            <button class="ai-icon-btn" @click="clearHistory" title="清空对话记录">
              <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" fill="none" stroke-width="2">
                <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
              </svg>
            </button>
            <button class="ai-icon-btn close-btn" @click="toggleOpen" title="收起面板">✕</button>
          </div>
        </header>

        <!-- 消息列表 -->
        <div ref="messagesContainer" class="ai-messages">
          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="ai-msg-row"
            :class="msg.role"
          >
            <div v-if="msg.role === 'assistant'" class="msg-avatar">
              <span>✚</span>
            </div>
            <div class="msg-bubble">
              <!-- 联网搜索提示条 -->
              <div v-if="msg.toolUsed && msg.toolQueries && msg.toolQueries.length" class="tool-badge-row">
                <span class="tool-tag">
                  <svg viewBox="0 0 24 24" width="12" height="12" stroke="currentColor" fill="none" stroke-width="2">
                    <circle cx="11" cy="11" r="8"></circle>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                  </svg>
                  已联网检索：{{ msg.toolQueries.join('、') }}
                </span>
              </div>

              <!-- 正在联网动态提示 -->
              <div v-if="isLoading && index === messages.length - 1 && currentTool" class="tool-searching-notice">
                <span class="spin-icon">⏳</span>
                <span>正在全网检索【{{ currentTool }}】最新权威医学资料…</span>
              </div>

              <!-- 正文内容渲染 -->
              <div class="msg-content" v-html="renderMarkdown(msg.content)"></div>
              <span v-if="isLoading && index === messages.length - 1 && !currentTool" class="typing-cursor"></span>
              <div class="msg-time">{{ msg.time }}</div>
            </div>
          </div>

          <!-- 预设问题快捷点选卡片 -->
          <div v-if="messages.length === 1" class="quick-prompts-card">
            <p class="quick-title">💡 常见健康咨询速问：</p>
            <div class="quick-chips">
              <button
                v-for="(qp, qi) in quickPrompts"
                :key="qi"
                class="quick-chip"
                @click="usePrompt(qp)"
              >
                {{ qp }}
              </button>
            </div>
          </div>
        </div>

        <!-- 底部输入栏 -->
        <footer class="ai-footer">
          <div class="input-wrapper">
            <textarea
              ref="textareaRef"
              v-model="input"
              class="ai-textarea"
              placeholder="向小养咨询老年人健康常识、腕表体征分析或日常饮食... (Enter 发送)"
              rows="2"
              @keydown="handleKeydown"
            ></textarea>
            <button
              class="ai-send-btn"
              :disabled="isLoading || !input.trim()"
              @click="sendMessage"
              title="发送消息"
            >
              <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
              </svg>
            </button>
          </div>
          <div class="ai-disclaimer">
            <span>🛡️ AI 建议仅供健康宣教与日常调理参考，遇突发重症请立即就医。</span>
          </div>
        </footer>
      </div>
    </transition>
  </div>
</template>

<style scoped>
/* ===================== 悬浮球体触发器 ===================== */
.ai-assistant-root {
  position: relative;
  z-index: 99999;
}

.ai-orb-trigger {
  position: fixed;
  bottom: 28px;
  right: 28px;
  width: 62px;
  height: 62px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
  transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 10px 28px -4px rgba(16, 185, 129, 0.45),
              0 0 0 1px rgba(255, 255, 255, 0.2) inset;
}

.ai-orb-trigger:hover {
  transform: translateY(-5px) scale(1.06);
  box-shadow: 0 16px 36px -2px rgba(16, 185, 129, 0.6),
              0 0 20px rgba(56, 239, 125, 0.5);
}

.ai-orb-trigger.is-active {
  transform: scale(0.92);
}

/* 呼吸微光外光晕 */
.orb-halo {
  position: absolute;
  inset: -6px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(16, 185, 129, 0.4) 0%, rgba(59, 130, 246, 0.2) 60%, transparent 75%);
  animation: pulse-ring 2.8s cubic-bezier(0.25, 0.1, 0.25, 1) infinite;
  z-index: 1;
  pointer-events: none;
}

@keyframes pulse-ring {
  0% {
    transform: scale(0.92);
    opacity: 0.5;
  }
  50% {
    transform: scale(1.18);
    opacity: 0.95;
  }
  100% {
    transform: scale(0.92);
    opacity: 0.5;
  }
}

/* 球体核心 3D 渐变 */
.orb-core {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 28%, #6ee7b7 0%, #10b981 40%, #047857 78%, #064e3b 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
  overflow: hidden;
  box-shadow: inset 0 2px 5px rgba(255, 255, 255, 0.6),
              inset 0 -4px 6px rgba(0, 0, 0, 0.35);
}

.orb-icon {
  width: 28px;
  height: 28px;
  color: #ffffff;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
  transition: transform 0.4s ease;
}

.ai-orb-trigger:hover .orb-icon {
  transform: rotate(15deg);
}

.orb-sparkle {
  position: absolute;
  top: 8px;
  right: 12px;
  color: #ecfdf5;
  font-size: 11px;
  animation: sparkle-blink 2s ease-in-out infinite;
}

@keyframes sparkle-blink {
  0%, 100% { opacity: 0.3; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.2); }
}

.orb-badge {
  position: absolute;
  bottom: -2px;
  right: -2px;
  background: #064e3b;
  border: 2px solid #ffffff;
  border-radius: 12px;
  padding: 1px 5px;
  font-size: 9px;
  font-weight: 800;
  color: #a7f3d0;
  display: flex;
  align-items: center;
  gap: 3px;
  z-index: 3;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
}

.orb-badge .dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 6px #34d399;
}

/* ===================== 对话面板窗口 ===================== */
.ai-chat-window {
  position: fixed;
  bottom: 102px;
  right: 28px;
  width: 440px;
  height: 620px;
  max-width: calc(100vw - 40px);
  max-height: calc(100vh - 130px);
  background: rgba(15, 23, 42, 0.94);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  box-shadow: 0 24px 64px -8px rgba(0, 0, 0, 0.65),
              0 0 0 1px rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  z-index: 99998;
}

/* 展开/收起过渡动画 */
.ai-pop-enter-active,
.ai-pop-leave-active {
  transition: all 0.32s cubic-bezier(0.16, 1, 0.3, 1);
}
.ai-pop-enter-from,
.ai-pop-leave-to {
  opacity: 0;
  transform: translateY(24px) scale(0.94);
}

/* 顶部栏 */
.ai-header {
  padding: 14px 18px;
  background: linear-gradient(180deg, rgba(30, 41, 59, 0.8) 0%, rgba(15, 23, 42, 0.6) 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ai-profile {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mini-orb {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 28%, #34d399 0%, #059669 80%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 10px rgba(16, 185, 129, 0.4);
}

.mini-sparkle {
  color: #ffffff;
  font-size: 14px;
}

.ai-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-title-row h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #f8fafc;
}

.ai-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 8px;
  font-weight: 600;
}

.model-tag {
  background: rgba(59, 130, 246, 0.18);
  color: #60a5fa;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.trace-tag {
  background: rgba(16, 185, 129, 0.18);
  color: #34d399;
  border: 1px solid rgba(16, 185, 129, 0.3);
  display: flex;
  align-items: center;
  gap: 4px;
}

.trace-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 6px #34d399;
}

.ai-subtitle {
  margin: 3px 0 0;
  font-size: 11px;
  color: #94a3b8;
}

.ai-actions {
  display: flex;
  gap: 6px;
}

.ai-icon-btn {
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 6px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.ai-icon-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #f8fafc;
}

.close-btn {
  font-size: 14px;
  font-weight: bold;
}

/* 消息列表容器 */
.ai-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ai-messages::-webkit-scrollbar {
  width: 6px;
}
.ai-messages::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.12);
  border-radius: 4px;
}

/* 消息气泡 */
.ai-msg-row {
  display: flex;
  gap: 10px;
  max-width: 90%;
}

.ai-msg-row.assistant {
  align-self: flex-start;
}

.ai-msg-row.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: linear-gradient(135deg, #10b981 0%, #047857 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 14px;
  font-weight: bold;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.35);
}

.msg-bubble {
  padding: 12px 14px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.6;
  position: relative;
  word-break: break-word;
}

.ai-msg-row.assistant .msg-bubble {
  background: rgba(30, 41, 59, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #e2e8f0;
  border-top-left-radius: 4px;
}

.ai-msg-row.user .msg-bubble {
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  color: #ffffff;
  border-top-right-radius: 4px;
  box-shadow: 0 4px 14px rgba(4, 120, 87, 0.35);
}

.msg-time {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.45);
  margin-top: 6px;
  text-align: right;
}

/* Markdown 内部样式 */
.msg-content :deep(.ai-md-h2) {
  font-size: 15px;
  font-weight: 700;
  color: #34d399;
  margin: 10px 0 4px;
}
.msg-content :deep(.ai-md-h3) {
  font-size: 14px;
  font-weight: 700;
  color: #6ee7b7;
  margin: 8px 0 3px;
}
.msg-content :deep(.ai-md-h4) {
  font-size: 13px;
  font-weight: 600;
  color: #a7f3d0;
  margin: 6px 0 2px;
}
.msg-content :deep(.ai-md-li) {
  margin-left: 16px;
  list-style-type: disc;
}

/* 联网工具标签与状态 */
.tool-badge-row {
  margin-bottom: 8px;
}
.tool-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 6px;
  background: rgba(59, 130, 246, 0.16);
  color: #93c5fd;
  border: 1px solid rgba(59, 130, 246, 0.25);
}

.tool-searching-notice {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #38bdf8;
  padding: 6px 10px;
  background: rgba(56, 189, 248, 0.1);
  border-radius: 8px;
  margin-bottom: 8px;
  animation: pulse-subtle 1.8s ease-in-out infinite;
}

@keyframes pulse-subtle {
  0%, 100% { opacity: 0.75; }
  50% { opacity: 1; }
}

.spin-icon {
  animation: spin 2s linear infinite;
  display: inline-block;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.typing-cursor {
  display: inline-block;
  width: 6px;
  height: 14px;
  background: #34d399;
  vertical-align: middle;
  margin-left: 4px;
  animation: blink 0.8s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* 快捷问题卡片 */
.quick-prompts-card {
  background: rgba(30, 41, 59, 0.45);
  border: 1px dashed rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  padding: 12px;
  margin-top: 4px;
}

.quick-title {
  margin: 0 0 8px;
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
}

.quick-chips {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.quick-chip {
  text-align: left;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #cbd5e1;
  font-size: 11px;
  padding: 7px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-chip:hover {
  background: rgba(16, 185, 129, 0.15);
  border-color: rgba(16, 185, 129, 0.4);
  color: #34d399;
  transform: translateX(3px);
}

/* 底部输入区 */
.ai-footer {
  padding: 12px 16px;
  background: rgba(15, 23, 42, 0.9);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.ai-textarea {
  width: 100%;
  box-sizing: border-box;
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  padding: 10px 42px 10px 12px;
  color: #f8fafc;
  font-size: 13px;
  line-height: 1.4;
  resize: none;
  outline: none;
  font-family: inherit;
  transition: border-color 0.2s ease;
}

.ai-textarea:focus {
  border-color: #10b981;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
}

.ai-send-btn {
  position: absolute;
  right: 8px;
  bottom: 8px;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border: none;
  color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.ai-send-btn:hover:not(:disabled) {
  transform: scale(1.05);
  box-shadow: 0 2px 10px rgba(16, 185, 129, 0.4);
}

.ai-send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.ai-disclaimer {
  margin-top: 6px;
  text-align: center;
  font-size: 9px;
  color: #64748b;
}
</style>

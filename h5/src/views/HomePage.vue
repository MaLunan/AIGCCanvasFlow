<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useProjectStore } from '../stores/projectStore'
import { storeToRefs } from 'pinia'

const router = useRouter()
const projectStore = useProjectStore()
const { projects } = storeToRefs(projectStore)

onMounted(() => projectStore.fetchProjects())

// ── Nav ─────────────────────────────────────────────────────────────────────
const navItems = [
  { label: '首页',     to: '/' },
  { label: '我的项目', to: '/projects' },
  { label: '模板广场', to: '/templates' },
  { label: '模型库',   to: '/models' },
  { label: '帮助',     to: '/help' },
]

// ── Recent projects (real data, up to 4, sorted by last updated) ─────────────
const recentProjects = computed(() =>
  projects.value
    .slice()
    .sort((a, b) => new Date(b.updateTime) - new Date(a.updateTime))
    .slice(0, 4)
)

// ── Templates (mock) ─────────────────────────────────────────────────────────
const templates = ref([
  { id: 1,  name: '悬疑短剧',   category: '短剧', uses: '12.4k', color: '#646cff' },
  { id: 2,  name: '情感口播',   category: '口播', uses: '8.9k',  color: '#42b883' },
  { id: 3,  name: '商品广告',   category: '广告', uses: '21.2k', color: '#ff6b6b' },
  { id: 4,  name: 'Vlog 旅行',  category: 'Vlog', uses: '5.6k',  color: '#f5c542' },
  { id: 5,  name: '知识分享',   category: '教育', uses: '14.1k', color: '#7fd1f5' },
  { id: 6,  name: '古风 MV',    category: 'MV',   uses: '9.3k',  color: '#d07ff5' },
  { id: 7,  name: '品牌宣传片', category: '广告', uses: '6.8k',  color: '#ff9966' },
  { id: 8,  name: '搞笑段子',   category: '娱乐', uses: '18.7k', color: '#42b883' },
])

// ── Features ─────────────────────────────────────────────────────────────────
const features = [
  {
    icon: '⬡',
    title: '无限画布 · 节点工作流',
    desc: '文本、图片、视频、音频节点自由拖拽连接，搭建专属 AI 创作管线，精细掌控每一帧的生成参数。',
    color: '#646cff',
  },
  {
    icon: '🎬',
    title: '智能脚本 · 连贯分镜',
    desc: 'AI 一键生成剧本与分镜，支持 9 宫格 / 25 宫格场景拆解，角色与动作保持跨镜一致。',
    color: '#42b883',
  },
  {
    icon: '🎭',
    title: '角色与镜头控制',
    desc: '三视图角色建模，多机位切换，360° 环绕运镜，灯光与景深实时调节，电影级镜头语言。',
    color: '#ff6b6b',
  },
  {
    icon: '⚡',
    title: '批量生成 · 一键交付',
    desc: '分镜批量出视频，画面延展与无损放大，AI 配音 / 音乐自动合成，分钟级完成整片制作。',
    color: '#f5c542',
  },
]

// ── Models ───────────────────────────────────────────────────────────────────
const models = [
  { name: 'Seedance 2.0', tag: '视频',   color: '#646cff' },
  { name: '可灵 3.0',      tag: '视频',   color: '#ff6b6b' },
  { name: 'Vidu 2.0',      tag: '视频',   color: '#42b883' },
  { name: 'Sora',          tag: '视频',   color: '#7fd1f5' },
  { name: 'Flux 1.1',      tag: '图像',   color: '#d07ff5' },
  { name: 'Midjourney',    tag: '图像',   color: '#f5c542' },
]

// ── Pricing ──────────────────────────────────────────────────────────────────
const plans = [
  {
    name: '免费版',
    price: '0',
    unit: '/ 月',
    desc: '轻量体验，感受 AI 创作魅力',
    color: '#42b883',
    features: ['每月 100 算力点', '5 个项目', '基础节点工作流', '社区模板（限量）', '标清导出'],
    cta: '免费开始',
    highlight: false,
  },
  {
    name: '专业版',
    price: '99',
    unit: '/ 月',
    desc: '全功能解锁，专业级创作体验',
    color: '#646cff',
    features: ['每月 2000 算力点', '无限项目', '全量节点 + 批量生成', '全部模板 + 资产库', '4K 导出 + API 调用'],
    cta: '立即升级',
    highlight: true,
  },
  {
    name: '企业版',
    price: '联系我们',
    unit: '',
    desc: '私有部署，专属 Agent 接入方案',
    color: '#ff6b6b',
    features: ['无限算力点', '多成员协作', '私有化部署', '专属 API + OpenClaw 接入', '优先技术支持'],
    cta: '联系销售',
    highlight: false,
  },
]

// ── Agent input ───────────────────────────────────────────────────────────────
const agentPrompt = ref('')
function startAgent() {
  if (!agentPrompt.value.trim()) return
  router.push({ path: '/canvas', query: { agent: agentPrompt.value } })
}

// ── Create project and open canvas ───────────────────────────────────────────
async function createAndOpen() {
  const project = await projectStore.createProject('未命名项目')
  router.push({ path: '/canvas', query: { projectId: project.id } })
}

// ── Time helper ───────────────────────────────────────────────────────────────
function formatTime(ts) {
  if (!ts) return ''
  const diff = Date.now() - new Date(ts).getTime()
  const m = Math.floor(diff / 60000)
  if (m < 1) return '刚刚'
  if (m < 60) return `${m}分钟前`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h}小时前`
  const d = Math.floor(h / 24)
  if (d < 7) return `${d}天前`
  return new Date(ts).toLocaleDateString('zh-CN')
}
</script>

<template>
  <div class="home">

    <!-- ═══════════════════════ NAVBAR ═══════════════════════ -->
    <nav class="navbar">
      <div class="nav-inner">
        <a class="nav-logo" href="/">
          <span class="logo-hex">⬡</span>
          <span class="logo-name">CanvasFlow</span>
          <span class="logo-badge">AIGC</span>
        </a>

        <div class="nav-links">
          <a v-for="item in navItems" :key="item.label" :href="item.to" class="nav-link">
            {{ item.label }}
          </a>
        </div>

        <div class="nav-actions">
          <button class="btn-ghost" @click="router.push('/canvas')">开始创作</button>
          <button class="btn-primary">登录 / 注册</button>
        </div>
      </div>
    </nav>

    <!-- ═══════════════════════ HERO ═══════════════════════ -->
    <section class="hero">
      <div class="hero-bg">
        <div class="hero-orb orb1" />
        <div class="hero-orb orb2" />
        <div class="hero-orb orb3" />
        <div class="grid-lines" />
      </div>

      <div class="hero-content">
        <div class="hero-tag">下一代 AI 视频创作平台</div>
        <h1 class="hero-title">
          用 AI 的方式<br />
          <span class="gradient-text">讲述你的故事</span>
        </h1>
        <p class="hero-desc">
          无限画布节点工作流 × 30+ 顶级视频模型 × Agent 全自动<br />
          从创意到成片，快人 10 倍
        </p>

        <!-- ── 最近项目 ── -->
        <div class="hero-projects">
          <div class="hero-projects-header">
            <span class="hero-projects-title">最近项目</span>
            <a href="/projects" class="hero-projects-more">查看全部 →</a>
          </div>
          <div class="hero-project-grid">
            <div class="hp-card hp-card-new" @click="createAndOpen">
              <span class="hp-new-icon">+</span>
              <span class="hp-new-label">新建项目</span>
            </div>
            <div
              v-for="p in recentProjects"
              :key="p.id"
              class="hp-card"
              @click="router.push({ path: '/canvas', query: { projectId: p.id } })"
            >
              <div class="hp-thumb"><span class="hp-thumb-icon">⬡</span></div>
              <div class="hp-info">
                <div class="hp-name">{{ p.name }}</div>
                <div class="hp-meta">{{ formatTime(p.updateTime) }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Agent entry -->
        <div class="hero-entries">
          <div class="entry-card entry-agent">
            <div class="entry-icon">🤖</div>
            <div class="entry-info">
              <div class="entry-title">Agent 全自动</div>
              <div class="entry-desc">一句话描述，AI 全程代劳<br />OpenClaw / API 直接调用</div>
            </div>
            <div class="entry-arrow">→</div>
            <div class="agent-input-wrap" @click.stop>
              <input
                v-model="agentPrompt"
                class="agent-input"
                placeholder="描述你想生成的视频，例如：生成一个 30 秒的悬疑短剧..."
                @keydown.enter="startAgent"
              />
              <button class="agent-go" :disabled="!agentPrompt.trim()" @click="startAgent">生成</button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══════════════════════ TEMPLATES ═══════════════════════ -->
    <section class="section section-dark">
      <div class="section-inner">
        <div class="section-header">
          <h2 class="section-title">模板广场</h2>
          <a href="/templates" class="section-more">全部模板 →</a>
        </div>
        <div class="template-scroll">
          <div
            v-for="t in templates"
            :key="t.id"
            class="template-card"
            :style="{ '--accent': t.color }"
          >
            <div class="template-thumb" :style="{ background: t.color + '22', borderColor: t.color + '44' }">
              <span class="template-icon">🎬</span>
            </div>
            <div class="template-name">{{ t.name }}</div>
            <div class="template-meta">
              <span class="template-cat" :style="{ color: t.color }">{{ t.category }}</span>
              <span class="template-uses">{{ t.uses }} 次使用</span>
            </div>
            <button class="template-use" :style="{ borderColor: t.color + '66', color: t.color }">
              使用模板
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══════════════════════ FEATURES ═══════════════════════ -->
    <section class="section">
      <div class="section-inner">
        <div class="section-header center">
          <h2 class="section-title">核心能力</h2>
          <p class="section-sub">从创意到成片，每一步都有 AI 加持</p>
        </div>
        <div class="feature-grid">
          <div
            v-for="f in features"
            :key="f.title"
            class="feature-card"
            :style="{ '--accent': f.color }"
          >
            <div class="feature-icon" :style="{ color: f.color, background: f.color + '18' }">
              {{ f.icon }}
            </div>
            <h3 class="feature-title">{{ f.title }}</h3>
            <p class="feature-desc">{{ f.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══════════════════════ MODELS ═══════════════════════ -->
    <section class="section section-dark">
      <div class="section-inner">
        <div class="section-header center">
          <h2 class="section-title">30+ 顶级模型</h2>
          <p class="section-sub">持续接入全球最新视频 / 图像大模型</p>
        </div>
        <div class="model-row">
          <div
            v-for="m in models"
            :key="m.name"
            class="model-chip"
            :style="{ borderColor: m.color + '66', color: m.color }"
          >
            <span class="model-tag" :style="{ background: m.color + '22' }">{{ m.tag }}</span>
            {{ m.name }}
          </div>
          <div class="model-chip model-more">+24 更多</div>
        </div>
      </div>
    </section>

    <!-- ═══════════════════════ PRICING ═══════════════════════ -->
    <section class="section" id="pricing">
      <div class="section-inner">
        <div class="section-header center">
          <h2 class="section-title">定价方案</h2>
          <p class="section-sub">按需选择，随时升降级</p>
        </div>
        <div class="pricing-grid">
          <div
            v-for="plan in plans"
            :key="plan.name"
            :class="['pricing-card', plan.highlight && 'pricing-highlight']"
            :style="{ '--accent': plan.color }"
          >
            <div v-if="plan.highlight" class="pricing-hot">最受欢迎</div>
            <div class="plan-name" :style="{ color: plan.color }">{{ plan.name }}</div>
            <div class="plan-price">
              <span class="price-num" :style="plan.price === '联系我们' ? { fontSize: '22px' } : {}">
                {{ plan.price === '联系我们' ? '' : '¥' }}{{ plan.price }}
              </span>
              <span class="price-unit">{{ plan.unit }}</span>
              <span v-if="plan.price === '联系我们'" class="price-contact">联系我们</span>
            </div>
            <p class="plan-desc">{{ plan.desc }}</p>
            <ul class="plan-features">
              <li v-for="feat in plan.features" :key="feat">
                <span class="feat-check" :style="{ color: plan.color }">✓</span>
                {{ feat }}
              </li>
            </ul>
            <button
              class="plan-cta"
              :class="plan.highlight ? 'cta-fill' : 'cta-outline'"
              :style="plan.highlight
                ? { background: plan.color, borderColor: plan.color }
                : { borderColor: plan.color + '88', color: plan.color }"
            >
              {{ plan.cta }}
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══════════════════════ FOOTER ═══════════════════════ -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-brand">
          <span class="logo-hex">⬡</span>
          <span class="logo-name">CanvasFlow</span>
        </div>
        <div class="footer-links">
          <a href="/help">帮助文档</a>
          <a href="/community">社区</a>
          <a href="/privacy">隐私政策</a>
          <a href="/terms">用户协议</a>
        </div>
        <div class="footer-copy">© 2025 AIGCCanvasFlow. All rights reserved.</div>
      </div>
    </footer>

  </div>
</template>

<style scoped>
/* ── Base ── */
.home {
  background: #0b0b16;
  color: #e0e0f0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  min-height: 100vh;
  overflow-y: auto;
}

/* ── Navbar ── */
.navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(11, 11, 22, 0.85);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid #1e1e35;
}
.nav-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 32px;
}
.nav-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  flex-shrink: 0;
}
.logo-hex { font-size: 22px; color: #646cff; }
.logo-name { font-size: 16px; font-weight: 700; color: #e0e0f0; }
.logo-badge {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 1px;
  background: #646cff22;
  border: 1px solid #646cff44;
  color: #a0aaff;
  padding: 2px 5px;
  border-radius: 4px;
}
.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}
.nav-link {
  padding: 6px 12px;
  font-size: 13px;
  color: #8080a0;
  text-decoration: none;
  border-radius: 6px;
  transition: color 0.15s, background 0.15s;
}
.nav-link:hover { color: #e0e0f0; background: #ffffff08; }
.nav-actions { display: flex; gap: 8px; flex-shrink: 0; }
.btn-ghost {
  padding: 7px 16px;
  background: none;
  border: 1px solid #2e2e50;
  border-radius: 8px;
  color: #a0a0c0;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}
.btn-ghost:hover { border-color: #646cff88; color: #a0aaff; }
.btn-primary {
  padding: 7px 18px;
  background: #646cff;
  border: 1px solid #646cff;
  border-radius: 8px;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}
.btn-primary:hover { background: #7c83ff; }

/* ── Hero ── */
.hero {
  position: relative;
  min-height: 60vh;
  display: flex;
  align-items: center;
  overflow: auto;
}
.hero-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.hero-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.18;
}
.orb1 { width: 600px; height: 600px; background: #646cff; top: -200px; left: -100px; }
.orb2 { width: 500px; height: 500px; background: #42b883; top: 100px; right: -150px; }
.orb3 { width: 400px; height: 400px; background: #ff6b6b; bottom: -100px; left: 30%; }
.grid-lines {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(100,108,255,0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(100,108,255,0.05) 1px, transparent 1px);
  background-size: 40px 40px;
}
.hero-content {
  position: relative;
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 24px;
  width: 100%;
}
.hero-tag {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: #646cff;
  background: #646cff18;
  border: 1px solid #646cff33;
  border-radius: 20px;
  padding: 5px 14px;
  margin-bottom: 24px;
}
.hero-title {
  font-size: clamp(36px, 5vw, 64px);
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -1px;
  margin-bottom: 20px;
  color: #f0f0ff;
}
.gradient-text {
  background: linear-gradient(135deg, #646cff 0%, #42b883 50%, #ff6b6b 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.hero-desc {
  font-size: 16px;
  color: #6060a0;
  line-height: 1.8;
  margin-bottom: 48px;
  max-width: 560px;
}

/* ── Hero recent projects ── */
.hero-projects {
  margin-top: 40px;
  margin-bottom: 40px;
  width: 100%;
}
.hero-projects-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 16px;
  width: 100%;
}
.hero-projects-title {
  font-size: 16px;
  font-weight: 700;
  color: #d0d0f0;
}
.hero-projects-more {
  font-size: 12px;
  color: #646cff;
  text-decoration: none;
}
.hero-projects-more:hover { color: #a0aaff; }
.hero-project-grid {
  display: flex;
  gap: 14px;
  overflow-x: auto;
  scrollbar-width: none;
}
.hero-project-grid::-webkit-scrollbar { display: none; }
.hp-card {
  flex-shrink: 0;
  width: 160px;
  background: rgba(255,255,255,0.04);
  border: 1px solid #2e2e50;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.15s, transform 0.15s, background 0.15s;
}
.hp-card:hover { border-color: #646cff66; transform: translateY(-2px); }
.hp-thumb {
  width: 100%;
  aspect-ratio: 4/3;
  background: #0d0d1a;
  display: flex;
  align-items: center;
  justify-content: center;
}
.hp-thumb-icon { font-size: 24px; opacity: 0.15; }
.hp-info { padding: 8px 10px 10px; }
.hp-name {
  font-size: 12px;
  font-weight: 600;
  color: #c0c0e0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 3px;
}
.hp-meta { font-size: 10px; color: #444466; }
.hp-card-new {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  aspect-ratio: unset;
  min-height: 140px;
  border-style: dashed;
  border-color: #2e2e50;
}
.hp-card-new:hover { border-color: #646cff88; background: #646cff08; }
.hp-new-icon { font-size: 22px; color: #333355; line-height: 1; }
.hp-new-label { font-size: 11px; color: #444466; }

/* Entry cards */
.hero-entries {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
  max-width: 560px;
}
.entry-card {
  background: rgba(255,255,255,0.03);
  border: 1px solid #2e2e50;
  border-radius: 16px;
  padding: 24px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s, transform 0.2s;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  position: relative;
  overflow: hidden;
}
.entry-card::before {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity 0.2s;
  border-radius: 16px;
}
.entry-human::before { background: linear-gradient(135deg, #646cff10, transparent); }
.entry-agent::before  { background: linear-gradient(135deg, #42b88310, transparent); }
.entry-card:hover { transform: translateY(-2px); }
.entry-human:hover { border-color: #646cff66; }
.entry-agent:hover  { border-color: #42b88366; }
.entry-card:hover::before { opacity: 1; }
.entry-icon { font-size: 28px; flex-shrink: 0; margin-top: 2px; }
.entry-info { flex: 1; }
.entry-title { font-size: 16px; font-weight: 700; color: #e0e0f0; margin-bottom: 6px; }
.entry-desc  { font-size: 12px; color: #6060a0; line-height: 1.6; }
.entry-arrow { font-size: 18px; color: #444466; align-self: center; transition: transform 0.15s; }
.entry-card:hover .entry-arrow { transform: translateX(4px); color: #a0a0c0; }

/* Agent input */
.agent-input-wrap {
  position: absolute;
  bottom: 0; left: 0; right: 0;
  padding: 12px;
  background: linear-gradient(transparent, rgba(11,11,22,0.95));
  display: flex;
  gap: 8px;
  border-radius: 0 0 16px 16px;
}
.entry-agent { padding-bottom: 68px; }
.agent-input {
  flex: 1;
  background: #12121e;
  border: 1px solid #2e2e50;
  border-radius: 8px;
  color: #d0d0f0;
  font-size: 11px;
  padding: 7px 10px;
  outline: none;
  font-family: inherit;
  transition: border-color 0.15s;
}
.agent-input:focus { border-color: #42b88388; }
.agent-input::placeholder { color: #333355; }
.agent-go {
  padding: 7px 16px;
  background: #42b883;
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s;
  white-space: nowrap;
}
.agent-go:hover:not(:disabled) { background: #5acf96; }
.agent-go:disabled { opacity: 0.4; cursor: not-allowed; }

/* ── Sections ── */
.section { padding: 72px 0; }
.section-dark { background: #080810; }
.section-inner { max-width: 1200px; margin: 0 auto; padding: 0 24px; }
.section-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 32px;
}
.section-header.center {
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 8px;
}
.section-title {
  font-size: 26px;
  font-weight: 700;
  color: #e0e0f0;
}
.section-sub { font-size: 14px; color: #6060a0; }
.section-more { font-size: 13px; color: #646cff; text-decoration: none; }
.section-more:hover { color: #a0aaff; }

/* ── Recent Projects ── */
.project-grid {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding-bottom: 4px;
  scrollbar-width: none;
}
.project-grid::-webkit-scrollbar { display: none; }
.project-card {
  flex-shrink: 0;
  width: 200px;
  background: #12121e;
  border: 1px solid #1e1e35;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.15s, transform 0.15s;
}
.project-card:hover { border-color: #646cff44; transform: translateY(-2px); }
.project-thumb {
  width: 100%;
  aspect-ratio: 4/3;
  background: #0d0d1a;
  display: flex;
  align-items: center;
  justify-content: center;
}
.thumb-placeholder { font-size: 32px; opacity: 0.15; }
.project-info { padding: 10px 12px 12px; }
.project-name { font-size: 13px; font-weight: 600; color: #d0d0f0; margin-bottom: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.project-meta { font-size: 11px; color: #444466; }
.dot { opacity: 0.4; }

.project-new {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  aspect-ratio: unset;
  min-height: 188px;
  border-style: dashed;
}
.project-new:hover { border-color: #646cff88; background: #646cff08; }
.new-icon { font-size: 28px; color: #333355; }
.new-label { font-size: 12px; color: #444466; }

/* ── Templates ── */
.template-scroll {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding-bottom: 8px;
  scrollbar-width: thin;
  scrollbar-color: #2e2e50 transparent;
}
.template-card {
  flex-shrink: 0;
  width: 160px;
  background: #12121e;
  border: 1px solid #1e1e35;
  border-radius: 12px;
  padding: 12px;
  cursor: pointer;
  transition: border-color 0.15s, transform 0.15s;
}
.template-card:hover { border-color: var(--accent, #646cff); transform: translateY(-2px); }
.template-thumb {
  height: 100px;
  border-radius: 8px;
  border: 1px solid;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
}
.template-icon { font-size: 28px; opacity: 0.6; }
.template-name { font-size: 13px; font-weight: 600; color: #d0d0f0; margin-bottom: 4px; }
.template-meta { display: flex; justify-content: space-between; font-size: 10px; margin-bottom: 10px; }
.template-cat { font-weight: 600; }
.template-uses { color: #444466; }
.template-use {
  width: 100%;
  padding: 6px;
  background: none;
  border: 1px solid;
  border-radius: 6px;
  font-size: 11px;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s;
}
.template-use:hover { background: var(--accent, #646cff); color: #fff; border-color: var(--accent, #646cff); }

/* ── Features ── */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 20px;
}
.feature-card {
  background: #12121e;
  border: 1px solid #1e1e35;
  border-radius: 16px;
  padding: 28px;
  transition: border-color 0.2s, transform 0.2s;
}
.feature-card:hover { border-color: var(--accent, #646cff); transform: translateY(-3px); }
.feature-icon {
  width: 48px; height: 48px;
  border-radius: 12px;
  font-size: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}
.feature-title { font-size: 15px; font-weight: 700; color: #e0e0f0; margin-bottom: 10px; }
.feature-desc { font-size: 13px; color: #6060a0; line-height: 1.7; }

/* ── Models ── */
.model-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
}
.model-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #12121e;
  border: 1px solid;
  border-radius: 100px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, transform 0.15s;
}
.model-chip:hover { transform: translateY(-1px); background: #1a1a2e; }
.model-more { border-color: #2e2e50; color: #444466; }
.model-tag {
  font-size: 9px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* ── Pricing ── */
.pricing-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  align-items: start;
}
.pricing-card {
  background: #12121e;
  border: 1px solid #1e1e35;
  border-radius: 20px;
  padding: 32px;
  position: relative;
  transition: border-color 0.2s, transform 0.2s;
}
.pricing-card:hover { transform: translateY(-3px); }
.pricing-highlight {
  border-color: var(--accent, #646cff);
  background: linear-gradient(180deg, #646cff08 0%, #12121e 100%);
  transform: scale(1.03);
  box-shadow: 0 0 40px rgba(100,108,255,0.12);
}
.pricing-hot {
  position: absolute;
  top: -12px; left: 50%;
  transform: translateX(-50%);
  background: #646cff;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 14px;
  border-radius: 20px;
  white-space: nowrap;
}
.plan-name { font-size: 14px; font-weight: 700; margin-bottom: 8px; }
.plan-price {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 8px;
}
.price-num { font-size: 40px; font-weight: 800; color: #f0f0ff; }
.price-unit { font-size: 13px; color: #6060a0; }
.price-contact { font-size: 28px; font-weight: 800; color: #f0f0ff; }
.plan-desc { font-size: 12px; color: #6060a0; margin-bottom: 20px; }
.plan-features {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 28px;
}
.plan-features li { display: flex; gap: 8px; font-size: 13px; color: #a0a0c0; }
.feat-check { font-size: 12px; font-weight: 700; flex-shrink: 0; }
.plan-cta {
  width: 100%;
  padding: 12px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}
.cta-fill { color: #fff; border: 1px solid; }
.cta-fill:hover { filter: brightness(1.15); }
.cta-outline { background: none; }
.cta-outline:hover { background: var(--accent, #646cff); color: #fff; }

/* ── Footer ── */
.footer {
  background: #080810;
  border-top: 1px solid #1e1e35;
  padding: 32px 0;
}
.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 32px;
  flex-wrap: wrap;
}
.footer-brand { display: flex; align-items: center; gap: 8px; }
.footer-links { display: flex; gap: 20px; flex: 1; }
.footer-links a { font-size: 13px; color: #444466; text-decoration: none; }
.footer-links a:hover { color: #a0a0c0; }
.footer-copy { font-size: 12px; color: #333355; }
</style>

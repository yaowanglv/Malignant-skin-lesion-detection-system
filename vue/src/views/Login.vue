<template>
  <div class="container">
    <!-- LEFT: Scanner + Brand -->
    <div class="panel-brand">
      <div class="scanner-area">
        <div class="scanner" :class="{ 'scan-paused': !scanEnabled }">
          <div class="scanner-tick"></div>
          <div class="scanner-tick"></div>
          <div class="scanner-tick"></div>
          <div class="scanner-tick"></div>
          <div class="scanner-tick"></div>
          <div class="scanner-tick"></div>
          <div class="scanner-tick"></div>
          <div class="scanner-tick"></div>
          <div class="scanner-tick"></div>
          <div class="scanner-tick"></div>
          <div class="scanner-tick"></div>
          <div class="scanner-tick"></div>
          <div class="scanner-rings">
            <div class="scanner-inner"></div>
          </div>
          <div class="scanner-beam" :style="{ animationPlayState: scanEnabled ? 'running' : 'paused' }"></div>
          <div class="scanner-crosshair-h"></div>
          <div class="scanner-crosshair-v"></div>
          <div class="scanner-center"></div>
          <div class="marker marker-1" v-show="markersEnabled">
            <div class="marker-dot"></div>
            <div class="marker-ring"></div>
          </div>
          <div class="marker marker-2" v-show="markersEnabled">
            <div class="marker-dot"></div>
            <div class="marker-ring"></div>
          </div>
          <div class="marker marker-3" v-show="markersEnabled">
            <div class="marker-dot"></div>
            <div class="marker-ring"></div>
          </div>
          <div class="marker marker-4" v-show="markersEnabled">
            <div class="marker-dot"></div>
            <div class="marker-ring"></div>
          </div>
          <div class="marker marker-5" v-show="markersEnabled">
            <div class="marker-dot"></div>
            <div class="marker-ring"></div>
          </div>
          <div class="marker marker-6" v-show="markersEnabled">
            <div class="marker-dot"></div>
            <div class="marker-ring"></div>
          </div>
          <div class="marker marker-7" v-show="markersEnabled">
            <div class="marker-dot"></div>
            <div class="marker-ring"></div>
          </div>
          <div class="marker marker-8" v-show="markersEnabled">
            <div class="marker-dot"></div>
            <div class="marker-ring"></div>
          </div>
        </div>
        <div class="brand-content">
          <div class="brand-title">
            大模型驱动视频流皮肤恶性病灶实时辅助研判系统
          </div>
          <div class="brand-subtitle">
            灵肤<span class="teal">智诊</span>
          </div>
          <div class="brand-tagline">
            AI 面部皮肤分析 · 实时扫描
          </div>
        </div>
      </div>
      <div class="particle particle-1" v-show="particlesEnabled"></div>
      <div class="particle particle-2" v-show="particlesEnabled"></div>
      <div class="particle particle-3" v-show="particlesEnabled"></div>
      <div class="particle particle-4" v-show="particlesEnabled"></div>
      <div class="particle particle-5" v-show="particlesEnabled"></div>
      <div class="particle particle-6" v-show="particlesEnabled"></div>
    </div>

    <!-- RIGHT: Login Form -->
    <div class="panel-form">
      <div class="form-wrapper">
        <div class="form-card">
          <div class="form-header">
            <h1>登录</h1>
            <p>欢迎回到灵肤智诊</p>
          </div>
          <form @submit.prevent="login">
            <div class="input-group">
                        <label for="username">用户名</label>
                        <div class="input-field">
                            <span class="input-icon icon-user">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                                    <circle cx="12" cy="8" r="4.5"/>
                                    <path d="M4 21c0-4.5 3.6-8 8-8s8 3.5 8 8"/>
                                </svg>
                            </span>
                            <input type="text" id="username" v-model="form.username" placeholder="请输入用户名或工号" autocomplete="username">
                        </div>
                    </div>

            <div class="input-group">
                        <label for="password">密码</label>
                        <div class="input-field">
                            <span class="input-icon icon-lock">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                                    <rect x="5" y="11" width="14" height="10" rx="2"/>
                                    <circle cx="12" cy="16" r="1.2"/>
                                    <path d="M8 11V7a4 4 0 0 1 8 0v4"/>
                                </svg>
                            </span>
                            <input :type="showPassword ? 'text' : 'password'" id="password" v-model="form.password" placeholder="请输入密码" autocomplete="current-password">
                            <button type="button" class="password-toggle" @click="showPassword = !showPassword" aria-label="切换密码可见性">
                                <svg v-if="!showPassword" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                                    <circle cx="12" cy="12" r="3"/>
                                </svg>
                                <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                                    <line x1="1" y1="1" x2="23" y2="23"/>
                                </svg>
                            </button>
                        </div>
                    </div>

            <div class="form-options">
                        <label class="remember-me">
                            <input type="checkbox" v-model="form.remember">
                            <span class="checkbox-custom"></span>
                            记住我
                        </label>
                        <a href="#" class="forgot-link"></a>
                    </div>

                    <button type="submit" class="btn-login">登 录</button>
                </form>

            </div>
      </div>
    </div>

    <!-- Tweaks Panel -->
    <button class="tweaks-toggle" @click="tweaksOpen = !tweaksOpen" aria-label="调整设置">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="3"/>
        <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>
      </svg>
    </button>

    <div class="tweaks-panel" :class="{ open: tweaksOpen }">
      <h3>界面调整</h3>
      <div class="tweak-item">
        <span class="tweak-label">扫描动效</span>
        <label class="toggle-switch">
          <input type="checkbox" v-model="scanEnabled" />
          <span class="toggle-track"></span>
        </label>
      </div>
      <div class="tweak-item">
        <span class="tweak-label">粒子效果</span>
        <label class="toggle-switch">
          <input type="checkbox" v-model="particlesEnabled" />
          <span class="toggle-track"></span>
        </label>
      </div>
      <div class="tweak-item">
        <span class="tweak-label">病灶标记</span>
        <label class="toggle-switch">
          <input type="checkbox" v-model="markersEnabled" />
          <span class="toggle-track"></span>
        </label>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from "vue";
import request from "@/utils/request";
import { ElMessage } from "element-plus";
import router from "@/router";

const form = reactive({
  username: '',
  password: '',
  remember: true
});

const errors = reactive({
  username: '',
  password: ''
});

const showPassword = ref(false);

// Tweaks
const tweaksOpen = ref(false);
const scanEnabled = ref(true);
const particlesEnabled = ref(true);
const markersEnabled = ref(true);

// Close tweaks panel on outside click
watch(tweaksOpen, (open) => {
  if (open) {
    const closePanel = (e) => {
      if (!e.target.closest('.tweaks-panel') && !e.target.closest('.tweaks-toggle')) {
        tweaksOpen.value = false;
        document.removeEventListener('click', closePanel);
      }
    };
    setTimeout(() => document.addEventListener('click', closePanel), 0);
  }
});

const validateForm = () => {
  let valid = true;
  if (!form.username) {
    errors.username = '请输入账号';
    valid = false;
  } else if (form.username.length < 4) {
    errors.username = '最少4位';
    valid = false;
  } else {
    errors.username = '';
  }
  if (!form.password) {
    errors.password = '请输入密码';
    valid = false;
  } else {
    errors.password = '';
  }
  return valid;
};

const login = () => {
  if (!validateForm()) return;
  request.post('/login', { username: form.username, password: form.password })
    .then(res => {
      if (res.code === '200') {
        localStorage.setItem("token", res.data?.token || '');
        localStorage.setItem("code_user", JSON.stringify(res.data?.user || {}));
        ElMessage.success('登录成功');
        router.push('/manager/dataview');
      } else {
        ElMessage.error(res.msg || '账号或密码错误');
      }
    }).catch(error => {
      console.error('登录请求失败:', error);
      ElMessage.error('账号或者密码错误');
    });
};
</script>

<style>
/* ============================================
   Design Tokens — Light Theme (global)
   ============================================ */
:root {
    --bg-page: #f0f4f8;
    --bg-left: #ffffff;
    --bg-right: #f8fafc;
    --card-bg: #ffffff;
    --card-border: #e8edf4;
    --card-shadow: 0 8px 40px rgba(0, 0, 0, 0.06), 0 2px 8px rgba(0, 0, 0, 0.04);
    --card-shadow-hover: 0 12px 48px rgba(0, 0, 0, 0.08), 0 2px 8px rgba(0, 0, 0, 0.04);
    --brand-teal: #0d9488;
    --brand-teal-light: rgba(13, 148, 136, 0.1);
    --brand-teal-dim: rgba(13, 148, 136, 0.06);
    --brand-tech: #3b82f6;
    --text-primary: #0f172a;
    --text-secondary: #475569;
    --text-muted: #94a3b8;
    --input-bg: #f8fafc;
    --input-border: #e2e8f0;
    --input-border-hover: #cbd5e1;
    --input-focus-ring: rgba(13, 148, 136, 0.2);
    --scan-ring: rgba(13, 148, 136, 0.2);
    --scan-ring-light: rgba(13, 148, 136, 0.1);
    --scan-crosshair: rgba(13, 148, 136, 0.15);
    --marker-teal: rgba(13, 148, 136, 0.7);
    --marker-blue: rgba(59, 130, 246, 0.7);
    --radius-card: 24px;
    --radius-input: 12px;
    --radius-button: 10px;
    --font-sans: "PingFang SC", "Microsoft YaHei", "Noto Sans SC", "HarmonyOS Sans", sans-serif;
    --scan-duration: 4s;
}

</style>

<style scoped>
/* ============================================
   Layout
   ============================================ */
.container {
    display: flex;
    height: 100vh;
    width: 100%;
    overflow: hidden;
    font-family: var(--font-sans);
    background: var(--bg-page);
    color: var(--text-primary);
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
}

/* ---------- Left Panel ---------- */
.panel-brand {
    flex: 0 0 55%;
    position: relative;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    background: var(--bg-left);
    overflow: hidden;
}

/* Subtle grid overlay */
.panel-brand::before {
    content: '';
    position: absolute;
    inset: 0;
    background-image:
        linear-gradient(var(--brand-teal-dim) 1px, transparent 1px),
        linear-gradient(90deg, var(--brand-teal-dim) 1px, transparent 1px);
    background-size: 48px 48px;
    pointer-events: none;
    z-index: 1;
}

/* Top-right decorative glow */
.panel-brand::after {
    content: '';
    position: absolute;
    top: -30%;
    right: -20%;
    width: 600px;
    height: 600px;
    background: radial-gradient(circle, rgba(13, 148, 136, 0.04) 0%, transparent 70%);
    pointer-events: none;
    z-index: 0;
}

/* ---------- Right Panel ---------- */
.panel-form {
    flex: 0 0 45%;
    position: relative;
    display: flex;
    justify-content: center;
    align-items: center;
    background: var(--bg-right);
    overflow: hidden;
}

.panel-form::before {
    content: '';
    position: absolute;
    bottom: -30%;
    left: -20%;
    width: 500px;
    height: 500px;
    background: radial-gradient(circle, rgba(59, 130, 246, 0.04) 0%, transparent 70%);
    pointer-events: none;
}

/* ============================================
   Scanner (face scan)
   ============================================ */
.scanner-area {
    position: relative;
    z-index: 2;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 36px;
}

.scanner {
    position: relative;
    width: 340px;
    height: 340px;
    border-radius: 50%;
    background: url('./veer-123154948.jpg') center / cover no-repeat;
    box-shadow: 0 0 0 4px rgba(13, 148, 136, 0.15), 0 8px 40px rgba(13, 148, 136, 0.1);
}

.scanner.scan-paused {
    animation: none;
}

/* Outer glow ring */
.scanner::before {
    content: '';
    position: absolute;
    inset: -18px;
    border-radius: 50%;
    border: 1.5px solid rgba(13, 148, 136, 0.12);
    animation: pulse-ring 3s ease-in-out infinite;
}

@keyframes pulse-ring {
    0%, 100% { transform: scale(1); opacity: 1; }
    50% { transform: scale(1.04); opacity: 0.3; }
}

/* Gradient overlay on the face */
.scanner::after {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: 50%;
    background: radial-gradient(circle at 50% 50%, transparent 50%, rgba(13, 148, 136, 0.04) 100%);
    pointer-events: none;
    z-index: 1;
}

/* Rings container */
.scanner-rings {
    position: absolute;
    inset: 0;
    border-radius: 50%;
    border: 1.5px solid var(--scan-ring);
    z-index: 2;
}

.scanner-rings::before,
.scanner-rings::after {
    content: '';
    position: absolute;
    border-radius: 50%;
    border: 1.5px solid transparent;
}

.scanner-rings::before {
    inset: 16%;
    border-color: var(--scan-ring);
}

.scanner-rings::after {
    inset: 32%;
    border-color: var(--scan-ring-light);
}

/* Inner very center ring */
.scanner-inner {
    position: absolute;
    inset: 48%;
    border-radius: 50%;
    background: rgba(13, 148, 136, 0.08);
    border: 1px solid rgba(13, 148, 136, 0.15);
}

/* Center dot */
.scanner-center {
    position: absolute;
    top: 50%;
    left: 50%;
    width: 6px;
    height: 6px;
    transform: translate(-50%, -50%);
    border-radius: 50%;
    background: var(--brand-teal);
    box-shadow: 0 0 16px rgba(13, 148, 136, 0.5);
    z-index: 5;
}

/* Scan beam */
.scanner-beam {
    position: absolute;
    inset: 0;
    border-radius: 50%;
    background: conic-gradient(
        from 0deg,
        transparent 0deg,
        rgba(13, 148, 136, 0.2) 3deg,
        rgba(13, 148, 136, 0.06) 8deg,
        transparent 14deg
    );
    animation: scan-rotate var(--scan-duration) linear infinite;
    transform-origin: center;
    z-index: 3;
    pointer-events: none;
}

@keyframes scan-rotate {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}

/* Crosshair lines */
.scanner-crosshair-h,
.scanner-crosshair-v {
    position: absolute;
    z-index: 2;
    pointer-events: none;
}

.scanner-crosshair-h {
    top: 50%;
    left: 4%;
    right: 4%;
    height: 1px;
    background: linear-gradient(90deg, transparent, var(--scan-crosshair) 30%, var(--scan-crosshair) 70%, transparent);
    transform: translateY(-50%);
}

.scanner-crosshair-v {
    left: 50%;
    top: 4%;
    bottom: 4%;
    width: 1px;
    background: linear-gradient(180deg, transparent, var(--scan-crosshair) 30%, var(--scan-crosshair) 70%, transparent);
    transform: translateX(-50%);
}

/* Tick marks */
.scanner-tick {
    position: absolute;
    z-index: 2;
    top: 0;
    left: 50%;
    width: 1.5px;
    height: 10px;
    background: rgba(13, 148, 136, 0.2);
    transform-origin: 0 170px;
}
.scanner-tick:nth-child(1)  { transform: translateX(-50%) rotate(0deg); }
.scanner-tick:nth-child(2)  { transform: translateX(-50%) rotate(30deg); }
.scanner-tick:nth-child(3)  { transform: translateX(-50%) rotate(60deg); }
.scanner-tick:nth-child(4)  { transform: translateX(-50%) rotate(90deg); }
.scanner-tick:nth-child(5)  { transform: translateX(-50%) rotate(120deg); }
.scanner-tick:nth-child(6)  { transform: translateX(-50%) rotate(150deg); }
.scanner-tick:nth-child(7)  { transform: translateX(-50%) rotate(180deg); }
.scanner-tick:nth-child(8)  { transform: translateX(-50%) rotate(210deg); }
.scanner-tick:nth-child(9)  { transform: translateX(-50%) rotate(240deg); }
.scanner-tick:nth-child(10) { transform: translateX(-50%) rotate(270deg); }
.scanner-tick:nth-child(11) { transform: translateX(-50%) rotate(300deg); }
.scanner-tick:nth-child(12) { transform: translateX(-50%) rotate(330deg); }

/* ============================================
   Lesion Markers (on face)
   ============================================ */
.marker {
    position: absolute;
    z-index: 4;
    pointer-events: none;
}

.marker-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--marker-teal);
    box-shadow: 0 0 10px rgba(13, 148, 136, 0.5);
}

.marker-ring {
    position: absolute;
    top: 50%;
    left: 50%;
    width: 24px;
    height: 24px;
    margin: -12px 0 0 -12px;
    border-radius: 50%;
    border: 1.5px solid rgba(13, 148, 136, 0.35);
    animation: marker-pulse 2s ease-in-out infinite;
}

.marker-1  { top: 22%; left: 55%; }
.marker-2  { top: 32%; left: 68%; }
.marker-3  { top: 52%; left: 38%; }
.marker-4  { top: 42%; left: 24%; }
.marker-5  { top: 48%; left: 58%; }
.marker-6  { top: 62%; left: 48%; }
.marker-7  { top: 28%; left: 38%; }
.marker-8  { top: 55%; left: 72%; }

.marker-3 .marker-dot,
.marker-6 .marker-dot {
    background: var(--marker-blue);
    box-shadow: 0 0 10px rgba(59, 130, 246, 0.5);
}
.marker-3 .marker-ring,
.marker-6 .marker-ring {
    border-color: rgba(59, 130, 246, 0.35);
}

.marker-2 .marker-dot,
.marker-5 .marker-dot {
    width: 10px;
    height: 10px;
}
.marker-2 .marker-ring,
.marker-5 .marker-ring {
    width: 32px;
    height: 32px;
    margin: -16px 0 0 -16px;
}

@keyframes marker-pulse {
    0%, 100% { transform: scale(1); opacity: 0.5; }
    50% { transform: scale(1.4); opacity: 0.1; }
}

/* ============================================
   Brand Text (Left)
   ============================================ */
.brand-content {
    position: relative;
    z-index: 2;
    text-align: center;
}

.brand-title {
    font-size: clamp(20px, 3vw, 32px);
    font-weight: 700;
    letter-spacing: 1px;
    color: var(--text-primary);
    margin-bottom: 10px;
    white-space: nowrap;
}

.brand-subtitle {
    font-size: clamp(18px, 2.4vw, 28px);
    font-weight: 600;
    color: var(--text-primary);
    letter-spacing: 2px;
}

.brand-subtitle .teal {
    color: var(--brand-teal);
}

.brand-tagline {
    margin-top: 18px;
    display: inline-flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: var(--text-muted);
    letter-spacing: 2px;
    border: 1px solid var(--card-border);
    padding: 6px 16px;
    border-radius: 20px;
    background: white;
}

.brand-tagline::before {
    content: '';
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: var(--brand-teal);
    animation: dot-blink 2s ease-in-out infinite;
}

@keyframes dot-blink {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.2; }
}

/* ============================================
   Floating Particles (light)
   ============================================ */
.particle {
    position: absolute;
    border-radius: 50%;
    pointer-events: none;
    z-index: 1;
    opacity: 0;
}
.particle-1 { width: 3px; height: 3px; top: 15%; left: 20%; background: var(--brand-teal); animation: particle-float 8s ease-in-out 0s infinite; }
.particle-2 { width: 4px; height: 4px; top: 35%; left: 82%; background: var(--brand-tech); animation: particle-float 10s ease-in-out 1s infinite; }
.particle-3 { width: 3px; height: 3px; top: 60%; left: 12%; background: var(--brand-teal); animation: particle-float 7s ease-in-out 2s infinite; }
.particle-4 { width: 2px; height: 2px; top: 80%; left: 72%; background: var(--brand-tech); animation: particle-float 9s ease-in-out 3s infinite; }
.particle-5 { width: 3px; height: 3px; top: 22%; left: 60%; background: var(--brand-teal); animation: particle-float 11s ease-in-out 0.5s infinite; }
.particle-6 { width: 2px; height: 2px; top: 72%; left: 35%; background: var(--brand-tech); animation: particle-float 8s ease-in-out 1.5s infinite; }

@keyframes particle-float {
    0% { transform: translateY(0) translateX(0); opacity: 0; }
    10% { opacity: 0.25; }
    90% { opacity: 0.15; }
    100% { transform: translateY(-120px) translateX(40px); opacity: 0; }
}

/* ============================================
   Glass Card (Right) — Light
   ============================================ */
.form-wrapper {
    position: relative;
    z-index: 2;
    width: 100%;
    max-width: 420px;
    padding: 0 24px;
}

.form-card {
    background: var(--card-bg);
    border: 1px solid var(--card-border);
    border-radius: var(--radius-card);
    padding: 44px 40px 40px;
    box-shadow: var(--card-shadow);
    transition: box-shadow 0.3s, border-color 0.3s;
}

.form-card:hover {
    border-color: #dce3ec;
    box-shadow: var(--card-shadow-hover);
}

.form-header {
    margin-bottom: 36px;
}

.form-header h1 {
    font-size: 26px;
    font-weight: 600;
    margin-bottom: 8px;
    letter-spacing: 0.5px;
    color: var(--text-primary);
}

.form-header p {
    font-size: 14px;
    color: var(--text-secondary);
    font-weight: 400;
}

/* ============================================
   Input Fields — Light
   ============================================ */
.input-group {
    margin-bottom: 20px;
    position: relative;
}

.input-group label {
    display: block;
    font-size: 13px;
    font-weight: 500;
    color: var(--text-secondary);
    margin-bottom: 8px;
    letter-spacing: 0.3px;
}

.input-field {
    position: relative;
}

.input-field input {
    width: 100%;
    padding: 14px 16px 14px 44px;
    background: var(--input-bg);
    border: 1.5px solid var(--input-border);
    border-radius: var(--radius-input);
    color: var(--text-primary);
    font-size: 15px;
    font-family: var(--font-sans);
    outline: none;
    transition: all 0.25s ease;
    box-sizing: border-box;
}

.input-field input::placeholder {
    color: var(--text-muted);
    font-size: 14px;
}

.input-field input:hover {
    border-color: var(--input-border-hover);
}

.input-field input:focus {
    border-color: var(--brand-teal);
    background: white;
    box-shadow: 0 0 0 4px var(--input-focus-ring);
}

.input-icon {
    position: absolute;
    left: 16px;
    top: 50%;
    transform: translateY(-50%);
    width: 18px;
    height: 18px;
    color: var(--text-muted);
    pointer-events: none;
    transition: color 0.25s;
}

.input-field:focus-within .input-icon {
    color: var(--brand-teal);
}

.password-toggle {
    position: absolute;
    right: 14px;
    top: 50%;
    transform: translateY(-50%);
    background: none;
    border: none;
    color: var(--text-muted);
    cursor: pointer;
    padding: 4px;
    transition: color 0.2s;
    font-size: 16px;
    line-height: 1;
    display: flex;
}

.password-toggle:hover {
    color: var(--text-secondary);
}

/* ============================================
   Options row
   ============================================ */
.form-options {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin: 20px 0 28px;
}

.remember-me {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    font-size: 13px;
    color: var(--text-secondary);
    user-select: none;
}

.remember-me input[type="checkbox"] {
    display: none;
}

.checkbox-custom {
    width: 18px;
    height: 18px;
    border: 1.5px solid var(--input-border);
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;
    flex-shrink: 0;
}

.remember-me input:checked + .checkbox-custom {
    background: var(--brand-teal);
    border-color: var(--brand-teal);
}

.remember-me input:checked + .checkbox-custom::after {
    content: '';
    width: 5px;
    height: 9px;
    border: solid white;
    border-width: 0 2px 2px 0;
    transform: rotate(45deg);
    margin-top: -1px;
}

.remember-me:hover .checkbox-custom {
    border-color: var(--input-border-hover);
}

.forgot-link {
    font-size: 13px;
    color: var(--text-muted);
    text-decoration: none;
    transition: color 0.2s;
}

.forgot-link:hover {
    color: var(--brand-teal);
}

/* ============================================
   Login Button
   ============================================ */
.btn-login {
    width: 100%;
    padding: 14px;
    border: none;
    border-radius: var(--radius-button);
    background: linear-gradient(135deg, var(--brand-teal), #0f766e);
    color: white;
    font-size: 16px;
    font-weight: 600;
    font-family: var(--font-sans);
    cursor: pointer;
    transition: all 0.3s ease;
    position: relative;
    overflow: hidden;
    letter-spacing: 1px;
}

.btn-login:hover {
    transform: translateY(-1px);
    box-shadow: 0 8px 28px rgba(13, 148, 136, 0.25);
}

.btn-login:active {
    transform: translateY(0);
    box-shadow: none;
}

.btn-login::after {
    content: '';
    position: absolute;
    top: -50%;
    left: -60%;
    width: 40%;
    height: 200%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.12), transparent);
    transform: rotate(25deg);
    animation: btn-shimmer 4s ease-in-out infinite;
}

@keyframes btn-shimmer {
    0% { left: -60%; }
    100% { left: 120%; }
}

/* ============================================
   Tweaks Panel
   ============================================ */
.tweaks-toggle {
    position: fixed;
    bottom: 24px;
    right: 24px;
    z-index: 100;
    width: 40px;
    height: 40px;
    border-radius: 50%;
    border: 1px solid var(--card-border);
    background: var(--card-bg);
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
    color: var(--text-muted);
    cursor: pointer;
    font-size: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.3s;
}

.tweaks-toggle:hover {
    border-color: var(--brand-teal);
    color: var(--brand-teal);
    box-shadow: 0 4px 16px rgba(13, 148, 136, 0.15);
}

.tweaks-panel {
    position: fixed;
    bottom: 72px;
    right: 24px;
    z-index: 99;
    background: white;
    border: 1px solid var(--card-border);
    border-radius: 16px;
    padding: 20px 22px;
    min-width: 200px;
    transform: translateY(12px);
    opacity: 0;
    pointer-events: none;
    transition: all 0.3s ease;
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
}

.tweaks-panel.open {
    transform: translateY(0);
    opacity: 1;
    pointer-events: auto;
}

.tweaks-panel h3 {
    font-size: 12px;
    font-weight: 600;
    color: var(--text-muted);
    letter-spacing: 2px;
    margin-bottom: 16px;
    text-transform: uppercase;
}

.tweak-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 14px;
}

.tweak-item:last-child {
    margin-bottom: 0;
}

.tweak-label {
    font-size: 13px;
    color: var(--text-secondary);
}

/* Toggle switch — light */
.toggle-switch {
    position: relative;
    width: 36px;
    height: 20px;
    cursor: pointer;
}

.toggle-switch input {
    display: none;
}

.toggle-track {
    position: absolute;
    inset: 0;
    border-radius: 10px;
    background: #e2e8f0;
    transition: background 0.3s;
}

.toggle-track::after {
    content: '';
    position: absolute;
    top: 2px;
    left: 2px;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background: white;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    transition: all 0.3s;
}

.toggle-switch input:checked + .toggle-track {
    background: var(--brand-teal);
}

.toggle-switch input:checked + .toggle-track::after {
    left: 18px;
    background: white;
}

/* ============================================
   Entry Animations
   ============================================ */
@keyframes fade-in-up {
    from { opacity: 0; transform: translateY(20px); }
    to { opacity: 1; transform: translateY(0); }
}

.form-card {
    animation: fade-in-up 0.6s ease-out;
}

.brand-content {
    animation: fade-in-up 0.6s ease-out 0.1s both;
}

.scanner {
    animation: fade-in-up 0.6s ease-out 0.05s both;
}

/* ============================================
   Responsive
   ============================================ */
@media (max-width: 900px) {
    .container {
        flex-direction: column;
    }
    .panel-brand {
        flex: 0 0 auto;
        height: 45vh;
        min-height: 340px;
        padding: 24px;
    }
    .panel-form {
        flex: 1;
        min-height: 400px;
    }
    .scanner {
        width: 220px;
        height: 220px;
    }
    .scanner-tick { transform-origin: 0 110px; }
    .brand-title { font-size: clamp(16px, 3.2vw, 24px); }
    .brand-subtitle { font-size: clamp(14px, 2.8vw, 22px); }
    .form-card {
        padding: 32px 24px 28px;
    }
    .tweaks-panel {
        right: 12px;
        bottom: 64px;
    }
    .tweaks-toggle {
        right: 12px;
        bottom: 16px;
    }
}

@media (max-width: 480px) {
    .panel-brand {
        height: 36vh;
        min-height: 260px;
    }
    .scanner {
        width: 160px;
        height: 160px;
    }
    .scanner-tick {
        transform-origin: 0 80px;
        height: 7px;
    }
    .brand-content { display: none; }
    .form-wrapper {
        padding: 0 16px;
    }
    .form-card {
        padding: 28px 20px 24px;
        border-radius: 20px;
    }
    .tweaks-panel {
        right: 8px;
        bottom: 60px;
        min-width: 180px;
    }
    .tweaks-toggle {
        right: 8px;
        bottom: 12px;
    }
}
</style>

<template>
  <div class="login-wrap">
    <!-- 视口四角 HUD -->
    <span class="hud tl"></span><span class="hud tr"></span>
    <span class="hud bl"></span><span class="hud br"></span>
    <span class="hud-vert">POLYAGENT // RETINAL-04 // 0x2F9A</span>

    <div class="login-left">
      <div class="ll-meta">
        <span>SYS.REF <b>0x2F/9A</b></span>
        <span>LAYER <b>RETINAL-04</b></span>
        <span>NODE <b>SH-04</b></span>
        <span>BUILD <b>2026.10</b></span>
      </div>

      <div class="ll-center">
        <div class="kicker">Multi-Agent Training Interface</div>
        <h1 class="brand-name">
          <span class="gl" data-t="POLYAGENT">POLYAGENT</span>
          <span class="outline">TRAINING SYS</span>
          <em>基于多智能体的教学培训系统</em>
        </h1>
        <p class="brand-desc">
          七个教学智能体在统一编排层下协同运转 ——
          <b>画像 / 资源 / 路径 / 答疑 / 课件 / 评估</b>
          以一条任务链贯穿培训全流程。本界面为受训者视网膜投影层，数据实时叠加于真实视野之上。
        </p>
        <div class="stats">
          <div class="st"><span class="n">07</span><span class="k">AGENTS</span></div>
          <div class="st"><span class="n">1,284</span><span class="k">TRAINEES</span></div>
          <div class="st"><span class="n">3,412</span><span class="k">KB NODES</span></div>
          <div class="st"><span class="n">96.4<small>%</small></span><span class="k">ROUTE ACC.</span></div>
        </div>
      </div>

      <div class="ll-foot">
        <span>POLYAGENT / EDUCATION DIV.</span>
        <span class="ascii">▓▒░ 0x2F ░▒▓</span>
        <span>ENCRYPTED · AES-256</span>
      </div>

      <pre class="ascii-deco">        ┌──────┐
   ┌────┤ 0x2F ├────┐
   │    └──────┘    │
 ┌─┴─┐            ┌─┴─┐
 │ A │  ◇  ◇  ◇   │ B │
 └─┬─┘            └─┬─┘
   └───────┬────────┘
        ┌──┴──┐</pre>
    </div>

    <div class="login-right">
      <div class="retina">
        <span class="ring"><i></i></span>
        RETINAL LINK READY
      </div>

      <div class="form-card">
        <div class="fc-hd">
          <span class="t">Operator Auth</span>
          <span class="s">STEP 01 / 02</span>
        </div>

        <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
          <div class="fld-label">操作员编号 / OPERATOR ID</div>
          <el-form-item prop="userName">
            <el-input v-model="loginForm.userName" placeholder="USERNAME" prefix-icon="el-icon-user" tabindex="1" />
          </el-form-item>

          <div class="fld-label">访问密钥 / ACCESS KEY</div>
          <el-form-item prop="password">
            <el-input :type="passwordType" v-model="loginForm.password" placeholder="••••••••" prefix-icon="el-icon-lock" tabindex="2" @keyup.enter.native="handleLogin">
              <i slot="suffix" :class="passwordType === 'password' ? 'el-icon-view' : 'el-icon-loading'" style="cursor:pointer;line-height:38px;color:#525850" @click="showPwd"></i>
            </el-input>
          </el-form-item>

          <el-button :loading="loading" class="login-btn" @click.prevent="handleLogin">
            {{ loading ? 'LINKING…' : '建立神经链接 →' }}
          </el-button>

          <div class="login-extra">
            <router-link to="/register" class="register-link">还没有账号？注册</router-link>
          </div>
        </el-form>

        <div class="fc-foot">
          <span>© 2026 POLYAGENT</span>
          <span>SESSION 0x0412</span>
        </div>
      </div>

      <div class="preflight">
        <div class="pf-hd"><span>// PREFLIGHT CHECK</span><span>0x2F9A</span></div>
        <pre>mesh ....... <b>7/7 ONLINE</b>
kb ......... <b>3,412 DOCS</b>
route ...... <b>96.4%</b>
latency .... 22ms</pre>
      </div>
    </div>
  </div>
</template>

<script>
import { mapMutations } from 'vuex'
import loginApi from '@/api/login'

export default {
  name: 'Login',
  data () {
    return {
      loginForm: { userName: '', password: '', remember: false },
      loginRules: {
        userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      passwordType: 'password',
      capsTooltip: false,
      loading: false
    }
  },
  mounted () { if (!this.loginForm.userName) this.$refs.userName && this.$refs.userName.focus() },
  beforeDestroy () {},
  methods: {
    showPwd () { this.passwordType = this.passwordType === 'password' ? 'text' : 'password' },
    handleLogin () {
      this.$refs.loginForm.validate(valid => {
        if (!valid) return
        this.loading = true
        loginApi.login(this.loginForm).then(result => {
          if (result && result.code === 1) {
            this.setUserName(this.loginForm.userName)
            // 保存用户信息（含头像 imagePath）
            if (result.response.user) {
              this.setUserInfo(result.response.user)
            }
            const token = result.response.token
            if (token) localStorage.setItem('token', token)
            this.$router.push({ path: '/dashboard' })
          } else {
            this.loading = false
            this.$message({ message: result.message || '登录失败', type: 'error' })
          }
        }).catch(() => { this.loading = false })
      })
    },
    ...mapMutations('user', ['setUserName', 'setUserInfo'])
  }
}
</script>

<style scoped>
.login-wrap {
  position: relative;
  display: flex;
  min-height: 100vh;
  width: 100%;
  background: transparent;
  color: var(--paper);
  font-family: var(--f-sans);
  overflow: hidden;
}

/* ═══ HUD 装饰 ═══ */
.hud {
  position: fixed;
  width: 13px;
  height: 13px;
  border: 1px solid rgba(var(--acid-rgb), 0.4);
  z-index: 3;
}

.hud.tl { top: 10px; left: 10px; border-right: 0; border-bottom: 0; }
.hud.tr { top: 10px; right: 10px; border-left: 0; border-bottom: 0; }
.hud.bl { bottom: 10px; left: 10px; border-right: 0; border-top: 0; }
.hud.br { bottom: 10px; right: 10px; border-left: 0; border-top: 0; }

.hud-vert {
  position: fixed;
  right: 8px;
  top: 50%;
  transform: rotate(90deg) translateX(50%);
  transform-origin: right center;
  font-family: var(--f-mono);
  font-size: 6.5px;
  letter-spacing: 0.34em;
  color: rgba(var(--acid-rgb), 0.22);
  white-space: nowrap;
  z-index: 3;
}

/* ═══ 左栏 ═══ */
.login-left {
  flex: 1.32;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 32px 56px 30px;
  border-right: 1px solid var(--line);
  background:
    radial-gradient(circle at 22% 30%, rgba(var(--acid-rgb), 0.05), transparent 55%),
    linear-gradient(180deg, rgba(var(--acid-rgb), 0.02), transparent 40%);
  overflow: hidden;
}

.ll-meta {
  display: flex;
  gap: 22px;
  flex-wrap: wrap;
  font-family: var(--f-mono);
  font-size: 8px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--grey-3);
}

.ll-meta b {
  color: rgba(var(--acid-rgb), 0.55);
  font-weight: 400;
}

.ll-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 20px 0;
}

.kicker {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  font-family: var(--f-mono);
  font-size: 9px;
  letter-spacing: 0.32em;
  text-transform: uppercase;
  color: var(--acid);
}

.kicker::before {
  content: "";
  width: 38px;
  height: 1px;
  background: var(--ink-3);
}

.brand-name {
  margin: 0;
  font-family: var(--f-cond);
  font-size: clamp(46px, 7.4vw, 104px);
  font-weight: 700;
  line-height: 0.8;
  letter-spacing: -0.035em;
  text-transform: uppercase;
  color: var(--paper);
}

.brand-name .gl {
  display: block;
  position: relative;
}

.brand-name .gl::before,
.brand-name .gl::after {
  content: attr(data-t);
  position: absolute;
  left: 0;
  top: 0;
  opacity: 0;
  pointer-events: none;
}

.brand-name .gl::before {
  color: var(--acid);
  animation: glA 5.6s steps(1) infinite;
}

.brand-name .gl::after {
  color: #ff3b20;
  animation: glB 5.6s steps(1) infinite;
}

@keyframes glA {
  0%, 91%, 100% { opacity: 0; transform: none; clip-path: inset(0 0 60% 0); }
  92% { opacity: 0.9; transform: translate(-2px, -1px); clip-path: inset(0 0 58% 0); }
  95% { opacity: 0.6; transform: translate(2px, 1px); clip-path: inset(40% 0 22% 0); }
  97% { opacity: 0; }
}

@keyframes glB {
  0%, 92%, 100% { opacity: 0; transform: none; clip-path: inset(55% 0 8% 0); }
  93% { opacity: 0.8; transform: translate(2px, 1px); }
  96% { opacity: 0.5; transform: translate(-3px, 0); clip-path: inset(20% 0 40% 0); }
  98% { opacity: 0; }
}

.brand-name .outline {
  display: block;
  margin-top: 0.04em;
  color: transparent;
  -webkit-text-stroke: 1px rgba(var(--acid-rgb), 0.55);
}

.brand-name em {
  display: block;
  margin-top: 0.3em;
  font-family: var(--f-serif);
  font-style: italic;
  font-weight: 400;
  font-size: 0.16em;
  line-height: 1.4;
  letter-spacing: 0;
  text-transform: none;
  color: var(--acid);
}

.brand-desc {
  max-width: 46ch;
  margin: 26px 0 0;
  font-size: 12.5px;
  line-height: 1.9;
  color: var(--grey);
}

.brand-desc b {
  color: var(--paper);
  font-weight: 600;
}

.stats {
  display: flex;
  margin-top: 30px;
  border-top: 1px solid var(--line-soft);
  padding-top: 14px;
}

.st {
  padding-right: 26px;
  margin-right: 26px;
  border-right: 1px solid var(--line-soft);
}

.st:last-child {
  border-right: 0;
}

.st .n {
  display: block;
  font-family: var(--f-cond);
  font-size: 24px;
  line-height: 1;
  color: var(--acid);
}

.st .n small {
  font-size: 11px;
}

.st .k {
  display: block;
  margin-top: 5px;
  font-family: var(--f-mono);
  font-size: 7px;
  letter-spacing: 0.2em;
  color: var(--grey-3);
}

.ll-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-family: var(--f-mono);
  font-size: 7.5px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--grey-3);
}

.ll-foot .ascii {
  color: rgba(var(--acid-rgb), 0.3);
}

.ascii-deco {
  position: absolute;
  right: 18px;
  bottom: 46px;
  margin: 0;
  font-family: var(--f-mono);
  font-size: 8px;
  line-height: 1.15;
  color: rgba(var(--acid-rgb), 0.14);
  white-space: pre;
  pointer-events: none;
}

/* ═══ 右栏 ═══ */
.login-right {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 40px 48px;
  background: linear-gradient(180deg, rgba(var(--acid-rgb), 0.03), transparent 45%);
}

.retina {
  position: absolute;
  top: 26px;
  right: 34px;
  display: flex;
  align-items: center;
  gap: 9px;
  font-family: var(--f-mono);
  font-size: 7.5px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--grey-3);
}

.retina .ring {
  position: relative;
  width: 20px;
  height: 20px;
  display: block;
}

.retina .ring i {
  position: absolute;
  inset: 0;
  border: 1px solid rgba(var(--acid-rgb), 0.3);
  border-top-color: var(--ink-5);
  border-radius: 50%;
  animation: spin 3.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.form-card {
  width: 100%;
  max-width: 390px;
  padding: 0;
  background: transparent;
  border: 0;
  box-shadow: none;
}

.fc-hd {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding-bottom: 9px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 20px;
}

.fc-hd .t {
  font-family: var(--f-cond);
  font-size: 15px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--paper);
}

.fc-hd .s {
  font-family: var(--f-mono);
  font-size: 8px;
  letter-spacing: 0.2em;
  color: rgba(var(--acid-rgb), 0.55);
}

.fld-label {
  margin-bottom: 6px;
  font-family: var(--f-mono);
  font-size: 8px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--grey-3);
}

.login-form .el-form-item {
  margin-bottom: 16px;
}

.login-form ::v-deep .el-input__inner {
  height: 38px;
  line-height: 38px;
}

.login-form ::v-deep .el-form-item__error {
  padding-top: 2px;
}

.login-btn {
  width: 100%;
  height: 38px;
  margin-top: 4px;
  border: 1px solid var(--acid) !important;
  border-radius: 0 !important;
  background: rgba(var(--acid-rgb), 0.06) !important;
  color: var(--acid) !important;
  font-family: var(--f-mono) !important;
  font-size: 11px !important;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  transition: all 0.15s;
}

.login-btn:hover {
  background: var(--ink-3) !important;
  color: #080a00 !important;
  box-shadow: 0 0 24px rgba(var(--acid-rgb), 0.35);
}

.login-extra {
  margin-top: 16px;
  text-align: center;
}

.register-link {
  font-family: var(--f-mono);
  font-size: 9px;
  letter-spacing: 0.14em;
  color: var(--grey-2);
}

.register-link:hover {
  color: var(--acid);
  text-decoration: none;
}

.fc-foot {
  display: flex;
  justify-content: space-between;
  margin-top: 22px;
  padding-top: 10px;
  border-top: 1px solid var(--line-soft);
  font-family: var(--f-mono);
  font-size: 7.5px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #2e342e;
}

.preflight {
  width: 100%;
  max-width: 390px;
  border: 1px solid var(--line-soft);
  background: rgba(0, 0, 0, 0.4);
  padding: 9px 11px;
}

.pf-hd {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-family: var(--f-mono);
  font-size: 7px;
  letter-spacing: 0.24em;
  color: var(--grey-3);
}

.preflight pre {
  margin: 0;
  font-family: var(--f-mono);
  font-size: 8.5px;
  line-height: 1.75;
  color: var(--grey-2);
}

.preflight pre b {
  color: var(--acid);
  font-weight: 400;
}

@media (max-width: 1100px) {
  .login-wrap {
    flex-direction: column;
  }

  .login-left {
    flex: none;
    padding: 26px 26px 22px;
    border-right: 0;
    border-bottom: 1px solid var(--line);
  }

  .login-right {
    flex: 1;
    padding: 30px 26px;
  }

  .ascii-deco {
    display: none;
  }
}
</style>

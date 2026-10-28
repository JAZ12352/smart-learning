<template>
  <div class="register-wrap">
    <div class="register-card">
      <h2 class="title">注册</h2>
      <p class="subtitle">注册教师账号，开始智能教学</p>
      <el-form ref="regForm" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="userName">
          <el-input v-model="form.userName" placeholder="请输入用户名" prefix-icon="el-icon-user" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="el-icon-lock" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPwd">
          <el-input v-model="form.confirmPwd" type="password" placeholder="请再次输入密码" prefix-icon="el-icon-lock" show-password />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" prefix-icon="el-icon-user-solid" />
        </el-form-item>
        <el-button :loading="loading" class="reg-btn" @click.prevent="handleRegister">
          {{ loading ? '注册中...' : '注 册' }}
        </el-button>
        <div class="reg-extra">
          <router-link to="/login" class="login-link">已有账号？去登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script>
import { post } from '@/utils/request'

export default {
  name: 'TeacherRegister',
  data () {
    const validateConfirmPwd = (rule, value, callback) => {
      if (value !== this.form.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      form: {
        userName: '',
        password: '',
        confirmPwd: '',
        realName: ''
      },
      rules: {
        userName: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
        ],
        confirmPwd: [
          { required: true, message: '请再次输入密码', trigger: 'blur' },
          { validator: validateConfirmPwd, trigger: 'blur' }
        ],
        realName: [
          { required: true, message: '请输入真实姓名', trigger: 'blur' }
        ]
      },
      loading: false
    }
  },
  methods: {
    handleRegister () {
      this.$refs.regForm.validate(valid => {
        if (!valid) return
        this.loading = true
        const payload = {
          userName: this.form.userName,
          password: this.form.password,
          realName: this.form.realName,
          role: 2 // 教师
        }
        post('/api/user/register', payload).then(result => {
          this.loading = false
          if (result && result.code === 1) {
            this.$message({ message: '注册成功！请登录', type: 'success' })
            this.$router.push('/login')
          } else {
            this.$message({ message: result.message || '注册失败', type: 'error' })
          }
        }).catch(() => {
          this.loading = false
          this.$message({ message: '注册失败，请稍后重试', type: 'error' })
        })
      })
    }
  }
}
</script>

<style scoped>
.register-wrap {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: var(--ink-3);
}
.register-card {
  width: 440px; max-width: 90%;
  background: var(--ink-3); border: 1px solid #e8e8ed; border-radius: 14px;
  padding: 48px 40px; box-shadow: 0 4px 32px rgba(0,0,0,0.06);
}
.title { font-size: 24px; font-weight: 700; color: #1d1d1f; margin: 0 0 4px; text-align: center; }
.subtitle { font-size: 13px; color: #999; margin: 0 0 32px; text-align: center; }
.reg-btn {
  width: 100%; height: 48px; border: none; border-radius: 10px;
  background: var(--ink-4); color: #ffffff; font-size: 15px; font-weight: 600;
  letter-spacing: 4px; cursor: pointer; margin-top: 8px; transition: all 0.25s;
}
.reg-btn:hover { background: var(--ink-5); }
.reg-extra { text-align: center; margin-top: 20px; }
.login-link { color: #1e3a5f; font-size: 13px; cursor: pointer; }
.login-link:hover { text-decoration: underline; }
</style>

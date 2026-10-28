<template>
  <el-card style="margin-bottom:20px;">
    <div slot="header" class="clearfix">
      <span>关于我</span>
      <el-button type="primary" size="small" @click="toggleEdit" style="float:right;">
        {{ isEditing ? '取消' : '编辑' }}
      </el-button>
    </div>

    <div class="user-profile">
      <div class="box-center">
        <div class="avatar-container">
          <img :src="editForm.imagePath === null ? require('@/assets/avatar.gif') : editForm.imagePath " height="100px" width="100px" style="border-radius: 50px">
          <el-button v-if="isEditing" type="primary" size="small" class="avatar-upload-btn" @click="triggerFileInput">
            更换头像
          </el-button>
          <input type="file" ref="fileInput" style="display:none" accept="image/*" @change="handleAvatarUpload">
        </div>
      </div>
      <div class="box-center">
        <div class="user-name text-center" v-if="!isEditing">{{ safeUserInfo.userName }}</div>
        <el-input v-else v-model="editForm.userName" placeholder="用户名" class="user-name-input"></el-input>
        <div class="user-role text-center text-muted">{{ enumFormat(roleEnum,safeUserInfo.role) }}</div>
      </div>
    </div>

    <div class="user-bio">
      <!-- 个人基础信息 -->
      <div class="user-basic-info user-bio-section">
        <div class="user-bio-section-header"><svg-icon icon-class="user" /><span>个人基础信息</span></div>
        <div class="user-bio-section-body">
          <div v-if="!isEditing" class="text-muted">
            {{ safeUserInfo.realName || '无' }}
          </div>
          <el-input v-else v-model="editForm.realName" placeholder="真实姓名"></el-input>
        </div>
      </div>

      <!-- 教育背景 -->
      <div class="user-education user-bio-section">
        <div class="user-bio-section-header"><svg-icon icon-class="education" /><span>教育背景</span></div>
        <div class="user-bio-section-body">
          <div v-if="!isEditing" class="text-muted">
            {{ safeUserInfo.education || '无' }}
          </div>
          <el-input v-else v-model="editForm.education" placeholder="教育背景" type="textarea" :rows="2"></el-input>
        </div>
      </div>

      <!-- 工作经历 -->
      <div class="user-experience user-bio-section">
        <div class="user-bio-section-header"><svg-icon icon-class="position" /><span>工作经历</span></div>
        <div class="user-bio-section-body">
          <div v-if="!isEditing" class="text-muted">
            {{ safeUserInfo.experience || '无' }}
          </div>
          <el-input v-else v-model="editForm.experience" placeholder="工作经历" type="textarea" :rows="3"></el-input>
        </div>
      </div>

      <!-- 保存按钮 -->
      <div v-if="isEditing" class="user-bio-section">
        <el-button type="success" @click="saveProfile">保存</el-button>
      </div>
    </div>
  </el-card>
</template>

<script>
import { mapGetters, mapState } from 'vuex'
import request from '@/utils/request'

export default {
  props: {
    userInfo: {
      type: Object,
      default: () => {
        return {
          realName: '',
          userName: '',
          role: '',
          imagePath: null,
          education: '',
          experience: ''
        }
      }
    }
  },
  data () {
    return {
      isEditing: false,
      editForm: {
        realName: '',
        userName: '',
        imagePath: null,
        education: '',
        experience: ''
      }
    }
  },
  computed: {
    ...mapGetters('enumItem', [
      'enumFormat'
    ]),
    ...mapState('enumItem', {
      roleEnum: state => state.user.roleEnum
    }),
    safeUserInfo () {
      return this.userInfo || {
        realName: '',
        userName: '',
        role: '',
        imagePath: null,
        education: '',
        experience: ''
      }
    }
  },
  watch: {
    userInfo: {
      handler (newVal) {
        this.editForm = {
          realName: newVal.realName || '',
          userName: newVal.userName || '',
          imagePath: newVal.imagePath || null,
          education: newVal.education || '',
          experience: newVal.experience || ''
        }
      },
      immediate: true
    }
  },
  methods: {
    toggleEdit () {
      this.isEditing = !this.isEditing
      if (!this.isEditing) {
        // 取消编辑，恢复原值
        this.editForm = {
          realName: this.safeUserInfo.realName || '',
          userName: this.safeUserInfo.userName || '',
          imagePath: this.safeUserInfo.imagePath || null,
          education: this.safeUserInfo.education || '',
          experience: this.safeUserInfo.experience || ''
        }
      }
    },
    triggerFileInput () {
      this.$refs.fileInput.click()
    },
    handleAvatarUpload (event) {
      const file = event.target.files[0]
      if (file) {
        // 这里可以添加文件上传逻辑
        const reader = new FileReader()
        reader.onload = (e) => {
          this.editForm.imagePath = e.target.result
        }
        reader.readAsDataURL(file)
      }
    },
    saveProfile () {
      // 这里可以添加保存逻辑
      console.log('保存个人简介:', this.editForm)
      // 模拟保存成功
      this.$message.success('保存成功')
      this.isEditing = false
      // 可以通过事件通知父组件更新数据
      this.$emit('profile-updated', this.editForm)
    }
  }
}
</script>

<style scoped>
.box-center {
  margin: 0 auto;
  display: table;
}

.text-muted {
  color: #777;
}

.user-profile .user-name {
  font-weight: bold;
}

.user-profile .box-center {
  padding-top: 10px;
}

.user-profile .user-role {
  padding-top: 10px;
  font-weight: 400;
  font-size: 14px;
}

.user-profile .box-social {
  padding-top: 30px;
}

.user-profile .box-social .el-table {
  border-top: 1px solid #dfe6ec;
}

.user-profile .user-follow {
  padding-top: 20px;
}

.user-bio {
  margin-top: 20px;
  color: var(--grey);
}

.user-bio span {
  padding-left: 4px;
}

.user-bio .user-bio-section {
  font-size: 14px;
  padding: 15px 0;
}

.user-bio .user-bio-section .user-bio-section-header {
  border-bottom: 1px solid #dfe6ec;
  padding-bottom: 10px;
  margin-bottom: 10px;
  font-weight: bold;
}

.avatar-container {
  position: relative;
  display: inline-block;
}

.avatar-upload-btn {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100px;
  border-radius: 0 0 50px 50px;
  opacity: 0.8;
}

.user-name-input {
  width: 200px;
  margin-top: 10px;
}
</style>

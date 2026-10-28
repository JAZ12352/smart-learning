<template>
  <div class="app-container">
    <div>
      <el-row :gutter="20">

        <el-col :span="6" :xs="24">
          <user-card  :userInfo="userInfo" @profile-updated="handleProfileUpdated" />
        </el-col>

        <el-col :span="18" :xs="24">
          <el-card>
            <el-tabs active-name="timeline">
              <el-tab-pane label="时间线" name="timeline">
                <timeline :userInfo="userInfo" />
              </el-tab-pane>
              <el-tab-pane label="账号" name="account">
                <account :userInfo="userInfo"  />
              </el-tab-pane>
            </el-tabs>
          </el-card>
        </el-col>

      </el-row>
    </div>
  </div>
</template>

<script>
import UserCard from './components/UserCard'
import Timeline from './components/Timeline'
import Account from './components/Account'
import userApi from '@/api/user'

export default {
  name: 'Profile',
  data () {
    return {
      userInfo: {
        realName: '',
        phone: '',
        lastActiveTime: '',
        createTime: '',
        role: '1',
        imagePath: null
      }
    }
  },
  components: { UserCard, Timeline, Account },
  created () {
    let _this = this
    userApi.getCurrentUser().then(re => {
      if (re.response) {
        _this.userInfo = re.response
        // 同步到 Vuex store，确保导航栏头像一致
        _this.$store.commit('user/setUserInfo', re.response)
      }
    })
  },
  methods: {
    handleProfileUpdated (editForm) {
      // 合并更新后的字段到本地 userInfo
      this.userInfo = Object.assign({}, this.userInfo, editForm)
      // 同步到 Vuex store，导航栏头像立即更新
      this.$store.commit('user/setUserInfo', this.userInfo)
    }
  }
}
</script>

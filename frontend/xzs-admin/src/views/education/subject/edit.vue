<template>
  <div class="app-container">
    <el-card shadow="hover">
      <div slot="header">
        <span>{{ form.id ? '编辑学科' : '添加学科' }}</span>
      </div>

      <el-form :model="form" ref="form" label-width="120px" v-loading="formLoading" :rules="rules">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学科名称" prop="name">
              <el-input v-model="form.name" placeholder="如：深度学习实战、大模型应用开发"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学历层次" prop="educationLevel">
              <el-select v-model="form.educationLevel" placeholder="选择学历层次" @change="onEducationLevelChange" style="width:100%">
                <el-option v-for="item in educationLevelEnum" :key="item.key" :value="item.key" :label="item.value"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="年级/学年" prop="level">
              <el-select v-model="form.level" placeholder="选择学年" style="width:100%">
                <el-option v-for="i in levelMax" :key="i" :value="i" :label="getLevelLabel(form.educationLevel, i)"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学科门类">
              <el-select v-model="form.category" placeholder="选择门类" clearable style="width:100%">
                <el-option v-for="item in categoryEnum" :key="item.key" :label="item.value" :value="item.key"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="学科描述">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="简要描述该学科的教学内容、适用专业等（可选）"></el-input>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm">提交</el-button>
          <el-button @click="resetForm">重置</el-button>
          <el-button @click="goBack">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { mapGetters, mapState, mapActions } from 'vuex'
import subjectApi from '@/api/subject'

export default {
  data () {
    return {
      form: {
        id: null,
        name: '',
        educationLevel: 2,
        educationLevelName: '本科',
        level: 1,
        levelName: '第一年',
        category: '',
        description: ''
      },
      formLoading: false,
      rules: {
        name: [{ required: true, message: '请输入学科名称', trigger: 'blur' }],
        educationLevel: [{ required: true, message: '请选择学历层次', trigger: 'change' }],
        level: [{ required: true, message: '请选择学年', trigger: 'change' }]
      }
    }
  },
  computed: {
    ...mapGetters('enumItem', ['enumFormat']),
    ...mapState('enumItem', {
      educationLevelEnum: state => state.user.educationLevelEnum,
      levelEnum: state => state.user.levelEnum,
      categoryEnum: state => state.user.categoryEnum
    }),
    levelMax () {
      const map = { 1: 3, 2: 4, 3: 3, 4: 4 }
      return map[this.form.educationLevel] || 4
    }
  },
  created () {
    const id = this.$route.query.id
    if (id && parseInt(id) !== 0) {
      this.formLoading = true
      subjectApi.select(id).then(re => {
        if (re.code === 1) {
          this.form = re.response
        }
        this.formLoading = false
      }).catch(() => { this.formLoading = false })
    }
  },
  methods: {
    getLevelLabel (eduLevel, year) {
      return '第' + year + '年'
    },
    onEducationLevelChange (val) {
      this.form.level = 1
      this.form.levelName = this.getLevelLabel(val, 1)
      this.form.educationLevelName = this.enumFormat(this.educationLevelEnum, val)
    },
    submitForm () {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.formLoading = true
        this.form.levelName = this.getLevelLabel(this.form.educationLevel, this.form.level)
        this.form.educationLevelName = this.enumFormat(this.educationLevelEnum, this.form.educationLevel)
        subjectApi.edit(this.form).then(data => {
          if (data.code === 1) {
            this.$message.success(data.message)
            this.delCurrentView(this).then(() => {
              this.$router.push('/education/subject/list')
            })
          } else {
            this.$message.error(data.message)
            this.formLoading = false
          }
        }).catch(e => {
          this.formLoading = false
        })
      })
    },
    resetForm () {
      const lastId = this.form.id
      this.$refs['form'].resetFields()
      this.form = {
        id: null,
        name: '',
        educationLevel: 2,
        educationLevelName: '本科',
        level: 1,
        levelName: '第一年',
        category: '',
        description: ''
      }
      this.form.id = lastId
    },
    goBack () {
      this.$router.push('/education/subject/list')
    },
    ...mapActions('tagsView', { delCurrentView: 'delCurrentView' })
  }
}
</script>

<style scoped>
.app-container { padding: 24px; background-color: var(--ink-3); min-height: calc(100vh - 64px); }
</style>

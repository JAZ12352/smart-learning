<template>
  <div class="student-subject-cascade">
    <el-row :gutter="12" type="flex" align="middle">
      <el-col :span="studentSpan">
        <el-form-item :label="studentLabel" :required="studentRequired" :prop="studentProp">
          <el-select
            v-model="localStudentId"
            :placeholder="studentPlaceholder"
            clearable filterable
            @change="onStudentChange"
            style="width:100%"
          >
            <el-option
              v-for="s in filteredStudents"
              :key="s.id"
              :value="s.id"
              :label="s.realName + ' (' + s.userName + ')' + (s.majorName ? ' - ' + s.majorName : '')"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="subjectSpan">
        <el-form-item :label="subjectLabel" :required="subjectRequired" :prop="subjectProp">
          <el-cascader
            v-if="cascadeMode"
            v-model="localSubjectPath"
            :options="filteredSubjects"
            :props="{ label: 'name', value: 'id', children: 'children', checkStrictly: true, expandTrigger: 'hover' }"
            :placeholder="subjectPlaceholder"
            clearable
            style="width:100%"
          />
          <el-select
            v-else
            v-model="localSubjectId"
            :placeholder="subjectPlaceholder"
            clearable filterable
            @change="onSubjectChange"
            style="width:100%"
          >
            <el-option
              v-for="s in flatSubjects"
              :key="s.id"
              :value="s.id"
              :label="s.name"
            />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import subjectApi from '@/api/subject'
import userApi from '@/api/user'

export default {
  name: 'StudentSubjectCascade',
  props: {
    // 学生相关
    studentLabel: { type: String, default: '学生' },
    studentPlaceholder: { type: String, default: '选择学生' },
    studentRequired: { type: Boolean, default: false },
    studentProp: { type: String, default: '' },
    studentValue: { type: Number, default: null },

    // 学科相关
    subjectLabel: { type: String, default: '学科' },
    subjectPlaceholder: { type: String, default: '选择学科' },
    subjectRequired: { type: Boolean, default: false },
    subjectProp: { type: String, default: '' },
    subjectValue: { type: Number, default: null },

    // 模式
    cascadeMode: { type: Boolean, default: true },
    showStudent: { type: Boolean, default: true },
    showSubject: { type: Boolean, default: true },
    studentSpan: { type: Number, default: 8 },
    subjectSpan: { type: Number, default: 8 },

    // 学历层次筛选
    educationLevel: { type: Number, default: null }
  },
  data () {
    return {
      allStudents: [],
      subjectTree: [],
      localStudentId: this.studentValue,
      localSubjectId: this.subjectValue,
      localSubjectPath: []
    }
  },
  computed: {
    filteredStudents () {
      if (!this.localSubjectId && !this.selectedMajorId) return this.allStudents
      const majorId = this.selectedMajorId || this.localSubjectId
      if (!majorId) return this.allStudents
      return this.allStudents.filter(s => s.majorId === majorId)
    },
    filteredSubjects () {
      if (!this.localStudentId) return this.subjectTree
      const student = this.allStudents.find(s => s.id === this.localStudentId)
      if (!student || !student.majorId) return this.subjectTree
      // 找到学生专业所在的父级，只显示该门类
      for (const cat of this.subjectTree) {
        // 检查这个专业是否属于该门类
        const found = this.findMajorInTree(cat, student.majorId)
        if (found) return [cat]
      }
      return this.subjectTree
    },
    flatSubjects () {
      if (!this.filteredSubjects || this.filteredSubjects.length === 0) return []
      const result = []
      const flatten = (nodes) => {
        for (const n of nodes) {
          if (n.children && n.children.length > 0) {
            flatten(n.children)
          }
          result.push(n)
        }
      }
      flatten(this.filteredSubjects)
      return result
    },
    selectedMajorId () {
      if (this.localSubjectPath && this.localSubjectPath.length > 0) {
        return this.localSubjectPath[this.localSubjectPath.length - 1]
      }
      return null
    }
  },
  watch: {
    studentValue (val) { this.localStudentId = val },
    subjectValue (val) { this.localSubjectId = val }
  },
  created () {
    this.loadAll()
  },
  methods: {
    async loadAll () {
      try {
        const [stuRes, subRes] = await Promise.all([
          userApi.allStudents(),
          subjectApi.getTree(this.educationLevel)
        ])
        if (stuRes.code === 1) this.allStudents = stuRes.response || []
        if (subRes.code === 1) this.subjectTree = subRes.response || []
      } catch (e) {}
    },
    findMajorInTree (node, majorId) {
      if (node.id === majorId) return true
      if (node.children) {
        for (const child of node.children) {
          if (this.findMajorInTree(child, majorId)) return true
        }
      }
      return false
    },
    onStudentChange (id) {
      this.localStudentId = id
      this.localSubjectId = null
      this.localSubjectPath = []
      this.$emit('update:studentValue', id)
      const student = this.allStudents.find(s => s.id === id)
      this.$emit('student-change', student || null)
      this.$emit('change', { studentId: id, subjectId: null })
    },
    onSubjectChange (id) {
      this.localSubjectId = id
      this.$emit('update:subjectValue', id)
      this.$emit('subject-change', id)
      this.$emit('change', { studentId: this.localStudentId, subjectId: id })
    },
    reset () {
      this.localStudentId = null
      this.localSubjectId = null
      this.localSubjectPath = []
    }
  }
}
</script>

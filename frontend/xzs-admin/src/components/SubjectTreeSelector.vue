<template>
  <div class="subject-selector">
    <el-row :gutter="12">
      <el-col :span="8">
        <el-select v-model="selectedCategory" placeholder="学科门类" clearable @change="onCategoryChange" style="width:100%">
          <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id">
            <span><i :class="catIcon(item.name)"></i> {{ item.name }}</span>
          </el-option>
        </el-select>
      </el-col>
      <el-col :span="8">
        <el-select v-model="selectedDiscipline" placeholder="专业类" :disabled="!selectedCategory" clearable @change="onDisciplineChange" style="width:100%">
          <el-option v-for="item in disciplines" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-col>
      <el-col :span="8">
        <el-select v-model="selectedMajor" placeholder="专业" :disabled="!selectedDiscipline" clearable @change="onMajorChange" style="width:100%">
          <el-option v-for="item in majors" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import subjectApi from '@/api/subject'

export default {
  name: 'SubjectTreeSelector',
  props: {
    value: { type: Object, default: () => ({}) },
    educationLevel: { type: Number, default: 2 }
  },
  data () {
    return {
      categories: [],
      disciplines: [],
      majors: [],
      selectedCategory: null,
      selectedDiscipline: null,
      selectedMajor: null
    }
  },
  watch: {
    educationLevel () { this.loadCategories() }
  },
  created () { this.loadCategories() },
  methods: {
    catIcon (name) {
      const map = { '工学': 'el-icon-setting', '理学': 'el-icon-data-line', '管理学': 'el-icon-s-management', '文学': 'el-icon-edit', '医学': 'el-icon-first-aid-kit', '法学': 'el-icon-s-check', '经济学': 'el-icon-coin', '教育学': 'el-icon-reading' }
      return map[name] || 'el-icon-collection-tag'
    },
    async loadCategories () {
      const res = await subjectApi.getByType(1)
      if (res.code === 1) this.categories = res.response || []
    },
    async onCategoryChange (id) {
      this.selectedCategory = id
      this.selectedDiscipline = null
      this.selectedMajor = null
      this.disciplines = []
      this.majors = []
      if (id) {
        const res = await subjectApi.getByParent(id)
        if (res.code === 1) this.disciplines = res.response || []
      }
      this.emitChange()
    },
    async onDisciplineChange (id) {
      this.selectedDiscipline = id
      this.selectedMajor = null
      this.majors = []
      if (id) {
        const res = await subjectApi.getByParent(id)
        if (res.code === 1) this.majors = res.response || []
      }
      this.emitChange()
    },
    onMajorChange (id) {
      this.selectedMajor = id
      this.emitChange()
    },
    emitChange () {
      this.$emit('input', {
        categoryId: this.selectedCategory,
        categoryName: this.catName(this.selectedCategory),
        disciplineId: this.selectedDiscipline,
        disciplineName: this.discName(this.selectedDiscipline),
        majorId: this.selectedMajor,
        majorName: this.majorName(this.selectedMajor)
      })
      this.$emit('change', this.selectedMajor || this.selectedDiscipline || this.selectedCategory)
    },
    catName (id) { const c = this.categories.find(x => x.id === id); return c ? c.name : '' },
    discName (id) { const c = this.disciplines.find(x => x.id === id); return c ? c.name : '' },
    majorName (id) { const c = this.majors.find(x => x.id === id); return c ? c.name : '' },
    reset () {
      this.selectedCategory = null
      this.selectedDiscipline = null
      this.selectedMajor = null
      this.disciplines = []
      this.majors = []
    },
    // 根据专业名称自动选择三级联动
    async autoSelectByMajorName (majorName) {
      if (!majorName) return
      // 确保学科门类已加载
      if (this.categories.length === 0) {
        const res = await subjectApi.getByType(1)
        if (res.code === 1) this.categories = res.response || []
      }
      // 遍历学科门类 → 专业类 → 专业，查找匹配
      for (var i = 0; i < this.categories.length; i++) {
        var cat = this.categories[i]
        var dRes = await subjectApi.getByParent(cat.id)
        var discList = (dRes && dRes.code === 1) ? dRes.response : []
        for (var j = 0; j < discList.length; j++) {
          var disc = discList[j]
          var mRes = await subjectApi.getByParent(disc.id)
          var majList = (mRes && mRes.code === 1) ? mRes.response : []
          var match = majList.find(function (m) { return m.name === majorName })
          if (match) {
            this.selectedCategory = cat.id
            this.disciplines = discList
            this.selectedDiscipline = disc.id
            this.majors = majList
            this.selectedMajor = match.id
            this.emitChange()
            return true
          }
        }
      }
      return false
    }
  }
}
</script>

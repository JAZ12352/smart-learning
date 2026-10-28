// initial state
const state = {
  user: {
    sexEnum: [{ key: 1, value: '男' }, { key: 2, value: '女' }],
    statusEnum: [{ key: 1, value: '启用' }, { key: 2, value: '禁用' }],
    educationLevelEnum: [
      { key: 1, value: '大专' },
      { key: 2, value: '本科' },
      { key: 3, value: '硕士' },
      { key: 4, value: '博士' }
    ],
    levelEnum: [
      { key: 1, value: '第一年' }, { key: 2, value: '第二年' }, { key: 3, value: '第三年' }, { key: 4, value: '第四年' }
    ],
    categoryEnum: [
      { key: '哲学', value: '哲学' },
      { key: '经济学', value: '经济学' },
      { key: '法学', value: '法学' },
      { key: '教育学', value: '教育学' },
      { key: '文学', value: '文学' },
      { key: '历史学', value: '历史学' },
      { key: '理学', value: '理学' },
      { key: '工学', value: '工学' },
      { key: '农学', value: '农学' },
      { key: '医学', value: '医学' },
      { key: '管理学', value: '管理学' },
      { key: '艺术学', value: '艺术学' },
      { key: '军事学', value: '军事学' },
      { key: '交叉学科', value: '交叉学科' }
    ],
    subjectTypeEnum: [
      { key: 1, value: '学科门类' },
      { key: 2, value: '专业类' },
      { key: 3, value: '专业' },
      { key: 4, value: '课程' }
    ],
    roleEnum: [{ key: 1, value: '学生' }, { key: 2, value: '教师' }, { key: 3, value: '管理员' }],
    statusTag: [{ key: 1, value: 'success' }, { key: 2, value: 'danger' }],
    statusBtn: [{ key: 1, value: '禁用' }, { key: 2, value: '启用' }]
  },
  exam: {
    examPaper: {
      paperTypeEnum: [{ key: 1, value: '固定试卷' }, { key: 4, value: '时段试卷' }, { key: 6, value: '任务试卷' }]
    },
    question: {
      typeEnum: [{ key: 1, value: '单选题' }, { key: 2, value: '多选题' }, { key: 3, value: '判断题' }, { key: 4, value: '填空题' }, { key: 5, value: '简答题' }],
      editUrlEnum: [{ key: 1, value: '/exam/question/edit/singleChoice', name: '单选题' },
        { key: 2, value: '/exam/question/edit/multipleChoice', name: '多选题' },
        { key: 3, value: '/exam/question/edit/trueFalse', name: '判断题' },
        { key: 4, value: '/exam/question/edit/gapFilling', name: '填空题' },
        { key: 5, value: '/exam/question/edit/shortAnswer', name: '简答题' }]
    }
  }
}

// getters
const getters = {
  enumFormat: (state) => (arrary, key) => {
    return format(arrary, key)
  }
}

// actions
const actions = {}

// mutations
const mutations = {}

const format = function (array, key) {
  for (let item of array) {
    if (item.key === key) {
      return item.value
    }
  }
  return null
}

export default {
  namespaced: true,
  state,
  getters,
  actions,
  mutations
}

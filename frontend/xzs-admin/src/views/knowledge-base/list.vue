<template>
  <div class="app-container">
    <el-card shadow="hover" class="main-card">
      <div slot="header" class="card-header">
        <i class="el-icon-collection header-icon"></i>
        <span class="header-title">本地知识库管理</span>
      </div>

      <div class="knowledge-base-container">
        <!-- 左侧：知识库列表 -->
        <div class="kb-sidebar">
          <div class="kb-actions">
            <el-button type="primary" @click="showCreateDialog" size="small">
              <i class="el-icon-plus"></i> 新建知识库
            </el-button>
            <el-button type="success" @click="showImportDialog" size="small">
              <i class="el-icon-upload"></i> 导入知识库
            </el-button>
          </div>

          <el-input
            v-model="searchQuery"
            placeholder="搜索知识库..."
            prefix-icon="el-icon-search"
            size="small"
            class="kb-search"
          />

          <div class="kb-list">
            <div
              v-for="kb in filteredKnowledgeBases"
              :key="kb.id"
              :class="['kb-item', { active: selectedKB && selectedKB.id === kb.id }]"
              @click="selectKnowledgeBase(kb)"
            >
              <div class="kb-item-header">
                <i class="el-icon-folder-opened kb-icon"></i>
                <span class="kb-name">{{ kb.name }}</span>
              </div>
              <div class="kb-item-meta">
                <span class="kb-count">{{ kb.documentCount }} 个文档</span>
                <span class="kb-date">{{ kb.updateTime }}</span>
              </div>
              <div class="kb-item-desc">{{ kb.description }}</div>
            </div>
          </div>
        </div>

        <!-- 右侧：知识库详情 -->
        <div class="kb-content" v-if="selectedKB">
          <div class="kb-header">
            <div class="kb-title-section">
              <h2 class="kb-title">{{ selectedKB.name }}</h2>
              <p class="kb-desc">{{ selectedKB.description }}</p>
            </div>
            <div class="kb-header-actions">
              <el-button type="primary" @click="showUploadDialog">
                <i class="el-icon-upload"></i> 上传文档
              </el-button>
              <el-button type="success" @click="showRAGDialog">
                <i class="el-icon-chat-dot-round"></i> RAG问答
              </el-button>
              <el-button type="danger" @click="deleteKnowledgeBase">
                <i class="el-icon-delete"></i> 删除
              </el-button>
            </div>
          </div>

          <!-- 文档列表 -->
          <div class="documents-section">
            <h3 class="section-title">
              <i class="el-icon-document"></i> 文档列表
            </h3>
            <el-table :data="documents" style="width: 100%" v-loading="loading">
              <el-table-column prop="name" label="文档名称" min-width="200">
                <template slot-scope="scope">
                  <i :class="getFileIcon(scope.row.type)"></i>
                  {{ scope.row.name }}
                </template>
              </el-table-column>
              <el-table-column prop="size" label="大小" width="120">
                <template slot-scope="scope">
                  {{ formatFileSize(scope.row.size) }}
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="120">
                <template slot-scope="scope">
                  <el-tag :type="getStatusType(scope.row.status)">
                    {{ getStatusText(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="uploadTime" label="上传时间" width="180" />
              <el-table-column label="操作" width="200" fixed="right">
                <template slot-scope="scope">
                  <el-button type="text" @click="viewDocument(scope.row)">查看</el-button>
                  <el-button type="text" @click="reprocessDocument(scope.row)">重新处理</el-button>
                  <el-button type="text" style="color: #f56c6c" @click="deleteDocument(scope.row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 向量检索测试 -->
          <div class="search-section">
            <h3 class="section-title">
              <i class="el-icon-search"></i> 向量检索测试
            </h3>
            <div class="search-box">
              <el-input
                v-model="searchTestQuery"
                placeholder="输入查询内容，测试知识库检索效果..."
                class="search-input"
              >
                <el-button slot="append" type="primary" @click="testSearch" :loading="searchLoading">
                  <i class="el-icon-search"></i> 检索
                </el-button>
              </el-input>
            </div>
            <div class="search-results" v-if="searchResults.length > 0">
              <div v-for="(result, index) in searchResults" :key="index" class="search-result-item">
                <div class="result-header">
                  <span class="result-rank">#{{ index + 1 }}</span>
                  <span class="result-score">相似度: {{ (result.score * 100).toFixed(2) }}%</span>
                  <span class="result-source">来源: {{ result.source }}</span>
                </div>
                <div class="result-content">{{ result.content }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div class="kb-empty" v-else>
          <i class="el-icon-collection empty-icon"></i>
          <p class="empty-text">请选择或创建一个知识库</p>
          <el-button type="primary" @click="showCreateDialog">创建知识库</el-button>
        </div>
      </div>
    </el-card>

    <!-- 创建知识库对话框 -->
    <el-dialog title="新建知识库" :visible.sync="createDialogVisible" width="500px">
      <el-form :model="newKBForm" :rules="kbRules" ref="kbForm" label-width="100px">
        <el-form-item label="知识库名称" prop="name">
          <el-input v-model="newKBForm.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="newKBForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入知识库描述"
          />
        </el-form-item>
        <el-form-item label="学科领域" prop="subject">
          <el-select v-model="newKBForm.subject" placeholder="请选择学科" style="width: 100%">
            <el-option label="数学" value="math" />
            <el-option label="语文" value="chinese" />
            <el-option label="英语" value="english" />
            <el-option label="物理" value="physics" />
            <el-option label="化学" value="chemistry" />
            <el-option label="生物" value="biology" />
            <el-option label="历史" value="history" />
            <el-option label="地理" value="geography" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createKnowledgeBase" :loading="creating">创建</el-button>
      </div>
    </el-dialog>

    <!-- 上传文档对话框 -->
    <el-dialog title="上传文档" :visible.sync="uploadDialogVisible" width="600px">
      <el-upload
        class="upload-demo"
        drag
        action="/api/admin/knowledge-base/upload"
        :data="{ kbId: selectedKB ? selectedKB.id : '' }"
        :headers="{ token: token }"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :before-upload="beforeUpload"
        accept=".txt,.pdf,.doc,.docx,.md,.zip,.json"
        multiple
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip" slot="tip">
          支持格式：.txt, .md, .json, .zip（自动解压提取文本）, .pdf, .doc, .docx，单个文件不超过50MB
        </div>
      </el-upload>
    </el-dialog>

    <!-- RAG问答对话框 -->
    <el-dialog title="RAG智能问答" :visible.sync="ragDialogVisible" width="800px" class="rag-dialog">
      <div class="rag-container">
        <div class="rag-messages" ref="ragChatBox">
          <div v-for="(msg, index) in ragMessages" :key="index" :class="['rag-message', msg.role]">
            <div class="rag-avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
            <div class="rag-content">
              <div class="rag-text">{{ msg.content }}</div>
              <div v-if="msg.references && msg.references.length > 0" class="rag-references">
                <div class="references-title">参考来源：</div>
                <div v-for="(ref, idx) in msg.references" :key="idx" class="reference-item">
                  [{{ idx + 1 }}] {{ ref.source }} - 相似度: {{ (ref.score * 100).toFixed(1) }}%
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="rag-input-box">
          <el-input
            v-model="ragInput"
            type="textarea"
            :rows="3"
            placeholder="请输入问题，AI将基于知识库内容回答..."
            @keyup.enter.native="sendRAGQuestion"
          />
          <el-button
            type="primary"
            @click="sendRAGQuestion"
            :loading="ragLoading"
            class="rag-send-btn"
          >
            发送
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 导入知识库对话框 -->
    <el-dialog title="导入本地知识库" :visible.sync="importDialogVisible" width="600px">
      <el-upload
        class="upload-demo"
        drag
        action="/api/admin/knowledge-base/import"
        :on-success="handleImportSuccess"
        :on-error="handleImportError"
        :before-upload="beforeImportUpload"
        accept=".json,.zip"
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将知识库文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip" slot="tip">
          支持格式：.json（知识库配置）, .zip（完整知识库包）
        </div>
      </el-upload>
    </el-dialog>
  </div>
</template>

<script>
import { knowledgeBaseApi } from '@/api/knowledgeBase'

export default {
  name: 'KnowledgeBaseList',
  data () {
    return {
      token: localStorage.getItem('token') || '',
      searchQuery: '',
      knowledgeBases: [],
      selectedKB: null,
      documents: [],
      loading: false,

      // 创建知识库
      createDialogVisible: false,
      creating: false,
      newKBForm: {
        name: '',
        description: '',
        subject: ''
      },
      kbRules: {
        name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }],
        description: [{ required: true, message: '请输入知识库描述', trigger: 'blur' }],
        subject: [{ required: true, message: '请选择学科', trigger: 'change' }]
      },

      // 上传文档
      uploadDialogVisible: false,

      // 向量检索
      searchTestQuery: '',
      searchLoading: false,
      searchResults: [],

      // RAG问答
      ragDialogVisible: false,
      ragMessages: [
        { role: 'assistant', content: '你好！我是基于本地知识库的AI助手，请输入您的问题。' }
      ],
      ragInput: '',
      ragLoading: false,

      // 导入知识库
      importDialogVisible: false
    }
  },
  created () {
    this.token = localStorage.getItem('token') || ''
  },
  computed: {
    filteredKnowledgeBases () {
      if (!this.searchQuery) return this.knowledgeBases
      const query = this.searchQuery.toLowerCase()
      return this.knowledgeBases.filter(kb =>
        kb.name.toLowerCase().includes(query) ||
        kb.description.toLowerCase().includes(query)
      )
    }
  },
  mounted () {
    this.loadKnowledgeBases()
  },
  methods: {
    // 加载知识库列表
    async loadKnowledgeBases () {
      try {
        const res = await knowledgeBaseApi.getList()
        if (res.code === 1) {
          this.knowledgeBases = res.response || []
          if (this.knowledgeBases.length > 0 && !this.selectedKB) {
            this.selectKnowledgeBase(this.knowledgeBases[0])
          }
        } else {
          console.error('知识库列表接口返回异常：', res)
          this.$message.error(res.message || '加载知识库列表失败')
        }
      } catch (error) {
        console.error('加载知识库列表异常：', error)
        this.$message.error('加载知识库列表失败：' + (error.message || '网络错误'))
      }
    },

    // 选择知识库
    async selectKnowledgeBase (kb) {
      this.selectedKB = kb
      await this.loadDocuments(kb.id)
    },

    // 加载文档列表
    async loadDocuments (kbId) {
      this.loading = true
      try {
        const res = await knowledgeBaseApi.getDocuments(kbId)
        if (res.code === 1) {
          this.documents = res.response || []
        } else {
          console.error('文档列表接口返回异常：', res)
        }
      } catch (error) {
        console.error('加载文档列表异常：', error)
        this.$message.error('加载文档列表失败')
      } finally {
        this.loading = false
      }
    },

    // 显示创建对话框
    showCreateDialog () {
      this.newKBForm = { name: '', description: '', subject: '' }
      this.createDialogVisible = true
    },

    // 创建知识库
    async createKnowledgeBase () {
      this.$refs.kbForm.validate(async valid => {
        if (!valid) return

        this.creating = true
        try {
          const res = await knowledgeBaseApi.create(this.newKBForm)
          if (res.code === 1) {
            this.$message.success('知识库创建成功')
            this.createDialogVisible = false
            await this.loadKnowledgeBases()
          } else {
            this.$message.error(res.message || '创建失败')
          }
        } catch (error) {
          this.$message.error('创建知识库失败')
        } finally {
          this.creating = false
        }
      })
    },

    // 删除知识库
    async deleteKnowledgeBase () {
      this.$confirm('确定要删除该知识库吗？所有文档将被删除且无法恢复！', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await knowledgeBaseApi.deleteKB(this.selectedKB.id)
          if (res.code === 1) {
            this.$message.success('删除成功')
            this.selectedKB = null
            await this.loadKnowledgeBases()
          }
        } catch (error) {
          this.$message.error('删除失败')
        }
      })
    },

    // 显示上传对话框
    showUploadDialog () {
      this.uploadDialogVisible = true
    },

    // 上传前检查
    beforeUpload (file) {
      const validTypes = ['.txt', '.pdf', '.doc', '.docx', '.md', '.zip', '.json']
      const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase()
      if (!validTypes.includes(ext)) {
        this.$message.error('不支持的文件格式')
        return false
      }
      if (file.size > 50 * 1024 * 1024) {
        this.$message.error('文件大小不能超过50MB')
        return false
      }
      return true
    },

    // 上传成功
    handleUploadSuccess () {
      this.$message.success('文档上传成功')
      this.uploadDialogVisible = false
      this.loadDocuments(this.selectedKB.id)
    },

    // 上传失败
    handleUploadError () {
      this.$message.error('文档上传失败')
    },

    // 查看文档
    viewDocument (doc) {
      const token = localStorage.getItem('token') || ''
      window.open(`/api/admin/knowledge-base/document/${doc.id}/preview?token=${token}`, '_blank')
    },

    // 重新处理文档
    async reprocessDocument (doc) {
      try {
        const res = await knowledgeBaseApi.reprocessDocument(doc.id)
        if (res.code === 1) {
          this.$message.success('文档重新处理中')
          this.loadDocuments(this.selectedKB.id)
        }
      } catch (error) {
        this.$message.error('重新处理失败')
      }
    },

    // 删除文档
    async deleteDocument (doc) {
      this.$confirm('确定要删除该文档吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await knowledgeBaseApi.deleteDocument(doc.id)
          if (res.code === 1) {
            this.$message.success('删除成功')
            this.loadDocuments(this.selectedKB.id)
          }
        } catch (error) {
          this.$message.error('删除失败')
        }
      })
    },

    // 获取文件图标
    getFileIcon (type) {
      const iconMap = {
        'txt': 'el-icon-document',
        'pdf': 'el-icon-document-checked',
        'doc': 'el-icon-document-copy',
        'docx': 'el-icon-document-copy',
        'md': 'el-icon-edit'
      }
      return iconMap[type] || 'el-icon-document'
    },

    // 格式化文件大小
    formatFileSize (size) {
      if (size < 1024) return size + ' B'
      if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
      if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)).toFixed(2) + ' MB'
      return (size / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
    },

    // 获取状态类型
    getStatusType (status) {
      const typeMap = {
        'pending': 'info',
        'processing': 'warning',
        'completed': 'success',
        'failed': 'danger'
      }
      return typeMap[status] || 'info'
    },

    // 获取状态文本
    getStatusText (status) {
      const textMap = {
        'pending': '待处理',
        'processing': '处理中',
        'completed': '已完成',
        'failed': '失败'
      }
      return textMap[status] || status
    },

    // 向量检索测试
    async testSearch () {
      if (!this.searchTestQuery.trim()) {
        this.$message.warning('请输入查询内容')
        return
      }

      this.searchLoading = true
      try {
        const res = await knowledgeBaseApi.search({
          kbId: this.selectedKB.id,
          query: this.searchTestQuery,
          topK: 5
        })
        if (res.code === 1) {
          this.searchResults = res.response || []
        }
      } catch (error) {
        this.$message.error('检索失败')
      } finally {
        this.searchLoading = false
      }
    },

    // 显示RAG对话框
    showRAGDialog () {
      this.ragDialogVisible = true
      this.ragMessages = [
        { role: 'assistant', content: '你好！我是基于知识库「' + this.selectedKB.name + '」的AI助手，请输入您的问题。' }
      ]
    },

    // 发送RAG问题
    async sendRAGQuestion () {
      if (!this.ragInput.trim()) return

      const question = this.ragInput
      this.ragMessages.push({ role: 'user', content: question })
      this.ragInput = ''
      this.ragLoading = true

      // 滚动到底部
      this.$nextTick(() => {
        if (this.$refs.ragChatBox) {
          this.$refs.ragChatBox.scrollTop = this.$refs.ragChatBox.scrollHeight
        }
      })

      try {
        let firstChunk = true
        await knowledgeBaseApi.ragChat({
          kbId: this.selectedKB.id,
          message: question
        }, (chunk, references) => {
          // 流式更新AI回复
          if (firstChunk) {
            // 第一个包，创建新的AI消息
            this.ragMessages.push({
              role: 'assistant',
              content: chunk,
              references: references || []
            })
            firstChunk = false
          } else {
            // 后续包，追加到现有消息
            const lastMsg = this.ragMessages[this.ragMessages.length - 1]
            if (lastMsg && lastMsg.role === 'assistant') {
              this.$set(lastMsg, 'content', lastMsg.content + chunk)
              if (references && (!lastMsg.references || lastMsg.references.length === 0)) {
                this.$set(lastMsg, 'references', references)
              }
            }
          }

          // 滚动到底部
          this.$nextTick(() => {
            if (this.$refs.ragChatBox) {
              this.$refs.ragChatBox.scrollTop = this.$refs.ragChatBox.scrollHeight
            }
          })
        })
      } catch (error) {
        console.error('RAG问答失败:', error)
        // 添加错误消息
        const lastMsg = this.ragMessages[this.ragMessages.length - 1]
        if (lastMsg && lastMsg.role === 'assistant') {
          this.$set(lastMsg, 'content', lastMsg.content + '\n\n[请求失败: ' + error.message + ']')
        } else {
          this.ragMessages.push({
            role: 'assistant',
            content: '抱歉，请求失败: ' + error.message
          })
        }
      } finally {
        this.ragLoading = false
      }
    },

    // 显示导入对话框
    showImportDialog () {
      this.importDialogVisible = true
    },

    // 导入前检查
    beforeImportUpload (file) {
      const validTypes = ['.json', '.zip']
      const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase()
      if (!validTypes.includes(ext)) {
        this.$message.error('不支持的文件格式，只支持 .json 和 .zip')
        return false
      }
      if (file.size > 100 * 1024 * 1024) {
        this.$message.error('文件大小不能超过100MB')
        return false
      }
      return true
    },

    // 导入成功
    handleImportSuccess () {
      this.$message.success('知识库导入成功')
      this.importDialogVisible = false
      this.loadKnowledgeBases()
    },

    // 导入失败
    handleImportError () {
      this.$message.error('知识库导入失败')
    }
  }
}
</script>

<style scoped>
.app-container {
  padding: 20px;
  background-color: var(--ink-3);
  min-height: calc(100vh - 84px);
}

.main-card {
  border-radius: 8px;
  border: none;
  background-color: #fff;
}

.card-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f2f5;
}

.header-icon {
  color: var(--acid);
  font-size: 18px;
  margin-right: 8px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--paper);
}

.knowledge-base-container {
  display: flex;
  height: calc(100vh - 200px);
}

/* 左侧边栏 */
.kb-sidebar {
  width: 300px;
  border-right: 1px solid #e4e7ed;
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.kb-actions {
  margin-bottom: 16px;
}

.kb-search {
  margin-bottom: 16px;
}

.kb-list {
  flex: 1;
  overflow-y: auto;
}

.kb-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 8px;
  border: 1px solid #e4e7ed;
}

.kb-item:hover {
  background-color: var(--ink-3);
  border-color: var(--ink-5);
}

.kb-item.active {
  background-color: var(--ink-3);
  border-color: var(--ink-5);
}

.kb-item-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.kb-icon {
  color: var(--acid);
  font-size: 16px;
  margin-right: 8px;
}

.kb-name {
  font-weight: 600;
  color: var(--paper);
  font-size: 14px;
}

.kb-item-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.kb-item-desc {
  font-size: 12px;
  color: var(--grey);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 右侧内容 */
.kb-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.kb-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  margin-bottom: 20px;
}

.kb-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.kb-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--paper);
  margin: 0 0 8px 0;
}

.kb-desc {
  color: var(--grey);
  margin: 0;
}

.kb-header-actions {
  display: flex;
  gap: 8px;
}

/* 文档列表 */
.documents-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--paper);
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}

.section-title i {
  margin-right: 8px;
  color: var(--acid);
}

/* 检索测试 */
.search-section {
  margin-top: 32px;
}

.search-box {
  margin-bottom: 16px;
}

.search-input {
  width: 100%;
}

.search-results {
  background: var(--ink-3);
  border-radius: 8px;
  padding: 16px;
}

.search-result-item {
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  border-left: 4px solid var(--acid);
}

.search-result-item:last-child {
  margin-bottom: 0;
}

.result-header {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
  font-size: 13px;
}

.result-rank {
  font-weight: 600;
  color: var(--acid);
}

.result-score {
  color: #67c23a;
}

.result-source {
  color: #909399;
}

.result-content {
  color: var(--grey);
  line-height: 1.6;
  font-size: 14px;
}

/* RAG对话框 */
.rag-container {
  display: flex;
  flex-direction: column;
  height: 500px;
}

.rag-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: var(--ink-3);
  border-radius: 8px;
  margin-bottom: 16px;
}

.rag-message {
  display: flex;
  margin-bottom: 16px;
}

.rag-message.user {
  flex-direction: row-reverse;
}

.rag-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--ink-3);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 10px;
  font-size: 12px;
}

.rag-message.user .rag-avatar {
  background: #67c23a;
}

.rag-content {
  max-width: 80%;
  padding: 12px 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.rag-message.user .rag-content {
  background: var(--ink-3);
}

.rag-text {
  line-height: 1.6;
  color: var(--grey);
}

.rag-references {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #dcdfe6;
  font-size: 12px;
  color: var(--grey);
}

.references-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.reference-item {
  margin-bottom: 2px;
}

.rag-input-box {
  display: flex;
  gap: 12px;
}

.rag-send-btn {
  align-self: flex-end;
}

/* 响应式 */
@media (max-width: 768px) {
  .knowledge-base-container {
    flex-direction: column;
  }

  .kb-sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #e4e7ed;
    max-height: 300px;
  }

  .kb-header {
    flex-direction: column;
    gap: 16px;
  }

  .kb-header-actions {
    flex-wrap: wrap;
  }
}
</style>

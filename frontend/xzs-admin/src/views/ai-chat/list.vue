<template>
  <div class="ai-chat-list-container">
    <el-card shadow="never" class="card-container">
      <div slot="header" class="card-header">
        <span>AI 对话历史列表</span>
        <el-button type="primary" size="mini" @click="goToChat">新建对话</el-button>
      </div>

      <el-table
        :data="chatList"
        border
        stripe
        style="width: 100%"
        v-loading="loading"
        :empty-text="emptyText"
      >
        <el-table-column prop="id" label="对话 ID" width="180" align="center" />
        <el-table-column prop="title" label="对话标题" min-width="240" />
        <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 'active' ? 'success' : 'info'">
              {{ scope.row.status === 'active' ? '进行中' : '已结束' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template slot-scope="scope">
            <el-button type="text" @click="viewChat(scope.row)">查看对话</el-button>
            <el-button type="text" danger @click="deleteChat(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="pageNum"
        :page-sizes="[10, 20, 50]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        style="margin-top: 20px; text-align: right"
      />
    </el-card>
  </div>
</template>

<script>
import aiChatApi from '@/api/ai-chat'

export default {
  name: 'AIChatList',
  data () {
    return {
      loading: false,
      chatList: [],
      pageNum: 1,
      pageSize: 10,
      total: 0,
      emptyText: '加载中...'
    }
  },
  created () {
    this.fetchChatList()
  },
  methods: {
    async fetchChatList () {
      this.loading = true
      this.emptyText = '加载中...'
      try {
        const res = await aiChatApi.list({
          pageNum: this.pageNum,
          pageSize: this.pageSize
        })
        if (res && res.data && Array.isArray(res.data.list)) {
          this.chatList = res.data.list
          this.total = res.data.total || 0
          this.emptyText = '暂无对话记录'
        } else {
          throw new Error('接口返回格式错误')
        }
      } catch (error) {
        console.error('加载对话列表失败:', error)
        this.chatList = []
        this.total = 0
        this.emptyText = '暂无对话记录'
      } finally {
        this.loading = false
      }
    },

    handleSizeChange (val) {
      this.pageSize = val
      this.fetchChatList()
    },

    handleCurrentChange (val) {
      this.pageNum = val
      this.fetchChatList()
    },

    goToChat () {
      this.$router.push({ path: '/agent/ai-chat/chat' })
    },

    viewChat (row) {
      this.$router.push({
        path: '/agent/ai-chat/chat',
        query: {
          chatId: row.id,
          title: row.title
        }
      })
    },

    async deleteChat (id) {
      try {
        await this.$confirm('确定删除该对话吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await aiChatApi.delete(id)
        this.chatList = this.chatList.filter(item => item.id !== id)
        this.total = Math.max(0, this.total - 1)
        this.$message.success('删除成功')
      } catch (error) {
        this.$message.info('已取消删除')
      }
    }
  }
}
</script>

<style scoped>
.ai-chat-list-container {
  padding: 20px;
  background-color: var(--ink-3);
  min-height: calc(100vh - 80px);
}

.card-container {
  background-color: #fff;
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
}
</style>

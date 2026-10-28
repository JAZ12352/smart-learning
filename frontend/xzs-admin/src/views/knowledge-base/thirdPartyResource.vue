<template>
  <div class="third-party-container">
    <!-- 顶部说明 -->
    <div class="tp-header">
      <div class="tp-header-left">
        <h2 class="tp-title">
          <i class="el-icon-link"></i>
          第三方资源获取
        </h2>
        <p class="tp-subtitle">从 B站技术区、Hugging Face、DataWhale、天池、AI Studio 等产业平台获取实训课件与项目数据集</p>
      </div>
      <div class="tp-header-right">
        <el-button size="small" type="text" icon="el-icon-info" @click="showHowTo = !showHowTo">
          {{ showHowTo ? '收起说明' : '使用说明' }}
        </el-button>
        <el-button size="small" type="text" icon="el-icon-refresh" @click="loadCachedResources">
          缓存管理
        </el-button>
      </div>
    </div>

    <!-- 使用说明 -->
    <el-alert
      v-if="showHowTo"
      type="info"
      :closable="true"
      @close="showHowTo = false"
      style="margin-bottom: 16px"
    >
      <div slot="title">
        <strong>如何使用：</strong>
        1. 选择资源类型（PPT课件 / 电子课本）→
        2. 输入关键词或粘贴第三方资源URL →
        3. 点击「搜索」从平台获取资源列表，或点击「直接获取」下载指定URL的文件 →
        4. 获取后的资源可在线预览（PDF/PPTX）或下载到本地，也可关联到课程中
      </div>
    </el-alert>

    <!-- 搜索 / 输入区 -->
    <div class="tp-input-section">
      <div class="tp-input-row">
        <el-select v-model="resourceType" placeholder="资源类型" size="medium" style="width: 140px">
          <el-option label="PPT课件" value="ppt" />
          <el-option label="电子课本" value="textbook" />
          <el-option label="文档" value="doc" />
          <el-option label="不限制" value="" />
        </el-select>

        <el-select v-model="selectedPlatform" placeholder="选择平台" size="medium" style="width: 160px; margin-left: 8px" clearable>
          <el-option
            v-for="p in platforms"
            :key="p.domain"
            :label="p.name"
            :value="p.name"
          />
          <el-option label="全部平台" value="" />
        </el-select>

        <el-input
          v-model="keyword"
          placeholder="输入课程名称或粘贴资源URL..."
          size="medium"
          clearable
          @keyup.enter.native="doSearch"
          style="flex: 1; margin-left: 8px"
        >
          <template slot="prepend">
            <i :class="isUrl(keyword) ? 'el-icon-link' : 'el-icon-search'"></i>
          </template>
        </el-input>

        <el-button
          v-if="isUrl(keyword)"
          type="primary"
          size="medium"
          icon="el-icon-download"
          :loading="fetching"
          @click="directFetch"
          style="margin-left: 8px"
        >
          直接获取
        </el-button>

        <el-button
          type="primary"
          size="medium"
          icon="el-icon-search"
          :loading="searching"
          @click="doSearch"
          style="margin-left: 8px"
        >
          搜索
        </el-button>
      </div>

      <!-- 搜索建议 -->
      <div class="tp-suggestions" v-if="suggestions.length">
        <span class="suggest-label">搜索建议：</span>
        <el-tag
          v-for="(s, i) in suggestions"
          :key="i"
          size="small"
          type="info"
          class="suggest-tag"
          @click="keyword = s; doSearch()"
        >
          {{ s }}
        </el-tag>
      </div>
    </div>

    <!-- 平台信息条 -->
    <div class="tp-platform-bar" v-if="platforms.length">
      <span class="platform-label">支持的平台：</span>
      <span v-for="p in platforms" :key="p.domain" class="platform-chip">
        <span class="chip-dot"></span>
        {{ p.name }}
        <span class="chip-types">({{ p.supportedTypes.join(' / ') }})</span>
      </span>
    </div>

    <!-- 搜索结果 -->
    <div v-if="searchResults && searchResults.total !== undefined" class="tp-results">
      <div class="tp-results-header">
        <h3>
          <i class="el-icon-search"></i>
          搜索结果（{{ searchResults.total }} 条 · 关键词: {{ searchResults.keyword }}）
        </h3>
        <span class="tp-results-time">
          搜索了 {{ searchResults.searchedPlatforms }} 个平台
        </span>
      </div>

      <el-empty v-if="!searchResults.items || !searchResults.items.length" description="未找到相关资源，请尝试更换关键词或平台" />

      <div v-else class="tp-results-grid">
        <div
          v-for="(item, idx) in searchResults.items"
          :key="idx"
          class="tp-result-card"
          @click="fetchAndPreview(item)"
        >
          <div class="tp-card-icon">
            <span v-if="item.type === 'PPT课件' || item.url.includes('.ppt')">📊</span>
            <span v-else-if="item.type === '电子课本' || item.url.includes('.pdf')">📖</span>
            <span v-else-if="item.type === 'search_link'">🔗</span>
            <span v-else>📄</span>
          </div>
          <div class="tp-card-body">
            <div class="tp-card-title">{{ item.title }}</div>
            <div class="tp-card-meta">
              <el-tag size="mini" type="info">{{ item.platform }}</el-tag>
              <span v-if="item.type">{{ item.type }}</span>
            </div>
            <div class="tp-card-note" v-if="item.note">{{ item.note }}</div>
          </div>
          <div class="tp-card-action">
            <el-button type="primary" size="mini" circle icon="el-icon-download" />
          </div>
        </div>
      </div>
    </div>

    <!-- 获取的资源详情 -->
    <el-dialog
      :visible.sync="previewVisible"
      width="800px"
      top="2vh"
      title="资源详情"
      @close="previewVisible = false"
    >
      <div v-if="currentResource" class="tp-preview">
        <!-- 资源信息 -->
        <div class="tp-preview-info">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="文件名">{{ currentResource.fileName }}</el-descriptions-item>
            <el-descriptions-item label="来源平台">{{ currentResource.platform }}</el-descriptions-item>
            <el-descriptions-item label="资源类型">{{ currentResource.resourceType }}</el-descriptions-item>
            <el-descriptions-item label="文件大小">{{ formatSize(currentResource.fileSize) }}</el-descriptions-item>
            <el-descriptions-item label="内容类型">{{ currentResource.contentType }}</el-descriptions-item>
            <el-descriptions-item label="缓存状态">
              <el-tag :type="currentResource.cached ? 'success' : 'warning'" size="small">
                {{ currentResource.cached ? '已缓存' : '新下载' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <!-- 元数据 -->
          <div v-if="currentResource.metadata" class="tp-metadata">
            <div v-for="(val, key) in currentResource.metadata" :key="key" class="tp-meta-item">
              <span class="meta-key">{{ key }}:</span>
              <span class="meta-val">{{ val }}</span>
            </div>
          </div>
        </div>

        <!-- 预览区域 -->
        <div class="tp-preview-area" v-if="isPreviewable(currentResource)">
          <div class="tp-preview-toolbar">
            <span>预览</span>
            <el-button size="mini" type="text" icon="el-icon-full-screen" @click="openFullscreen">全屏</el-button>
          </div>
          <div class="tp-preview-frame">
            <iframe
              v-if="currentResource.localUrl"
              :src="currentResource.localUrl + '?disposition=inline'"
              frameborder="0"
              class="tp-iframe"
            />
            <div v-else class="tp-no-preview">
              <i class="el-icon-warning-outline"></i>
              <span>该资源类型暂不支持在线预览，请下载后查看</span>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="tp-preview-actions">
          <el-button
            v-if="currentResource.localUrl"
            type="primary"
            icon="el-icon-download"
            @click="downloadResource(currentResource)"
          >
            下载文件
          </el-button>

          <el-button
            type="success"
            icon="el-icon-plus"
            @click="addToCourse(currentResource)"
          >
            关联到课程
          </el-button>

          <el-button
            type="warning"
            icon="el-icon-refresh"
            @click="refetchResource"
            :loading="fetching"
          >
            重新获取
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 缓存管理对话框 -->
    <el-dialog
      :visible.sync="cacheVisible"
      width="700px"
      title="缓存管理"
      @close="cacheVisible = false"
    >
      <div class="tp-cache-toolbar">
        <span>共 {{ cachedResources.length }} 个缓存文件</span>
        <el-button size="small" type="danger" icon="el-icon-delete" @click="clearAllCache" :disabled="!cachedResources.length">
          清空全部
        </el-button>
      </div>
      <el-table :data="cachedResources" max-height="400" size="small">
        <el-table-column prop="name" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="90">
          <template slot-scope="{row}">
            <el-tag size="mini">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="90">
          <template slot-scope="{row}">{{ formatSize(row.size) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template slot-scope="{row}">
            <el-button size="mini" type="text" icon="el-icon-download" @click="downloadByName(row.name)">下载</el-button>
            <el-button size="mini" type="text" icon="el-icon-delete" style="color: #f56c6c" @click="deleteCache(row.name)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 关联课程对话框 -->
    <el-dialog
      :visible.sync="addToCourseVisible"
      width="500px"
      title="关联到课程"
    >
      <el-form label-width="80px" size="small">
        <el-form-item label="课程名称">
          <el-input v-model="courseForm.name" placeholder="输入课程名称" />
        </el-form-item>
        <el-form-item label="资源类型">
          <el-tag>{{ currentResource ? currentResource.resourceType : '' }}</el-tag>
        </el-form-item>
        <el-form-item label="来源平台">
          <el-tag type="info">{{ currentResource ? currentResource.platform : '' }}</el-tag>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="addToCourseVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddToCourse" :loading="addingToCourse">确认关联</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import thirdPartyApi from '@/api/thirdPartyResource'
import courseApi from '@/api/courseResource'

export default {
  name: 'ThirdPartyResource',
  data () {
    return {
      // 输入
      keyword: '',
      resourceType: '',
      selectedPlatform: '',
      // 状态
      searching: false,
      fetching: false,
      addingToCourse: false,
      showHowTo: false,
      // 数据
      platforms: [],
      suggestions: [],
      searchResults: null,
      currentResource: null,
      cachedResources: [],
      // 对话框
      previewVisible: false,
      cacheVisible: false,
      addToCourseVisible: false,
      // 课程关联表单
      courseForm: {
        name: '',
        resourceType: ''
      }
    }
  },
  mounted () {
    this.loadPlatforms()
    this.loadCachedResources()
  },
  methods: {
    // ── 平台 ──
    loadPlatforms () {
      thirdPartyApi.getPlatforms().then(res => {
        if (res && res.code === 1) {
          this.platforms = res.response || []
        }
      }).catch(() => {})
    },

    // ── 搜索 ──
    doSearch () {
      const kw = this.keyword.trim()
      if (!kw) {
        this.$message.warning('请输入搜索关键词')
        return
      }

      // 如果输入的是URL，自动检测平台
      if (this.isUrl(kw)) {
        this.detectAndSearch(kw)
        return
      }

      this.searching = true
      thirdPartyApi.search(kw, this.selectedPlatform, this.resourceType).then(res => {
        if (res && res.code === 1) {
          this.searchResults = res.response
          if (!res.response.items || !res.response.items.length) {
            this.$message.info('未找到相关资源，请尝试更换关键词')
          }
        } else {
          this.$message.error(res ? res.message : '搜索失败')
        }
      }).catch(e => {
        this.$message.error('搜索请求失败: ' + (e.message || '网络错误'))
      }).finally(() => {
        this.searching = false
      })
    },

    detectAndSearch (url) {
      thirdPartyApi.detectPlatform(url).then(res => {
        if (res && res.code === 1 && res.response.recognized) {
          this.selectedPlatform = res.response.platform
          this.$message.success('识别到平台: ' + res.response.platform)
        }
        this.doSearchUrl(url)
      }).catch(() => {
        this.doSearchUrl(url)
      })
    },

    doSearchUrl (url) {
      this.searching = true
      thirdPartyApi.search(url, this.selectedPlatform, this.resourceType).then(res => {
        if (res && res.code === 1) {
          this.searchResults = res.response
        }
      }).catch(e => {
        this.$message.error('搜索失败')
      }).finally(() => {
        this.searching = false
      })
    },

    // ── 直接获取 ──
    directFetch () {
      const url = this.keyword.trim()
      if (!url) return

      this.fetching = true
      thirdPartyApi.fetchResource(url, false).then(res => {
        if (res && res.code === 1) {
          this.currentResource = res.response
          this.previewVisible = true
          this.$message.success('资源获取成功')
        } else {
          this.$message.error(res ? res.message : '获取失败')
        }
      }).catch(e => {
        this.$message.error('获取失败: ' + (e.message || '网络错误'))
      }).finally(() => {
        this.fetching = false
      })
    },

    // ── 预览 ──
    fetchAndPreview (item) {
      const url = item.url
      if (item.type === 'search_link') {
        // 直接打开搜索链接
        window.open(url, '_blank')
        return
      }

      this.fetching = true
      thirdPartyApi.fetchResource(url, false).then(res => {
        if (res && res.code === 1) {
          this.currentResource = res.response
          this.previewVisible = true
        } else {
          this.$message.warning('无法直接获取该资源，请尝试在原始平台下载')
          window.open(url, '_blank')
        }
      }).catch(e => {
        this.$message.warning('获取失败，正在打开原始链接...')
        window.open(url, '_blank')
      }).finally(() => {
        this.fetching = false
      })
    },

    refetchResource () {
      if (!this.currentResource || !this.currentResource.url) return
      const url = this.currentResource.url
      this.fetching = true
      thirdPartyApi.fetchResource(url, true).then(res => {
        if (res && res.code === 1) {
          this.currentResource = res.response
          this.$message.success('已重新获取')
        } else {
          this.$message.error('重新获取失败')
        }
      }).finally(() => {
        this.fetching = false
      })
    },

    isPreviewable (resource) {
      if (!resource || !resource.fileName) return false
      const ext = resource.fileName.toLowerCase().split('.').pop()
      return ['pdf', 'pptx', 'ppt', 'jpg', 'jpeg', 'png', 'gif', 'svg', 'txt', 'html'].includes(ext)
    },

    openFullscreen () {
      if (this.currentResource && this.currentResource.localUrl) {
        window.open(this.currentResource.localUrl + '?disposition=inline', '_blank')
      }
    },

    // ── 下载 ──
    downloadResource (resource) {
      if (resource && resource.localUrl) {
        const link = document.createElement('a')
        link.href = resource.localUrl + '?disposition=attachment'
        link.download = resource.fileName || 'resource'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
      }
    },

    downloadByName (fileName) {
      const link = document.createElement('a')
      link.href = '/api/admin/third-party/download/' + encodeURIComponent(fileName) + '?disposition=attachment'
      link.download = fileName
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    },

    // ── 缓存管理 ──
    loadCachedResources () {
      thirdPartyApi.listCached().then(res => {
        if (res && res.code === 1) {
          this.cachedResources = res.response || []
        }
      }).catch(() => {})
    },

    deleteCache (fileName) {
      this.$confirm('确定删除缓存文件 "' + fileName + '" 吗？', '确认', { type: 'warning' }).then(() => {
        thirdPartyApi.deleteCached(fileName).then(res => {
          if (res && res.code === 1) {
            this.$message.success('已删除')
            this.loadCachedResources()
          }
        }).catch(() => {
          this.$message.error('删除失败')
        })
      }).catch(() => {})
    },

    clearAllCache () {
      this.$confirm('确定清空所有缓存的第三方资源？此操作不可恢复。', '确认清空', { type: 'warning' }).then(() => {
        thirdPartyApi.clearAllCached().then(res => {
          if (res && res.code === 1) {
            this.$message.success('已清空 ' + (res.response.deleted || 0) + ' 个缓存文件')
            this.loadCachedResources()
            this.cacheVisible = false
          }
        }).catch(() => {
          this.$message.error('清空失败')
        })
      }).catch(() => {})
    },

    // ── 课程关联 ──
    addToCourse (resource) {
      this.courseForm.name = resource.metadata ? (resource.metadata.inferredTitle || resource.fileName) : resource.fileName
      this.courseForm.resourceType = resource.resourceType
      this.addToCourseVisible = true
    },

    confirmAddToCourse () {
      this.addingToCourse = true
      const courseData = {
        name: this.courseForm.name,
        description: '从 ' + (this.currentResource ? this.currentResource.platform : '第三方') + ' 获取的' + (this.currentResource ? this.currentResource.resourceType : '资源'),
        category: this.currentResource ? this.currentResource.resourceType : '其他',
        source: 'third_party',
        sourceUrl: this.currentResource ? this.currentResource.url : '',
        chapters: [],
        pptList: this.currentResource && this.currentResource.resourceType === 'PPT课件'
          ? [{ title: this.currentResource.fileName, url: this.currentResource.localUrl, platform: this.currentResource.platform }]
          : [],
        textbookResources: this.currentResource && this.currentResource.resourceType === '电子课本'
          ? [{ title: this.currentResource.fileName, url: this.currentResource.localUrl, platform: this.currentResource.platform }]
          : [],
        videoList: [],
        exercises: []
      }

      courseApi.importCourse(courseData).then(res => {
        if (res && res.code === 1) {
          this.$message.success('已关联到课程: ' + this.courseForm.name)
          this.addToCourseVisible = false
        } else {
          this.$message.error(res ? res.message : '关联失败')
        }
      }).catch(e => {
        this.$message.error('关联失败: ' + (e.message || '网络错误'))
      }).finally(() => {
        this.addingToCourse = false
      })
    },

    // ── 工具 ──
    isUrl (str) {
      return /^https?:\/\//.test(str.trim())
    },

    formatSize (bytes) {
      if (!bytes || bytes < 0) return '未知'
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
    }
  }
}
</script>

<style scoped>
.third-party-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.tp-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.tp-header-left .tp-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--grey);
  margin: 0 0 4px 0;
}

.tp-header-left .tp-title i {
  color: var(--acid);
  margin-right: 6px;
}

.tp-header-left .tp-subtitle {
  color: #909399;
  font-size: 13px;
  margin: 0;
}

/* 搜索区 */
.tp-input-section {
  background: #fff;
  border: 1px solid #e8ecf1;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.tp-input-row {
  display: flex;
  align-items: center;
}

.tp-suggestions {
  margin-top: 12px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.suggest-label {
  font-size: 12px;
  color: #909399;
  margin-right: 4px;
}

.suggest-tag {
  cursor: pointer;
}
.suggest-tag:hover {
  color: var(--acid);
  border-color: var(--ink-5);
}

/* 平台条 */
.tp-platform-bar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.platform-label {
  font-size: 13px;
  color: var(--grey);
  font-weight: 500;
}

.platform-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: var(--ink-3);
  border-radius: 20px;
  font-size: 12px;
  color: var(--grey);
}

.chip-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ink-3);
}

.chip-types {
  color: #a0a4ab;
}

/* 结果 */
.tp-results {
  background: #fff;
  border: 1px solid #e8ecf1;
  border-radius: 12px;
  padding: 16px;
}

.tp-results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.tp-results-header h3 {
  font-size: 16px;
  margin: 0;
  color: var(--grey);
}

.tp-results-header h3 i {
  color: var(--acid);
  margin-right: 6px;
}

.tp-results-time {
  font-size: 12px;
  color: #909399;
}

.tp-results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
}

.tp-result-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #e8ecf1;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.tp-result-card:hover {
  border-color: var(--ink-5);
  box-shadow: 0 2px 12px rgba(var(--acid-rgb), 0.12);
  transform: translateY(-1px);
}

.tp-card-icon {
  font-size: 28px;
  flex-shrink: 0;
}

.tp-card-body {
  flex: 1;
  min-width: 0;
}

.tp-card-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--grey);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 6px;
}

.tp-card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #909399;
}

.tp-card-note {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 4px;
}

/* 预览对话框 */
.tp-preview-info {
  margin-bottom: 16px;
}

.tp-metadata {
  margin-top: 12px;
  padding: 12px;
  background: var(--ink-3);
  border-radius: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.tp-meta-item {
  font-size: 13px;
}

.meta-key {
  color: #909399;
  margin-right: 4px;
}

.meta-val {
  color: var(--grey);
  font-weight: 500;
}

.tp-preview-area {
  border: 1px solid #e8ecf1;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 16px;
}

.tp-preview-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 14px;
  background: var(--ink-3);
  border-bottom: 1px solid #e8ecf1;
  font-size: 13px;
  font-weight: 500;
}

.tp-preview-frame {
  height: 500px;
  background: var(--ink-3);
}

.tp-iframe {
  width: 100%;
  height: 100%;
}

.tp-no-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
  font-size: 14px;
  gap: 8px;
}

.tp-no-preview i {
  font-size: 40px;
  color: #dcdfe6;
}

.tp-preview-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
  padding-top: 8px;
}

/* 缓存管理 */
.tp-cache-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--grey);
}
</style>

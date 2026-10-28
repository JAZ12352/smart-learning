<template>
  <el-breadcrumb class="app-breadcrumb" separator="">
    <transition-group name="breadcrumb">
      <el-breadcrumb-item v-for="(item,index) in levelList" :key="item.path">
        <!-- 分隔符 -->
        <span v-if="index > 0" class="breadcrumb-separator">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="sep-icon">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </span>
        <span v-if="item.redirect==='noRedirect'||index==levelList.length-1" class="no-redirect">{{ item.meta.title }}</span>
        <a v-else @click.prevent="handleLink(item)">{{ item.meta.title }}</a>
      </el-breadcrumb-item>
    </transition-group>
  </el-breadcrumb>
</template>

<script>
import * as pathToRegexp from 'path-to-regexp'

export default {
  data () {
    return {
      levelList: null
    }
  },
  watch: {
    $route (route) {
      if (route.path.startsWith('/redirect/')) {
        return
      }
      this.getBreadcrumb()
    }
  },
  created () {
    this.getBreadcrumb()
  },
  methods: {
    getBreadcrumb () {
      let matched = this.$route.matched.filter(item => item.meta && item.meta.title)
      const first = matched[0]

      if (!this.isDashboard(first)) {
        matched = [{ path: '/dashboard', meta: { title: '主页' } }].concat(matched)
      }
      this.levelList = matched.filter(item => item.meta && item.meta.title && item.meta.breadcrumb !== false)
    },
    isDashboard (route) {
      const name = route && route.name
      if (!name) {
        return false
      }
      return name.trim().toLocaleLowerCase() === 'Dashboard'.toLocaleLowerCase()
    },
    pathCompile (path) {
      const { params } = this.$route
      var toPath = pathToRegexp.compile(path)
      return toPath(params)
    },
    handleLink (item) {
      const { redirect, path } = item
      if (redirect) {
        this.$router.push(redirect)
        return
      }
      this.$router.push(this.pathCompile(path))
    }
  }
}
</script>

<style scoped>
.app-breadcrumb.el-breadcrumb {
  display: inline-flex;
  align-items: center;
  font-size: 13px;
  line-height: 56px;
  margin-left: 4px;
}

.app-breadcrumb.el-breadcrumb .no-redirect {
  color: var(--grey);
  cursor: text;
  font-weight: 500;
}

/* 分隔符 */
.breadcrumb-separator {
  display: inline-flex;
  align-items: center;
  margin: 0 4px;
  color: #cbd5e1;
}

.sep-icon {
  width: 13px;
  height: 13px;
}

/* 链接样式 */
.app-breadcrumb.el-breadcrumb >>> .el-breadcrumb__inner {
  color: #94a3b8;
  font-weight: 400;
  transition: color 0.2s ease;
}

.app-breadcrumb.el-breadcrumb >>> .el-breadcrumb__inner.is-link:hover {
  color: var(--acid);
}

.app-breadcrumb.el-breadcrumb >>> .el-breadcrumb__item:last-child .el-breadcrumb__inner {
  color: #1e293b;
  font-weight: 500;
}
</style>

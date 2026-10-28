<template>
  <div class="teaching-hub">
    <!-- ═══ Animated Background ═══ -->
    <div class="hub-bg">
      <div class="hub-bg-gradient"></div>
      <div class="hub-bg-particles">
        <span v-for="i in 8" :key="'p'+i" class="hub-particle" :style="{left: (i*9%73)+'%', top: (i*17%87)+'%', animationDelay: (i*0.3)+'s', animationDuration: (8+i*0.7)+'s'}"></span>
      </div>
      <div class="hub-bg-grid"></div>
    </div>

    <!-- ═══ Top Bar ═══ -->
    <div class="hub-topbar">
      <div class="hub-topbar-inner">
        <div class="hub-brand">
          <div class="hub-brand-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c0 1.1 2.7 2 6 2s6-.9 6-2v-5"/></svg>
          </div>
          <span class="hub-brand-text">教学资源中心</span>
        </div>
        <div class="hub-nav">
          <span v-for="cat in navCats" :key="cat.key" :class="['hub-nav-link',{active:moocTab===cat.key}]" @click="moocTab=cat.key">
            {{ cat.label }}
            <span class="hub-nav-indicator"></span>
          </span>
        </div>
        <div class="hub-actions">
          <div class="hub-search-wrap">
            <i class="el-icon-search hub-search-icon"></i>
            <input class="hub-search-input" v-model="searchQuery" placeholder="搜索课程、课件、电子课本..." @input="onSearch" />
            <i v-if="searchQuery" class="el-icon-close hub-search-clear" @click="searchQuery='';onSearch()"></i>
          </div>
          <button class="hub-btn hub-btn-primary" @click="showImportDialog = true">
            <i class="el-icon-plus"></i> 导入课程
          </button>
        </div>
      </div>
    </div>

    <!-- ═══ Hero Banner ═══ -->
    <div class="hub-hero">
      <div class="hub-hero-overlay"></div>
      <div class="hub-hero-content">
        <div class="hub-hero-badge">📚 专业教学资源平台</div>
        <h1 class="hub-hero-title">汇聚<span class="hub-hero-accent">全国高校</span>精品课程资源</h1>
        <p class="hub-hero-desc">课件PPT · 教学视频 · 电子课本 · 习题库 — 一站式教学资源管理</p>
        <div class="hub-hero-stats">
          <div class="hub-stat" v-for="s in heroStats" :key="s.label">
            <span class="hub-stat-num" ref="heroStatNums">{{ s.value }}</span>
            <span class="hub-stat-label">{{ s.label }}</span>
          </div>
        </div>
      </div>
      <div class="hub-hero-shape hub-hero-shape-1"></div>
      <div class="hub-hero-shape hub-hero-shape-2"></div>
    </div>

    <!-- ═══ Platform Hub ═══ -->
    <div class="hub-section">
      <div class="hub-section-header">
        <div>
          <h2 class="hub-section-title">
            <span class="hub-section-icon">🔗</span> 第三方平台接入
          </h2>
          <p class="hub-section-desc">上传PPT课件或电子课本到以下平台，获取在线预览链接后可直接嵌入本地预览</p>
        </div>
        <button class="hub-btn hub-btn-ghost" @click="showPlatformBar = !showPlatformBar">
          {{ showPlatformBar ? '收起' : '展开' }} <i :class="showPlatformBar?'el-icon-arrow-up':'el-icon-arrow-down'"></i>
        </button>
      </div>
      <div class="hub-platform-grid" v-show="showPlatformBar">
        <a v-for="p in recommendPlatforms" :key="p.name" :href="p.url" target="_blank" class="hub-platform-card">
          <div class="hub-platform-card-icon">{{ p.icon }}</div>
          <div class="hub-platform-card-body">
            <strong>{{ p.name }}</strong>
            <p>{{ p.desc }}</p>
          </div>
          <span class="hub-platform-card-tag">{{ p.type }}</span>
          <i class="el-icon-top-right hub-platform-card-arrow"></i>
        </a>
      </div>
    </div>

    <!-- ═══ Course Grid ═══ -->
    <div class="hub-section">
      <div class="hub-section-header">
        <div>
          <h2 class="hub-section-title">
            <span class="hub-section-icon">📋</span> {{ moocTabName }}
          </h2>
          <p class="hub-section-desc">共 {{ filteredCourses.length }} 门课程</p>
        </div>
        <div class="hub-section-filters">
          <el-button size="small" type="text" icon="el-icon-info" @click="showPlatformBar = !showPlatformBar" v-if="!showPlatformBar">第三方平台推荐</el-button>
        </div>
      </div>

      <div class="hub-course-grid">
        <div v-for="course in filteredCourses" :key="course.id" class="hub-course-card" @click="openCourse(course)">
          <div class="hub-card-cover" :style="{background: coverGradient(course.name)}">
            <div class="hub-card-cover-pattern"></div>
            <div class="hub-card-badge" :class="course.status || 'ongoing'">{{ statusText(course.status) }}</div>
            <div class="hub-card-school">{{ course.school }}</div>
          </div>
          <div class="hub-card-content">
            <h3 class="hub-card-title">{{ course.name }}</h3>
            <p class="hub-card-desc">{{ course.description }}</p>
            <div class="hub-card-meta">
              <span class="hub-meta-item"><i class="el-icon-user"></i>{{ fmtNum(course.studentCount) }}</span>
              <span class="hub-meta-item" v-if="course.rating"><i class="el-icon-star-on"></i>{{ course.rating }}</span>
              <span class="hub-meta-item"><i class="el-icon-document"></i>{{ course.resourceCount }} 资源</span>
            </div>
          </div>
          <div class="hub-card-footer">
            <div class="hub-card-footer-left">
              <span class="hub-card-credit">{{ course.credit }} 学分</span>
              <span class="hub-card-term">{{ course.term }}</span>
            </div>
            <span class="hub-card-link">进入课程 <i class="el-icon-arrow-right"></i></span>
          </div>
        </div>

        <!-- ═══ Empty State ═══ -->
        <div v-if="filteredCourses.length === 0" class="hub-empty">
          <div class="hub-empty-icon">🔍</div>
          <h3>没有找到相关课程</h3>
          <p>试试其他分类或搜索关键词</p>
          <button class="hub-btn hub-btn-primary" @click="searchQuery='';moocTab='all'">查看全部课程</button>
        </div>
      </div>
    </div>

    <!-- ═══ Course Detail Dialog ═══ -->
    <el-dialog :visible.sync="courseDetailVisible" width="1000px" top="2vh" custom-class="hub-detail-dialog" @close="courseDetailVisible=false" :close-on-click-modal="false">
      <div slot="title" class="hub-detail-header" v-if="currentCourse">
        <div class="hub-detail-cover" :style="{background: coverGradient(currentCourse.name)}">
          <span>{{ currentCourse.name.charAt(0) }}</span>
        </div>
        <div class="hub-detail-info">
          <h2>{{ currentCourse.name }}</h2>
          <p class="hub-detail-instructor">
            <i class="el-icon-user-solid"></i> {{ currentCourse.instructor }}
            <span class="hub-detail-dot">·</span>
            <i class="el-icon-school"></i> {{ currentCourse.school }}
          </p>
          <div class="hub-detail-tags">
            <span class="hub-tag" :class="currentCourse.status==='ongoing'?'hub-tag-success':'hub-tag-warning'">
              {{ currentCourse.status==='ongoing'?'进行中':'即将开始' }}
            </span>
            <span class="hub-tag hub-tag-default">{{ currentCourse.credit }} 学分</span>
            <span class="hub-tag hub-tag-default">{{ currentCourse.period }} 学时</span>
          </div>
        </div>
      </div>
      <div class="hub-detail-body" v-if="courseDetailVisible && currentCourse">
        <el-tabs v-model="detailTab" class="hub-detail-tabs">
          <el-tab-pane label="课程介绍" name="intro">
            <div class="hub-intro">
              <div class="hub-intro-content" v-html="currentCourse.richDesc"></div>
              <div class="hub-intro-stats">
                <div class="hub-intro-stat">
                  <div class="hub-intro-stat-icon" style="background:linear-gradient(135deg,#667eea,#764ba2)"><i class="el-icon-document"></i></div>
                  <div><strong>课件PPT</strong><span>{{ (currentCourse.pptList||[]).length }} 个</span></div>
                </div>
                <div class="hub-intro-stat">
                  <div class="hub-intro-stat-icon" style="background:linear-gradient(135deg,#f093fb,#f5576c)"><i class="el-icon-video-camera"></i></div>
                  <div><strong>教学视频</strong><span>{{ (currentCourse.videoList||[]).length }} 个</span></div>
                </div>
                <div class="hub-intro-stat">
                  <div class="hub-intro-stat-icon" style="background:linear-gradient(135deg,#4facfe,#00f2fe)"><i class="el-icon-notebook-2"></i></div>
                  <div><strong>电子课本</strong><span>{{ currentCourse.textbook }}</span></div>
                </div>
                <div class="hub-intro-stat">
                  <div class="hub-intro-stat-icon" style="background:linear-gradient(135deg,#43e97b,#38f9d7)"><i class="el-icon-edit-outline"></i></div>
                  <div><strong>习题</strong><span>{{ (currentCourse.exercises||[]).length }} 题</span></div>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- Chapters Tab -->
          <el-tab-pane label="课程章节" name="chapters">
            <div v-if="currentChapters.length" class="hub-chapters">
              <div v-for="(ch,idx) in currentChapters" :key="idx" class="hub-chapter">
                <div class="hub-chapter-header" :style="{background: coverGradient(ch.title)}" @click="activeChapters=activeChapters===idx?-1:idx">
                  <div class="hub-chapter-header-left">
                    <span class="hub-chapter-num">{{idx+1}}</span>
                    <span class="hub-chapter-title">{{ch.title}}</span>
                  </div>
                  <div class="hub-chapter-header-right">
                    <span class="hub-chapter-meta">{{ch.lessons.length}} 课时</span>
                    <i :class="activeChapters===idx?'el-icon-arrow-up':'el-icon-arrow-down'" class="hub-chapter-arrow"></i>
                  </div>
                </div>
                <div v-show="activeChapters===idx" class="hub-chapter-body">
                  <div class="ch-detail" v-html="ch.detailHtml"></div>
                  <div class="hub-lesson-grid">
                    <div v-for="(lesson,li) in ch.lessons" :key="li" class="hub-lesson" @click="playLesson(lesson)">
                      <div class="hub-lesson-icon" :class="lesson.type">
                        <i :class="lesson.type==='video'?'el-icon-video-play':'el-icon-presentation'"></i>
                      </div>
                      <div class="hub-lesson-info">
                        <div class="hub-lesson-name">{{lesson.name}}</div>
                        <div class="hub-lesson-meta">
                          <span>{{lesson.desc}}</span>
                          <el-tag v-if="lesson.type==='video'&&lesson.url&&isExternalUrl(lesson.url)" size="mini" effect="plain" type="danger">B站</el-tag>
                        </div>
                      </div>
                      <button class="hub-btn hub-btn-sm" :class="lesson.type==='video'?'hub-btn-play':'hub-btn-view'">
                        <i :class="lesson.btnIcon"></i> {{lesson.btnText}}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="hub-empty-small">
              <i class="el-icon-document"></i>
              <p>暂无章节数据</p>
            </div>
          </el-tab-pane>

          <!-- PPT Tab — 直接显示PPT幻灯片 -->
          <el-tab-pane label="课件PPT" name="ppt">
            <div class="hub-resource-tab" v-if="currentChapterSlides.length">
              <!-- 顶部工具栏 -->
              <div class="hub-ppt-toolbar">
                <el-select v-model="pptActiveChapter" size="small" style="width:220px" @change="onPptChapterChange" placeholder="选择章节">
                  <el-option v-for="(ch,idx) in (currentCourse._chTitles||[])" :key="idx" :label="'第'+(idx+1)+'章 '+ch" :value="idx">
                    <span>第{{ idx+1 }}章 {{ ch }}</span>
                    <span style="float:right;color:#94a3b8;font-size:11px">{{ getChapterSlideCount(idx) }}页</span>
                  </el-option>
                </el-select>
                <span style="color:#64748b;font-size:13px">
                  <strong style="color:#1e40af">{{ pptSlideIdx+1 }}</strong> / {{ currentChapterSlides.length }} 页
                </span>
                <div style="flex:1"></div>
                <button class="hub-btn hub-btn-sm hub-btn-outline" @click="regenerateChapterPPT(pptActiveChapter)"><i class="el-icon-refresh"></i></button>
              </div>
              <!-- PPT幻灯片展示 -->
              <div class="hub-ppt-slideshow">
                <button class="hub-ppt-nav hub-ppt-prev" @click="pptSlideIdx=Math.max(0,pptSlideIdx-1)" :disabled="pptSlideIdx<=0"><i class="el-icon-arrow-left"></i></button>
                <div class="hub-ppt-slide-display" @click="previewChapterPPTSlide(pptSlideIdx)">
                  <div class="hub-ppta-slide-card" :class="'hub-ppta-'+getPptaType(currentChapterSlides[pptSlideIdx])">
                    <template v-if="getPptaType(currentChapterSlides[pptSlideIdx])==='cover'">
                      <div class="sv-topbar"></div>
                      <div class="ppta-cover-badge">课堂教学课件</div>
                      <h1 class="ppta-cover-title">{{ currentChapterSlides[pptSlideIdx].title }}</h1>
                      <p class="ppta-cover-sub" v-if="currentChapterSlides[pptSlideIdx]">{{ (currentChapterSlides[pptSlideIdx].points||[]).join(' · ') }}</p>
                    </template>
                    <template v-else-if="getPptaType(currentChapterSlides[pptSlideIdx])==='goal'">
                      <div class="sv-topbar"></div>
                      <h2 class="sv-section-title">{{ currentChapterSlides[pptSlideIdx].title }}</h2>
                      <div class="sv-goal-cards">
                        <div v-for="(pt,pi) in (currentChapterSlides[pptSlideIdx].points||[]).slice(0,3)" :key="pi" class="sv-goal-card" :class="'sv-goal-card--'+pi">
                          <span class="sv-goal-num">0{{ pi+1 }}</span><p class="sv-goal-text">{{ pt }}</p>
                        </div>
                      </div>
                    </template>
                    <template v-else-if="getPptaType(currentChapterSlides[pptSlideIdx])==='keypoint'">
                      <div class="sv-topbar sv-topbar--orange"></div>
                      <h2 class="sv-section-title sv-section-title--orange">{{ currentChapterSlides[pptSlideIdx].title }}</h2>
                      <div class="sv-kp-steps">
                        <div v-for="(pt,pi) in (currentChapterSlides[pptSlideIdx].points||[]).slice(0,5)" :key="pi" class="sv-kp-step">
                          <div class="sv-kp-dot">{{ pi+1 }}</div><p class="sv-kp-text">{{ pt }}</p>
                        </div>
                      </div>
                    </template>
                    <template v-else-if="getPptaType(currentChapterSlides[pptSlideIdx])==='scenario'">
                      <div class="sv-topbar sv-topbar--teal"></div>
                      <h2 class="sv-section-title sv-section-title--teal">{{ currentChapterSlides[pptSlideIdx].title }}</h2>
                      <div class="sv-scenario-box"><div class="sv-scenario-bar"></div>
                        <p class="sv-scenario-story" v-for="(pt,pi) in (currentChapterSlides[pptSlideIdx].points||[]).slice(0,3)" :key="pi">{{ pt }}</p>
                      </div>
                    </template>
                    <template v-else-if="getPptaType(currentChapterSlides[pptSlideIdx])==='exercise'">
                      <div class="sv-topbar sv-topbar--purple"></div>
                      <h2 class="sv-section-title sv-section-title--purple">{{ currentChapterSlides[pptSlideIdx].title }}</h2>
                      <div class="sv-exer-grid">
                        <div v-for="(pt,pi) in (currentChapterSlides[pptSlideIdx].points||[]).slice(0,4)" :key="pi" class="sv-exer-card" :class="pi<2?'sv-exer-card--think':'sv-exer-card--do'">
                          <div class="sv-exer-head">{{ (pi<2?'💬 思考题 ':'✍ 练习题 ')+(pi+1) }}</div>
                          <p class="sv-exer-text">{{ pt }}</p>
                        </div>
                      </div>
                    </template>
                    <template v-else-if="getPptaType(currentChapterSlides[pptSlideIdx])==='summary'">
                      <div class="sv-topbar"></div>
                      <h2 class="sv-section-title">{{ currentChapterSlides[pptSlideIdx].title }}</h2>
                      <div class="sv-summary-cards">
                        <div v-for="(pt,pi) in (currentChapterSlides[pptSlideIdx].points||[]).slice(0,6)" :key="pi" class="sv-summary-card" :class="'sv-summary-card--'+(pi%3)">
                          <div class="sv-summary-card-bar"></div><span class="sv-summary-card-num">0{{ pi+1 }}</span>
                          <p class="sv-summary-card-text">{{ pt }}</p>
                        </div>
                      </div>
                    </template>
                    <template v-else-if="getPptaType(currentChapterSlides[pptSlideIdx])==='end'">
                      <div class="ppta-end-badge">课程结束</div>
                      <h1 class="ppta-cover-title" style="margin-top:20px;color:#fff">{{ currentChapterSlides[pptSlideIdx].title }}</h1>
                      <p class="ppta-cover-sub" style="color:rgba(255,255,255,0.7)">{{ (currentChapterSlides[pptSlideIdx].points||[]).join(' · ') }}</p>
                    </template>
                  </div>
                </div>
                <button class="hub-ppt-nav hub-ppt-next" @click="pptSlideIdx=Math.min(currentChapterSlides.length-1,pptSlideIdx+1)" :disabled="pptSlideIdx>=currentChapterSlides.length-1"><i class="el-icon-arrow-right"></i></button>
              </div>
              <!-- 底部缩略图 -->
              <div class="hub-ppt-thumbstrip">
                <div v-for="(s,idx) in currentChapterSlides" :key="'th'+idx" class="hub-ppt-thumb" :class="{active:idx===pptSlideIdx}" @click="pptSlideIdx=idx">
                  <span>{{ idx+1 }}</span>
                </div>
              </div>
            </div>
            <div v-else class="hub-empty-small">
              <i class="el-icon-presentation"></i><p>课件加载中...</p>
            </div>
          </el-tab-pane>

          <!-- Video Tab -->
          <el-tab-pane label="教学视频" name="video">
            <div v-if="currentCourse.videoList && currentCourse.videoList.length" class="hub-video-grid">
              <div v-for="(v,idx) in currentCourse.videoList" :key="idx" class="hub-video-card" @click="playVideo(v, idx)">
                <div class="hub-video-card-thumb" :style="{background: coverGradient(v.name)}">
                  <div class="hub-video-play-btn"><i class="el-icon-video-play"></i></div>
                  <span v-if="v.url && isExternalUrl(v.url)" class="hub-video-platform-badge">B站</span>
                  <span class="hub-video-duration">{{ v.duration }}</span>
                </div>
                <div class="hub-video-card-info">
                  <h4>{{ v.name }}</h4>
                  <span>{{ v.views || '1.2万' }}次播放</span>
                </div>
              </div>
            </div>
            <div v-else class="hub-empty-small">
              <i class="el-icon-video-camera"></i>
              <p>暂无教学视频</p>
            </div>
          </el-tab-pane>

          <!-- Textbook Tab -->
          <el-tab-pane label="电子课本" name="textbook">
            <div class="hub-resource-tab">
              <!-- 嵌入第三方电子课本网站 -->
              <div class="hub-textbook-embed-section">
                <div class="hub-textbook-embed-header">
                  <div class="hub-textbook-embed-title">
                    <i class="el-icon-reading"></i>
                    <span>在线电子课本</span>
                  </div>
                  <div class="hub-textbook-embed-platforms">
                    <button
                      v-for="p in textbookPlatforms"
                      :key="p.url"
                      :class="['hub-tb-platform-btn', { active: currentTextbookPlatform === p.url }]"
                      @click="currentTextbookPlatform = p.url"
                    >
                      {{ p.name }}
                    </button>
                    <button class="hub-tb-platform-btn hub-tb-open-btn" @click="openTextbookExternal">
                      <i class="el-icon-top-right"></i> 新窗口打开
                    </button>
                  </div>
                </div>
                <div class="hub-textbook-iframe-wrap">
                  <iframe
                    :src="currentTextbookPlatform"
                    class="hub-textbook-iframe"
                    frameborder="0"
                    allowfullscreen
                    referrerpolicy="no-referrer"
                    @load="tbIframeLoaded = true"
                  ></iframe>
                  <div v-if="!tbIframeLoaded" class="hub-textbook-iframe-loading">
                    <i class="el-icon-loading"></i>
                    <p>正在加载电子课本阅读器...</p>
                  </div>
                </div>
              </div>

              <el-divider><span style="color:#94a3b8;font-size:12px">或搜索 / 上传本地课本</span></el-divider>

              <div class="hub-resource-search">
                <div class="hub-resource-search-row">
                  <el-input v-model="tbSearchKeyword" placeholder="粘贴电子课本链接或搜索关键词..." size="medium" clearable @keyup.enter.native="searchThirdTb" class="hub-research-input">
                    <template slot="prepend"><i :class="isUrl(tbSearchKeyword) ? 'el-icon-link' : 'el-icon-search'"></i></template>
                  </el-input>
                  <el-select v-model="tbSearchPlatform" placeholder="平台" size="medium" clearable style="width:140px">
                    <el-option v-for="p in thirdPlatforms" :key="p.domain" :label="p.name" :value="p.name" />
                  </el-select>
                  <button class="hub-btn hub-btn-primary" :class="{loading:tbSearching}" @click="searchThirdTb" :disabled="tbSearching">
                    <i class="el-icon-search"></i> 搜索
                  </button>
                  <button v-if="isUrl(tbSearchKeyword)" class="hub-btn hub-btn-success" :class="{loading:tbFetching}" @click="fetchThirdTb" :disabled="tbFetching">
                    <i class="el-icon-download"></i> 获取
                  </button>
                  <button class="hub-btn hub-btn-outline" @click="$refs.tbUpload.click()">
                    <i class="el-icon-upload2"></i> 上传课本
                  </button>
                  <input type="file" ref="tbUpload" accept=".pdf,.doc,.docx" style="display:none" @change="handleTbUpload" />
                </div>
                <div v-if="tbThirdResults.length" class="hub-search-results">
                  <div v-for="(item,idx) in tbThirdResults" :key="'sr'+idx" class="hub-search-result" @click="fetchThirdItem(item,'textbook')">
                    <span class="hub-search-result-icon">📖</span>
                    <span class="hub-search-result-title">{{ item.title }}</span>
                    <el-tag size="mini">{{ item.platform }}</el-tag>
                    <i class="el-icon-download hub-search-result-action"></i>
                  </div>
                </div>
                <div v-if="tbFetchResult" class="hub-fetch-success">
                  <i class="el-icon-circle-check"></i>
                  <span>已获取: {{ tbFetchResult.fileName }}</span>
                  <button class="hub-btn hub-btn-sm hub-btn-primary" @click="previewThirdFile(tbFetchResult,'textbook')">预览</button>
                  <button class="hub-btn hub-btn-sm hub-btn-success" @click="addThirdToCourse(tbFetchResult,'textbook')">加入课本资源</button>
                </div>
              </div>
              <el-divider v-if="tbSearchKeyword || tbThirdResults.length || tbFetchResult" />
              <div v-if="currentCourse.textbookResources && currentCourse.textbookResources.length" class="hub-textbook-list">
                <div v-for="(tb,idx) in currentCourse.textbookResources" :key="idx" class="hub-textbook-item" @click="openReader(tb)">
                  <div class="hub-textbook-cover" :style="{background: coverGradient(tb.name)}">
                    <i class="el-icon-notebook-2"></i>
                  </div>
                  <div class="hub-textbook-info">
                    <h4>{{ tb.name }}</h4>
                    <p>{{ tb.author }} · {{ tb.publisher }} · {{ tb.edition }}</p>
                  </div>
                  <button class="hub-btn hub-btn-primary hub-btn-sm" @click.stop="openReader(tb)">
                    <i class="el-icon-reading"></i> 在线阅读
                  </button>
                  <button class="hub-btn hub-btn-outline hub-btn-sm" @click.stop="getEmbedCode(currentCourse)" style="margin-left:4px">
                    <i class="el-icon-connection"></i> 嵌入
                  </button>
                </div>
              </div>
              <div v-else class="hub-empty-small">
                <i class="el-icon-notebook-2"></i>
                <p>暂未关联电子课本</p>
                <span>使用上方搜索或上传添加电子课本资源</span>
              </div>
            </div>
          </el-tab-pane>

          <!-- Exercises Tab -->
          <el-tab-pane label="习题练习" name="exercises">
            <div v-if="currentCourse.exercises && currentCourse.exercises.length" class="hub-exercises">
              <div v-for="(ex,idx) in currentCourse.exercises" :key="idx" class="hub-exercise">
                <div class="hub-exercise-num">{{ idx+1 }}</div>
                <div class="hub-exercise-content">
                  <h4>{{ ex.question }}</h4>
                  <div v-if="ex.options" class="hub-exercise-options">
                    <span v-for="(opt,oi) in ex.options" :key="oi" class="hub-exercise-opt">{{ String.fromCharCode(65+oi) }}. {{ opt }}</span>
                  </div>
                  <div v-if="ex.answer" class="hub-exercise-answer">
                    <i class="el-icon-check"></i> {{ ex.answer }}
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="hub-empty-small">
              <i class="el-icon-edit-outline"></i>
              <p>暂无习题</p>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>

    <!-- ═══ PPT Preview Dialog ═══ -->
    <el-dialog :visible.sync="pptPreviewVisible" width="960px" top="2vh" custom-class="hub-preview-dialog" @close="pptPreviewVisible=false;pptExternalIframe='';pptShowWebCard=false;pptExternalPlatform='';pptPreviewLoading=false">
      <div slot="title" class="hub-preview-title">
        <i class="el-icon-presentation"></i>
        <span>{{ pptPreviewTitle }}</span>
      </div>
      <div class="hub-preview-body">
        <div v-if="pptPreviewLoading" class="hub-preview-loading">
          <div class="hub-loading-spinner"></div>
          <p>正在获取资源...</p>
          <span>从远程下载并准备本地查看</span>
        </div>
        <template v-else>
          <div class="hub-preview-toolbar" v-if="!pptExternalIframe && !pptShowWebCard">
            <div class="hub-preview-toolbar-left">
              <button class="hub-btn hub-btn-icon" :disabled="pptSlideIdx===0" @click="pptSlideIdx--"><i class="el-icon-arrow-left"></i></button>
              <span class="hub-preview-page">{{ pptSlideIdx+1 }} / {{ pptPreviewMode === 'auto' ? (currentChapterSlides.length || pptSlidesHtml.length) : pptSlidesHtml.length }}</span>
              <button class="hub-btn hub-btn-icon" :disabled="pptSlideIdx>=(pptPreviewMode==='auto'?(currentChapterSlides.length||pptSlidesHtml.length)-1:pptSlidesHtml.length-1)" @click="pptSlideIdx++"><i class="el-icon-arrow-right"></i></button>
              <el-tag size="mini" effect="plain" v-if="pptPreviewMode==='auto'">共 {{ currentChapterSlides.length }} 页</el-tag>
              <el-tag size="mini" effect="plain" v-else-if="pptPreviewItem">{{ pptPreviewItem.pages }} 页</el-tag>
            </div>
            <div class="hub-preview-toolbar-right">
              <button class="hub-btn hub-btn-sm hub-btn-outline" @click="togglePptFullscreen"><i class="el-icon-full-screen"></i> 全屏</button>
              <button v-if="pptExternalUrl" class="hub-btn hub-btn-sm hub-btn-outline" @click="window.open(pptExternalUrl, '_blank')"><i class="el-icon-link"></i> 原始链接</button>
              <button v-if="pptExternalIframe && pptExternalUrl" class="hub-btn hub-btn-sm hub-btn-outline" @click="pptExternalIframe='';pptShowWebCard=true"><i class="el-icon-back"></i> 返回信息页</button>
            </div>
          </div>
          <!-- PDF/Office embedded view -->
          <iframe v-if="pptExternalIframe" :src="pptExternalIframe" class="hub-preview-iframe" />
          <!-- External link card -->
          <div v-else-if="pptShowWebCard" class="hub-preview-card">
            <div class="hub-preview-card-icon"><i class="el-icon-ppt"></i></div>
            <h3>{{ pptPreviewItem ? pptPreviewItem.name : '课件PPT' }}</h3>
            <p>{{ currentCourse ? currentCourse.name : '' }} · {{ currentCourse ? currentCourse.school : '' }}</p>
            <div class="hub-preview-card-tags">
              <el-tag size="small" type="info">{{ pptExternalPlatform || '第三方平台' }}</el-tag>
              <span>第三方资源，可通过以下方式查看</span>
            </div>
            <div class="hub-preview-card-actions">
              <button class="hub-btn hub-btn-primary" @click="window.open(pptExternalUrl, '_blank')"><i class="el-icon-link"></i> 新窗口打开</button>
              <button class="hub-btn hub-btn-outline" @click="copyToClipboard(pptExternalUrl)"><i class="el-icon-document-copy"></i> 复制链接</button>
              <button v-if="pptExternalUrl" class="hub-btn hub-btn-outline" @click="pptExternalIframe=getPreferredViewerUrl(pptExternalUrl);pptShowWebCard=false;$forceUpdate()"><i class="el-icon-view"></i> 在线查看</button>
            </div>
            <span class="hub-preview-card-hint">提示：该资源托管在第三方平台，如无法直接嵌入请使用上方按钮查看</span>
          </div>
          <!-- Auto-generated slides (ppt-agent quality) -->
          <div v-else-if="pptPreviewMode==='auto' && currentChapterSlides.length" class="hub-auto-slide-view">
            <div class="hub-ppta-slide-card" :class="'hub-ppta-'+getPptaType(currentChapterSlides[pptSlideIdx])">
              <!-- 封面页 -->
              <template v-if="getPptaType(currentChapterSlides[pptSlideIdx])==='cover'">
                <div class="sv-topbar"></div>
                <div class="ppta-cover-badge">课堂教学课件</div>
                <h1 class="ppta-cover-title">{{ currentChapterSlides[pptSlideIdx] ? currentChapterSlides[pptSlideIdx].title : '' }}</h1>
                <p class="ppta-cover-sub" v-if="currentChapterSlides[pptSlideIdx]">{{ (currentChapterSlides[pptSlideIdx].points||[]).join(' · ') }}</p>
                <p class="ppta-cover-meta" v-if="currentChapterSlides[pptSlideIdx] && currentChapterSlides[pptSlideIdx].desc">{{ currentChapterSlides[pptSlideIdx].desc }}</p>
                <span class="sv-footer-tag">封面</span>
              </template>
              <!-- 目标页 -->
              <template v-else-if="getPptaType(currentChapterSlides[pptSlideIdx])==='goal'">
                <div class="sv-topbar"></div>
                <h2 class="sv-section-title">{{ currentChapterSlides[pptSlideIdx] ? currentChapterSlides[pptSlideIdx].title : '' }}</h2>
                <div class="sv-goal-cards">
                  <div v-for="(pt,pi) in (currentChapterSlides[pptSlideIdx]?(currentChapterSlides[pptSlideIdx].points||[]).slice(0,3):[])" :key="pi" class="sv-goal-card" :class="'sv-goal-card--'+pi">
                    <span class="sv-goal-num">0{{ pi+1 }}</span><p class="sv-goal-text">{{ pt }}</p>
                  </div>
                </div>
                <span class="sv-footer-tag">学习目标</span>
              </template>
              <!-- 目录页 -->
              <template v-else-if="getPptaType(currentChapterSlides[pptSlideIdx])==='toc'">
                <div class="sv-topbar"></div>
                <h2 class="sv-section-title">{{ currentChapterSlides[pptSlideIdx] ? currentChapterSlides[pptSlideIdx].title : '' }}</h2>
                <div class="ppta-toc-grid">
                  <div v-for="(pt,pi) in (currentChapterSlides[pptSlideIdx]?(currentChapterSlides[pptSlideIdx].points||[]):[])" :key="pi" class="ppta-toc-item">
                    <span class="ppta-toc-num">{{ pi+1 }}</span><span>{{ pt }}</span>
                  </div>
                </div>
                <span class="sv-footer-tag">目录</span>
              </template>
              <!-- 知识点/重难点页 (keypoint style) -->
              <template v-else-if="getPptaType(currentChapterSlides[pptSlideIdx])==='keypoint'">
                <div class="sv-topbar sv-topbar--orange"></div>
                <h2 class="sv-section-title sv-section-title--orange">{{ currentChapterSlides[pptSlideIdx] ? currentChapterSlides[pptSlideIdx].title : '' }}</h2>
                <div class="sv-kp-main">
                  <div class="sv-kp-steps">
                    <div v-for="(pt,pi) in (currentChapterSlides[pptSlideIdx]?(currentChapterSlides[pptSlideIdx].points||[]).slice(0,5):[])" :key="pi" class="sv-kp-step">
                      <div class="sv-kp-dot">{{ pi+1 }}</div><p class="sv-kp-text">{{ pt }}</p>
                      <div v-if="pi<Math.min((currentChapterSlides[pptSlideIdx]?currentChapterSlides[pptSlideIdx].points.length:0),5)-1" class="sv-kp-line"></div>
                    </div>
                  </div>
                  <div class="sv-kp-warn" v-if="(currentChapterSlides[pptSlideIdx]?currentChapterSlides[pptSlideIdx].points.length:0)>5">
                    <div class="sv-kp-warn-bar"></div><h4>⚠ 常见误区</h4>
                    <p>{{ currentChapterSlides[pptSlideIdx] ? currentChapterSlides[pptSlideIdx].points[5] : '' }}</p>
                  </div>
                </div>
                <span class="sv-footer-tag sv-footer-tag--orange">重难点突破</span>
              </template>
              <!-- 情境导入页 -->
              <template v-else-if="getPptaType(currentChapterSlides[pptSlideIdx])==='scenario'">
                <div class="sv-topbar sv-topbar--teal"></div>
                <h2 class="sv-section-title sv-section-title--teal">{{ currentChapterSlides[pptSlideIdx] ? currentChapterSlides[pptSlideIdx].title : '' }}</h2>
                <div class="sv-scenario-box">
                  <div class="sv-scenario-bar"></div>
                  <p class="sv-scenario-story" v-if="currentChapterSlides[pptSlideIdx] && currentChapterSlides[pptSlideIdx].points[0]">{{ currentChapterSlides[pptSlideIdx].points[0] }}</p>
                  <p class="sv-scenario-story" v-if="currentChapterSlides[pptSlideIdx] && currentChapterSlides[pptSlideIdx].points[1]">{{ currentChapterSlides[pptSlideIdx].points[1] }}</p>
                </div>
                <span class="sv-footer-tag sv-footer-tag--teal">情境导入</span>
              </template>
              <!-- 练习页 -->
              <template v-else-if="getPptaType(currentChapterSlides[pptSlideIdx])==='exercise'">
                <div class="sv-topbar sv-topbar--purple"></div>
                <h2 class="sv-section-title sv-section-title--purple">{{ currentChapterSlides[pptSlideIdx] ? currentChapterSlides[pptSlideIdx].title : '' }}</h2>
                <div class="sv-exer-grid">
                  <div v-for="(pt,pi) in (currentChapterSlides[pptSlideIdx]?(currentChapterSlides[pptSlideIdx].points||[]).slice(0,4):[])" :key="pi" class="sv-exer-card" :class="pi<2?'sv-exer-card--think':'sv-exer-card--do'">
                    <div class="sv-exer-head">{{ (pi<2?'💬 思考题 ':'✍ 练习题 ')+(pi+1) }}</div>
                    <p class="sv-exer-text">{{ pt }}</p>
                  </div>
                </div>
                <span class="sv-footer-tag sv-footer-tag--purple">思考与练习</span>
              </template>
              <!-- 小结页 -->
              <template v-else-if="getPptaType(currentChapterSlides[pptSlideIdx])==='summary'">
                <div class="sv-topbar"></div>
                <h2 class="sv-section-title">{{ currentChapterSlides[pptSlideIdx] ? currentChapterSlides[pptSlideIdx].title : '' }}</h2>
                <p class="sv-summary-sub">一起来回顾今天学到的知识吧 ✨</p>
                <div class="sv-summary-cards">
                  <div v-for="(pt,pi) in (currentChapterSlides[pptSlideIdx]?(currentChapterSlides[pptSlideIdx].points||[]).slice(0,6):[])" :key="pi" class="sv-summary-card" :class="'sv-summary-card--'+(pi%3)">
                    <div class="sv-summary-card-bar"></div><span class="sv-summary-card-num">0{{ pi+1 }}</span>
                    <p class="sv-summary-card-text">{{ pt }}</p>
                  </div>
                </div>
                <span class="sv-footer-tag">课堂小结</span>
              </template>
              <!-- 结束页 -->
              <template v-else-if="getPptaType(currentChapterSlides[pptSlideIdx])==='end'">
                <div class="ppta-end-badge">课程结束</div>
                <h1 class="ppta-cover-title" style="margin-top:20px">{{ currentChapterSlides[pptSlideIdx] ? currentChapterSlides[pptSlideIdx].title : '' }}</h1>
                <p class="ppta-cover-sub" v-if="currentChapterSlides[pptSlideIdx]">{{ (currentChapterSlides[pptSlideIdx].points||[]).join(' · ') }}</p>
                <span class="sv-footer-tag">结束</span>
              </template>
            </div>
          </div>
          <!-- Local slides -->
          <div v-else class="hub-slide-view" v-html="pptSlidesHtml[pptSlideIdx]"></div>
        </template>
      </div>
    </el-dialog>

    <!-- ═══ PPT 全屏放映 ═══ -->
    <div v-if="pptFullscreen" class="hub-ppt-fullscreen" @keydown.esc="pptFullscreen=false" tabindex="0">
      <div class="hub-ppt-fullscreen-bar">
        <span>{{ currentCourse ? currentCourse.name : '' }} · 课件放映</span>
        <span style="margin-left:16px;opacity:0.7">{{ pptFullscreenIdx + 1 }} / {{ pptPreviewMode==='auto' ? currentChapterSlides.length : pptSlidesHtml.length }}</span>
        <div style="flex:1"></div>
        <button class="hub-btn hub-btn-sm" @click="pptFullscreenIdx=Math.max(0,pptFullscreenIdx-1)" :disabled="pptFullscreenIdx<=0"><i class="el-icon-arrow-left"></i></button>
        <button class="hub-btn hub-btn-sm" @click="pptFullscreenIdx=Math.min((pptPreviewMode==='auto'?currentChapterSlides.length:pptSlidesHtml.length)-1,pptFullscreenIdx+1)"><i class="el-icon-arrow-right"></i></button>
        <button class="hub-btn hub-btn-sm hub-btn-outline" @click="pptFullscreen=false"><i class="el-icon-close"></i> 退出</button>
      </div>
      <div class="hub-ppt-fullscreen-body">
        <div v-if="pptPreviewMode==='auto' && currentChapterSlides[pptFullscreenIdx]" class="hub-auto-slide-card" style="width:90%;max-width:1100px;min-height:70vh;padding:60px 80px;background:linear-gradient(135deg, #e0f2fe, #f0f9ff);border-radius:20px;box-shadow:0 24px 80px rgba(0,0,0,0.15)">
          <div style="display:inline-block;padding:6px 24px;border-radius:24px;color:#fff;font-size:14px;font-weight:600;background:linear-gradient(135deg,#0ea5e9,#38bdf8);margin-bottom:12px">第 {{ pptFullscreenIdx + 1 }} 页</div>
          <h2 style="font-size:32px;font-weight:700;color:#0c4a6e;margin:20px 0 16px">{{ currentChapterSlides[pptFullscreenIdx].title }}</h2>
          <div style="width:100px;height:4px;border-radius:2px;background:linear-gradient(90deg,#0ea5e9,transparent);margin-bottom:32px"></div>
          <ul style="list-style:none;padding:0;font-size:20px;line-height:2.4;color:#1e40af">
            <li v-for="(p,pi) in (currentChapterSlides[pptFullscreenIdx].points||[])" :key="pi" style="display:flex;align-items:flex-start;gap:14px;padding:10px 0">
              <span style="width:10px;height:10px;border-radius:50%;background:#0ea5e9;margin-top:14px;flex-shrink:0"></span>
              {{ p }}
            </li>
          </ul>
        </div>
        <div v-else class="hub-slide-view" v-html="pptSlidesHtml[pptFullscreenIdx]" style="transform:scale(1.4)"></div>
      </div>
    </div>

    <!-- ═══ Video Player Dialog (unchanged logic, enhanced styling via CSS) ═══ -->
    <el-dialog :visible.sync="videoPlayerVisible" width="880px" top="2vh" custom-class="hub-video-dialog" :before-close="closeVideoPlayer">
      <div slot="title" class="hub-preview-title">
        <i class="el-icon-video-camera"></i>
        <span>{{ videoDialogTitle }}</span>
      </div>
      <div class="hub-video-body">
        <div v-if="videoPlayerVisible && isBilibiliUrl(currentVideoUrl)" class="bilibili-section">
          <BilibiliPlayer ref="bilibiliPlayer" :video-url="currentVideoUrl" :autoplay="false" :quality="80" @ready="onBilibiliReady" @error="onBilibiliError" @fallback-iframe="onBilibiliFallbackIframe" />
        </div>
        <!-- 网易公开课 open.163.com → 后端解析直链后原生播放 -->
        <div v-else-if="videoPlayerVisible && neteasePlayUrl(currentVideoUrl)" class="hub-native-video">
          <video :src="neteasePlayUrl(currentVideoUrl)" controls autoplay preload="metadata" class="video-native-player"></video>
          <div class="hub-video-hint" v-if="videoNativeSrc">{{ videoNativeSrc }}</div>
        </div>
        <div v-else-if="videoPlayerVisible && videoEmbedUrl && !isBilibiliUrl(currentVideoUrl)" class="hub-embed-video">
          <iframe :src="videoEmbedUrl" allowfullscreen="true" allow="autoplay; encrypted-media" scrolling="no" frameborder="0" />
        </div>
        <div v-else-if="videoPlayerVisible" class="mp4-player">
          <div class="mp4-screen" @click="simulateVideoPlay">
            <div class="mp4-lecture-bg">
              <div class="mp4-lecture-header">
                <span class="mp4-uni-badge">{{ currentCourse ? currentCourse.school : '' }}</span>
                <span class="mp4-course-label">{{ currentCourse ? currentCourse.name : '' }}</span>
              </div>
              <div class="mp4-content-area">
                <transition name="mp4-fade" mode="out-in">
                  <div :key="videoContentIdx" class="mp4-content-card">
                    <h3 class="mp4-point-title">{{ videoContents[videoContentIdx] ? videoContents[videoContentIdx].title : '' }}</h3>
                    <div class="mp4-point-detail">{{ videoContents[videoContentIdx] ? videoContents[videoContentIdx].detail : '' }}</div>
                  </div>
                </transition>
              </div>
              <div class="mp4-instructor-bar">
                <div class="mp4-instructor-avatar">{{ currentCourse ? currentCourse.instructor.charAt(0) : '讲' }}</div>
                <span class="mp4-instructor-name">{{ currentCourse ? currentCourse.instructor : '' }} 主讲</span>
              </div>
            </div>
            <div v-if="simVideoPaused" class="mp4-center-play" @click.stop="simulateVideoPlay">
              <i class="el-icon-video-play"></i>
            </div>
          </div>
          <div class="mp4-controls">
            <div class="mp4-ctrl-btn" @click="simulateVideoPlay">
              <i :class="simVideoPaused ? 'el-icon-video-play' : 'el-icon-video-pause'"></i>
            </div>
            <span class="mp4-time-current">{{ formatTime(simVideoElapsed) }}</span>
            <div class="mp4-progress-wrap" ref="mp4Progress" @mousedown="seekStart" @mousemove="seekMove" @mouseup="seekEnd" @mouseleave="seekEnd" @click="seekClick">
              <div class="mp4-progress-bar">
                <div class="mp4-progress-buffered" :style="{width: simVideoBuffered + '%'}"></div>
                <div class="mp4-progress-fill" :style="{width: simVideoProgress + '%'}"></div>
                <div class="mp4-progress-thumb" :style="{left: simVideoProgress + '%'}" @mousedown.stop="thumbDragStart"></div>
              </div>
            </div>
            <span class="mp4-time-total">{{ formatTime(simVideoDuration) }}</span>
            <div class="mp4-ctrl-btn" @click="simVideoMuted=!simVideoMuted">
              <i :class="simVideoMuted ? 'el-icon-close' : 'el-icon-microphone'"></i>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- ═══ Reader Dialog ═══ -->
    <el-dialog :visible.sync="readerVisible" width="92%" top="1vh" custom-class="hub-reader-dialog" @close="readerVisible=false;readerIsExternal=false;readerShowWebCard=false;readerLoading=false">
      <div slot="title" class="hub-preview-title">
        <i class="el-icon-notebook-2"></i>
        <span>📖 {{ readerBook ? readerBook.name : '' }}</span>
        <button v-if="readerExternalUrl" class="hub-btn hub-btn-sm hub-btn-outline" style="margin-left:auto" @click="window.open(readerExternalUrl, '_blank')"><i class="el-icon-link"></i> 原始链接</button>
      </div>
      <div class="hub-reader-body">
        <div v-if="readerLoading" class="hub-preview-loading">
          <div class="hub-loading-spinner"></div>
          <p>正在获取电子课本...</p>
          <span>正在从远程下载并准备本地查看</span>
        </div>
        <iframe v-else-if="!readerLoading && readerVisible && readerIsExternal && !readerShowWebCard" :src="readerIframeUrl" class="hub-reader-iframe" />
        <div v-else-if="!readerLoading && readerVisible && readerShowWebCard" class="hub-preview-card">
          <div class="hub-preview-card-icon"><i class="el-icon-notebook-2"></i></div>
          <h3>{{ readerBook ? readerBook.name : '电子课本' }}</h3>
          <p>{{ readerBook ? (readerBook.author || '') : '' }}{{ readerBook && readerBook.publisher ? ' · ' + readerBook.publisher : '' }}{{ readerBook && readerBook.edition ? ' · ' + readerBook.edition : '' }}</p>
          <div class="hub-preview-card-tags">
            <el-tag size="small" type="warning">{{ readerExternalPlatform || '第三方平台' }}</el-tag>
            <span>第三方资源，可通过以下方式查看</span>
          </div>
          <div class="hub-preview-card-actions">
            <button class="hub-btn hub-btn-primary" @click="window.open(readerExternalUrl, '_blank')"><i class="el-icon-link"></i> 新窗口打开</button>
            <button class="hub-btn hub-btn-outline" @click="copyToClipboard(readerExternalUrl)"><i class="el-icon-document-copy"></i> 复制链接</button>
            <button v-if="readerExternalUrl" class="hub-btn hub-btn-outline" @click="readerIframeUrl=getPreferredViewerUrl(readerExternalUrl);readerShowWebCard=false;readerIsExternal=true;$forceUpdate()"><i class="el-icon-view"></i> 在线查看</button>
          </div>
          <span class="hub-preview-card-hint">提示：该资源托管在第三方平台，如无法直接嵌入请使用上方按钮查看</span>
        </div>
        <div class="reader-layout" v-else-if="!readerLoading && readerVisible && !readerIsExternal && !readerShowWebCard">
          <div class="reader-toc">
            <h4>目录</h4>
            <div v-for="(ch,idx) in readerChapters" :key="idx" :class="['reader-toc-item',{active:readerChapter===idx}]" @click="readerChapter=idx">{{ ch.title }}</div>
          </div>
          <div class="reader-content">
            <div class="reader-body" v-html="currentChapterText"></div>
            <div class="reader-nav">
              <button class="hub-btn hub-btn-outline hub-btn-sm" :disabled="readerChapter===0" @click="readerChapter--"><i class="el-icon-arrow-left"></i> 上一章</button>
              <span class="reader-progress">{{ readerChapter+1 }} / {{ readerChapters.length }}</span>
              <button class="hub-btn hub-btn-outline hub-btn-sm" :disabled="readerChapter>=readerChapters.length-1" @click="readerChapter++">下一章 <i class="el-icon-arrow-right"></i></button>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- ═══ Import Dialog ═══ -->
    <el-dialog :visible.sync="showImportDialog" width="700px" top="5vh" custom-class="hub-import-dialog" @close="resetImportForm">
      <div slot="title" class="hub-preview-title">
        <i class="el-icon-plus"></i>
        <span>导入课程资源</span>
      </div>
      <el-tabs v-model="importTab" class="hub-import-tabs">
        <el-tab-pane label="从URL导入" name="url">
          <div class="hub-import-form">
            <el-form label-width="100px" size="small">
              <el-form-item label="课程URL">
                <el-input v-model="importUrl" placeholder="输入课程网页地址（如中国大学MOOC、学银在线等）" clearable />
              </el-form-item>
              <el-form-item label="课程名称">
                <el-input v-model="importName" placeholder="可选，自动从页面抓取" clearable />
              </el-form-item>
              <el-form-item label="所属分类">
                <el-select v-model="importCategory" placeholder="选择分类" style="width:100%">
                  <el-option v-for="cat in availableCategories" :key="cat" :label="cat" :value="cat" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <button class="hub-btn hub-btn-primary" :class="{loading:importing}" @click="doImportFromUrl" :disabled="importing">
                  <i class="el-icon-download"></i> {{ importing ? '导入中...' : '开始导入' }}
                </button>
                <span class="hub-import-hint">系统将自动抓取课程元数据</span>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
        <el-tab-pane label="手动添加" name="manual">
          <div class="hub-import-form">
            <el-form label-width="100px" size="small">
              <el-form-item label="课程名称"><el-input v-model="manualCourse.name" placeholder="输入课程名称" /></el-form-item>
              <el-form-item label="学校"><el-input v-model="manualCourse.school" placeholder="如：北京大学" /></el-form-item>
              <el-form-item label="主讲教师"><el-input v-model="manualCourse.instructor" placeholder="授课教师姓名" /></el-form-item>
              <el-form-item label="分类">
                <el-select v-model="manualCourse.category" placeholder="选择分类" style="width:100%">
                  <el-option v-for="cat in availableCategories" :key="cat" :label="cat" :value="cat" />
                </el-select>
              </el-form-item>
              <el-form-item label="学分"><el-input-number v-model="manualCourse.credit" :min="1" :max="10" /></el-form-item>
              <el-form-item label="课程简介">
                <el-input v-model="manualCourse.description" type="textarea" :rows="3" placeholder="课程简介" />
              </el-form-item>
              <el-form-item label="教材"><el-input v-model="manualCourse.textbook" placeholder="参考教材名称" /></el-form-item>
              <el-form-item>
                <button class="hub-btn hub-btn-primary" :class="{loading:importing}" @click="doImportManual" :disabled="importing">
                  <i class="el-icon-plus"></i> 添加课程
                </button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- ═══ 嵌入代码弹窗 ═══ -->
    <el-dialog :visible.sync="embedDialogVisible" title="获取嵌入代码" width="560px" append-to-body :close-on-click-modal="true">
      <div style="margin-bottom:16px">
        <p style="font-size:13px;color:#64748b;margin-bottom:10px">将以下代码复制到第三方教学平台的HTML编辑器中，即可嵌入课程资源：</p>
        <div style="background:#1e293b;border-radius:8px;padding:14px;position:relative">
          <pre style="color:#e2e8f0;font-size:12px;line-height:1.6;margin:0;white-space:pre-wrap;word-break:break-all;font-family:'JetBrains Mono',monospace">{{ embedCode || '加载中...' }}</pre>
        </div>
        <div style="display:flex;gap:8px;margin-top:12px">
          <el-button type="primary" size="small" icon="el-icon-document-copy" @click="copyEmbedCode">复制代码</el-button>
          <el-button size="small" icon="el-icon-view" @click="previewEmbed">新窗口预览</el-button>
        </div>
      </div>
      <div v-if="embedInfo" style="background:#f0f4f8;border-radius:8px;padding:12px">
        <p style="font-size:12px;color:#475569;margin:0 0 6px"><strong>📋 嵌入资源列表：</strong></p>
        <p v-if="embedInfo.directUrls && embedInfo.directUrls.ppt && embedInfo.directUrls.ppt.length" style="font-size:12px;color:#64748b;margin:2px 0">
          📊 PPT课件：{{ embedInfo.directUrls.ppt.length }} 个
        </p>
        <p v-if="embedInfo.directUrls && embedInfo.directUrls.textbook && embedInfo.directUrls.textbook.length" style="font-size:12px;color:#64748b;margin:2px 0">
          📖 电子课本：{{ embedInfo.directUrls.textbook.length }} 个
        </p>
      </div>
    </el-dialog>

    <!-- ═══ ResourceEmbedViewer (third-party inline preview) ═══ -->
    <ResourceEmbedViewer ref="embedViewer" />
  </div>
</template>

<script>
/* eslint-disable */
import courseApi from '@/api/courseResource'
import resourceApi from '@/api/resourceGeneration'
import thirdPartyApi from '@/api/thirdPartyResource'
import BilibiliPlayer from './components/BilibiliPlayer.vue'
import ResourceEmbedViewer from './components/ResourceEmbedViewer.vue'
var GRAD=['linear-gradient(135deg,#2A332A,#3A463A)','linear-gradient(135deg,#2d6a4f,#52b788)','linear-gradient(135deg,#6b21a8,#a855f7)','linear-gradient(135deg,#b45309,#f59e0b)','linear-gradient(135deg,#be123c,#f43f5e)','linear-gradient(135deg,#0e7490,#A9F0FF)','linear-gradient(135deg,#CCFF00,#CCFF00)','linear-gradient(135deg,#059669,#34d399)','linear-gradient(135deg,#A8D400,#CCFF00)','linear-gradient(135deg,#7c2d12,#ea580c)','linear-gradient(135deg,#0d9488,#A9F0FF)','linear-gradient(135deg,#A8D400,#CCFF00)']
function H(s){var h=0;for(var i=0;i<(s||'').length;i++)h=s.charCodeAt(i)+((h<<5)-h);return Math.abs(h)}
function G(n){return GRAD[H(n)%GRAD.length]}

// ═══════════ 课程数据（与后端 course-data.json 同源：26 门岗位导向课程） ═══════════
function CD(){return[
['数据结构与算法','计算机科学','浙江大学','陈越',4,'2026春','国家精品课程·MOOC首批上线课程。系统学习线性表、栈队列、树、图等核心数据结构及经典查找排序算法，掌握复杂度分析方法与编程实现。配套PTA在线判题平台。',186500,4.9,128,'ongoing','数据结构（第2版）·高教社',10,72,['绪论','线性表','栈和队列','串与数组','树与二叉树','图','查找','排序','动态存储管理','文件'],'MOOC-DSA-001'],
['操作系统','计算机科学','北京大学','陈向群',4,'2026春','国家精品课程·深入理解进程管理、内存管理、文件系统、I/O设备管理等操作系统核心原理与实现技术。结合Linux和Windows实例讲解内核工作机制。',125600,4.7,96,'ongoing','计算机操作系统（第4版）',9,64,['操作系统引论','进程描述与控制','处理机调度与死锁','存储器管理','虚拟存储器','输入输出系统','文件管理','磁盘存储器管理','操作系统接口'],'MOOC-OS-002'],
['计算机网络','计算机科学','哈尔滨工业大学','李全龙',3,'2026春','国家精品在线开放课程。系统掌握TCP/IP协议栈、网络体系结构、路由算法、传输控制与网络安全等核心技术。配套Cisco Packet Tracer实验。',98000,4.6,85,'ongoing','计算机网络（第8版）·谢希仁',9,56,['概述','物理层','数据链路层','网络层','运输层','应用层','网络安全','无线网络','下一代互联网'],'MOOC-CN-003'],
['数据库系统概论','计算机科学','中国人民大学','王珊',3,'2026春','国家精品课程·国家级教学名师王珊教授主讲。全面学习关系模型、SQL语言、数据库设计方法、事务管理与查询优化技术。',112000,4.5,78,'ongoing','数据库系统概论（第5版）',11,56,['绪论','关系数据库','SQL语言','数据库安全性','完整性','关系数据理论','数据库设计','嵌入式SQL','查询处理与优化','数据库恢复技术','并发控制'],'MOOC-DB-004'],
['计算机组成原理','计算机科学','清华大学','王诚',4,'2026秋','学习计算机硬件系统组成与工作原理，包括运算器、存储器、控制器和I/O系统。配套Logisim数字电路仿真实验。',89000,4.5,72,'ongoing','计算机组成原理（第3版）',8,64,['计算机系统概论','数据表示与运算','存储器层次','指令系统','中央处理器','总线','输入输出系统','并行体系结构'],'MOOC-CO-005'],
['高等数学（上）','数学','同济大学','李教授',5,'2026春','国家级精品课程·同济大学数学系出品。系统学习函数极限、导数与微分、不定积分与定积分，建立微积分基本思想与方法。配套同济《高等数学》第七版教材。',125800,4.8,142,'ongoing','高等数学（第7版）上册',7,96,['函数与极限','导数与微分','微分中值定理与导数应用','不定积分','定积分','定积分的应用','微分方程'],'MOOC-AM1-006'],
['高等数学（下）','数学','同济大学','张教授',5,'2026春','多元函数微积分、曲线曲面积分及无穷级数理论。将一元微积分思想推广到多元，学习大学物理、工程力学必备数学基础。',102300,4.7,118,'ongoing','高等数学（第7版）下册',6,80,['空间解析几何与向量代数','多元函数微分学','重积分','曲线积分与曲面积分','无穷级数','微分方程进阶'],'MOOC-AM2-007'],
['线性代数','数学','清华大学','杨教授',4,'2026春','国家精品课程。系统学习行列式、矩阵理论、向量空间、特征值与二次型等核心代数内容。配套MIT线性代数公开课风格教学。',153200,4.9,105,'ongoing','线性代数（第6版）',6,64,['行列式','矩阵及其运算','向量组的线性相关性','线性方程组','相似矩阵与二次型','线性空间与线性变换'],'MOOC-LA-008'],
['概率论与数理统计','数学','北京大学','刘教授',4,'2026秋','学习概率空间、随机变量分布、数字特征、大数定律与中心极限定理、参数估计与假设检验。理论与实践紧密结合，含Python统计实验。',89300,4.6,88,'upcoming','概率论与数理统计（第4版）',9,64,['概率论基本概念','随机变量及其分布','多维随机变量','数字特征','大数定律与中心极限定理','样本及抽样分布','参数估计','假设检验','回归分析'],'MOOC-PS-009'],
['离散数学','计算机科学','电子科技大学','王教授',4,'2026春','学习数理逻辑、集合论、图论、代数系统四大核心理论。计算机科学的重要数学基础，培养抽象思维和逻辑推理能力。',45600,4.5,68,'ongoing','离散数学（第2版）',8,64,['数理逻辑','集合论','关系与函数','图论基础','树与平面图','代数系统','格与布尔代数','形式语言与自动机'],'MOOC-CS-013'],
['机器学习','计算机科学','清华大学','张教授',4,'2026秋','MOOC明星课程。系统学习监督学习、无监督学习、强化学习等核心机器学习理论与算法，基于Python和Scikit-learn实现。',125000,4.8,95,'upcoming','机器学习（西瓜书）',10,72,['绪论','模型评估与选择','线性模型','决策树','神经网络','支持向量机SVM','贝叶斯分类器','集成学习','聚类','降维与度量学习'],'MOOC-ML-014'],
['软件工程','计算机科学','复旦大学','赵文耘',3,'2026秋','国家精品课程。学习软件生命周期全过程：需求分析、系统设计、编码实现、测试与维护管理。配套UML建模工具实践。',76500,4.4,72,'upcoming','软件工程导论（第6版）',8,48,['软件工程概述','可行性分析','需求分析','总体设计','详细设计','编码实现','软件测试','软件维护'],'MOOC-SE-016'],
['程序设计基础（C语言）','计算机科学','浙江大学','翁恺',4,'2026春','中国大学MOOC零基础学编程第一课。从零开始学习C语言程序设计，掌握结构化编程思想，配套PTA在线判题系统。累计选课超200万人次。',235000,4.9,88,'ongoing','C语言程序设计（第3版）',8,72,['程序设计入门','分支与循环','数组与函数','指针','字符串','结构体与链表','文件操作','综合项目'],'MOOC-C-019'],
['人工智能导论','计算机科学','北京大学','李文新',3,'2026秋','学习AI基本概念与方法：搜索策略、知识表示、机器学习、深度学习基础等。配套Python/AI平台实验。',68500,4.6,58,'upcoming','人工智能：一种现代方法（第3版）',8,48,['AI概述','问题求解与搜索','知识表示与推理','机器学习基础','神经网络与深度学习','自然语言处理','计算机视觉','AI伦理与安全'],'MOOC-AI-020'],
['Python网络爬虫与信息提取','计算机科学','中国农业大学','嵩天',2,'2026春','中国大学MOOC Python系列课程。学习Requests/Scrapy框架、BeautifulSoup解析、动态网页爬取与反爬策略。',152000,4.7,42,'ongoing','Python网络爬虫与信息提取',6,32,['爬虫入门与Requests','信息提取与BeautifulSoup','动态网页与Selenium','Scrapy框架','反爬与策略','数据存储与可视化'],'MOOC-SE-021'],
['深入理解计算机系统','计算机科学','北京大学','陆俊林',4,'2026春','CMU经典教材CS:APP中文配套MOOC。从程序员视角理解计算机系统，掌握内存层次、链接、异常控制流、虚拟内存等核心概念。',38000,4.9,56,'ongoing','深入理解计算机系统（第3版）',8,72,['计算机系统漫游','信息的表示与处理','程序的机器级表示','处理器体系结构','优化程序性能','存储器层次结构','链接','异常控制流'],'MOOC-CN-023'],
['Java程序设计','计算机科学','北京大学','唐大仕',4,'2026春','零基础学习Java面向对象编程。涵盖基础语法、面向对象、异常处理、集合框架、IO流、多线程、网络编程等核心内容。',168000,4.6,76,'ongoing','Java核心技术（第10版）',9,64,['Java入门','面向对象基础','继承与多态','异常处理','集合与泛型','IO流','多线程','网络编程','Java新特性'],'MOOC-JAVA-024'],
['深度学习实战：从原理到工业落地','AI岗位技能','B站 · 动手学深度学习','李沐团队',3,'2026秋','面向算法工程师与转岗人员的深度学习实战课。基于《动手学深度学习》开源教材，覆盖多层网络、CNN、RNN、注意力机制与视觉实战，全部代码可在免费GPU环境运行。',28000,4.8,128,'ongoing','动手学深度学习（d2l.ai 中文版）· 人民邮电出版社',7,64,['深度学习概述与环境搭建','多层感知机与调参基础','卷积神经网络CNN','循环网络与注意力雏形','注意力机制与Transformer','计算机视觉工业案例','优化算法与工程调参'],'CAREER-DL-PRACTICE'],
['大模型应用开发：RAG与Agent实战','大模型应用','DataWhale 开源社区','DataWhale LLM 组',3,'2026秋','面向大模型应用开发岗位与企业内训场景。系统讲解Prompt工程、RAG检索增强生成、多智能体编排与生产化部署，最终独立交付一个带引用溯源的知识问答系统。',28000,4.8,112,'ongoing','LLM应用开发实战（DataWhale开源教程）',6,56,['大模型API与Prompt工程','文本向量化与语义检索','RAG系统构建全流程','Agent与多智能体编排','评测与幻觉防控工程','生产化部署与成本控制'],'CAREER-LLM-APP'],
['提示工程与大模型交互设计','大模型应用','DeepLearning.AI · B站翻译版','Andrew Ng · Isa Fulford',3,'2026秋','吴恩达《ChatGPT Prompt Engineering for Developers》中文社区版。面向需要把大模型嵌入业务流程的工程师与产品人员，掌握迭代式提示开发方法。',28000,4.8,32,'ongoing','ChatGPT Prompt Engineering for Developers（官方Notebooks）',3,16,['提示工程两大原则','四大任务范式','对话机器人与温度参数'],'CAREER-PROMPT-ENG'],
['大模型微调实战：LoRA与QLoRA','大模型应用','Hugging Face Course · B站中文区','HF 社区讲师',3,'2026秋','面向需要在私有领域定制大模型的工程师。从微调路线决策到QLoRA显存压缩实测，完成一次领域SFT并建立前后评测基线，规避灾难性遗忘。',28000,4.8,80,'ongoing','Hugging Face NLP Course（hf.co/learn）· Transformers文档',4,40,['微调路线决策框架','LoRA原理与实现','QLoRA与单卡训练','评测与灾难性遗忘治理'],'CAREER-LLM-FINETUNE'],
['模型部署与推理加速实战','AI岗位技能','B站技术区 · MLOps 讲师团','一线推理优化工程师',3,'2026秋','面向MLOps与推理工程师岗位培训。覆盖ONNX/TensorRT转换链路、INT8/GPTQ量化、vLLM服务化部署与灰度发布规范，对标生产环境P99延迟指标。',28000,4.8,80,'ongoing','ML模型部署实战手册（内部讲义）',5,40,['部署架构与服务化基础','ONNX与TensorRT加速','量化技术实战','vLLM与大模型服务化','发布流程与线上运维'],'CAREER-MODEL-DEPLOY'],
['工业视觉质检实训：YOLO缺陷检测','工业AI','阿里云天池 · B站实训','天池认证讲师',3,'2026秋','面向制造企业质检智能化改造与转岗人员。从相机选型光源布置到YOLO训练、无监督异常检测PatchCore，再到人机对拍验收标准，完整走通一条产线质检项目。',28000,4.8,96,'ongoing','工业机器视觉项目实战讲义',5,48,['产线图像采集基础','缺陷检测数据集构建','YOLO检测模型训练','无监督异常检测','产线部署与验收对拍'],'CAREER-INDUSTRIAL-VISION'],
['设备预测性维护与时序数据分析','工业AI','阿里云天池 · C-MAPSS实战','工业大数据讲师团',3,'2026秋','面向工业互联网岗位的预测性维护实训。从振动信号特征工程到轴承故障特征频率、自编码器健康指标与RUL剩余寿命预测，建立报警精确率优先的落地方法论。',28000,4.8,80,'ongoing','基于深度学习的装备剩余寿命预测（C-MAPSS论文集）',5,40,['工业数据采集与时序基础','振动信号特征工程','健康指标构建','RUL剩余寿命预测','报警治理与维护闭环'],'CAREER-PREDICTIVE-MAINTENANCE'],
['MLOps工程实践：从训练到上线','AI岗位技能','Microsoft Learn · DataWhale','MLOps Community',3,'2026秋','面向AI平台工程师与算法团队的工程化必修课。打通实验追踪、特征商店、CI/CD、模型注册与监控告警全链路，实现模型发布全自动与指标可追溯。',28000,4.8,80,'ongoing','MLOps工程实践指南（Google/Microsoft白皮书）',5,40,['MLOps成熟度模型','特征商店与数据版本','CI/CD for ML','K8s上的模型服务','监控告警与持续演进'],'CAREER-MLOPS'],
['多智能体系统开发实战','大模型应用','B站技术区 · LangGraph中文社区','LLM Ops 讲师',3,'2026秋','对标本系统架构的多智能体开发实训。基于LangGraph实现Supervisor编排、共享态协同、辩论交叉验证与动态决策节点，理解「分析-生成-校验-决策」闭环的工程细节。',28000,4.8,80,'ongoing','LangGraph官方文档 · Multi-Agent设计模式白皮书',4,40,['Agent设计模式总览','共享态协同与依赖编排','辩论交叉验证机制','动态决策与人机协同'],'CAREER-AGENT-DEV']
]}

// ═══════════ 详细课程知识库 ═══════════
var KB={
DSA:{
  intro:'<h3 style="color:#1e293b;font-size:17px">📌 课程概述</h3><p style="line-height:2;text-indent:2em;margin-bottom:16px"><b>《数据结构与算法》</b>是计算机科学与技术专业的核心必修课程，由浙江大学陈越教授主讲。本课程系统讲授常用数据结构（线性表、栈、队列、树、图等）的逻辑结构、存储表示和基本运算，以及经典查找和排序算法的设计思想与性能分析方法。</p><p style="line-height:2;text-indent:2em;margin-bottom:16px">课程以C语言为描述工具，强调<b>理论联系实际</b>。通过大量编程实践，学生将掌握：①如何根据实际问题选择合适的数据结构；②如何分析算法的时间复杂度和空间复杂度；③如何设计高效、可维护的程序。本课程是后续操作系统、数据库、编译原理等课程的先修基础。</p><p style="line-height:2;text-indent:2em;margin-bottom:16px"><b>学习建议：</b>每周4学时课堂学习+4学时上机实践。建议课前预习教材对应章节，课后独立完成编程作业（PTA在线判题平台），期末通过综合项目检验学习成果。</p><h3 style="color:#1e293b;font-size:17px">🎯 学习目标</h3><ul style="line-height:2;color:#475569"><li>掌握线性表、栈、队列、串、数组、树、图等数据结构的逻辑特征与物理实现</li><li>理解各类查找和排序算法的核心思想，能够分析比较不同算法的适用场景</li><li>具备运用数据结构和算法解决实际问题的能力，养成良好的程序设计习惯</li><li>能够使用大O表示法分析算法的时间复杂度和空间复杂度</li></ul>',
  chapters:{
    '绪论':{points:['数据结构的基本概念与术语（数据、数据元素、数据项、数据结构）','算法定义与五大特性（有穷性、确定性、可行性、输入、输出）','时间复杂度分析：大O表示法、最好/最坏/平均情况','空间复杂度分析：原地算法与额外存储空间','抽象数据类型（ADT）的概念与设计方法']},
    '线性表':{points:['线性表的逻辑结构定义与基本运算','顺序表：连续存储、随机存取O(1)、插入删除O(n)','单链表：结点结构（数据域+指针域）、头插法/尾插法建立','循环链表与双向链表的结构特点与实现','线性表的应用：一元多项式相加、Josephus问题']},
    '栈和队列':{points:['栈（LIFO）：顺序栈与链栈的实现','栈的应用：括号匹配、表达式求值（中缀转后缀）、递归实现','队列（FIFO）：循环队列解决假溢出问题','链队列与双端队列（deque）的实现','队列应用：BFS、缓冲区管理、打印机队列模拟']},
    '树与二叉树':{points:['树的基本概念：结点、度、深度、森林','二叉树的性质（第i层最多2^(i-1)个结点、深度为k最多2^k-1个结点）','二叉树的存储：顺序存储与链式存储（二叉链表）','三种遍历方式：先序/中序/后序遍历（递归与非递归）','二叉搜索树BST的查找/插入/删除操作','平衡二叉树（AVL）与Huffman树']},
    '图':{points:['图的基本概念：有向图/无向图、完全图、连通分量','图的存储：邻接矩阵O(V²)与邻接表O(V+E)','图的遍历：深度优先搜索（DFS）与广度优先搜索（BFS）','最小生成树：Prim算法（O(V²)）与Kruskal算法（O(ElogE)）','最短路径：Dijkstra算法（非负权）、Floyd算法（多源）','拓扑排序与关键路径（AOV网与AOE网）']},
    '查找':{points:['顺序查找与二分查找（O(logn)）的实现','二叉排序树的查找分析','平衡二叉树（AVL）的四种旋转调整（LL/RR/LR/RL）','B树与B+树（多路查找树、数据库索引结构）','哈希表：哈希函数设计（除留余数法）、冲突解决（链地址法、开放地址法）']},
    '串与数组':{points:['串的定义与存储结构（定长、堆分配、块链）','模式匹配：BF算法O(n*m)与KMP算法O(n+m)','数组的顺序存储与地址计算公式','特殊矩阵压缩存储：对称矩阵、三角矩阵、稀疏矩阵','三元组与十字链表存储稀疏矩阵']},
      '动态存储管理':{points:['内存分配策略：首次适配、最佳适配、最差适配','边界标识法与伙伴系统（Buddy System）','垃圾回收：引用计数、标记清除、分代收集','malloc/free与内存泄漏检测预防','可利用空间表与存储紧缩']},
      '文件':{points:['文件的逻辑结构与物理结构','顺序文件、索引文件、索引顺序文件','ISAM索引顺序存取方法','散列文件（直接存取）组织方式','多关键字文件：倒排文件与多重表']},
      '排序':{points:['插入排序：直接插入排序、希尔排序','交换排序：冒泡排序、快速排序（分区+递归）','选择排序：简单选择排序、堆排序（建堆+调整）','归并排序：二路归并、递归与非递归实现','基数排序：多关键字排序（LSD/MSD）','各排序算法复杂度对比与稳定性分析']}
  },
  exercises:[{q:'在长度为n的顺序表中插入一个元素的时间复杂度是多少？',opts:['O(1)','O(logn)','O(n)','O(n²)'],ans:'C'},{q:'栈和队列的共同特点是？',opts:['都是先进先出','都是先进后出','只允许在端点处插入和删除元素','没有共同点'],ans:'C'},{q:'在一棵深度为5的完全二叉树中，最多有多少个结点？',opts:['15','31','32','63'],ans:'B'}]
},
AM1:{
  intro:'<h3 style="color:#1e293b;font-size:17px">📌 课程概述</h3><p style="line-height:2;text-indent:2em;margin-bottom:16px"><b>《高等数学（上）》</b>是同济大学数学系倾力打造的国家级精品课程，是理工科各专业最重要的数学基础课。课程系统讲授一元函数微积分学的基本概念、理论和方法，内容涵盖函数极限、导数与微分、不定积分、定积分及其应用、常微分方程初步等核心模块。</p><p style="line-height:2;text-indent:2em;margin-bottom:16px">本课程强调<b>理解概念本质，掌握计算方法，培养数学思维</b>。微积分不仅是后续专业课程（大学物理、工程力学、信号处理等）的数学工具，更是训练逻辑推理和抽象思维能力的重要途径。课程配合同济大学编写的《高等数学》（第七版）国家级规划教材，例题丰富、习题精选。</p><h3 style="color:#1e293b;font-size:17px">🎯 学习目标</h3><ul style="line-height:2;color:#475569"><li>掌握极限的ε-δ定义，理解极限思想在微积分中的核心地位</li><li>熟练运用导数公式和求导法则，理解导数的几何意义与物理意义</li><li>掌握不定积分和定积分的计算方法，理解Newton-Leibniz公式</li><li>能够运用微积分知识解决几何、物理和工程中的实际问题</li></ul>',
  chapters:{
    '函数与极限':{points:['集合与映射的基本概念','函数的定义、表示法与基本性质（单调性、奇偶性、周期性、有界性）','数列极限的ε-N定义与收敛数列的性质','函数极限的ε-δ定义、左右极限、极限存在准则（夹逼准则）','两个重要极限：lim[x→0]sinx/x=1、lim[x→∞](1+1/x)^x=e','无穷小量与无穷大量：阶的比较、等价无穷小替换（x→0: sinx~x, tanx~x, ln(1+x)~x, e^x-1~x）','函数的连续性与间断点分类（可去、跳跃、无穷、振荡）','闭区间上连续函数的性质（有界性、最大最小值、介值定理）']},
    '导数与微分':{points:['导数定义：f\'(x₀)=lim[Δx→0][f(x₀+Δx)-f(x₀)]/Δx','导数的几何意义（切线斜率）与物理意义（瞬时变化率）','基本求导公式：(xⁿ)\'=nxⁿ⁻¹、(eˣ)\'=eˣ、(lnx)\'=1/x、(sinx)\'=cosx','四则运算法则与复合函数链式法则','隐函数求导与参数方程求导','微分的定义：dy=f\'(x)dx、微分的几何意义','高阶导数（二阶导数与加速度、函数的凹凸性）']},
    '微分中值定理与导数应用':{points:['Rolle定理：若f(a)=f(b)∃ξ∈(a,b)使f\'(ξ)=0','Lagrange中值定理：∃ξ使f\'(ξ)=[f(b)-f(a)]/(b-a)','Cauchy中值定理及其与Lagrange定理的关系','L\'Hôpital法则（0/0型和∞/∞型不定式）','函数的单调性判定（f\'(x)>0↗、f\'(x)<0↘）','函数的极值与最值、驻点与不可导点','曲线的凹凸性（f\'\'(x)>0凹向上）与拐点']},
    '不定积分':{points:['原函数的概念与不定积分的定义：∫f(x)dx=F(x)+C','基本积分公式表（幂函数、指数函数、三角函数）','第一类换元积分法（凑微分法）','第二类换元积分法（三角代换、倒代换）','分部积分法：∫udv=uv-∫vdu','有理函数的积分：部分分式分解法']},
    '定积分':{points:['定积分的定义（Riemann和极限）与几何意义','Newton-Leibniz公式：∫[a,b]f(x)dx=F(b)-F(a)','定积分的换元积分法（换元同时改变积分限）','定积分的分部积分法','广义积分（无穷区间和无界函数）的收敛性判别']},
    '定积分的应用':{points:['平面图形的面积（直角坐标与极坐标）','旋转体的体积（圆盘法和柱壳法）','平面曲线的弧长（直角坐标、参数方程、极坐标）','变力沿直线做功的计算','液体压力与质心/形心的计算']},
      '微分方程':{points:['微分方程的基本概念（阶、解、通解、特解）','可分离变量的微分方程及其解法','一阶线性微分方程与常数变易法','齐次型微分方程与可化为齐次的方程','高阶线性微分方程解的结构','常系数齐次线性微分方程的特征根法']}
  },
  exercises:[{q:'lim[x→0](sin3x)/x的值为？',opts:['0','1','3','不存在'],ans:'C'},{q:'函数f(x)=x³-3x的单调递增区间是？',opts:['(-∞,-1)∪(1,+∞)','(-1,1)','(-∞,+∞)','(0,+∞)'],ans:'A'},{q:'∫₀¹x²dx的值为？',opts:['1/4','1/3','1/2','1'],ans:'B'}]
},
OS:{
  intro:'<h3 style="color:#1e293b;font-size:17px">📌 课程概述</h3><p style="line-height:2;text-indent:2em;margin-bottom:16px"><b>《操作系统》</b>是计算机专业的核心必修课程，由北京大学陈向群教授主讲。操作系统是管理计算机硬件资源、控制程序运行、改善人机界面并为应用软件提供支持的系统软件。本课程系统讲授操作系统的设计原理与实现技术。</p><p style="line-height:2;text-indent:2em;margin-bottom:16px">课程以<b>进程管理、存储器管理、文件管理和设备管理</b>四大模块为主线，结合Linux和Windows实例，深入讲解操作系统内核的工作机制。通过理论与实践结合，帮助学生建立对计算机系统软件的完整认知。</p>',
  chapters:{
    '操作系统引论':{points:['操作系统的定义、目标和作用','操作系统的发展历程：手工操作→批处理→分时→实时→网络→分布式','操作系统的基本特征：并发、共享、虚拟、异步','操作系统的五大功能：处理机管理、存储器管理、设备管理、文件管理、用户接口','操作系统内核架构：宏内核（Linux）与微内核（Minix）']},
    '进程描述与控制':{points:['进程的定义与特征、进程控制块（PCB）','进程的三种基本状态（就绪、执行、阻塞）与状态转换','进程创建（fork）与终止（exit）、进程同步的基本概念','线程的引入：内核级线程与用户级线程的比较']},
    '处理机调度与死锁':{points:['三级调度：高级调度（作业）、中级调度（交换）、低级调度（进程）','调度算法：FCFS、SJF、优先级调度、轮转法（RR）、多级反馈队列','死锁的定义与四个必要条件（互斥、请求保持、不可剥夺、循环等待）','死锁预防（破坏必要条件）与避免（银行家算法）','死锁检测（资源分配图）与解除（终止进程、剥夺资源）']},
    '存储器管理':{points:['程序的装入与链接（绝对装入、可重定位装入、动态运行时装入）','连续分配：单一连续、固定分区、动态分区、可重定位分区','离散分配：分页存储管理（页表、地址变换机构）','快表（TLB）与二级/多级页表','分段存储管理（段表、地址变换）','段页式存储管理：先分段、段内再分页']},
    '虚拟存储器':{points:['虚拟存储器的基本概念与局部性原理（时间局部性、空间局部性）','请求分页系统：页表扩展（状态位、访问字段、修改位、外存地址）','页面置换算法：OPT（最佳置换）、FIFO、LRU（最近最久未使用）、CLOCK','Belady异常（FIFO算法可能出现）','请求分段系统与段页式虚拟存储系统']}
  },
  exercises:[{q:'在操作系统中，进程的三种基本状态不包括？',opts:['就绪','执行','阻塞','等待'],ans:'D'},{q:'下面哪种页面置换算法可能产生Belady异常？',opts:['OPT','LRU','FIFO','CLOCK'],ans:'C'},{q:'死锁的四个必要条件中，哪个是可以被破坏来预防死锁的？',opts:['互斥条件','请求和保持条件','不可剥夺条件','循环等待条件'],ans:'B'}]
},
CN:{
  intro:'<h3 style="color:#1e293b;font-size:17px">📌 课程概述</h3><p style="line-height:2;text-indent:2em;margin-bottom:16px"><b>《计算机网络》</b>由哈尔滨工业大学李全龙教授主讲，以互联网TCP/IP体系结构为主线，系统讲授计算机网络各层协议的设计原理、工作过程和性能分析。课程涵盖物理层、数据链路层、网络层、运输层和应用层的核心内容，帮助学生建立对网络体系结构的全面理解。</p>',
  chapters:{
    '概述':{points:['计算机网络的定义与分类（按范围：LAN/MAN/WAN、按拓扑：星型/总线/环型/网状）','网络性能指标：速率、带宽、吞吐量、时延（发送/传播/处理/排队）、RTT','计算机网络体系结构：OSI七层模型vs TCP/IP四层模型','协议与服务的概念：协议数据单元（PDU）、服务访问点（SAP）']},
    '物理层':{points:['物理层的主要任务：确定与传输媒体的接口特性（机械/电气/功能/规程）','数据通信基础：码元、波特率、比特率、信道容量（Nyquist定理/Shannon公式）','传输媒体：双绞线、同轴电缆、光纤（多模/单模）、无线（无线电/微波/红外）','信道复用技术：FDM/TDM/STDM/WDM/CDM']},
    '数据链路层':{points:['数据链路层的主要功能：成帧、差错检测、可靠传输','三种基本问题：封装成帧（字符计数/字符填充/零比特填充）、透明传输','差错检测：奇偶校验、CRC循环冗余检验','点对点协议PPP的组成与工作过程','CSMA/CD协议（以太网）：争用期、截断二进制指数退避算法','MAC地址格式（6字节48位）与以太网帧格式']},
    '网络层':{points:['网络层提供的两种服务：虚电路与数据报','IPv4地址分类（A/B/C/D/E类）、子网掩码、CIDR（无分类编址）','ARP地址解析协议与ICMP网际控制报文协议','路由算法：RIP（距离向量）、OSPF（链路状态Dijkstra）、BGP（路径向量）','IPv6地址格式（128位冒号十六进制）与IPv4到IPv6过渡技术','VPN与NAT网络地址转换']},
    '运输层':{points:['运输层的复用与分用（端口号：熟知/登记/动态）','UDP协议：无连接、尽最大努力交付（首部8字节）','TCP协议：面向连接、可靠传输（首部20字节）','TCP流量控制：滑动窗口协议','TCP拥塞控制：慢开始、拥塞避免、快重传、快恢复','TCP连接管理：三次握手建立连接、四次挥手释放连接']}
  },
  exercises:[{q:'TCP协议通过什么机制实现可靠传输？',opts:['滑动窗口','CSMA/CD','ARP','ICMP'],ans:'A'},{q:'IPv4地址192.168.1.0/24的网络中可用的主机地址数量是？',opts:['256','254','128','512'],ans:'B'},{q:'OSI参考模型的第三层是？',opts:['数据链路层','网络层','运输层','物理层'],ans:'B'}]
},
AM2:{
  intro:'<h3 style="color:#1e293b;font-size:17px">📌 课程概述</h3><p style="line-height:2;text-indent:2em;margin-bottom:16px"><b>《高等数学（下）》</b>是高等数学（上）的延续课程，系统讲授多元函数微积分、曲线曲面积分及无穷级数理论。本课程将一元微积分的思想推广到多元情形，是学习大学物理、工程力学、电磁学等课程的必备数学基础。</p><p style="line-height:2;text-indent:2em;margin-bottom:16px">空间解析几何为多元函数提供了直观的几何背景；多元函数微分学将导数概念推广到偏导数和方向导数；重积分将定积分推广到二维和三维区域；曲线曲面积分将积分推广到曲线和曲面上；无穷级数为函数表示和近似计算提供了强大工具。</p>',
  chapters:{
    '空间解析几何与向量代数':{points:['空间直角坐标系与向量概念：向量的模、方向角、方向余弦','向量的线性运算：加法（三角形法则和平行四边形法则）、数乘','向量的数量积（点积）：a·b=|a||b|cosθ，满足交换律和分配律','向量的向量积（叉积）：a×b的方向由右手法则确定，|a×b|=|a||b|sinθ','平面方程：点法式、一般式Ax+By+Cz+D=0、截距式','空间直线方程：一般方程（两平面交线）、对称式方程、参数方程','二次曲面：椭球面、抛物面、双曲面、锥面的标准方程与图形特征']},
    '多元函数微分学':{points:['多元函数的基本概念：定义域、极限（二重极限）、连续性','偏导数的定义与几何意义：∂f/∂x表示沿x轴方向的变化率','全微分：dz=(∂z/∂x)dx+(∂z/∂y)dy，可微的充分条件（偏导数连续）','多元复合函数求导：链式法则的树形图分析方法','隐函数求导：F(x,y)=0确定y=f(x)的导数公式dy/dx=-F_x/F_y','方向导数：∂f/∂l=f_x cosα+f_y cosβ，梯度grad f=(f_x,f_y)','多元函数的极值：驻点（偏导数为0）、Hessian矩阵判别法（AC-B²）','条件极值与Lagrange乘数法：构造L(x,y,λ)=f(x,y)+λφ(x,y)']},
    '重积分':{points:['二重积分的定义与几何意义：曲顶柱体的体积','二重积分计算：直角坐标下X型区域和Y型区域的积分次序','极坐标下二重积分的计算：x=rcosθ,y=rsinθ,dxdy=rdrdθ','三重积分的定义与直角坐标计算（先一后二、先二后一）','柱坐标下三重积分：dxdydz=rdrdθdz','球坐标下三重积分：dxdydz=r²sinφdrdφdθ','重积分的应用：曲面面积、质心坐标、转动惯量']},
    '曲线积分与曲面积分':{points:['第一类曲线积分（对弧长）：∫_L f(x,y)ds，与方向无关','第二类曲线积分（对坐标）：∫_L Pdx+Qdy，与路径方向有关','Green公式：∮_L Pdx+Qdy=∬_D(∂Q/∂x-∂P/∂y)dxdy','曲线积分与路径无关的条件：∂P/∂y=∂Q/∂x、保守场与势函数','第一类曲面积分（对面积）：∫∫_Σ f(x,y,z)dS','第二类曲面积分（对坐标）：∫∫_Σ Pdydz+Qdzdx+Rdxdy','Gauss公式（散度定理）：∯_Σ F·dS=∭_Ω div F dV','Stokes公式：∮_L F·dr=∬_Σ curl F·dS，是Green公式的三维推广']},
    '无穷级数':{points:['常数项级数的收敛与发散：部分和数列的极限','正项级数审敛法：比较判别法、比值判别法(d\'Alembert)、根值判别法(Cauchy)','交错级数与Leibniz判别法：通项单调递减趋于0则收敛','绝对收敛与条件收敛：绝对收敛级数可以任意重排','幂级数：收敛半径R=lim|a_n/a_{n+1}|、收敛区间(-R,R)','函数展开为幂级数：Taylor级数f(x)=Σ[f^(n)(x₀)/n!](x-x₀)^n','Fourier级数：周期函数展开为三角级数，Dirichlet收敛定理']}
  },
  exercises:[{q:'二重积分∬_D dxdy（D为单位圆）的值为？',opts:['π','2π','π/2','1'],ans:'A'},{q:'级数Σ(1/n²)的敛散性是？',opts:['发散','条件收敛','绝对收敛','无法判断'],ans:'C'},{q:'梯度grad f的方向是？',opts:['函数值变化最快的方向','函数值不变的方向','函数值减小的方向','任意方向'],ans:'A'}]
},
LA:{
  intro:'<h3 style="color:#1e293b;font-size:17px">📌 课程概述</h3><p style="line-height:2;text-indent:2em;margin-bottom:16px"><b>《线性代数》</b>是理工科和经济管理类专业的核心数学基础课。课程系统讲授行列式、矩阵、向量空间、线性方程组、特征值与二次型等核心理论。线性代数是数据科学、机器学习、计算机图形学、量子力学等领域的数学基石。</p><p style="line-height:2;text-indent:2em;margin-bottom:16px">矩阵运算在线性代数中处于核心地位，行列式的值为判断矩阵可逆性提供了工具，向量空间理论揭示了线性结构的本质，特征值分解在数据降维（PCA）中有重要应用。</p>',
  chapters:{
    '行列式':{points:['n阶行列式的定义：取自不同行不同列的n个元素乘积的代数和','行列式的性质：转置不变、互换两行变号、某行乘以k行列式乘k','行列式按行（列）展开：Laplace展开定理','三角行列式和对角行列式的值等于主对角线元素乘积','Vandermonde行列式的计算公式','Cramer法则：用行列式求解n元线性方程组，x_j=D_j/D']},
    '矩阵及其运算':{points:['矩阵的定义与类型：方阵、对角阵、单位阵、对称阵、反对称阵','矩阵的线性运算：加法、数乘及其运算律','矩阵乘法：AB中(i,j)元为A的第i行与B的第j列对应元素乘积之和','矩阵乘法的性质：结合律成立、交换律一般不成立、AB=0不能推出A=0或B=0','矩阵的转置：(A^T)^T=A、(AB)^T=B^T A^T','方阵的幂：A^k=A·A·...·A（k个），A^m A^n=A^(m+n)','逆矩阵：方阵A可逆⇔|A|≠0，A^(-1)=A*/|A|（伴随矩阵法）','初等变换与初等矩阵：用初等行变换求逆矩阵[A|E]→[E|A^(-1)]']},
    '向量组的线性相关性':{points:['n维向量的概念与线性运算','线性组合与线性表示：β可由α₁,...,α_m线性表示⇔方程组有解','线性相关与线性无关的定义与判定：向量组线性相关⇔存在不全为零的系数','向量组线性相关的几何意义：两个向量共线、三个向量共面','向量组的秩：极大无关组所含向量的个数，反映向量组的本质维度','矩阵的秩=行秩=列秩，初等变换不改变矩阵的秩']},
    '线性方程组':{points:['齐次线性方程组Ax=0：有非零解⇔R(A)<n','非齐次线性方程组Ax=b：有解⇔R(A)=R(A,b)','解的结构：齐次通解=基础解系的线性组合；非齐次通解=特解+齐次通解','用初等行变换求解线性方程组（化为行阶梯形和行最简形）']},
    '相似矩阵与二次型':{points:['特征值与特征向量：Ax=λx，特征多项式|A-λE|=0','特征值的性质：特征值之和=迹tr(A)、特征值之积=行列式|A|','相似矩阵：A~B⇔存在可逆P使P^(-1)AP=B，相似矩阵有相同特征值','矩阵对角化：n阶方阵可对角化⇔有n个线性无关的特征向量','实对称矩阵：特征值全为实数、不同特征值的特征向量正交、必可正交对角化','二次型：f=x^T Ax，用正交变换x=Qy化为标准形f=λ₁y₁²+...+λ_ny_n²','正定二次型的判定：特征值全正、顺序主子式全正、合同于单位阵']}
  },
  exercises:[{q:'若3阶方阵A的特征值为1,2,3，则|A|=？',opts:['0','6','1','5'],ans:'B'},{q:'向量组α₁=(1,0,0),α₂=(0,1,0),α₃=(1,1,0)的秩为？',opts:['1','2','3','0'],ans:'B'},{q:'实对称矩阵的不同特征值对应的特征向量之间的关系是？',opts:['线性相关','正交','相等','无关'],ans:'B'}]
},
PH1:{
  intro:'<h3 style="color:#1e293b;font-size:17px">📌 课程概述</h3><p style="line-height:2;text-indent:2em;margin-bottom:16px"><b>《大学物理（上）》</b>是中国科学技术大学面向理工科本科生开设的经典物理基础课程。课程系统讲授经典力学、热学和电磁学的基本原理和实验方法。物理学是自然科学和工程技术的基础，通过本课程的学习，学生不仅掌握基本物理规律，更培养科学思维和建模能力。</p>',
  chapters:{
    '质点运动学':{points:['参考系与坐标系：惯性系与非惯性系的概念','位置矢量、位移、速度、加速度的定义与矢量表示','直线运动：匀变速运动公式v=v₀+at, s=v₀t+½at²','抛体运动：水平匀速+竖直匀变速的叠加','圆周运动：角速度ω、角加速度α、切向加速度a_t=rα、法向加速度a_n=v²/r=rω²','相对运动：绝对速度=相对速度+牵连速度(Galileo变换)']},
    '牛顿运动定律':{points:['牛顿第一定律（惯性定律）：不受外力时物体保持静止或匀速直线运动','牛顿第二定律：F=ma（矢量形式），力的独立作用原理','牛顿第三定律：作用力与反作用力大小相等、方向相反、作用在不同物体上','常见力：重力G=mg、弹性力F=-kx（Hooke定律）、摩擦力f=μN','用牛顿定律解题的步骤：选对象→画力图→建坐标系→列方程→求解']},
    '动量与能量':{points:['动量定理：冲量I=∫Fdt=Δp，动量p=mv','动量守恒定律：系统合外力为零时总动量守恒，是自然界最基本的守恒律之一','功的计算：W=∫F·dr，变力做功需积分','动能定理：合外力做功=动能变化W=½mv₂²-½mv₁²','保守力（重力、弹力、万有引力）做功与路径无关','势能：重力势能E_p=mgh、弹性势能E_p=½kx²、引力势能E_p=-GMm/r','功能原理：W_外+W_非保内=ΔE_m，机械能守恒条件：只有保守内力做功']},
    '刚体转动':{points:['刚体定轴转动的运动学描述：角位移、角速度ω=dθ/dt、角加速度α=dω/dt','转动惯量：I=∫r²dm，反映质量分布对转动的阻碍','平行轴定理：I=I_C+md²，垂直轴定理（薄板）','转动定律：M=Iα（力矩=转动惯量×角加速度）','角动量：L=Iω，角动量定理：∫Mdt=ΔL','角动量守恒：合外力矩为零时角动量守恒（如花样滑冰、天体运动）']}
  },
  exercises:[{q:'一个质点做匀速圆周运动，下列哪个物理量不变？',opts:['速度','加速度','动能','动量'],ans:'C'},{q:'质量为m的物体从高度h处自由落下，落地时速度大小为？',opts:['√(gh)','√(2gh)','2gh','gh/2'],ans:'B'},{q:'两个物体发生完全非弹性碰撞后，下列哪个量守恒？',opts:['动能','动量','速度','以上都不守恒'],ans:'B'}]
},
MIC:{
  intro:'<h3 style="color:#1e293b;font-size:17px">📌 课程概述</h3><p style="line-height:2;text-indent:2em;margin-bottom:16px"><b>《微观经济学》</b>是经济管理类专业的核心基础课。课程研究个体经济单位（消费者、企业）的决策行为以及市场机制如何配置资源。通过本课程的学习，学生将掌握供求分析、消费者理论、厂商理论、市场结构理论等核心内容，培养经济学思维方式。</p>',
  chapters:{
    '绪论':{points:['经济学的定义：研究稀缺资源在竞争性用途之间配置的科学','微观经济学vs宏观经济学：个体决策vs总量分析','实证经济学（是什么）vs规范经济学（应该是什么）','生产可能性边界（PPF）：表示在给定资源和技术条件下能生产的最大产出组合','机会成本：为获得某物而放弃的最大价值','边际分析：理性人考虑边际量（边际收益=边际成本时最优）']},
    '供求理论':{points:['需求定律：价格上升→需求量下降（需求曲线向右下方倾斜）','供给定律：价格上升→供给量增加（供给曲线向右上方倾斜）','市场均衡：需求=供给时达到均衡价格P*和均衡数量Q*','需求变动vs需求量变动：前者是曲线平移（收入/偏好变化），后者是沿曲线移动','供给变动vs供给量变动：前者是曲线平移（技术/成本变化），后者是沿曲线移动','供求定理：需求增加→P↑Q↑；供给增加→P↓Q↑']},
    '弹性理论':{points:['需求价格弹性：E_d=|(ΔQ/Q)/(ΔP/P)|，衡量需求量对价格的敏感度','弹性分类：E_d>1富有弹性（奢侈品）、E_d<1缺乏弹性（必需品）、E_d=1单位弹性','弹性与总收益TR=P×Q：降价→E_d>1时TR↑、E_d<1时TR↓','需求收入弹性：正常品（>0）vs劣等品（<0），奢侈品（>1）vs必需品（<1）','需求交叉弹性：替代品（>0）vs互补品（<0）','供给价格弹性及其影响因素：生产周期、产能利用率']},
    '消费者选择理论':{points:['效用：总效用TU与边际效用MU，边际效用递减规律（Gossen第一定律）','无差异曲线：能给消费者带来相同满足程度的商品组合','边际替代率MRS=ΔY/ΔX=MU_X/MU_Y，无差异曲线斜率递减','预算约束线：P_X·X+P_Y·Y=I，斜率=-P_X/P_Y','消费者均衡：MRS=P_X/P_Y即MU_X/P_X=MU_Y/P_Y（等边际原理）','价格消费曲线与需求曲线的推导','收入效应与替代效应：正常品的两种效应同向、劣等品反向']},
    '完全竞争市场':{points:['完全竞争市场的四个条件：大量买者卖者、产品同质、自由进出、信息完全','完全竞争企业的需求曲线：水平线（价格接受者）P=MR=AR','利润最大化条件：MR=MC→P=MC','短期均衡：P>AVC时继续生产、P<AVC时停产（停止营业点）','短期供给曲线：MC曲线在AVC以上的部分','长期均衡：P=LMC=LAC_min，经济利润为零','生产者剩余与消费者剩余、市场效率分析']}
  },
  exercises:[{q:'如果需求曲线向右移动而供给曲线不变，则？',opts:['均衡价格上升，均衡数量增加','均衡价格下降，均衡数量减少','均衡价格上升，均衡数量减少','均衡价格下降，均衡数量增加'],ans:'A'},{q:'消费者均衡的条件是？',opts:['MU_X=MU_Y','MU_X/P_X=MU_Y/P_Y','TU_X=TU_Y','P_X=P_Y'],ans:'B'},{q:'完全竞争企业短期停产的临界点是？',opts:['P<ATC','P<AFC','P<AVC','P<MC'],ans:'C'}]
},
PS:{
  intro:'<h3 style="color:#1e293b;font-size:17px">📌 课程概述</h3><p style="line-height:2;text-indent:2em;margin-bottom:16px"><b>《概率论与数理统计》</b>研究随机现象的统计规律性。概率论部分讲授随机事件及其概率、随机变量分布、数字特征、大数定律等；数理统计部分讲授参数估计、假设检验、方差分析和回归分析等统计推断方法。本课程是数据科学、机器学习、金融工程等领域的数学基础。</p>',
  chapters:{
    '概率论基本概念':{points:['随机试验与样本空间的定义','随机事件的关系与运算：包含、相等、和、积、差、互斥、对立','概率的统计定义（频率稳定性）与古典概型（等可能）','条件概率：P(B|A)=P(AB)/P(A)','乘法公式：P(AB)=P(A)P(B|A)','全概率公式：将复杂事件分解为完备事件组的加权平均','Bayes公式（逆概率公式）：已知结果反推原因的概率','事件的独立性：P(AB)=P(A)P(B)，两两独立与相互独立']},
    '随机变量及其分布':{points:['随机变量的概念：将样本点映射为实数','离散型随机变量的分布律（概率质量函数）P(X=x_k)=p_k','二项分布X~B(n,p)：n重Bernoulli试验中成功的次数','Poisson分布X~P(λ)：描述稀有事件的发生次数，E(X)=D(X)=λ','连续型随机变量的概率密度函数f(x)','均匀分布X~U(a,b)与指数分布X~E(λ)','正态分布X~N(μ,σ²)：概率密度f(x)=1/(√(2π)σ)·exp(-(x-μ)²/(2σ²))','标准正态分布：Z~N(0,1)，标准化Z=(X-μ)/σ']},
    '数字特征':{points:['数学期望E(X)：随机变量的加权平均（重心位置）','方差D(X)=E[(X-E(X))²]：衡量随机变量的离散程度','常见分布的期望与方差：二项E=np,D=npq；正态E=μ,D=σ²','协方差Cov(X,Y)=E[(X-EX)(Y-EY)]','相关系数ρ_XY=Cov(X,Y)/(σ_X·σ_Y)，|ρ|≤1','矩：原点矩α_k=E(X^k)、中心矩μ_k=E[(X-EX)^k]']},
    '大数定律与中心极限定理':{points:['Chebyshev不等式：P(|X-μ|≥ε)≤σ²/ε²','大数定律：大量随机变量的算术平均值趋于稳定','独立同分布中心极限定理：无论总体分布，样本均值近似正态分布','De Moivre-Laplace定理：二项分布的正态近似']},
    '参数估计':{points:['点估计：用样本统计量估计总体参数','矩估计法：用样本矩替代总体矩','极大似然估计（MLE）：选择使观测数据出现概率最大的参数值','估计量的评价标准：无偏性、有效性、一致性','区间估计：给出参数的一个置信区间（如95%置信区间）','正态总体均值的置信区间：σ已知用Z分布、σ未知用t分布']}
  },
  exercises:[{q:'若X~N(0,1)，则P(|X|<1.96)约等于？',opts:['0.90','0.95','0.975','0.99'],ans:'B'},{q:'样本均值的期望E(X̄)等于？',opts:['总体方差σ²','总体均值μ','μ/n','σ²/n'],ans:'B'}]
},
DB:{
  intro:'<h3 style="color:#1e293b;font-size:17px">📌 课程概述</h3><p style="line-height:2;text-indent:2em"><b>《数据库系统概论》</b>系统讲授数据库的基本理论、设计方法和应用技术，是计算机专业核心课程。内容包括关系模型与关系代数、SQL语言、数据库设计（E-R模型与规范化理论）、事务管理（ACID特性、并发控制、恢复技术）。</p>',
  chapters:{
    '关系数据库':{points:['关系模型：用二维表表示实体和联系','关系代数的基本运算：选择σ、投影π、连接⋈、除÷','关系的完整性约束：实体完整性（主键非空）、参照完整性（外键约束）','关系代数等价变换规则：用于查询优化']},
    '标准SQL语言':{points:['SQL的特点：综合统一、高度非过程化、面向集合','数据定义DDL：CREATE TABLE、ALTER TABLE、DROP TABLE','数据查询DQL：SELECT-FROM-WHERE-GROUP BY-HAVING-ORDER BY','连接查询：内连接INNER JOIN、左连接LEFT JOIN、右连接、全连接','嵌套子查询：IN、EXISTS、比较运算符','聚集函数：COUNT、SUM、AVG、MAX、MIN','数据更新：INSERT、UPDATE、DELETE']},
    '数据库设计':{points:['数据库设计的六个阶段：需求分析→概念结构→逻辑结构→物理结构→实施→维护','E-R模型：实体(矩形)、属性(椭圆)、联系(菱形)','E-R图向关系模型的转换：实体→表、1:1/1:N/M:N联系的处理','规范化理论：1NF→2NF→3NF→BCNF→4NF','函数依赖与码：完全函数依赖、部分函数依赖、传递函数依赖']},
    '查询处理优化':{points:['查询处理步骤：解析→检查→优化→执行','查询树与等价变换','代数优化：选择下移、投影下移、连接顺序优化','物理优化：基于代价的优化（选择度估算、代价估算）','索引的作用：B+树索引加速查找']},
    '并发控制':{points:['事务的ACID特性：原子性、一致性、隔离性、持久性','并发操作的问题：丢失修改、不可重复读、读脏数据','封锁：共享锁(S锁)和排他锁(X锁)','封锁协议：一级(防止丢失修改)、二级(防读脏)、三级(可重复读)','两段锁协议(2PL)：保证并发调度的可串行化','死锁的检测（等待图）与预防（一次封锁法、顺序封锁法）']}
  },
  exercises:[{q:'SQL中删除表中所有行的命令是？',opts:['DELETE FROM table','DROP TABLE','TRUNCATE','REMOVE'],ans:'A'},{q:'关系模式R属于3NF意味着？',opts:['消除了部分函数依赖','消除了传递函数依赖','消除了所有冗余','每个属性都不可再分'],ans:'B'}]
},
PH2:{
  intro:'<h3 style="color:#1e293b;font-size:17px">📌 课程概述</h3><p style="line-height:2;text-indent:2em"><b>《大学物理（下）》</b>继续讲授电磁学深化内容、波动光学和近代物理基础。电磁感应是发电机和变压器的物理基础；光的干涉、衍射和偏振揭示了光的波动性质；狭义相对论和量子物理则开启了现代物理学的大门。</p>',
  chapters:{
    '电磁感应与电磁场':{points:['Faraday电磁感应定律：ε=-dΦ/dt','动生电动势：ε=∫(v×B)·dl=Blv','感生电动势与涡旋电场：变化的磁场产生电场','自感现象：ε_L=-L·dI/dt，L=Φ/I','互感：ε_21=-M·dI_1/dt','磁场的能量：W=½LI²，磁能密度w=½B²/μ','位移电流：I_d=ε₀·dΦ_E/dt，Maxwell对Ampere定律的修正','Maxwell方程组的积分形式（四个方程完整描述电磁场）']},
    '光的干涉':{points:['相干条件：频率相同、振动方向相同、相位差恒定','分波阵面法：Young双缝干涉，明纹条件dsinθ=kλ','光程差与相位差的关系：Δφ=2πΔ/λ','分振幅法：薄膜干涉（等倾干涉和等厚干涉）','劈尖干涉：等间距的明暗条纹','Newton环：平凸透镜与平板玻璃的等厚干涉','Michelson干涉仪及其应用（测波长、测折射率）']},
    '光的衍射':{points:['Huygens-Fresnel原理：波前上每点都是次级子波源','单缝Fraunhofer衍射：暗纹条件asinθ=kλ','圆孔衍射与光学仪器的分辨本领：Rayleigh判据θ=1.22λ/D','光栅衍射：光栅方程dsinθ=kλ','光栅光谱：不同波长光的色散','X射线衍射与Bragg公式：2dsinθ=kλ']},
    '光的偏振':{points:['自然光与偏振光的概念','Malus定律：I=I₀cos²θ','Brewster定律：反射光完全偏振时tan i_B=n₂/n₁','双折射现象：o光(寻常光)和e光(非寻常光)','偏振片的起偏和检偏作用','旋光现象及其应用（测糖浓度）']},
    '狭义相对论':{points:['Einstein的两个基本假设：相对性原理和光速不变原理','Lorentz变换（替代Galileo变换）：x\'=γ(x-vt), t\'=γ(t-vx/c²)','同时性的相对性：不同参考系中同时是相对的','时间膨胀：Δt=γΔt₀（运动的时钟变慢）','长度收缩：L=L₀/γ（运动物体在运动方向上变短）','质能方程：E=mc²，静能E₀=m₀c²']}
  },
  exercises:[{q:'Faraday电磁感应定律的数学表达式为？',opts:['ε=-dΦ/dt','ε=Blv','ε=L·dI/dt','ε=∮E·dl'],ans:'A'},{q:'Young双缝实验中，相邻明纹间距与哪个量成正比？',opts:['缝间距d','波长λ','屏距D','缝宽a'],ans:'B'}]
},
MAC:{
  intro:'<h3 style="color:#1e293b;font-size:17px">📌 课程概述</h3><p style="line-height:2;text-indent:2em"><b>《宏观经济学》</b>研究国民经济整体的运行规律和政府宏观调控政策。核心内容包括GDP核算、失业与通货膨胀理论、IS-LM模型、AD-AS模型、财政政策与货币政策、经济增长理论等。</p>',
  chapters:{
    '宏观经济基本指标':{points:['GDP的定义：一国在一定时期内生产的最终产品和服务的市场价值总和','GDP核算的三种方法：支出法(GDP=C+I+G+NX)、收入法、生产法','名义GDP vs 实际GDP：GDP平减指数','CPI(消费者价格指数)与通货膨胀率','失业率=失业人数/劳动力人口×100%','奥肯定律：GDP每下降2%，失业率上升1%']},
    '国民收入决定理论':{points:['均衡产出：总产出=总需求（Y=AD）','消费函数：C=a+bY_d，a为自发消费、b为边际消费倾向MPC','储蓄函数：S=Y_d-C=-a+(1-b)Y_d','乘数效应：投资乘数k_I=1/(1-MPC)=1/MPS','政府支出乘数、税收乘数（负值）','平衡预算乘数等于1']},
    'IS-LM模型':{points:['IS曲线：产品市场均衡时利率r与收入Y的组合，r↑→I↓→Y↓（向右下倾斜）','LM曲线：货币市场均衡时r与Y的组合，Y↑→L₁↑→r↑（向右上倾斜）','IS-LM均衡：产品市场和货币市场同时均衡的r*和Y*','IS曲线移动：扩张财政(G↑或T↓)→IS右移→r↑Y↑(挤出效应)','LM曲线移动：扩张货币(M↑)→LM右移→r↓Y↑','流动性陷阱：利率极低时LM水平，货币政策无效']},
    'AD-AS模型':{points:['总需求AD曲线：P↑→实际货币供给↓→r↑→I↓→Y↓（向右下倾斜）','AD曲线移动：扩张政策→AD右移','总供给AS曲线：短期AS向右上倾斜（粘性工资模型）、长期AS垂直','AS曲线移动：技术进步/资本增加→AS右移','AD-AS均衡分析经济波动（衰退、过热、滞胀）']},
    '失业与通货膨胀':{points:['失业类型：摩擦性、结构性、周期性失业','自然失业率：充分就业时仍存在的失业','通货膨胀的三种类型：需求拉动型、成本推动型、结构型','Phillips曲线：失业率与通胀率的短期替代关系','预期的Phillips曲线与长期垂直Phillips曲线']}
  },
  exercises:[{q:'GDP平减指数等于？',opts:['名义GDP/实际GDP','实际GDP/名义GDP','CPI/PPI','（名义-实际）/名义'],ans:'A'},{q:'IS曲线描述的是哪个市场的均衡？',opts:['货币市场','产品市场','劳动力市场','外汇市场'],ans:'B'}]
}
}

// ═══════════ 岗位技能课程知识库（KB-CAREER-BEGIN，与 course-data.json 富知识点同步，自动生成） ═══════════
KB["CAREER-DL-PRACTICE"]={
  intro:"<h3 style=\"color:#1e293b;font-size:17px\">📌 课程概述</h3><p style=\"line-height:2;text-indent:2em\">面向算法工程师与转岗人员的深度学习实战课。基于《动手学深度学习》开源教材，覆盖多层网络、CNN、RNN、注意力机制与视觉实战，全部代码可在免费GPU环境运行。</p>",
  chapters:{
   "深度学习概述与环境搭建": [
    "人工智能、机器学习与深度学习的关系：人工智能是让机器具备类人智能的总目标；机器学习用数据驱动方式让计算机自动学习规律；深度学习是机器学习的分支，依靠多层神经网络自动提取特征。三者逐层包含，深度学习凭借大数据与算力突破成为当前AI落地的主流技术路线。",
    "深度学习发展的三次浪潮：1943年M-P神经元与1958年感知机完成第一次萌芽；1986年反向传播算法让多层网络可训练，形成第二次浪潮；2012年AlexNet在ImageNet竞赛夺冠引爆第三次浪潮。GPU算力、大数据与ReLU等算法改进共同促成深度学习全面爆发。",
    "主流深度学习框架对比：PyTorch以动态图与Pythonic风格著称，是科研与教学的首选；TensorFlow 2.x与Keras在工业部署生态成熟；飞桨PaddlePaddle提供完善中文文档与国产化适配。本课程统一使用PyTorch，兼顾易用性与工程迁移能力。",
    "Anaconda+CUDA+PyTorch环境搭建：用Anaconda创建独立虚拟环境隔离项目依赖；确认显卡型号后从PyTorch官网选择匹配的CUDA版本安装GPU版框架；最后用torch.cuda.is_available()验证GPU可用，并用nvidia-smi监控显存占用。"
   ],
   "多层感知机与调参基础": [
    "感知机与多层结构：单层感知机只能解决线性可分问题，异或问题暴露其根本局限；多层感知机通过隐藏层与非线性激活函数获得万能逼近能力。理论上两层网络即可拟合任意连续函数，关键在于如何训练。",
    "激活函数的选择：Sigmoid存在梯度消失且输出非零中心；ReLU计算简单、正区间梯度恒为1，是隐藏层的默认选择。输出层二分类用Sigmoid、多分类用Softmax，激活函数选错会显著拖慢收敛。",
    "过拟合与欠拟合诊断：欠拟合是模型容量不足，训练误差与测试误差都高；过拟合是模型记住了训练集噪声，训练误差低但测试误差高。将训练与验证损失画在同一张曲线上对比即可快速诊断问题类型。",
    "正则化与早停：L2权重衰减限制权重幅度防止过大；Dropout训练时随机丢弃神经元，迫使网络学习冗余表达；早停监控验证集损失，在其开始上升时停止训练。数据增强则是治本手段。",
    "超参数调参流程：学习率是最重要的超参数，通常从1e-3起步按对数尺度搜索；再按batch size、隐藏层宽度、网络深度的顺序逐一调整。务必固定独立验证集，绝不能用测试集调参造成信息泄漏。"
   ],
   "卷积神经网络CNN": [
    "卷积操作的三大特性：局部连接让每个神经元只观察局部感受野；权值共享使同一卷积核在整张图上滑动，参数量骤减；平移不变性让目标无论出现在图像哪个位置都能被检测。三者共同契合图像的空间结构先验。",
    "经典CNN架构演进：LeNet-5确立了卷积+池化+全连接的经典范式；AlexNet引入ReLU与Dropout大幅提升训练稳定性；VGG用堆叠3×3小卷积核加深网络；ResNet提出残差连接解决深层网络退化问题，使上千层网络可训练。",
    "池化与特征图尺寸计算：最大池化保留局部最强响应，平均池化保留整体分布信息。输出尺寸=(输入尺寸-卷积核+2×padding)/步长+1，网络越深通道数越多、空间尺寸越小，形成金字塔式特征层级。",
    "迁移学习训练要点：工业数据集小，从零训练不如用ImageNet预训练的ResNet18或YOLO backbone微调；先冻结骨干只训练新分类头，再解冻全网络用小学习率精调；训练中重点监控混淆矩阵而非只看总准确率。"
   ],
   "循环网络与注意力雏形": [
    "序列数据的建模需求：文本、传感器时序、语音都是变长序列，前后存在依赖关系，全连接与CNN难以处理。RNN通过隐藏状态在时间步之间传递信息，参数跨时间步共享，天然适配序列建模任务。",
    "RNN的梯度消失与爆炸：沿时间反向传播（BPTT）时梯度要连乘多个雅可比矩阵，导致消失或爆炸。梯度爆炸可用梯度裁剪快速处理；梯度消失则必须依靠门控机制解决，这正是LSTM诞生的直接动机。",
    "LSTM的门控设计：遗忘门决定旧记忆的保留比例；输入门控制新信息写入细胞状态；输出门决定当前时刻的隐藏状态输出。细胞状态以加法更新为主干通道，让梯度可以近乎无衰减地长距离流动。",
    "双向与多层RNN：双向LSTM同时编码正反两个方向的上下文信息，适合文本分类等离线任务；多层堆叠可增强表达能力。但实时流式场景只能用单向结构，因为未来信息在推理时尚不可见。"
   ],
   "注意力机制与Transformer": [
    "注意力的核心思想：打破RNN顺序传递的信息瓶颈，让输出直接与输入的所有位置建立加权关联。通过Query与Key做点积计算相关性，经softmax归一化得到注意力权重，再对Value加权求和完成信息聚合。",
    "自注意力与多头机制：自注意力让序列内部每个位置互相观察，一步捕捉任意距离的依赖；多头注意力把向量投影到多个子空间并行计算注意力，不同头分别关注语法、语义、位置等不同模式，最后拼接融合。",
    "Transformer整体架构：编码器由多头注意力、前馈网络、残差连接与层归一化堆叠而成；解码器额外增加掩码注意力防止偷看未来信息；由于注意力本身不含位置概念，需要位置编码显式注入序列顺序。",
    "预训练范式与BERT/GPT：BERT用双向掩码语言模型学习通用表征，擅长理解类任务；GPT用自回归的下一词预测训练，擅长生成类任务。预训练+微调范式正是从Transformer起步，直接催生了今天的大模型时代。"
   ],
   "计算机视觉工业案例": [
    "工业视觉任务图谱：图像分类判断整图类别，目标检测定位目标并输出边界框，语义分割做到像素级标注。工业质检中缺陷往往小且形态不规则，通常需要检测加分割的组合方案才能覆盖完整需求。",
    "迁移学习实战流程：选用ImageNet预训练的ResNet18或YOLO backbone作为起点；第一阶段冻结骨干仅训练新分类头，第二阶段解冻全网用小学习率精调。工业数据少时该流程可比从零训练提升十个百分点以上。",
    "故障诊断的时频图方法：将振动信号经短时傅里叶变换转为时频谱图，把一维信号诊断转化为二维图像分类问题；CNN自动学习纹理与形态特征，比人工设计特征更鲁棒，已在轴承与齿轮箱诊断中广泛落地。",
    "数据不平衡与小样本策略：缺陷样本远少于正常样本时，采用过采样、Focal Loss、Copy-Paste增强等手段缓解失衡。质检场景必须以召回率为核心指标——漏检一件缺陷的代价远高于误检十件良品。"
   ],
   "优化算法与工程调参": [
    "SGD与动量法：SGD按mini-batch梯度更新，噪声大但泛化性能好；动量法累积历史梯度方向，像小球滚坡的惯性一样抑制震荡、加速收敛。工业训练中SGD+动量在视觉任务上依然常驻 baseline。",
    "自适应学习率家族：AdaGrad为稀疏特征累计梯度平方但学习率衰减过快；RMSProp引入指数衰减平均修正了这一问题；Adam结合动量与自适应步长，开箱即用，是深度学习的默认优化器选择。",
    "Adam与AdamW的区别：Adam将L2正则混入梯度，与自适应步长相耦合，权重衰减效果被稀释；AdamW把权重衰减从梯度中解耦出来单独执行，在Transformer类模型上泛化显著更好，是大模型训练的标准配置。",
    "学习率调度策略：StepLR阶梯衰减简单有效；Cosine退火配合warmup是大模型微调的标配；OneCycle在有限epoch内最大化收敛速度。调度时机应以验证集曲线为准，而不是机械执行预设计划。"
   ]
  }
}

KB["CAREER-LLM-APP"]={
  intro:"<h3 style=\"color:#1e293b;font-size:17px\">📌 课程概述</h3><p style=\"line-height:2;text-indent:2em\">面向大模型应用开发岗位与企业内训场景。系统讲解Prompt工程、RAG检索增强生成、多智能体编排与生产化部署，最终独立交付一个带引用溯源的知识问答系统。</p>",
  chapters:{
   "大模型API与Prompt工程": [
    "大模型API调用范式：主流API均为消息数组结构，system设定角色、user承载输入、assistant存储历史回复。temperature控制随机性，max_tokens限制输出长度；流式SSE返回可显著改善交互体验。",
    "Token计费与上下文窗口：一次调用成本等于输入token单价乘以长度加输出token单价乘以长度；中文通常1.5字符左右折合1个token。上下文窗口有限，超长对话需要做截断或摘要压缩策略。",
    "System Prompt设计原则：按角色设定、行为约束、输出格式三段式书写；硬性要求放在前部并明确禁止项；给出少样本示例比形容词堆砌有效得多。prompt要版本化管理，便于回归测试与效果对比。",
    "API工程化要点：使用官方SDK处理重试与超时；429限流错误用指数退避重试；密钥必须放在服务端环境变量，绝不能进入前端代码。同时记录每次调用的prompt版本与token用量，支撑成本核算。"
   ],
   "文本向量化与语义检索": [
    "Embedding的本质：将文本映射为高维稠密向量，语义相近的文本在向量空间中距离更近。它把语义理解问题转化为向量近邻搜索问题，是RAG、推荐、去重等系统的共同技术底座。",
    "相似度度量选择：余弦相似度忽略向量长度只关注方向，是文本检索的首选；点积在高维归一化后与余弦等价且计算更快；欧氏距离对绝对位置敏感，更常用于图像特征匹配场景。",
    "向量数据库对比：Faiss是嵌入式库，适合单机百万级检索；Milvus与Qdrant提供分布式部署与标量过滤能力；Chroma轻量易用适合原型验证。生产选型需综合评估持久化、过滤能力与吞吐要求。",
    "检索质量优化三板斧：混合检索（向量+BM25关键词）兼顾语义与精确匹配；查询改写与扩展提升召回覆盖；用Reranker精排模型对top50候选重排序，可显著提升最终准确率。"
   ],
   "RAG系统构建全流程": [
    "RAG的动机与架构：大模型存在知识截止与幻觉两大问题，RAG先从外部知识库检索相关片段，再让模型基于证据作答。文档加载、切分、向量化、检索、增强生成是标准五步架构。",
    "文档切分策略：固定长度切分简单但容易割裂语义；按标题与段落的结构感知切分能保留完整性。chunk size在300-500 token、重叠50左右是常见起点，需按文档类型实验调整。",
    "增强生成的Prompt设计：把用户问题与检索片段拼装进模板，明确要求仅依据给定资料回答并标注引用来源。引用溯源让答案可验证，是工程上控制幻觉的第一道防线。",
    "RAG效果评估体系：用RAGAS等框架评估忠实度、答案相关性、上下文精确率与召回率四个维度；构建带标准答案的评测集做版本回归。检索质量决定系统上限，生成质量决定下限。"
   ],
   "Agent与多智能体编排": [
    "Agent的构成要素：以LLM为大脑，加上工具调用、记忆与规划能力，构成能自主决策的执行体。ReAct范式让模型交替进行思考与行动，观察结果回填上下文，是Agent的基本工作循环。",
    "Function Calling机制：将函数名称、描述与参数JSON Schema注册给模型；模型输出结构化调用意图，运行时执行后将结果回填对话继续推理。工具描述的质量直接决定调用准确率。",
    "从单Agent到多智能体：单一Agent上下文膨胀后指令遵循能力下降；按职能拆分为规划者、执行者、审查者，通过消息传递协作。LangGraph以状态图显式定义协作拓扑，过程可控、可回溯。",
    "编排框架选型：LangChain生态最全适合快速原型；LangGraph提供图编排与状态持久化适合生产；AutoGen主打多Agent对话协作。框架只是工具，核心仍是任务拆解与工具设计能力。"
   ],
   "评测与幻觉防控工程": [
    "幻觉的成因分类：知识缺失型源于训练数据未覆盖；推理漂移型是多步推理中途偏离；对齐偏差型表现为模型迎合提问而非陈述事实。分清类型才能对症下药。",
    "检索侧防幻觉：提高top-k召回质量、加入时间过滤、对低置信检索结果明确拒答。必须显式允许模型说资料中没有相关信息，否则模型会被迫编造答案。",
    "生成侧约束技术：prompt中要求逐句标注引用来源；输出后用NLI模型做答案与证据的一致性校验；结构化输出配合JSON Schema校验，可拦截大部分格式与内容幻觉。",
    "在线评估双轨制：离线评测集回归+在线抽样人工复核并行；关键业务上线前先跑影子模式对比新旧版本；建立badcase回流机制，持续补充评测集形成飞轮。"
   ],
   "生产化部署与成本控制": [
    "模型服务化基础：vLLM、TGI提供OpenAI兼容接口与连续批处理，吞吐可达原始推理的数倍。生产环境用Docker加K8s部署，配置健康检查与HPA自动扩缩容是标配。",
    "推理成本优化：利用prompt缓存复用相同前缀；输出长度是成本大头，用max_tokens与few-shot示例约束；小模型能胜任的任务绝不上大模型，路由分流可节省一半以上成本。",
    "缓存与降级策略：高频问答对做结果缓存；向量检索结果按文档版本失效更新；大模型超时或异常时降级到小模型或固定话术兜底，可用性优先于效果完整性。",
    "安全合规底线：输入侧过滤敏感词与注入攻击；输出侧接入内容审核；用户数据遵循最小化留存并加密存储。完整留存prompt审计日志，满足合规追溯要求。"
   ]
  }
}

KB["CAREER-PROMPT-ENG"]={
  intro:"<h3 style=\"color:#1e293b;font-size:17px\">📌 课程概述</h3><p style=\"line-height:2;text-indent:2em\">吴恩达《ChatGPT Prompt Engineering for Developers》中文社区版。面向需要把大模型嵌入业务流程的工程师与产品人员，掌握迭代式提示开发方法。</p>",
  chapters:{
   "提示工程两大原则": [
    "原则一：写清晰具体的指令：用分隔符隔离指令与待处理数据，防止提示注入；明确要求结构化输出（JSON、表格）便于下游解析；让模型先检查前提条件是否满足，再执行任务。",
    "原则二：给模型思考时间：把复杂任务拆解为编号步骤指引；让模型先自行完整推导再下结论，而不是直接判断对错。链式思考能显著提升数学与逻辑类任务的准确率。",
    "迭代式prompt开发：第一版prompt必然不完美；遵循分析badcase、补充指令或示例、重新评测的循环。建立固定评测集量化每次修改的效果，避免凭感觉调优。",
    "少样本示例的威力：给出2-5个输入输出示例，模型会自动模仿其格式与风格；示例应覆盖边界情况。示例与真实输入的分布越接近，效果越好。"
   ],
   "四大任务范式": [
    "概括任务：指定摘要的侧重点（面向受众、限定字数、提取要点）远比笼统地说总结一下效果好；处理多篇评论时可分别概括再聚合为一份生产摘要。",
    "推断任务：情感极性、情绪类型、信息抽取等任务零样本即可完成；一条prompt就能替代传统需要标注数据训练的NLP流水线，这是大模型最提效的用法之一。",
    "转换任务：翻译时指定语域与目标受众；语气改写、格式转换（HTML转JSON）、拼写语法纠错都是典型场景。关键是把输入输出的格式契约写清楚。",
    "扩展任务：根据要点生成客服邮件等完整文本；用temperature控制创造性程度；提供写作角色与语气示例，让输出风格稳定可控。"
   ],
   "对话机器人与温度参数": [
    "ChatAPI的对话管理：messages数组天然支持多轮对话；每轮请求需携带完整历史，模型才能理解上下文。长对话要做历史压缩或滑动窗口，控制token成本。",
    "System角色设定人格：system消息定义助手的身份、语气、知识边界与拒绝策略，相当于整个对话的操作系统，其优先级高于用户消息中的相反指令。",
    "温度参数的直观理解：temperature为0时近似贪心解码，输出确定可复现，适合抽取与分类；接近1时输出多样有创造性，适合文案创作。生产环境按任务类型固化温度配置。",
    "对话机器人工程边界：设置最大轮数与token预算防止失控；敏感话题及时转向人工；把机器人定位与拒绝话术写入system，并纳入定期回归测试。"
   ]
  }
}

KB["CAREER-LLM-FINETUNE"]={
  intro:"<h3 style=\"color:#1e293b;font-size:17px\">📌 课程概述</h3><p style=\"line-height:2;text-indent:2em\">面向需要在私有领域定制大模型的工程师。从微调路线决策到QLoRA显存压缩实测，完成一次领域SFT并建立前后评测基线，规避灾难性遗忘。</p>",
  chapters:{
   "微调路线决策框架": [
    "Prompt、RAG与微调的决策链：知识更新频繁用RAG；任务风格或格式需要内化用微调，两者可以叠加。先穷尽prompt优化再考虑微调，两者成本差一个数量级。",
    "全量微调与参数高效微调：全量微调更新全部权重，7B模型需要数十GB显存与完整训练管线；LoRA类PEFT只训练低秩增量，单张24GB卡即可微调7B-13B模型，效果损失极小。",
    "数据质量决定上限：垃圾进垃圾出在微调中尤其明显；一千条高质量、分布一致、去重后的样本往往胜过十万条脏数据。数据清洗与人工抽检环节不可省略。",
    "微调数据格式规范：采用instruction-input-output三元组或messages对话格式；样本需覆盖任务边界与异常case；按8:1:1切分训练验证测试集，严防数据泄漏。"
   ],
   "LoRA原理与实现": [
    "低秩分解的数学直觉：微调时权重变化量ΔW近似低秩，可用两个小矩阵A与B的乘积近似表示，秩r取8到64即可，可训练参数量降至原模型的百分之一以下。",
    "LoRA的实现细节：原权重W全程冻结，前向传播为Wx加BAx；通常只对注意力层的q_proj与v_proj注入LoRA。推理时可将BA合并回W，实现零额外推理延迟。",
    "HuggingFace PEFT实战流程：prepare_model_for_kbit_training预处理基座，LoraConfig配置秩与目标模块，get_peft_model包装模型；配合bitsandbytes加载4bit基座即可单卡训练。",
    "超参数经验值：r=8起步效果已可观，复杂任务可提升到32或64；alpha取2倍r是常见配置；LoRA学习率比全量微调大约一个数量级（1e-4左右）；训练2-3个epoch防止过拟合。"
   ],
   "QLoRA与单卡训练": [
    "NF4量化的原理：4-bit NormalFloat是针对正态分布权重设计的量化格式，信息损失低于均匀INT4；双重量化把量化常数本身也压缩，进一步节省显存开销。",
    "QLoRA的显存账本：7B模型FP16需要约14GB显存，NF4量化后仅约3.5GB；加上LoRA参数、梯度与激活值，单张24GB消费级显卡即可完成7B模型微调，这是QLoRA最大的工程意义。",
    "分页优化器与训练稳定性：paged optimizer在显存峰值时把优化器状态换页到CPU内存避免OOM；梯度检查点用重复计算换取显存节省，两者配合让长序列训练成为可能。",
    "单卡训练实操清单：bitsandbytes加载4bit基座，开启梯度检查点，batch size设1-2配合梯度累积，启用bf16混合精度，最终用peft只保存适配器权重而非完整模型。"
   ],
   "评测与灾难性遗忘治理": [
    "微调效果评测双维度：任务内指标（准确率、ROUGE、人工评分）加上通用能力回归（MMLU等基准抽样）。只看任务指标会发现模型学会了任务、却变笨了其它能力。",
    "灾难性遗忘的机理：微调梯度把原有权重推向新任务最优解，旧任务的知识表征被覆盖。数据分布越窄、学习率越大、训练越久，遗忘越严重。",
    "遗忘治理策略：混入一定比例通用指令数据做正则回放；降低学习率与训练轮数；LoRA天然缓解遗忘因为主干被冻结只动低秩增量；必要时用EWC等正则约束重要参数。",
    "模型卡与发布规范：记录基座版本、数据构成、训练超参、评测结果与已知局限；灰度发布对比线上表现；保留回滚到基座加旧适配器的能力，确保随时可退。"
   ]
  }
}

KB["CAREER-MODEL-DEPLOY"]={
  intro:"<h3 style=\"color:#1e293b;font-size:17px\">📌 课程概述</h3><p style=\"line-height:2;text-indent:2em\">面向MLOps与推理工程师岗位培训。覆盖ONNX/TensorRT转换链路、INT8/GPTQ量化、vLLM服务化部署与灰度发布规范，对标生产环境P99延迟指标。</p>",
  chapters:{
   "部署架构与服务化基础": [
    "从notebook到生产的鸿沟：实验代码假设单用户低并发，生产要面对高并发、低延迟、故障恢复与监控告警。推理服务必须提供标准化API、批处理与资源隔离能力。",
    "推理服务的标准形态：模型封装为HTTP或gRPC接口，OpenAI兼容格式已成事实标准；内部由请求队列、批处理器、推理引擎组成；健康检查与指标端点必不可少。",
    "部署全流程概述：模型导出、容器化、压测定容量、灰度上线、监控迭代五步走。每一步的产物都要版本化管理，确保可复现、可回滚、可审计。",
    "延迟与吞吐的权衡：延迟是单个请求的响应时间，吞吐是单位时间处理总量。批处理提升吞吐但增加排队延迟；在线交互看P99延迟，离线任务看吞吐量。"
   ],
   "ONNX与TensorRT加速": [
    "ONNX的定位：开放中立的模型交换格式，把PyTorch、TF等模型统一为标准计算图表示。一次导出即可在GPU、CPU、移动端多环境推理，解耦训练框架与部署环境。",
    "PyTorch导出ONNX要点：torch.onnx.export需要示例输入做图追踪；用dynamic_axes标明batch与序列长度可变；导出后必须用onnxruntime做一致性校验，误差应控制在千分之一以内。",
    "TensorRT的三大优化：算子融合把卷积、BN、ReLU合并为单一kernel；FP16或INT8量化降低访存开销；kernel自动调优为当前显卡挑选最优实现，综合提速2到5倍。",
    "典型加速流水线：PyTorch训练、ONNX导出、trtexec构建引擎、C++或Python运行时部署。注意引擎与显卡型号绑定，更换显卡必须重建引擎。"
   ],
   "量化技术实战": [
    "量化的基本原理：用更低比特（FP16、INT8、INT4）表示权重与激活，显存与访存量成比例下降。核心是用校准数据确定各层缩放因子，最小化量化误差。",
    "PTQ与QAT两条路线：训练后量化PTQ无需重训练，几十条校准数据即可完成，是首选尝试；量化感知训练QAT在训练中模拟量化误差，精度更高但成本也高，用于PTQ不达标时。",
    "大模型量化实务：GPTQ与AWQ面向4bit权重量化，压缩后精度损失通常在1%以内；KL散度校准比minmax更稳健。先做W4A16权重量化验证，更激进的全量化需要完整评测背书。",
    "压缩组合拳：量化降低比特、剪枝删除冗余通道、蒸馏让小模型学大模型输出，三者可以叠加。工业落地通常是蒸馏出小模型、INT8量化、TensorRT部署的完整链路。"
   ],
   "vLLM与大模型服务化": [
    "KV Cache与显存困境：自回归生成时历史token的KV缓存随长度增长，静态预分配导致大量显存碎片。PagedAttention借鉴虚拟内存分页思想管理KV Cache，碎片率从60-80%降至4%以下。",
    "连续批处理：传统静态batch必须等最慢的请求完成才能开始下一批；vLLM的连续批处理在每个解码步动态插入新请求、移除完成请求，吞吐提升2到4倍，是它成为部署事实标准的关键。",
    "vLLM部署实践：一条命令即可启动OpenAI兼容服务；gpu-memory-utilization控制显存占比，max-model-len限制上下文长度，tensor-parallel-size支持多卡切分大模型。",
    "服务化配套能力：前缀缓存让相同system prompt只计算一次；AWQ、GPTQ量化模型可直接加载进一步省显存；配合Prometheus指标与负载均衡可构建高可用推理集群。"
   ],
   "发布流程与线上运维": [
    "灰度发布策略：按流量百分比从1%、10%、50%到100%逐步放量，全程对比新旧版本核心指标与错误率。出问题秒级回滚是灰度的前提，模型服务必须支持双版本并存。",
    "蓝绿与滚动发布：蓝绿发布双环境一键切换，资源翻倍但回滚最快；滚动发布逐实例替换，节省资源但周期较长。模型大、启动慢的服务更适合蓝绿模式。",
    "线上监控三件套：业务指标（准确率、转化率）、系统指标（QPS、P99、显存）、数据指标（输入分布漂移）。大盘、告警、值班响应形成闭环，任何恶化一小时内可定位。",
    "模型版本管理：模型文件、推理引擎配置、预处理代码三位一体版本化。发布记录写明训练数据快照与评测报告，满足可追溯与审计要求。"
   ]
  }
}

KB["CAREER-INDUSTRIAL-VISION"]={
  intro:"<h3 style=\"color:#1e293b;font-size:17px\">📌 课程概述</h3><p style=\"line-height:2;text-indent:2em\">面向制造企业质检智能化改造与转岗人员。从相机选型光源布置到YOLO训练、无监督异常检测PatchCore，再到人机对拍验收标准，完整走通一条产线质检项目。</p>",
  chapters:{
   "产线图像采集基础": [
    "工业相机与镜头选型：面阵相机适合静态拍照，线阵相机适合传送带连续扫描；分辨率按缺陷最小尺寸除以视野再乘3倍冗余估算；远心镜头消除透视畸变，是尺寸测量的标配。",
    "光源与成像方案：明场、暗场、背光对应不同缺陷类型——划痕用暗场凸显散射，尺寸测量用背光剪影；频闪配合短曝光冻结运动模糊。光源是整套系统里最值得投入的一环。",
    "采集稳定性保障：固定工作距离与曝光参数，锁定相机白平衡与增益；建立每日标准片标定流程。光照漂移是工业视觉系统上线后精度劣化的头号原因。",
    "数据采集规范：每个缺陷类别至少采集300到500张实拍样本，覆盖极端光照与位置变化；原始图像不做压缩存档；标注前完成保密脱敏处理。"
   ],
   "缺陷检测数据集构建": [
    "数据标注规范：统一标注工具与类别定义手册；边界框紧贴缺陷外沿；歧义样本双人标注交叉仲裁。标注质量抽检不合格率应控制在5%以内。",
    "数据增强策略：旋转、翻转、亮度对比度扰动、马赛克增强都可提升泛化能力；但工业图像要注意物理合理性，例如上下翻转可能制造出产线上不存在的缺陷方向。增强参数随配置版本管理。",
    "数据集划分与泄漏防控：按批次或工件划分训练验证测试集，而不是按图片随机划分，防止同一工件不同角度的照片跨集泄漏；类别不平衡时采用分层抽样。",
    "公开数据集冷启动：NEU表面缺陷、PCB缺陷等公开数据集可用于算法预研；但产线域差异大，正式模型必须用自有数据微调，公开数据集只能作为起点。"
   ],
   "YOLO检测模型训练": [
    "YOLO一阶段检测思想：把检测转化为单次前向传播的回归问题，网格直接预测框与类别，速度远超两阶段方法。v8沿用anchor-free设计并采用解耦检测头分离分类与回归。",
    "训练配置要点：输入尺寸640是精度与速度的平衡点；batch size按显存拉满；数据yaml写明训练验证路径与类别；从yolov8s预训练权重起步做迁移学习。",
    "训练过程监控：紧盯mAP50-95与各类别AP；loss不降先检查标注质量与学习率；用训练中的验证图可视化预测框，直观发现漏检模式；早停patience默认50轮。",
    "常见问题排查：小目标漏检就提高输入分辨率并增加P2检测层；类别混淆检查标注一致性与困难样本；过拟合则加强增强、缩小模型规模。实验日志与权重按编号归档。"
   ],
   "无监督异常检测": [
    "无监督范式的动机：缺陷种类无法穷举且新型缺陷不断出现，有监督检测永远缺类别样本。异常检测只用正常样本建模，偏离即异常，天然覆盖未知缺陷类型。",
    "PatchCore方法原理：用预训练网络提取正常样本的局部patch特征建立memory bank；推理时计算测试patch到memory bank的最近邻距离，距离大即为异常，同时输出图像级与像素级异常图。",
    "MVTec AD基准与指标：MVTec AD是工业异常检测的标准评测集；常用指标为图像级与像素级AUROC。PatchCore在该基准上达到99%以上的图像级AUROC，是首选基线方法。",
    "落地要点：正常样本必须覆盖产线所有正常状态，包括换料与光照波动；判定阈值按漏检率倒推设定；定期用新正常样本更新memory bank以应对分布漂移。"
   ],
   "产线部署与验收对拍": [
    "边缘部署方案：NVIDIA Jetson或工控机加TensorRT是主流组合；ONNX导出、FP16引擎构建、C#或C++ SDK集成到产线上位机；帧率需求决定模型规格选择。",
    "产线系统集成：光电开关触发采图、推理、结果经IO或Modbus回传、剔除机构动作，端到端节拍必须小于产线节拍，通常预留20%以上的余量。",
    "验收对拍流程：盲测集与人工质检员对拍至少一周；统计漏检率、误检率、过杀率并与人工基线对比。验收标准要在合同中量化写明，例如漏检率不超过千分之一。",
    "上线后持续运营：建立badcase回流标注机制，按月做增量训练；监控光照与相机健康度；模型与配置变更全部走版本管理，任何调整都可回滚。"
   ]
  }
}

KB["CAREER-PREDICTIVE-MAINTENANCE"]={
  intro:"<h3 style=\"color:#1e293b;font-size:17px\">📌 课程概述</h3><p style=\"line-height:2;text-indent:2em\">面向工业互联网岗位的预测性维护实训。从振动信号特征工程到轴承故障特征频率、自编码器健康指标与RUL剩余寿命预测，建立报警精确率优先的落地方法论。</p>",
  chapters:{
   "工业数据采集与时序基础": [
    "工业时序数据特点：传感器按固定频率采样形成多元时间序列，包含趋势、周期与突发异常三类模式。采样频率须满足奈奎斯特定理，至少覆盖故障特征频率的两倍。",
    "数据采集架构：传感器、边缘网关做预处理与降采样、时序数据库如InfluxDB或TDengine存储、分析平台消费。多源数据融合前必须完成时间戳对齐与时区统一。",
    "时序预处理四步：缺失值用插值补齐或状态标记；异常点用3σ或箱线图识别后按业务判断修正；重采样统一频率；滑动窗口切分是后续特征工程与建模的基础操作。",
    "机理知识先行：转速、负载、工况切换信息必须与振动温度数据同步采集。脱离工况谈数据会得出错误结论，机理标签是数据驱动模型的护栏。"
   ],
   "振动信号特征工程": [
    "振动信号的三大分析域：时域统计量如均值、峰值、峭度反映整体能量与冲击；频域FFT频谱与特征频率用于定位故障部位；时频域小波与包络谱处理非平稳信号。",
    "轴承特征频率计算：根据转速与几何参数计算内外圈、滚动体、保持架的特征频率BPFO、BPFI、BSF、FTF；频谱中特征频率处出现峰值即指向对应部件故障。",
    "包络解调技术：轴承早期故障的周期冲击会调制在高频共振区，Hilbert包络解调后低频故障特征清晰浮现。这是滚动轴承诊断中最经典有效的手段。",
    "特征选择与降维：从数百个候选特征中用相关性分析、递归消除结合机理筛选；PCA压缩共线维度；特征集必须跨设备验证稳定性，避免过拟合到单台设备。"
   ],
   "健康指标构建": [
    "健康指标HI的定义：将多维监测数据融合为单一标量，单调反映设备退化程度。好的HI应具备单调性、趋势性与跨设备一致性三个性质。",
    "基于机理的HI构建：选取对退化敏感的物理特征如振动RMS、油液铁含量，按退化模型归一化融合。优点是可解释性强，缺点是依赖专家经验。",
    "数据驱动的HI构建：用正常数据训练自编码器，重构误差即为健康指标——正常时误差低，退化后误差上升；也可以用健康样本训练分类器输出健康概率。",
    "HI规范化处理：消除转速负载等工况影响做归一化；按首点归一化到0到1区间便于跨设备比较；对HI做平滑去噪后再交给寿命预测模型使用。"
   ],
   "RUL剩余寿命预测": [
    "RUL预测的问题定义：给定当前观测与历史退化轨迹，预测设备距失效的剩余可用时间。它是预测性维护的核心输出，直接决定维护排期计划。",
    "C-MAPSS基准数据集：NASA涡扇发动机仿真数据，包含四个子集与真实RUL标签，是该领域的标准评测集。评价指标常用RMSE与不对称评分函数，晚发现惩罚更重。",
    "三大建模路线：相似度匹配与历史轨迹比对、基于HI的退化曲线拟合外推、LSTM或Transformer端到端深度建模。工业实践常用LSTM配合分阶段RUL标签，clip上限一般取120到130。",
    "不确定性量化：在点预测之外给出置信区间供维护决策参考；用MC Dropout或分位数回归输出P10、P50、P90三档预测。维护排期按保守分位规划更安全。"
   ],
   "报警治理与维护闭环": [
    "报警阈值设计：静态阈值简单但误报多；动态阈值基于历史同期基线自适应调整；预警分注意、警告、危险三级，对应不同响应时效，避免狼来了效应。",
    "报警治理机制：合并同源报警、抑制已确认故障的衍生报警、定期复盘报警响应率与准确率。治理目标是将可行动报警占比提升到90%以上。",
    "从预测到维护闭环：RUL预测结合备件库存与生产计划联合优化维护窗口；通过CMMS或EAM系统自动生成工单形成闭环。预测的价值最终体现在维护成本与非计划停机的下降。",
    "闭环效果度量：跟踪预警命中率、平均提前量、误报率、非计划停机时长、维护成本五个指标。每季度复盘模型表现并增量更新，系统才能越用越准。"
   ]
  }
}

KB["CAREER-MLOPS"]={
  intro:"<h3 style=\"color:#1e293b;font-size:17px\">📌 课程概述</h3><p style=\"line-height:2;text-indent:2em\">面向AI平台工程师与算法团队的工程化必修课。打通实验追踪、特征商店、CI/CD、模型注册与监控告警全链路，实现模型发布全自动与指标可追溯。</p>",
  chapters:{
   "MLOps成熟度模型": [
    "MLOps三级成熟度：Level0手工流程，笔记本训练手动发布，迭代以月计；Level1流水线自动化，训练评估发布由pipeline驱动；Level2 CI/CD全自动化，代码与数据变更自动触发测试与部署。",
    "手动发布的十二个坑：环境不一致、数据无版本、模型无追踪、评测靠肉眼、发布靠人肉拷贝、无回滚能力等。每一项在规模化后都会演变成线上事故的来源。",
    "实验追踪MLflow上手：记录每次训练的超参数、指标、模型工件与数据版本；通过UI横向对比实验确定最优配置。就像git管理代码一样，MLflow管理的是模型侧的版本历史。",
    "模型卡与合规留痕：模型卡写明用途、数据、评测、局限与伦理考量；训练数据的血缘记录满足审计要求。在金融与医疗行业，这些文档是模型上线的硬性门槛。"
   ],
   "特征商店与数据版本": [
    "特征商店解决的两个问题：训练服务偏斜——同一特征离线在线各算一遍结果不一致；特征重复开发——多团队重复造轮子。特征商店统一特征的定义、计算与存储，训练推理读取同一来源。",
    "Feast快速上手：feature repo用Python声明数据源与实体；离线store对接数仓供训练取数，在线store如Redis供低延迟查询；get_historical_features接口保证point-in-time正确性。",
    "数据版本管理DVC：数据文件用git可寻址哈希管理，配合S3或MinIO远程存储。每次实验记录代码commit、数据版本、超参三元组，任何模型都能精确复现。",
    "特征质量监控：用PSI与KS检验检测特征漂移，监控空值率与更新延迟并告警；上游表结构变更必须走评审流程，特征下线要有消费者通知机制。"
   ],
   "CI/CD for ML": [
    "ML的CI/CD与传统差异：测试对象除了代码，还包括数据schema、模型质量与训练管线本身；CD的最终产物不是软件包，而是通过评测门槛的模型版本。",
    "机器学习流水线组成：数据校验、预处理、训练、评估、模型验证、注册发布六个环节，每步产出工件化，步骤间依赖显式声明。Kubeflow Pipelines与TFX是代表框架。",
    "模型评估门禁：测试集指标低于阈值自动阻断发布；与线上当前模型做影子对比；公平性指标与鲁棒性测试纳入CI检查项，不达标一律不放行。",
    "GitHub Actions实践：单元测试与数据契约测试在PR时触发；训练作业在GPU runner或云端触发；评测通过后自动构建镜像推送registry，人工审批后部署生产。"
   ],
   "K8s上的模型服务": [
    "模型服务上K8s的理由：弹性扩缩容应对流量波峰、健康检查自愈、滚动更新零停机、GPU资源池化调度。这些能力自建机房很难达到同等水平。",
    "KServe框架：InferenceService声明式定义predictor、transformer、explainer组件；支持金丝雀流量切分与自动扩缩容，甚至可缩容到零；底层可选用Triton、TorchServe或vLLM运行时。",
    "GPU资源调度要点：GPU通过nvidia.com/gpu资源申请；MIG切分可提升利用率；结合GPU显存与利用率指标设置合理的HPA阈值，同时注意冷启动延迟的权衡。",
    "发布与回滚：金丝雀按5%、25%、100%三阶段放量，以错误率与P99延迟为门禁指标；kubectl rollout undo实现秒级回滚；模型热更新与服务扩缩容解耦设计。"
   ],
   "监控告警与持续演进": [
    "模型监控三层次：基础设施层监控CPU、GPU与延迟；预测层监控输出分布与置信度；效果层监控真实标签回流后的业务指标。三层缺一不可，效果层最滞后也最重要。",
    "数据漂移与概念漂移：数据漂移是输入分布变化，概念漂移是输入输出关系变化。PSI大于0.2触发关注；但检测到漂移不等于要重训，先分析背后的业务原因。",
    "告警治理实践：告警分层分级，P0线上一票否决立即处理，P1次工作日处理，P2周会复盘；每条告警必须附带runbook；定期清理无效告警避免告警疲劳。",
    "持续演进飞轮：badcase回流、标注、增量数据集、定期重训流水线；季度模型健康审查；每次迭代用A/B测试验证收益，形成监控、诊断、重训、验证的完整闭环。"
   ]
  }
}

KB["CAREER-AGENT-DEV"]={
  intro:"<h3 style=\"color:#1e293b;font-size:17px\">📌 课程概述</h3><p style=\"line-height:2;text-indent:2em\">对标本系统架构的多智能体开发实训。基于LangGraph实现Supervisor编排、共享态协同、辩论交叉验证与动态决策节点，理解「分析-生成-校验-决策」闭环的工程细节。</p>",
  chapters:{
   "Agent设计模式总览": [
    "Agent与工作流的分界：工作流是预先编排的固定路径，可控且成本低；Agent由LLM动态决策下一步行动，灵活但成本高。实践建议是能用工作流解决就不要上Agent。",
    "四大基础模式：提示链将任务分步串联每步可校验；路由按意图分流到专用分支；并行化多路生成后投票或汇聚；评估者优化者通过生成加反馈循环迭代质量。",
    "ReAct循环：Thought、Action、Observation三步迭代，让模型边推理边调用工具，每轮把观察结果回填上下文。必须设置最大步数与预算上限，防止死循环烧钱。",
    "模式组合与选型：真实系统是模式的组合——主流程用提示链保证可控，分支节点用路由，关键产出用评估者循环把关。先画出任务状态图，再决定用哪些模式。"
   ],
   "共享态协同与依赖编排": [
    "共享状态的设计：多Agent系统的核心是状态Schema——任务目标、中间产物、消息历史、控制标记。状态即唯一事实源，所有Agent围绕状态读写，避免上下文相互抄写导致失真。",
    "LangGraph状态图机制：StateGraph定义状态类型与节点；节点是读取状态、执行、返回增量更新的函数；边与条件边决定流转方向；reducer定义并行写入如何合并，如消息列表追加。",
    "依赖编排实践：用条件边实现质量不达标回退重做的分支逻辑；checkpointer持久化每步状态，使断点恢复与时间旅行调试成为可能；interrupt节点实现人工审批。",
    "上下文与成本控制：每个节点只传递必要的状态切片而非全量历史；为每个Agent配置独立的system prompt与最小工具集；持续监控token预算防止长图跑飞。"
   ],
   "辩论交叉验证机制": [
    "辩论模式的动机：单模型自我检查容易自己骗自己；多个独立实例以不同视角、温度或角色生成答案后互相质证，能显著暴露推理漏洞，是提升可靠性的有效手段。",
    "多角色辩论实现：辩论者A与B分别独立作答，交换答案后要求反驳对方论据，再由仲裁者综合裁决。每轮辩论内容写入共享状态，保留完整论证链供追溯。",
    "交叉验证的变体：自我一致性对同一问题多次采样投票；Verifier模型单独对答案打分；工具校验用代码执行器验证数学或SQL结果。按错误代价选择验证强度。",
    "成本与收益平衡：辩论使token成本成倍增加，适合医疗、金融、代码上线等高风险决策；低风险任务用采样一致性即可。把验证预算集中花在关键节点。"
   ],
   "动态决策与人机协同": [
    "Human-in-the-loop的定位：对外发送、资金操作、不可逆删除等高风险动作前中断等待人工确认。LangGraph的interrupt_before在指定节点暂停，人工审批后resume继续执行。",
    "动态路由与升级：Agent按置信度决策——高置信自动执行，中等置信降级为简化流程，低置信转人工处理。置信度可来自模型自评、验证器打分或历史统计。",
    "状态持久化与恢复：checkpointer支持内存、SQLite、Postgres等多种后端保存图执行状态；进程重启或人工介入数小时后可从断点继续；异步审批通过webhook唤醒流程。",
    "生产级保障：全链路日志记录每次决策的输入输出与理由，满足审计要求；工具代码在沙箱中执行；为每个Agent设置权限白名单，最小权限与人机协同双保险。"
   ]
  }
}

// ═══════════ KB-CAREER-END ═══════════

// 获取课程知识库内容
function getKB(course, chTitle){
  var k=KB[course._kb]; if(!k) return null
  if(chTitle){
    var chs=k.chapters||{}
    for(var key in chs){
      if(chTitle.indexOf(key)>=0||key.indexOf(chTitle)>=0) return chs[key]
    }
    return null
  }
  return k
}

// ═══════════ 构建课程对象 ═══════════
function buildCourses(){
  var raw=CD()
  return raw.map(function(r,i){
    var c={id:i+1,name:r[0],cat:r[1],school:r[2],instructor:r[3],credit:r[4],term:r[5],description:r[6],studentCount:r[7],rating:r[8],resourceCount:r[9],status:r[10],textbook:r[11],period:r[12],totalHours:r[13],_chTitles:r[14],_kb:r[15]}
    // 从KB中取章节数据用于PPT内容匹配
    var chsFromKB=(KB[r[15]]&&KB[r[15]].chapters)||{}
    c.chapters=chsFromKB
    var kb=getKB(c)
    c.richDesc=kb?kb.intro:'<p style="line-height:2">'+c.description+'</p>'

    var chNames=c._chTitles; var chCount=chNames.length
    c.chapterList=[]
    for(var ci=0;ci<chCount;ci++){
      var chTitle=chNames[ci]
      var chKb=getKB(c,chTitle)
      var detailHtml=''
      if(chKb&&chKb.points){
        detailHtml='<div style="margin-bottom:12px"><strong style="color:#1e293b">📌 本章知识点：</strong></div><ul style="margin:0 0 12px;padding-left:18px;color:#475569;line-height:2">'
        for(var pi=0;pi<chKb.points.length;pi++){detailHtml+='<li>'+chKb.points[pi]+'</li>'}
        detailHtml+='</ul>'
      }
      var lc=2+(ci%3); var lessons=[]
      // B站视频链接目录 — 为部分课程的章节关联真实B站视频
      var bl=["BV1os4y1p7zm","BV1Hk4y187pT","BV1LZ4y1V7Kf","BV1kG411Y7gM","BV1yT411Y7fD","BV1eG4y1M7Mx","BV1bZ4y1Q7vY","BV1pK411R7hY","BV1U64y1S7Nu","BV1a94y1j7Gx","BV1GM4y1W7Vs","BV1Th4y1i7Nr","BV18X4y1s7zK","BV1uJ4m1Y7Bc","BV1C6421f7E2","BV1ob421J7yB","BV12J4m1w7Lq","BV1UJ4m1Y7Ns","BV1F1421D7x3","BV1s1421Q7uU","BV1K1421Q7Uu","BV1cM4m1S7df","BV1br421c7mV","BV1oZ421M7KN"]
      for(var li=0;li<lc;li++){
        var ifVideo=li%3===0
        var idx=ci*lc+li
        var bvUrl=ifVideo&&idx<bl.length?'https://www.bilibili.com/video/'+bl[idx]+'/?p=1':(ifVideo?'https://search.bilibili.com/all?keyword='+encodeURIComponent(c.name+' '+chTitle+' 教学'):'')
        lessons.push({
          name:chTitle+' · '+(ifVideo?'教学视频':'课件PPT'),
          desc:ifVideo?('B站视频讲解 (约'+(18+ci*3+li*2)+'分钟)'):('PPT课件 (共'+(8+ci*5+li*3)+'页)'),
          iconClass:ifVideo?'el-icon-video-play':'el-icon-presentation',
          btnIcon:ifVideo?'el-icon-video-play':'el-icon-view',
          btnText:ifVideo?'B站播放':'查看',
          type:ifVideo?'video':'ppt',
          url:bvUrl,
          _sourceUrl:bvUrl,
          _chapterIdx:ci,
          duration:(15+ci*4+li*6)+':'+('0'+(10+li*17)%60).slice(-2)
        })
      }
      c.chapterList.push({title:chTitle,lessons:lessons,detailHtml:detailHtml})
    }

    // PPT — 按课程章节生成专属PPT（每章一个）
    c.pptList=[]
    if (!c.pptList || c.pptList.length === 0) {
      for(var ci=0; ci<chCount; ci++){
        var chTitle=chNames[ci]||'第'+(ci+1)+'章'
        var chKb=getKB(c,chTitle)
        var pointCount=chKb&&chKb.points?chKb.points.length:3
        var slidePages=4+Math.min(8,pointCount) // 封面+概述+知识点页+小结
        c.pptList.push({
          name:c.name+' · '+'第'+(ci+1)+'章 '+chTitle,
          size:(1.0+pointCount*0.3).toFixed(1)+' MB',
          pages:slidePages,
          _chapterIdx:ci,
          _chTitle:chTitle
        })
      }
    }
    // 视频 — 为有B站链接的章节生成视频条目
    c.videoList=[]
    for(var vi=0;vi<Math.min(6,chCount);vi++){
      var dm=18+vi*7; var ds=(vi*11)%60
      c.videoList.push({name:c.name+' 第'+(vi+1)+'讲 · '+chNames[vi],duration:dm+':'+('0'+ds).slice(-2),views:(1+(vi%3)*0.3).toFixed(1)+'万'})
    }
    // 课本 — 根据课程提供真实第三方资源URL
    c.textbookResources=[]
    // 为热门课程预设公开可预览的第三方电子课本
    var pubTextbooks={
      '计算机科学':{
        '数据结构与算法':[
          {name:'数据结构（C语言版）',author:'严蔚敏',publisher:'人民邮电出版社',edition:'经典教材',url:'https://book.yunzhan365.com/vmla/izyw/mobile/index.html'},
          {name:'Data Structures and Algorithms',author:'Goodrich & Tamassia',publisher:'Wiley',edition:'6th',url:'https://people.cs.vt.edu/~shaffer/Book/C++3e20120102.pdf'}
        ],
        '操作系统':[
          {name:'计算机操作系统（第4版）',author:'汤小丹',publisher:'西安电子科技大学出版社',edition:'最新版',url:'https://book.yunzhan365.com/qwxe/gkjf/mobile/index.html'}
        ],
        '计算机网络':[
          {name:'计算机网络（第8版）',author:'谢希仁',publisher:'电子工业出版社',edition:'最新版',url:'https://book.yunzhan365.com/kybf/uhuq/mobile/index.html'}
        ]
      },
      '数学':{
        '高等数学（上）':[
          {name:'高等数学（第7版）上册',author:'同济大学',publisher:'高等教育出版社',edition:'国家级规划教材',url:'https://book.yunzhan365.com/ghlf/wmvs/mobile/index.html'}
        ],
        '线性代数':[
          {name:'OpenStax Calculus Volume 1',author:'Strang & Herman',publisher:'OpenStax (MIT授权)',edition:'免费公开教材',url:'https://d3bxy9euw4e147.cloudfront.net/oscms-prodcms/media/documents/CalculusVolume1-OP.pdf'}
        ],
        '概率论与数理统计':[
          {name:'Introduction to Probability',author:'Blitzstein & Hwang',publisher:'CRC Press',edition:'2nd',url:'https://drive.google.com/file/d/1VmkAAGOYCTORq1wxSQqy255qLJjTNvBI/view'}
        ]
      },
      '物理':{
        '大学物理（上）':[
          {name:'OpenStax University Physics Volume 1',author:'Moebs, Ling & Sanny',publisher:'OpenStax (Rice University)',edition:'免费公开教材',url:'https://d3bxy9euw4e147.cloudfront.net/oscms-prodcms/media/documents/UniversityPhysicsVolume1-LR.pdf'}
        ]
      },
      '经济学':{
        '微观经济学':[
          {name:'Principles of Microeconomics',author:'Mankiw',publisher:'Cengage',edition:'8th',url:'https://d3bxy9euw4e147.cloudfront.net/oscms-prodcms/media/documents/PrinciplesofMicroeconomics2e-OP.pdf'}
        ]
      }
    }
    var catBooks=pubTextbooks[course.cat]
    if(catBooks&&catBooks[course.name]){
      c.textbookResources=catBooks[course.name]
    }else{
      // 没有预设资源时回退到本地TB数据
      c.textbookResources=[{name:c.textbook||c.name+'教材',author:c.instructor+'等编著',publisher:'高等教育出版社',edition:'最新版',chapters:chCount,_local:true}]
    }
    // 为所有课本项补充必要字段
    for(var tbi=0;tbi<c.textbookResources.length;tbi++){
      var tb=c.textbookResources[tbi]
      if(!tb.chapters) tb.chapters=chCount
      if(!tb._sourceUrl&&tb.url) tb._sourceUrl=tb.url
    }
    // 习题
    c.exercises=(kb&&kb.exercises)?kb.exercises.map(function(e,i){return{question:e.q,options:e.opts,answer:'参考答案：'+e.opts[e.ans.charCodeAt(0)-65]+' ('+e.ans+')'}}):[
      {question:'简述本课程第一章的核心概念',answer:'第一章主要介绍课程的基本概念和理论框架。重点理解核心定义和基本原理，建立完整的知识体系结构。'},
      {question:'举例说明本课程知识在实际中的应用',answer:'本课程知识广泛应用于科研、工程和教育等领域。具体包括：数据分析建模、系统设计与优化、教学策略制定等。'}
    ]
    return c
  })
}

// ═══════════ PPT幻灯片生成（每章专属内容，不重复） ═══════════
function buildPptSlides(pptItem,course){
  var slides=[], grad=G(course.name)
  var chNames=course._chTitles, chCount=chNames.length
  var subjectIcon = {'数学':'📐','物理':'🔬','化学':'⚗️','生物':'🧬','计算机科学':'💻','英语':'📖','经济学':'📊','管理学':'👥','法学':'⚖️','文学':'📚','历史':'🏛️'}
  var icon = subjectIcon[course.cat] || '📝'

  // 确定本PPT对应哪一个章节（三重兜底）
  var chIdx=pptItem._chapterIdx
  // 兜底1：从lesson名称推章节名
  if(chIdx===undefined||chIdx===null||chIdx<0||chIdx>=chCount){
    var lessonName=pptItem.name||''
    for(var ci=0;ci<chCount;ci++){
      if(lessonName.indexOf(chNames[ci])>=0){chIdx=ci;break}
    }
  }
  // 兜底2：取0
  if(chIdx===undefined||chIdx===null||chIdx<0||chIdx>=chCount)chIdx=0
  var chName=chNames[chIdx]||'第'+(chIdx+1)+'章'
  var chKb=getKB(course,chName)
  var chTb=TB[course._kb]&&TB[course._kb][chName]
  var chNum=chIdx+1

  // 1. 封面页 — 课程名 + 章节名
  slides.push('<div style="background:'+grad+';color:#fff;padding:60px 40px;text-align:center;min-height:380px;display:flex;flex-direction:column;justify-content:center;border-radius:12px"><div style="font-size:48px;margin-bottom:12px">'+icon+'</div><h1 style="font-size:26px;font-weight:700;margin:8px 0">'+course.name+'</h1><div style="width:60px;height:3px;background:rgba(255,255,255,0.3);margin:16px auto;border-radius:2px"></div><p style="font-size:18px;opacity:.9;margin:0">第'+chNum+'章  '+chName+'</p><p style="font-size:13px;opacity:.5;margin-top:24px">'+course.school+' · '+course.instructor+'</p></div>')

  // 2. 章节概述页 — 从课程描述+KB中提取
  var descText=course.description||'本章系统介绍'+chName+'的核心理论与方法。'
  slides.push('<div style="background:#fff;padding:32px;min-height:380px;border-radius:12px"><h2 style="font-size:22px;font-weight:600;color:#1e293b;margin:0 0 16px;padding-bottom:8px;border-bottom:3px solid #6366f133">📌 第'+chNum+'章 学习目标</h2><div style="padding:20px;background:linear-gradient(135deg,#eff6ff,#f0f9ff);border-radius:10px;border-left:4px solid #CCFF00;margin-bottom:16px"><p style="line-height:2;color:#475569;font-size:14px;text-indent:2em;margin:0">'+descText+'</p></div><div style="display:grid;grid-template-columns:1fr 1fr;gap:10px"><div style="padding:14px;background:#f8fafc;border-radius:8px;border-left:3px solid #CCFF00"><strong style="color:#1e293b">课程</strong><br><span style="color:#475569;font-size:13px">'+course.name+'</span></div><div style="padding:14px;background:#f8fafc;border-radius:8px;border-left:3px solid #10b981"><strong style="color:#1e293b">章节位置</strong><br><span style="color:#475569;font-size:13px">第'+chNum+'章 / 共'+chCount+'章</span></div><div style="padding:14px;background:#f8fafc;border-radius:8px;border-left:3px solid #f59e0b"><strong style="color:#1e293b">知识点数</strong><br><span style="color:#475569;font-size:13px">'+(chKb&&chKb.points?chKb.points.length:'3+')+'个核心知识点</span></div><div style="padding:14px;background:#f8fafc;border-radius:8px;border-left:3px solid #CCFF00"><strong style="color:#1e293b">建议学时</strong><br><span style="color:#475569;font-size:13px">'+(2+chIdx%3)+' 学时</span></div></div></div>')

  // 3. 知识点详解页 — 每个知识点一页（最多6页）
  if(chKb&&chKb.points&&chKb.points.length>0){
    var pts=chKb.points
    for(var pi=0;pi<Math.min(6,pts.length);pi++){
      var ptText=pts[pi]
      var colonIdx=ptText.indexOf('：'); if(colonIdx<0) colonIdx=ptText.indexOf(':')
      var ptTitle=colonIdx>0?ptText.substring(0,colonIdx).replace(/[：:、，,。·]/g,'').trim():ptText.substring(0,20)
      var ptDetail=colonIdx>0?ptText.substring(colonIdx+1).trim():ptText

      // 根据知识点索引添加不同的示例/说明
      var extraNote=''
      var exNames=['例','典型应用','关键推导','核心公式','图示理解','易混淆点']
      var exContents=[
        '通过一个典型例题加深理解：将'+ptTitle+'应用于实际问题中，分析其求解步骤和注意事项。',
        '在实际工程/科研中，'+ptTitle+'常用于解决'+chName+'相关的复杂问题。',
        '推导思路：从基本定义出发，逐步展开'+ptTitle+'的数学推演过程。',
        '关键公式与性质总结，便于记忆和快速查阅。',
        '通过图表直观展示'+ptTitle+'的变化规律和内在结构。',
        '容易与相关概念混淆的地方，需注意区分边界条件和适用范围。'
      ]
      extraNote='<div style="margin-top:10px;padding:8px 12px;background:#fffbeb;border-radius:6px;border-left:3px solid #f59e0b;font-size:12px;color:#92400e"><strong>'+(exNames[pi%exNames.length])+'：</strong>'+exContents[pi%exContents.length]+'</div>'

      slides.push('<div style="background:#fff;padding:28px 32px;min-height:380px;border-radius:12px"><h2 style="font-size:18px;font-weight:600;color:#1e293b;margin:0 0 16px;display:flex;align-items:center;gap:10px"><span style="display:flex;width:28px;height:28px;border-radius:50%;background:'+grad+';color:#fff;align-items:center;justify-content:center;font-size:12px;font-weight:600;flex-shrink:0">'+(pi+3)+'</span>知识点'+(pi+1)+'：'+ptTitle+'</h2><div style="padding:16px 20px;background:#f8fafc;border-radius:10px;border-left:4px solid #CCFF00;font-size:14px;color:#475569;line-height:2">'+ptDetail+'</div>'+extraNote+'<div style="margin-top:12px;padding:8px 16px;background:#f0fdf4;border-radius:8px;font-size:11px;color:#15803d">💡 重点掌握：理解'+ptTitle+'的核心定义，能够独立推导和应用相关结论</div></div>')
    }
    // 如果知识点太多，加一个汇总页
    if(pts.length>6){
      var remainingNames=[]
      for(var ri=6;ri<pts.length;ri++){
        var rt=pts[ri]; var rc=rt.indexOf('：'); if(rc<0) rc=rt.indexOf(':')
        remainingNames.push(rc>0?rt.substring(0,rc).replace(/[：:、，,。·]/g,'').trim():rt.substring(0,20))
      }
      slides.push('<div style="background:#fff;padding:28px 32px;min-height:380px;border-radius:12px"><h2 style="font-size:18px;font-weight:600;color:#1e293b;margin:0 0 16px">📋 补充知识点</h2><div style="display:flex;flex-direction:column;gap:6px">'+remainingNames.map(function(n,i){return '<div style="padding:10px 14px;background:#f8fafc;border-radius:8px;border:1px solid #e2e8f0;font-size:13px;color:#475569">'+(7+i)+'.  '+n+'</div>'}).join('')+'</div><p style="color:#94a3b8;font-size:12px;margin-top:12px">以上为本章补充知识点，详见教材和课后阅读材料</p></div>')
    }
  }else{
    // 无KB数据时为非KB课程生成内容
    slides.push('<div style="background:#fff;padding:28px 32px;min-height:380px;border-radius:12px"><h2 style="font-size:18px;font-weight:600;color:#1e293b;margin:0 0 16px">📌 '+chName+' 核心内容</h2><div style="padding:16px 20px;background:linear-gradient(135deg,#f8fafc,#f0f2f5);border-radius:10px;min-height:200px;display:flex;align-items:center;justify-content:center"><p style="font-size:15px;color:#64748b;text-align:center;line-height:2">'+chName+'是《'+course.name+'》的<br>第'+chNum+'章核心内容<br><br>详细课件请参考课程教材和讲义<br>或通过第三方平台搜索相关PPT资源</p></div></div>')
  }

  // 4. 教材精讲页（如果有TB教材数据）
  if(chTb&&chTb.sections&&chTb.sections.length>0&&chTb.sections[0].content.length>20){
    var sec0=chTb.sections[0]
    var excerpt=sec0.content.replace(/\n\n/g,'<br><br>').substring(0,300)
    slides.push('<div style="background:#fff;padding:28px 32px;min-height:380px;border-radius:12px"><h2 style="font-size:18px;font-weight:600;color:#1e293b;margin:0 0 16px;padding-bottom:8px;border-bottom:3px solid #10b98133;display:flex;align-items:center;gap:8px"><span style="font-size:20px">📖</span> '+sec0.title+'</h2><div style="padding:16px 20px;background:#f0fdf4;border-radius:10px;border-left:4px solid #10b981;font-size:14px;color:#065f46;line-height:2;text-indent:2em">'+excerpt+'</div><p style="font-size:12px;color:#94a3b8;margin-top:10px;text-align:right">—— 选自《'+course.textbook+'》</p></div>')
  }

  // 5. 例题讲解页（如果课程有练习题且属于本章相关）
  var courseEx=course.exercises||[]
  if(courseEx.length>0&&chIdx<courseEx.length){
    var ex=courseEx[chIdx%courseEx.length]
    var exQ=ex.question||''
    var exA=ex.answer||''
    var exOpts=ex.options||[]
    var exOptsHtml=exOpts.length?exOpts.map(function(o,i){return '<div style="padding:6px 12px;background:#f8fafc;border-radius:6px;font-size:13px;color:#475569">'+String.fromCharCode(65+i)+'. '+o+'</div>'}).join(''):''
    slides.push('<div style="background:#fff;padding:28px 32px;min-height:380px;border-radius:12px"><h2 style="font-size:18px;font-weight:600;color:#1e293b;margin:0 0 16px;padding-bottom:8px;border-bottom:3px solid #f59e0b33;display:flex;align-items:center;gap:8px"><span style="font-size:20px">✍️</span> 典型例题</h2><div style="padding:16px 20px;background:#fffbeb;border-radius:10px;border-left:4px solid #f59e0b;margin-bottom:12px"><strong style="color:#1e293b;font-size:14px">例题：</strong><span style="font-size:14px;color:#475569;line-height:1.8">'+exQ+'</span></div><div style="display:flex;flex-direction:column;gap:4px;margin-bottom:12px">'+exOptsHtml+'</div><div style="padding:12px 16px;background:#ecfdf5;border-radius:8px;border:1px solid #a7f3d0"><strong style="color:#065f46">参考答案：</strong><span style="color:#065f46">'+exA+'</span></div></div>')
  }

  // 6. 章节小结页
  slides.push('<div style="background:#fff;padding:32px;min-height:380px;border-radius:12px"><h2 style="font-size:20px;font-weight:600;color:#1e293b;margin:0 0 20px;text-align:center">📝 本章小结</h2><div style="padding:16px 20px;background:linear-gradient(135deg,#f0f4ff,#eef2ff);border-radius:10px;border:2px dashed #A9F0FF;text-align:center"><p style="font-size:15px;color:#475569;line-height:2;margin:0 0 12px">✅ 掌握<strong>'+chName+'</strong>的核心概念和基本原理</p><p style="font-size:15px;color:#475569;line-height:2;margin:0 0 12px">✅ 熟练运用<strong>相关公式和方法</strong>解决实际问题</p><p style="font-size:15px;color:#475569;line-height:2;margin:0 0 12px">✅ 理解与其他章节之间的<strong>逻辑关系</strong></p><p style="font-size:15px;color:#475569;line-height:2;margin:0">✅ 完成课后习题，<strong>巩固学习效果</strong></p></div><div style="margin-top:16px;text-align:center"><p style="font-size:13px;color:#94a3b8">下一章预告：'+(chIdx+1<chCount?chNames[chIdx+1]:'总复习')+'</p></div></div>')

  // 7. 尾页
  slides.push('<div style="background:'+grad+';color:#fff;padding:60px 40px;text-align:center;min-height:380px;display:flex;flex-direction:column;justify-content:center;border-radius:12px"><div style="font-size:48px;margin-bottom:12px">📖</div><h1 style="font-size:28px;font-weight:700;margin:0 0 12px">下节预告</h1><p style="font-size:18px;opacity:.85">'+(chIdx+1<chCount?'第'+(chIdx+2)+'章 '+chNames[chIdx+1]:'总复习与考试')+'</p><p style="font-size:13px;opacity:.5;margin-top:24px">'+course.school+' · '+course.instructor+' 主讲</p></div>')
  return slides
}

// ═══════════ 视频教学内容生成（每个视频针对其具体章节） ═══════════
function buildVideoContents(course, videoName, chNames){
  // 从视频名称中提取章节名
  var targetCh = ''
  var vidx = 0
  // 匹配"第N讲 · 章节名"或"第N讲"模式
  var m = videoName.match(/第(\d+)讲\s*[·]\s*(.+)/)
  if (m) { vidx = parseInt(m[1])-1; targetCh = m[2].trim() }
  else {
    // 或者从chNames中匹配
    for (var ci=0;ci<chNames.length;ci++) {
      if (videoName.indexOf(chNames[ci]) >= 0) { targetCh = chNames[ci]; vidx = ci; break }
    }
  }
  // 如果都匹配不上，使用第一个章节
  if (!targetCh && chNames.length > 0) { targetCh = chNames[0]; vidx = 0 }
  if (!targetCh) targetCh = '课程概述'

  var kb = getKB(course, targetCh)
  var contents = []

  if (kb && kb.points && kb.points.length > 0) {
    // 为每个知识点生成一段视频讲解内容（8-10段）
    var totalSegs = Math.min(10, kb.points.length * 2)
    for (var i = 0; i < totalSegs; i++) {
      var pi = i % kb.points.length
      var p = kb.points[pi]
      var colonIdx = -1
      for (var ci2=0;ci2<p.length;ci2++){if(p[ci2]==='：'||p[ci2]===':'){colonIdx=ci2;break}}
      var t = colonIdx>0 ? p.substring(0,colonIdx).replace(/[：:、，,。·]/g,'').trim() : p.substring(0,25)
      var d = colonIdx>0 ? p.substring(colonIdx+1).trim() : p

      // 每段内容做细微变化使其不重复
      var prefixes = ['首先','接下来','值得注意的是','深入分析','进一步来看','需要强调的是','关键点在于','我们来详细讲解']
      var pre = prefixes[i % prefixes.length]

      contents.push({
        title: t,
        detail: pre + '，' + d + '。这是' + targetCh + '的核心内容，对于理解《' + course.name + '》整体框架至关重要。'
      })
    }
  } else {
    // 没有KB数据时，针对具体章节生成内容
    var genTopics = [
      {t: targetCh+'的概念引入', d: '我们从最基本的概念出发，系统介绍'+targetCh+'的定义、背景和核心要素。理解这些基础概念是深入学习的前提，建议同学们做好笔记。'},
      {t: targetCh+'的基本定义', d: '现在我们来严格定义'+targetCh+'。在《'+course.name+'》中，'+targetCh+'是一个非常重要的知识点模块，需要准确理解每个术语的含义。'},
      {t: targetCh+'的核心原理', d: '接下来讲解'+targetCh+'的核心原理。这个原理是本章最重要的理论基础，我们通过逻辑推导和直观解释两种方式来帮助大家理解。'},
      {t: targetCh+'的关键方法', d: '掌握了原理之后，我们来学习'+targetCh+'相关的关键方法。这个方法在实际问题中应用广泛，同学们一定要熟练掌握。'},
      {t: targetCh+'的经典案例', d: '理论结合实践，现在我们来看一个关于'+targetCh+'的经典案例。通过这个案例的分析，你会更深刻地理解前面所学的理论知识。'},
      {t: targetCh+'的深入分析', d: '让我们更深入地分析'+targetCh+'的内在结构。这里有一些容易混淆的地方需要特别注意，我们逐一辨析。'},
      {t: targetCh+'的应用拓展', d: '了解了'+targetCh+'的基础之后，我们来看一些拓展应用。这些应用场景展示了'+targetCh+'在不同领域的实际价值。'},
      {t: targetCh+'的常见问题', d: '在学习'+targetCh+'的过程中，同学们经常会遇到一些问题。我们总结了几种常见误区，帮助大家避免踩坑。'},
      {t: targetCh+'的习题讲解', d: '现在我们通过一道典型习题来巩固'+targetCh+'的知识。请同学们先自己思考，然后再看详细解析。'},
      {t: targetCh+'的总结回顾', d: '最后，我们对'+targetCh+'进行一个系统的总结回顾。掌握这个知识点框架，你就能够灵活运用到实际问题中了。'}
    ]
    for (var j=0;j<genTopics.length;j++) {
      contents.push({title: genTopics[j].t, detail: genTopics[j].d})
    }
  }
  return contents
}

// ═══════════ 电子课本详细内容库（第三方平台风格的完整教材内容） ═══════════
var TB={
DSA:{
  '绪论':{intro:'数据结构是计算机科学的核心基础课程，研究数据的逻辑结构、存储结构及其运算。本章建立后续所有章节的概念基础。',sections:[
    {title:'1.1 数据结构的基本概念',content:'数据（Data）是信息的载体，是所有能被计算机识别、存储和加工处理的符号的总称。数据元素（Data Element）是数据的基本单位，在计算机程序中通常被作为一个整体进行考虑和处理。数据项（Data Item）是组成数据元素的、有独立含义的、不可分割的最小单位。\n\n数据结构（Data Structure）是指相互之间存在一种或多种特定关系的数据元素的集合。数据结构包含三个方面的内容：逻辑结构、物理结构（存储结构）和数据的运算。'},
    {title:'1.2 逻辑结构的分类',content:'集合结构：数据元素之间没有其他关系，仅属于同一个集合。如：整数集合、字母集合。\n\n线性结构：数据元素之间是一对一的关系。除第一个和最后一个元素外，每个元素有且仅有一个前驱和一个后继。典型：线性表、栈、队列。\n\n树形结构：数据元素之间是一对多的关系。除根节点外，每个节点有且仅有一个前驱，可以有多个后继。典型：二叉树、B树。\n\n图形结构：数据元素之间是多对多的关系。每个节点可以有多个前驱和多个后继。典型：有向图、无向图。'},
    {title:'1.3 存储结构',content:'顺序存储：把逻辑上相邻的元素存储在物理位置上也相邻的存储单元中。优点是可以实现随机存取O(1)，缺点是插入删除需要移动大量元素O(n)。\n\n链式存储：借助指示元素存储地址的指针来表示元素之间的逻辑关系。不要求逻辑上相邻的元素在物理位置上也相邻。优点是插入删除O(1)，缺点是不能随机存取。\n\n索引存储：在存储元素信息的同时，还建立附加的索引表。索引表中的每一项称为索引项，形式为（关键字，地址）。\n\n散列存储：根据元素的关键字直接计算出该元素的存储地址，又称Hash存储。'},
    {title:'1.4 算法与复杂度分析',content:'算法（Algorithm）是对特定问题求解步骤的一种描述。算法的五大特性：有穷性、确定性、可行性、有输入、有输出。\n\n时间复杂度T(n)：算法中基本操作的执行次数与问题规模n之间的关系。用大O表示法：T(n) = O(f(n))。\n\n常见时间复杂度排序：O(1) < O(log n) < O(n) < O(n log n) < O(n²) < O(n³) < O(2ⁿ) < O(n!)\n\n空间复杂度S(n)：算法执行过程中所需的额外存储空间与问题规模n之间的关系。'}
  ],exercises:[{q:'以下哪种数据结构是线性结构？',a:'线性表、栈、队列都是线性结构。树和图是非线性结构。'},{q:'顺序存储和链式存储的主要区别是什么？',a:'顺序存储要求逻辑上相邻的元素物理上也相邻，支持随机存取；链式存储通过指针链接，不要求物理相邻，不支持随机存取。'}]},

  '线性表':{intro:'线性表是最基本、最简单、也是最常用的一种数据结构。线性表中数据元素之间的关系是一对一的关系，即除了第一个和最后一个数据元素之外，其它数据元素都是首尾相接的。',sections:[
    {title:'2.1 线性表的定义',content:'线性表是具有相同数据类型的n（n≥0）个数据元素的有限序列。其中n为表长，当n=0时称为空表。\n\n若将线性表记为(a₁, a₂, ..., aᵢ₋₁, aᵢ, aᵢ₊₁, ..., aₙ)，则aᵢ₋₁是aᵢ的直接前驱，aᵢ₊₁是aᵢ的直接后继。第一个元素a₁没有前驱，最后一个元素aₙ没有后继。'},
    {title:'2.2 顺序表',content:'顺序表是用一组地址连续的存储单元依次存储线性表中的数据元素。\n\n插入操作：在位置i插入元素，需要将aᵢ到aₙ全部后移一位。平均移动次数为n/2，时间复杂度O(n)。\n\n删除操作：删除位置i的元素，需要将aᵢ₊₁到aₙ全部前移一位。平均移动次数为(n-1)/2，时间复杂度O(n)。\n\n按位查找：O(1)，这是顺序表最大的优势。'},
    {title:'2.3 单链表',content:'单链表中每个结点包含数据域和指针域。指针域存储指向下一个结点的指针。\n\n头插法建表：每次将新结点插入到链表头部，时间复杂度O(n)。生成的链表中元素的顺序与输入顺序相反。\n\n尾插法建表：每次将新结点插入到链表尾部，需要维护一个尾指针，时间复杂度O(n)。生成的链表中元素的顺序与输入顺序相同。'},
    {title:'2.4 双向链表与循环链表',content:'双向链表的每个结点有前驱指针prior和后继指针next两个指针域。优点是访问前驱结点的时间为O(1)。\n\n循环链表中最后一个结点的指针指向头结点，形成一个环。判断空表的条件是头结点的指针指向自身。\n\n循环双向链表兼具两者的优点，在实际应用中非常灵活。'}
  ],exercises:[{q:'在长度为n的顺序表中插入一个元素的时间复杂度是？',a:'O(n)。因为最坏情况下需要移动所有元素，平均移动n/2个元素。'},{q:'头插法和尾插法建立单链表的区别是什么？',a:'头插法生成的链表元素顺序与输入相反，尾插法生成的链表元素顺序与输入相同。'}]},

  '栈和队列':{intro:'栈和队列是两种重要的线性结构。它们的特殊之处在于运算受限——栈只能在一端进行插入和删除，队列只能在一端插入、另一端删除。',sections:[
    {title:'3.1 栈的定义与实现',content:'栈（Stack）是限定仅在表尾进行插入或删除操作的线性表。表尾端称为栈顶（top），表头端称为栈底（bottom）。后进先出（LIFO）。\n\n顺序栈：使用数组实现，top指针指向栈顶元素。入栈：top++, data[top]=x。出栈：x=data[top], top--。\n\n链栈：使用单链表实现，链表头部即为栈顶。入栈使用头插法，出栈删除头结点。'},
    {title:'3.2 栈的应用',content:'括号匹配：遇到左括号入栈，遇到右括号出栈并匹配。如果栈不为空或栈为空时遇到右括号，则匹配失败。\n\n表达式求值：将中缀表达式转换为后缀表达式，然后用栈求值。\n中缀转后缀规则：操作数直接输出；运算符与栈顶比较优先级，若当前优先级≤栈顶则栈顶出栈再入栈；左括号入栈；右括号则弹出到左括号。\n\n递归实现：函数调用使用系统栈保存返回地址和局部变量。'},
    {title:'3.3 队列的定义与实现',content:'队列（Queue）是只允许在表的一端进行插入（队尾rear）、在另一端进行删除（队头front）的线性表。先进先出（FIFO）。\n\n循环队列：用数组实现，通过取模运算将数组"首尾相接"。队满条件：(rear+1)%MaxSize==front。队空条件：front==rear。元素个数：(rear-front+MaxSize)%MaxSize。'},
    {title:'3.4 队列的应用',content:'BFS（广度优先搜索）：使用队列存储待访问的节点。\n\n操作系统中的进程调度：就绪队列、等待队列。\n\n缓冲区管理：打印缓冲区、消息队列等。'}
  ],exercises:[{q:'栈的特点是什么？',a:'后进先出（LIFO），只能在栈顶进行插入和删除操作。'},{q:'循环队列如何判断队满？',a:'(rear+1)%MaxSize == front 时队满。'}]}
},

AM1:{
  '函数与极限':{intro:'极限是微积分的基础概念，贯穿整个高等数学。本章从函数概念出发，系统介绍数列极限和函数极限的理论。',sections:[
    {title:'1.1 函数的概念与性质',content:'设D是一个非空实数集，如果按照某个确定的法则f，对每个x∈D，都有唯一确定的实数y与之对应，则称y是x的函数，记为y=f(x)。\n\n函数的四个基本性质：\n① 有界性：若存在M>0，使|f(x)|≤M对一切x∈D成立，则f(x)在D上有界。\n② 单调性：若x₁<x₂时f(x₁)<f(x₂)，则f(x)单调递增。\n③ 奇偶性：f(-x)=f(x)为偶函数（关于y轴对称），f(-x)=-f(x)为奇函数（关于原点对称）。\n④ 周期性：若存在T>0使f(x+T)=f(x)，则f(x)为周期函数。'},
    {title:'1.2 数列极限',content:'设{aₙ}为一数列，如果存在常数a，对于任意给定的正数ε（不论它多么小），总存在正整数N，使得当n>N时，不等式|aₙ-a|<ε都成立，则称常数a是数列{aₙ}的极限，记为lim(n→∞)aₙ=a。\n\n收敛数列的性质：\n① 唯一性：若数列收敛，则极限唯一。\n② 有界性：收敛数列必有界。\n③ 保号性：若lim aₙ=a>0，则存在N，当n>N时aₙ>0。'},
    {title:'1.3 函数极限',content:'函数极限的ε-δ定义：设函数f(x)在点x₀的某去心邻域内有定义。如果存在常数A，对于任意给定的正数ε，总存在正数δ，使得当0<|x-x₀|<δ时，不等式|f(x)-A|<ε恒成立，则称A为f(x)当x→x₀时的极限。\n\n两个重要极限：\n① lim(x→0) sinx/x = 1\n② lim(x→∞) (1+1/x)ˣ = e ≈ 2.71828\n\n等价无穷小替换（x→0时）：\nsinx ~ x, tanx ~ x, arcsinx ~ x, arctanx ~ x\nln(1+x) ~ x, eˣ-1 ~ x, (1+x)ᵅ-1 ~ αx'},
    {title:'1.4 连续性',content:'函数f(x)在点x₀连续 ⟺ lim(x→x₀)f(x) = f(x₀)\n\n间断点分类：\n第一类间断点（左右极限都存在）：\n• 可去间断点：左右极限相等但不等于函数值\n• 跳跃间断点：左右极限不相等\n\n第二类间断点（左右极限至少有一个不存在）：\n• 无穷间断点：极限为∞\n• 振荡间断点：极限不存在且不为∞'}
  ],exercises:[{q:'lim(x→0)(sin3x)/x 的值为？',a:'3。利用等价无穷小sin3x ~ 3x，所以原式 = lim(3x/x) = 3。'},{q:'函数f(x)=x³-3x的单调递增区间是？',a:'(-∞,-1)∪(1,+∞)。f\'(x)=3x²-3=3(x+1)(x-1)>0当x<-1或x>1。'}]}
},

OS:{
  '操作系统引论':{intro:'操作系统是计算机系统中最基本的系统软件，它管理计算机的硬件资源和软件资源，为用户和应用程序提供服务接口。',sections:[
    {title:'1.1 操作系统的定义与目标',content:'操作系统（Operating System）是控制和管理计算机硬件和软件资源、合理地组织计算机工作流程、方便用户使用的程序集合。\n\n操作系统的三大目标：\n① 方便性：提供用户接口，使计算机易于使用\n② 有效性：提高系统资源利用率和系统吞吐量\n③ 可扩展性：适应硬件和体系结构的发展'},
    {title:'1.2 操作系统的发展历程',content:'手工操作阶段 → 无操作系统\n早期批处理（单道批处理）→ 自动作业切换\n多道批处理 → 多个程序同时驻留内存\n分时系统 → 多用户交互式使用\n实时系统 → 及时响应外部事件\n网络操作系统 → 网络资源共享\n分布式系统 → 多机协同处理'},
    {title:'1.3 操作系统的基本特征',content:'并发（Concurrency）：多个程序在同一时间段内交替执行。注意与并行的区别——并行是同一时刻同时执行。\n\n共享（Sharing）：系统中的资源可供多个并发执行的进程共同使用。分为互斥共享（临界资源）和同时访问。\n\n虚拟（Virtual）：通过某种技术将一个物理实体变为若干个逻辑上的对应物。如虚拟处理器、虚拟内存。\n\n异步（Asynchronism）：进程以不可预知的速度向前推进。'},
    {title:'1.4 操作系统的五大功能',content:'① 处理机管理：进程控制、进程同步、进程通信、调度\n② 存储器管理：内存分配、内存保护、地址映射、内存扩充\n③ 设备管理：设备分配、设备传输控制、设备独立性\n④ 文件管理：文件存储空间管理、目录管理、文件读写保护\n⑤ 用户接口：命令接口、程序接口（系统调用）、图形接口'}
  ],exercises:[{q:'操作系统的四个基本特征是什么？',a:'并发、共享、虚拟、异步。其中并发是最基本的特征。'},{q:'分时系统和实时系统的主要区别是什么？',a:'分时系统追求交互性和响应时间，实时系统追求在规定时间内完成特定任务，对可靠性要求更高。'}]},

  '进程描述与控制':{intro:'进程是操作系统中最核心的概念之一，是资源分配和调度的基本单位。理解进程的生命周期和状态转换是掌握操作系统的关键。',sections:[
    {title:'2.1 进程的定义与特征',content:'进程是程序在一个数据集合上运行的过程，是系统进行资源分配和调度的基本单位。\n\n进程与程序的区别：\n• 进程是动态的，程序是静态的\n• 进程有生命周期，程序是永久的\n• 一个程序可对应多个进程，一个进程可执行多个程序\n• 进程有独立的地址空间，程序没有'},
    {title:'2.2 进程控制块PCB',content:'PCB（Process Control Block）是操作系统用来描述和管理进程的数据结构。\n\nPCB包含的信息：\n① 进程标识符PID\n② 处理机状态信息（寄存器值）\n③ 进程调度信息（状态、优先级、等待事件）\n④ 进程控制信息（程序段和数据段地址、资源清单、链接指针）'},
    {title:'2.3 进程状态与转换',content:'三态模型：\n就绪(Ready) → 执行(Running)：进程被调度\n执行(Running) → 阻塞(Blocked)：等待I/O或事件\n阻塞(Blocked) → 就绪(Ready)：I/O完成或事件发生\n执行(Running) → 就绪(Ready)：时间片用完\n\n五态模型增加：创建态和终止态'},
    {title:'2.4 线程',content:'线程是进程中的一个执行实体，是CPU调度的基本单位。\n\n线程与进程的比较：\n• 拥有资源：进程是资源分配的基本单位，线程不拥有资源\n• 调度：线程是调度的基本单位，切换线程的开销远小于切换进程\n• 并发性：线程之间可以并发执行\n• 系统开销：创建/撤销线程的开销远小于进程'}
  ],exercises:[{q:'进程的三种基本状态是什么？',a:'就绪（Ready）、执行（Running）、阻塞（Blocked）。'},{q:'线程与进程的主要区别是什么？',a:'线程是CPU调度的基本单位，不拥有资源；进程是资源分配的基本单位。线程切换开销小于进程切换。'}]}
},

CN:{
  '概述':{intro:'计算机网络是将地理位置不同的具有独立功能的多台计算机及其外部设备，通过通信线路和通信设备连接起来。',sections:[
    {title:'1.1 计算机网络的定义与分类',content:'计算机网络是将分散的、具有独立功能的计算机系统，通过通信设备和线路连接起来，由功能完善的网络软件实现资源共享和信息传递的系统。\n\n按覆盖范围分类：\n• 局域网LAN：几米到几千米，如校园网\n• 城域网MAN：几千米到几十千米\n• 广域网WAN：几十千米到几千千米，如Internet\n\n按拓扑结构分类：星型、总线型、环型、网状型'},
    {title:'1.2 网络性能指标',content:'速率（Data Rate）：数据传输速率，单位bps\n\n带宽（Bandwidth）：信号频率范围或最大数据传输速率\n\n吞吐量（Throughput）：单位时间内通过网络的数据量\n\n时延（Delay）= 发送时延 + 传播时延 + 处理时延 + 排队时延\n• 发送时延 = 数据长度/发送速率\n• 传播时延 = 信道长度/传播速度\n\n往返时延RTT：从发送数据到收到确认的时间'},
    {title:'1.3 OSI与TCP/IP体系结构',content:'OSI七层模型：物理层→数据链路层→网络层→传输层→会话层→表示层→应用层\n\nTCP/IP四层模型：网络接口层→网际层→传输层→应用层\n\n实际教学常用五层模型：物理层→数据链路层→网络层→传输层→应用层\n\n各层的协议数据单元PDU：\n• 物理层：比特bit\n• 数据链路层：帧frame\n• 网络层：分组packet\n• 传输层：报文段segment'}
  ],exercises:[{q:'OSI参考模型有几层？',a:'七层：物理层、数据链路层、网络层、传输层、会话层、表示层、应用层。'},{q:'发送时延如何计算？',a:'发送时延 = 数据长度(bit) / 发送速率(bps)。'}]}
},

MIC:{
  '绪论':{intro:'经济学研究的是社会如何管理自己的稀缺资源。微观经济学研究个体经济单位的决策行为以及市场机制如何配置资源。',sections:[
    {title:'1.1 经济学的基本问题',content:'稀缺性（Scarcity）：相对于人类无限的欲望，资源是有限的。稀缺性是经济学存在的根本原因。\n\n经济学研究的核心问题：\n① 生产什么（What）：社会应该生产哪些产品和服务\n② 如何生产（How）：用什么技术和资源来生产\n③ 为谁生产（For Whom）：产品和服务如何在社会成员之间分配'},
    {title:'1.2 生产可能性边界',content:'生产可能性边界PPF（Production Possibilities Frontier）表示在给定的资源和技术条件下，一个经济体所能生产的两种产品的最大数量组合。\n\nPPF的特征：\n• PPF上的点：有效率的生产组合\n• PPF内的点：无效率（资源未充分利用）\n• PPF外的点：当前条件下不可达到\n• PPF向外移动：经济增长（技术进步或资源增加）'},
    {title:'1.3 机会成本与边际分析',content:'机会成本（Opportunity Cost）：为了得到某种东西而必须放弃的东西。例如上大学的机会成本不仅是学费，还包括放弃工作的收入。\n\n边际分析：理性人在做决策时比较边际收益（MB）和边际成本（MC）。\n• 当MB>MC时，增加该活动\n• 当MB<MC时，减少该活动\n• 当MB=MC时，达到最优'},
    {title:'1.4 微观经济学与宏观经济学',content:'微观经济学：研究个体决策单位（消费者、企业）的行为和市场均衡。\n核心概念：供给与需求、弹性、消费者剩余、生产者剩余\n\n宏观经济学：研究国民经济整体的运行。\n核心概念：GDP、失业率、通货膨胀率、经济增长\n\n两者的关系：宏观现象是微观个体行为的加总。'}
  ],exercises:[{q:'什么是机会成本？',a:'为了得到某种东西而必须放弃的最有价值的东西。'},{q:'边际分析的基本原理是什么？',a:'理性人比较边际收益和边际成本，当边际收益等于边际成本时达到最优决策。'}]}
}
}

// ═══════════ 每章独立PPT生成（每章一个完整PPT，不是全部合并） ═══════════
// 富知识点解析：「标题：讲解1。讲解2。讲解3。」→ { head:标题, body:[句子...] }
function kpParse(p) {
  var s = (p || '').trim();
  var cx = -1;
  for (var i = 0; i < s.length && i < 30; i++) { if (s[i] === '：' || s[i] === ':') { cx = i; break; } }
  if (cx > 1) {
    var head = s.substring(0, cx).replace(/[，,、。]/g, '').trim();
    var rest = s.substring(cx + 1).trim();
    if (head.length <= 25 && rest.length >= 15) {
      var body = rest.split(/[。；;]/).map(function(x){ return x.trim(); }).filter(function(x){ return x.length > 1; });
      if (body.length) return { head: head, body: body };
    }
  }
  return { head: s.length > 25 ? s.substring(0, 25) : s, body: [] };
}

function buildAllChapterPPTs(course) {
  var cache = {};
  var chTitles = course._chTitles || course.chapterTitles || [];
  for (var ci = 0; ci < chTitles.length; ci++) {
    cache[ci] = buildSingleChapterPPT(course, ci);
  }
  return cache;
}

function buildSingleChapterPPT(course, chIdx) {
  var slides = [];
  var chTitles = course._chTitles || course.chapterTitles || [];
  var chTitle = chTitles[chIdx];
  if (!chTitle) chTitle = '第' + (chIdx+1) + '章';
  var chNum = chIdx + 1;
  var chKw = chTitle.replace(/[第\d+章\s]/g, '').trim() || chTitle;
  var cn = course.name || '';
  var cat = course.cat || '';

  // 知识点列表 — 精确匹配章节名
  var allPoints = [];
  var chapters = course.chapters || {};
  // 先用完整章节名精确查找
  if (chapters[chTitle] && chapters[chTitle].points && chapters[chTitle].points.length) {
    allPoints = chapters[chTitle].points.slice();
  }
  // 再用 getKB 模糊匹配
  if (!allPoints.length) {
    var kb = getKB(course, chTitle);
    if (kb && kb.points && kb.points.length) allPoints = kb.points.slice();
  }
  // 遍历所有KB章节键名进行包含匹配
  if (!allPoints.length) {
    for (var key in chapters) {
      if (chTitle.indexOf(key) >= 0 || key.indexOf(chTitle) >= 0) {
        if (chapters[key] && chapters[key].points && chapters[key].points.length) {
          allPoints = chapters[key].points.slice();
          break;
        }
      }
    }
  }
  // 兜底：基于章名+学科生成
  if (!allPoints.length) {
    var isMath = (cat.indexOf('数学')>=0 || cn.indexOf('数学')>=0);
    var isCS = (cat.indexOf('计算机')>=0 || cn.indexOf('编程')>=0);
    var isPhysics = (cat.indexOf('物理')>=0);
    var isEcon = (cat.indexOf('经济')>=0);
    if (isMath) allPoints = [chKw+'的数学定义',chKw+'的几何直观',chKw+'的核心定理',chKw+'的计算方法',chKw+'的题型分类',chKw+'的综合应用'];
    else if (isCS) allPoints = [chKw+'的背景与思想',chKw+'的核心数据结构',chKw+'的算法流程',chKw+'的复杂度分析',chKw+'的代码实现',chKw+'的应用场景'];
    else if (isPhysics) allPoints = [chKw+'的物理现象',chKw+'的基本概念',chKw+'的核心定律',chKw+'的公式推导',chKw+'的计算方法',chKw+'的实验设计'];
    else if (isEcon) allPoints = [chKw+'的经济学定义',chKw+'的理论框架',chKw+'的图形分析',chKw+'的案例分析',chKw+'的政策含义',chKw+'的实证方法'];
    else allPoints = [chKw+'的基本概念',chKw+'的核心理论',chKw+'的关键方法',chKw+'的典型应用',chKw+'的计算分析',chKw+'的知识总结'];
  }

  // 章封面
  slides.push({ title: '第'+chNum+'章 '+chTitle, type:'cover',
    points:[course.school||'', '主讲：'+(course.instructor||'优秀教师'), '本章共'+allPoints.length+'个知识点', course.textbook?'教材：'+course.textbook:'']
  });

  // 本章学习目标 — 基于实际知识点（富知识点只取标题，避免文字墙）
  slides.push({ title: chTitle+' · 学习目标', type:'goal',
    points: allPoints.slice(0,3).map(function(p,i){
      var kpp = kpParse(p);
      return '目标'+(i+1)+'：掌握'+(kpp.head || p);
    })
  });

  // 每个知识点独立精讲页 — 富知识点按句子拆分为讲解步骤
  for (var pi=0; pi<allPoints.length; pi++) {
    var pt = allPoints[pi];
    var kpp = kpParse(pt);
    var detailPoints = [];
    if (kpp.body.length >= 2) {
      // 富知识点：标题进页标题，讲解句子逐条呈现
      detailPoints = kpp.body.slice(0, 5);
    } else {
      // 短知识点：基于知识点的关键词生成讲解提示
      detailPoints.push(pt);
      var keywords = pt.split(/[，,、；;：:]/);
      for (var ki=0; ki<Math.min(keywords.length, 3); ki++) {
        var kw = keywords[ki].trim();
        if (kw && kw.length > 2) {
          detailPoints.push('深入理解：' + kw);
        }
      }
      // 如果知识点本身不长，补充教学提示
      if (pt.length < 30) {
        detailPoints.push('要点：' + pt + '，需结合实例加深理解');
        detailPoints.push('应用：将' + pt + '运用到实际问题中');
      }
    }
    slides.push({ title: chTitle + ' · 知识精讲：' + kpp.head, type:'keypoint', points: detailPoints });
  }

  // 例题精讲 — 基于前两个知识点的实际应用
  if (allPoints.length >= 2) {
    var kpA = kpParse(allPoints[0]).head || allPoints[0];
    var kpB = kpParse(allPoints[1]).head || allPoints[1];
    var kpZ = kpParse(allPoints[allPoints.length-1]).head || chKw;
    slides.push({ title: chKw + ' · 例题精讲', type:'scenario',
      points: [
        '题目：请分析' + kpA + '，并说明其与' + kpB + '的关系',
        '分析思路：从' + kpA + '出发，逐步推导',
        '解答步骤：1) 明确概念  2) 列举关键点  3) 对比分析  4) 得出结论',
        '重点提示：注意' + kpZ + '的综合运用'
      ]
    });
  }

  // 课堂练习
  var exPts = allPoints.slice(0, 4).map(function(p){ return kpParse(p).head || p; });
  while (exPts.length < 4) exPts.push(chKw + '相关知识点');
  slides.push({ title: chKw + ' · 课堂练习', type:'exercise',
    points: [
      '简答题：请用自己的语言解释' + exPts[0],
      '分析题：举例说明' + exPts[1] + '在实际中的应用',
      '计算/推导题：基于' + exPts[2] + '完成相关练习',
      '综合题：综合运用' + exPts[0] + '和' + (exPts[3] || exPts[2]) + '解决实际问题'
    ]
  });

  // 本章小结 — 展示全部知识点标题
  slides.push({ title: chTitle+' · 本章小结', type:'summary',
    points: allPoints.slice(0,6).map(function(p,i){ return '第'+(i+1)+'讲：'+(kpParse(p).head || p); })
  });

  // 结束页
  slides.push({ title: '第'+chNum+'章结束', type:'end',
    points: [cn+' · '+chTitle, '共'+slides.length+'页 · '+allPoints.length+'个知识点全覆盖', '系统生成 · 以教材为准']
  });

  return slides;
}

// ═══════════ 电子课本章节内容生成（详细教材风格） ═══════════
function buildChapterContent(book,chIdx,chTitle,courseName,course){
  var num=chIdx+1; var kb=getKB(course,chTitle)
  var tb=TB[course._kb]&&TB[course._kb][chTitle]

  var html='<div style="line-height:2.1;font-size:15px;color:#334155;padding:8px 4px">'
  html+='<h3 style="color:#1e293b;margin:0 0 20px;font-size:22px;font-weight:700;border-bottom:3px solid #CCFF00;padding-bottom:12px">第'+num+'章 '+chTitle+'</h3>'

  // 章节引言
  var introText=tb?tb.intro:chTitle+'是《'+book.name+'》的核心章节，系统介绍'+chTitle+'的基本概念、核心理论和关键方法。通过本章学习，学生将深入理解'+chTitle+'的基本原理，掌握相关分析方法与应用技巧。'
  html+='<div style="margin:20px 0;padding:18px;background:linear-gradient(135deg,#eff6ff,#f0f9ff);border-radius:10px;border-left:5px solid #CCFF00">'
  html+='<h4 style="margin:0 0 10px;color:#A8D400;font-size:16px">📌 章节概述</h4>'
  html+='<p style="margin:0;color:#A8D400;line-height:2;text-indent:2em">'+introText+'</p></div>'

  // 学习目标
  html+='<div style="margin:20px 0;padding:18px;background:#f0fdf4;border-radius:10px;border-left:5px solid #22c55e">'
  html+='<h4 style="margin:0 0 10px;color:#15803d;font-size:16px">🎯 学习目标</h4>'
  html+='<ul style="margin:0;padding-left:20px;color:#15803d;line-height:2">'
  html+='<li>理解并掌握'+chTitle+'的核心概念和基本原理</li>'
  html+='<li>熟悉'+chTitle+'相关的定理、公式和推导方法</li>'
  html+='<li>能够运用所学知识分析和解决实际问题</li>'
  html+='<li>建立完整的知识体系框架，为后续学习奠定基础</li></ul></div>'

  // 详细知识点内容
  if(tb&&tb.sections){
    // 使用详细教材内容
    html+='<h4 style="font-size:17px;color:#1e293b;margin:24px 0 12px">📖 知识点详解</h4>'
    for(var si=0;si<tb.sections.length;si++){
      var sec=tb.sections[si]
      html+='<div style="margin:16px 0;padding:20px;background:#fff;border:1px solid #e2e8f0;border-radius:10px">'
      html+='<h5 style="font-size:15px;font-weight:600;color:#1e293b;margin:0 0 10px;padding-bottom:8px;border-bottom:2px solid #6366f133">'+sec.title+'</h5>'
      var paragraphs=sec.content.split('\n\n')
      for(var pi=0;pi<paragraphs.length;pi++){
        var para=paragraphs[pi].trim()
        if(para) html+='<p style="margin:8px 0;line-height:2;color:#475569;text-indent:2em">'+para+'</p>'
      }
      html+='</div>'
    }
    }else if(kb&&kb.points){
    // 使用KB知识点生成详细教材内容(拆分标题和详情)
    html+='<h4 style="font-size:17px;color:#1e293b;margin:24px 0 12px">📖 知识点详解</h4>'
    for(var i=0;i<kb.points.length;i++){
      var p=kb.points[i]
      var cx=-1
      for(var cc=0;cc<p.length;cc++){if(p[cc]==='：'||p[cc]===':'){cx=cc;break}}
      var pt=cx>0?p.substring(0,cx).replace(/[：:、，,。·]/g,'').trim():p
      var pd=cx>0?p.substring(cx+1).trim():''
      html+='<div style="margin:12px 0;padding:18px;background:#fff;border:1px solid #e2e8f0;border-radius:10px">'
      html+='<h5 style="font-size:15px;font-weight:600;color:#1e293b;margin:0 0 8px">'+(i+1)+'. '+pt+'</h5>'
      html+='<p style="margin:0;line-height:2;color:#475569;text-indent:2em">'+(pd||pt+'是'+chTitle+'中的核心知识点。理解这一概念对于构建完整的'+courseName+'知识体系至关重要。在学习过程中应结合具体实例加深理解,注重与其他知识点的关联。')+'</p>'
      html+='</div>'
    }
  }else{
    // 没有KB数据时生成通用详细内容
    html+='<h4 style="font-size:17px;color:#1e293b;margin:24px 0 12px">📖 知识点详解</h4>'
    html+='<div style="margin:12px 0;padding:18px;background:#fff;border:1px solid #e2e8f0;border-radius:10px">'
    html+='<p style="margin:0;line-height:2.2;color:#475569;text-indent:2em">'+chTitle+'是《'+book.name+'》的第'+num+'章。本章系统介绍'+chTitle+'的基本概念、核心理论和关键方法。在学习'+chTitle+'时，需要重点关注以下几个方面：首先，理解'+chTitle+'的基本定义和术语，这是后续学习的基础；其次，掌握'+chTitle+'的核心原理和分析方法，能够进行逻辑推理和数学推导；最后，学会将理论知识应用到实际问题中，培养分析和解决问题的能力。</p>'
    html+='<p style="margin:10px 0 0;line-height:2.2;color:#475569;text-indent:2em">建议学习时长：'+(2+chIdx%3)+'小时。学习时可配合教学视频和PPT课件，先预习教材对应章节，再观看视频讲解，最后独立完成课后习题。遇到疑难问题可反复观看视频或查阅参考书籍。</p>'
    html+='</div>'
  }

  // 练习与思考题
  if(tb&&tb.exercises){
    html+='<div style="margin:24px 0;padding:18px;background:#fef3c7;border-radius:10px;border:1px solid #fbbf24">'
    html+='<h4 style="margin:0 0 12px;color:#92400e;font-size:16px">✍️ 练习与思考</h4>'
    for(var ei=0;ei<tb.exercises.length;ei++){
      var ex=tb.exercises[ei]
      html+='<div style="margin:10px 0;padding:12px;background:#fff;border-radius:8px">'
      html+='<p style="margin:0 0 6px;font-weight:600;color:#1e293b">第'+(ei+1)+'题：'+ex.q+'</p>'
      html+='<p style="margin:0;color:#475569;line-height:1.8"><strong>参考解答：</strong>'+ex.a+'</p>'
      html+='</div>'
    }
    html+='</div>'
  }else{
    html+='<div style="margin:24px 0;padding:18px;background:#fef3c7;border-radius:10px;border:1px solid #fbbf24">'
    html+='<h4 style="margin:0 0 8px;color:#92400e;font-size:16px">✍️ 课后练习建议</h4>'
    html+='<p style="margin:0;color:#92400e;line-height:2">建议完成课后习题第'+(chIdx*3+1)+'至'+(chIdx*3+6)+'题，重点练习'+chTitle+'相关应用题。建议学习时长：'+(2+chIdx%3)+'小时。课后可利用配套PPT和教学视频进行复习巩固。</p>'
    html+='</div>'
  }

  // 重点提示
  html+='<div style="margin:24px 0;padding:16px;background:#fefce8;border-radius:10px;border:1px solid #fef08a">'
  html+='<h4 style="margin:0 0 8px;color:#854d0e;font-size:15px">⚠️ 重点提示</h4>'
  html+='<ul style="margin:0;padding-left:20px;color:#854d0e;line-height:2">'
  html+='<li>重点掌握'+chTitle+'的核心公式和关键推导过程</li>'
  html+='<li>注意区分易混淆的概念和理论，做好笔记对比</li>'
  html+='<li>结合例题和课后习题加深理解，举一反三</li>'
  html+='<li>建议配合教学视频和PPT课件同步学习</li></ul></div>'

  // 参考文献
  html+='<div style="margin:28px 0 8px;padding:12px 16px;background:#f8fafc;border-radius:8px;border:1px dashed #cbd5e1">'
  html+='<p style="margin:0;font-size:13px;color:#94a3b8">📖 参考：《'+book.name+'》，'+book.author+'编著，'+book.publisher+'出版。本章依据教材第'+num+'章编写。</p></div>'

  html+='</div>'
  return html
}

export default{
  name:'TeachingHub',
  components:{BilibiliPlayer, ResourceEmbedViewer},
  data:function(){return{
    moocTab:'all',searchQuery:'',allCourses:[],courseDetailVisible:false,currentCourse:null,detailTab:'intro',
    currentChapters:[],activeChapters:0,
    pptPreviewLoading:false,pptPreviewVisible:false,pptPreviewItem:null,pptSlidesHtml:[],pptSlideIdx:0,pptPreviewTitle:'',pptPreviewMode:'local',pptExternalUrl:'',pptExternalIframe:'',pptShowWebCard:false,pptExternalPlatform:'',
    videoPlayerVisible:false,currentVideo:null,currentVideoUrl:'',videoContents:[],videoContentIdx:0,videoEmbedUrl:'',videoNativeSrc:'',
    simVideoProgress:0,simVideoElapsed:0,simVideoDuration:420,simVideoBuffered:30,simVideoPaused:false,simVideoMuted:false,simVideoTimer:null,
    seeking:false,thumbDragging:false,
    readerLoading:false,readerVisible:false,readerBook:null,readerIsExternal:false,readerExternalUrl:'',readerIframeUrl:'',readerIframeError:false,readerShowWebCard:false,readerExternalPlatform:'',readerChapters:[],readerChapter:0,
    videoDialogTitle:'',
    // 导入功能
    showImportDialog:false,importTab:'url',importUrl:'',importName:'',importCategory:'',importing:false,
    embedDialogVisible:false,embedCode:'',embedInfo:null,
    manualCourse:{name:'',school:'',instructor:'',category:'',credit:3,description:'',textbook:''},
    manualCourse:{name:'',school:'',instructor:'',category:'',credit:3,description:'',textbook:''},
    showPlatformBar: true, // 默认展开平台推荐
    // 电子课本嵌入
    textbookPlatforms: [
      { name: '超星电子书', url: 'https://book.chaoxing.com/' },
      { name: '国家图书馆', url: 'https://www.nlc.cn/' },
      { name: '百度阅读', url: 'https://yuedu.baidu.com/' },
      { name: '高校图书馆', url: 'https://www.calis.edu.cn/' }
    ],
    currentTextbookPlatform: 'https://book.chaoxing.com/',
    tbIframeLoaded: false,
    recommendPlatforms: [
      { name:'B站 技术区', icon:'📺', desc:'AI/编程/工业实训视频首选，海量免费岗位技能教程与公开课', url:'https://search.bilibili.com/all?keyword=AI%20%E5%AE%9E%E6%88%98', type:'视频' },
      { name:'Hugging Face', icon:'🤗', desc:'大模型课程与开源模型库，LLM微调/RAG官方教程', url:'https://huggingface.co/learn', type:'课程/模型' },
      { name:'DataWhale 开源社区', icon:'🐋', desc:'开源学习组织，大模型应用/数据科学系列组队学习', url:'https://github.com/datawhalechina', type:'开源课程' },
      { name:'飞桨 AI Studio', icon:'🔥', desc:'百度AI实训平台，免费GPU算力+企业级实战项目', url:'https://aistudio.baidu.com', type:'实训/GPU' },
      { name:'阿里云天池', icon:'☁️', desc:'产业实战数据集与竞赛，工业质检/预测性维护真实场景', url:'https://tianchi.aliyun.com', type:'数据集/竞赛' },
      { name:'Microsoft Learn', icon:'🛠️', desc:'MLOps/Azure AI工程认证路径，企业级部署规范', url:'https://learn.microsoft.com/training', type:'认证路径' },
      { name:'Kaggle Learn', icon:'🏆', desc:'技能速成证书课程+全球最大数据科学竞赛社区', url:'https://www.kaggle.com/learn', type:'速成/竞赛' },
      { name:'中国大学MOOC', icon:'🎓', desc:'数学/计算机等先修理论基础课补充（国家精品课程）', url:'https://www.icourse163.org', type:'理论补充' }
    ],
    // ═══ 第三方资源获取状态 ═══
    thirdPlatforms: [],
    pptSearchKeyword:'',pptSearchPlatform:'',pptSearching:false,pptFetching:false,pptThirdResults:[],pptFetchResult:null,
    tbSearchKeyword:'',tbSearchPlatform:'',tbSearching:false,tbFetching:false,tbThirdResults:[],tbFetchResult:null,
    pptChapterCache:{},pptActiveChapter:0,pptSlideIdx:0,pptGenerating:false,pptFullscreen:false,pptFullscreenIdx:0
  }},
  computed:{
    categories:function(){var c=this.allCourses.map(function(x){return x.cat});return [...new Set(c)].sort()},
    navCats:function(){var l=[{key:'all',label:'全部课程'}];var c=this.categories;for(var i=0;i<c.length;i++)l.push({key:c[i],label:c[i]});return l},
    moocTabName:function(){return this.moocTab==='all'?'全部课程':this.moocTab},
    filteredCourses:function(){var s=this;var l=this.allCourses;if(this.moocTab!=='all')l=l.filter(function(c){return c.cat===s.moocTab});if(this.searchQuery){var kw=this.searchQuery.toLowerCase();l=l.filter(function(c){return c.name.toLowerCase().indexOf(kw)>=0||c.school.toLowerCase().indexOf(kw)>=0||c.description.toLowerCase().indexOf(kw)>=0})}return l},
    totalResources:function(){var t=0;this.allCourses.forEach(function(c){t+=(c.resourceCount||0)});return t},
    heroStats:function(){return [{value:this.allCourses.length,label:'精品课程'},{value:this.totalResources,label:'教学资源'},{value:this.categories.length,label:'学科分类'}]},
    currentChapterText:function(){var ch=this.readerChapters[this.readerChapter];return ch?ch.content:'<p class="hub-textbook-placeholder">加载中...</p>'},
    availableCategories:function(){var c=this.categories;if(c.indexOf('其他')<0)c.push('其他');return c},
    currentChapterSlides:function(){
      if (this.pptActiveChapter === null || this.pptActiveChapter === undefined) return [];
      var cid = this.currentCourse ? (this.currentCourse.id || this.currentCourse._raw_id || '') : '';
      var cache = this.pptChapterCache[cid];
      return (cache && cache[this.pptActiveChapter] !== undefined) ? cache[this.pptActiveChapter] : [];
    }
  },
  created:function(){var s=this;s.loadCourses();s.loadThirdPlatforms()},
  beforeDestroy:function(){this.cs();this.stopTTS();if(window.speechSynthesis)window.speechSynthesis.cancel()},
  methods:{
    coverGradient:G,fmtNum:function(n){return n>=10000?(n/10000).toFixed(1)+'万':''+n},
    statusText:function(s){return s==='ongoing'?'正在进行':s==='upcoming'?'即将开始':'已结束'},
    onSearch:function(){},
    openTextbookExternal:function(){
      if(this.currentTextbookPlatform){
        window.open(this.currentTextbookPlatform, '_blank');
      }
    },
    openCourse:function(c){
      this.currentCourse = c;
      this.currentChapters = c.chapterList || [];
      this.courseDetailVisible = true;
      this.detailTab = 'intro';
      this.activeChapters = 0;
      this.embedDialogVisible = false;
      this.embedCode = '';
      this.tbIframeLoaded = false;
      // 为每章独立生成PPT
      var cid = c.id || c._raw_id || '';
      this.pptActiveChapter = 0;
      this.pptSlideIdx = 0;
      this.pptChapterCache[cid] = buildAllChapterPPTs(c);
    },
    /* ═══ 嵌入代码 ─ 一键获取iframe嵌入代码 ═══ */
    getEmbedCode: async function(course) {
      this.$set(this, 'embedDialogVisible', true);
      this.$set(this, 'embedCode', '');
      var courseId = course.id || course._code || '';
      try {
        var res = await courseApi.getEmbedInfo(courseId);
        if (res && res.code === 1 && res.response) {
          this.$set(this, 'embedCode', res.response.iframeCode || '');
          this.$set(this, 'embedInfo', res.response);
        } else {
          // fallback: 自动生成嵌入代码
          this.$set(this, 'embedCode', '<iframe src="' + window.location.origin + '/api/admin/courses/embed-viewer?courseId=' + courseId + '&resourceType=ppt&resourceIndex=0" width="100%" height="600" frameborder="0" allowfullscreen></iframe>');
        }
      } catch(e) {
        this.$set(this, 'embedCode', '<iframe src="' + window.location.origin + '/api/admin/courses/embed-viewer?courseId=' + courseId + '&resourceType=ppt&resourceIndex=0" width="100%" height="600" frameborder="0" allowfullscreen></iframe>');
      }
    },
    previewEmbed: function() {
      var courseId = this.currentCourse ? (this.currentCourse.id || this.currentCourse._code || '') : '';
      var url = window.location.origin + '/api/admin/courses/embed-viewer?courseId=' + courseId + '&resourceType=ppt&resourceIndex=0';
      window.open(url, '_blank');
    },
    copyEmbedCode: function() {
      var ta = document.createElement('textarea');
      ta.value = this.embedCode;
      ta.style.position = 'fixed'; ta.style.left = '-9999px';
      document.body.appendChild(ta); ta.select();
      document.execCommand('copy');
      document.body.removeChild(ta);
      if (this.$message) this.$message.success('已复制嵌入代码到剪贴板');
    },
    previewPPT: function(ppt, idx) {
      if (!this.currentCourse) return;
      var rawUrl = ppt.url || ppt._sourceUrl || '';
      var realUrl = (rawUrl && rawUrl !== '#' && (rawUrl.startsWith('http://') || rawUrl.startsWith('https://'))) ? rawUrl : '';
      this.pptPreviewItem = ppt;
      this.pptPreviewTitle = '📄 ' + ppt.name;
      this.pptExternalUrl = realUrl;
      this.pptExternalIframe = '';
      this.pptShowWebCard = false;
      this.pptExternalPlatform = '';
      // 无真实URL → 本地幻灯片渲染
      if (!realUrl) {
        this.pptPreviewMode = 'local';
        this.pptSlidesHtml = buildPptSlides(ppt, this.currentCourse);
        this.pptSlideIdx = 0;
        this.pptPreviewVisible = true;
        return;
      }
      // 有URL → 优先代理下载到本地，然后使用合适的查看器
      this.pptPreviewVisible = true;
      this.pptPreviewLoading = true;
      this.downloadAndShow(realUrl, ppt.name || '课件', 'ppt');
    },
    // ═══ 每章独立PPT方法 ═══
    openChapterPPT: function(chIdx) { this.pptActiveChapter = chIdx; this.pptSlideIdx = 0; },
    onPptChapterChange: function() { this.pptSlideIdx = 0; },
    getChapterSlideCount: function(chIdx) {
      var cid = this.currentCourse ? (this.currentCourse.id || this.currentCourse._raw_id || '') : '';
      var cache = this.pptChapterCache[cid];
      return (cache && cache[chIdx]) ? cache[chIdx].length : 0;
    },
    regenerateChapterPPT: function(chIdx) {
      var cid = this.currentCourse ? (this.currentCourse.id || this.currentCourse._raw_id || '') : '';
      if (!this.pptChapterCache[cid]) this.pptChapterCache[cid] = {};
      this.pptChapterCache[cid][chIdx] = buildSingleChapterPPT(this.currentCourse, chIdx);
      this.$message.success('已重新生成第'+(chIdx+1)+'章课件');
    },
    regenerateAllChapterPPTs: function() {
      var cid = this.currentCourse ? (this.currentCourse.id || this.currentCourse._raw_id || '') : '';
      this.pptChapterCache[cid] = buildAllChapterPPTs(this.currentCourse);
      var total = 0; var cache = this.pptChapterCache[cid];
      for (var k in cache) total += cache[k].length;
      this.$message.success('已重新生成全部课件，共'+total+'页');
    },
    previewPPTSlide: function (idx) {
      this.pptPreviewItem = { name: (this.currentCourse._chTitles||[])[this.pptActiveChapter] || '' };
      this.pptPreviewTitle = '课件放映 · 第 ' + (idx + 1) + ' / ' + this.currentChapterSlides.length + ' 页';
      this.pptPreviewMode = 'auto';
      this.pptSlideIdx = idx;
      this.pptPreviewVisible = true;
    },
    getPptaType: function(slide) {
      if (!slide) return 'keypoint';
      var t = slide.type || '';
      if (t === 'cover') return 'cover';
      if (t === 'goal') return 'goal';
      if (t === 'toc') return 'toc';
      if (t === 'keypoint' || t === 'chapter') return 'keypoint';
      if (t === 'scenario' || t === 'lesson') return 'scenario';
      if (t === 'exercise') return 'exercise';
      if (t === 'summary') return 'summary';
      if (t === 'end') return 'end';
      return 'keypoint';
    },
    previewPPTFullscreen: function () {
      this.pptFullscreenIdx = this.pptSlideIdx;
      this.pptFullscreen = true;
      var self = this;
      this.$nextTick(function () {
        var el = document.querySelector('.hub-ppt-fullscreen');
        if (el && el.focus) el.focus();
      });
    },
    togglePptFullscreen: function () {
      this.pptFullscreenIdx = this.pptSlideIdx;
      this.pptFullscreen = !this.pptFullscreen;
      if (this.pptFullscreen) {
        var self = this;
        this.$nextTick(function () {
          var el = document.querySelector('.hub-ppt-fullscreen');
          if (el && el.focus) el.focus();
        });
      }
    },
    downloadPptx: async function () {
      var course = this.currentCourse;
      if (!course || !this.currentChapterSlides.length) return;
      try {
        var res = await fetch('/api/admin/courses/' + (course.id || '') + '/generate-ppts', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'token': localStorage.getItem('token') || '' },
          body: JSON.stringify({ slides: this.currentChapterSlides, download: true })
        }).then(function (r) { return r.json(); });
        if (res && res.code === 1 && res.response && res.response.pptxUrl) {
          window.open(res.response.pptxUrl, '_blank');
          this.$message.success('PPTX下载已开始');
        } else {
          this.$message.warning('PPTX生成中，请稍后...');
        }
      } catch (e) {
        this.$message.warning('下载功能需要后端支持，已复制课件内容可粘贴到PowerPoint');
      }
    },
    playVideo:function(video,idx){
      var realUrl = video.url || video._sourceUrl || '';
      var isReal = realUrl && (realUrl.startsWith('http://') || realUrl.startsWith('https://'));
      // ⭐ B站视频：使用DPlayer + flv.js 直接播放FLV流
      if (isReal && this.isBilibiliUrl(realUrl)) {
        this.currentVideo = video;
        this.currentVideoUrl = realUrl;
        this.videoDialogTitle = '🎬 ' + video.name;
        this.videoEmbedUrl = '';
        this.videoPlayerVisible = true;
        this.simVideoPaused = true;
        return;
      }
      // ⭐ 网易公开课 open.163.com → 原生 video 播放（走后端解析直链代理）
      if (isReal && realUrl.indexOf('open.163.com') !== -1) {
        this.currentVideo = video;
        this.currentVideoUrl = realUrl;
        this.videoDialogTitle = '🎬 ' + video.name;
        this.videoEmbedUrl = '';
        this.videoNativeSrc = realUrl;
        this.videoPlayerVisible = true;
        this.simVideoPaused = true;
        return;
      }
      // ⭐ 其他真实视频：iframe内嵌播放
      if (isReal) {
        this.currentVideo = video;
        this.currentVideoUrl = realUrl;
        this.videoDialogTitle = '🎬 ' + video.name;
        this.videoEmbedUrl = this.buildEmbedUrl(realUrl);
        this.videoNativeSrc = '';
        this.videoPlayerVisible = true;
        this.simVideoPaused = true;
        return;
      }
      // 回退到模拟播放器
      this.currentVideo=video;this.videoDialogTitle='🎬 '+video.name;this.videoPlayerVisible=true;this.simVideoPaused=false
      this.videoEmbedUrl='';this.videoNativeSrc='';
      var d=video.duration||'420';var p=d.split(':');this.simVideoDuration=p.length===2?parseInt(p[0])*60+parseInt(p[1]):parseInt(d)||420
      this.simVideoProgress=0;this.simVideoElapsed=0;this.simVideoBuffered=25+Math.random()*15
      var chNames=(this.currentCourse&&this.currentCourse._chTitles)?this.currentCourse._chTitles:[]
      this.videoContents=buildVideoContents(this.currentCourse,video.name,chNames);this.videoContentIdx=-1;this.simVideoMuted=false
      this.startVideoTimer()
    },
    // ⭐ 解析第三方URL为内嵌播放地址
    buildEmbedUrl: function (url) {
      // B站: https://www.bilibili.com/video/av55114968/?p=1 → embed player
      var biliMatch = url.match(/bilibili\.com\/video\/(?:av)?(\d+)/i);
      if (biliMatch) {
        var aid = biliMatch[1];
        var pageMatch = url.match(/[?&]p=(\d+)/i);
        var page = pageMatch ? pageMatch[1] : '1';
        return '//player.bilibili.com/player.html?aid=' + aid + '&page=' + page + '&high_quality=1&autoplay=0';
      }
      // B站 BV号格式
      var bvMatch = url.match(/bilibili\.com\/video\/(BV[\w]+)/i);
      if (bvMatch) {
        var bvid = bvMatch[1];
        var pMatch = url.match(/[?&]p=(\d+)/i);
        var p2 = pMatch ? pMatch[1] : '1';
        return '//player.bilibili.com/player.html?bvid=' + bvid + '&page=' + p2 + '&high_quality=1&autoplay=0';
      }
      // YouTube
      var ytMatch = url.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([\w-]+)/i);
      if (ytMatch) {
        return '//www.youtube.com/embed/' + ytMatch[1] + '?autoplay=0';
      }
      // iCourse163 / 中国大学MOOC
      if (url.indexOf('icourse163.org') >= 0 || url.indexOf('icourse163') >= 0) {
        return url; // MOOC网站本身就可以iframe
      }
      // 其他URL直接尝试嵌入
      return url;
    },
    // ═══ B站播放器辅助方法 ═══
    isBilibiliUrl:function(url){if(!url)return false;return url.indexOf('bilibili.com/video/')!==-1||url.indexOf('b23.tv/')!==-1},
    // ⭐ 网易公开课 open.163.com → 后端解析直链播放代理地址
    neteasePlayUrl:function(url){if(!url)return null;var s=String(url);if(s.indexOf('open.163.com')===-1)return null;var pid=(s.match(/[?&]pid=([A-Za-z0-9]+)/i)||[])[1];var mid=(s.match(/[?&]mid=([A-Za-z0-9]+)/i)||[])[1];if(!pid||!mid)return null;return '/api/admin/netease/play?pid='+pid+'&mid='+mid},
    isExternalUrl:function(url){return url&&(url.startsWith('http://')||url.startsWith('https://'))},
    onBilibiliReady:function(info){console.log('B站视频就绪:',info&&info.title)},
    onBilibiliError:function(err){this.$message.warning('B站直连播放失败，已切换到嵌入模式');if(err&&err.message)console.warn('B站播放器错误:',err.message)},
    onBilibiliFallbackIframe:function(url){this.videoEmbedUrl=url;this.$forceUpdate()},
    simulateVideoPlay:function(){this.simVideoPaused=!this.simVideoPaused;if(this.simVideoPaused){this.cs();this.stopTTS()}else{this.startVideoTimer()}},
    // TTS 语音合成
    speakContent:function(text){if(this.simVideoMuted||!window.speechSynthesis||!text)return;this.stopTTS();var u=new SpeechSynthesisUtterance(text);u.lang='zh-CN';u.rate=1.1;u.pitch=1.0;u.volume=0.9;var voices=window.speechSynthesis.getVoices();var v=voices.find(function(x){return x.lang.startsWith('zh')&&x.name.includes('Tingting')})||voices.find(function(x){return x.lang.startsWith('zh-CN')});if(v)u.voice=v;window.speechSynthesis.speak(u)},
    stopTTS:function(){if(window.speechSynthesis)window.speechSynthesis.cancel()},
    startVideoTimer:function(){
      var s=this;this.cs();var vc=this.videoContents.length||1
      var segLen=Math.floor(s.simVideoDuration/vc)
      // 初始化第一段语音
      if(s.videoContentIdx<0&&vc>0){s.videoContentIdx=0;var c0=s.videoContents[0];s.speakContent(c0.title+'。'+c0.detail)}
      this.simVideoTimer=setInterval(function(){
        if(s.seeking||s.thumbDragging)return
        if(s.simVideoElapsed>=s.simVideoDuration){s.simVideoPaused=true;s.cs();s.simVideoProgress=100;s.stopTTS();return}
        s.simVideoElapsed+=1;s.simVideoProgress=Math.round((s.simVideoElapsed/s.simVideoDuration)*100)
        if(s.simVideoProgress>s.simVideoBuffered-5&&s.simVideoBuffered<98)s.simVideoBuffered=Math.min(98,s.simVideoBuffered+Math.random()*8)
        var idx=Math.min(Math.floor(s.simVideoElapsed/segLen),vc-1)
        if(idx!==s.videoContentIdx){s.videoContentIdx=idx;s.stopTTS();var c=s.videoContents[idx];s.speakContent(c.title+'。'+c.detail)}
      },500)
    },
    cs:function(){if(this.simVideoTimer){clearInterval(this.simVideoTimer);this.simVideoTimer=null}},
    // ── 进度条拖动 ──
    seekClick:function(e){if(this.thumbDragging)return;var r=e.currentTarget.getBoundingClientRect();var pct=Math.max(0,Math.min(100,((e.clientX-r.left)/r.width)*100));this.seekTo(pct)},
    seekStart:function(e){this.seeking=true},
    seekMove:function(e){if(!this.seeking)return;var r=e.currentTarget.getBoundingClientRect();var pct=Math.max(0,Math.min(100,((e.clientX-r.left)/r.width)*100));this.simVideoProgress=pct;this.simVideoElapsed=Math.round(pct/100*this.simVideoDuration)},
    seekEnd:function(){if(!this.seeking)return;this.seeking=false;this.seekTo(this.simVideoProgress)},
    thumbDragStart:function(){this.thumbDragging=true;var s=this;var onUp=function(){s.thumbDragging=false;s.seekTo(s.simVideoProgress);document.removeEventListener('mousemove',onMove);document.removeEventListener('mouseup',onUp)}
    var onMove=function(e){var bar=s.$refs.mp4Progress;if(!bar)return;var r=bar.getBoundingClientRect();var pct=Math.max(0,Math.min(100,((e.clientX-r.left)/r.width)*100));s.simVideoProgress=pct;s.simVideoElapsed=Math.round(pct/100*s.simVideoDuration)}
    document.addEventListener('mousemove',onMove);document.addEventListener('mouseup',onUp)},
    seekTo:function(pct){this.simVideoProgress=pct;this.simVideoElapsed=Math.round(pct/100*this.simVideoDuration);var vc=this.videoContents.length||1;var segLen=Math.floor(this.simVideoDuration/vc);var idx=Math.min(Math.floor(this.simVideoElapsed/segLen),vc-1);if(idx!==this.videoContentIdx){this.videoContentIdx=idx;this.stopTTS();var c=this.videoContents[idx];this.speakContent(c.title+'。'+c.detail)}if(!this.simVideoPaused)this.startVideoTimer()},
    closeVideoPlayer:function(done){this.cs();this.stopTTS();this.videoEmbedUrl='';this.currentVideoUrl='';this.videoNativeSrc='';if(this.$refs.bilibiliPlayer&&this.$refs.bilibiliPlayer.destroyPlayer){this.$refs.bilibiliPlayer.destroyPlayer()};done()},
    formatTime:function(s){var m=Math.floor(s/60),se=s%60;return m+':'+(se<10?'0':'')+se},
    playLesson:function(l){if(l.type==='video')this.playVideo(l,0);else this.previewPPT(l,0)},
    openReader: function(book) {
      var realUrl = book.url || book._sourceUrl || '';
      // 排除占位符URL
      if (!realUrl || realUrl === '#' || !realUrl.startsWith('http')) realUrl = '';
      // 无真实URL → 本地阅读器（内置内容）
      if (!realUrl) {
        this.readerIsExternal = false;
        this.readerShowWebCard = false;
        this.readerBook = book; this.readerChapter = 0; this.readerVisible = true;
        var s = this, count = book.chapters || 8, titles = this.currentCourse && this.currentCourse._chTitles ? this.currentCourse._chTitles : [];
        var course = this.currentCourse; var cn = course ? course.name : '';
        this.readerChapters = Array.from({length: count}, function(_, i) {
          var t = titles[i] || '第' + (i + 1) + '章';
          return { title: t, content: buildChapterContent(book, i, t, cn, course) };
        });
        return;
      }
      // ★ 云阅读器URL（yunzhan365等）→ 直接iframe嵌入
      var isCloudReader = realUrl.indexOf('yunzhan365.com') !== -1 || realUrl.indexOf('book.') !== -1;
      if (isCloudReader) {
        this.readerBook = book;
        this.readerVisible = true;
        this.readerLoading = false;
        this.readerIsExternal = true;
        this.readerShowWebCard = false;
        this.readerExternalUrl = realUrl;
        this.readerIframeUrl = realUrl;
        this.readerLocalFailed = false;
        return;
      }
      // ★ PDF URL → 直接iframe嵌入
      if (realUrl.toLowerCase().endsWith('.pdf')) {
        this.readerBook = book;
        this.readerVisible = true;
        this.readerLoading = false;
        this.readerIsExternal = true;
        this.readerShowWebCard = false;
        this.readerExternalUrl = realUrl;
        this.readerIframeUrl = realUrl + '#view=FitH';
        this.readerLocalFailed = false;
        return;
      }
      // ★ 其他URL → 尝试代理下载
      this.readerBook = book;
      this.readerVisible = true;
      this.readerLoading = true;
      this.readerIsExternal = true;
      this.readerShowWebCard = false;
      this.readerExternalUrl = realUrl;
      var self = this;
      fetch('/api/admin/resource/proxy', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'token': localStorage.getItem('token') || ''
        },
        body: JSON.stringify({ url: realUrl, fileName: (book.name || 'resource') + '.pdf' })
      }).then(function(r) { return r.json(); })
      .then(function(result) {
        if (result.code === 1 && result.response && result.response.localUrl) {
          var localUrl = result.response.localUrl;
          if (localUrl.toLowerCase().endsWith('.pdf')) {
            self.readerIframeUrl = localUrl + '?disposition=inline#view=FitH';
          } else {
            self.readerIframeUrl = localUrl + '?disposition=inline';
          }
          self.readerLoading = false;
          self.readerIsExternal = true;
          self.$forceUpdate();
        } else {
          self.legacyOpenReader(book, realUrl);
        }
      }).catch(function() {
        self.legacyOpenReader(book, realUrl);
      });
    },
    // 回退：远程URL无法代理时直接预览
    legacyOpenReader: function(book, realUrl) {
      var self = this;
      self.readerBook = book;
      self.readerVisible = true;
      self.readerLoading = true;
      self.readerIsExternal = true;
      self.readerShowWebCard = false;
      self.readerExternalUrl = realUrl;
      var urlLower = realUrl.toLowerCase();

      // 策略1: 本地代理的PDF → 直接嵌入
      if (realUrl.indexOf('/api/admin/resource/download/') >= 0 || realUrl.indexOf('/api/admin/third-party/download/') >= 0) {
        if (urlLower.endsWith('.pdf')) {
          this.readerIframeUrl = realUrl + '#view=FitH';
        } else {
          this.readerIframeUrl = 'https://view.officeapps.live.com/op/embed.aspx?src=' + encodeURIComponent(window.location.origin + realUrl);
        }
        this.readerLoading = false;
        this.$forceUpdate();
        return;
      }

      // 策略2: CloudFront CDN PDF (OpenStax等) → Office Online Viewer（因为CloudFront可能阻止直接iframe）
      if (realUrl.indexOf('cloudfront.net') >= 0 || realUrl.indexOf('openstax') >= 0) {
        this.readerIframeUrl = 'https://view.officeapps.live.com/op/embed.aspx?src=' + encodeURIComponent(realUrl);
        this.readerLoading = false;
        this.$forceUpdate();
        return;
      }

      // 策略3: Google Drive → 用Google嵌入查看器
      if (realUrl.indexOf('drive.google.com') >= 0 || realUrl.indexOf('docs.google.com') >= 0) {
        // Google Drive: 提取file ID并构建嵌入URL
        var match = realUrl.match(/\/d\/([a-zA-Z0-9_-]+)/) || realUrl.match(/id=([a-zA-Z0-9_-]+)/);
        if (match) {
          this.readerIframeUrl = 'https://drive.google.com/file/d/' + match[1] + '/preview';
        } else {
          this.readerIframeUrl = realUrl;
        }
        this.readerLoading = false;
        this.$forceUpdate();
        return;
      }

      // 策略4: yunzhan365.com 云展网（在线翻页书）→ 直接嵌入
      if (realUrl.indexOf('yunzhan365.com') >= 0) {
        this.readerIframeUrl = realUrl;
        this.readerLoading = false;
        this.$forceUpdate();
        return;
      }

      // 策略5: PDF直接嵌入
      if (urlLower.endsWith('.pdf')) {
        this.readerIframeUrl = realUrl + '#view=FitH';
        this.readerLoading = false;
        this.$forceUpdate();
        return;
      }

      // 策略6: 其他文档用Office Online Viewer内嵌预览
      this.readerIframeUrl = 'https://view.officeapps.live.com/op/embed.aspx?src=' + encodeURIComponent(realUrl);
      this.readerLoading = false;
      this.$forceUpdate();
    },
    // ═══ URL工具函数 ═══
    detectPlatform:function(url){
      var h=(url||'').toLowerCase();
      if(h.indexOf('github.com')>=0)return 'GitHub';
      if(h.indexOf('bilibili.com')>=0)return 'B站';
      if(h.indexOf('douban.com')>=0)return '豆瓣图书';
      if(h.indexOf('icourse163.org')>=0)return '中国大学MOOC';
      if(h.indexOf('xuetangx.com')>=0)return '学堂在线';
      if(h.indexOf('chaoxing.com')>=0)return '超星学习通';
      if(h.indexOf('mooc1')>=0)return 'MOOC平台';
      if(h.indexOf('chinesemooc')>=0)return '华文慕课';
      if(h.indexOf('mofile.net')>=0)return 'MOOC资源站';
      if(h.indexOf('openstax')>=0)return 'OpenStax';
      if(h.indexOf('eol.nwafu')>=0)return '教育平台';
      if(h.indexOf('waitang.com')>=0)return '外唐教程';
      if(h.indexOf('pages.cs.wisc.edu')>=0)return '教材官网';
      if(h.indexOf('sili-math')>=0)return '数学知识库';
      if(h.indexOf('bananaspace')>=0)return 'Banana空间';
      if(h.indexOf('linux.do')>=0)return '技术论坛';
      return '第三方平台'
    },
    // ═══ 多级资源查看策略 ═══
    // 核心方法：下载远程资源到本地 → 选择合适的查看器展示
    downloadAndShow: async function(remoteUrl, name, type) {
      var self = this;
      if (!remoteUrl || remoteUrl === '#' || !remoteUrl.startsWith('http')) {
        console.warn('downloadAndShow: invalid URL:', remoteUrl);
        return;
      }
      try {
        // 判断URL扩展名
        var urlLower = remoteUrl.toLowerCase();
        var isPdfUrl = urlLower.endsWith('.pdf');
        var isPptUrl = urlLower.endsWith('.ppt') || urlLower.endsWith('.pptx');
        var fileName = name;

        // 尝试不同扩展名下载
        var extensions = isPdfUrl ? ['pdf'] : (isPptUrl ? ['pptx', 'ppt', 'pdf'] : ['pdf', 'html']);
        if (!fileName.match(/\.\w{2,5}$/)) fileName += '.' + extensions[0];

        // 尝试代理下载
        var localUrl = null;
        var downloadResult = null;
        for (var ei = 0; ei < extensions.length; ei++) {
          var tryName = fileName.replace(/\.\w{2,5}$/, '.' + extensions[ei]);
          try {
            var res = await resourceApi.proxyResource(remoteUrl, tryName);
            if (res && res.code === 1 && res.response && res.response.localUrl) {
              localUrl = res.response.localUrl;
              downloadResult = res.response;
              break;
            }
          } catch (e) { /* 继续尝试下一个扩展名 */ }
        }

        if (type === 'ppt') {
          this.pptPreviewLoading = false;
          if (localUrl) {
            this.pptExternalIframe = localUrl;
            this.pptShowWebCard = false;
          } else {
            // 下载失败 → 显示外链卡片
            this.pptExternalPlatform = this.detectPlatform(remoteUrl);
            this.pptExternalUrl = remoteUrl;
            var gvUrl = this.getPreferredViewerUrl(remoteUrl);
            this.pptExternalIframe = gvUrl || remoteUrl;
            this.pptShowWebCard = !gvUrl;
          }
        } else if (type === 'textbook') {
          this.readerLoading = false;
          if (localUrl) {
            var ct2 = (downloadResult && downloadResult.contentType) || '';
            this.readerIframeUrl = localUrl;
            this.readerIsExternal = true;
            this.readerShowWebCard = false;
          } else {
            // 下载失败 → 外链卡片
            this.readerExternalUrl = remoteUrl;
            this.readerExternalPlatform = this.detectPlatform(remoteUrl);
            var gv2 = this.getPreferredViewerUrl(remoteUrl);
            if (gv2) {
              this.readerIframeUrl = gv2;
              this.readerIsExternal = true;
              this.readerShowWebCard = false;
            } else {
              this.readerIsExternal = true;
              this.readerShowWebCard = true;
            }
          }
        }
        this.$forceUpdate();
      } catch (e) {
        // 捕获异常，显示外链卡片
        console.warn('downloadAndShow error:', e);
        if (type === 'ppt') {
          this.pptPreviewLoading = false;
          this.pptExternalUrl = remoteUrl;
          this.pptExternalPlatform = this.detectPlatform(remoteUrl);
          this.pptExternalIframe = this.getPreferredViewerUrl(remoteUrl) || remoteUrl;
          this.pptShowWebCard = !!this.pptExternalUrl;
        } else if (type === 'textbook') {
          this.readerLoading = false;
          this.readerExternalUrl = remoteUrl;
          this.readerExternalPlatform = this.detectPlatform(remoteUrl);
          this.readerIsExternal = true;
          this.readerShowWebCard = true;
        }
        this.$forceUpdate();
      }
    },

    // Microsoft Office Online Viewer — 使用Edge/Office在线查看器
    getPreferredViewerUrl: function(url) {
      if (!url) return '';
      return 'https://view.officeapps.live.com/op/embed.aspx?src=' + encodeURIComponent(url);
    },

    copyToClipboard:function(text){
      if(!text)return;
      var ta=document.createElement('textarea');ta.value=text;
      ta.style.position='fixed';ta.style.left='-9999px';document.body.appendChild(ta);
      ta.select();document.execCommand('copy');document.body.removeChild(ta);
      this.$message({type:'success',message:'链接已复制到剪贴板',duration:1500})
    },
    onReaderIframeLoad:function(){
      // iframe加载完成，清除错误状态
      this.readerIframeError=false
    },
    onReaderIframeError:function(){
      // iframe加载失败，显示错误提示
      this.readerIframeError=true
    },
    // ── 课程导入与数据加载 ──
    loadThirdPlatforms: function () {
      var self = this
      thirdPartyApi.getPlatforms().then(function (res) {
        if (res && res.code === 1) { self.thirdPlatforms = res.response || [] }
      }).catch(function () {})
    },
    isUrl: function (str) { return /^https?:\/\//.test((str || '').trim()) },
    // ── 第三方搜索（PPT） ──
    searchThirdPpt: function () {
      var self = this
      var kw = this.pptSearchKeyword.trim()
      if (!kw) return this.$message.warning('请输入关键词或粘贴URL')
      this.pptSearching = true; this.pptThirdResults = []
      thirdPartyApi.search(kw, this.pptSearchPlatform, 'ppt').then(function (res) {
        if (res && res.code === 1 && res.response) {
          self.pptThirdResults = res.response.items || []
          if (!self.pptThirdResults.length) self.$message.info('未找到PPT资源')
        }
      }).catch(function () { self.$message.error('搜索失败') }).finally(function () { self.pptSearching = false })
    },
    fetchThirdPpt: function () {
      var self = this; this.pptFetching = true; this.pptFetchResult = null
      thirdPartyApi.fetchResource(this.pptSearchKeyword.trim(), false).then(function (res) {
        if (res && res.code === 1) {
          self.pptFetchResult = res.response;
          // 如果是预览模式（非下载），直接打开Office在线预览
          if (res.response.previewMode === 'online' && res.response.previewUrl) {
            self.pptExternalIframe = res.response.previewUrl;
            self.pptShowWebCard = false;
            self.pptPreviewVisible = true;
            self.pptPreviewTitle = '📄 ' + (res.response.fileName || '第三方PPT');
          }
          self.$message.success('PPT资源获取成功')
        }
        else { self.$message.error(res ? res.message : '获取失败') }
      }).catch(function () { self.$message.error('获取失败') }).finally(function () { self.pptFetching = false })
    },
    // ── 第三方搜索（电子课本） ──
    searchThirdTb: function () {
      var self = this
      var kw = this.tbSearchKeyword.trim()
      if (!kw) return this.$message.warning('请输入关键词或粘贴URL')
      this.tbSearching = true; this.tbThirdResults = []
      thirdPartyApi.search(kw, this.tbSearchPlatform, 'pdf').then(function (res) {
        if (res && res.code === 1 && res.response) {
          self.tbThirdResults = res.response.items || []
          if (!self.tbThirdResults.length) self.$message.info('未找到电子课本资源')
        }
      }).catch(function () { self.$message.error('搜索失败') }).finally(function () { self.tbSearching = false })
    },
    fetchThirdTb: function () {
      var self = this; this.tbFetching = true; this.tbFetchResult = null
      thirdPartyApi.fetchResource(this.tbSearchKeyword.trim(), false).then(function (res) {
        if (res && res.code === 1) { self.tbFetchResult = res.response; self.$message.success('电子课本获取成功') }
        else { self.$message.error(res ? res.message : '获取失败') }
      }).catch(function () { self.$message.error('获取失败') }).finally(function () { self.tbFetching = false })
    },
    // ── 点击搜索结果项获取资源 ──
    fetchThirdItem: function (item, type) {
      var self = this
      if (item.type === 'search_link') { window.open(item.url, '_blank'); return }
      if (type === 'ppt') { this.pptFetching = true } else { this.tbFetching = true }
      thirdPartyApi.fetchResource(item.url, false).then(function (res) {
        if (res && res.code === 1) {
          var result = res.response
          if (type === 'ppt') { self.pptFetchResult = result } else { self.tbFetchResult = result }
          // 预览模式 → 直接打开
          if (result.previewMode === 'online' && result.previewUrl) {
            if (type === 'ppt') {
              self.pptExternalIframe = result.previewUrl;
              self.pptShowWebCard = false;
              self.pptPreviewVisible = true;
              self.pptPreviewTitle = '📄 ' + (result.fileName || '第三方PPT');
            } else {
              self.readerIframeUrl = result.previewUrl;
              self.readerIsExternal = true;
              self.readerShowWebCard = false;
              self.readerVisible = true;
              self.readerBook = { name: result.fileName || '第三方电子课本' };
            }
          }
          self.$message.success('资源获取成功')
        } else { self.$message.warning('无法直接获取，请尝试原始链接下载'); window.open(item.url, '_blank') }
      }).catch(function () { self.$message.warning('获取失败，打开原始链接...'); window.open(item.url, '_blank') })
      .finally(function () { if (type === 'ppt') { self.pptFetching = false } else { self.tbFetching = false } })
    },
    // ── 预览已获取的第三方文件 ──
    previewThirdFile: function (fetchResult, type) {
      if (!fetchResult) return
      // 预览模式 → 用返回的预览URL
      if (fetchResult.previewMode === 'online' && fetchResult.previewUrl) {
        if (type === 'ppt') {
          this.pptExternalIframe = fetchResult.previewUrl
          this.pptShowWebCard = false
          this.pptPreviewVisible = true
          this.pptPreviewTitle = '📄 ' + (fetchResult.fileName || '第三方资源')
        } else {
          this.readerIframeUrl = fetchResult.previewUrl
          this.readerIsExternal = true
          this.readerShowWebCard = false
          this.readerVisible = true
          this.readerBook = { name: fetchResult.fileName || '第三方资源' }
        }
        return
      }
      // 下载模式 → 用本地localUrl
      if (!fetchResult.localUrl) return
      if (type === 'ppt') {
        this.pptExternalIframe = fetchResult.localUrl + '?disposition=inline'
        this.pptShowWebCard = false
        this.pptPreviewVisible = true
        this.pptPreviewTitle = '📄 ' + (fetchResult.fileName || '第三方PPT')
      } else {
        this.readerIframeUrl = fetchResult.localUrl + '?disposition=inline'
        this.readerIsExternal = true
        this.readerShowWebCard = false
        this.readerVisible = true
        this.readerBook = { name: fetchResult.fileName || '第三方电子课本' }
      }
    },
    // ── 将第三方资源加入当前课程 ──
    addThirdToCourse: function (fetchResult, type) {
      if (!this.currentCourse) return
      if (type === 'ppt') {
        if (!this.currentCourse.pptList) this.currentCourse.pptList = []
        this.currentCourse.pptList.push({
          name: fetchResult.fileName || '第三方PPT',
          url: fetchResult.localUrl,
          _sourceUrl: fetchResult.url,
          pages: '--', size: this.formatFileSize(fetchResult.fileSize),
          platform: fetchResult.platform
        })
        this.$message.success('已添加到课程PPT列表')
      } else {
        if (!this.currentCourse.textbookResources) this.currentCourse.textbookResources = []
        this.currentCourse.textbookResources.push({
          name: fetchResult.fileName || '第三方电子课本',
          url: fetchResult.localUrl,
          _sourceUrl: fetchResult.url,
          author: fetchResult.platform || '第三方平台',
          publisher: '在线资源', edition: '最新版',
          platform: fetchResult.platform
        })
        this.$message.success('已添加到电子课本列表')
      }
      this.pptFetchResult = null; this.tbFetchResult = null
    },
    formatFileSize: function (bytes) {
      if (!bytes || bytes < 0) return '未知'
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
    },
    loadCourses: async function () {
      // 优先从后端API加载真实课程数据
      try {
        var res = await courseApi.list()
        if (res && res.code === 1 && Array.isArray(res.response) && res.response.length > 0) {
          this.allCourses = res.response.map(function (rc) {
            var chTitles = (rc.chapterTitles || (rc.chapters ? Object.keys(rc.chapters) : []) || [])
            // 将章节数据转为前端chapterList格式
            var chList = []
            var rawChapters = rc.chapters || {}
            chTitles.forEach(function (title, ci) {
              var chData = rawChapters[title] || { points: [], lessons: [] }
              var rawLessons = chData.lessons || []
              // ★ 如果课程有知识点但没有预置课时，根据知识点自动生成课时
              if (rawLessons.length === 0 && (chData.points || []).length > 0) {
                var nPoints = chData.points.length
                // 每个知识点生成一个video课时
                for (var pi2 = 0; pi2 < nPoints; pi2++) {
                  rawLessons.push({
                    name: title + ' · 课时' + (pi2 + 1),
                    type: 'video',
                    iconClass: 'el-icon-video-play',
                    desc: (15 + pi2 * 4) + ':00',
                    btnIcon: 'el-icon-video-play',
                    btnText: '观看',
                    _chapterIdx: ci
                  })
                }
                // 再加一个PPT课时
                rawLessons.push({
                  name: title + ' · 课件PPT',
                  type: 'ppt',
                  iconClass: 'el-icon-presentation',
                  desc: 'PPT课件',
                  btnIcon: 'el-icon-view',
                  btnText: '预览',
                  _chapterIdx: ci
                })
              }
              chList.push({
                title: title,
                detailHtml: '<ul>' + (chData.points || []).map(function (p) { return '<li>' + p + '</li>' }).join('') + '</ul>',
                lessons: rawLessons.map(function (l) {
                  return {
                    name: l.name,
                    type: l.type || 'video',
                    iconClass: l.iconClass || (l.type === 'video' ? 'el-icon-video-play' : 'el-icon-presentation'),
                    desc: l.desc || '',
                    btnIcon: l.btnIcon || 'el-icon-view',
                    btnText: l.btnText || (l.type === 'video' ? '观看' : '预览'),
                    _lessonRef: l,
                    _chapterIdx: l._chapterIdx !== undefined ? l._chapterIdx : ci,
                    url: ''  // 稍后通过videoList匹配注入
                  }
                })
              })
            })

            // ★ 将videoList的URL按视频名关键词匹配注入对应章节lesson
            var vlist2 = (rc.videoList || []).filter(function(v){return v.url && (v.url.startsWith('http://')||v.url.startsWith('https://'))})
            // 为每个video建立关键词索引
            var vKeywords = vlist2.map(function(v,vi){
              var name = (v.name||'').toLowerCase()
              return {idx:vi, name:name, url:v.url, duration:v.duration, used:false}
            })
            // 章节关键词映射表（MOOC视频标题→章节标题的对应关系）
            var chKeywordMap = [
              ['绪论','基本概念|绪论|概述|引论|概论|介绍|计算机系统'],
              ['线性表','线性|链表|顺序表|线性结构'],
              ['栈和队列','栈|队列|堆栈'],
              ['串与数组','串|数组|字符串|模式匹配'],
              ['树与二叉树','树|二叉树|二叉|遍历|Huffman|AVL|平衡'],
              ['图','图|最短路径|最小生成|Dijkstra|拓扑|Floyd'],
              ['查找','查找|搜索|检索|哈希|散列|KMP|匹配'],
              ['排序','排序|快排|归并|堆|冒泡|插入|希尔'],
              ['动态存储管理','存储|动态|内存|分配|管理'],
              ['文件','文件|磁盘|外存|索引'],
              ['进程描述与控制','进程|线程|PCB|并发|同步|互斥|信号量'],
              ['处理机调度与死锁','调度|死锁|银行家|处理机'],
              ['存储器管理','存储|内存|分页|分段|页表|TLB|地址'],
              ['虚拟存储器','虚拟|页面置换|OPT|FIFO|LRU|Belady|请求分页'],
              ['输入输出系统','I/O|输入|输出|设备|SPOOLing'],
              ['函数与极限','极限|函数|连续|无穷小|间断'],
              ['导数与微分','导数|微分|求导|链式|切线'],
              ['微分中值定理','中值|Rolle|Lagrange|Cauchy|洛必达|极值|凹凸'],
              ['不定积分','积分|不定|原函数|换元|分部'],
              ['定积分','定积分|Newton|积分|面积|体积'],
              ['空间解析几何','空间|向量|平面|直线|曲面|解析几何'],
              ['多元函数微分学','多元|偏导|全微分|方向导数|梯度|Lagrange乘数'],
              ['重积分','重积分|二重|三重|极坐标|球坐标'],
              ['曲线积分与曲面积分','曲线积分|Green|Gauss|Stokes'],
              ['无穷级数','级数|幂级数|Fourier|Taylor|收敛|发散'],
              ['网络层','网络层|IP|路由|RIP|OSPF|BGP|CIDR|ARP'],
              ['运输层','运输层|TCP|UDP|拥塞|滑动窗口|三次握手|四次挥手'],
              ['行列式','行列式|Cramer|展开|线性方程组'],
              ['矩阵及其运算','矩阵|特征值|特征向量|逆矩阵|对角化|二次型'],
              ['向量组的线性相关性','向量|线性相关|线性无关|秩|极大无关'],
              ['质点运动学','运动|质点|速度|加速度|位移|参考系'],
              ['动量与能量','动量|能量|动量守恒|功|动能|势能|机械能'],
              ['刚体转动','刚体|转动|角动量|转动惯量|角速度'],
              ['静电场','电场|电势|Gauss|电容|库仑'],
              ['电磁感应','电磁感应|Faraday|Lenz|自感|互感|涡旋电场'],
              ['供求理论','供给|需求|供求|均衡|弹性'],
              ['消费者选择','消费者|效用|无差异|预算|边际替代'],
              ['完全竞争市场','竞争|垄断|寡头|博弈|均衡'],
              ['IS-LM模型','IS-LM|AD-AS|国民收入|乘数|货币政策|财政政策'],
              ['关系数据库','关系|关系代数|SQL|规范化|ACID|事务|并发'],
              ['标准SQL语言','SQL|DML|DDL|DQL|SELECT|JOIN|GROUP|HAVING']
            ]
            // 为每个video找到最匹配的章节
            chList.forEach(function(ch){
              ch.lessons.forEach(function(l){
                if(l.type !== 'video') return
                var bestMatch = null, bestScore = 0
                var lessonName = (l.name||'').toLowerCase()
                for(var vi=0;vi<vKeywords.length;vi++){
                  if(vKeywords[vi].used) continue
                  var vn = vKeywords[vi].name
                  // 计算匹配分数
                  var score = 0
                  // 规则1：视频名关键词匹配章节map
                  for(var mi=0;mi<chKeywordMap.length;mi++){
                    var km = chKeywordMap[mi]
                    if(ch.title.indexOf(km[0])>=0){
                      var kws = km[1].split('|')
                      for(var ki=0;ki<kws.length;ki++){
                        if(vn.indexOf(kws[ki].toLowerCase())>=0) score += 10
                      }
                    }
                  }
                  // 规则2：视频中的章节号匹配
                  var vidNum = 0; var vidMatch = vn.match(/第(\d+)/)
                  if(vidMatch) vidNum = parseInt(vidMatch[1])
                  if(vidNum === (chList.indexOf(ch)+1)) score += 5
                  // 规则3：视频名中的关键词直接出现在课程名中
                  if(lessonName && vn && (vn.indexOf(lessonName)>=0 || lessonName.indexOf(vn)>=0)) score += 3
                  if(score > bestScore){bestScore = score; bestMatch = vi}
                }
                if(bestMatch !== null && bestScore >= 5){
                  l.url = vKeywords[bestMatch].url || ''
                  l.desc = vKeywords[bestMatch].duration || l.desc
                  l.name = vlist2[bestMatch].name || l.name
                  vKeywords[bestMatch].used = true
                }
              })
            })

            return {
              id: rc.id,
              name: rc.name || '未命名课程',
              school: rc.school || '未知院校',
              instructor: rc.instructor || '未知',
              credit: rc.credit || 3,
              term: rc.term || '待定',
              description: rc.description || '',
              richDesc: rc.richDesc || '<p>' + (rc.description || '暂无详细介绍') + '</p>',
              studentCount: rc.studentCount || 0,
              rating: rc.rating || 0,
              resourceCount: rc.resourceCount || 0,
              status: rc.status || 'ongoing',
              textbook: rc.textbook || '',
              cat: rc.category || '其他',
              chapterCount: chTitles.length,
              period: rc.period || 48,
              chapterTitles: chTitles,
              chapterList: chList,
              chapters: rawChapters,
              _code: rc.id,
              _chTitles: chTitles,
              _kb: (function(id){
                // 映射 course-data.json id 到 KB 数据库代码
                var map = {
                  'mooc-dsa-001':'DSA','mooc-os-002':'OS','mooc-cn-003':'CN',
                  'mooc-db-004':'DB','mooc-co-005':'CO',
                  'mooc-am1-006':'AM1','mooc-am2-007':'AM2',
                  'mooc-la-008':'LA','mooc-ph1-009':'PH1','mooc-ph2-010':'PH2',
                  'mooc-pb-011':'PS','mooc-mic-012':'MIC','mooc-mac-013':'MAC',
                  'mooc-mgt-014':'MGT','mooc-civ-015':'CIV','mooc-ccl-016':'CCL',
                  'mooc-pst-017':'PS','mooc-eng1-018':'EN1','mooc-eng2-019':'EN2'
                }
                return map[id] || (id||'').replace(/^mooc-([a-z]+)-.*$/i,'$1').toUpperCase()
              })(rc.id),
              // ⭐ 真实第三方资源：课件PPT
              pptList: Array.isArray(rc.pptList) ? rc.pptList.map(function (ppt) {
                return {
                  name: ppt.name || '课件',
                  size: ppt.size || '未知',
                  pages: ppt.pages || 30,
                  url: ppt.url || '#',
                  _sourceUrl: rc.sourceUrl || ''
                }
              }) : [],
              // ⭐ 真实第三方资源：教学视频
              videoList: Array.isArray(rc.videoList) ? rc.videoList.map(function (v) {
                return {
                  name: v.name || '教学视频',
                  duration: v.duration || '00:00',
                  views: v.views || '0',
                  url: v.url || '#',
                  _sourceUrl: rc.sourceUrl || ''
                }
              }) : [],
              // ⭐ 真实第三方资源：电子课本
              textbookResources: Array.isArray(rc.textbookResources) ? rc.textbookResources.map(function (tb) {
                return {
                  name: tb.name || '教材',
                  author: tb.author || '未知',
                  publisher: tb.publisher || '未知',
                  edition: tb.edition || '最新版',
                  chapters: tb.chapters || 8,
                  pages: tb.pages || 300,
                  url: tb.url || '',
                  _sourceUrl: rc.sourceUrl || ''
                }
              }) : [],
              // ⭐ 真实第三方资源：习题
              exercises: Array.isArray(rc.exercises) ? rc.exercises.map(function (ex) {
                return {
                  question: ex.question || '题目',
                  options: ex.options || [],
                  answer: ex.answer || ''
                }
              }) : [],
              _raw: rc
            }
          }.bind(this))
          this.$message.success('已加载 ' + this.allCourses.length + ' 门课程资源')
          return
        }
      } catch (e) {
        console.warn('API加载失败，使用本地数据:', e.message)
      }
      // 回退到本地种子数据
      this.allCourses = buildCourses()
    },
    doImportFromUrl: async function () {
      if (!this.importUrl.trim()) { this.$message.warning('请输入课程URL'); return }
      this.importing = true
      try {
        var res = await courseApi.importFromUrl({
          url: this.importUrl.trim(),
          name: this.importName.trim(),
          category: this.importCategory || '其他'
        })
        if (res && res.code === 1 && res.response) {
          this.$message.success('课程导入成功！')
          this.showImportDialog = false
          this.resetImportForm()
          // 重新加载课程列表
          setTimeout(function () { this.loadCourses() }.bind(this), 500)
        } else {
          this.$message.error(res.message || '导入失败')
        }
      } catch (e) {
        this.$message.error('导入失败: ' + (e.message || '网络错误'))
      } finally {
        this.importing = false
      }
    },
    doImportManual: async function () {
      var mc = this.manualCourse
      if (!mc.name.trim()) { this.$message.warning('请输入课程名称'); return }
      this.importing = true
      try {
        var res = await courseApi.importCourse({
          name: mc.name.trim(),
          school: mc.school.trim() || '未知院校',
          instructor: mc.instructor.trim() || '未知',
          category: mc.category || '其他',
          credit: mc.credit || 3,
          description: mc.description.trim() || mc.name.trim() + ' 课程',
          textbook: mc.textbook.trim() || ''
        })
        if (res && res.code === 1 && res.response) {
          this.$message.success('课程添加成功！')
          this.showImportDialog = false
          this.resetImportForm()
          setTimeout(function () { this.loadCourses() }.bind(this), 500)
        } else {
          this.$message.error(res.message || '添加失败')
        }
      } catch (e) {
        this.$message.error('添加失败: ' + (e.message || '网络错误'))
      } finally {
        this.importing = false
      }
    },
    // ── 本地文件上传 ──
    handlePptUpload: async function (e) {
      var file = e.target.files && e.target.files[0]
      if (!file) return
      var self = this
      try {
        var res = await courseApi.uploadFile(file)
        if (res && res.code === 1 && res.response) {
          var result = res.response
          self.pptFetchResult = result
          self.$message.success('PPT上传成功！')
          // 自动预览
          if (result.localUrl) {
            self.pptExternalIframe = self.getPreferredViewerUrl(window.location.origin + result.localUrl)
            self.pptShowWebCard = false
            self.pptPreviewVisible = true
            self.pptPreviewTitle = '📄 ' + (result.fileName || '上传课件')
          }
        } else {
          self.$message.error(res ? res.message : '上传失败')
        }
      } catch (err) {
        self.$message.error('上传失败: ' + (err.message || '网络错误'))
      }
      // 清除文件选择，允许重复上传同一文件
      e.target.value = ''
    },
    handleTbUpload: async function (e) {
      var file = e.target.files && e.target.files[0]
      if (!file) return
      var self = this
      try {
        var res = await courseApi.uploadFile(file)
        if (res && res.code === 1 && res.response) {
          var result = res.response
          self.tbFetchResult = result
          self.$message.success('电子课本上传成功！')
          if (result.localUrl) {
            // PDF直接嵌入，其他用Office Online
            var urlLower = result.fileName.toLowerCase()
            if (urlLower.endsWith('.pdf')) {
              self.readerIframeUrl = result.localUrl + '#view=FitH'
            } else {
              self.readerIframeUrl = self.getPreferredViewerUrl(window.location.origin + result.localUrl)
            }
            self.readerBook = { name: result.fileName, author: '本地上传', publisher: '', edition: '' }
            self.readerIsExternal = true
            self.readerShowWebCard = false
            self.readerVisible = true
          }
        } else {
          self.$message.error(res ? res.message : '上传失败')
        }
      } catch (err) {
        self.$message.error('上传失败: ' + (err.message || '网络错误'))
      }
      e.target.value = ''
    },
    resetImportForm: function () {
      this.importTab = 'url'
      this.importUrl = ''
      this.importName = ''
      this.importCategory = ''
      this.manualCourse = { name: '', school: '', instructor: '', category: '', credit: 3, description: '', textbook: '' }
    }
  }
}
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════════════════
   Teaching Hub — Modern Design System
   暗曜学术风格 + 玻璃质感 + 微动效
   ═══════════════════════════════════════════════════════════════════ */
:root {
  --hub-bg: #f0f2f5;
  --hub-surface: #ffffff;
  --hub-text: #141A14;
  --hub-text-secondary: #64748b;
  --hub-text-muted: #94a3b8;
  --hub-accent: var(--acid);
  --hub-accent-light: #8b83e8;
  --hub-accent-glow: rgba(var(--acid-rgb), 0.12);
  --hub-radius-lg: 16px;
  --hub-radius-md: 12px;
  --hub-radius-sm: 8px;
  --hub-shadow-sm: 0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.06);
  --hub-shadow-md: 0 4px 16px rgba(0,0,0,0.06), 0 2px 6px rgba(0,0,0,0.04);
  --hub-shadow-lg: 0 12px 40px rgba(0,0,0,0.08), 0 4px 12px rgba(0,0,0,0.04);
  --hub-shadow-xl: 0 20px 60px rgba(0,0,0,0.1), 0 8px 20px rgba(0,0,0,0.06);
  --hub-transition: 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ═══ Page Container ═══ */
.teaching-hub {
  min-height: 100vh;
  background: var(--hub-bg);
  position: relative;
  overflow-x: hidden;
}

/* ═══ Animated Background ═══ */
.hub-bg {
  position: fixed; inset: 0; z-index: 0; pointer-events: none; overflow: hidden;
}
.hub-bg-gradient {
  position: absolute; inset: 0;
  background: radial-gradient(ellipse 80% 60% at 50% 0%, rgba(var(--acid-rgb), 0.04), transparent 60%),
              radial-gradient(ellipse 60% 80% at 80% 60%, rgba(139,131,232,0.03), transparent 50%);
}
.hub-bg-particles {}
.hub-particle {
  position: absolute; width: 4px; height: 4px; border-radius: 50%;
  background: rgba(var(--acid-rgb), 0.06);
  animation: hub-float linear infinite;
}
@keyframes hub-float {
  0%, 100% { transform: translateY(0) scale(1); opacity: 0.3; }
  50% { transform: translateY(-30px) scale(1.5); opacity: 0.06; }
}
.hub-bg-grid {
  position: absolute; inset: 0;
  background-image: linear-gradient(rgba(var(--acid-rgb), 0.02) 1px, transparent 1px),
                    linear-gradient(90deg, rgba(var(--acid-rgb), 0.02) 1px, transparent 1px);
  background-size: 60px 60px;
}

/* ═══ Top Bar ═══ */
.hub-topbar {
  position: sticky; top: 0; z-index: 100;
  background: rgba(255,255,255,0.8); backdrop-filter: blur(16px) saturate(180%);
  border-bottom: 1px solid rgba(0,0,0,0.05);
}
.hub-topbar-inner {
  max-width: 1280px; margin: 0 auto; display: flex; align-items: center;
  height: 60px; padding: 0 24px; gap: 24px;
}
.hub-brand { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.hub-brand-icon {
  width: 36px; height: 36px; border-radius: 10px;
  background: linear-gradient(135deg, var(--acid), #8b83e8);
  color: #fff; display: flex; align-items: center; justify-content: center;
  box-shadow: 0 2px 8px rgba(var(--acid-rgb), 0.3);
}
.hub-brand-text { font-size: 17px; font-weight: 700; color: var(--hub-text); letter-spacing: -0.3px; }
.hub-nav { display: flex; gap: 4px; flex: 1; overflow-x: auto; scrollbar-width: none; }
.hub-nav::-webkit-scrollbar { display: none; }
.hub-nav-link {
  position: relative; padding: 6px 16px; font-size: 13px; font-weight: 500;
  color: var(--hub-text-secondary); cursor: pointer; border-radius: 8px;
  white-space: nowrap; transition: var(--hub-transition);
}
.hub-nav-link:hover { color: var(--hub-accent); background: var(--hub-accent-glow); }
.hub-nav-link.active { color: var(--hub-accent); background: var(--hub-accent-glow); font-weight: 600; }
.hub-nav-indicator {
  position: absolute; bottom: 3px; left: 50%; transform: translateX(-50%) scaleX(0);
  width: 16px; height: 2px; border-radius: 1px; background: var(--hub-accent);
  transition: transform 0.2s ease;
}
.hub-nav-link.active .hub-nav-indicator { transform: translateX(-50%) scaleX(1); }

.hub-actions { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.hub-search-wrap {
  position: relative; display: flex; align-items: center;
  background: rgba(0,0,0,0.04); border-radius: 10px; padding: 0 12px;
  border: 1px solid transparent; transition: var(--hub-transition);
}
.hub-search-wrap:focus-within { background: #fff; border-color: var(--hub-accent); box-shadow: 0 0 0 3px var(--hub-accent-glow); }
.hub-search-icon { font-size: 15px; color: var(--hub-text-muted); margin-right: 6px; }
.hub-search-input {
  border: none; outline: none; background: transparent; padding: 8px 0;
  font-size: 13px; color: var(--hub-text); width: 200px;
}
.hub-search-input::placeholder { color: var(--hub-text-muted); }
.hub-search-clear { cursor: pointer; color: var(--hub-text-muted); font-size: 13px; padding: 4px; border-radius: 50%; }
.hub-search-clear:hover { color: var(--hub-text); background: rgba(0,0,0,0.06); }

/* ═══ Buttons ═══ */
.hub-btn {
  display: inline-flex; align-items: center; gap: 6px; padding: 8px 16px;
  font-size: 13px; font-weight: 500; border: none; border-radius: 10px;
  cursor: pointer; transition: var(--hub-transition); white-space: nowrap;
  font-family: inherit; line-height: 1.4;
}
.hub-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.hub-btn-primary {
  background: linear-gradient(135deg, var(--acid), #7c6ff7);
  color: #fff; box-shadow: 0 2px 8px rgba(var(--acid-rgb), 0.25);
}
.hub-btn-primary:hover:not(:disabled) {
  box-shadow: 0 4px 16px rgba(var(--acid-rgb), 0.35); transform: translateY(-1px);
}
.hub-btn-success {
  background: linear-gradient(135deg, #10b981, #059669); color: #fff;
  box-shadow: 0 2px 8px rgba(16,185,129,0.25);
}
.hub-btn-success:hover:not(:disabled) { box-shadow: 0 4px 12px rgba(16,185,129,0.35); transform: translateY(-1px); }
.hub-btn-outline {
  background: #fff; color: var(--hub-text); border: 1px solid rgba(0,0,0,0.1);
}
.hub-btn-outline:hover:not(:disabled) { border-color: var(--hub-accent); color: var(--hub-accent); background: var(--hub-accent-glow); }
.hub-btn-ghost { background: transparent; color: var(--hub-text-secondary); }
.hub-btn-ghost:hover { color: var(--hub-accent); background: rgba(0,0,0,0.04); }
.hub-btn-icon {
  width: 34px; height: 34px; padding: 0; border-radius: 8px; justify-content: center;
  background: #fff; color: var(--hub-text); border: 1px solid rgba(0,0,0,0.08);
}
.hub-btn-icon:hover:not(:disabled) { border-color: var(--hub-accent); color: var(--hub-accent); background: var(--hub-accent-glow); }
.hub-btn-sm { padding: 6px 12px; font-size: 12px; border-radius: 8px; }
.hub-btn.loading { position: relative; pointer-events: none; }
.hub-btn.loading::after {
  content: ''; width: 14px; height: 14px; border: 2px solid transparent;
  border-top-color: currentColor; border-radius: 50%; animation: hub-spin 0.7s linear infinite;
  margin-left: 6px;
}
@keyframes hub-spin { to { transform: rotate(360deg); } }
.hub-btn-play { background: var(--ink-3); color: #dc2626; }
.hub-btn-play:hover { background: #fecaca; }
.hub-btn-view { background: var(--ink-3); color: var(--hub-accent); }
.hub-btn-view:hover { background: var(--ink-3); }

/* ═══ Hero ═══ */
.hub-hero {
  position: relative; padding: 60px 24px 64px; text-align: center;
  background: linear-gradient(160deg, var(--ink) 0%, #141A14 30%, var(--ink) 60%, #141A14 100%);
  overflow: hidden; color: #fff;
}
.hub-hero-overlay {
  position: absolute; inset: 0; opacity: 0.04;
  background-image: radial-gradient(circle at 30% 20%, #fff 1px, transparent 1px),
                    radial-gradient(circle at 70% 80%, #fff 1px, transparent 1px);
  background-size: 50px 50px, 70px 70px;
}
.hub-hero-content { position: relative; z-index: 2; max-width: 720px; margin: 0 auto; }
.hub-hero-badge {
  display: inline-block; padding: 4px 16px; border-radius: 20px;
  font-size: 12px; font-weight: 500; margin-bottom: 16px;
  background: rgba(255,255,255,0.08); border: 1px solid rgba(255,255,255,0.12);
  color: rgba(255,255,255,0.8); backdrop-filter: blur(8px);
}
.hub-hero-title { font-size: 36px; font-weight: 800; margin: 0 0 12px; line-height: 1.3; letter-spacing: -0.5px; }
.hub-hero-accent {
  background: linear-gradient(135deg, var(--ice), #c4b5fd);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}
.hub-hero-desc { font-size: 16px; color: rgba(255,255,255,0.6); margin: 0 0 32px; }
.hub-hero-stats { display: flex; justify-content: center; gap: 48px; }
.hub-stat { text-align: center; }
.hub-stat-num {
  display: block; font-size: 36px; font-weight: 800;
  background: linear-gradient(135deg, #f0e6d3, #c9a96e);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}
.hub-stat-label { font-size: 13px; color: rgba(255,255,255,0.5); font-weight: 500; }
.hub-hero-shape {
  position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.15;
}
.hub-hero-shape-1 { width: 400px; height: 400px; background: var(--ink-3); top: -100px; right: -80px; }
.hub-hero-shape-2 { width: 300px; height: 300px; background: var(--ink-3); bottom: -80px; left: -60px; }

/* ═══ Section ═══ */
.hub-section { position: relative; z-index: 1; max-width: 1280px; margin: 0 auto; padding: 32px 24px; }
.hub-section-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px;
}
.hub-section-icon { margin-right: 8px; }
.hub-section-title { font-size: 20px; font-weight: 700; color: var(--hub-text); margin: 0; }
.hub-section-desc { font-size: 13px; color: var(--hub-text-muted); margin: 4px 0 0; }
.hub-section-filters { display: flex; align-items: center; gap: 12px; }

/* ═══ Platform Grid ═══ */
.hub-platform-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 12px;
  margin-bottom: 8px;
}
.hub-platform-card {
  position: relative; display: flex; align-items: center; gap: 14px;
  padding: 16px; background: var(--hub-surface); border-radius: var(--hub-radius-md);
  text-decoration: none; color: inherit; border: 1px solid rgba(0,0,0,0.06);
  transition: var(--hub-transition); overflow: hidden;
}
.hub-platform-card::before {
  content: ''; position: absolute; inset: 0; background: linear-gradient(135deg, rgba(var(--acid-rgb), 0.03), transparent);
  opacity: 0; transition: opacity 0.3s;
}
.hub-platform-card:hover { border-color: rgba(var(--acid-rgb), 0.2); box-shadow: var(--hub-shadow-md); }
.hub-platform-card:hover::before { opacity: 1; }
.hub-platform-card-icon {
  width: 44px; height: 44px; border-radius: var(--hub-radius-sm);
  background: var(--hub-accent-glow); display: flex; align-items: center;
  justify-content: center; font-size: 20px; flex-shrink: 0;
}
.hub-platform-card-body { flex: 1; min-width: 0; }
.hub-platform-card-body strong { font-size: 14px; color: var(--hub-text); display: block; margin-bottom: 3px; }
.hub-platform-card-body p { font-size: 12px; color: var(--hub-text-muted); margin: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.hub-platform-card-tag {
  padding: 2px 10px; border-radius: 6px; font-size: 11px; font-weight: 500;
  background: var(--hub-accent-glow); color: var(--hub-accent); flex-shrink: 0;
}
.hub-platform-card-arrow { font-size: 14px; color: var(--hub-text-muted); flex-shrink: 0; opacity: 0; transition: opacity 0.2s; }
.hub-platform-card:hover .hub-platform-card-arrow { opacity: 1; }

/* ═══ Course Grid ═══ */
.hub-course-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px;
}
.hub-course-card {
  background: var(--hub-surface); border-radius: var(--hub-radius-lg);
  overflow: hidden; cursor: pointer; transition: var(--hub-transition);
  border: 1px solid rgba(0,0,0,0.05); box-shadow: var(--hub-shadow-sm);
}
.hub-course-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--hub-shadow-xl);
  border-color: rgba(var(--acid-rgb), 0.15);
}
.hub-card-cover {
  position: relative; height: 140px; padding: 16px;
  display: flex; flex-direction: column; justify-content: space-between;
  overflow: hidden;
}
.hub-card-cover-pattern {
  position: absolute; inset: 0; opacity: 0.08;
  background-image: repeating-linear-gradient(45deg, #fff 0px, #fff 2px, transparent 2px, transparent 8px);
}
.hub-card-badge {
  align-self: flex-start; padding: 2px 10px; border-radius: 6px;
  font-size: 11px; font-weight: 500; background: rgba(255,255,255,0.2); color: #fff;
  backdrop-filter: blur(4px);
}
.hub-card-badge.ongoing { background: rgba(34,197,94,0.8); }
.hub-card-badge.upcoming { background: rgba(251,191,36,0.8); }
.hub-card-school { font-size: 12px; color: rgba(255,255,255,0.85); font-weight: 500; position: relative; z-index: 1; }
.hub-card-content { padding: 16px; }
.hub-card-title { font-size: 15px; font-weight: 600; color: var(--hub-text); margin: 0 0 6px; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.hub-card-desc { font-size: 12px; color: var(--hub-text-muted); margin: 0; line-height: 1.5; min-height: 36px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.hub-card-meta { display: flex; gap: 14px; margin-top: 10px; }
.hub-meta-item { display: flex; align-items: center; gap: 4px; font-size: 12px; color: var(--hub-text-muted); }
.hub-meta-item i { color: var(--hub-accent); font-size: 13px; }
.hub-card-footer {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 16px 14px; border-top: 1px solid rgba(0,0,0,0.04);
}
.hub-card-footer-left { display: flex; align-items: center; gap: 8px; }
.hub-card-credit { font-size: 11px; padding: 2px 8px; border-radius: 5px; background: var(--ink-3); color: #d97706; font-weight: 500; }
.hub-card-term { font-size: 12px; color: var(--hub-text-muted); }
.hub-card-link {
  font-size: 12px; font-weight: 500; color: var(--hub-accent); opacity: 0;
  transition: opacity 0.2s; display: flex; align-items: center; gap: 4px;
}
.hub-course-card:hover .hub-card-link { opacity: 1; }

/* ═══ Empty State ═══ */
.hub-empty { grid-column: 1/-1; text-align: center; padding: 64px 20px; }
.hub-empty-icon { font-size: 48px; margin-bottom: 12px; }
.hub-empty h3 { font-size: 18px; color: var(--hub-text); margin: 0 0 6px; }
.hub-empty p { font-size: 14px; color: var(--hub-text-muted); margin: 0 0 20px; }
.hub-empty-small { text-align: center; padding: 40px 20px; color: var(--hub-text-muted); }
.hub-empty-small i { font-size: 36px; display: block; margin-bottom: 10px; color: var(--hub-text-muted); }
.hub-empty-small p { font-size: 14px; margin: 0 0 6px; }
.hub-empty-small span { font-size: 12px; }

/* ═══ Detail Dialog ═══ */
.hub-detail-header {
  display: flex; align-items: center; gap: 18px;
}
.hub-detail-cover {
  width: 64px; height: 64px; border-radius: 14px; display: flex;
  align-items: center; justify-content: center; color: #fff; font-size: 24px; font-weight: 700;
  flex-shrink: 0; box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}
.hub-detail-info { flex: 1; }
.hub-detail-info h2 { font-size: 20px; font-weight: 700; color: var(--hub-text); margin: 0 0 4px; }
.hub-detail-instructor { font-size: 13px; color: var(--hub-text-secondary); margin: 0; display: flex; align-items: center; gap: 4px; }
.hub-detail-dot { color: var(--hub-text-muted); margin: 0 2px; }
.hub-detail-tags { display: flex; gap: 8px; margin-top: 8px; }
.hub-tag { padding: 2px 10px; border-radius: 6px; font-size: 11px; font-weight: 500; }
.hub-tag-success { background: var(--ink-3); color: #065f46; }
.hub-tag-warning { background: var(--ink-3); color: #92400e; }
.hub-tag-default { background: var(--ink-3); color: var(--grey); }
.hub-detail-body { min-height: 300px; }
.hub-detail-tabs >>> .el-tabs__header { margin-bottom: 20px; }
.hub-detail-tabs >>> .el-tabs__item { font-size: 14px; font-weight: 500; padding: 0 20px; }

/* ═══ Intro Section ═══ */
.hub-intro-content { line-height: 2; color: var(--grey); }
.hub-intro-content h3 { color: var(--hub-text); margin: 0 0 8px; font-size: 17px; }
.hub-intro-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-top: 20px; }
.hub-intro-stat {
  display: flex; align-items: center; gap: 12px; padding: 16px;
  background: var(--ink-3); border-radius: 12px; border: 1px solid rgba(0,0,0,0.04);
  transition: var(--hub-transition);
}
.hub-intro-stat:hover { border-color: rgba(var(--acid-rgb), 0.15); }
.hub-intro-stat-icon {
  width: 42px; height: 42px; border-radius: 10px; display: flex;
  align-items: center; justify-content: center; color: #fff; flex-shrink: 0;
}
.hub-intro-stat-icon i { font-size: 18px; }
.hub-intro-stat strong { display: block; font-size: 14px; color: var(--hub-text); }
.hub-intro-stat span { font-size: 12px; color: var(--hub-text-muted); }

/* ═══ Chapters ═══ */
.hub-chapters { display: flex; flex-direction: column; gap: 12px; }
.hub-chapter { border: 1px solid rgba(0,0,0,0.06); border-radius: var(--hub-radius-md); overflow: hidden; }
.hub-chapter-header {
  padding: 16px 20px; color: #fff; cursor: pointer;
  display: flex; align-items: center; justify-content: space-between;
  user-select: none;
}
.hub-chapter-header-left { display: flex; align-items: center; gap: 12px; }
.hub-chapter-num {
  width: 30px; height: 30px; border-radius: 50%;
  background: rgba(255,255,255,0.2); display: flex; align-items: center;
  justify-content: center; font-size: 13px; font-weight: 700; flex-shrink: 0;
}
.hub-chapter-title { font-size: 15px; font-weight: 600; }
.hub-chapter-header-right { display: flex; align-items: center; gap: 10px; }
.hub-chapter-meta { font-size: 12px; opacity: 0.7; }
.hub-chapter-arrow { font-size: 14px; transition: transform 0.2s; }
.hub-chapter-body { background: var(--ink-3); }
.hub-lesson-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 8px;
  padding: 0 16px 16px;
}
.hub-lesson {
  display: flex; align-items: center; gap: 12px; padding: 12px;
  background: #fff; border: 1px solid rgba(0,0,0,0.04); border-radius: 10px;
  cursor: pointer; transition: var(--hub-transition);
}
.hub-lesson:hover { border-color: var(--hub-accent); box-shadow: 0 2px 8px var(--hub-accent-glow); }
.hub-lesson-icon {
  width: 40px; height: 40px; border-radius: 10px; display: flex;
  align-items: center; justify-content: center; flex-shrink: 0;
}
.hub-lesson-icon.video { background: linear-gradient(135deg, var(--acid), var(--ice)); }
.hub-lesson-icon.ppt { background: linear-gradient(135deg, var(--acid), var(--ice)); }
.hub-lesson-icon i { color: #fff; font-size: 17px; }
.hub-lesson-info { flex: 1; min-width: 0; }
.hub-lesson-name { font-size: 13px; font-weight: 600; color: var(--hub-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.hub-lesson-meta { display: flex; align-items: center; gap: 6px; margin-top: 2px; }
.hub-lesson-meta span { font-size: 11px; color: var(--hub-text-muted); }

/* ═══ Resource Tab ═══ */
.hub-resource-tab {}
/* 电子课本嵌入 */
.hub-textbook-embed-section { margin-bottom: 16px; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; }
.hub-textbook-embed-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; background: var(--ink-3); border-bottom: 1px solid #e2e8f0; }
.hub-textbook-embed-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: #1e293b; }
.hub-textbook-embed-title i { color: var(--acid); font-size: 18px; }
.hub-textbook-embed-platforms { display: flex; gap: 6px; flex-wrap: wrap; }
.hub-tb-platform-btn { padding: 5px 14px; border: 1px solid #e2e8f0; border-radius: 8px; background: #fff; font-size: 12px; font-weight: 500; color: #64748b; cursor: pointer; transition: all 0.18s; }
.hub-tb-platform-btn:hover { border-color: #a5b4fc; color: var(--acid); }
.hub-tb-platform-btn.active { background: var(--ink-3); border-color: var(--ink-5); color: #fff; }
.hub-tb-open-btn { color: var(--acid); border-color: var(--ink-5); }
.hub-textbook-iframe-wrap { position: relative; width: 100%; height: 560px; background: #fff; }
.hub-textbook-iframe { width: 100%; height: 100%; border: none; display: block; }
.hub-textbook-iframe-loading { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; background: var(--ink-3); color: #94a3b8; }
.hub-textbook-iframe-loading i { font-size: 32px; margin-bottom: 8px; }
.hub-textbook-iframe-loading p { font-size: 13px; }
.hub-resource-search { margin-bottom: 12px; }
.hub-resource-search-row { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.hub-research-input { flex: 1; min-width: 200px; }
.hub-search-results { margin-top: 12px; display: flex; flex-direction: column; gap: 6px; }
.hub-search-result {
  display: flex; align-items: center; gap: 10px; padding: 10px 14px;
  background: #fff; border: 1px solid rgba(0,0,0,0.06); border-radius: 10px;
  cursor: pointer; transition: var(--hub-transition);
}
.hub-search-result:hover { border-color: var(--hub-accent); background: var(--ink-3); }
.hub-search-result-icon { font-size: 18px; flex-shrink: 0; }
.hub-search-result-title { flex: 1; font-size: 13px; color: var(--hub-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.hub-search-result-action { font-size: 16px; color: var(--hub-accent); flex-shrink: 0; }
.hub-fetch-success {
  display: flex; align-items: center; gap: 10px; padding: 12px 16px;
  margin-top: 12px; background: var(--ink-3); border-radius: 10px;
  border: 1px solid #a7f3d0; font-size: 13px;
}
.hub-fetch-success i { color: #10b981; font-size: 18px; }
.hub-fetch-success span { flex: 1; color: #065f46; }

/* ═══ PPT Grid ═══ */
.hub-ppt-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 14px; }
.hub-ppt-card {
  position: relative; background: #fff; border-radius: var(--hub-radius-md);
  border: 1px solid rgba(0,0,0,0.06); overflow: hidden; cursor: pointer;
  transition: var(--hub-transition);
}
.hub-ppt-card:hover { border-color: rgba(14,165,233,0.2); box-shadow: 0 8px 30px rgba(0,0,0,0.1); }
/* 自动生成PPT卡片 */
.hub-auto-ppt-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 12px; }
.hub-ppt-chapter-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px; }
.hub-ppt-chapter-card { background: #fff; border-radius: 14px; border: 1px solid rgba(0,0,0,0.06); overflow: hidden; cursor: pointer; transition: all 0.25s ease; }
.hub-ppt-chapter-card:hover { border-color: rgba(var(--acid-rgb), 0.25); box-shadow: 0 8px 30px rgba(0,0,0,0.1); transform: translateY(-3px); }
.hub-ppt-chapter-cover { height: 100px; display: flex; align-items: center; justify-content: center; }
.hub-ppt-chapter-info { padding: 14px 16px; }
.hub-ppt-chapter-info h4 { font-size: 14px; font-weight: 600; color: #1e3a5f; margin: 0 0 4px; }
.hub-ppt-chapter-info span { font-size: 12px; color: #94a3b8; }
/* PPT 幻灯片展示 */
.hub-ppt-toolbar { display:flex; align-items:center; gap:12px; padding:8px 0; margin-bottom:12px; }
.hub-ppt-slideshow { display:flex; align-items:center; gap:8px; min-height:380px; }
.hub-ppt-nav { width:40px; height:40px; border-radius:50%; border:1px solid #e2e8f0; background:#fff; cursor:pointer; display:flex; align-items:center; justify-content:center; font-size:18px; color:var(--grey); transition:all 0.2s; flex-shrink:0; }
.hub-ppt-nav:hover:not(:disabled) { background:var(--ink-3); color:#fff; border-color:var(--ink-5); }
.hub-ppt-nav:disabled { opacity:0.3; cursor:not-allowed; }
.hub-ppt-slide-display { flex:1; display:flex; justify-content:center; cursor:pointer; }
.hub-ppt-thumbstrip { display:flex; gap:6px; margin-top:12px; padding:8px 0; overflow-x:auto; justify-content:center; }
.hub-ppt-thumb { width:28px; height:28px; border-radius:6px; background:var(--ink-3); color:#64748b; font-size:11px; font-weight:600; display:flex; align-items:center; justify-content:center; cursor:pointer; border:2px solid transparent; transition:all 0.15s; flex-shrink:0; }
.hub-ppt-thumb.active { background:var(--ink-3); color:#fff; border-color:var(--ink-5); }
.hub-ppt-thumb:hover:not(.active) { background:var(--ink-3); }
.hub-auto-ppt-card { background: #fff; border-radius: 12px; border: 1px solid rgba(14,165,233,0.1); padding: 16px; cursor: pointer; transition: all 0.25s ease; display: flex; gap: 14px; align-items: flex-start; }
.hub-auto-ppt-card:hover { border-color: rgba(14,165,233,0.3); box-shadow: 0 6px 24px rgba(14,165,233,0.12); transform: translateY(-2px); }
.hub-auto-ppt-card-num { width: 40px; height: 40px; border-radius: 10px; background: linear-gradient(135deg, var(--ice), var(--acid)); color: #fff; font-weight: 700; font-size: 16px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.hub-auto-ppt-card-body h4 { font-size: 14px; color: #0c4a6e; margin: 0 0 6px; font-weight: 600; }
.hub-auto-ppt-card-body ul { margin: 0; padding: 0 0 0 16px; }
.hub-auto-ppt-card-body li { font-size: 12px; color: var(--grey); line-height: 1.6; }
.hub-generating-status { padding: 12px 16px; background: rgba(14,165,233,0.06); border-radius: 10px; color: var(--ice); font-size: 13px; display: flex; align-items: center; gap: 8px; margin-top: 8px; }
.hub-generating-status i { animation: spin 1s linear infinite; }
/* 自动生成幻灯片预览 */
.hub-auto-slide-view { min-height: 460px; display: flex; align-items: center; justify-content: center; padding: 20px; }
.hub-auto-slide-card { width: 100%; max-width: 780px; border-radius: 16px; padding: 40px 48px; box-shadow: 0 12px 48px rgba(14,165,233,0.08); min-height: 420px; }
.hub-auto-slide-header { margin-bottom: 8px; }
.hub-auto-slide-badge { display: inline-block; padding: 4px 16px; border-radius: 20px; color: #fff; font-size: 12px; font-weight: 600; letter-spacing: 1px; }
.hub-auto-slide-title { font-size: 26px; font-weight: 700; margin: 16px 0 12px; line-height: 1.3; }
.hub-auto-slide-divider { width: 80px; height: 3px; border-radius: 2px; margin-bottom: 24px; }
.hub-auto-slide-list { list-style: none; padding: 0; margin: 0; }
.hub-auto-slide-list li { font-size: 16px; line-height: 2.2; padding: 8px 0; display: flex; align-items: flex-start; gap: 12px; animation: fadeInUp 0.4s ease both; }
.hub-auto-slide-bullet { width: 8px; height: 8px; border-radius: 50%; margin-top: 10px; flex-shrink: 0; }
@keyframes fadeInUp { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
/* ═══ ppt-agent 品质幻灯片布局 ═══ */
.hub-ppta-slide-card { width:100%;max-width:820px;min-height:460px;border-radius:18px;padding:40px 48px;background:#fff;box-shadow:0 12px 48px rgba(0,0,0,0.08);position:relative;overflow:hidden; }
.hub-ppta-slide-card.hub-ppta-cover { background:linear-gradient(135deg, var(--ice), #eff6ff); text-align:center; display:flex;flex-direction:column;align-items:center;justify-content:center; }
.hub-ppta-slide-card.hub-ppta-end { background:linear-gradient(135deg, var(--ink-4), var(--acid)); text-align:center; display:flex;flex-direction:column;align-items:center;justify-content:center; color:#fff; }
.sv-topbar { position:absolute;top:0;left:0;right:0;height:5px;background:linear-gradient(90deg,var(--acid),var(--ice)); }
.sv-topbar--orange { background:linear-gradient(90deg,#f59e0b,#f97316); }
.sv-topbar--teal { background:linear-gradient(90deg,var(--ice),#0d9488); }
.sv-topbar--purple { background:linear-gradient(90deg,var(--acid),var(--ice)); }
.ppta-cover-badge { display:inline-block;padding:6px 24px;border-radius:24px;background:linear-gradient(135deg,var(--acid),var(--ice));color:#fff;font-size:13px;font-weight:600;margin-bottom:16px; }
.ppta-cover-title { font-size:30px;font-weight:700;color:#1e3a5f;margin:12px 0; }
.hub-ppta-end .ppta-cover-title { color:#fff; }
.ppta-cover-sub { color:#64748b;font-size:15px;margin-top:8px; }
.ppta-cover-meta { color:#94a3b8;font-size:13px;margin-top:6px; }
.ppta-end-badge { display:inline-block;padding:4px 20px;border-radius:20px;background:rgba(255,255,255,0.15);color:#fff;font-size:13px;margin-bottom:8px; }
.sv-section-title { font-size:22px;font-weight:700;color:#1e3a5f;margin:12px 0 20px; }
.sv-section-title--orange { color:#c2410c; }
.sv-section-title--teal { color:#0f766e; }
.sv-section-title--purple { color:#6b21a8; }
.sv-goal-cards { display:flex;gap:16px;margin-top:8px; }
.sv-goal-card { flex:1;padding:20px;border-radius:14px;position:relative;overflow:hidden; }
.sv-goal-card--0 { background:linear-gradient(135deg,var(--ice),#eff6ff); }
.sv-goal-card--1 { background:linear-gradient(135deg,var(--ice),#eef2ff); }
.sv-goal-card--2 { background:linear-gradient(135deg,#fce7f3,#fdf2f8); }
.sv-goal-num { font-size:28px;font-weight:800;color:rgba(var(--acid-rgb), 0.15); }
.sv-goal-text { font-size:14px;color:var(--acid-dim);margin-top:6px;line-height:1.5; }
.sv-footer-tag { position:absolute;bottom:16px;right:24px;font-size:12px;color:#cbd5e1;font-weight:500; }
.sv-footer-tag--orange { color:#fdba74; }
.sv-footer-tag--teal { color:var(--ice); }
.sv-footer-tag--purple { color:#c4b5fd; }
.ppta-toc-grid { display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:8px; }
.ppta-toc-item { display:flex;align-items:center;gap:10px;padding:10px 14px;background:var(--ink-3);border-radius:10px;font-size:14px;color:var(--grey); }
.ppta-toc-item:hover { background:var(--ink-3); }
.ppta-toc-num { width:28px;height:28px;border-radius:50%;background:linear-gradient(135deg,var(--acid),var(--ice));color:#fff;display:flex;align-items:center;justify-content:center;font-size:12px;font-weight:700;flex-shrink:0; }
.sv-kp-main { display:flex;gap:20px;margin-top:8px; }
.sv-kp-steps { flex:1; }
.sv-kp-step { display:flex;align-items:flex-start;gap:10px;padding:8px 0;position:relative; }
.sv-kp-dot { width:26px;height:26px;border-radius:50%;background:linear-gradient(135deg,#f59e0b,#f97316);color:#fff;display:flex;align-items:center;justify-content:center;font-size:12px;font-weight:700;flex-shrink:0; }
.sv-kp-text { font-size:14px;color:var(--grey);line-height:1.5; }
.sv-kp-line { position:absolute;left:13px;top:34px;bottom:-8px;width:1px;background:#fbbf24;opacity:0.5; }
.sv-kp-warn { width:200px;background:var(--ink-3);border-radius:14px;padding:16px;border:1px solid #fed7aa; }
.sv-kp-warn-bar { width:100%;height:3px;background:linear-gradient(90deg,#f97316,transparent);border-radius:2px;margin-bottom:10px; }
.sv-kp-warn h4 { font-size:14px;color:#c2410c;margin:0 0 4px; }
.sv-kp-warn p { font-size:12px;color:#9a3412;line-height:1.5;margin:0; }
.sv-scenario-box { background:var(--ink-3);border-radius:14px;padding:20px;border:1px solid #99f6e4;margin-top:8px; }
.sv-scenario-bar { width:60px;height:3px;background:linear-gradient(90deg,var(--ice),transparent);border-radius:2px;margin-bottom:12px; }
.sv-scenario-story { font-size:15px;color:#134e4a;line-height:1.7;margin:6px 0; }
.sv-exer-grid { display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:8px; }
.sv-exer-card { padding:16px;border-radius:12px; }
.sv-exer-card--think { background:var(--ink-3);border:1px solid var(--ice); }
.sv-exer-card--do { background:var(--ink-3);border:1px solid #fef08a; }
.sv-exer-head { font-size:13px;font-weight:600;color:#4c1d95;margin-bottom:6px; }
.sv-exer-do .sv-exer-head { color:#854d0e; }
.sv-exer-text { font-size:14px;color:var(--grey);line-height:1.5; }
.sv-summary-sub { text-align:center;color:#64748b;font-size:14px;margin-bottom:16px; }
.sv-summary-cards { display:grid;grid-template-columns:1fr 1fr 1fr;gap:12px; }
.sv-summary-card { padding:16px;border-radius:12px;text-align:center;position:relative;overflow:hidden; }
.sv-summary-card--0 { background:linear-gradient(135deg,var(--ice),#eff6ff); }
.sv-summary-card--1 { background:linear-gradient(135deg,#fef3c7,#fffbeb); }
.sv-summary-card--2 { background:linear-gradient(135deg,#fce7f3,#fdf2f8); }
.sv-summary-card-bar { position:absolute;top:0;left:0;right:0;height:3px; }
.sv-summary-card--0 .sv-summary-card-bar { background:linear-gradient(90deg,var(--acid),var(--ice)); }
.sv-summary-card--1 .sv-summary-card-bar { background:linear-gradient(90deg,#f59e0b,#fbbf24); }
.sv-summary-card--2 .sv-summary-card-bar { background:linear-gradient(90deg,var(--acid),var(--ice)); }
.sv-summary-card-num { font-size:24px;font-weight:800;color:rgba(0,0,0,0.06); }
.sv-summary-card-text { font-size:13px;color:var(--grey);margin-top:4px;line-height:1.4; }
/* PPT 全屏放映 */
.hub-ppt-fullscreen { position: fixed; inset: 0; z-index: 9999; background: linear-gradient(170deg, #0c1929, #0f2440); display: flex; flex-direction: column; outline: none; }
.hub-ppt-fullscreen-bar { display: flex; align-items: center; padding: 12px 24px; color: #e0f2fe; font-size: 15px; font-weight: 500; background: rgba(0,0,0,0.3); border-bottom: 1px solid rgba(14,165,233,0.15); gap: 8px; }
.hub-ppt-fullscreen-bar button { color: #e0f2fe; border-color: rgba(255,255,255,0.2); background: transparent; }
.hub-ppt-fullscreen-bar button:hover { background: rgba(14,165,233,0.15); border-color: rgba(14,165,233,0.3); }
.hub-ppt-fullscreen-bar button:disabled { opacity: 0.3; cursor: not-allowed; }
.hub-ppt-fullscreen-body { flex: 1; display: flex; align-items: center; justify-content: center; overflow: auto; padding: 40px; }
.hub-ppt-card-visual {
  height: 120px; display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
}
.hub-ppt-card-pattern {
  position: absolute; inset: 0; opacity: 0.05;
  background-image: linear-gradient(45deg, #fff 25%, transparent 25%, transparent 75%, #fff 75%),
                    linear-gradient(45deg, #fff 25%, transparent 25%, transparent 75%, #fff 75%);
  background-size: 20px 20px; background-position: 0 0, 10px 10px;
}
.hub-ppt-card-icon-large { font-size: 36px; color: rgba(255,255,255,0.7); position: relative; z-index: 1; }
.hub-ppt-card-info { padding: 12px 14px; }
.hub-ppt-card-name { font-size: 13px; font-weight: 600; color: var(--hub-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.hub-ppt-card-details { display: flex; gap: 8px; margin-top: 4px; font-size: 12px; color: var(--hub-text-muted); }
.hub-ppt-card-hover {
  position: absolute; inset: 0; background: rgba(var(--acid-rgb), 0.9);
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: #fff; opacity: 0; transition: opacity 0.2s; border-radius: var(--hub-radius-md);
}
.hub-ppt-card-hover i { font-size: 32px; margin-bottom: 4px; }
.hub-ppt-card-hover span { font-size: 14px; font-weight: 500; }
.hub-ppt-card:hover .hub-ppt-card-hover { opacity: 1; }

/* ═══ Video Grid ═══ */
.hub-video-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 14px; }
.hub-video-card { border: 1px solid rgba(0,0,0,0.06); border-radius: var(--hub-radius-md); overflow: hidden; cursor: pointer; transition: var(--hub-transition); background: #fff; }
.hub-video-card:hover { border-color: var(--hub-accent); transform: translateY(-3px); box-shadow: var(--hub-shadow-md); }
.hub-video-card-thumb {
  height: 120px; display: flex; align-items: center; justify-content: center;
  position: relative;
}
.hub-video-play-btn {
  width: 48px; height: 48px; border-radius: 50%; background: rgba(255,255,255,0.25);
  display: flex; align-items: center; justify-content: center;
  backdrop-filter: blur(4px); transition: transform 0.2s;
}
.hub-video-play-btn i { font-size: 22px; color: rgba(255,255,255,0.85); }
.hub-video-card:hover .hub-video-play-btn { transform: scale(1.1); }
.hub-video-platform-badge {
  position: absolute; top: 8px; right: 8px; padding: 2px 6px; border-radius: 4px;
  font-size: 10px; font-weight: 600; background: rgba(251,114,153,0.85); color: #fff;
}
.hub-video-duration {
  position: absolute; bottom: 8px; right: 8px; padding: 2px 6px; border-radius: 4px;
  font-size: 10px; background: rgba(0,0,0,0.6); color: #fff;
}
.hub-video-card-info { padding: 10px 12px; }
.hub-video-card-info h4 { font-size: 13px; color: var(--hub-text); margin: 0 0 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.hub-video-card-info span { font-size: 11px; color: var(--hub-text-muted); }

/* ═══ Textbook List ═══ */
.hub-textbook-list { display: flex; flex-direction: column; gap: 10px; }
.hub-textbook-item {
  display: flex; align-items: center; gap: 16px; padding: 14px;
  background: var(--ink-3); border-radius: var(--hub-radius-md); cursor: pointer;
  transition: var(--hub-transition); border: 1px solid rgba(0,0,0,0.04);
}
.hub-textbook-item:hover { background: var(--ink-3); border-color: rgba(var(--acid-rgb), 0.15); }
.hub-textbook-cover {
  width: 44px; height: 56px; border-radius: 6px; display: flex;
  align-items: center; justify-content: center; color: #fff; font-size: 20px; flex-shrink: 0;
}
.hub-textbook-info { flex: 1; }
.hub-textbook-info h4 { font-size: 14px; color: var(--hub-text); margin: 0 0 3px; }
.hub-textbook-info p { font-size: 12px; color: var(--hub-text-muted); margin: 0; }

/* ═══ Exercises ═══ */
.hub-exercises { display: flex; flex-direction: column; gap: 12px; }
.hub-exercise {
  display: flex; gap: 14px; padding: 16px; background: #fff;
  border: 1px solid rgba(0,0,0,0.05); border-radius: var(--hub-radius-md);
  transition: var(--hub-transition);
}
.hub-exercise:hover { border-color: rgba(var(--acid-rgb), 0.12); }
.hub-exercise-num {
  width: 32px; height: 32px; border-radius: 50%; display: flex;
  align-items: center; justify-content: center; font-size: 13px; font-weight: 700;
  background: var(--hub-accent-glow); color: var(--hub-accent); flex-shrink: 0;
}
.hub-exercise-content { flex: 1; }
.hub-exercise-content h4 { font-size: 14px; color: var(--hub-text); margin: 0 0 6px; line-height: 1.6; }
.hub-exercise-options { margin: 8px 0; padding-left: 16px; display: flex; flex-direction: column; gap: 4px; }
.hub-exercise-opt { font-size: 13px; color: var(--grey); }
.hub-exercise-answer { margin-top: 8px; padding: 8px 12px; background: var(--ink-3); border-radius: 8px; font-size: 13px; color: #065f46; display: flex; align-items: center; gap: 6px; }

/* ═══ Preview Dialog ═══ */
.hub-preview-title { display: flex; align-items: center; gap: 10px; font-size: 16px; font-weight: 600; }
.hub-preview-title i { font-size: 18px; color: var(--hub-accent); }
.hub-preview-body { min-height: 400px; display: flex; flex-direction: column; }
.hub-preview-loading {
  flex: 1; display: flex; flex-direction: column; align-items: center;
  justify-content: center; gap: 12px; min-height: 350px;
}
.hub-loading-spinner {
  width: 44px; height: 44px; border: 3px solid rgba(var(--acid-rgb), 0.15);
  border-top-color: var(--hub-accent); border-radius: 50%;
  animation: hub-spin 0.8s linear infinite;
}
.hub-preview-loading p { font-size: 15px; color: var(--hub-text-secondary); margin: 0; }
.hub-preview-loading span { font-size: 12px; color: var(--hub-text-muted); }
.hub-preview-toolbar {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 12px; flex-wrap: wrap; gap: 8px;
}
.hub-preview-toolbar-left { display: flex; align-items: center; gap: 8px; }
.hub-preview-page { font-size: 14px; color: var(--hub-text-secondary); font-variant-numeric: tabular-nums; }
.hub-preview-toolbar-right { display: flex; gap: 8px; flex-wrap: wrap; }
.hub-preview-iframe {
  flex: 1; border: 1px solid rgba(0,0,0,0.06); border-radius: var(--hub-radius-md);
  min-height: 60vh; background: var(--ink-3);
}
.hub-slide-view {
  flex: 1; border: 1px solid rgba(0,0,0,0.06); border-radius: var(--hub-radius-md);
  overflow-y: auto; min-height: 400px;
}
.hub-preview-card {
  flex: 1; border: 1px solid rgba(0,0,0,0.06); border-radius: var(--hub-radius-md);
  padding: 56px 40px; text-align: center; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  background: linear-gradient(135deg, #f8fafc, #f0f2f5); min-height: 380px;
}
.hub-preview-card-icon { font-size: 64px; color: var(--hub-accent); margin-bottom: 16px; }
.hub-preview-card-icon i { display: block; }
.hub-preview-card h3 { font-size: 18px; color: var(--hub-text); margin: 8px 0; }
.hub-preview-card > p { font-size: 14px; color: var(--hub-text-secondary); margin: 4px 0 8px; }
.hub-preview-card-tags { display: flex; gap: 8px; align-items: center; margin: 12px 0 24px; }
.hub-preview-card-tags span { font-size: 13px; color: var(--hub-text-muted); }
.hub-preview-card-actions { display: flex; gap: 10px; flex-wrap: wrap; justify-content: center; }
.hub-preview-card-hint { font-size: 12px; color: var(--hub-text-muted); margin-top: 20px; }

/* ═══ Video Body ═══ */
.hub-video-body { min-height: 400px; }
.hub-embed-video { position: relative; padding-top: 56.25%; background: #000; border-radius: 10px; overflow: hidden; }
.hub-embed-video iframe { position: absolute; inset: 0; width: 100%; height: 100%; border: 0; }

/* ═══ NetEase native video (open.163.com via backend proxy) ═══ */
.hub-native-video { background: #000; border-radius: 10px; overflow: hidden; }
.hub-native-video .video-native-player { width: 100%; height: 480px; display: block; background: #000; outline: none; }
.hub-video-hint { padding: 6px 12px; font-size: 11px; color: rgba(255,255,255,0.55); background: rgba(0,0,0,0.4); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* ═══ Reader Body ═══ */
.hub-reader-body { min-height: 60vh; }
.hub-reader-iframe { width: 100%; min-height: 65vh; border: 1px solid rgba(0,0,0,0.06); border-radius: var(--hub-radius-sm); background: var(--ink-3); }

/* ═══ Import Dialog ═══ */
.hub-import-tabs >>> .el-tabs__header { margin-bottom: 20px; }
.hub-import-form { padding: 8px 0; }
.hub-import-hint { margin-left: 12px; color: var(--hub-text-muted); font-size: 12px; }

/* ═══ Global Dialog Styling ═══ */
>>> .el-dialog__header { padding: 20px 24px 16px; border-bottom: 1px solid rgba(0,0,0,0.04); }
>>> .el-dialog__body { padding: 20px 24px 24px; }
>>> .hub-detail-dialog .el-dialog__header { padding: 24px 28px 20px; }
>>> .hub-detail-dialog .el-dialog__body { padding: 0 28px 28px; }
>>> .hub-preview-dialog .el-dialog__header,
>>> .hub-video-dialog .el-dialog__header,
>>> .hub-reader-dialog .el-dialog__header,
>>> .hub-import-dialog .el-dialog__header { padding: 16px 24px; }
>>> .el-dialog { border-radius: 18px; overflow: hidden; box-shadow: 0 24px 80px rgba(0,0,0,0.12); }
>>> .el-dialog__title { font-size: 16px; font-weight: 600; }

/* ═══ Existing styles preserved ═══ */
.ch-detail { margin-bottom: 12px; padding: 12px 16px; background: var(--ink-3); border-radius: 8px; border: 1px solid rgba(0,0,0,0.04); }

/* ═══ Reader Layout (preserved) ═══ */
.reader-title-bar { display: flex; align-items: center; }
.reader-layout { display: flex; gap: 20px; height: 65vh; }
.reader-toc { width: 220px; flex-shrink: 0; border-right: 1px solid rgba(0,0,0,0.06); overflow-y: auto; padding-right: 8px; }
.reader-toc h4 { font-size: 14px; color: var(--hub-text); margin: 0 0 8px; padding-bottom: 8px; border-bottom: 1px solid rgba(0,0,0,0.06); }
.reader-toc-item { padding: 7px 10px; font-size: 13px; color: var(--grey); cursor: pointer; border-radius: 6px; margin: 2px 0; transition: var(--hub-transition); }
.reader-toc-item:hover { background: rgba(0,0,0,0.03); }
.reader-toc-item.active { background: var(--hub-accent-glow); color: var(--hub-accent); font-weight: 500; }
.reader-content { flex: 1; overflow-y: auto; display: flex; flex-direction: column; }
.reader-body { flex: 1; line-height: 2.1; font-size: 15px; color: var(--grey); }
.reader-nav { display: flex; align-items: center; justify-content: center; gap: 16px; padding: 12px 0; border-top: 1px solid rgba(0,0,0,0.06); margin-top: 16px; }
.reader-progress { font-size: 14px; color: var(--hub-text-secondary); }

/* ═══ MP4 Player (preserved) ═══ */
.mp4-player { background: var(--ink-2); border-radius: 10px; overflow: hidden; }
.mp4-screen { position: relative; min-height: 360px; cursor: pointer; background: #1a1f2e; overflow: hidden; }
.mp4-lecture-bg { min-height: 360px; padding: 24px 28px; display: flex; flex-direction: column; background: radial-gradient(ellipse at 30% 40%, rgba(var(--acid-rgb), 0.06) 0%, transparent 60%); }
.mp4-lecture-header { display: flex; align-items: center; gap: 10px; margin-bottom: 20px; }
.mp4-uni-badge { padding: 2px 10px; border-radius: 4px; background: rgba(var(--acid-rgb), 0.2); color: #a5b4fc; font-size: 11px; font-weight: 500; }
.mp4-course-label { font-size: 12px; color: #64748b; }
.mp4-content-area { flex: 1; display: flex; align-items: flex-start; padding: 8px 0; }
.mp4-content-card { width: 100%; padding: 20px 24px; background: rgba(255,255,255,0.03); border-radius: 10px; border: 1px solid rgba(255,255,255,0.06); }
.mp4-point-title { font-size: 18px; font-weight: 600; color: #f1f5f9; margin: 0 0 12px; line-height: 1.4; }
.mp4-point-detail { font-size: 14px; color: #94a3b8; line-height: 1.9; }
.mp4-instructor-bar { display: flex; align-items: center; gap: 8px; padding-top: 16px; border-top: 1px solid rgba(255,255,255,0.06); margin-top: 16px; }
.mp4-instructor-avatar { width: 28px; height: 28px; border-radius: 50%; background: rgba(var(--acid-rgb), 0.3); color: var(--ice); display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 600; }
.mp4-instructor-name { font-size: 12px; color: #64748b; }
.mp4-center-play { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.15); z-index: 2; }
.mp4-center-play i { font-size: 56px; color: rgba(255,255,255,0.85); filter: drop-shadow(0 2px 8px rgba(0,0,0,0.3)); transition: transform .2s; }
.mp4-center-play:hover i { transform: scale(1.1); }
.mp4-controls { display: flex; align-items: center; gap: 10px; padding: 10px 14px; background: #141A14; border-top: 1px solid #334155; }
.mp4-ctrl-btn { width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #cbd5e1; border-radius: 6px; transition: all .15s; font-size: 14px; }
.mp4-ctrl-btn:hover { color: #fff; background: rgba(255,255,255,0.08); }
.mp4-time-current, .mp4-time-total { font-size: 11px; color: #64748b; font-variant-numeric: tabular-nums; min-width: 36px; }
.mp4-time-current { text-align: right; }
.mp4-time-total { text-align: left; }
.mp4-progress-wrap { flex: 1; height: 28px; display: flex; align-items: center; cursor: pointer; position: relative; }
.mp4-progress-bar { width: 100%; height: 4px; background: #334155; border-radius: 2px; position: relative; transition: height .15s; }
.mp4-progress-wrap:hover .mp4-progress-bar { height: 6px; }
.mp4-progress-buffered { position: absolute; left: 0; top: 0; height: 100%; background: rgba(148,163,184,0.25); border-radius: 2px; }
.mp4-progress-fill { position: absolute; left: 0; top: 0; height: 100%; background: linear-gradient(90deg, var(--acid), var(--ice)); border-radius: 2px; }
.mp4-progress-thumb { position: absolute; top: 50%; width: 12px; height: 12px; border-radius: 50%; background: #fff; transform: translate(-50%,-50%); opacity: 0; transition: opacity .15s; box-shadow: 0 1px 4px rgba(0,0,0,0.4); pointer-events: auto; }
.mp4-progress-wrap:hover .mp4-progress-thumb, .mp4-progress-thumb:active { opacity: 1; }
.mp4-fade-enter-active, .mp4-fade-leave-active { transition: opacity .4s; }
.mp4-fade-enter, .mp4-fade-leave-to { opacity: 0; }

/* ═══ Responsive ═══ */
@media (max-width: 768px) {
  .hub-topbar-inner { flex-wrap: wrap; height: auto; padding: 10px 12px; gap: 8px; }
  .hub-nav { order: 3; width: 100%; overflow-x: auto; }
  .hub-search-wrap { width: 100%; }
  .hub-search-input { width: 100%; }
  .hub-hero { padding: 40px 16px 44px; }
  .hub-hero-title { font-size: 26px; }
  .hub-hero-stats { gap: 24px; }
  .hub-hero-stat { min-width: 80px; }
  .hub-course-grid { grid-template-columns: 1fr; }
  .hub-intro-stats { grid-template-columns: 1fr 1fr; }
  .hub-platform-grid { grid-template-columns: 1fr; }
  .hub-ppt-grid, .hub-video-grid { grid-template-columns: 1fr; }
  .hub-detail-header { flex-direction: column; text-align: center; }
  .hub-detail-tags { justify-content: center; }
  .reader-layout { flex-direction: column; height: auto; }
  .reader-toc { width: 100%; border-right: none; border-bottom: 1px solid rgba(0,0,0,0.06); max-height: 120px; }
}
</style>

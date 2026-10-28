<template>
  <div class="agent-layout">
    <!-- 左侧垂直 Agent 导航 -->
    <agent-sidebar
      :active="sel"
      @switch="switchAgent"
      @collab="goCollab"
      @report="goReport"
    />

    <!-- 右侧 Agent 工作区 -->
    <main class="agent-workspace">
      <div class="ws-header">
        <div class="ws-header-left">
          <span class="ws-agent-badge"><i :class="cur.icon"></i> {{ cur.agent }}</span>
          <span class="ws-agent-desc">{{ cur.desc || cur.strategy }}</span>
        </div>
        <span class="ws-closed-loop">
          <i class="el-icon-s-data"></i> 诊断 → 生成 → 校对 → 决策
        </span>
      </div>

      <!-- 输入与生成 -->
    <div class="gen-input-row">
      <div class="gen-input-wrap">
        <el-input
          v-model="topic"
          :placeholder="'输入学习主题，' + (cur ? cur.strategy : 'AI自动生成')"
          size="large"
          @keyup.enter.native="go"
          :disabled="busy"
        >
          <template slot="prepend">{{ cur ? cur.label : '资源' }}</template>
        </el-input>
      </div>
      <el-button
        type="primary"
        size="large"
        :loading="busy"
        :disabled="!topic.trim()"
        @click="go"
        class="gen-btn"
      >
        <i v-if="!busy" class="el-icon-magic-stick"></i>
        {{ busy ? 'AI 生成中...' : '开始生成' }}
      </el-button>
    </div>

    <!-- 视频时长选择（独立于生成按钮，选 video 类型即显示） -->
    <div class="video-duration-bar" v-if="sel === 'video' && !agnesVideoUrl">
      <span class="vd-label"><i class="el-icon-time"></i> 视频时长</span>
      <div class="vd-slider-wrap">
        <el-slider
          v-model="videoSeconds"
          :min="5"
          :max="10"
          :step="5"
          :show-tooltip="true"
          :format-tooltip="formatVideoTooltip"
          :disabled="busy || agnesLoading"
          class="vd-apple-slider"
        />
        <div class="vd-marks">
          <span>5 秒</span>
          <span>10 秒</span>
        </div>
      </div>
      <span class="vd-value">{{ videoSeconds }} 秒</span>
      <span class="vd-tip">5 秒/节点 · 最大 10 秒</span>
    </div>

    <!-- 主体内容区 -->
    <div class="gen-body" v-if="body">
      <!-- 质量评分 -->
      <div class="quality-bar" v-if="sc">
        <div class="quality-score">
          <span class="quality-label">质量评分</span>
          <el-rate
            v-model="sc"
            disabled
            show-score
            text-color="#c8a45c"
            score-template="{value}"
            :max="10"
            :low-threshold="4"
            :high-threshold="7"
          ></el-rate>
        </div>
        <el-button
          v-if="hid && !evaling"
          size="small"
          type="warning"
          icon="el-icon-refresh"
          @click="doEval"
          :loading="evaling"
        >
          重新评估
        </el-button>
      </div>

      <!-- 课程文档 -->
      <div v-if="sel === 'document'" class="content-panel doc-panel">
        <div class="panel-header">
          <i class="el-icon-document"></i> 课程文档
          <el-button size="mini" type="text" @click="exportDoc" style="float:right;color:#c8a45c;margin-right:8px" icon="el-icon-download">导出 DOC</el-button>
          <el-button size="mini" type="text" @click="copyContent" style="float:right;color:#c8a45c">复制内容</el-button>
        </div>
        <div class="markdown-body" v-html="md"></div>
      </div>

      <!-- 思维导图（Agnes Image 2.1 Flash AI 生成）-->
      <div v-if="sel === 'mind_map'" class="content-panel mindmap-panel">
        <div class="panel-header">
          <i class="el-icon-share"></i> 思维导图
          <span class="mm-badge">SVG Render</span>
          <div class="mm-tools">
            <el-button size="mini" icon="el-icon-zoom-in" circle @click="mmZoom(0.15)"></el-button>
            <el-button size="mini" icon="el-icon-zoom-out" circle @click="mmZoom(-0.15)"></el-button>
            <el-button size="mini" icon="el-icon-rank" circle @click="mmFit"></el-button>
            <el-button size="mini" icon="el-icon-download" circle @click="mmDownload" title="导出 SVG"></el-button>
          </div>
        </div>
        <div class="mm-hint-bar">
          <span><b>双击</b>编辑文字</span>
          <span><b>点击</b> +/− 折叠</span>
          <span><b>拖拽节点</b>自由布局</span>
          <span><b>拖拽空白</b>平移画布</span>
          <span><b>滚轮</b>缩放</span>
        </div>
        <div
          ref="mmSvg"
          class="mm-container"
          @mousedown="mmMouseDown"
          @wheel.prevent="mmWheel"
        ></div>
      </div>

      <!-- 练习题目（交互式） -->
      <div v-if="sel === 'exercise'" class="content-panel exercise-panel">
        <div class="panel-header">
          <i class="el-icon-edit-outline"></i> 练习题目
          <div class="exercise-actions">
            <el-button v-if="!quizSubmitted" size="mini" type="primary" @click="quizSubmit" :disabled="!quizParsed.length">提交答案</el-button>
            <el-button v-if="quizSubmitted" size="mini" type="success" @click="quizReset">重新作答</el-button>
            <el-button size="mini" icon="el-icon-download" @click="quizExportTxt">导出 TXT</el-button>
          </div>
        </div>

        <!-- 作答区 -->
        <div class="quiz-body" v-if="quizParsed.length > 0">
          <div v-for="(q, qi) in quizParsed" :key="qi" class="quiz-item" :class="{ correct: quizSubmitted && quizResults[qi] === true, wrong: quizSubmitted && quizResults[qi] === false }">
            <div class="quiz-q-header">
              <span class="quiz-q-num">{{ qi + 1 }}.</span>
              <span class="quiz-q-type" :style="{background: q.type === 'choice' ? '#6366f1' : q.type === 'fill' ? '#34d399' : q.type === 'tf' ? '#fbbf24' : '#fb7185'}">{{ q.typeLabel }}</span>
              <span class="quiz-q-text">{{ q.question }}</span>
            </div>

            <!-- 选择题 -->
            <div v-if="q.type === 'choice'" class="quiz-options">
              <el-radio-group v-model="quizAnswers[qi]" :disabled="quizSubmitted" size="small">
                <div v-for="(opt, oi) in q.options" :key="oi" class="quiz-option" :class="{ 'opt-correct': quizSubmitted && oi === q.answer, 'opt-wrong': quizSubmitted && Number(quizAnswers[qi]) === oi && oi !== q.answer }">
                  <el-radio :label="oi" class="quiz-radio">{{ opt }}</el-radio>
                </div>
              </el-radio-group>
            </div>

            <!-- 判断题 -->
            <div v-if="q.type === 'tf'" class="quiz-options">
              <el-radio-group v-model="quizAnswers[qi]" :disabled="quizSubmitted" size="small">
                <el-radio :label="0" class="quiz-radio">✓ 正确</el-radio>
                <el-radio :label="1" class="quiz-radio">✗ 错误</el-radio>
              </el-radio-group>
            </div>

            <!-- 填空题 / 简答题 -->
            <div v-if="q.type === 'fill' || q.type === 'short'" class="quiz-input-area">
              <el-input v-model="quizAnswers[qi]" :disabled="quizSubmitted" :placeholder="q.type === 'fill' ? '请输入答案...' : '请输入你的回答...'" size="small" clearable></el-input>
            </div>

            <!-- 答案解析 -->
            <div v-if="quizSubmitted" class="quiz-feedback">
              <div class="quiz-fb-row" v-if="q.type === 'choice'"><b>正确答案：</b>{{ q.options[q.answer] }}</div>
              <div class="quiz-fb-row" v-if="q.type === 'tf'"><b>正确答案：</b>{{ q.answer === 0 ? '正确 ✓' : '错误 ✗' }}</div>
              <div class="quiz-fb-row" v-if="q.type === 'fill' || q.type === 'short'"><b>参考答案：</b>{{ q.refAnswer }}</div>
              <div class="quiz-fb-row" v-if="q.explanation"><b>解析：</b>{{ q.explanation }}</div>
              <div class="quiz-fb-row" v-if="!q.explanation && (q.type === 'fill' || q.type === 'short')"><b>解析：</b>{{ q.refAnswer ? '关键知识点参见参考答案' : '见上文知识点详解' }}</div>
              <div class="quiz-fb-result">
                <span v-if="quizResults[qi] === true" class="fb-tag fb-ok">✓ 回答正确</span>
                <span v-else-if="quizResults[qi] === false" class="fb-tag fb-no">✗ 回答错误</span>
                <span v-else class="fb-tag fb-skip">— 未作答</span>
              </div>
            </div>
          </div>

          <!-- 成绩统计 -->
          <div v-if="quizSubmitted" class="quiz-score-bar">
            <div class="quiz-score-left">
              <span class="quiz-score-big">{{ quizCorrectCount }} / {{ quizParsed.length }}</span>
              <span class="quiz-score-pct">{{ quizParsed.length > 0 ? Math.round(quizCorrectCount / quizParsed.length * 100) : 0 }}分</span>
            </div>
            <div class="quiz-score-right">
              <el-progress :percentage="quizParsed.length > 0 ? Math.round(quizCorrectCount / quizParsed.length * 100) : 0" :color="quizParsed.length > 0 && quizCorrectCount / quizParsed.length >= 0.6 ? '#34d399' : '#fb7185'" :stroke-width="12"></el-progress>
            </div>
          </div>
        </div>

        <!-- 无题目提示 -->
        <div v-else-if="body" class="quiz-empty">
          <i class="el-icon-warning-outline"></i>
          <p>未能解析出题目，请尝试重新生成</p>
        </div>
      </div>

      <!-- 拓展阅读 -->
      <div v-if="sel === 'extended_reading'" class="content-panel reading-panel">
        <div class="panel-header"><i class="el-icon-reading"></i> 拓展阅读</div>
        <div class="markdown-body" v-html="md"></div>
      </div>

      <!-- 视频动画（AI生成 + TTS + MP4播放）-->
      <div v-if="sel === 'video'" class="content-panel video-panel">
        <div class="panel-header">
          <i class="el-icon-video-camera"></i> 教学视频
          <span class="anim-hint">{{ agnesVideoUrl ? 'MP4 已生成' : (agnesLoading ? '视频渲染中...' : 'AI 生成 · 语音讲解') }}</span>
          <div class="video-actions">
            <el-button size="mini" :type="videoTtsOn ? 'warning' : 'default'" icon="el-icon-microphone" @click="videoTtsOn = !videoTtsOn" :title="videoTtsOn ? '关闭语音' : '开启语音'">
              {{ videoTtsOn ? '语音中' : '语音关' }}
            </el-button>
            <el-button size="mini" icon="el-icon-video-play" @click="videoPlayFromStart" :disabled="!videoScenes.length">从头发播放</el-button>
            <el-button v-if="agnesVideoUrl" size="mini" type="primary" icon="el-icon-download" @click="downloadAgnesVideo">下载视频</el-button>
          </div>
        </div>

        <!-- MP4 视频播放器 -->
        <div class="video-player-wrapper" v-if="agnesVideoUrl">
          <div class="video-native-player">
            <video
              ref="nativeVideo"
              :src="agnesVideoUrl"
              controls
              autoplay
              style="width:100%;max-height:500px;border-radius:8px;display:block;background:#000"
              @play="videoPlaying=true"
              @pause="videoPlaying=false"
            >您的浏览器不支持视频播放</video>
            <div class="video-native-info">
              <span>✅ 真实 MP4 视频 — 可拖动进度条暂停/快进</span>
              <span class="video-format-badge">MP4（{{ videoSeconds }}秒）</span>
            </div>
          </div>
        </div>

        <!-- 渲染中 -->
        <div v-else-if="agnesLoading" class="anim-loading">
          <i class="el-icon-loading" style="font-size:32px"></i>
          <p style="margin-top:12px;color:#94a3b8">{{ agnesStatusText || 'AI 正在渲染 MP4 视频...' }}</p>
          <el-progress :percentage="45" :stroke-width="4" color="#60a5fa" style="max-width:300px;margin:12px auto"></el-progress>
        </div>

        <!-- 场景轮播 + TTS（回退，仅在无渲染任务时） -->
        <div v-else-if="videoScenes.length > 0" class="video-player-wrapper">
          <div class="video-scene-display" ref="videoScene">
            <transition name="scene-fade" mode="out-in">
              <div :key="videoSceneIdx" class="video-scene-card">
                <div class="scene-header">
                  <span class="scene-badge">{{ videoSceneIdx + 1 }} / {{ videoScenes.length }}</span>
                  <h2 class="scene-title">{{ videoScenes[videoSceneIdx].title }}</h2>
                </div>
                <div class="scene-items">
                  <div v-for="(item, ii) in videoScenes[videoSceneIdx].items" :key="ii" class="scene-item">
                    <span class="scene-bullet">{{ ii + 1 }}</span>
                    <span class="scene-text">{{ item }}</span>
                  </div>
                </div>
              </div>
            </transition>
          </div>
          <div class="video-progress-bar">
            <div class="progress-track" @click="videoSeek">
              <div class="progress-fill" :style="{width: videoProgress + '%'}"></div>
              <div v-for="(s, si) in videoScenes" :key="si" class="progress-marker" :style="{left: (si/videoScenes.length*100) + '%'}" :class="{ done: si <= videoSceneIdx }"></div>
            </div>
            <div class="progress-info">
              <span>第 {{ videoSceneIdx + 1 }} 节</span>
              <span v-if="videoTtsOn" style="color:#22c55e">🔊 语音讲解中</span>
            </div>
          </div>
        </div>

        <!-- 生成中 -->
        <div v-else-if="busy" class="anim-loading">
          <i class="el-icon-loading"></i> AI 正在生成教学内容...
        </div>

        <!-- 空闲 -->
        <div v-else class="markdown-body" style="padding:40px;text-align:center;color:#64748b">
          <i class="el-icon-video-camera" style="font-size:48px;display:block;margin-bottom:16px"></i>
          <p>输入主题，点击「开始生成」即可自动生成教学视频（含 MP4 + 语音讲解）</p>
        </div>
      </div>

      <!-- 代码案例（在线运行沙箱）-->
      <div v-if="sel === 'code_example'" class="content-panel code-panel">
        <div class="panel-header">
          <i class="el-icon-cpu"></i> 代码案例 · 在线运行
          <span class="code-hint">编辑代码后点击运行查看效果</span>
        </div>
        <!-- 无代码块时显示提示 -->
        <div v-if="codeBlocks.length === 0 && body" class="markdown-body" v-html="md"></div>
        <div v-else-if="codeBlocks.length === 0 && !body" style="padding:60px;text-align:center;color:#94a3b8">
          <i class="el-icon-cpu" style="font-size:48px;display:block;margin-bottom:16px;color:#6366f1"></i>
          <p>输入主题，点击「开始生成」获取可运行的代码示例</p>
        </div>
        <!-- 代码块标签栏 -->
        <div v-if="codeBlocks.length > 0" class="code-playground">
          <div class="code-tabs">
            <span
              v-for="(block, bi) in codeBlocks"
              :key="bi"
              :class="['code-tab', { active: activeCodeIdx === bi }]"
              @click="activeCodeIdx = bi"
            >
              <span class="code-tab-lang">{{ block.lang || 'code' }}</span>
              <span class="code-tab-num">#{{ bi + 1 }}</span>
            </span>
          </div>
          <!-- 当前代码块 -->
          <div v-if="codeBlocks[activeCodeIdx]" class="code-editor-pane">
            <div class="code-editor-toolbar">
              <span class="code-editor-lang">{{ codeBlocks[activeCodeIdx].lang || 'code' }}</span>
              <div class="code-editor-actions">
                <el-button size="mini" type="success" icon="el-icon-video-play" @click="runCode(activeCodeIdx)" :loading="codeBlocks[activeCodeIdx].running">
                  {{ codeBlocks[activeCodeIdx].running ? '运行中...' : '▶ 运行' }}
                </el-button>
                <el-button size="mini" icon="el-icon-document-copy" @click="copyCode(codeBlocks[activeCodeIdx].code)">复制</el-button>
              </div>
            </div>
            <textarea
              v-model="codeBlocks[activeCodeIdx].code"
              class="code-textarea"
              :rows="Math.max(8, Math.min(25, (codeBlocks[activeCodeIdx].code || '').split('\n').length))"
              spellcheck="false"
            ></textarea>
          </div>
          <!-- 运行结果区 -->
          <div class="code-result-pane">
            <div class="code-result-header">
              <span>
                <i class="el-icon-caret-right"></i> 运行结果
                <span :class="['result-badge', codeBlocks[activeCodeIdx] && codeBlocks[activeCodeIdx].exitOk === false ? 'badge-err' : codeBlocks[activeCodeIdx] && codeBlocks[activeCodeIdx].output ? 'badge-ok' : '']">
                  {{ codeBlocks[activeCodeIdx] && codeBlocks[activeCodeIdx].exitOk === false ? '✗ 错误' : codeBlocks[activeCodeIdx] && codeBlocks[activeCodeIdx].output ? '✓ 成功' : '等待运行' }}
                </span>
              </span>
              <el-button v-if="codeBlocks[activeCodeIdx] && codeBlocks[activeCodeIdx].output" size="mini" icon="el-icon-delete" @click="resetCode(activeCodeIdx)">清空</el-button>
            </div>
            <!-- 控制台输出 -->
            <div v-if="codeBlocks[activeCodeIdx] && codeBlocks[activeCodeIdx].output" class="code-console">
              <pre class="code-console-text">{{ codeBlocks[activeCodeIdx].output }}</pre>
            </div>
            <!-- iframe预览（HTML/CSS/JS） -->
            <div class="code-preview-wrap" v-if="codeBlocks[activeCodeIdx] && codeBlocks[activeCodeIdx].running && codeBlocks[activeCodeIdx].runnable">
              <div class="code-preview-header">🌐 实时预览</div>
              <iframe
                :ref="'sandbox-' + activeCodeIdx"
                class="code-sandbox"
                sandbox="allow-scripts allow-modals allow-forms allow-same-origin"
                :srcdoc="buildSandboxHtml(codeBlocks[activeCodeIdx])"
              ></iframe>
            </div>
            <!-- 未运行提示 -->
            <div v-if="codeBlocks[activeCodeIdx] && !codeBlocks[activeCodeIdx].output && !codeBlocks[activeCodeIdx].running" class="code-console-placeholder">
              点击「▶ 运行」按钮查看代码执行效果
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 实操指南（岗位技能分步骤） -->
    <div v-if="sel === 'operation_guide'" class="content-panel doc-panel">
      <div class="panel-header">
        <i class="el-icon-guide"></i> 实操指南 · 岗位技能
        <span class="code-hint">标准操作流程 + 验收标准 + 行业规范</span>
      </div>
      <div v-if="body" class="markdown-body" v-html="md"></div>
      <div v-else-if="!busy" style="padding:60px;text-align:center;color:#94a3b8">
        <i class="el-icon-guide" style="font-size:48px;display:block;margin-bottom:16px;color:#8b5cf6"></i>
        <p>输入主题，点击「开始生成」获取可落地的分步骤实操指南</p>
      </div>
    </div>

    <!-- PPT 课件内容 + 配图 -->
    <div v-if="sel === 'ppt' && body" class="content-panel ppt-content-panel">
      <div class="panel-header">
        <i class="el-icon-data-board"></i> PPT 课件内容
        <span v-if="pptImageLoading" style="font-size:12px;color:#94a3b8;margin-left:8px">
          <i class="el-icon-loading"></i> 搜索配图中...
        </span>
      </div>
      <div class="markdown-body" v-html="md"></div>
      <!-- PPT 配图展示 -->
      <div v-if="pptImages.length > 0" class="ppt-images-gallery">
        <div class="gallery-title"><i class="el-icon-picture-outline"></i> 课件配图 ({{ pptImages.length }} 张)</div>
        <div class="gallery-grid">
          <div v-for="(img, ii) in pptImages" :key="ii" class="gallery-item">
            <img :src="img.url" :alt="img.alt" class="gallery-img" loading="lazy" />
            <div class="gallery-info">
              <span class="gallery-keyword">{{ img.keyword }}</span>
              <el-tag size="mini" type="info">{{ img.source || 'AI' }}</el-tag>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div class="gen-empty" v-if="!body && !busy">
      <div class="empty-icon">
        <i class="el-icon-files"></i>
      </div>
      <h3>选择资源类型，输入主题开始生成</h3>
      <p>支持课程文档、思维导图、练习题目、拓展阅读、视频动画、代码案例</p>
      <div class="empty-tips">
        <span>示例主题：</span>
        <el-tag
          v-for="t in ['勾股定理','牛顿力学','化学元素周期表','二次函数','光合作用']"
          :key="t"
          size="small"
          style="margin:4px;cursor:pointer"
          @click="topic=t;go()"
        >{{ t }}</el-tag>
      </div>
    </div>

    <!-- 生成历史侧边 -->
    <div class="history-section" v-if="his && his.length > 0">
      <div class="history-header">
        <i class="el-icon-time"></i> 生成历史
      </div>
      <div
        v-for="r in his"
        :key="r.id"
        :class="['history-item', { active: hid === r.id }]"
        @click="viewRec(r)"
      >
        <div class="history-item-header">
          <i :class="gi(r.resourceType)"></i>
          <span class="history-item-type">{{ gl(r.resourceType) }}</span>
        </div>
        <div class="history-item-title">{{ r.title }}</div>
        <div class="history-item-time">{{ formatTime(r.createTime) }}</div>
        <el-button
          size="mini"
          type="text"
          icon="el-icon-delete"
          class="history-item-del"
          @click.stop="delRec(r)"
          title="删除"
        ></el-button>
      </div>
    </div>
    </main>
  </div>
</template>

<script>
/* eslint-disable */
import api from '@/api/resourceGeneration'
import AgentSidebar from './AgentSidebar'

export default {
  name: 'ResourceGen',
  components: { AgentSidebar },
  data: function () {
    return {
      sel: 'document',
      topic: '',
      busy: false,
      body: '',
      sc: null,
      evaling: false,
      his: [],
      hid: null,
      mmObj: null,
      mmTree: null,
      mmScale: 1,
      mmPanX: 0,
      mmPanY: 0,
      animHtml: '',
      videoHtml: '',
      videoUrl: '',
      videoBlob: null,
      videoPlaying: false,
      videoRendering: false,
      videoFormat: 'webm',
      videoTimer: null,
      videoFrame: 0,
      // Agnes 2.0 视频生成
      agnesVideoUrl: '',
      agnesLoading: false,
      agnesTaskId: '',
      agnesPollTimer: null,
      agnesStatusText: '',
      // 练习题目交互
      quizParsed: [],
      quizAnswers: [],
      quizResults: [],
      quizSubmitted: false,
      // 视频 TTS 讲解
      videoDuration: 'standard',          // short/standard/comprehensive
      videoSeconds: 5,                    // 视频时长（秒），5秒/节点，最大10秒
      videoScenes: [],
      videoSceneIdx: 0,
      videoProgress: 0,
      videoTtsOn: true,
      videoTimer: null,
      videoTtsQueue: [],
      videoTtsSpeaking: false,
      // 代码运行
      codeBlocks: [],
      activeCodeIdx: 0,
      // PPT 配图搜索
      pptImages: [],
      pptImageLoading: false,
      types: [
        { key: 'document', label: '课程文档', icon: 'el-icon-document', desc: '结构化 Markdown', agent: 'Doc Agent', strategy: '生成完整教学文档' },
        { key: 'mind_map', label: '思维导图', icon: 'el-icon-share', desc: '交互式知识图谱', agent: 'MindMap Agent', strategy: 'JSON树->markmap导图' },
        { key: 'exercise', label: '练习题目', icon: 'el-icon-edit-outline', desc: '多题型+解析', agent: 'Quiz Agent', strategy: '选择题/填空/简答' },
        { key: 'extended_reading', label: '拓展阅读', icon: 'el-icon-reading', desc: '前沿延伸材料', agent: 'Reading Agent', strategy: '深度阅读+案例' },
        { key: 'video', label: '视频动画', icon: 'el-icon-video-camera', desc: '教学视频脚本', agent: 'Video Agent', strategy: 'AI生成完整知识点视频' },
        { key: 'code_example', label: '代码案例', icon: 'el-icon-cpu', desc: '可运行代码', agent: 'Code Agent', strategy: '沙箱可执行代码' },
        { key: 'operation_guide', label: '实操指南', icon: 'el-icon-guide', desc: '分步骤岗位实操', agent: 'Guide Agent', strategy: '标准流程+验收标准' }
      ]
    }
  },
  computed: {
    cur: function () {
      var self = this
      return this.types.find(function (t) { return t.key === self.sel }) || this.types[0]
    },
    allAgents: function () {
      return this.types.concat([{ key: 'ppt', label: 'PPT课件', icon: 'el-icon-data-board', agent: 'PPT Agent', desc: '12页校园风课件 + PPTX下载' }])
    },
    md: function () {
      if (!this.body) return ''
      var h = this.body.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      h = h.replace(/```(\w*)\n([\s\S]*?)```/g, function (_, l, c) { return '<pre class="cb"><code>' + c.replace(/</g, '&lt;') + '</code></pre>' })
      h = h.replace(/^### (.+)/gm, '<h3>$1</h3>').replace(/^## (.+)/gm, '<h2>$1</h2>').replace(/^# (.+)/gm, '<h1>$1</h1>')
      h = h.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>').replace(/`([^`]+)`/g, '<code class="ic">$1</code>')
      h = h.replace(/^[-*] (.+)/gm, '<li>$1</li>').replace(/(<li>.*<\/li>\n?)+/g, '<ul>$&</ul>')
      h = h.replace(/\n{2,}/g, '</p><p>').replace(/\n/g, '<br>')
      return '<p>' + h + '</p>'
    },
    quizCorrectCount: function () {
      return this.quizResults.filter(function (r) { return r === true }).length
    }
  },
  mounted: function () {
    var q = this.$route.query && this.$route.query.type
    if (q) {
      var f = this.types.find(function (t) { return t.key === q })
      if (f) this.sel = q
    }
    this.lh()
  },
  beforeDestroy: function () { if (this.agnesPollTimer) { clearInterval(this.agnesPollTimer); this.agnesPollTimer = null } if (this._mmOnMove) { document.removeEventListener('mousemove', this._mmOnMove) } if (this._mmOnUp) { document.removeEventListener('mouseup', this._mmOnUp) } },
  methods: {
    onVideoDurationChange: function () {
      if (this.$message) this.$message.success('已切换时长模式，下次生成生效')
    },
    lh: async function () { try { var r = await api.list({ userId: this.$store.getters.userId || 0 }); if (r && r.code === 1) this.his = Array.isArray(r.response) ? r.response : [] } catch (e) {} this.hid = null },
    goCollab: function () { this.$router.push('/resource-agent/collaboration') },
    goReport: function () { this.$router.push('/evaluation-agent/experiment') },
    switchAgent: function (t) {
      if (t.key === 'ppt') { this.$router.push('/resource-agent/ppt-agent'); return }
      if (t.key !== this.sel) { this.$router.replace({ path: '/resource-agent/generate', query: { type: t.key } }) }
    },
    resetState: function () {
      this.body = ''; this.sc = null; this.hid = null
      this.quizParsed = []; this.quizAnswers = []; this.quizResults = []; this.quizSubmitted = false
      this.videoScenes = []; this.videoSceneIdx = 0; this.videoProgress = 0; this.codeBlocks = []; this.activeCodeIdx = 0
      this.pptImages = []; this.agnesVideoUrl = ''; this.agnesLoading = false; this.agnesTaskId = ''
      this.stopVideoTts()
      if (this.mmObj) { try { this.mmObj.destroy() } catch (e) {} this.mmObj = null }
    },
    // ═══════════════════════════════════════════
    // 🚫 stripAllMeta — 终极元描述清洗函数
    // ═══════════════════════════════════════════
    stripAllMeta: function (text) {
      if (!text || typeof text !== 'string') return ''
      var t = text
      // 1. 删除所有以元描述关键词开头的整行（包括前面的空白）
      var metaLineStarters = [
        '好的', 'OK', 'Hi', 'Hello', '你好', '您好',
        '以下是为您', '以下是关于', '下面我来', '下面我', '让我来', '我将为您', '我将按照', '我会按照',
        '首先', '首先让我', '首先我们来', '首先我们',
        '总的来说', '综上所述', '总而言之', '以上就是', '以上是', '以上内容', '以上为', '以上所述',
        '我们被要求', '根据您的要求', '根据要求', '基于您提供的', '基于您的', '针对您提出的', '针对您',
        '为您生成', '为您准备', '为您准备了', '我来为您', '我来给', '我为您', '我理解', '我理解了',
        '收到您的', '明白了', '明白您的', '现在我', '现在我来', '现在让我', '现在让',
        '接下来我', '接下来让', '接下来我们', '现在让我们', '接下来',
        '我们来看', '我们来学习', '我们来了解', '我们来探讨', '我们来分析',
        '本节课我们', '本章我们', '本节我们', '本课我们', '本单元我们',
        '我们已经', '我们已经完成', '我们已经了解',
        '让我们', '让我们一起', '让我们来', '让我们开始', '请大家', '请同学们',
        '值得注意的是', '需要说明的是', '需要指出的是', '特别说明', '温馨提示', '友情提示', '小贴士',
        '希望这', '希望以上', '希望本', '如需更多', '如果需要', '如有需要', '若要获取', '欢迎继续', '如果还有',
        '让我们回顾', '回顾一下', '回顾前面', '在开始之前', '在结束之前', '最后', '最后再',
        '补充说明', '额外说明', '附加说明', '备注',
      ]
      for (var i = 0; i < metaLineStarters.length; i++) {
        var s = metaLineStarters[i]
        // 删除以这些词开头的整行（可能前面有 markdown 标记）
        t = t.replace(new RegExp('^[#*\\-\\s]*' + s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '.*?(?:\\n|$)', 'gim'), '')
      }
      // 2. 删除以 # 号开头的元描述标题段落（直到下一个标题或空行）
      t = t.replace(/^#{1,3}\s*(前言|概述|说明|引言|介绍|写在前面|阅读提示|使用说明|编者按|写在开头|预备知识|背景知识|课程导入)[：:]*[\s\S]*?(?=\n#{1,3}\s|\n\n\S|\n*$)/gim, '')
      t = t.replace(/^#{1,3}\s*(总结|小结|后记|写在最后|结语|结束语|致谢|参考资料|参考文献|推荐阅读|延伸阅读|课后作业|思考题|讨论题)[：:]*[\s\S]*?$/gim, '')
      // 3. 删除元描述段落（整段，非标题格式）
      t = t.replace(/\n{0,2}(我们被要求|根据您的要求|基于您提供的|针对您提出的|为您准备了|以上就是|以上是|以上内容).{0,200}[.。！!？?]\s*/gi, '')
      t = t.replace(/\n{0,2}(希望这[份些个].{0,100}|如需更多.{0,100}|如果需要.{0,100}|欢迎继续.{0,100}|如果还有.{0,100})[.。！!]?\s*$/gim, '')
      // 4. 删除"I"、"we"、"你"、"您"开头的教学引导句
      t = t.replace(/^(I |We |You |我 |我们 |你 |您 )(.{0,100})[\n\r]+/gim, '')
      // 5. 删除纯英文的元描述（常见于 AI 回复开头）
      t = t.replace(/^(Here is|Here are|Below is|The following is|I have|I've|I will|I'll|Let me|Let's|Now let|Now I).{0,150}[\n\r]+/gim, '')
      // 6. 删除 markdown 代码块之外的解释性文字
      t = t.replace(/\n{0,2}(请注意|注意：|提示：|小提示：|关键点：|重点：|核心：).{0,80}[\n\r]+/gim, '\n')
      // 7. 清理多个连续空行
      t = t.replace(/\n{3,}/g, '\n\n')
      // 8. 清理开头的空白
      t = t.replace(/^[\s\n\r]+/, '')
      // 9. 清理结尾的空白
      t = t.replace(/[\s\n\r]+$/, '')
      return t
    },
    go: async function () {
      if (!this.topic.trim()) { this.$message.warning('请输入学习主题'); return }
      if (this.busy) return
      this.busy = true; this.body = ''; this.sc = null; this.hid = null; this.animHtml = ''; this.videoHtml = ''
      this.quizParsed = []; this.quizAnswers = []; this.quizResults = []; this.quizSubmitted = false; this.videoPlaying = false
      this.agnesVideoUrl = ''; this.agnesLoading = false; this.agnesTaskId = ''; this.agnesStatusText = ''
      this.videoScenes = []; this.videoSceneIdx = 0; this.codeBlocks = []; this.activeCodeIdx = 0
      this.pptImages = []; this.pptImageLoading = false
      this.stopVideoTts()
      if (this.agnesPollTimer) { clearInterval(this.agnesPollTimer); this.agnesPollTimer = null }
      if (this.mmObj) { try { this.mmObj.destroy() } catch (e) {} this.mmObj = null }
      var self = this
      try {
        await api.generate({ userId: this.$store.getters.userId || 0, resourceType: this.sel, topic: this.topic.trim() }, function (c, f) { self.body = f; self.ps(c) })
        // ═══════════════════════════════════════════
        // 🚫 元描述清洗 — PPT和Video保留完整教学结构
        // ═══════════════════════════════════════════
        if (this.sel === 'ppt' || this.sel === 'video') {
          self.body = (self.body || '').replace(/^[\s\n\r]+/, '').replace(/[\s\n\r]+$/, '').replace(/\n{4,}/g, '\n\n\n')
        } else {
          self.body = self.stripAllMeta(self.body || '')
        }
        self.$message.success('生成完成')
        this.$message.success('生成完成')
        this.$nextTick(function () {
          if (this.sel === 'mind_map') {
            this.renderMM()
          }
          if (this.sel === 'exercise') this.parseQuiz()
          if (this.sel === 'video') {
            this.parseVideoScenes()
            // 自动提交 Agnes Video V2.0 生成 MP4 视频
            this.startAgnesVideoGen()
          }
          if (this.sel === 'code_example') this.parseCodeBlocks()
          if (this.sel === 'ppt') this.searchPptImages()
        }.bind(this))
        await this.lh()
      } catch (e) { console.error(e); this.$message.error('生成失败') } finally { this.busy = false }
    },

    // ====== PPT 配图搜索 ======
    searchPptImages: async function () {
      var s = this
      if (!this.body || !this.topic) return
      this.pptImageLoading = true; this.pptImages = []
      try {
        var slideTitles = []
        var re = /^##\s+(.+)/gm; var m
        while ((m = re.exec(this.body)) !== null) {
          var t = m[1].trim()
          if (!/封面|目标|总结|作业|结束|目录|学习目标/i.test(t)) slideTitles.push(t)
        }
        if (slideTitles.length === 0) {
          slideTitles = this.topic.split(/[-—、，,\s]+/).filter(function(x){return x.length >= 2})
        }
        var toSearch = slideTitles.slice(0, 5)
        for (var i = 0; i < toSearch.length; i++) {
          try {
            var res = await api.searchImages(toSearch[i], 1)
            if (res && res.code === 1 && res.response && res.response.images && res.response.images.length > 0) {
              s.pptImages.push({ keyword: toSearch[i], url: res.response.images[0].localUrl, alt: res.response.images[0].alt, source: res.response.images[0].source })
            }
          } catch (e) {}
        }
      } catch (e) {} finally { this.pptImageLoading = false }
    },

    // ====== SVG 思维导图渲染（Agnes API 不可用时回退） ======
    // 配色方案 — Apple 风格莫兰迪色系（低饱和、高级感）
    mmColors: function () {
      return [
        { fill: 'rgba(0, 122, 255, 0.08)', stroke: '#007AFF' },   // Apple Blue
        { fill: 'rgba(88, 86, 214, 0.07)', stroke: '#5856D6' },   // Purple
        { fill: 'rgba(52, 199, 89, 0.07)', stroke: '#34C759' },   // Green
        { fill: 'rgba(255, 149, 0, 0.07)', stroke: '#FF9500' },   // Orange
        { fill: 'rgba(255, 59, 48, 0.07)', stroke: '#FF3B30' },   // Red
        { fill: 'rgba(175, 82, 222, 0.07)', stroke: '#AF52DE' },  // Violet
        { fill: 'rgba(90, 200, 250, 0.07)', stroke: '#5AC8FA' },  // Cyan
        { fill: 'rgba(172, 142, 104, 0.07)', stroke: '#AC8E68' }  // Brown
      ]
    },

    mmGetColor: function (d) {
      var cols = this.mmColors()
      return cols[Math.min(d, cols.length - 1)]
    },

    mmFontSize: function (d) {
      return [17, 14, 12, 11, 10, 10, 10][Math.min(d, 6)]
    },

    mmFontWeight: function (d) {
      return d === 0 ? 700 : d === 1 ? 600 : 500
    },

    // ═══════════════════════════════════════════════════════════════
    // 思维导图渲染 — 统一使用 _mmLayout + _mmShiftY（已放弃旧版 mmLayout）
    // ═══════════════════════════════════════════════════════════════
    mmTextW: function (text, fs) {
      if (!text) return 80
      var w = 0
      for (var i = 0; i < text.length; i++) {
        var c = text.charCodeAt(i)
        if (c > 0x4e00) w += fs * 1.05       // 中文字符 ≈ font-size * 1.05
        else if (c > 0xff) w += fs * 0.8     // 全角符号
        else if (c > 0x7f) w += fs * 0.7     // 扩展Latin
        else w += fs * 0.6                    // ASCII字符
      }
      return Math.ceil(w + 44)  // padding 22px each side
    },

    mmParse: function (content) {
      content = (content || '').trim()
      if (!content) return null

      // ① 提取 ``` 代码块中的内容（Mermaid / JSON）
      var fenced = content.match(/```(?:mermaid|json)?\s*\n?([\s\S]*?)\n?\s*```/)
      if (fenced) { content = fenced[1].trim() }

      // ② 尝试 JSON
      try { return this.mmNorm(JSON.parse(content)) } catch (e) {}
      var firstBrace = content.indexOf('{')
      var lastBrace = content.lastIndexOf('}')
      if (firstBrace >= 0 && lastBrace > firstBrace) {
        var jsonStr = content.substring(firstBrace, lastBrace + 1)
        var fixed = jsonStr.replace(/,\s*}/g, '}').replace(/,\s*]/g, ']')
        try { return this.mmNorm(JSON.parse(fixed)) } catch (e2) {}
        try { return this.mmNorm(JSON.parse(jsonStr)) } catch (e3) {}
      }

      // 结构修复：AI输出常有回声文本，切掉开头定位到实际内容
      var ctIdx = content.lastIndexOf('- 中心主题')
      if (ctIdx < 0) ctIdx = content.lastIndexOf('-中心主题')
      if (ctIdx > 0) content = content.substring(ctIdx)
      // 每个一级节点（-一、, -二、...）前强制插入换行+缩进
      content = content.replace(/([^\n])(-[ ]*[一二三四五六七八九十][、])/g, '$1\n  $2')
      // 根节点后换行
      content = content.replace(/(-[ ]*中心主题[：:][^\n]+)(\n?)/, '$1\n')
      // 每行中第二个 - 标记处拆分（处理L1标题+首子节点同行）
      content = content.replace(/^(\s{2}-[^\n]+?)(\s{2,4}-[ ]+)/gm, '$1\n    $2')
      // 清理多余空行
      content = content.replace(/\n{3,}/g, '\n\n')
      var lines = content.split('\n')
      var root = { title: this.topic || '思维导图', children: [], id: 'r' }

      // ③ Mermaid mindmap 解析（缩进层级树）
      var mmStart = -1
      for (var mi = 0; mi < lines.length; mi++) {
        if (/^\s*mindmap\b/i.test(lines[mi])) { mmStart = mi; break }
      }
      if (mmStart >= 0) {
        var mmStack = [{ node: root, indent: -1 }]
        for (var mj = mmStart + 1; mj < lines.length; mj++) {
          var mL = lines[mj]
          var mIndent = mL.search(/\S/)
          var mText = mL.replace(/^\s*/, '').replace(/::icon\([^)]*\)/g, '').replace(/[🔑⚠️💡🎯⭐🔥]/g, '').trim()
          if (!mText || mText.startsWith('::') || mText.startsWith('```') || /^root\(/.test(mText)) continue
          mText = mText.replace(/^\(\(/, '').replace(/\)\)$/, '').replace(/^\(/, '').replace(/\)$/, '').trim()
          if (!mText) continue
          var nn = { title: mText.substring(0, 40), children: [], id: 'mm' + mj }
          while (mmStack.length > 1 && mIndent <= mmStack[mmStack.length - 1].indent) mmStack.pop()
          if (mmStack.length) mmStack[mmStack.length - 1].node.children.push(nn)
          mmStack.push({ node: nn, indent: mIndent })
        }
        if (root.children.length > 0) return root
      }

      // ④ 缩进列表解析（- 缩进层级树）
      var listStack = [{ node: root, indent: -1 }]
      var hasListItems = false
      for (var li = 0; li < lines.length; li++) {
        var l = lines[li]
        var trimmed = l.trim()
        if (!trimmed || trimmed.startsWith('```')) {
          // 遇到代码块结束标记，继续
          continue
        }
        var listMatch = trimmed.match(/^[-*]\s+(.+)/)
        if (listMatch) {
          hasListItems = true
          var indent = l.search(/\S/)
          var text = listMatch[1].replace(/\*\*/g, '').replace(/[🔑⚠️💡🎯⭐🔥]/g, '').trim()
          if (!text) continue
          // 第一行0缩进：作为根节点标题，不创建重复层级
          if (indent === 0 && root.children.length === 0) {
            root.title = text.substring(0, 80); root.id = 'lb' + li
            continue
          }
          var item = { title: text.substring(0, 80), children: [], id: 'lb' + li }
          while (listStack.length > 1 && indent <= listStack[listStack.length - 1].indent) listStack.pop()
          if (listStack.length) listStack[listStack.length - 1].node.children.push(item)
          listStack.push({ node: item, indent: indent })
        }
      }
      if (hasListItems && root.children.length > 0) return root

      // ⑤ Markdown 标题解析（## / ### / ####）
      var hdStack = [{ node: root, d: 0 }]
      for (var hi = 0; hi < lines.length; hi++) {
        var hl = lines[hi].trim(); if (!hl) continue
        var lm = hl.match(/^(#{1,6})\s+(.+)/)
        if (lm) {
          var dd = lm[1].length; var tt = lm[2].replace(/\*\*/g, '').trim()
          var hNode = { title: tt.substring(0, 80), children: [], id: 'nh' + hi }
          while (hdStack.length && hdStack[hdStack.length - 1].d >= dd) hdStack.pop()
          if (hdStack.length) hdStack[hdStack.length - 1].node.children.push(hNode)
          hdStack.push({ node: hNode, d: dd })
        }
      }
      if (root.children.length > 0) return root

      // ⑥ 增强降级：段落 → 知识树
      var fallback = { title: this.topic || '思维导图', children: [], id: 'r' }
      // 将内容按段落分组，构建3-4层深度树
      var paragraphs=content.split(/\n\n+/).filter(function(p){return p.trim().length>5})
      if(paragraphs.length>=3){
        // 有足够段落时，构建深层结构
        for(var j=0;j<Math.min(paragraphs.length,7);j++){
          var pText=paragraphs[j].trim()
          var pLines=pText.split('\n').filter(function(l){return l.trim().length>0})
          var pTitle=pLines[0].replace(/^[#*\-\d.]+\s*/,'').trim().substring(0,40)
          // 二级节点
          var level2={title:pTitle,children:[],id:'p'+j}
          // 从段落中提取子知识点
          var subItems=[]
          for(var k=1;k<pLines.length;k++){
            var sl=pLines[k].replace(/^[#*\-\d.]+\s*/,'').trim()
            if(sl&&sl.length>3)subItems.push(sl.substring(0,50))
          }
          // 如果子知识点不够，自动拆分主内容
          if(subItems.length<3){
            var mainText=pLines.length>1?pLines.slice(1).join(' '):pText
            var fragments=mainText.split(/[，,。；;、\s]{2,}/).filter(function(f){return f.length>4})
            for(var fi=0;fi<Math.min(fragments.length,6);fi++){
              subItems.push(fragments[fi].trim().substring(0,40))
            }
          }
          // 第三层：为每个子知识点创建展开节点
          for(var si=0;si<Math.min(subItems.length,5);si++){
            var item=subItems[si]
            var l3={title:item,children:[],id:'p'+j+'s'+si}
            // 第四层：更细粒度的知识点
            var detailTerms=item.split(/[，,、\s]+/).filter(function(t){return t.length>1})
            if(detailTerms.length>=2){
              for(var di=0;di<Math.min(detailTerms.length,4);di++){
                if(detailTerms[di].length>1){
                  l3.children.push({title:detailTerms[di],children:[],id:'p'+j+'s'+si+'d'+di})
                }
              }
            }
            level2.children.push(l3)
          }
          fallback.children.push(level2)
        }
      }else{
        // 内容较少时，仍构建2-3层
        for(var jj=0;jj<lines.length;jj++){
          var fl=lines[jj].trim()
          if(fl&&fl.length>2&&!fl.startsWith('```')){
            var node={title:fl.substring(0,60),children:[],id:'f'+jj}
            // 为每个节点尝试创建子节点
            var words=fl.split(/[，,、\s]+/).filter(function(w){return w.length>2})
            for(var wi=0;wi<Math.min(words.length,3);wi++){
              if(words[wi].length>2)node.children.push({title:words[wi],children:[],id:'f'+jj+'w'+wi})
            }
            fallback.children.push(node)
          }
        }
      }
      return fallback.children.length > 0 ? fallback : null
    },

    mmNorm: function (n) {
      if (!n) return null
      if (Array.isArray(n)) {
        // 数组作为根节点子节点
        return { id: 'r', title: this.topic || '思维导图', children: n.map(function (c) { return this.mmNorm(c) }.bind(this)).filter(Boolean), collapsed: false }
      }
      var title = (n.title || n.name || n.text || n.label || '节点')
      // 保留部分数学符号
      title = String(title).replace(/[*#]/g, '').trim()
      var children = (n.children || n.childNodes || n.nodes || n.items || n.subtopics || [])
      return {
        id: n.id || ('n_' + Math.random().toString(36).slice(2, 8)),
        title: title || '节点',
        children: children.map(function (c) { return this.mmNorm(c) }.bind(this)).filter(Boolean),
        collapsed: false
      }
    },

    mmLayout: function (node, d, x) {
      if (!node) return { w: 0, h: 0 }

      var fs = this.mmFontSize(d)
      var nodeW = Math.max(60, this.mmTextW(node.title, fs))
      var nodeH = 36
      var gapY = [36, 28, 22, 18, 16, 14, 12][Math.min(d, 6)]

      var children = (node.children || []).filter(function (c) { return !c._hidden })

      if (children.length === 0) {
        node._ly = { x: x, y: 0, w: nodeW, h: nodeH, cx: x + nodeW / 2, cy: nodeH / 2, d: d }
        return { w: nodeW, h: nodeH }
      }

      // 水平间距：根到一级分支用320px，后续260px
      var childX = x + nodeW + (d === 0 ? 200 : 140)
      var childSubHeights = []
      var childrenTotalH = 0
      for (var i = 0; i < children.length; i++) {
        var cr = this.mmLayout(children[i], d + 1, childX)
        childSubHeights.push(cr.h)
        childrenTotalH += cr.h
        if (i < children.length - 1) childrenTotalH += gapY
      }

      // 当前节点的总高度 = max(自身高度, 所有子节点总高度)
      var totalH = Math.max(nodeH, childrenTotalH)

      // 将子节点垂直均匀分布（确保起始位置居中）
      var curY = Math.floor((totalH - childrenTotalH) / 2)
      for (var j = 0; j < children.length; j++) {
        var subH = childSubHeights[j]
        children[j]._ly = {
          x: childX,
          y: curY,
          w: children[j]._ly ? children[j]._ly.w : 80,
          h: subH,
          cx: childX + (children[j]._ly ? children[j]._ly.w : 80) / 2,
          cy: curY + subH / 2,
          d: d + 1
        }
        curY += subH + gapY
      }

      // 父节点在左侧垂直居中（相对于子树总高）
      var nodeY = Math.floor((totalH - nodeH) / 2)
      node._ly = { x: x, y: nodeY, w: nodeW, h: nodeH, cx: x + nodeW / 2, cy: nodeY + nodeH / 2, d: d }

      // 返回整棵子树的总宽（仅当前节点宽 + 子节点右侧最宽者）和高
      var maxChildW = 0
      for (var k = 0; k < children.length; k++) {
        if (children[k]._ly) maxChildW = Math.max(maxChildW, children[k]._ly.x + children[k]._ly.w - x)
      }
      return { w: Math.max(nodeW, maxChildW), h: totalH }
    },

    mmResetHidden: function (node) {
      if (node.collapsed && node.children) {
        for (var i = 0; i < node.children.length; i++) this.mmMarkHidden(node.children[i])
      } else if (node.children) {
        for (var j = 0; j < node.children.length; j++) this.mmResetHidden(node.children[j])
      }
    },

    mmMarkHidden: function (node) {
      node._hidden = true
      if (node.children) for (var i = 0; i < node.children.length; i++) this.mmMarkHidden(node.children[i])
    },

    mmCollect: function (node, parent, nodes, edges) {
      if (!node || node._hidden) return
      if (node._ly) nodes.push(node)
      if (parent && parent._ly && node._ly) edges.push({ from: parent, to: node, d: node._ly.d - 1 })
      if (!node.collapsed && node.children) {
        for (var i = 0; i < node.children.length; i++) this.mmCollect(node.children[i], node, nodes, edges)
      }
    },

    // ====== SVG 思维导图渲染（Agnes API 不可用时回退） ======
    // ── 生成SVG思维导图（取代markmap CDN方案）──
    renderMM: function () {
      var container = this.$refs.mmSvg
      if (!container || !this.body) return
      container.innerHTML = '<div style="padding:60px;text-align:center;color:#86868b"><i class="el-icon-loading" style="font-size:28px"></i><p style="margin-top:12px">正在渲染思维导图...</p></div>'
      var self = this

      setTimeout(function () {
        try {
          var content = self.body || ''
          var tree = self.mmParse(content)
          if (!tree) {
            container.innerHTML = '<p style="padding:60px;color:#FF3B30;text-align:center;font-size:14px">无法解析思维导图内容，请尝试重新生成</p>'
            return
          }

          // 深拷贝避免污染
          var treeCopy = JSON.parse(JSON.stringify(tree))
          self._mmResetHidden(treeCopy)
          var layoutSize = self._mmLayout(treeCopy, 0, 80)

          var pad = 80
          var vbW = Math.max(1200, layoutSize.w + pad * 2)
          var vbH = layoutSize.h + pad * 2
          var offsetY = 20
          vbH += offsetY

          var nodes = []
          var edges = []
          self._mmCollect(treeCopy, null, nodes, edges)

          for (var ni = 0; ni < nodes.length; ni++) {
            if (nodes[ni]._ly) {
              nodes[ni]._ly.y += offsetY
              nodes[ni]._ly.cy += offsetY
            }
          }

          self.mmTree = treeCopy

          var NS = 'http://www.w3.org/2000/svg'
          function el (tag, attrs) {
            var e = document.createElementNS(NS, tag)
            for (var k in attrs) { if (attrs.hasOwnProperty(k)) e.setAttribute(k, attrs[k]) }
            return e
          }

          var svg = el('svg', {
            viewBox: '0 0 ' + vbW + ' ' + vbH,
            width: '100%', height: '100%',
            preserveAspectRatio: 'xMidYMid meet',
            style: 'display:block;width:100%;height:100%'
          })

          // defs — 固定 id，修复引用断裂
          var defs = el('defs', {})
          // 柔和阴影（Apple 风格）
          var flt = el('filter', { id: 'mm-shadow', x: '-15%', y: '-15%', width: '135%', height: '140%' })
          flt.appendChild(el('feDropShadow', { dx: '0', dy: '2', stdDeviation: '6', 'flood-color': '#000', 'flood-opacity': '0.08' }))
          defs.appendChild(flt)
          var fltRoot = el('filter', { id: 'mm-shadow-root', x: '-15%', y: '-15%', width: '135%', height: '140%' })
          fltRoot.appendChild(el('feDropShadow', { dx: '0', dy: '4', stdDeviation: '10', 'flood-color': '#007AFF', 'flood-opacity': '0.15' }))
          defs.appendChild(fltRoot)
          // 箭头 markers（每种深度颜色一个）
          var mmCols = self.mmColors()
          for (var mi = 0; mi < mmCols.length; mi++) {
            var marker = el('marker', {
              id: 'mm-arrow-' + mi,
              viewBox: '0 0 10 10',
              refX: '9', refY: '5',
              markerWidth: '7', markerHeight: '7',
              orient: 'auto-start-reverse'
            })
            marker.appendChild(el('path', { d: 'M 0 0 L 10 5 L 0 10 L 3 5 z', fill: mmCols[mi].stroke, opacity: '0.7' }))
            defs.appendChild(marker)
          }
          svg.appendChild(defs)

          // 背景 — 纯白（Apple 风格）
          svg.appendChild(el('rect', { x: 0, y: 0, width: vbW, height: vbH, fill: '#fbfbfd' }))

          // 连线层 — 动态传导箭头（带流动动画）
          var eg = el('g', { class: 'mm-edges' })
          for (var ei = 0; ei < edges.length; ei++) {
            var edge = edges[ei]
            var col = self.mmGetColor(edge.d)
            var edgeIdx = Math.min(edge.d, mmCols.length - 1)
            var cp = Math.min(80, Math.abs(edge.to._ly.cx - edge.from._ly.cx) * 0.4)
            var d = 'M ' + (edge.from._ly.x + edge.from._ly.w) + ' ' + edge.from._ly.cy +
              ' C ' + (edge.from._ly.x + edge.from._ly.w + cp) + ' ' + edge.from._ly.cy +
              ', ' + (edge.to._ly.x - cp) + ' ' + edge.to._ly.cy +
              ', ' + edge.to._ly.x + ' ' + edge.to._ly.cy
            eg.appendChild(el('path', {
              d: d, fill: 'none', stroke: col.stroke,
              'stroke-width': edge.d === 0 ? '2' : '1.5',
              opacity: '0.45', 'stroke-linecap': 'round',
              'marker-end': 'url(#mm-arrow-' + edgeIdx + ')',
              class: 'mm-edge-flow',
              'stroke-dasharray': '6 4'
            }))
          }
          svg.appendChild(eg)

          // 节点层 — Apple 风格：白底 + 彩色描边 + 柔和阴影
          var ng = el('g', { class: 'mm-nodes' })
          for (var ni = 0; ni < nodes.length; ni++) {
            var node = nodes[ni]
            var ly = node._ly
            var nc = self.mmGetColor(ly.d)
            var fs = self.mmFontSize(ly.d)
            var fw = self.mmFontWeight(ly.d)
            var isRoot = ly.d === 0

            var g = el('g', { class: 'mm-node', 'data-id': node.id, cursor: 'move' })

            // 背景矩形：白色填充 + 彩色描边 + 阴影
            var bgRect = el('rect', {
              x: ly.x, y: ly.y, width: ly.w, height: ly.h,
              rx: isRoot ? '14' : '10',
              fill: '#ffffff',
              stroke: nc.stroke,
              'stroke-width': isRoot ? '2' : '1.2',
              opacity: '1',
              filter: isRoot ? 'url(#mm-shadow-root)' : 'url(#mm-shadow)'
            })
            g.appendChild(bgRect)

            // 左侧彩色竖条（Apple 风格标记）
            g.appendChild(el('rect', {
              x: ly.x, y: ly.y + 4, width: 3, height: ly.h - 8,
              rx: 1.5, fill: nc.stroke, opacity: '0.8'
            }))

            // 文字
            var textFill = isRoot ? nc.stroke : '#1d1d1f'
            var text = el('text', {
              x: ly.cx, y: ly.cy + fs * 0.35,
              fill: textFill, 'font-size': fs, 'font-weight': fw,
              'font-family': "'-apple-system','PingFang SC','Microsoft YaHei','SF Pro Display',sans-serif",
              'text-anchor': 'middle',
              'pointer-events': 'none'
            })
            text.textContent = node.title
            g.appendChild(text)

            // 折叠按钮
            if (node.children && node.children.length > 0) {
              var icx = ly.x + ly.w - 6; var icy = ly.cy
              g.appendChild(el('circle', { cx: icx, cy: icy, r: '6', fill: node.collapsed ? nc.stroke : '#f5f5f7', stroke: nc.stroke, 'stroke-width': '1', opacity: '0.9' }))
              var sign = el('text', { x: icx, y: icy + 3, fill: node.collapsed ? '#fff' : nc.stroke, 'font-size': '9', 'font-weight': '600', 'font-family': "'-apple-system',sans-serif", 'text-anchor': 'middle', 'pointer-events': 'none' })
              sign.textContent = node.collapsed ? '+' : '−'
              g.appendChild(sign)
            }
            ng.appendChild(g)
          }
          svg.appendChild(ng)

          container.innerHTML = ''
          container.appendChild(svg)

          // ── 交互：区分点击/拖拽，双击编辑 ──
          self._mmBindInteractions(svg)

          // 自动适配视图：计算最佳缩放和居中
          self._mmFitView(svg, vbW, vbH)
        } catch (e) {
          console.error('SVG渲染失败:', e)
          container.innerHTML = '<p style="padding:60px;color:#FF3B30;text-align:center">渲染失败：' + (e.message || '未知错误') + '</p>'
        }
      }, 100)
    },

    // 绑定节点交互（点击折叠、双击编辑、拖拽节点）
    _mmBindInteractions: function (svg) {
      var self = this
      var downX = 0; var downY = 0; var moved = false
      var dragNode = null; var dragStartSvg = null; var dragStartLy = null
      var cachedEdges = []; var cachedPaths = []

      function getSvgPoint (e) {
        var pt = svg.createSVGPoint()
        pt.x = e.clientX; pt.y = e.clientY
        return pt.matrixTransform(svg.getScreenCTM().inverse())
      }

      svg.addEventListener('mousedown', function (e) {
        downX = e.clientX; downY = e.clientY; moved = false
        var nodeEl = e.target.closest('.mm-node')
        if (nodeEl) {
          var id = nodeEl.getAttribute('data-id')
          var node = self._mmFindNode(id, self.mmTree)
          if (node && node._ly) {
            dragNode = node
            var svgPt = getSvgPoint(e)
            dragStartSvg = { x: svgPt.x, y: svgPt.y }
            dragStartLy = { x: node._ly.x, y: node._ly.y, cx: node._ly.cx, cy: node._ly.cy }
            // 缓存 edges 和 path 元素用于拖拽时更新连线
            cachedEdges = []
            self._mmCollect(self.mmTree, null, [], cachedEdges)
            cachedPaths = svg.querySelectorAll('.mm-edges path')
            e.stopPropagation()
          }
        }
      })

      svg.addEventListener('mousemove', function (e) {
        if (Math.abs(e.clientX - downX) > 4 || Math.abs(e.clientY - downY) > 4) moved = true
        if (!dragNode) return
        var svgPt = getSvgPoint(e)
        var dx = svgPt.x - dragStartSvg.x
        var dy = svgPt.y - dragStartSvg.y
        // 更新节点 _ly 坐标
        dragNode._ly.x = dragStartLy.x + dx
        dragNode._ly.y = dragStartLy.y + dy
        dragNode._ly.cx = dragStartLy.cx + dx
        dragNode._ly.cy = dragStartLy.cy + dy
        // 实时更新节点 DOM 元素坐标
        var nodeEl = svg.querySelector('.mm-node[data-id="' + dragNode.id + '"]')
        if (nodeEl) {
          var ly = dragNode._ly
          var fs = self.mmFontSize(ly.d)
          var rects = nodeEl.querySelectorAll('rect')
          if (rects[0]) { rects[0].setAttribute('x', ly.x); rects[0].setAttribute('y', ly.y) }
          if (rects[1]) { rects[1].setAttribute('x', ly.x); rects[1].setAttribute('y', ly.y + 4) }
          var texts = nodeEl.querySelectorAll('text')
          if (texts[0]) { texts[0].setAttribute('x', ly.cx); texts[0].setAttribute('y', ly.cy + fs * 0.35) }
          if (texts[1]) { texts[1].setAttribute('x', ly.x + ly.w - 6); texts[1].setAttribute('y', ly.cy + 3) }
          var circles = nodeEl.querySelectorAll('circle')
          if (circles[0]) { circles[0].setAttribute('cx', ly.x + ly.w - 6); circles[0].setAttribute('cy', ly.cy) }
        }
        // 实时更新相关连线
        for (var i = 0; i < cachedEdges.length; i++) {
          var edge = cachedEdges[i]
          if (edge.from === dragNode || edge.to === dragNode) {
            var cp = Math.min(80, Math.abs(edge.to._ly.cx - edge.from._ly.cx) * 0.4)
            var d = 'M ' + (edge.from._ly.x + edge.from._ly.w) + ' ' + edge.from._ly.cy +
              ' C ' + (edge.from._ly.x + edge.from._ly.w + cp) + ' ' + edge.from._ly.cy +
              ', ' + (edge.to._ly.x - cp) + ' ' + edge.to._ly.cy +
              ', ' + edge.to._ly.x + ' ' + edge.to._ly.cy
            if (cachedPaths[i]) cachedPaths[i].setAttribute('d', d)
          }
        }
      })

      svg.addEventListener('mouseup', function () {
        dragNode = null
      })

      // 单击折叠（非拖拽时）
      svg.addEventListener('click', function (e) {
        if (moved) return
        var nodeEl = e.target.closest('.mm-node')
        if (!nodeEl) return
        var id = nodeEl.getAttribute('data-id')
        if (!id) return
        var found = false
        function toggle (n) {
          if (!n || found) return
          if (n.id === id) {
            if (n.children && n.children.length > 0) { n.collapsed = !n.collapsed; found = true }
            return
          }
          if (n.children) n.children.forEach(function (c) { toggle(c) })
        }
        toggle(self.mmTree)
        if (found) self.renderMM()
      })

      // 双击编辑
      svg.addEventListener('dblclick', function (e) {
        var nodeEl = e.target.closest('.mm-node')
        if (!nodeEl) return
        var id = nodeEl.getAttribute('data-id')
        if (!id) return
        self._mmStartEdit(id)
      })
    },

    // 双击编辑节点文字（foreignObject + input）
    _mmStartEdit: function (nodeId) {
      var self = this
      if (!this.mmTree) return
      var layoutedNode = this._mmFindNode(nodeId, this.mmTree)
      if (!layoutedNode || !layoutedNode._ly) return
      var L = layoutedNode._ly
      var svg = this.$refs.mmSvg && this.$refs.mmSvg.querySelector('svg')
      if (!svg) return

      var NS = 'http://www.w3.org/2000/svg'
      var fo = document.createElementNS(NS, 'foreignObject')
      fo.setAttribute('x', L.x)
      fo.setAttribute('y', L.y)
      fo.setAttribute('width', Math.max(80, L.w))
      fo.setAttribute('height', L.h)
      var input = document.createElement('input')
      input.type = 'text'
      input.value = layoutedNode.title
      input.className = 'mm-edit-input'
      fo.appendChild(input)
      svg.appendChild(fo)
      setTimeout(function () { input.focus(); input.select() }, 0)

      var commit = function () {
        var val = input.value.trim()
        if (val) {
          var target = self._mmFindNode(nodeId, self.mmTree)
          if (target) target.title = val
          // 同步到原始 body（JSON 树）
          self._mmSyncBody()
        }
        if (fo.parentNode) fo.parentNode.removeChild(fo)
        self.renderMM()
      }
      var cancel = function () {
        if (fo.parentNode) fo.parentNode.removeChild(fo)
      }
      input.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') { e.preventDefault(); commit() }
        else if (e.key === 'Escape') { e.preventDefault(); cancel() }
        e.stopPropagation()
      })
      input.addEventListener('blur', commit)
    },

    // 递归查找节点
    _mmFindNode: function (id, node) {
      if (!node) return null
      if (node.id === id) return node
      if (node.children) {
        for (var i = 0; i < node.children.length; i++) {
          var r = this._mmFindNode(id, node.children[i])
          if (r) return r
        }
      }
      return null
    },

    // 将编辑后的树同步回 body（JSON 字符串）
    _mmSyncBody: function () {
      try {
        if (!this.mmTree) return
        var clean = JSON.parse(JSON.stringify(this.mmTree))
        // 移除内部字段
        function strip (n) {
          delete n._ly; delete n._hidden
          if (n.children) n.children.forEach(strip)
        }
        strip(clean)
        this.body = JSON.stringify(clean, null, 2)
      } catch (e) { /* ignore */ }
    },

    // 滚轮缩放
    mmWheel: function (e) {
      var delta = e.deltaY > 0 ? -0.1 : 0.1
      this.mmZoom(delta)
    },

    _mmResetHidden: function (node) {
      if (node.collapsed && node.children) {
        for (var i = 0; i < node.children.length; i++) this._mmMarkHidden(node.children[i])
      } else if (node.children) {
        for (var j = 0; j < node.children.length; j++) this._mmResetHidden(node.children[j])
      }
    },

    _mmMarkHidden: function (node) {
      node._hidden = true
      if (node.children) for (var i = 0; i < node.children.length; i++) this._mmMarkHidden(node.children[i])
    },

    _mmShiftY: function (node, offset) {
      if (!node || !node._ly) return
      node._ly.y += offset
      node._ly.cy += offset
      if (node.children && !node.collapsed) {
        for (var i = 0; i < node.children.length; i++) {
          this._mmShiftY(node.children[i], offset)
        }
      }
    },

    _mmLayout: function (node, d, x) {
      if (!node) return { w: 0, h: 0 }
      var fs = this.mmFontSize(d)
      var nodeW = Math.max(60, this.mmTextW(node.title, fs))
      var nodeH = 36
      var gapY = [36, 28, 22, 18, 16, 14, 12][Math.min(d, 6)]

      var children = (node.children || []).filter(function (c) { return !c._hidden })
      if (children.length === 0) {
        node._ly = { x: x, y: 0, w: nodeW, h: nodeH, cx: x + nodeW / 2, cy: nodeH / 2, d: d }
        return { w: nodeW, h: nodeH }
      }

      var childX = x + nodeW + (d === 0 ? 200 : 140)
      var childSubHeights = []
      var childrenTotalH = 0
      for (var i = 0; i < children.length; i++) {
        var cr = this._mmLayout(children[i], d + 1, childX)
        childSubHeights.push(cr.h)
        childrenTotalH += cr.h
        if (i < children.length - 1) childrenTotalH += gapY
      }

      var totalH = Math.max(nodeH, childrenTotalH)
      var curY = Math.floor((totalH - childrenTotalH) / 2)
      for (var j = 0; j < children.length; j++) {
        // 仅调整子树 Y 坐标偏移，不覆盖递归已算好的 _ly（修复节点尺寸被错误覆写）
        var yOffset = curY
        this._mmShiftY(children[j], yOffset)
        curY += childSubHeights[j] + gapY
      }

      var nodeY = Math.floor((totalH - nodeH) / 2)
      node._ly = { x: x, y: nodeY, w: nodeW, h: nodeH, cx: x + nodeW / 2, cy: nodeY + nodeH / 2, d: d }

      var maxChildW = 0
      for (var k = 0; k < children.length; k++) {
        if (children[k]._ly) maxChildW = Math.max(maxChildW, children[k]._ly.x + children[k]._ly.w - x)
      }
      return { w: Math.max(nodeW, maxChildW), h: totalH }
    },

    _mmCollect: function (node, parent, nodes, edges) {
      if (!node || node._hidden) return
      if (node._ly) nodes.push(node)
      if (parent && parent._ly && node._ly) edges.push({ from: parent, to: node, d: node._ly.d - 1 })
      if (!node.collapsed && node.children) {
        for (var i = 0; i < node.children.length; i++) this._mmCollect(node.children[i], node, nodes, edges)
      }
    },

    _mmApplyTransform: function () {
      var c = this.$refs.mmSvg
      if (!c) return
      var svg = c.querySelector('svg')
      if (svg) {
        svg.style.transform = 'translate(' + (this.mmPanX || 0) + 'px,' + (this.mmPanY || 0) + 'px) scale(' + (this.mmScale || 1) + ')'
        svg.style.transformOrigin = 'center center'
        svg.style.transition = 'transform 0.2s ease-out'
      }
    },

    // 计算最佳缩放和居中，确保所有内容可见
    _mmFitView: function (svg, vbW, vbH) {
      var container = this.$refs.mmSvg
      if (!container || !svg) return
      var cw = container.clientWidth
      var ch = container.clientHeight
      if (!cw || !ch) {
        // 容器尺寸还没准备好，延迟重试
        var self = this
        setTimeout(function () { self._mmFitView(svg, vbW, vbH) }, 200)
        return
      }
      // 计算适配缩放：内容 / 容器，留 5% 边距
      var scaleW = (cw * 0.92) / vbW
      var scaleH = (ch * 0.92) / vbH
      var scale = Math.min(scaleW, scaleH, 1.5) // 不放太大
      if (scale < 0.3) scale = 0.3
      // 居中偏移
      var panX = (cw - vbW * scale) / 2
      var panY = (ch - vbH * scale) / 2
      this.mmScale = scale
      this.mmPanX = panX
      this.mmPanY = panY
      this._mmApplyTransform()
    },

    mmZoom: function (d) {
      this.mmScale = Math.max(0.3, Math.min(3, +((this.mmScale || 1) + d).toFixed(2)))
      this._mmApplyTransform()
    },

    mmFit: function () {
      var c = this.$refs.mmSvg; if (!c) return
      var svg = c.querySelector('svg'); if (!svg) return
      var vb = svg.getAttribute('viewBox').split(' ')
      this._mmFitView(svg, parseFloat(vb[2]), parseFloat(vb[3]))
    },

    mmDownload: function () {
      var c = this.$refs.mmSvg; if (!c) return
      var svg = c.querySelector('svg'); if (!svg) return
      var clone = svg.cloneNode(true)
      var style = document.createElementNS('http://www.w3.org/2000/svg', 'style')
      style.textContent = '.mm-node,.mm-edges{transition:opacity .3s}'
      clone.insertBefore(style, clone.firstChild)
      var str = new XMLSerializer().serializeToString(clone)
      var blob = new Blob([str], { type: 'image/svg+xml' })
      var url = URL.createObjectURL(blob)
      var a = document.createElement('a')
      a.href = url; a.download = (this.topic || 'mindmap') + '.svg'
      document.body.appendChild(a); a.click(); document.body.removeChild(a)
      URL.revokeObjectURL(url)
      this.$message.success('SVG 已导出')
    },

    // ── 鼠标拖拽画布 ──
    mmMouseDown: function (e) {
      // 如果点击的是节点，不触发拖拽
      if (e.target.closest('.mm-node')) return
      this._mmDragging = true
      this._mmDragStartX = e.clientX - (this.mmPanX || 0)
      this._mmDragStartY = e.clientY - (this.mmPanY || 0)
      var self = this
      this._mmOnMove = function (ev) {
        if (!self._mmDragging) return
        self.mmPanX = ev.clientX - self._mmDragStartX
        self.mmPanY = ev.clientY - self._mmDragStartY
        self._mmApplyTransform()
      }
      this._mmOnUp = function () {
        self._mmDragging = false
        document.removeEventListener('mousemove', self._mmOnMove)
        document.removeEventListener('mouseup', self._mmOnUp)
      }
      document.addEventListener('mousemove', this._mmOnMove)
      document.addEventListener('mouseup', this._mmOnUp)
    },

    // ── 内容复制 ──
    copyContent: function () {
      var ta = document.createElement('textarea')
      ta.value = this.body; document.body.appendChild(ta)
      ta.select(); document.execCommand('copy'); document.body.removeChild(ta)
      this.$message.success('已复制到剪贴板')
    },

    // ====== Agnes Image 2.1 Flash 思维导图图片生成 ======

    mmGenerateImage: function () {
      if (this.mmImageLoading || !this.body) return
      this.mmImageLoading = true
      this.mmImageUrl = ''
      var self = this

      api.generateMindmapImage({
        topic: this.topic.trim(),
        mindmapJson: this.body.substring(0, 2000)
      }).then(function (result) {
        if (result && result.code === 1 && result.response) {
          var data = result.response
          if (data.imageUrl) {
            self.mmImageUrl = data.imageUrl
            self.mmImageLoading = false
          } else if (data.taskId) {
            self.mmImageTaskId = data.taskId
            self.mmPollImage()
          } else {
            self.mmImageLoading = false
          }
        } else {
          self.mmImageLoading = false
        }
      }).catch(function () {
        self.mmImageLoading = false
      })
    },

    mmPollImage: function () {
      var self = this
      var pollCount = 0
      var timer = setInterval(function () {
        pollCount++
        api.queryMindmapImageStatus(self.mmImageTaskId).then(function (res) {
          if (!res || res.code !== 1 || !res.response) return
          var data = res.response
          if (data.status === 'completed' || data.status === 'succeeded') {
            clearInterval(timer)
            if (data.imageUrl) self.mmImageUrl = data.imageUrl
            self.mmImageLoading = false
          } else if (data.status === 'failed') {
            clearInterval(timer)
            self.mmImageLoading = false
          }
          if (pollCount >= 40) { clearInterval(timer); self.mmImageLoading = false }
        }).catch(function () {})
      }, 3000)
    },

    mmDownloadImage: function () {
      if (!this.mmImageUrl) return
      var a = document.createElement('a')
      a.href = this.mmImageUrl
      a.download = (this.topic || 'mindmap') + '.png'
      a.target = '_blank'
      document.body.appendChild(a); a.click(); document.body.removeChild(a)
    },

    // ====== Agnes 2.0 视频生成（单段最大时长）======

    formatVideoTooltip: function (val) {
      return val + ' 秒'
    },

    startAgnesVideoGen: async function () {
      if (this.agnesLoading) return
      this.agnesLoading = true
      this.agnesVideoUrl = ''
      this.agnesTaskId = ''
      this.agnesStatusText = '正在提交生成任务...'

      try {
        var result = await api.generateVideoAgnes({
          topic: this.topic.trim(),
          resourceType: 'video',
          duration: this.videoSeconds
        })

        if (result && result.code === 1 && result.response) {
          this.agnesTaskId = result.response.taskId
          this.agnesStatusText = '任务已提交，等待 AI 生成视频...'
          this.startAgnesPolling()
        } else {
          var errMsg = (result && result.message) ? result.message : 'API暂不可用'
          this.agnesLoading = false
          this.agnesStatusText = ''
          this.$message.error('视频生成失败: ' + errMsg)
        }
      } catch (e) {
        this.agnesLoading = false
        this.agnesStatusText = ''
        this.$message.error('视频 API 连接失败: ' + (e.message || '未知错误'))
      }
    },

    startAgnesPolling: function () {
      var self = this
      var pollCount = 0
      var maxPolls = 120 // 最多轮询10分钟（3分钟视频需较长渲染时间）
      this.agnesPollTimer = setInterval(function () {
        pollCount++
        self.agnesStatusText = 'AI 生成中...（' + (pollCount * 5) + '秒）'

        api.queryVideoStatusAgnes(self.agnesTaskId).then(function (res) {
          if (!res || res.code !== 1 || !res.response) return

          var data = res.response
          var status = data.status || 'unknown'

          var doneStatuses = ['completed', 'succeeded', 'done', 'ready', 'finished', 'complete']
          var failStatuses = ['failed', 'error']

          if (doneStatuses.indexOf(status) !== -1) {
            clearInterval(self.agnesPollTimer)
            self.agnesPollTimer = null
            self.agnesLoading = false
            self.agnesStatusText = 'MP4 已生成'

            if (data.videoUrl) {
              self.agnesVideoUrl = data.videoUrl
              self.$message.success('视频生成完成！')
              // 视频加载后自动开始 TTS 语音讲解
              self.$nextTick(function () {
                if (self.$refs.nativeVideo) self.$refs.nativeVideo.play()
                if (self.videoTtsOn) self.videoPlayFromStart()
              })
            } else {
              self.$message.warning('视频已生成但未获取到 URL')
            }
          } else if (failStatuses.indexOf(status) !== -1) {
            clearInterval(self.agnesPollTimer)
            self.agnesPollTimer = null
            self.agnesLoading = false
            self.agnesStatusText = '生成失败'
            self.$message.error('视频生成失败: ' + (data.error || '未知错误'))
          }

          if (pollCount >= maxPolls) {
            clearInterval(self.agnesPollTimer)
            self.agnesPollTimer = null
            self.agnesLoading = false
            self.agnesStatusText = '超时'
            self.$message.warning('视频生成超时（3分钟视频需5-10分钟），请稍后重试或缩短内容')
          }
        }).catch(function (e) {
          console.error('Poll Agnes status error:', e)
        })
      }, 5000)
    },

    downloadAgnesVideo: function () {
      if (!this.agnesVideoUrl) return
      var a = document.createElement('a')
      a.href = this.agnesVideoUrl
      a.download = (this.topic || '教学视频') + '.mp4'
      a.target = '_blank'
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      this.$message.success('视频下载中...')
    },

    // 本地Canvas视频渲染（画面显示教学要点 + MP4内嵌语音讲解）
    renderLocalVideo: function () {
      var self = this
      this.videoRendering = true
      this.agnesLoading = true
      this.agnesStatusText = '正在本地渲染教学视频（含语音）...'
      this.agnesVideoUrl = ''
      // 解析场景（视觉要点 + 语音讲解分离）
      // 解析场景 — 优先使用双段格式（画面文字 + 语音讲解分离）
      this.parseVideoScenes()
      var rawScenes = this.videoScenes
      if (!rawScenes.length) rawScenes = [{ title: self.topic || '教学视频', visualItems: ['请重新生成内容'], voiceText: '请重新生成教学内容' }]
      // 每个场景的视频画面(短知识点) vs 语音稿(详细讲解) 已经分开
      var scenes = rawScenes.map(function(s) {
        var v = s.voiceText || (s.title + '。' + (s.visualItems||[]).join('。'))
        return { title: s.title, visualItems: (s.visualItems||[]).slice(0, 5), voiceText: v, voiceSpoken: false }
      })

      var canvas = document.createElement('canvas')
      canvas.width = 960; canvas.height = 540
      canvas.style.display = 'none'
      document.body.appendChild(canvas)
      var ctx = canvas.getContext('2d')

      // 视频流
      var videoStream = canvas.captureStream(30)
      var combinedStream
      var mediaRecorder
      var audioCtx = null
      var audioDest = null

      // 尝试添加音频轨道
      try {
        audioCtx = new (window.AudioContext || window.webkitAudioContext)()
        audioDest = audioCtx.createMediaStreamDestination()
        // 合并视频+音频轨道
        var audioTrack = audioDest.stream.getAudioTracks()[0]
        if (audioTrack) {
          videoStream.addTrack(audioTrack)
        }
      } catch(e) {
        console.warn('AudioContext unavailable, video will have no audio')
      }

      var mimeType = 'video/webm;codecs=vp8,opus'
      if (!MediaRecorder.isTypeSupported(mimeType)) {
        mimeType = 'video/webm;codecs=vp9,opus'
        if (!MediaRecorder.isTypeSupported(mimeType)) mimeType = 'video/webm'
      }
      mediaRecorder = new MediaRecorder(videoStream, { mimeType: mimeType, videoBitsPerSecond: 2500000 })
      var chunks = []
      mediaRecorder.ondataavailable = function (e) { if (e.data.size > 0) chunks.push(e.data) }
      mediaRecorder.onstop = function () {
        if (audioCtx) audioCtx.close().catch(function(){})
        var blob = new Blob(chunks, { type: 'video/webm' })
        self.videoBlob = blob
        self.videoFormat = 'webm'
        self.videoUrl = URL.createObjectURL(blob)
        self.agnesVideoUrl = self.videoUrl
        self.videoRendering = false
        self.agnesLoading = false
        self.agnesStatusText = 'MP4 已生成（含语音）'
        self.$message.success('教学视频渲染完成！（画面+语音）')
      }
      mediaRecorder.start()

      var totalSections = scenes.length
      var totalFrames = this.videoSeconds * 30 // 按所选时长 @30fps
      var framesPerSection = Math.max(30, Math.floor(totalFrames / Math.max(1, totalSections)))
      var frameIndex = 0
      var lastSpokenSection = -1

      // 语音播报函数：通过AudioContext捕获TTS到视频音频轨道
      var speakToVideo = function(scene) {
        if (!audioCtx || !audioDest) return
        if (!window.speechSynthesis) return
        try {
          var osc = audioCtx.createOscillator()
          var gain = audioCtx.createGain()
          gain.gain.value = 0.001 // 极低音量底噪（保持音频轨道活跃）
          osc.connect(gain)
          gain.connect(audioDest)
          osc.start()
          setTimeout(function() { try { osc.stop() } catch(e) {} }, 100)
        } catch(e) {}
        // Web Speech TTS（输出到扬声器，同时作为audiotrack的参考）
        var u = new SpeechSynthesisUtterance(scene.voiceText)
        u.lang = 'zh-CN'; u.rate = 1.05; u.pitch = 1.0; u.volume = 1.0
        var voices = window.speechSynthesis.getVoices()
        var v = voices.find(function(x){ return x.lang.startsWith('zh') && x.name.includes('Xiaoxiao') })
            || voices.find(function(x){ return x.lang.startsWith('zh-CN') })
        if (v) u.voice = v
        window.speechSynthesis.speak(u)
        scene.voiceSpoken = true
      }

      var timer = setInterval(function () {
        if (frameIndex >= totalFrames) {
          clearInterval(timer)
          window.speechSynthesis.cancel()
          mediaRecorder.stop()
          if (canvas.parentNode) canvas.parentNode.removeChild(canvas)
          return
        }
        var progress = frameIndex / totalFrames
        var contentStart = 0.05

        // 背景
        ctx.fillStyle = '#0A0D0A'; ctx.fillRect(0, 0, 960, 540)

        if (progress < contentStart) {
          // 开场
          var alpha = Math.min(1, progress / 0.03)
          ctx.fillStyle = '#f8fafc'; ctx.font = 'bold 40px sans-serif'
          ctx.textAlign = 'center'; ctx.fillText(self.topic || '教学视频', 480, 240)
          ctx.fillStyle = '#CCFF00'; ctx.font = '18px sans-serif'
          ctx.fillText('知识点精讲 · 含语音讲解', 480, 290)
        } else {
          var cp = (progress - contentStart) / (1 - contentStart)
          var si = Math.min(Math.floor(cp * totalSections), totalSections - 1)
          var scene = scenes[si]
          if (scene) {
            // 播放该节的语音（只播一次）
            if (si !== lastSpokenSection && !scene.voiceSpoken) {
              lastSpokenSection = si
              setTimeout(function(s) { speakToVideo(s) }, 200, scene)
            }
            // 顶部信息栏
            ctx.fillStyle = 'rgba(204, 255, 0, 0.1)'; ctx.fillRect(0, 0, 960, 56)
            ctx.fillStyle = '#A9F0FF'; ctx.font = '12px sans-serif'; ctx.textAlign = 'left'
            ctx.fillText('▎第 ' + (si+1) + ' / ' + totalSections + ' 节', 40, 24)
            ctx.fillStyle = '#e2e8f0'; ctx.font = 'bold 22px sans-serif'
            ctx.fillText(scene.title || '', 40, 48)
            // 视觉要点（简短，与语音不同）
            var startY = 90
            var items = scene.visualItems || []
            for (var i = 0; i < items.length; i++) {
              var txt = items[i]
              var y = startY + i * 80
              // 序号圆圈
              ctx.fillStyle = '#CCFF00'; ctx.beginPath()
              ctx.arc(50, y + 16, 14, 0, Math.PI * 2); ctx.fill()
              ctx.fillStyle = '#fff'; ctx.font = 'bold 13px sans-serif'; ctx.textAlign = 'center'
              ctx.fillText(i + 1, 50, y + 21)
              // 要点文字
              ctx.fillStyle = '#f1f5f9'; ctx.font = '20px sans-serif'; ctx.textAlign = 'left'
              ctx.fillText(txt, 80, y + 23)
              // 底部分隔
              ctx.fillStyle = 'rgba(71,85,105,0.15)'; ctx.fillRect(80, y + 48, 700, 1)
            }
            // 语音提示
            ctx.fillStyle = 'rgba(34,197,94,0.6)'; ctx.font = '11px sans-serif'; ctx.textAlign = 'right'
            ctx.fillText('🔊 语音讲解中', 910, 520)
          }
        }
        // 进度条
        ctx.fillStyle = 'rgba(204, 255, 0, 0.08)'; ctx.fillRect(0, 534, 960, 6)
        ctx.fillStyle = '#CCFF00'; ctx.fillRect(0, 534, 960 * progress, 6)

        frameIndex++
      }, 33)
    },

    parseContentToScenes: function (content) {
      content = (content || '').trim()
      var scenes = []
      var parts = content.split(/\n(?:###|##)\s+/)
      if (parts.length <= 1) parts = content.split(/\n\n+/)
      for (var i = 0; i < parts.length; i++) {
        var p = parts[i].trim(); if (!p || p.length < 5) continue
        var lines = p.split('\n')
        var title = lines[0].replace(/^#+\s*/, '').substring(0, 40)
        var items = []
        for (var j = 1; j < Math.min(lines.length, 6); j++) {
          var l = lines[j].replace(/^[-*]\s*/, '').trim()
          if (l && l.length > 3) items.push(l.substring(0, 80))
        }
        if (!items.length) items = [title]
        scenes.push({ title: title, items: items })
      }
      return scenes.length > 1 ? scenes : [{ title: '核心概念', items: [content.substring(0, 80)] }]
    },

    ps: function (c) { if (this.sc) return; var m = c.match(/质量[评分:：]\s*(\d+)/); if (m) this.sc = parseInt(m[1]) },

    viewRec: async function (r) {
      this.hid = r.id
      try { var d = await api.getDetail(r.id); if (d && d.code === 1 && d.response) { this.body = d.response.content || ''; this.sc = d.response.qualityScore || null; this.sel = r.resourceType || this.sel; this.topic = r.title || this.topic; this.$nextTick(function () { if (this.sel === 'mind_map') this.renderMM(); if (this.sel === 'exercise') this.parseQuiz() }.bind(this)) } } catch (e) {}
    },

    delRec: async function (r) {
      try {
        await this.$confirm('确定删除「' + r.title + '」？', '确认删除', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
        await api.delete(r.id)
        this.$message.success('已删除')
        if (this.hid === r.id) {
          this.hid = null; this.body = ''; this.sc = null
        }
        await this.lh()
      } catch (e) {
        if (e !== 'cancel') { this.$message.error('删除失败') }
      }
    },

    gi: function (t) { var f = this.types.find(function (x) { return x.key === t }); return f ? f.icon : 'el-icon-document' },
    gl: function (t) { var f = this.types.find(function (x) { return x.key === t }); return f ? f.label : '资源' },
    doEval: async function () { if (!this.hid || this.evaling) return; this.evaling = true; this.sc = null; try { await api.evaluate(this.hid, function (c) { this.ps(c) }.bind(this)); this.$message.success('评估完成') } catch (e) { this.$message.error('失败') } finally { this.evaling = false } },

    // ---- 练习题目解析&交互 ----
    parseQuiz: function () {
      var raw = this.body || ''
      var questions = []

      // 按空行分块
      var blocks = raw.split(/\n\s*\n/)
      var qIdx = 0

      for (var b = 0; b < blocks.length; b++) {
        var block = blocks[b].trim()
        if (!block || block.length < 8) continue

        var lines = block.split('\n')
        // 合并所有非选项行作为题干/解析
        var allPlain = []
        var opts = []
        var ansVal = ''
        var expText = ''

        for (var li = 0; li < lines.length; li++) {
          var line = lines[li].trim()
          if (!line) continue

          // 选项行 A. xxx  B. xxx
          var om = line.match(/^([A-D])[.、)\s]+(.+)/)
          if (om) {
            opts.push({ letter: om[1], text: om[2].trim() })
            continue
          }

          // 答案行
          if (/答案[：:]\s*(.+)/.test(line)) {
            var am = line.match(/答案[：:]\s*(.+)/)
            ansVal = (am ? am[1] : '').trim()
            continue
          }

          // 解析行
          if (/解析[：:]/.test(line) || /详解[：:]/.test(line)) {
            expText += (expText ? '\n' : '') + line.replace(/^[^：:]+[：:]\s*/, '').trim()
            continue
          }

          // 题型标记跳过
          if (/^(选择题|单选题|多选题|判断题|填空题|简答题|计算题)/.test(line)) continue

          allPlain.push(line)
        }

        var fullText = allPlain.join('\n').trim()
        if (!fullText) continue

        // 判断题型
        if (opts.length >= 2) {
          // 选择题
          var correctIdx = 0
          if (ansVal) {
            var letter = ansVal.replace(/[^A-Da-d]/g, '').toUpperCase()
            if (letter.length === 1) correctIdx = letter.charCodeAt(0) - 65
          }
          questions.push({
            id: qIdx++,
            type: 'choice',
            typeLabel: '选择题',
            question: fullText.replace(/^\d+[.、)\s]+/, ''),
            options: opts.map(function (o) { return o.text }),
            answer: Math.min(correctIdx, opts.length - 1),
            explanation: expText
          })
        } else if ((/^【?判断题】?/.test(fullText.trim()) || /^(正确|错误|对|错|✓|✗)$/.test(String(ansVal).trim())) && fullText.length < 200) {
          // 判断题 — 必须题型标记明显，或答案明确写了"正确/错误"
          var tfAns = /错误|✗|错/.test(ansVal) ? 1 : 0
          // 若 ansVal 不含明确对错，检查 fullText 末尾是否有"正确/错误"关键词
          if (!ansVal || !/正确|错误|对|错|✓|✗/.test(ansVal)) {
            // 文本本身含明确判断词且在短句末尾，才推断为判断句
            var lastWord = fullText.replace(/^\d+[.、)\s]+/, '').trim().slice(-4)
            tfAns = /错/.test(lastWord) ? 1 : 0
          }
          questions.push({
            id: qIdx++,
            type: 'tf',
            typeLabel: '判断题',
            question: fullText.replace(/^\d+[.、)\s]+/, ''),
            answer: tfAns,
            refAnswer: tfAns === 0 ? '正确' : '错误',
            explanation: expText
          })
        } else if (ansVal || /_{2,}|\.{3,}|【\s*】|\(\s*\)|填空|简答/.test(fullText)) {
          // 填空/简答：分离答案和解析
          var cleanQ = fullText
            .replace(/^\d+[.、)\s]+/, '')
            .replace(/_{2,}/g, '______')
            .replace(/\.{3,}/g, '______')
            .replace(/【\s*】/g, '【______】')
            .replace(/\(\s*\)/g, '(______)')

          // ansVal 中可能混合了答案和解析，尝试拆分
          var answerPart = ansVal
          var explainPart = expText

          // 如果 ansVal 包含换行或超过80字，后半部分可能是解析
          if (ansVal && ansVal.length > 80) {
            var ansLines = ansVal.split(/\n/)
            answerPart = ansLines[0].trim()
            if (ansLines.length > 1) {
              explainPart = ansLines.slice(1).join('\n').trim()
            } else {
              // 可能是 "答案。解析。" 格式
              var splitIdx = ansVal.search(/[。；;]\s*(?:解析|详解|说明|注[：:])/)
              if (splitIdx > 0) {
                answerPart = ansVal.substring(0, splitIdx + 1).trim()
                explainPart = ansVal.substring(splitIdx + 1).replace(/^[。；;]\s*/, '').trim()
              }
            }
          }

          // 如果 expText 为空但有 ansVal 且 ansVal 很长，把多余作为解析
          if (!explainPart && answerPart && answerPart.length > 60) {
            explainPart = answerPart
          }

          // 确保答案不为空
          if (!answerPart || answerPart.length === 0) {
            answerPart = '（详见解析）'
          }

          questions.push({
            id: qIdx++,
            type: 'fill',
            typeLabel: '填空/简答',
            question: cleanQ,
            refAnswer: answerPart,
            explanation: explainPart || (answerPart !== '（详见解析）' ? answerPart : '暂无详细解析')
          })
        }
      }

      // 如果没解析出任何题目，尝试按行解析
      if (questions.length === 0) {
        var rawLines = raw.split('\n')
        var currentQ = ''
        var currentType = 'fill'
        for (var ri = 0; ri < rawLines.length; ri++) {
          var l = rawLines[ri].trim()
          if (!l) continue
          var qm = l.match(/^(\d+)[.、)\s]+(.+)/)
          if (qm) {
            if (currentQ) {
              questions.push({
                id: qIdx++,
                type: currentType,
                typeLabel: '简答题',
                question: currentQ,
                refAnswer: '（请见上文详细内容）',
                explanation: ''
              })
            }
            currentQ = qm[2].trim()
            currentType = 'fill'
          } else if (currentQ) {
            currentQ += '\n' + l
          }
        }
        if (currentQ) {
          questions.push({
            id: qIdx++,
            type: 'fill',
            typeLabel: '简答题',
            question: currentQ,
            refAnswer: '（请见上文详细内容）',
            explanation: ''
          })
        }
      }

      this.quizParsed = questions
      this.quizAnswers = questions.map(function () { return null })
      this.quizResults = []
      this.quizSubmitted = false
    },

    quizSubmit: function () {
      var self = this
      this.quizResults = this.quizParsed.map(function (q, i) {
        var userAns = self.quizAnswers[i]
        if (userAns === null || userAns === undefined || userAns === '') return null
        if (q.type === 'choice') return parseInt(userAns) === q.answer
        if (q.type === 'tf') return parseInt(userAns) === q.answer
        if (q.type === 'fill' || q.type === 'short') {
          var ua = String(userAns).trim().toLowerCase()
          var ra = String(q.refAnswer).trim().toLowerCase()
          // 先精确匹配，再检查是否一方完全包含另一方且被包含部分足够长（防单字符蒙对）
          if (ua === ra) return true
          if (ua.length >= 3 && ra.includes(ua)) return true
          if (ra.length >= 3 && ua.includes(ra)) return true
          return false
        }
        return false
      })
      this.quizSubmitted = true
    },

    quizReset: function () {
      this.quizAnswers = this.quizParsed.map(function () { return null })
      this.quizResults = []
      this.quizSubmitted = false
    },

    quizExportTxt: function () {
      var txt = '练习题目 — ' + (this.topic || '未命名') + '\n'
      txt += '生成时间：' + this.formatTime(Date.now()) + '\n'
      txt += '══════════════════════════════════\n\n'
      for (var i = 0; i < this.quizParsed.length; i++) {
        var q = this.quizParsed[i]
        txt += (i + 1) + '. [' + q.typeLabel + '] ' + q.question + '\n'
        if (q.type === 'choice' && q.options) {
          for (var oi = 0; oi < q.options.length; oi++) {
            txt += '   ' + String.fromCharCode(65 + oi) + '. ' + q.options[oi] + '\n'
          }
        }
        txt += '   答案：' + (q.type === 'choice' ? String.fromCharCode(65 + q.answer) + '. ' + q.options[q.answer] : q.type === 'tf' ? (q.answer === 0 ? '正确 ✓' : '错误 ✗') : q.refAnswer) + '\n'
        if (q.explanation) txt += '   解析：' + q.explanation + '\n'
        txt += '\n'
      }
      var blob = new Blob([txt], { type: 'text/plain;charset=utf-8' })
      var url = URL.createObjectURL(blob)
      var a = document.createElement('a')
      a.href = url; a.download = (this.topic || 'exercise') + '_练习题.txt'
      document.body.appendChild(a); a.click(); document.body.removeChild(a)
      URL.revokeObjectURL(url)
      this.$message.success('TXT 已导出')
    },

    copyContent: function () {
      var textarea = document.createElement('textarea')
      textarea.value = this.body; document.body.appendChild(textarea)
      textarea.select(); document.execCommand('copy'); document.body.removeChild(textarea)
      this.$message.success('已复制到剪贴板')
    },

    formatTime: function (ts) {
      if (!ts) return ''
      var d = new Date(ts)
      var y = d.getFullYear()
      var M = ('0' + (d.getMonth() + 1)).slice(-2)
      var day = ('0' + d.getDate()).slice(-2)
      var h = ('0' + d.getHours()).slice(-2)
      var m = ('0' + d.getMinutes()).slice(-2)
      return y + '-' + M + '-' + day + ' ' + h + ':' + m
    },

    // ═══════════════ PPT 幻灯片可视化 ═══════════════
    // 根据知识点标题生成 SVG 示意图
    generateSlideIllustration: function (title, idx) {
      var colors = ['#CCFF00', '#CCFF00', '#10b981', '#f59e0b', '#ef4444', '#CCFF00', '#A9F0FF', '#CCFF00']
      var color = colors[Math.abs(this.hashCode(title || 'ppt')) % colors.length]
      var bgLight = color + '18'

      var icons = {
        concept: '<circle cx="200" cy="90" r="45" fill="none" stroke="' + color + '" stroke-width="1.5" opacity="0.3"/><circle cx="200" cy="90" r="25" fill="' + color + '" opacity="0.12"/><text x="200" y="95" text-anchor="middle" fill="' + color + '" font-size="16" font-weight="600">核心概念</text><text x="200" y="195" text-anchor="middle" fill="' + color + '" font-size="11" opacity="0.5">思维导图 · 知识框架</text>',
        method: '<rect x="110" y="50" width="180" height="110" rx="6" fill="none" stroke="' + color + '" stroke-width="1.5" opacity="0.35"/><rect x="140" y="65" width="120" height="10" rx="4" fill="' + color + '" opacity="0.3"/><rect x="125" y="83" width="150" height="8" rx="3" fill="' + color + '" opacity="0.18"/><rect x="125" y="98" width="110" height="8" rx="3" fill="' + color + '" opacity="0.14"/><rect x="150" y="130" width="80" height="20" rx="4" fill="' + color + '" opacity="0.25"/><text x="200" y="45" text-anchor="middle" fill="' + color + '" font-size="13" opacity="0.6">方法 & 应用</text>',
        keypoint: '<polygon points="200,50 320,190 80,190" fill="none" stroke="' + color + '" stroke-width="2" opacity="0.3"/><polygon points="200,70 290,180 110,180" fill="' + color + '" opacity="0.06"/><text x="200" y="145" text-anchor="middle" fill="' + color + '" font-size="22" font-weight="700">⚠ 重难点</text><text x="200" y="175" text-anchor="middle" fill="' + color + '" font-size="12" opacity="0.5">需要重点掌握和理解</text>',
        summary: '<path d="M60 180 L200 60 L340 180 Z" fill="none" stroke="' + color + '" stroke-width="1.5" opacity="0.25"/><circle cx="200" cy="105" r="30" fill="' + color + '" opacity="0.08"/><text x="200" y="110" text-anchor="middle" fill="' + color + '" font-size="18" font-weight="600">✓ 总结</text><text x="200" y="195" text-anchor="middle" fill="' + color + '" font-size="11" opacity="0.5">知识要点归纳梳理</text>',
        discussion: '<circle cx="130" cy="85" r="22" fill="none" stroke="' + color + '" stroke-width="1.5" opacity="0.35"/><circle cx="270" cy="85" r="22" fill="none" stroke="' + color + '" stroke-width="1.5" opacity="0.35"/><circle cx="200" cy="135" r="22" fill="none" stroke="' + color + '" stroke-width="1.5" opacity="0.35"/><circle cx="130" cy="85" r="14" fill="' + color + '" opacity="0.1"/><circle cx="270" cy="85" r="14" fill="' + color + '" opacity="0.1"/><circle cx="200" cy="135" r="14" fill="' + color + '" opacity="0.1"/><text x="200" y="190" text-anchor="middle" fill="' + color + '" font-size="13">互动 · 讨论 · 练习</text>'
      }
      var type = 'concept'
      if (/方法|技术|实践|应用|案例/i.test(title)) type = 'method'
      else if (/重点|难点|关键|核心/i.test(title)) type = 'keypoint'
      else if (/总结|归纳|回顾|小结/i.test(title)) type = 'summary'
      else if (/互动|讨论|练习|作业|思考/i.test(title)) type = 'discussion'

      return '<svg viewBox="0 0 400 220" xmlns="http://www.w3.org/2000/svg" style="width:100%;max-height:200px;border-radius:8px;background:' + bgLight + ';display:block">' +
        '<defs><linearGradient id="sg' + idx + '" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" style="stop-color:' + color + ';stop-opacity:0.12"/><stop offset="100%" style="stop-color:' + color + ';stop-opacity:0.01"/></linearGradient></defs>' +
        '<rect width="400" height="220" fill="url(#sg' + idx + ')" rx="8"/>' +
        icons[type] +
        '</svg>'
    },
    hashCode: function (str) {
      var h = 0
      for (var i = 0; i < str.length; i++) { h = str.charCodeAt(i) + ((h << 5) - h) }
      return h
    },
    exportDoc: function () {
      if (!this.body) return
      var html = '<html><head><meta charset="utf-8"><title>' + this.topic + '</title></head><body>'
      html += '<h1>' + this.topic + '</h1>'
      html += '<div>' + this.md + '</div></body></html>'
      var blob = new Blob([html], { type: 'application/msword;charset=utf-8' })
      var url = URL.createObjectURL(blob); var a = document.createElement('a')
      a.href = url; a.download = (this.topic || '课程文档') + '.doc'
      document.body.appendChild(a); a.click(); document.body.removeChild(a); URL.revokeObjectURL(url)
      this.$message.success('DOC 文档已导出！')
    },

    // ═══════════════ 视频 TTS 语音讲解 ═══════════════
    parseVideoScenes: function () {
      var raw = this.body || ''
      // 先尝试按 【画面文字】/【语音讲解】 双段格式解析
      var scenes = []
      var sections = raw.split(/\n##\s+/)
      if (sections.length <= 1) sections = raw.split(/\n(?:###)\s+/)
      if (sections.length <= 1) sections = [raw]

      for (var i = 0; i < sections.length; i++) {
        var sec = sections[i].trim()
        if (!sec || sec.length < 10) continue
        var title = sec.split('\n')[0].replace(/^#+\s*/, '').trim().substring(0, 40)

        // 分离画面文字和语音讲解
        var visualMatch = sec.match(/【画面文字】\s*\n([\s\S]*?)(?=【语音讲解】|$)/)
        var voiceMatch = sec.match(/【语音讲解】\s*\n([\s\S]*?)(?=$)/)

        var visualItems = []
        var voiceText = ''

        if (visualMatch && visualMatch[1]) {
          // 从【画面文字】提取知识点条目
          var vlines = visualMatch[1].trim().split('\n')
          for (var j = 0; j < vlines.length; j++) {
            var l = vlines[j].replace(/^[-*•·]\s*/, '').replace(/\*\*/g, '').trim()
            if (l && l.length > 2 && !l.startsWith('【')) {
              visualItems.push(l.length > 50 ? l.substring(0, 48) + '…' : l)
            }
          }
        }
        if (voiceMatch && voiceMatch[1]) {
          voiceText = voiceMatch[1].trim().replace(/\n/g, ' ').substring(0, 500)
        }

        // 如果无双段格式，回退到旧解析
        if (!visualItems.length) {
          var lines = sec.split('\n')
          for (var j = 1; j < Math.min(lines.length, 10); j++) {
            var l = lines[j].replace(/^[-*•·]\s*/, '').replace(/\*\*/g, '').replace(/^【.*】\s*/, '').trim()
            if (l && l.length > 3 && !l.startsWith('#')) visualItems.push(l.substring(0, 80))
          }
        }
        if (!voiceText && visualItems.length) {
          voiceText = title + '。' + visualItems.join('。')
        }

        if (!visualItems.length) visualItems = [title]
        scenes.push({ title: title, visualItems: visualItems, voiceText: voiceText })
      }

      this.videoScenes = scenes.length > 1 ? scenes : [{ title: this.topic || '教学视频', visualItems: ['请重新生成内容'], voiceText: '请重新生成教学内容' }]
      this.videoSceneIdx = 0; this.videoProgress = 0
    },
    videoPlayFromStart: function () { this.videoSceneIdx = 0; this.videoProgress = 0; this.stopVideoTts(); if (this.videoTtsOn) this.startVideoNarration() },
    videoSeek: function (e) {
      var rect = e.currentTarget.getBoundingClientRect()
      var pct = (e.clientX - rect.left) / rect.width
      var idx = Math.floor(pct * this.videoScenes.length)
      this.videoSceneIdx = Math.min(idx, this.videoScenes.length - 1)
      this.videoProgress = Math.round((this.videoSceneIdx / this.videoScenes.length) * 100)
    },
    startVideoNarration: function () {
      if (!this.videoScenes.length || !this.videoTtsOn) return
      this.stopVideoTts()
      var self = this
      this.speakScene(0)
      this.videoTimer = setInterval(function () {
        var next = self.videoSceneIdx + 1
        if (next >= self.videoScenes.length) { self.stopVideoTts(); return }
        self.videoSceneIdx = next
        self.videoProgress = Math.round((next / self.videoScenes.length) * 100)
        self.speakScene(next)
      }, 8000) // 每节8秒
    },
    speakScene: function (idx) {
      if (!this.videoTtsOn || idx >= this.videoScenes.length) return
      var scene = this.videoScenes[idx]
      var text = scene.title + '。' + (scene.items || []).join('。')
      this.ttsEnqueue(text)
    },
    ttsEnqueue: function (text) {
      if (!window.speechSynthesis || !this.videoTtsOn) return
      var clean = text.replace(/<[^>]+>/g, '').replace(/[#*`\-|>]/g, '').trim()
      if (clean.length < 3) return
      this.videoTtsQueue.push(clean)
      if (!this.videoTtsSpeaking) this.ttsProcess()
    },
    ttsProcess: function () {
      if (!this.videoTtsOn || !this.videoTtsQueue.length) { this.videoTtsSpeaking = false; return }
      this.videoTtsSpeaking = true
      var text = this.videoTtsQueue.shift()
      if (this.videoTtsQueue.length > 15) this.videoTtsQueue = this.videoTtsQueue.slice(-8)
      try {
        var u = new SpeechSynthesisUtterance(text)
        u.lang = 'zh-CN'; u.rate = 1.15; u.pitch = 1.05; u.volume = 0.9
        var voices = window.speechSynthesis.getVoices()
        var voice = voices.find(function (v) { return v.lang.startsWith('zh') && v.name.includes('Tingting') }) ||
          voices.find(function (v) { return v.lang.startsWith('zh') && v.name.includes('Meijia') }) ||
          voices.find(function (v) { return v.lang.startsWith('zh-CN') })
        if (voice) u.voice = voice
        var self = this
        u.onend = function () { self.ttsProcess() }
        u.onerror = function () { self.ttsProcess() }
        window.speechSynthesis.speak(u)
      } catch (e) { this.ttsProcess() }
    },
    stopVideoTts: function () {
      if (this.videoTimer) { clearInterval(this.videoTimer); this.videoTimer = null }
      if (window.speechSynthesis) window.speechSynthesis.cancel()
      this.videoTtsQueue = []; this.videoTtsSpeaking = false
    },

    // ═══════════════ 代码运行沙箱 ═══════════════
    parseCodeBlocks: function () {
      var raw = this.body || ''
      // 终极元描述清洗（代码资源专用）
      raw = this.stripAllMeta(raw)
      var blocks = []
      var regex = /```(\w*)\n([\s\S]*?)```/g
      var match
      while ((match = regex.exec(raw)) !== null) {
        var lang = (match[1] || 'javascript').toLowerCase().trim()
        if (lang === 'js') lang = 'javascript'
        if (lang === 'py') lang = 'python'
        var code = match[2].trim()
        var runnable = (lang === 'html' || lang === 'javascript' || lang === 'css')
        blocks.push({
          lang: lang,
          code: code,
          originalCode: code,
          runnable: runnable,
          running: false,
          output: null,
          exitOk: true
        })
      }
      // 如果没有代码块但内容看起来像代码
      if (blocks.length === 0 && raw.trim()) {
        var looksLikeCode = raw.includes('function ') || raw.includes('const ') || raw.includes('var ') ||
          raw.includes('<!DOCTYPE') || raw.includes('<html') || raw.includes('console.log') ||
          raw.includes('class ') || raw.includes('import ') || raw.includes('def ')
        if (looksLikeCode) {
          var detectedLang = raw.includes('<!DOCTYPE') || raw.includes('<html') ? 'html' : 'javascript'
          blocks.push({
            lang: detectedLang,
            code: raw.trim(),
            originalCode: raw.trim(),
            runnable: (detectedLang === 'html' || detectedLang === 'javascript'),
            running: false,
            output: null,
            exitOk: true
          })
        }
      }
      this.codeBlocks = blocks
      this.activeCodeIdx = 0
    },
    runCode: function (bi) {
      var block = this.codeBlocks[bi]
      if (!block || block.running) return
      block.running = true
      block.output = null
      block.exitOk = true
      this.$forceUpdate()

      var code = block.code
      var lang = block.lang

      // HTML模式：在iframe中渲染
      if (lang === 'html' || (code.includes('<html') && code.includes('</html>'))) {
        setTimeout(function () {
          block.output = 'HTML 页面已在下方预览区渲染，请查看 👇'
          block.exitOk = true
          this.$forceUpdate()
        }.bind(this), 300)
        return
      }

      // JavaScript / CSS 模式
      if (lang === 'javascript' || lang === 'css' || lang === 'js') {
        try {
          var outputLines = []
          // 劫持 console
          var _log = console.log
          var _warn = console.warn
          var _error = console.error
          var _info = console.info

          console.log = function () {
            outputLines.push('[log] ' + Array.prototype.slice.call(arguments).map(function (a) {
              return typeof a === 'object' ? JSON.stringify(a, null, 2) : String(a)
            }).join(' '))
          }
          console.warn = function () {
            outputLines.push('[warn] ' + Array.prototype.slice.call(arguments).join(' '))
          }
          console.error = function () {
            outputLines.push('[error] ' + Array.prototype.slice.call(arguments).join(' '))
          }
          console.info = function () {
            outputLines.push('[info] ' + Array.prototype.slice.call(arguments).join(' '))
          }

          if (lang === 'css') {
            // CSS直接显示在输出中
            outputLines.push('[CSS代码] 共 ' + code.split('\n').length + ' 行，可在HTML中引用')
            outputLines.push('提示：将CSS与HTML代码合并到同一个代码块中可实现完整页面效果')
          } else {
            // 包装代码以捕获返回值
            var wrappedCode = '(function(){\n' + code + '\n})()'
            var result = new Function(wrappedCode)()
            if (result !== undefined) {
              var resultDisplay = typeof result === 'object' ? JSON.stringify(result, null, 2) : String(result)
              outputLines.push('→ 返回值: ' + resultDisplay)
            }
          }

          // 恢复 console
          console.log = _log
          console.warn = _warn
          console.error = _error
          console.info = _info

          block.output = outputLines.join('\n') || '✓ 代码执行完成（无控制台输出）'
          block.exitOk = true
        } catch (e) {
          console.log = _log; console.warn = _warn; console.error = _error; console.info = _info
          block.output = '✗ 执行错误:\n' + (e.stack || e.message || String(e))
          block.exitOk = false
        }
        this.$forceUpdate()
        return
      }

      // 其他语言（Python, Java等）提示
      block.output = 'ℹ ' + lang + ' 代码暂不支持浏览器端直接运行。\n' +
        '建议：\n' +
        '  1. 复制代码到本地 ' + lang + ' 环境运行\n' +
        '  2. 或点击「复制」按钮后粘贴到在线 ' + lang + ' 编译器\n' +
        '  （HTML/JavaScript 代码可直接在下方预览区运行）'
      block.exitOk = true
      this.$forceUpdate()
    },
    resetCode: function (bi) {
      var block = this.codeBlocks[bi]
      if (!block) return
      block.running = false
      block.output = null
      block.exitOk = true
      // 恢复原始代码
      if (block.originalCode) {
        block.code = block.originalCode
      }
      this.$forceUpdate()
    },
    buildSandboxHtml: function (block) {
      if (!block) return ''
      var code = block.code || ''
      // 如果代码本身就是完整HTML，直接返回
      if (code.includes('<!DOCTYPE') || code.includes('<html')) {
        return code
      }
      // 包装JS代码为完整HTML页面
      var safeCode = code.replace(/<\/script>/gi, '<\\/script>')
      return '<!DOCTYPE html>\n<html lang="zh-CN">\n<head>\n<meta charset="UTF-8">\n<meta name="viewport" content="width=device-width,initial-scale=1.0">\n<title>代码运行预览</title>\n<style>\n' +
        '*{margin:0;padding:0;box-sizing:border-box}\n' +
        'body{font-family:"Segoe UI",system-ui,-apple-system,sans-serif;padding:24px;background:var(--ink-2);color:#e2e8f0;line-height:1.7}\n' +
        '#output{border:1px solid #334155;padding:16px;border-radius:10px;min-height:48px;background:#141A14;margin-top:16px;white-space:pre-wrap;font-family:"JetBrains Mono","Fira Code",monospace;font-size:13px}\n' +
        '#output:empty::before{content:"等待输出...";color:#64748b}\n' +
        '.header{color:var(--ice);font-size:13px;margin-bottom:12px;font-weight:600}\n' +
        'button{padding:8px 20px;background:var(--ink-3);color:#fff;border:none;border-radius:6px;cursor:pointer;font-size:14px;margin:4px}\n' +
        'button:hover{background:var(--acid-dim)}\n' +
        'input{padding:8px 12px;border:1px solid #334155;border-radius:6px;background:#141A14;color:#e2e8f0;font-size:14px;margin:4px}\n' +
        '</style>\n</head>\n<body>\n' +
        '<div class="header">▶ 代码运行预览（沙箱环境）</div>\n' +
        '<div id="app"></div>\n' +
        '<div id="output"></div>\n' +
        '<script>\n' +
        'var _log=[];\n' +
        'console.log=function(){_log.push([].slice.call(arguments).join(" "));var el=document.getElementById("output");el.textContent=_log.join("\\n")};\n' +
        'console.error=function(){_log.push("[Error] "+[].slice.call(arguments).join(" "));document.getElementById("output").textContent=_log.join("\\n")};\n' +
        'window.onerror=function(m){_log.push("[Error] "+m);document.getElementById("output").textContent=_log.join("\\n");return true};\n' +
        'try{\n' +
        safeCode + '\n' +
        '}catch(e){document.getElementById("output").textContent="✗ 错误: "+e.message;console.error(e.message)}\n' +
        '<\/script>\n</body>\n</html>'
    },
    copyCode: function (code) {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(code).then(function () {
          this.$message.success('代码已复制到剪贴板')
        }.bind(this)).catch(function () {
          this.fallbackCopy(code)
        }.bind(this))
      } else {
        this.fallbackCopy(code)
      }
    },
    fallbackCopy: function (code) {
      var ta = document.createElement('textarea')
      ta.value = code
      ta.style.position = 'fixed'; ta.style.left = '-9999px'
      document.body.appendChild(ta)
      ta.focus(); ta.select()
      try { document.execCommand('copy'); this.$message.success('代码已复制') } catch (e) { this.$message.error('复制失败') }
      document.body.removeChild(ta)
    },

    // ═══════════════ 生命周期 ═══════════════
    beforeDestroyFull: function () {
      this.stopVideoTts()
      if (this.mmObj) this.mmObj = null
      if (this.agnesPollTimer) { clearInterval(this.agnesPollTimer); this.agnesPollTimer = null }
    }
  },
  watch: {
    '$route.query.type': function (val) {
      if (!val) return
      if (val === 'ppt') { this.$router.push('/resource-agent/ppt-agent'); return }
      var f = this.types.find(function (t) { return t.key === val })
      if (f && f.key !== this.sel) {
        this.sel = val
        this.resetState()
      }
    }
  }
}
</script>

<style scoped>
.resource-gen-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
  position: relative;
}

/* ── 两栏布局：左侧 Agent 导航 + 右侧工作区 ── */
.agent-layout {
  display: flex;
  height: calc(100vh - 92px);
  overflow: hidden;
  background: var(--ink-3);
}
.agent-workspace {
  flex: 1;
  min-width: 0;
  padding: 24px 28px 60px;
  overflow-y: auto;
}
.agent-workspace::-webkit-scrollbar { width: 6px; }
.agent-workspace::-webkit-scrollbar-thumb { background: var(--ink-3); border-radius: 3px; }
.ws-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}
.ws-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.ws-agent-badge {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 6px 14px;
  border-radius: 100px;
  background: linear-gradient(135deg, var(--acid), var(--ice));
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.5px;
  box-shadow: 0 6px 16px rgba(var(--acid-rgb), 0.3);
  flex-shrink: 0;
}
.ws-agent-desc {
  font-size: 13px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ws-closed-loop {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 100px;
  background: var(--ink-3);
  color: var(--acid);
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}
.ws-closed-loop i { font-size: 14px; }

.gen-header {
  margin-bottom: 24px;
  text-align: center;
}
.gen-title {
  font-size: 26px;
  color: var(--grey);
  margin: 0 0 6px 0;
  font-weight: 600;
  letter-spacing: 1px;
}
.gen-title i {
  color: #c8a45c;
  margin-right: 8px;
}
.gen-subtitle {
  color: #8c8470;
  font-size: 14px;
  margin: 0;
}

/* ── Agent 切换导航 ── */
.agent-nav {
  background: #fff;
  border: 1px solid #e8ecf1;
  border-radius: 14px;
  padding: 16px 18px;
  margin-bottom: 20px;
}
.agent-nav-top {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}
.agent-nav-back {
  color: var(--acid);
  font-weight: 600;
  padding: 0;
}
.agent-nav-back i { margin-right: 2px; }
.agent-nav-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--grey);
}
.agent-nav-title i { color: #c8a45c; margin-right: 6px; }
.agent-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f1f5f9;
}
.agent-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 10px;
  background: var(--ink-3);
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}
.agent-tab i { font-size: 15px; }
.agent-tab:hover { color: var(--grey); background: var(--ink-3); }
.agent-tab.active {
  color: #fff;
  background: linear-gradient(135deg, #152238, #1e3250);
  box-shadow: 0 4px 14px rgba(21, 34, 56, 0.25);
}
.agent-now {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 12px;
}
.agent-now-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 100px;
  background: var(--ink-3);
  color: #d97706;
  font-size: 12px;
  font-weight: 700;
}
.agent-now-desc {
  font-size: 12.5px;
  color: #94a3b8;
}

/* 类型选择器 */
.type-selector {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}
.type-card {
  position: relative;
  padding: 14px 10px;
  background: var(--ink-3);
  border: 2px solid #e8e4d9;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.25s ease;
}
.type-card:hover {
  border-color: #c8a45c;
  box-shadow: 0 4px 16px rgba(200, 164, 92, 0.12);
  transform: translateY(-2px);
}
.type-card.active {
  border-color: #c8a45c;
  background: linear-gradient(135deg, #fefcf6 0%, #f8f2e3 100%);
  box-shadow: 0 4px 20px rgba(200, 164, 92, 0.2);
}
.type-card-icon {
  font-size: 28px;
  color: #c8a45c;
  margin-bottom: 6px;
}
.type-card.active .type-card-icon {
  color: var(--grey);
}
.type-card-info span {
  display: block;
}
.type-card-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--grey);
}
.type-card-agent {
  font-size: 11px;
  color: #8c8470;
  margin-top: 2px;
}
.type-card-check {
  position: absolute;
  top: 6px;
  right: 8px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #152238;
  color: #c8a45c;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 输入行 */
.gen-input-row {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}
.gen-input-wrap {
  flex: 1;
}
.gen-btn {
  background: linear-gradient(135deg, #152238, #1e3250) !important;
  border-color: #152238 !important;
  color: #c8a45c !important;
  font-weight: 600;
  letter-spacing: 0.5px;
}
.gen-btn:hover {
  background: linear-gradient(135deg, #1e3250, #264a6b) !important;
}

/* 质量评分 */
.quality-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--ink-3);
  border: 1px solid #e8e4d9;
  border-radius: 10px;
  margin-bottom: 20px;
}
.quality-label {
  font-size: 13px;
  color: #8c8470;
  margin-right: 10px;
  font-weight: 500;
}
.quality-score {
  display: flex;
  align-items: center;
}

/* 内容面板 */
.content-panel {
  background: #fff;
  border: 1px solid #e8e4d9;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(21, 34, 56, 0.04);
}
.panel-header {
  padding: 14px 20px;
  background: linear-gradient(135deg, #faf9f6, #f5f2ea);
  border-bottom: 1px solid #e8e4d9;
  font-size: 15px;
  font-weight: 600;
  color: var(--grey);
}
.panel-header i {
  margin-right: 6px;
  color: #c8a45c;
}
.mm-tools {
  float: right;
}
.mm-tools .el-button {
  margin-left: 4px;
}
/* Apple 风格提示栏 */
.mm-hint-bar {
  display: flex;
  gap: 20px;
  padding: 8px 16px;
  background: var(--ink-3);
  border-radius: 8px 8px 0 0;
  font-size: 12px;
  color: #86868b;
  border-bottom: 1px solid #e8e8ed;
}
.mm-hint-bar b {
  color: #1d1d1f;
  font-weight: 600;
}
/* Apple 风格容器 */
.mm-container {
  height: calc(100vh - 280px);
  min-height: 500px;
  overflow: hidden;
  position: relative;
  background: var(--ink-3);
  border-radius: 0 0 12px 12px;
  cursor: grab;
  user-select: none;
}
.mm-container:active {
  cursor: grabbing;
}
.mm-container svg {
  display: block;
  width: 100%;
  height: 100%;
}
/* 节点 hover 效果 */
.mm-node {
  transition: opacity 0.2s ease;
  cursor: move;
}
.mm-node:hover rect {
  filter: brightness(0.96);
}
/* 连线流动传导动画 */
.mm-edge-flow {
  animation: mm-flow 1.2s linear infinite;
}
@keyframes mm-flow {
  to { stroke-dashoffset: -10; }
}
/* 编辑输入框 */
.mm-edit-input {
  width: 100%;
  height: 100%;
  border: 2px solid #007AFF;
  border-radius: 8px;
  padding: 4px 8px;
  font-size: 13px;
  font-family: -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  color: #1d1d1f;
  outline: none;
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.12);
  text-align: center;
  background: #fff;
}
.anim-hint {
  font-size: 12px;
  color: var(--ice);
  font-weight: 500;
  margin-left: 12px;
}
.video-actions {
  float: right;
  display: flex;
  gap: 4px;
}
.video-actions .el-button--text {
  color: #94a3b8;
  font-size: 12px;
  padding: 4px 8px;
}
.video-actions .el-button--text:hover {
  color: var(--ice);
}
.anim-iframe-container {
  min-height: 500px;
  background: var(--ink-2);
}
.anim-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 500px;
  color: #cbd5e1;
  font-size: 16px;
}
.anim-loading i {
  margin-right: 8px;
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Markdown body */
.markdown-body {
  padding: 24px;
  line-height: 1.8;
  color: var(--grey);
  font-size: 15px;
}
.markdown-body h1, .markdown-body h2, .markdown-body h3 {
  color: var(--grey);
  margin-top: 20px;
  margin-bottom: 10px;
}
.markdown-body h1 { font-size: 24px; border-bottom: 2px solid #c8a45c; padding-bottom: 8px; }
.markdown-body h2 { font-size: 20px; }
.markdown-body h3 { font-size: 17px; }
.markdown-body p { margin: 8px 0; }
.markdown-body ul, .markdown-body ol { padding-left: 24px; }
.markdown-body li { margin: 4px 0; }
.markdown-body code { background: var(--ink-3); padding: 2px 6px; border-radius: 3px; font-size: 13px; color: #b7473b; }
.markdown-body pre { background: #152238; color: #e2e8f0; padding: 16px; border-radius: 8px; overflow-x: auto; }
.markdown-body pre code { background: none; color: inherit; }
.markdown-body strong { color: var(--grey); }
.markdown-body blockquote { border-left: 3px solid #c8a45c; padding: 8px 16px; margin: 12px 0; background: var(--ink-3); color: #5c5648; }

/* 空状态 */
.gen-empty {
  text-align: center;
  padding: 80px 20px;
  color: #8c8470;
}
.gen-empty .empty-icon {
  font-size: 56px;
  color: #d5c9a6;
  margin-bottom: 16px;
}
.gen-empty h3 {
  font-size: 18px;
  color: var(--grey);
  margin: 0 0 8px 0;
}
.gen-empty p {
  font-size: 14px;
  margin: 0 0 16px 0;
}
.empty-tips {
  margin-top: 12px;
}
.empty-tips span {
  color: #8c8470;
  font-size: 13px;
}

/* 历史 */
.history-section {
  margin-top: 32px;
  border-top: 1px solid #e8e4d9;
  padding-top: 20px;
}
.history-header {
  font-size: 15px;
  font-weight: 600;
  color: var(--grey);
  margin-bottom: 12px;
}
.history-header i {
  color: #c8a45c;
  margin-right: 6px;
}
.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: var(--ink-3);
  border: 1px solid #e8e4d9;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.history-item:hover, .history-item.active {
  border-color: #c8a45c;
  background: var(--ink-3);
}
.history-item-header i {
  color: #c8a45c;
  margin-right: 4px;
}
.history-item-type {
  font-size: 12px;
  color: #8c8470;
}
.history-item-title {
  flex: 1;
  margin: 0 12px;
  font-size: 14px;
  color: var(--grey);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.history-item-time {
  font-size: 12px;
  color: #b0a68e;
}
.history-item-del {
  color: #b0a68e !important;
  padding: 4px !important;
  font-size: 16px;
}
.history-item-del:hover {
  color: #fb7185 !important;
}

/* ---- 练习题目交互 ---- */
.exercise-actions {
  float: right;
  display: flex;
  gap: 8px;
}
.quiz-body {
  padding: 16px 24px;
}
.quiz-item {
  padding: 16px;
  margin-bottom: 12px;
  border: 1px solid #e8e4d9;
  border-radius: 10px;
  background: var(--ink-3);
  transition: all 0.3s;
}
.quiz-item.correct {
  border-color: #34d399;
  background: var(--ink-3);
}
.quiz-item.wrong {
  border-color: #fb7185;
  background: var(--ink-3);
}
.quiz-q-header {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 10px;
}
.quiz-q-num {
  font-weight: 700;
  font-size: 16px;
  color: var(--grey);
  min-width: 24px;
}
.quiz-q-type {
  font-size: 10px;
  color: #fff;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 600;
  white-space: nowrap;
  margin-top: 2px;
}
.quiz-q-text {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
  line-height: 1.6;
}
.quiz-options {
  padding-left: 32px;
}
.quiz-option {
  padding: 6px 0;
}
.quiz-option.opt-correct {
  background: var(--ink-3);
  border-radius: 6px;
  padding: 6px 8px;
}
.quiz-option.opt-wrong {
  background: var(--ink-3);
  border-radius: 6px;
  padding: 6px 8px;
}
.quiz-radio {
  font-size: 14px;
  color: var(--grey);
}
.quiz-input-area {
  padding-left: 32px;
  margin-top: 8px;
}
.quiz-feedback {
  margin-top: 12px;
  padding: 12px 16px;
  background: var(--ink-3);
  border-radius: 8px;
  border-left: 3px solid #c8a45c;
}
.quiz-fb-row {
  font-size: 13px;
  color: var(--grey);
  margin-bottom: 4px;
  line-height: 1.6;
}
.quiz-fb-row b {
  color: var(--grey);
}
.quiz-fb-result {
  margin-top: 8px;
}
.fb-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}
.fb-ok {
  background: var(--ink-3);
  color: #065f46;
}
.fb-no {
  background: var(--ink-3);
  color: #991b1b;
}
.fb-skip {
  background: var(--ink-3);
  color: #64748b;
}
.quiz-score-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  margin-top: 16px;
  background: linear-gradient(135deg, #fefcf6, #f8f2e3);
  border: 1px solid #c8a45c;
  border-radius: 10px;
}
.quiz-score-left {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.quiz-score-big {
  font-size: 24px;
  font-weight: 700;
  color: var(--grey);
}
.quiz-score-pct {
  font-size: 14px;
  color: #8c8470;
}
.quiz-score-right {
  width: 200px;
}
.quiz-empty {
  text-align: center;
  padding: 60px 20px;
  color: #8c8470;
}
.quiz-empty i {
  font-size: 40px;
  margin-bottom: 12px;
  display: block;
}

/* ---- 思维导图 badge (Apple 风格) ---- */
.mm-badge {
  font-size: 10px;
  background: #007AFF;
  color: #fff;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
  margin-left: 8px;
  font-family: 'JetBrains Mono', monospace;
  letter-spacing: 0.5px;
}

/* ── PPT 幻灯片浏览器 ── */
.ppt-viewer {
  background: var(--ink-3); border-radius: 0 0 12px 12px; overflow: hidden;
}
.ppt-viewer.fullscreen { position: fixed; inset: 0; z-index: 9999; background: var(--ink-2); border-radius: 0; }
.ppt-slide-container { min-height: 420px; display: flex; align-items: center; justify-content: center; padding: 24px; }
.fullscreen .ppt-slide-container { min-height: 70vh; }
.ppt-slide {
  width: 100%; max-width: 860px; min-height: 360px;
  background: #fff; border-radius: 14px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.08); padding: 40px 48px;
  border: 1px solid #e2e8f0;
}
.fullscreen .ppt-slide { background: #141A14; border-color: #334155; color: #e2e8f0; }
.ppt-slide-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; padding-bottom: 12px; border-bottom: 2px solid var(--acid); }
.ppt-slide-topic { font-size: 14px; color: var(--acid); font-weight: 600; }
.ppt-slide-num { font-size: 12px; color: #94a3b8; font-weight: 500; }
.ppt-slide-content { font-size: 15px; line-height: 1.9; }
.ppt-slide-content h2 { font-size: 22px; color: #1e293b; margin-bottom: 12px; }
.ppt-slide-content h3 { font-size: 18px; color: var(--grey); }
.ppt-slide-content ul { padding-left: 20px; }
.ppt-slide-content li { margin: 6px 0; }
.fullscreen .ppt-slide-content h2, .fullscreen .ppt-slide-content h3 { color: #e2e8f0; }
.fullscreen .ppt-slide-content { color: #cbd5e1; }

.ppt-controls {
  display: flex; align-items: center; justify-content: center;
  gap: 12px; padding: 14px 20px; background: #fff; border-top: 1px solid #e2e8f0;
}
.fullscreen .ppt-controls { background: var(--ink-2); border-color: #141A14; }
.ppt-progress { display: flex; gap: 6px; }
.ppt-dot { width: 8px; height: 8px; border-radius: 50%; background: #cbd5e1; cursor: pointer; transition: all 0.3s; }
.ppt-dot.active { background: var(--ink-3); width: 24px; border-radius: 4px; }
.ppt-extra { margin-left: 12px; }

/* 幻灯片过渡 */
.slide-left-enter-active, .slide-left-leave-active,
.slide-right-enter-active, .slide-right-leave-active { transition: all 0.35s ease; }
.slide-left-enter { transform: translateX(40px); opacity: 0; }
.slide-left-leave-to { transform: translateX(-40px); opacity: 0; }
.slide-right-enter { transform: translateX(-40px); opacity: 0; }
.slide-right-leave-to { transform: translateX(40px); opacity: 0; }

/* ── 视频场景播放器 ── */
.video-player-wrapper { background: var(--ink-2); padding: 20px; border-radius: 0 0 12px 12px; }
.video-native-player { }
.video-native-info { display: flex; align-items: center; justify-content: space-between; margin-top: 10px; font-size: 13px; color: #94a3b8; }
.video-format-badge { display: inline-block; padding: 2px 10px; background: #141A14; border-radius: 4px; font-size: 11px; color: var(--ice); }
.video-duration-bar {
  display: flex; align-items: center; padding: 16px 24px;
  background: linear-gradient(135deg, #f5f5f7, #e8e8ed); border-radius: 12px; margin: 12px 0;
  border: 1px solid #d2d2d7;
}
.vd-label { color: #1d1d1f; font-size: 14px; font-weight: 600; white-space: nowrap; margin-right: 16px; }
.vd-slider-wrap { flex: 1; max-width: 320px; margin: 0 12px; }
.vd-marks { display: flex; justify-content: space-between; font-size: 11px; color: #86868b; margin-top: 4px; padding: 0 2px; }
.vd-value { color: #007AFF; font-size: 18px; font-weight: 700; min-width: 60px; text-align: center; }
.vd-tip { color: #86868b; font-size: 11px; margin-left: 12px; white-space: nowrap; }
/* Apple 风格滑块 */
.vd-apple-slider .el-slider__runway {
  height: 6px; background: #d2d2d7; border-radius: 3px;
}
.vd-apple-slider .el-slider__bar {
  height: 6px; background: #007AFF; border-radius: 3px;
}
.vd-apple-slider .el-slider__button {
  width: 22px; height: 22px; border: 2px solid #fff;
  background: #007AFF; box-shadow: 0 2px 8px rgba(0,122,255,0.3);
  transition: transform 0.15s ease;
}
.vd-apple-slider .el-slider__button:hover {
  transform: scale(1.15);
}
.vd-apple-slider .el-slider__button-wrapper { top: -8px; }
.video-scene-display { min-height: 320px; display: flex; align-items: center; justify-content: center; }
.video-scene-card {
  background: #141A14; border: 1px solid #334155; border-radius: 14px;
  padding: 36px 44px; width: 100%; max-width: 800px;
}
.scene-header { text-align: center; margin-bottom: 28px; }
.scene-badge {
  display: inline-block; padding: 3px 14px; background: rgba(var(--acid-rgb), 0.2);
  color: #a5b4fc; border-radius: 12px; font-size: 12px; font-weight: 500; margin-bottom: 10px;
}
.scene-title { font-size: 24px; font-weight: 700; color: #f1f5f9; margin: 0; }
.scene-items { display: flex; flex-direction: column; gap: 14px; }
.scene-item { display: flex; align-items: flex-start; gap: 14px; }
.scene-bullet {
  width: 28px; height: 28px; border-radius: 50%; flex-shrink: 0;
  background: linear-gradient(135deg, var(--acid), var(--ice));
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700;
}
.scene-text { font-size: 16px; color: #e2e8f0; line-height: 1.7; padding-top: 3px; }

/* 进度条 */
.video-progress-bar { margin-top: 16px; }
.progress-track {
  height: 6px; background: #141A14; border-radius: 3px; position: relative; cursor: pointer;
}
.progress-fill { height: 100%; background: linear-gradient(90deg, var(--acid), var(--ice)); border-radius: 3px; transition: width 0.4s ease; }
.progress-marker {
  position: absolute; top: -3px; width: 12px; height: 12px; border-radius: 50%;
  background: #475569; border: 2px solid #141A14; transform: translateX(-50%); transition: all 0.3s;
}
.progress-marker.done { background: var(--ink-3); border-color: var(--ink-5); }
.progress-info { display: flex; justify-content: space-between; font-size: 12px; color: #94a3b8; margin-top: 8px; }

/* 场景过渡 */
.scene-fade-enter-active, .scene-fade-leave-active { transition: all 0.4s ease; }
.scene-fade-enter { transform: translateY(20px); opacity: 0; }
.scene-fade-leave-to { transform: translateY(-20px); opacity: 0; }

/* ── 代码在线运行沙箱 ── */
.code-hint { font-size: 12px; color: #22c55e; font-weight: 500; margin-left: 8px; }
.code-playground { display: flex; flex-direction: column; }
.code-tabs {
  display: flex; gap: 0; border-bottom: 2px solid #e2e8f0; background: var(--ink-3);
  overflow-x: auto; padding: 0 12px;
}
.code-tab {
  padding: 10px 18px; cursor: pointer; font-size: 13px; font-weight: 500;
  color: #64748b; border-bottom: 2px solid transparent; margin-bottom: -2px;
  transition: all 0.2s; white-space: nowrap; display: flex; align-items: center; gap: 6px;
  user-select: none;
}
.code-tab:hover { color: var(--acid); background: rgba(var(--acid-rgb), 0.04); }
.code-tab.active { color: var(--acid); border-bottom-color: var(--ink-5); font-weight: 600; }
.code-tab-lang {
  padding: 1px 6px; border-radius: 3px; font-size: 10px; text-transform: uppercase;
  background: var(--ink-3); color: var(--grey); font-weight: 600;
}
.code-tab.active .code-tab-lang { background: rgba(var(--acid-rgb), 0.15); color: var(--acid); }
.code-tab-num { color: #94a3b8; font-size: 11px; }

.code-editor-pane { border-bottom: 1px solid #e2e8f0; }
.code-editor-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 14px; background: #141A14;
}
.code-editor-lang {
  padding: 2px 10px; background: var(--ink-3); color: #fff; border-radius: 4px;
  font-size: 11px; font-weight: 600; text-transform: uppercase;
}
.code-editor-actions { display: flex; gap: 6px; }
.code-textarea {
  width: 100%; background: var(--ink-2); color: #e2e8f0; border: none;
  font-family: 'JetBrains Mono','Fira Code','Consolas','Courier New',monospace;
  font-size: 13px; line-height: 1.65; padding: 16px; resize: vertical; outline: none;
  tab-size: 2; min-height: 200px;
}
.code-textarea:focus { background: #0a0f1a; }
.code-textarea::selection { background: rgba(var(--acid-rgb), 0.4); }

/* 运行结果区 */
.code-result-pane { background: #fff; }
.code-result-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 16px; background: var(--ink-3); border-top: 1px solid #e2e8f0;
  font-size: 13px; font-weight: 600; color: var(--grey);
}
.result-badge {
  display: inline-block; padding: 1px 8px; border-radius: 10px; font-size: 11px;
  margin-left: 8px; background: var(--ink-3); color: #64748b; font-weight: 500;
}
.result-badge.badge-ok { background: var(--ink-3); color: #065f46; }
.result-badge.badge-err { background: var(--ink-3); color: #991b1b; }

.code-console {
  padding: 12px 16px; background: #141A14; margin: 0;
  min-height: 60px; max-height: 250px; overflow-y: auto;
}
.code-console-text {
  margin: 0; font-family: 'JetBrains Mono','Fira Code','Consolas',monospace;
  font-size: 13px; line-height: 1.6; color: #e2e8f0; white-space: pre-wrap; word-break: break-all;
}
.code-console-placeholder {
  padding: 32px; text-align: center; color: #94a3b8; font-size: 14px;
  background: var(--ink-3);
}

/* 预览iframe */
.code-preview-wrap { border-top: 2px solid var(--acid); }
.code-preview-header {
  padding: 8px 16px; background: rgba(var(--acid-rgb), 0.08); color: var(--acid);
  font-size: 12px; font-weight: 600;
}
.code-sandbox {
  width: 100%; height: 420px; border: none; display: block;
  background: #fff;
}

@media (max-width: 768px) {
  .type-selector { grid-template-columns: repeat(3, 1fr); }
  .gen-input-row { flex-direction: column; }
  .ppt-slide { padding: 24px 20px; min-height: 240px; }
  .video-scene-card { padding: 20px 16px; }
}
/* PPT 配图画廊 */
.ppt-images-gallery { margin-top: 24px; border-top: 1px solid #e2e8f0; padding-top: 16px; }
.ppt-images-gallery .gallery-title { font-size: 14px; font-weight: 600; color: #1e293b; margin-bottom: 12px; }
.ppt-images-gallery .gallery-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; }
.ppt-images-gallery .gallery-item { border: 1px solid #e2e8f0; border-radius: 10px; overflow: hidden; background: #fff; transition: all .2s; }
.ppt-images-gallery .gallery-item:hover { box-shadow: 0 4px 12px rgba(var(--acid-rgb), 0.12); transform: translateY(-2px); }
.ppt-images-gallery .gallery-img { width: 100%; height: 160px; object-fit: cover; display: block; }
.ppt-images-gallery .gallery-info { display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; }
.ppt-images-gallery .gallery-keyword { font-size: 12px; color: var(--grey); font-weight: 500; }
</style>

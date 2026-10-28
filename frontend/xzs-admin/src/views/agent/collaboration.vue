<template>
  <div class="collab-container">
    <div class="collab-header">
      <h2>多智能体协作可视化</h2>
      <p>实时观察 Supervisor 编排多个专业 Agent 共享态协同生成教学资源的真实过程</p>
    </div>

    <div class="collab-main">
      <!-- Left: Agent Graph -->
      <div class="graph-panel">
        <div class="panel-title">
          <i class="el-icon-connection"></i>
          <span>智能体拓扑（实时高亮当前执行 Agent）</span>
        </div>
        <div ref="agentGraph" style="width:100%;height:460px;"></div>
      </div>

      <!-- Right: Chat -->
      <div class="chat-panel">
        <div class="panel-title">
          <i class="el-icon-chat-dot-round"></i>
          <span>协作过程</span>
        </div>
        <div class="chat-messages" ref="chatList">
          <div v-for="(msg, i) in messages" :key="i" :class="['msg', msg.role]">
            <div class="msg-avatar">{{ msg.role === 'user' ? 'U' : 'AI' }}</div>
            <div class="msg-content">
              <div class="msg-name">{{ msg.role === 'user' ? '用户' : msg.agentName }}</div>
              <div class="msg-body">{{ msg.content }}</div>
            </div>
          </div>
        </div>
        <div class="chat-input">
          <el-input
            v-model="topic"
            placeholder="输入教学主题，如「导数」「二叉树」「TCP/IP」..."
            @keyup.enter.native="startCollaboration"
            :disabled="streaming"
          />
          <el-button type="primary" :loading="streaming" @click="startCollaboration">
            <span v-if="!streaming">启动协作</span>
            <span v-else>协作中</span>
          </el-button>
        </div>
        <!-- 学习交互反馈输入（可选，用于动态迭代决策） -->
        <div class="feedback-input">
          <el-button type="text" @click="showFeedbackInput = !showFeedbackInput">
            <i class="el-icon-edit-outline"></i>
            {{ showFeedbackInput ? '收起学习反馈' : '填写学习反馈（可选，用于动态迭代决策）' }}
          </el-button>
          <div v-if="showFeedbackInput" class="feedback-form">
            <el-input v-model="learningFeedback.total_questions" placeholder="答题总数" style="width:120px;" :disabled="streaming">
              <template slot="prepend">题数</template>
            </el-input>
            <el-input v-model="learningFeedback.correct_questions" placeholder="答对数" style="width:120px;" :disabled="streaming">
              <template slot="prepend">对数</template>
            </el-input>
            <span class="feedback-hint">用于 Adapt Agent 决策「降维解释」或「进阶挑战」</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 学情诊断报告（PDF 整改新增） -->
    <div class="diagnosis-panel" v-if="diagnosisResult">
      <div class="panel-title">
        <i class="el-icon-aim"></i>
        <span>学情诊断报告（Diagnosis Agent）</span>
        <span class="overall-badge" v-if="diagnosisResult.difficulty_target">推荐难度：{{ diagnosisResult.difficulty_target }}</span>
      </div>
      <div class="diagnosis-content">
        <div class="diagnosis-section" v-if="diagnosisResult.knowledge_gaps && diagnosisResult.knowledge_gaps.length">
          <div class="section-title">🎯 知识盲区定位</div>
          <div class="gap-list">
            <div class="gap-item" v-for="(g, i) in diagnosisResult.knowledge_gaps" :key="i">
              <span class="gap-severity" :class="'sev-' + (g.severity || 'low')">{{ g.severity || 'low' }}</span>
              <span class="gap-point">{{ g.point }}</span>
              <span class="gap-subject">{{ g.subject }}</span>
              <span class="gap-score" v-if="g.score != null">掌握度 {{ g.score }}</span>
            </div>
          </div>
        </div>
        <div class="diagnosis-section" v-if="diagnosisResult.priority_focus && diagnosisResult.priority_focus.length">
          <div class="section-title">⚡ 优先关注</div>
          <ol class="focus-list">
            <li v-for="(f, i) in diagnosisResult.priority_focus" :key="i">{{ f }}</li>
          </ol>
        </div>
        <div class="diagnosis-section" v-if="diagnosisResult.adaptation_strategy">
          <div class="section-title">🛠️ 适配策略</div>
          <div class="strategy-text">{{ diagnosisResult.adaptation_strategy }}</div>
        </div>
        <!-- 资源难度匹配曲线（PDF 智能决策与反馈①：学情与资源匹配度报告） -->
        <div class="diagnosis-section" v-if="(diagnosisResult.knowledge_gaps || []).length">
          <div class="section-title">📈 资源难度匹配曲线</div>
          <div ref="matchCurve" class="match-curve-chart"></div>
          <div class="curve-legend-hint">实线为学习者当前掌握度，虚线为多智能体适配后的资源难度——高严重度盲区自动降维、低严重度适度进阶，两条线的贴合程度即匹配度。</div>
        </div>
      </div>
    </div>

    <!-- 辩论与交叉验证报告（PDF 创新性核心） -->
    <div class="debate-panel" v-if="debateResult">
      <div class="panel-title">
        <i class="el-icon-shield"></i>
        <span>辩论与交叉验证报告（Debate Agent）</span>
        <span class="overall-badge" :class="debateResult.hallucination_prevented ? 'badge-success' : 'badge-warning'">
          幻觉率 {{ ((debateResult.hallucination_rate || 0) * 100).toFixed(1) }}%（目标 &lt;5%）
        </span>
      </div>
      <div class="debate-content">
        <div class="debate-stats">
          <div class="stat-card">
            <div class="stat-value">{{ debateResult.contradictions_count || 0 }}</div>
            <div class="stat-label">Agent 间矛盾</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ debateResult.hallucinations_count || 0 }}</div>
            <div class="stat-label">识别幻觉</div>
          </div>
          <div class="stat-card" v-if="debateResult.total_facts">
            <div class="stat-value">{{ debateResult.verified_count || 0 }}/{{ debateResult.total_facts }}</div>
            <div class="stat-label">事实核验通过</div>
          </div>
          <div class="stat-card" :class="debateResult.hallucination_prevented ? 'stat-success' : 'stat-warning'">
            <div class="stat-value">{{ debateResult.hallucination_prevented ? '是' : '否' }}</div>
            <div class="stat-label">幻觉已防控</div>
          </div>
        </div>

        <!-- 幻觉证据链明细（高保真溯源） -->
        <div class="evidence-section" v-if="(debateResult.hallucinations || []).length">
          <div class="section-title">🔍 幻觉识别与裁决证据链</div>
          <div class="hallucination-card" v-for="(h, i) in debateResult.hallucinations" :key="'h' + i"
               :class="'sev-' + (h.severity || 'minor')">
            <div class="h-head">
              <span class="h-type">{{ h.type || '幻觉' }}</span>
              <span class="h-agent">来源：{{ agentLabel(h.agent) }}</span>
              <span class="h-sev" :class="'sev-tag-' + (h.severity || 'minor')">{{ severityLabel(h.severity) }}</span>
            </div>
            <div class="h-row"><span class="h-key">问题内容</span><span class="h-val bad">{{ h.content }}</span></div>
            <div class="h-row"><span class="h-key">辩论裁决修正</span><span class="h-val good">{{ h.correction }}</span></div>
          </div>
        </div>

        <!-- Agent 间矛盾与裁决 -->
        <div class="evidence-section" v-if="(debateResult.contradictions || []).length">
          <div class="section-title">⚖️ 交叉验证矛盾裁决</div>
          <div class="contradiction-card" v-for="(c, i) in debateResult.contradictions" :key="'c' + i">
            <div class="h-head">
              <span class="h-agent">{{ agentLabel(c.agent_a) }} ⇄ {{ agentLabel(c.agent_b) }}</span>
            </div>
            <div class="h-row"><span class="h-key">矛盾点</span><span class="h-val">{{ c.issue }}</span></div>
            <div class="h-row"><span class="h-key">裁决结论</span><span class="h-val good">{{ c.resolution }}</span></div>
          </div>
        </div>

        <!-- 知识库溯源引用 -->
        <div class="evidence-section" v-if="(debateResult.kb_evidence || []).length">
          <div class="section-title">📚 知识库溯源引用（高保真依据）</div>
          <div class="kb-evidence-item" v-for="(e, i) in debateResult.kb_evidence" :key="'e' + i">
            <i class="el-icon-document-checked"></i>
            <span class="kb-claim">{{ e.claim }}</span>
            <span class="kb-source">—— {{ e.source_doc }}<template v-if="e.section"> · {{ e.section }}</template></span>
          </div>
        </div>

        <div class="debate-summary" v-if="debateResult.debate_summary">
          <div class="section-title">📋 辩论总结</div>
          <div class="summary-text">{{ debateResult.debate_summary }}</div>
        </div>
      </div>
    </div>

    <!-- 质量评估摘要 -->
    <div class="review-panel" v-if="reviewScores">
      <div class="panel-title">
        <i class="el-icon-data-analysis"></i>
        <span>资源质量评估（Quality Review Agent）</span>
        <span class="overall-badge">总体 {{ reviewOverall }} / 10</span>
      </div>
      <div class="score-grid">
        <div class="score-card" v-for="(v, k) in reviewScores" :key="k">
          <div class="score-label">{{ scoreLabel(k) }}</div>
          <div class="score-value" :class="scoreClass(v)">{{ v }}<span>/10</span></div>
          <div class="score-bar"><div class="score-bar-fill" :style="{ width: (v*10) + '%', background: scoreColor(v) }"></div></div>
        </div>
      </div>
    </div>

    <!-- 动态迭代决策报告（PDF 反馈机制） -->
    <div class="adapt-panel" v-if="adaptResult">
      <div class="panel-title">
        <i class="el-icon-refresh"></i>
        <span>动态迭代决策（Adapt Agent）</span>
        <span class="overall-badge" :class="'decision-' + adaptResult.decision">
          决策：{{ decisionLabel(adaptResult.decision) }}
        </span>
      </div>
      <div class="adapt-content">
        <div class="adapt-reason" v-if="adaptResult.reason">
          <div class="section-title">📊 决策依据</div>
          <div class="reason-text">{{ adaptResult.reason }}</div>
        </div>
        <div class="adapt-feedback" v-if="adaptResult.learning_feedback">
          <div class="section-title">💬 学习反馈</div>
          <div class="feedback-text">{{ adaptResult.learning_feedback }}</div>
        </div>
      </div>
    </div>

    <!-- Bottom: Execution Timeline（真实事件） -->
    <div class="timeline-panel" v-if="steps.length > 0">
      <div class="panel-title">
        <i class="el-icon-time"></i>
        <span>执行时间线（真实事件 · 真实耗时）</span>
        <span class="step-count">{{ steps.length }} 步</span>
      </div>
      <div class="timeline">
        <div
          v-for="(step, i) in steps"
          :key="i"
          class="timeline-item"
          :class="[step.status, { clickable: step.agentKey && step.status === 'completed' && (agentOutputs[step.agentKey] || step.panelSelector) }]"
          @click="jumpToAgent(step)"
        >
          <div class="step-index">{{ i + 1 }}</div>
          <div class="step-info">
            <div class="step-top-row">
              <span class="step-agent">{{ step.agentName }}</span>
              <span class="step-duration" v-if="step.durationMs">{{ step.durationMs }}ms</span>
              <span class="step-status-badge">
                <span :class="'badge-' + step.status">{{ step.status === 'completed' ? '完成' : step.status === 'failed' ? '失败' : '运行中' }}</span>
              </span>
              <span v-if="step.agentKey && agentOutputs[step.agentKey]" class="step-jump-hint">
                <i class="el-icon-view" /> 查看内容
              </span>
            </div>
            <div class="step-action">{{ step.action }}</div>
            <div v-if="step.inputSummary || step.outputSummary" class="step-io">
              <span v-if="step.inputSummary" class="step-input">
                <i class="el-icon-bottom" /> {{ step.inputSummary }}
              </span>
              <span v-if="step.outputSummary" class="step-output">
                <i class="el-icon-top" /> {{ step.outputSummary }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 完整生成结果 -->
    <div ref="resultPanel" class="result-panel" v-if="showFinal || Object.keys(agentOutputs).length > 0">
      <div class="panel-title">
        <i class="el-icon-document"></i>
        <span>多智能体协同生成结果</span>
        <el-button type="text" @click="showFinal = !showFinal">{{ showFinal ? '收起' : '展开' }}</el-button>
      </div>

      <!-- 各 Agent 生成产出 Tab 切换 -->
      <div class="output-tabs" v-if="Object.keys(agentOutputs).length">
        <div class="output-tab-bar">
          <div v-for="key in outputKeys" :key="key"
               class="output-tab-item" :class="{ active: activeOutputTab === key }"
               @click="switchOutputTab(key)">
            <i :class="outputIcon(key)"></i>
            <span>{{ outputLabel(key) }}</span>
          </div>
          <el-button class="view-detail-btn" type="text" @click="openDetailDialog">
            <i class="el-icon-full-screen"></i> 查看详情
          </el-button>
        </div>
        <div class="output-tab-content">
          <!-- 文档 / 拓展阅读：Markdown 渲染 -->
          <div v-if="activeOutputTab === 'document' || activeOutputTab === 'reading'" class="detail-markdown" v-html="renderedMarkdown"></div>

          <!-- 思维导图：SVG 可视化 -->
          <div v-else-if="activeOutputTab === 'mindmap'" class="mindmap-panel">
            <div class="panel-header">
              <i class="el-icon-share"></i> 思维导图
              <span class="mm-badge">SVG Render</span>
              <div class="mm-tools">
                <el-button size="mini" icon="el-icon-zoom-in" circle @click="collabMmZoom(0.15)"></el-button>
                <el-button size="mini" icon="el-icon-zoom-out" circle @click="collabMmZoom(-0.15)"></el-button>
                <el-button size="mini" icon="el-icon-rank" circle @click="collabMmFit"></el-button>
                <el-button size="mini" icon="el-icon-download" circle @click="collabMmDownload" title="导出 SVG"></el-button>
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
              ref="collabMmContainer"
              class="mm-container"
              @mousedown="collabMmMouseDown"
              @wheel.prevent="collabMmWheel"
            ></div>
          </div>

          <!-- 练习题目：交互式答题卡 -->
          <div v-else-if="activeOutputTab === 'quiz'" class="detail-quiz">
            <div v-if="collabQuizParsed.length === 0" class="output-placeholder">暂未解析出题目</div>
            <template v-else>
              <div class="exercise-actions" style="margin-bottom:12px;display:flex;gap:8px;">
                <el-button v-if="!collabQuizSubmitted" size="mini" type="primary" @click="collabQuizSubmit" :disabled="!collabQuizParsed.length">提交答案</el-button>
                <el-button v-if="collabQuizSubmitted" size="mini" type="success" @click="collabQuizReset">重新作答</el-button>
              </div>
              <div
                v-for="(q, qi) in collabQuizParsed"
                :key="qi"
                class="quiz-card"
                :class="{ correct: collabQuizSubmitted && collabQuizResults[qi] === true, wrong: collabQuizSubmitted && collabQuizResults[qi] === false }"
              >
                <div class="quiz-num">Q{{ qi + 1 }}</div>
                <div class="quiz-q-type" :style="{ background: q.type === 'choice' ? '#6366f1' : q.type === 'fill' ? '#34d399' : q.type === 'tf' ? '#fbbf24' : '#fb7185' }">{{ q.typeLabel }}</div>
                <div class="quiz-q-text">{{ q.question }}</div>

                <!-- 选择题 -->
                <div v-if="q.type === 'choice'" class="quiz-options">
                  <el-radio-group v-model="collabQuizAnswers[qi]" :disabled="collabQuizSubmitted" size="small">
                    <div v-for="(opt, oi) in q.options" :key="oi" class="quiz-option" :class="{ 'opt-correct': collabQuizSubmitted && oi === q.answer, 'opt-wrong': collabQuizSubmitted && Number(collabQuizAnswers[qi]) === oi && oi !== q.answer }">
                      <el-radio :label="oi" class="quiz-radio">{{ opt }}</el-radio>
                    </div>
                  </el-radio-group>
                </div>

                <!-- 判断题 -->
                <div v-else-if="q.type === 'tf'" class="quiz-options">
                  <el-radio-group v-model="collabQuizAnswers[qi]" :disabled="collabQuizSubmitted" size="small">
                    <el-radio :label="0" class="quiz-radio">✓ 正确</el-radio>
                    <el-radio :label="1" class="quiz-radio">✗ 错误</el-radio>
                  </el-radio-group>
                </div>

                <!-- 填空/简答 -->
                <div v-else class="quiz-input-area">
                  <el-input v-model="collabQuizAnswers[qi]" :disabled="collabQuizSubmitted" placeholder="请输入答案..." size="small" clearable></el-input>
                </div>

                <!-- 答案解析 -->
                <div v-if="collabQuizSubmitted" class="quiz-feedback">
                  <div class="quiz-fb-row" v-if="q.type === 'choice'"><b>正确答案：</b>{{ q.options[q.answer] }}</div>
                  <div class="quiz-fb-row" v-if="q.type === 'tf'"><b>正确答案：</b>{{ q.answer === 0 ? '正确 ✓' : '错误 ✗' }}</div>
                  <div class="quiz-fb-row" v-if="q.type === 'fill' || q.type === 'short'"><b>参考答案：</b>{{ q.refAnswer }}</div>
                  <div class="quiz-fb-row" v-if="q.explanation"><b>解析：</b>{{ q.explanation }}</div>
                  <div class="quiz-fb-result">
                    <span v-if="collabQuizResults[qi] === true" class="fb-tag fb-ok">✓ 回答正确</span>
                    <span v-else-if="collabQuizResults[qi] === false" class="fb-tag fb-no">✗ 回答错误</span>
                    <span v-else class="fb-tag fb-skip">— 未作答</span>
                  </div>
                </div>
              </div>

              <!-- 成绩统计 -->
              <div v-if="collabQuizSubmitted" class="quiz-score-bar">
                <div class="quiz-score-left">
                  <span class="quiz-score-big">{{ collabQuizCorrectCount }} / {{ collabQuizParsed.length }}</span>
                  <span class="quiz-score-pct">{{ collabQuizParsed.length > 0 ? Math.round(collabQuizCorrectCount / collabQuizParsed.length * 100) : 0 }}分</span>
                </div>
                <el-progress :percentage="collabQuizParsed.length > 0 ? Math.round(collabQuizCorrectCount / collabQuizParsed.length * 100) : 0" :color="collabQuizParsed.length > 0 && collabQuizCorrectCount / collabQuizParsed.length >= 0.6 ? '#34d399' : '#fb7185'" :stroke-width="12"></el-progress>
              </div>
            </template>
          </div>

          <!-- 代码案例：代码块 + 运行 -->
          <div v-else-if="activeOutputTab === 'code'" class="detail-code">
            <div v-if="collabCodeBlocks.length === 0" class="output-placeholder">暂未解析出代码</div>
            <template v-else>
              <div class="code-tabs">
                <div
                  v-for="(block, bi) in collabCodeBlocks"
                  :key="bi"
                  :class="['code-tab', { active: collabActiveCodeIdx === bi }]"
                  @click="collabActiveCodeIdx = bi"
                >
                  <span class="code-tab-lang">{{ block.lang || 'code' }}</span>
                  <span class="code-tab-num">#{{ bi + 1 }}</span>
                </div>
              </div>
              <div v-if="collabCodeBlocks[collabActiveCodeIdx]" class="code-editor-pane">
                <div class="code-editor-toolbar">
                  <span class="code-editor-lang">{{ collabCodeBlocks[collabActiveCodeIdx].lang || 'code' }}</span>
                  <div class="code-editor-actions">
                    <el-button size="mini" type="success" icon="el-icon-video-play" @click="collabRunCode(collabActiveCodeIdx)" :loading="collabCodeBlocks[collabActiveCodeIdx].running">
                      {{ collabCodeBlocks[collabActiveCodeIdx].running ? '运行中...' : '▶ 运行' }}
                    </el-button>
                    <el-button size="mini" icon="el-icon-document-copy" @click="collabCopyCode(collabCodeBlocks[collabActiveCodeIdx].code)">复制</el-button>
                  </div>
                </div>
                <textarea
                  v-model="collabCodeBlocks[collabActiveCodeIdx].code"
                  class="code-textarea"
                  :rows="Math.max(8, Math.min(25, (collabCodeBlocks[collabActiveCodeIdx].code || '').split('\n').length))"
                  spellcheck="false"
                ></textarea>
              </div>
              <div v-if="collabCodeBlocks[collabActiveCodeIdx]" class="code-result-pane">
                <div class="code-result-header">
                  <span>
                    <i class="el-icon-caret-right"></i> 运行结果
                    <span :class="['result-badge', collabCodeBlocks[collabActiveCodeIdx].exitOk === false ? 'badge-err' : collabCodeBlocks[collabActiveCodeIdx].output ? 'badge-ok' : '']">
                      {{ collabCodeBlocks[collabActiveCodeIdx].exitOk === false ? '✗ 错误' : collabCodeBlocks[collabActiveCodeIdx].output ? '✓ 成功' : '等待运行' }}
                    </span>
                  </span>
                </div>
                <div v-if="collabCodeBlocks[collabActiveCodeIdx].output" class="code-console">
                  <pre class="code-console-text">{{ collabCodeBlocks[collabActiveCodeIdx].output }}</pre>
                </div>
                <div v-else class="code-console-placeholder">点击「▶ 运行」按钮查看代码执行效果</div>
              </div>
            </template>
          </div>

          <!-- 视频脚本：场景轮播 + TTS（与多智能体生成页一致） -->
          <div v-else-if="activeOutputTab === 'video'" class="video-panel">
            <div class="panel-header">
              <i class="el-icon-video-camera"></i> 教学视频
              <span class="anim-hint">AI 生成 · 语音讲解</span>
              <div class="video-actions">
                <el-button size="mini" :type="collabVideoTtsOn ? 'warning' : 'default'" icon="el-icon-microphone" @click="collabVideoTtsOn = !collabVideoTtsOn" :title="collabVideoTtsOn ? '关闭配音' : '开启配音'">
                  {{ collabVideoTtsOn ? '配音中' : '已静音' }}
                </el-button>
                <el-button size="mini" icon="el-icon-video-play" @click="collabVideoPlayFromStart" :disabled="!collabVideoScenes.length">从头播放</el-button>
                <el-select v-model="collabAgnesDuration" size="mini" style="width:86px" title="AI 成片时长">
                  <el-option :value="5" label="5 秒成片" />
                  <el-option :value="10" label="10 秒成片" />
                </el-select>
                <el-button size="mini" type="success" icon="el-icon-film" :loading="collabAgnesSubmitting" @click="collabGenerateAgnesVideo" title="将脚本提交 Agnes V2.0 生成真实 MP4 教学视频">
                  AI 生成真视频
                </el-button>
              </div>
            </div>

            <!-- Agnes 真视频：轮询状态 / 成片播放器 -->
            <div v-if="collabAgnesStatus === 'done' && collabAgnesVideoUrl" class="agnes-video-wrapper" style="margin-top:12px">
              <div style="display:flex;align-items:center;gap:8px;margin-bottom:8px">
                <el-tag size="mini" type="success">Agnes V2.0 成片</el-tag>
                <span style="font-size:12px;color:#64748b">{{ collabAgnesDuration }} 秒 · 1280×720 · MP4</span>
                <div style="flex:1"></div>
                <el-button size="mini" type="text" icon="el-icon-refresh" @click="collabGenerateAgnesVideo">重新生成</el-button>
              </div>
              <video :src="collabAgnesVideoUrl" controls autoplay loop style="width:100%;max-height:420px;border-radius:12px;background:#000" />
            </div>
            <div v-else-if="collabAgnesTaskId" class="agnes-video-progress" style="margin-top:12px;padding:14px 18px;border-radius:12px;background:linear-gradient(135deg,#eef4ff,#f8fbff);display:flex;align-items:center;gap:12px">
              <i class="el-icon-loading" style="font-size:20px;color:#3b82f6"></i>
              <div style="flex:1">
                <div style="font-size:13px;font-weight:600;color:#1e40af">Agnes 正在渲染真实教学视频…</div>
                <div style="font-size:12px;color:#64748b;margin-top:2px">状态：{{ collabAgnesStatus || 'submitted' }} · 任务 {{ collabAgnesTaskId.substring(0,16) }}… · 通常需要 1~5 分钟，可先浏览其他资源</div>
              </div>
              <el-button size="mini" type="text" icon="el-icon-close" @click="collabResetAgnes">取消</el-button>
            </div>

            <div v-if="collabVideoScenes.length > 0" class="video-player-wrapper">
              <div class="video-scene-display" ref="collabVideoScene">
                <transition name="scene-fade" mode="out-in">
                  <div :key="collabVideoSceneIdx" class="video-scene-card">
                    <div class="scene-header">
                      <span class="scene-badge">{{ collabVideoSceneIdx + 1 }} / {{ collabVideoScenes.length }}</span>
                      <h2 class="scene-title">{{ collabVideoScenes[collabVideoSceneIdx].title }}</h2>
                    </div>
                    <div class="scene-items">
                      <div v-for="(item, ii) in collabVideoScenes[collabVideoSceneIdx].items" :key="ii" class="scene-item">
                        <span class="scene-bullet">{{ ii + 1 }}</span>
                        <span class="scene-text">{{ item }}</span>
                      </div>
                    </div>
                  </div>
                </transition>
              </div>
              <div class="video-progress-bar">
                <div class="progress-track" @click="collabVideoSeek">
                  <div class="progress-fill" :style="{width: collabVideoProgress + '%'}"></div>
                  <div v-for="(s, si) in collabVideoScenes" :key="si" class="progress-marker" :style="{left: (si/collabVideoScenes.length*100) + '%'}" :class="{ done: si <= collabVideoSceneIdx }"></div>
                </div>
                <div class="progress-info">
                  <span>第 {{ collabVideoSceneIdx + 1 }} 节</span>
                  <span v-if="collabVideoTtsOn" style="color:#22c55e">🔊 语音讲解中</span>
                </div>
              </div>
            </div>
            <div v-else class="output-placeholder">暂未解析出视频脚本</div>
          </div>

          <!-- 其他兜底 -->
          <pre v-else class="output-text">{{ agentOutputs[activeOutputTab] || '（无内容）' }}</pre>
        </div>
      </div>

    <!-- 全屏详情对话框 -->
    <el-dialog
      :title="outputLabel(activeOutputTab) + ' — 完整内容'"
      :visible.sync="detailDialogVisible"
      width="80%"
      top="5vh"
      custom-class="detail-dialog">
      <div class="detail-dialog-body" v-if="detailDialogVisible">
        <!-- 思维导图：Markdown 渲染 -->
        <div v-if="activeOutputTab === 'mindmap'" class="detail-markdown" v-html="renderedMarkdown"></div>
        <!-- 代码案例：代码块 -->
        <div v-else-if="activeOutputTab === 'code'" class="detail-code">
          <pre><code>{{ agentOutputs[activeOutputTab] || '' }}</code></pre>
        </div>
        <!-- 测试题：尝试解析为题目卡片 -->
        <div v-else-if="activeOutputTab === 'quiz'" class="detail-quiz">
          <div v-for="(q, qi) in parsedQuiz" :key="qi" class="quiz-card">
            <div class="quiz-num">Q{{ qi + 1 }}</div>
            <div class="quiz-content" v-html="q"></div>
          </div>
        </div>
        <!-- 其他：Markdown 渲染 -->
        <div v-else class="detail-markdown" v-html="renderedMarkdown"></div>
      </div>
    </el-dialog>

      <!-- Supervisor 汇总 -->
      <div class="result-summary" v-if="finalReply">
        <div class="summary-label"><i class="el-icon-coordination"></i> Supervisor 汇总</div>
        <div class="result-body">{{ finalReply }}</div>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { chartTheme, tc, tca } from '@/utils/fui-echarts-theme'
import resourceApi from '@/api/resourceGeneration'

// 事件 agent_key → 拓扑图节点名
const AGENT_NODE_MAP = {
  supervisor: 'Supervisor',
  mindmap: 'MindMap Agent',
  document: 'Doc Agent',
  reading: 'Reading Agent',
  code: 'Code Agent',
  quiz: 'Quiz Agent',
  video: 'Video Agent',
  review: 'Review Agent',
  diagnosis: 'Diagnosis Agent',
  debate: 'Debate Agent',
  adapt: 'Adapt Agent'
}

export default {
  name: 'AgentCollaboration',
  data () {
    return {
      chart: null,
      topic: '',
      streaming: false,
      messages: [],
      steps: [],
      reviewScores: null,
      reviewOverall: null,
      finalReply: '',
      showFinal: false,
      requestId: null,
      graphNodes: [],
      graphLinks: [],
      activeNodes: new Set(),
      // ── PDF 整改新增：学情诊断 / 辩论交叉验证 / 动态迭代决策 ──
      diagnosisResult: null,
      debateResult: null,
      adaptResult: null,
      // 各 Agent 的生成产出（字典：mindmap/document/reading/code/quiz/video → 文本）
      agentOutputs: {},
      activeOutputTab: 'mindmap',
      detailDialogVisible: false,
      // 协作页富展示：思维导图 / 题目 / 代码 / 视频脚本
      collabMmSvg: '',
      mmTree: null,
      mmScale: 1,
      mmPanX: 0,
      mmPanY: 0,
      collabQuizParsed: [],
      collabQuizAnswers: [],
      collabQuizResults: [],
      collabQuizSubmitted: false,
      collabCodeBlocks: [],
      collabActiveCodeIdx: 0,
      collabVideoScenes: [],
      collabVideoSceneIdx: 0,
      collabVideoProgress: 0,
      collabVideoTtsOn: true,
      // Agnes 真视频生成状态
      collabAgnesTaskId: '',
      collabAgnesStatus: '',
      collabAgnesVideoUrl: '',
      collabAgnesSubmitting: false,
      collabAgnesDuration: 10,
      collabAgnesPollTimer: null,
      collabVideoTtsQueue: [],
      collabVideoTtsSpeaking: false,
      collabVideoTimer: null,
      // 学习交互反馈（可选输入，用于动态迭代决策）
      learningFeedback: {
        accuracy_rate: '',
        total_questions: '',
        correct_questions: ''
      },
      showFeedbackInput: false
    }
  },
  computed: {
    outputKeys () {
      var order = ['mindmap', 'document', 'reading', 'code', 'quiz', 'video']
      var keys = Object.keys(this.agentOutputs)
      keys.sort(function (a, b) { return order.indexOf(a) - order.indexOf(b) })
      return keys
    },
    renderedMarkdown () {
      var text = this.agentOutputs[this.activeOutputTab] || ''
      if (!text) return '<p style="color:#94a3b8">（无内容）</p>'
      return this.simpleMarkdown(text)
    },
    parsedQuiz () {
      var text = this.agentOutputs[this.activeOutputTab] || ''
      if (!text) return ['（无内容）']
      var blocks = text.split(/\n(?=第[一二三四五六七八九十]+题|Q\d|题目\d|\d+[.、])/)
      if (blocks.length <= 1) {
        blocks = text.split(/\n\n+/)
      }
      return blocks.filter(function (b) { return b.trim() }).map(function (b) {
        return this.simpleMarkdown(b)
      }.bind(this))
    },
    collabQuizCorrectCount () {
      return this.collabQuizResults.filter(function (r) { return r === true }).length
    }
  },
  watch: {
    activeOutputTab: function () {
      this.refreshCollabOutput()
    },
    agentOutputs: {
      handler: function () {
        if (this.agentOutputs[this.activeOutputTab]) {
          this.refreshCollabOutput()
        }
      },
      deep: true
    }
  },
  mounted () {
    this.initGraph()
    window.addEventListener('resize', this.handleResize)
    this.addWelcomeMessage()
  },
  beforeDestroy () {
    window.removeEventListener('resize', this.handleResize)
    this.collabStopVideoTts()
    if (this._mmOnMove) document.removeEventListener('mousemove', this._mmOnMove)
    if (this._mmOnUp) document.removeEventListener('mouseup', this._mmOnUp)
    if (this.chart) {
      this.chart.dispose()
      this.chart = null
    }
  },
  methods: {
    addWelcomeMessage () {
      this.messages.push({
        role: 'assistant',
        agentName: 'Supervisor',
        content: '输入教学主题启动协作。完整流程：Supervisor 规划 → Diagnosis Agent 学情诊断 → MindMap/Doc/Reading/Code/Guide/Quiz/Video 七大 Agent 共享态协同生成 → Debate Agent 辩论与交叉验证（消除幻觉+知识溯源）→ Quality Review Agent 质量审查 → Adapt Agent 动态迭代决策（降维解释/进阶挑战）。'
      })
    },

    outputLabel (key) {
      return {
        mindmap: '思维导图',
        document: '课程文档',
        reading: '拓展阅读',
        code: '代码案例',
        guide: '实操指南',
        quiz: '分阶测试',
        video: '视频脚本'
      }[key] || key
    },
    outputIcon (key) {
      return {
        mindmap: 'el-icon-share',
        document: 'el-icon-document',
        reading: 'el-icon-reading',
        code: 'el-icon-edit',
        guide: 'el-icon-guide',
        quiz: 'el-icon-question',
        video: 'el-icon-video-camera'
      }[key] || 'el-icon-files'
    },

    openDetailDialog () {
      this.detailDialogVisible = true
    },

    simpleMarkdown (text) {
      if (!text) return ''
      var html = text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
      html = html.replace(/```([\s\S]*?)```/g, function (m, p1) {
        return '<pre class="md-code-block"><code>' + p1.trim() + '</code></pre>'
      })
      html = html.replace(/`([^`]+)`/g, '<code class="md-inline-code">$1</code>')
      html = html.replace(/^### (.+)$/gm, '<h3 class="md-h3">$1</h3>')
      html = html.replace(/^## (.+)$/gm, '<h2 class="md-h2">$1</h2>')
      html = html.replace(/^# (.+)$/gm, '<h1 class="md-h1">$1</h1>')
      html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
      html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>')
      html = html.replace(/^[-*•] (.+)$/gm, '<li>$1</li>')
      html = html.replace(/^\d+[.、] (.+)$/gm, '<li class="md-ol">$1</li>')
      html = html.replace(/(<li[\s\S]*?<\/li>)/g, '<ul class="md-ul">$1</ul>')
      html = html.replace(/<\/ul>\s*<ul/g, '</ul>\n<ul')
      html = html.replace(/\n{2,}/g, '</p><p class="md-p">')
      html = '<p class="md-p">' + html + '</p>'
      html = html.replace(/<p class="md-p"><\/p>/g, '')
      html = html.replace(/<p class="md-p"><h/g, '<h').replace(/<\/h(\d)><\/p>/g, '</h$1>')
      html = html.replace(/<p class="md-p"><pre/g, '<pre').replace(/<\/pre><\/p>/g, '</pre>')
      html = html.replace(/<p class="md-p"><ul/g, '<ul').replace(/<\/ul><\/p>/g, '</ul>')
      html = html.replace(/<p class="md-p"><li/g, '<li').replace(/<\/li><\/p>/g, '</li>')
      return html
    },

    initGraph () {
      if (!this.$refs.agentGraph) return
      this.chart = echarts.init(this.$refs.agentGraph, chartTheme())

      var nodes = [
        { name: 'Supervisor', category: 0, symbolSize: 70, fixed: true, x: 400, y: 230, itemStyle: { color: tc('acid'), shadowBlur: 20, shadowColor: tca('acid', 0.5) } },
        { name: 'Diagnosis Agent', category: 3, symbolSize: 48, itemStyle: { color: tc('ice') } },
        { name: 'MindMap Agent', category: 1, symbolSize: 50 },
        { name: 'Doc Agent', category: 1, symbolSize: 50 },
        { name: 'Reading Agent', category: 1, symbolSize: 50 },
        { name: 'Code Agent', category: 1, symbolSize: 50 },
        { name: 'Guide Agent', category: 1, symbolSize: 50 },
        { name: 'Quiz Agent', category: 1, symbolSize: 50 },
        { name: 'Video Agent', category: 1, symbolSize: 50 },
        { name: 'Debate Agent', category: 3, symbolSize: 48, itemStyle: { color: '#ef4444' } },
        { name: 'Review Agent', category: 1, symbolSize: 50 },
        { name: 'Adapt Agent', category: 3, symbolSize: 48, itemStyle: { color: '#10b981' } },
        { name: '数据库', category: 2, symbolSize: 32 },
        { name: '向量检索', category: 2, symbolSize: 32 }
      ]

      var links = [
        // Supervisor 编排所有 Agent
        { source: 'Supervisor', target: 'Diagnosis Agent' },
        { source: 'Supervisor', target: 'MindMap Agent' },
        { source: 'Supervisor', target: 'Doc Agent' },
        { source: 'Supervisor', target: 'Reading Agent' },
        { source: 'Supervisor', target: 'Code Agent' },
        { source: 'Supervisor', target: 'Guide Agent' },
        { source: 'Supervisor', target: 'Quiz Agent' },
        { source: 'Supervisor', target: 'Video Agent' },
        { source: 'Supervisor', target: 'Debate Agent' },
        { source: 'Supervisor', target: 'Review Agent' },
        { source: 'Supervisor', target: 'Adapt Agent' },
        // 协同依赖：diagnosis 先行 → generate 阶段复用诊断结论
        { source: 'Diagnosis Agent', target: 'MindMap Agent' },
        // 共享态依赖：mindmap 先行 → doc/reading/code/guide 复用；quiz 依赖 doc；video 依赖 doc+mindmap
        { source: 'MindMap Agent', target: 'Doc Agent' },
        { source: 'MindMap Agent', target: 'Reading Agent' },
        { source: 'MindMap Agent', target: 'Code Agent' },
        { source: 'MindMap Agent', target: 'Guide Agent' },
        { source: 'Doc Agent', target: 'Quiz Agent' },
        { source: 'Doc Agent', target: 'Video Agent' },
        { source: 'MindMap Agent', target: 'Video Agent' },
        // 辩论与交叉验证：对全部生成资源做交叉比对
        { source: 'Doc Agent', target: 'Debate Agent' },
        { source: 'Guide Agent', target: 'Debate Agent' },
        { source: 'Quiz Agent', target: 'Debate Agent' },
        { source: 'Code Agent', target: 'Debate Agent' },
        // 辩论结果 → Review → Adapt
        { source: 'Debate Agent', target: 'Review Agent' },
        { source: 'Review Agent', target: 'Adapt Agent' },
        { source: 'Quiz Agent', target: '数据库' },
        { source: 'Review Agent', target: '向量检索' },
        { source: 'Reading Agent', target: '向量检索' }
      ]

      this.graphNodes = nodes
      this.graphLinks = links

      var option = {
        backgroundColor: 'transparent',
        tooltip: {
          backgroundColor: 'rgba(255,255,255,0.95)',
          borderColor: tc('paper'),
          textStyle: { color: '#141A14' },
          formatter: function (params) {
            if (params.dataType === 'node') {
              var catName = ['编排器', '专业智能体', '工具', '协同决策'][params.data.category] || '节点'
              return '<strong>' + params.name + '</strong><br/>类型: ' + catName
            }
            return params.data.source + ' → ' + params.data.target
          }
        },
        legend: {
          data: ['编排器', '专业智能体', '工具', '协同决策'],
          textStyle: { color: '#475569', fontSize: 12 },
          top: 10,
          right: 10,
          orient: 'vertical',
          itemWidth: 12,
          itemHeight: 12
        },
        series: [{
          type: 'graph',
          layout: 'force',
          roam: true,
          draggable: true,
          categories: [
            { name: '编排器', itemStyle: { color: tc('acid') } },
            { name: '专业智能体', itemStyle: { color: tc('acid') } },
            { name: '工具', itemStyle: { color: '#f59e0b' } },
            { name: '协同决策', itemStyle: { color: tc('ice') } }
          ],
          data: nodes.map(function (n) { return Object.assign({}, n) }),
          links: links.map(function (l) { return Object.assign({}, l) }),
          lineStyle: { color: tca('acid', 0.25), curveness: 0.1, width: 1.5 },
          emphasis: { focus: 'adjacency', lineStyle: { width: 3 } },
          label: { show: true, color: '#334155', fontSize: 11, fontWeight: 500 },
          force: { repulsion: 320, edgeLength: [70, 150], gravity: 0.08 },
          animationDuration: 1000,
          animationEasingUpdate: 'quinticInOut',
          edgeSymbol: ['none', 'arrow'],
          edgeSymbolSize: [0, 7]
        }]
      }
      this.chart.setOption(option)
    },

    async startCollaboration () {
      var topic = this.topic.trim()
      if (!topic || this.streaming) return

      this.streaming = true
      this.steps = []
      this.messages = []
      this.reviewScores = null
      this.reviewOverall = null
      this.finalReply = ''
      this.showFinal = false
      this.diagnosisResult = null
      this.debateResult = null
      this.adaptResult = null
      this.agentOutputs = {}
      this.activeOutputTab = 'mindmap'
      this.collabResetAgnes()
      this.requestId = 'req_' + Date.now()
      this.activeNodes = new Set()

      this.addMessage('assistant', 'Supervisor', '为教学主题「' + topic + '」启动多智能体协作…')
      this.highlightNode('Supervisor', true)

      // 构造学习交互反馈（若用户填写了答题正确率）
      var feedback = {}
      if (this.learningFeedback.total_questions && this.learningFeedback.correct_questions) {
        var total = parseInt(this.learningFeedback.total_questions)
        var correct = parseInt(this.learningFeedback.correct_questions)
        if (total > 0) {
          feedback.accuracy_rate = correct / total
          feedback.total_questions = total
          feedback.correct_questions = correct
        }
      } else if (this.learningFeedback.accuracy_rate) {
        feedback.accuracy_rate = parseFloat(this.learningFeedback.accuracy_rate)
      }

      try {
        var token = localStorage.getItem('token') || ''
        var resp = await fetch('/api/admin/agent/stream-collaboration', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', token: token },
          credentials: 'include',
          body: JSON.stringify({
            topic: topic,
            context: {},
            requestId: this.requestId,
            learningFeedback: feedback
          })
        })
        if (!resp.ok) throw new Error('HTTP ' + resp.status)

        var reader = resp.body.getReader()
        var decoder = new TextDecoder('utf-8')
        var buffer = ''
        while (true) {
          var chunk = await reader.read()
          if (chunk.done) break
          buffer += decoder.decode(chunk.value, { stream: true })
          var idx
          while ((idx = buffer.indexOf('\n')) >= 0) {
            var line = buffer.slice(0, idx).trim()
            buffer = buffer.slice(idx + 1)
            if (line.indexOf('data: ') === 0) {
              var payload = line.slice(6).trim()
              if (payload === '[DONE]') continue
              try {
                this.handleEvent(JSON.parse(payload))
              } catch (e) {
                /* ignore malformed line */
              }
            }
          }
        }
      } catch (e) {
        this.addMessage('assistant', 'System', '协作生成失败：' + (e && e.message ? e.message : e))
      } finally {
        this.streaming = false
        this.resetAllNodes()
      }
    },

    handleEvent (ev) {
      if (!ev) return
      var node = AGENT_NODE_MAP[ev.agent_key]
      switch (ev.type) {
        case 'plan_start':
          this.addMessage('assistant', 'Supervisor', '正在规划任务、编排参与 Agent 及依赖关系…')
          break
        case 'plan_end':
          this.addStep('Supervisor', '任务规划与 Agent 编排', 'completed', ev.durationMs, '', (ev.agents || []).length + ' 个 Agent', 'supervisor', '.result-panel')
          this.addMessage('assistant', 'Supervisor',
            '规划完成：激活 ' + (ev.agents || []).length + ' 个 Agent，难度「' + (ev.difficulty || '进阶') + '」')
          break
        // ── 学情诊断事件（PDF 整改新增）──
        case 'diagnosis_start':
          if (node) { this.activeNodes.add(node); this.highlightActive() }
          this.addMessage('assistant', 'Diagnosis Agent', '正在基于学生先验知识画像锚定理论强项与技能盲区…')
          this.addStep(node || 'Diagnosis Agent', ev.action || '学情诊断', 'running', null, '', '', 'diagnosis', '.diagnosis-panel')
          break
        case 'diagnosis_end':
          this.diagnosisResult = {
            knowledge_gaps: ev.knowledge_gaps || [],
            difficulty_target: ev.difficulty_target || '',
            priority_focus: ev.priority_focus || [],
            adaptation_strategy: ev.adaptation_strategy || ''
          }
          this.completeStep(node || 'Diagnosis Agent', 'completed', ev.durationMs,
            '难度:' + (ev.difficulty_target || 'N/A') + ' 盲区:' + (ev.knowledge_gaps || []).length + '个')
          if (node) { this.activeNodes.delete(node); this.highlightActive() }
          this.addMessage('assistant', 'Diagnosis Agent',
            '学情诊断完成：推荐难度「' + (ev.difficulty_target || '进阶') + '」，识别 ' +
            (ev.knowledge_gaps || []).length + ' 个知识盲区')
          var self = this
          this.$nextTick(function () { self.renderMatchCurve() })
          break
        case 'agent_start':
          if (node) { this.activeNodes.add(node); this.highlightActive() }
          this.addStep(node || ev.agent, ev.action || '执行任务', 'running', null, ev.inputSummary || '', '', ev.agent_key || '', '')
          break
        case 'agent_end':
          this.completeStep(node || ev.agent, ev.status || 'completed', ev.durationMs, ev.outputSummary || '')
          if (ev.agent_key && ev.output) {
            this.$set(this.agentOutputs, ev.agent_key, ev.output)
          }
          if (node) { this.activeNodes.delete(node); this.highlightActive() }
          this.addMessage('assistant', ev.agent || node,
            (ev.action || '执行') + ' ' + (ev.status === 'failed' ? '失败' : '完成') + '（' + (ev.durationMs || 0) + 'ms）')
          break
        // ── 辩论与交叉验证事件（PDF 创新性核心）──
        case 'debate_start':
          if (node) { this.activeNodes.add(node); this.highlightActive() }
          this.addMessage('assistant', 'Debate Agent', '正在对多 Agent 产出进行交叉比对、事实核验、幻觉识别与辩论裁决…')
          this.addStep(node || 'Debate Agent', ev.action || '辩论与交叉验证', 'running', null, '', '', 'debate', '.debate-panel')
          break
        case 'debate_end':
          this.debateResult = {
            contradictions_count: ev.contradictions_count || 0,
            hallucinations_count: ev.hallucinations_count || 0,
            hallucination_rate: ev.hallucination_rate || 0,
            verified_count: ev.verified_count || 0,
            total_facts: ev.total_facts || 0,
            hallucinations: ev.hallucinations || [],
            contradictions: ev.contradictions || [],
            kb_evidence: ev.kb_evidence || [],
            hallucination_prevented: ev.hallucination_prevented || false,
            debate_summary: ev.debate_summary || ''
          }
          this.completeStep(node || 'Debate Agent', 'completed', ev.durationMs,
            '幻觉率:' + ((ev.hallucination_rate || 0) * 100).toFixed(1) + '%')
          if (node) { this.activeNodes.delete(node); this.highlightActive() }
          var hallucRate = ((ev.hallucination_rate || 0) * 100).toFixed(1)
          this.addMessage('assistant', 'Debate Agent',
            '辩论与交叉验证完成：发现 ' + (ev.contradictions_count || 0) + ' 处矛盾、' +
            (ev.hallucinations_count || 0) + ' 个幻觉，幻觉率 ' + hallucRate + '%（目标 <5%）')
          break
        case 'review_start':
          if (node) { this.activeNodes.add(node); this.highlightActive() }
          this.addMessage('assistant', 'Quality Review Agent', '正在对全部生成资源做多维度质量评估…')
          this.addStep(node || 'Quality Review Agent', '资源质量多维度评估', 'running', null, '', '', 'review', '.review-panel')
          break
        case 'review_end':
          this.reviewScores = ev.scores || {}
          this.reviewOverall = ev.overall
          this.completeStep('Quality Review Agent', 'completed', ev.durationMs, '总体 ' + ev.overall + '/10')
          if (node) { this.activeNodes.delete(node); this.highlightActive() }
          var issueCount = (ev.issues || []).length
          this.addMessage('assistant', 'Quality Review Agent',
            '质量评估完成：总体 ' + ev.overall + '/10' + (issueCount ? '，发现 ' + issueCount + ' 个问题' : '，未发现明显问题'))
          break
        // ── 动态迭代决策事件（PDF 反馈机制）──
        case 'adapt_start':
          if (node) { this.activeNodes.add(node); this.highlightActive() }
          this.addMessage('assistant', 'Adapt Agent', '正在基于学情诊断、质量评估、辩论验证与学习反馈进行动态迭代决策…')
          this.addStep(node || 'Adapt Agent', ev.action || '动态迭代决策', 'running', null, '', '', 'adapt', '.adapt-panel')
          break
        case 'adapt_end':
          this.adaptResult = {
            decision: ev.decision || '',
            reason: ev.reason || '',
            actions_count: ev.actions_count || 0,
            learning_feedback: ev.learning_feedback || '',
            next_iteration_needed: ev.next_iteration_needed || false
          }
          this.completeStep(node || 'Adapt Agent', 'completed', ev.durationMs, '决策:' + (ev.decision || 'N/A'))
          if (node) { this.activeNodes.delete(node); this.highlightActive() }
          var decisionLabel = this.decisionLabel(ev.decision)
          this.addMessage('assistant', 'Adapt Agent',
            '动态迭代决策完成：' + decisionLabel + '（' + (ev.reason || '') + '）')
          break
        case 'retry':
          this.addStep('Supervisor', ev.action || '质量审查触发重试', 'completed', null, '', '', 'supervisor', '.result-panel')
          this.addMessage('assistant', 'Supervisor', ev.action || '质量审查发现关键问题，触发重试')
          break
        case 'final':
          this.finalReply = ev.reply || ''
          this.showFinal = true
          // 接收各 Agent 的完整生成产出（字典）
          if (ev.outputs && typeof ev.outputs === 'object' && !Array.isArray(ev.outputs)) {
            this.agentOutputs = ev.outputs
            // 完成时若正停留在 mindmap/video 等需要主动渲染的 tab，补一次渲染
            var selfF = this
            this.$nextTick(function () { selfF.refreshCollabOutput() })
          }
          // 同时接收 final 事件携带的 diagnosis/debate/adapt 结构化结果
          if (ev.diagnosis) this.diagnosisResult = this.diagnosisResult || ev.diagnosis
          if (ev.debate) this.debateResult = this.debateResult || ev.debate
          if (ev.adapt) this.adaptResult = this.adaptResult || ev.adapt
          var outputCount = Object.keys(this.agentOutputs).length
          this.addMessage('assistant', 'Supervisor',
            '协同完成：共生成 ' + outputCount + ' 项资源' +
            (this.reviewOverall != null ? '，总体质量 ' + this.reviewOverall + '/10' : '') +
            (this.debateResult ? '，幻觉率 ' + ((this.debateResult.hallucination_rate || 0) * 100).toFixed(1) + '%' : '') +
            '。详见下方结果。')
          break
        case 'error':
          this.addMessage('assistant', 'System', '错误：' + (ev.message || '未知错误'))
          break
      }
    },

    decisionLabel (decision) {
      return {
        simplify: '降维解释',
        challenge: '进阶挑战',
        correct: '内容修正',
        maintain: '维持当前资源'
      }[decision] || decision || '未知'
    },

    agentLabel (key) {
      return {
        mindmap: 'MindMap Agent',
        document: 'Doc Agent',
        reading: 'Reading Agent',
        code: 'Code Agent',
        guide: 'Guide Agent',
        quiz: 'Quiz Agent',
        video: 'Video Agent',
        review: 'Quality Review Agent',
        supervisor: 'Supervisor',
        diagnosis: 'Diagnosis Agent',
        debate: 'Debate Agent',
        adapt: 'Adapt Agent'
      }[key] || key || '未知'
    },

    severityLabel (sev) {
      return { critical: '严重', major: '较重', minor: '轻微' }[sev] || sev || '轻微'
    },

    // 资源难度匹配曲线：学习者掌握度 vs 多智能体适配后的资源难度（PDF 匹配度报告）
    renderMatchCurve () {
      var el = this.$refs.matchCurve
      if (!el || !this.diagnosisResult) return
      var gaps = this.diagnosisResult.knowledge_gaps || []
      if (!gaps.length) return
      if (!this._curveChart) this._curveChart = echarts.init(el, chartTheme())
      var chart = this._curveChart

      var labels = []
      var mastery = []
      var resourceDiff = []
      var baseMap = { '基础': 55, '进阶': 75, '高级': 90 }
      var target = baseMap[this.diagnosisResult.difficulty_target] || 75
      gaps.forEach(function (g) {
        labels.push(g.point && g.point.length > 8 ? g.point.slice(0, 8) + '…' : (g.point || g.subject))
        mastery.push(g.score != null ? Math.max(0, Math.min(100, Number(g.score))) : 60)
        // 适配规则：高严重度盲区降维(-12)，中严重度贴合目标，低严重度适度进阶(+8)
        var adj = g.severity === 'high' ? -12 : (g.severity === 'medium' ? 0 : 8)
        resourceDiff.push(Math.max(20, Math.min(100, target + adj)))
      })

      chart.setOption({
        backgroundColor: 'transparent',
        grid: { left: 42, right: 16, top: 34, bottom: 52 },
        tooltip: { trigger: 'axis' },
        legend: { data: ['学习者掌握度', '适配后资源难度'], textStyle: { color: tc('grey'), fontSize: 11 }, top: 2 },
        xAxis: {
          type: 'category', data: labels,
          axisLabel: { color: tc('grey'), fontSize: 10, interval: 0, rotate: 24 },
          axisLine: { lineStyle: { color: tc('grey2') } }
        },
        yAxis: {
          type: 'value', min: 0, max: 100, name: '分值',
          nameTextStyle: { color: tc('grey'), fontSize: 10 },
          axisLabel: { color: tc('grey'), fontSize: 10 },
          splitLine: { lineStyle: { color: tc('paper'), type: 'dashed' } }
        },
        series: [
          {
            name: '学习者掌握度', type: 'line', data: mastery, smooth: true,
            lineStyle: { width: 3, color: tc('acid') },
            itemStyle: { color: tc('acid') },
            areaStyle: { color: tca('acid', 0.08) },
            markLine: {
              silent: true, symbol: 'none',
              data: [{ yAxis: 60, label: { formatter: '及格线 60', position: 'insideEndTop', fontSize: 10, color: '#f59e0b' }, lineStyle: { color: '#f59e0b', type: 'dotted' } }]
            }
          },
          {
            name: '适配后资源难度', type: 'line', data: resourceDiff, smooth: true,
            lineStyle: { width: 3, color: tc('acid'), type: 'dashed' },
            itemStyle: { color: tc('acid') }
          }
        ]
      })
    },

    addStep (agent, action, status, dur, input, output, agentKey, panelSelector) {
      this.steps.push({
        agentName: agent,
        action: action,
        status: status,
        durationMs: dur || 0,
        inputSummary: input || '',
        outputSummary: output || '',
        agentKey: agentKey || '',
        panelSelector: panelSelector || ''
      })
    },
    completeStep (agent, status, dur, output) {
      for (var i = this.steps.length - 1; i >= 0; i--) {
        if (this.steps[i].agentName === agent && this.steps[i].status === 'running') {
          this.steps[i].status = status
          this.steps[i].durationMs = dur || 0
          this.steps[i].outputSummary = output || ''
          break
        }
      }
    },
    jumpToAgent (step) {
      if (!step || !step.agentKey) return
      // 资源生成 Agent：切换到该 Agent 的输出 Tab 并滚动到结果区
      if (this.agentOutputs[step.agentKey]) {
        this.activeOutputTab = step.agentKey
        this.showFinal = true
        var self = this
        this.$nextTick(function () {
          if (self.$refs.resultPanel) {
            self.$refs.resultPanel.scrollIntoView({ behavior: 'smooth', block: 'start' })
          }
        })
        return
      }
      // 诊断/辩论/审核/决策等 Agent：滚动到对应报告面板
      if (step.panelSelector) {
        var el = document.querySelector(step.panelSelector)
        if (el) {
          el.scrollIntoView({ behavior: 'smooth', block: 'start' })
        }
      }
    },

    // ═══════════════ 富展示：与多智能体生成页保持一致 ═══════════════
    switchOutputTab (key) {
      this.activeOutputTab = key
      // v-else-if 面板需要等 DOM 重新渲染后 $refs 才存在，否则 mindmap 容器为 null 直接 return
      var self = this
      this.$nextTick(function () { self.refreshCollabOutput() })
    },
    refreshCollabOutput () {
      var key = this.activeOutputTab
      if (!this.agentOutputs[key]) return
      if (key === 'mindmap') this.renderCollabMindMap()
      else if (key === 'quiz') this.parseCollabQuiz()
      else if (key === 'code') this.parseCollabCode()
      else if (key === 'video') this.parseCollabVideo()
    },

    // ── 思维导图 SVG（与多智能体生成页一致）──
    renderCollabMindMap: function () {
      var container = this.$refs.collabMmContainer
      if (!container) return
      var content = this.agentOutputs[this.activeOutputTab] || ''
      if (!content.trim()) {
        container.innerHTML = '<div class="output-placeholder">暂无思维导图内容</div>'
        return
      }
      container.innerHTML = '<div style="padding:60px;text-align:center;color:#86868b"><i class="el-icon-loading" style="font-size:28px"></i><p style="margin-top:12px">正在渲染思维导图...</p></div>'
      var self = this
      setTimeout(function () {
        try {
          var tree = self.mmParse(content)
          if (!tree) {
            container.innerHTML = '<p style="padding:60px;color:#FF3B30;text-align:center;font-size:14px">无法解析思维导图内容，请尝试重新生成</p>'
            return
          }
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
          var el = function (tag, attrs) {
            var e = document.createElementNS(NS, tag)
            for (var k in attrs) { if (attrs.hasOwnProperty(k)) e.setAttribute(k, attrs[k]) }
            return e
          }
          var svg = el('svg', {
            viewBox: '0 0 ' + vbW + ' ' + vbH,
            width: '100%',
            height: '100%',
            preserveAspectRatio: 'xMidYMid meet',
            style: 'display:block;width:100%;height:100%'
          })
          var defs = el('defs', {})
          var flt = el('filter', { id: 'mm-shadow', x: '-15%', y: '-15%', width: '135%', height: '140%' })
          flt.appendChild(el('feDropShadow', { dx: '0', dy: '2', stdDeviation: '6', 'flood-color': '#000', 'flood-opacity': '0.08' }))
          defs.appendChild(flt)
          var fltRoot = el('filter', { id: 'mm-shadow-root', x: '-15%', y: '-15%', width: '135%', height: '140%' })
          fltRoot.appendChild(el('feDropShadow', { dx: '0', dy: '4', stdDeviation: '10', 'flood-color': '#007AFF', 'flood-opacity': '0.15' }))
          defs.appendChild(fltRoot)
          var mmCols = self.mmColors()
          for (var mi = 0; mi < mmCols.length; mi++) {
            var marker = el('marker', {
              id: 'mm-arrow-' + mi,
              viewBox: '0 0 10 10',
              refX: '9',
              refY: '5',
              markerWidth: '7',
              markerHeight: '7',
              orient: 'auto-start-reverse'
            })
            marker.appendChild(el('path', { d: 'M 0 0 L 10 5 L 0 10 L 3 5 z', fill: mmCols[mi].stroke, opacity: '0.7' }))
            defs.appendChild(marker)
          }
          svg.appendChild(defs)
          svg.appendChild(el('rect', { x: 0, y: 0, width: vbW, height: vbH, fill: '#fbfbfd' }))
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
              d: d,
              fill: 'none',
              stroke: col.stroke,
              'stroke-width': edge.d === 0 ? '2' : '1.5',
              opacity: '0.45',
              'stroke-linecap': 'round',
              'marker-end': 'url(#mm-arrow-' + edgeIdx + ')',
              class: 'mm-edge-flow',
              'stroke-dasharray': '6 4'
            }))
          }
          svg.appendChild(eg)
          var ng = el('g', { class: 'mm-nodes' })
          for (var ni2 = 0; ni2 < nodes.length; ni2++) {
            var node = nodes[ni2]
            var ly = node._ly
            var nc = self.mmGetColor(ly.d)
            var fs = self.mmFontSize(ly.d)
            var fw = self.mmFontWeight(ly.d)
            var isRoot = ly.d === 0
            var g = el('g', { class: 'mm-node', 'data-id': node.id, cursor: 'move' })
            var bgRect = el('rect', {
              x: ly.x,
              y: ly.y,
              width: ly.w,
              height: ly.h,
              rx: isRoot ? '14' : '10',
              fill: '#ffffff',
              stroke: nc.stroke,
              'stroke-width': isRoot ? '2' : '1.2',
              opacity: '1',
              filter: isRoot ? 'url(#mm-shadow-root)' : 'url(#mm-shadow)'
            })
            g.appendChild(bgRect)
            g.appendChild(el('rect', { x: ly.x, y: ly.y + 4, width: 3, height: ly.h - 8, rx: 1.5, fill: nc.stroke, opacity: '0.8' }))
            var textFill = isRoot ? nc.stroke : '#1d1d1f'
            var text = el('text', {
              x: ly.cx,
              y: ly.cy + fs * 0.35,
              fill: textFill,
              'font-size': fs,
              'font-weight': fw,
              'font-family': "'-apple-system','PingFang SC','Microsoft YaHei','SF Pro Display',sans-serif",
              'text-anchor': 'middle',
              'pointer-events': 'none'
            })
            text.textContent = node.title
            g.appendChild(text)
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
          self._mmBindInteractions(svg)
          self._mmFitView(svg, vbW, vbH)
        } catch (e) {
          console.error('SVG渲染失败:', e)
          container.innerHTML = '<p style="padding:60px;color:#FF3B30;text-align:center">渲染失败：' + (e.message || '未知错误') + '</p>'
        }
      }, 100)
    },

    mmColors: function () {
      return [
        { fill: 'rgba(0, 122, 255, 0.08)', stroke: '#007AFF' },
        { fill: 'rgba(88, 86, 214, 0.07)', stroke: '#5856D6' },
        { fill: 'rgba(52, 199, 89, 0.07)', stroke: '#34C759' },
        { fill: 'rgba(255, 149, 0, 0.07)', stroke: '#FF9500' },
        { fill: 'rgba(255, 59, 48, 0.07)', stroke: '#FF3B30' },
        { fill: 'rgba(175, 82, 222, 0.07)', stroke: '#AF52DE' },
        { fill: 'rgba(90, 200, 250, 0.07)', stroke: '#5AC8FA' },
        { fill: 'rgba(172, 142, 104, 0.07)', stroke: '#AC8E68' }
      ]
    },
    mmGetColor: function (d) {
      var cols = this.mmColors()
      return cols[Math.min(d, cols.length - 1)]
    },
    mmTextW: function (text, fs) {
      if (!text) return 80
      var w = 0
      for (var i = 0; i < text.length; i++) {
        var c = text.charCodeAt(i)
        if (c > 0x4e00) w += fs * 1.05
        else if (c > 0xff) w += fs * 0.8
        else if (c > 0x7f) w += fs * 0.7
        else w += fs * 0.6
      }
      return Math.ceil(w + 44)
    },
    mmFontSize: function (d) {
      return [17, 14, 12, 11, 10, 10, 10][Math.min(d, 6)]
    },
    mmFontWeight: function (d) {
      return d === 0 ? 700 : d === 1 ? 600 : 500
    },

    mmParse: function (content) {
      content = (content || '').trim()
      if (!content) return null
      var fenced = content.match(/```(?:mermaid|json)?\s*\n?([\s\S]*?)\n?\s*```/)
      if (fenced) { content = fenced[1].trim() }
      try { return this.mmNorm(JSON.parse(content)) } catch (e) {}
      var firstBrace = content.indexOf('{')
      var lastBrace = content.lastIndexOf('}')
      if (firstBrace >= 0 && lastBrace > firstBrace) {
        var jsonStr = content.substring(firstBrace, lastBrace + 1)
        var fixed = jsonStr.replace(/,\s*}/g, '}').replace(/,\s*]/g, ']')
        try { return this.mmNorm(JSON.parse(fixed)) } catch (e2) {}
        try { return this.mmNorm(JSON.parse(jsonStr)) } catch (e3) {}
      }
      var ctIdx = content.lastIndexOf('- 中心主题')
      if (ctIdx < 0) ctIdx = content.lastIndexOf('-中心主题')
      if (ctIdx > 0) content = content.substring(ctIdx)
      content = content.replace(/([^\n])(-[ ]*[一二三四五六七八九十][、])/g, '$1\n  $2')
      content = content.replace(/(-[ ]*中心主题[：:][^\n]+)(\n?)/, '$1\n')
      content = content.replace(/^(\s{2}-[^\n]+?)(\s{2,4}-[ ]+)/gm, '$1\n    $2')
      content = content.replace(/\n{3,}/g, '\n\n')
      var lines = content.split('\n')
      var root = { title: this.topic || '思维导图', children: [], id: 'r' }
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
      var listStack = [{ node: root, indent: -1 }]
      var hasListItems = false
      for (var li = 0; li < lines.length; li++) {
        var l = lines[li]
        var trimmed = l.trim()
        if (!trimmed || trimmed.startsWith('```')) continue
        var listMatch = trimmed.match(/^[-*]\s+(.+)/)
        if (listMatch) {
          hasListItems = true
          var indent = l.search(/\S/)
          var text = listMatch[1].replace(/\*\*/g, '').replace(/[🔑⚠️💡🎯⭐🔥]/g, '').trim()
          if (!text) continue
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
      var fallback = { title: this.topic || '思维导图', children: [], id: 'r' }
      var paragraphs = content.split(/\n\n+/).filter(function (p) { return p.trim().length > 5 })
      if (paragraphs.length >= 3) {
        for (var j = 0; j < Math.min(paragraphs.length, 7); j++) {
          var pText = paragraphs[j].trim()
          var pLines = pText.split('\n').filter(function (l) { return l.trim().length > 0 })
          var pTitle = pLines[0].replace(/^[#*\-\d.]+\s*/, '').trim().substring(0, 40)
          var level2 = { title: pTitle, children: [], id: 'p' + j }
          var subItems = []
          for (var k = 1; k < pLines.length; k++) {
            var sl = pLines[k].replace(/^[#*\-\d.]+\s*/, '').trim()
            if (sl && sl.length > 3) subItems.push(sl.substring(0, 50))
          }
          if (subItems.length < 3) {
            var mainText = pLines.length > 1 ? pLines.slice(1).join(' ') : pText
            var fragments = mainText.split(/[，,。；;、\s]{2,}/).filter(function (f) { return f.length > 4 })
            for (var fi = 0; fi < Math.min(fragments.length, 6); fi++) subItems.push(fragments[fi].trim().substring(0, 40))
          }
          for (var si = 0; si < Math.min(subItems.length, 5); si++) {
            var item2 = subItems[si]
            var l3 = { title: item2, children: [], id: 'p' + j + 's' + si }
            var detailTerms = item2.split(/[，,、\s]+/).filter(function (t) { return t.length > 1 })
            if (detailTerms.length >= 2) {
              for (var di = 0; di < Math.min(detailTerms.length, 4); di++) {
                if (detailTerms[di].length > 1) l3.children.push({ title: detailTerms[di], children: [], id: 'p' + j + 's' + si + 'd' + di })
              }
            }
            level2.children.push(l3)
          }
          fallback.children.push(level2)
        }
      } else {
        for (var jj = 0; jj < lines.length; jj++) {
          var fl = lines[jj].trim()
          if (fl && fl.length > 2 && !fl.startsWith('```')) {
            var node = { title: fl.substring(0, 60), children: [], id: 'f' + jj }
            var words = fl.split(/[，,、\s]+/).filter(function (w) { return w.length > 2 })
            for (var wi = 0; wi < Math.min(words.length, 3); wi++) {
              if (words[wi].length > 2) node.children.push({ title: words[wi], children: [], id: 'f' + jj + 'w' + wi })
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
        return { id: 'r', title: this.topic || '思维导图', children: n.map(function (c) { return this.mmNorm(c) }.bind(this)).filter(Boolean), collapsed: false }
      }
      var title = String(n.title || n.name || n.text || n.label || '节点').replace(/[*#]/g, '').trim()
      var children = (n.children || n.childNodes || n.nodes || n.items || n.subtopics || [])
      return {
        id: n.id || ('n_' + Math.random().toString(36).slice(2, 8)),
        title: title || '节点',
        children: children.map(function (c) { return this.mmNorm(c) }.bind(this)).filter(Boolean),
        collapsed: false
      }
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
        for (var i = 0; i < node.children.length; i++) this._mmShiftY(node.children[i], offset)
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
        this._mmShiftY(children[j], curY)
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
        dragNode._ly.x = dragStartLy.x + dx
        dragNode._ly.y = dragStartLy.y + dy
        dragNode._ly.cx = dragStartLy.cx + dx
        dragNode._ly.cy = dragStartLy.cy + dy
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
      svg.addEventListener('mouseup', function () { dragNode = null })
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
        if (found) self.renderCollabMindMap()
      })
      svg.addEventListener('dblclick', function (e) {
        var nodeEl = e.target.closest('.mm-node')
        if (!nodeEl) return
        var id = nodeEl.getAttribute('data-id')
        if (id) self._mmStartEdit(id)
      })
    },

    _mmStartEdit: function (nodeId) {
      var self = this
      if (!this.mmTree) return
      var layoutedNode = this._mmFindNode(nodeId, this.mmTree)
      if (!layoutedNode || !layoutedNode._ly) return
      var L = layoutedNode._ly
      var svg = this.$refs.collabMmContainer && this.$refs.collabMmContainer.querySelector('svg')
      if (!svg) return
      var NS = 'http://www.w3.org/2000/svg'
      var fo = document.createElementNS(NS, 'foreignObject')
      fo.setAttribute('x', L.x); fo.setAttribute('y', L.y)
      fo.setAttribute('width', Math.max(80, L.w)); fo.setAttribute('height', L.h)
      var input = document.createElement('input')
      input.type = 'text'; input.value = layoutedNode.title; input.className = 'mm-edit-input'
      fo.appendChild(input)
      svg.appendChild(fo)
      setTimeout(function () { input.focus(); input.select() }, 0)
      var commit = function () {
        var val = input.value.trim()
        if (val) {
          var target = self._mmFindNode(nodeId, self.mmTree)
          if (target) target.title = val
          self._mmSyncBody()
        }
        if (fo.parentNode) fo.parentNode.removeChild(fo)
        self.renderCollabMindMap()
      }
      var cancel = function () {
        if (fo.parentNode) fo.parentNode.removeChild(fo)
      }
      input.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') { e.preventDefault(); commit() } else if (e.key === 'Escape') { e.preventDefault(); cancel() }
        e.stopPropagation()
      })
      input.addEventListener('blur', commit)
    },

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

    _mmSyncBody: function () {
      try {
        if (!this.mmTree) return
        var clean = JSON.parse(JSON.stringify(this.mmTree))
        var strip = function (n) {
          delete n._ly; delete n._hidden
          if (n.children) n.children.forEach(strip)
        }
        strip(clean)
        this.$set(this.agentOutputs, 'mindmap', JSON.stringify(clean, null, 2))
      } catch (e) { /* ignore */ }
    },

    collabMmWheel: function (e) {
      var delta = e.deltaY > 0 ? -0.1 : 0.1
      this.collabMmZoom(delta)
    },
    collabMmZoom: function (d) {
      this.mmScale = Math.max(0.3, Math.min(3, +((this.mmScale || 1) + d).toFixed(2)))
      this._mmApplyTransform()
    },
    collabMmFit: function () {
      var c = this.$refs.collabMmContainer; if (!c) return
      var svg = c.querySelector('svg'); if (!svg) return
      var vb = svg.getAttribute('viewBox').split(' ')
      this._mmFitView(svg, parseFloat(vb[2]), parseFloat(vb[3]))
    },
    collabMmDownload: function () {
      var c = this.$refs.collabMmContainer; if (!c) return
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
    collabMmMouseDown: function (e) {
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
    _mmApplyTransform: function () {
      var c = this.$refs.collabMmContainer
      if (!c) return
      var svg = c.querySelector('svg')
      if (svg) {
        svg.style.transform = 'translate(' + (this.mmPanX || 0) + 'px,' + (this.mmPanY || 0) + 'px) scale(' + (this.mmScale || 1) + ')'
        svg.style.transformOrigin = 'center center'
        svg.style.transition = 'transform 0.2s ease-out'
      }
    },
    _mmFitView: function (svg, vbW, vbH) {
      var container = this.$refs.collabMmContainer
      if (!container || !svg) return
      var cw = container.clientWidth
      var ch = container.clientHeight
      if (!cw || !ch) {
        var self = this
        setTimeout(function () { self._mmFitView(svg, vbW, vbH) }, 200)
        return
      }
      var scaleW = (cw * 0.92) / vbW
      var scaleH = (ch * 0.92) / vbH
      var scale = Math.min(scaleW, scaleH, 1.5)
      if (scale < 0.3) scale = 0.3
      var panX = (cw - vbW * scale) / 2
      var panY = (ch - vbH * scale) / 2
      this.mmScale = scale
      this.mmPanX = panX
      this.mmPanY = panY
      this._mmApplyTransform()
    },

    // ── 练习题目解析 ──
    parseCollabQuiz: function () {
      var raw = this.agentOutputs[this.activeOutputTab] || ''
      var questions = []
      var blocks = raw.split(/\n\s*\n/)
      var qIdx = 0
      for (var b = 0; b < blocks.length; b++) {
        var block = blocks[b].trim()
        if (!block || block.length < 8) continue
        var lines = block.split('\n')
        var allPlain = []
        var opts = []
        var ansVal = ''
        var expText = ''
        for (var li = 0; li < lines.length; li++) {
          var line = lines[li].trim()
          if (!line) continue
          var om = line.match(/^([A-D])[.、)\s]+(.+)/)
          if (om) { opts.push({ letter: om[1], text: om[2].trim() }); continue }
          if (/答案[：:]\s*(.+)/.test(line)) {
            var am = line.match(/答案[：:]\s*(.+)/)
            ansVal = (am ? am[1] : '').trim()
            continue
          }
          if (/解析[：:]/.test(line) || /详解[：:]/.test(line)) {
            expText += (expText ? '\n' : '') + line.replace(/^[^：:]+[：:]\s*/, '').trim()
            continue
          }
          if (/^【?(选择题|单选题|多选题|判断题|填空题|简答题|计算题|问答题|论述题|名词解释题|案例分析题)】?([（(][^）)]{0,8}[）)])?[:：]?\s*$/.test(line)) continue
          allPlain.push(line)
        }
        var fullText = allPlain.join('\n').trim()
        if (!fullText) continue
        if (opts.length >= 2) {
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
          var tfAns = /错误|✗|错/.test(ansVal) ? 1 : 0
          if (!ansVal || !/正确|错误|对|错|✓|✗/.test(ansVal)) {
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
          var cleanQ = fullText
            .replace(/^\d+[.、)\s]+/, '')
            .replace(/_{2,}/g, '______')
            .replace(/\.{3,}/g, '______')
            .replace(/【\s*】/g, '【______】')
            .replace(/\(\s*\)/g, '(______)')
          var answerPart = ansVal
          var explainPart = expText
          if (ansVal && ansVal.length > 80) {
            var ansLines = ansVal.split(/\n/)
            answerPart = ansLines[0].trim()
            if (ansLines.length > 1) explainPart = ansLines.slice(1).join('\n').trim()
          }
          if (!answerPart || answerPart.length === 0) answerPart = '（详见解析）'
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
      this.collabQuizParsed = questions
      this.collabQuizAnswers = questions.map(function () { return null })
      this.collabQuizResults = []
      this.collabQuizSubmitted = false
    },
    collabQuizSubmit: function () {
      var self = this
      this.collabQuizResults = this.collabQuizParsed.map(function (q, i) {
        var userAns = self.collabQuizAnswers[i]
        if (userAns === null || userAns === undefined || userAns === '') return null
        if (q.type === 'choice') return parseInt(userAns) === q.answer
        if (q.type === 'tf') return parseInt(userAns) === q.answer
        if (q.type === 'fill' || q.type === 'short') {
          var ua = String(userAns).trim().toLowerCase()
          var ra = String(q.refAnswer).trim().toLowerCase()
          if (ua === ra) return true
          if (ua.length >= 3 && ra.includes(ua)) return true
          if (ra.length >= 3 && ua.includes(ra)) return true
          return false
        }
        return false
      })
      this.collabQuizSubmitted = true
    },
    collabQuizReset: function () {
      this.collabQuizAnswers = this.collabQuizParsed.map(function () { return null })
      this.collabQuizResults = []
      this.collabQuizSubmitted = false
    },

    // ── 代码块解析与运行 ──
    parseCollabCode: function () {
      var raw = this.agentOutputs[this.activeOutputTab] || ''
      var blocks = []
      var regex = /```(\w*)\n([\s\S]*?)```/g
      var match
      while ((match = regex.exec(raw)) !== null) {
        var lang = (match[1] || 'javascript').toLowerCase().trim()
        if (lang === 'js') lang = 'javascript'
        if (lang === 'py') lang = 'python'
        var code = match[2].trim()
        var runnable = (lang === 'html' || lang === 'javascript' || lang === 'css')
        blocks.push({ lang: lang, code: code, originalCode: code, runnable: runnable, running: false, output: null, exitOk: true })
      }
      if (blocks.length === 0 && raw.trim()) {
        var looksLikeCode = raw.includes('function ') || raw.includes('const ') || raw.includes('var ') ||
          raw.includes('<!DOCTYPE') || raw.includes('<html') || raw.includes('console.log') ||
          raw.includes('class ') || raw.includes('import ') || raw.includes('def ')
        if (looksLikeCode) {
          var detectedLang = raw.includes('<!DOCTYPE') || raw.includes('<html') ? 'html' : 'javascript'
          blocks.push({ lang: detectedLang, code: raw.trim(), originalCode: raw.trim(), runnable: (detectedLang === 'html' || detectedLang === 'javascript'), running: false, output: null, exitOk: true })
        }
      }
      this.collabCodeBlocks = blocks
      this.collabActiveCodeIdx = 0
    },
    collabRunCode: function (bi) {
      var block = this.collabCodeBlocks[bi]
      if (!block || block.running) return
      block.running = true
      block.output = null
      block.exitOk = true
      this.$forceUpdate()
      var code = block.code
      var lang = block.lang
      if (lang === 'html' || (code.includes('<html') && code.includes('</html>'))) {
        setTimeout(function () {
          block.output = 'HTML 页面已在下方预览区渲染，请查看 👇'
          block.exitOk = true
          this.$forceUpdate()
        }.bind(this), 300)
        return
      }
      if (lang === 'javascript' || lang === 'css' || lang === 'js') {
        try {
          var outputLines = []
          var _log = console.log
          var _warn = console.warn
          var _error = console.error
          var _info = console.info
          console.log = function () {
            outputLines.push('[log] ' + Array.prototype.slice.call(arguments).map(function (a) {
              return typeof a === 'object' ? JSON.stringify(a, null, 2) : String(a)
            }).join(' '))
          }
          console.warn = function () { outputLines.push('[warn] ' + Array.prototype.slice.call(arguments).join(' ')) }
          console.error = function () { outputLines.push('[error] ' + Array.prototype.slice.call(arguments).join(' ')) }
          console.info = function () { outputLines.push('[info] ' + Array.prototype.slice.call(arguments).join(' ')) }
          if (lang === 'css') {
            outputLines.push('[CSS代码] 共 ' + code.split('\n').length + ' 行，可在HTML中引用')
            outputLines.push('提示：将CSS与HTML代码合并到同一个代码块中可实现完整页面效果')
          } else {
            var wrappedCode = '(function(){\n' + code + '\n})()'
            // eslint-disable-next-line no-new-func
            var result = new Function(wrappedCode)()
            if (result !== undefined) {
              var resultDisplay = typeof result === 'object' ? JSON.stringify(result, null, 2) : String(result)
              outputLines.push('→ 返回值: ' + resultDisplay)
            }
          }
          console.log = _log; console.warn = _warn; console.error = _error; console.info = _info
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
      block.output = 'ℹ ' + lang + ' 代码暂不支持浏览器端直接运行。\n建议：\n  1. 复制代码到本地 ' + lang + ' 环境运行\n  2. 或点击「复制」按钮后粘贴到在线 ' + lang + ' 编译器'
      block.exitOk = true
      this.$forceUpdate()
    },
    collabCopyCode: function (code) {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(code).then(function () {
          this.$message.success('代码已复制到剪贴板')
        }.bind(this)).catch(function () {
          this.collabFallbackCopy(code)
        }.bind(this))
      } else {
        this.collabFallbackCopy(code)
      }
    },
    collabFallbackCopy: function (code) {
      var ta = document.createElement('textarea')
      ta.value = code
      ta.style.position = 'fixed'; ta.style.left = '-9999px'
      document.body.appendChild(ta)
      ta.focus(); ta.select()
      try { document.execCommand('copy'); this.$message.success('代码已复制') } catch (e) { this.$message.error('复制失败') }
      document.body.removeChild(ta)
    },

    // ── 视频脚本场景解析 ──
    parseCollabVideo: function () {
      var raw = this.agentOutputs[this.activeOutputTab] || ''
      var scenes = []
      var sections = raw.split(/\n##\s+/)
      if (sections.length <= 1) sections = raw.split(/\n###\s+/)
      if (sections.length <= 1) sections = raw.split(/\n#\s+/)
      // 容错：粗体分节「**场景一**」/「场景一：」行首形式
      if (sections.length <= 1) sections = raw.split(/\n(?=\*\*场景|\*\*第|场景[一二三四五六七八九十]+[：:])/)
      for (var i = 0; i < sections.length; i++) {
        var sec = sections[i].trim()
        if (!sec || sec.length < 10) continue
        var title = sec.split('\n')[0].replace(/^#+\s*/, '').trim().substring(0, 40)
        var visualMatch = sec.match(/【画面文字】\s*\n([\s\S]*?)(?=【语音讲解】|$)/)
        var voiceMatch = sec.match(/【语音讲解】\s*\n([\s\S]*?)(?=$)/)
        var visualItems = []
        var voiceText = ''
        if (visualMatch && visualMatch[1]) {
          var vlines = visualMatch[1].trim().split('\n')
          for (var j = 0; j < vlines.length; j++) {
            var l = vlines[j].replace(/^[-*•·]\s*/, '').replace(/\*\*/g, '').trim()
            if (l && l.length > 2 && !l.startsWith('【')) visualItems.push(l.length > 50 ? l.substring(0, 48) + '…' : l)
          }
        }
        if (voiceMatch && voiceMatch[1]) voiceText = voiceMatch[1].trim().replace(/\n/g, ' ').substring(0, 500)
        if (!visualItems.length) {
          var fallbackLines = sec.split('\n')
          for (var k = 1; k < Math.min(fallbackLines.length, 10); k++) {
            var fallbackLine = fallbackLines[k].replace(/^[-*•·]\s*/, '').replace(/\*\*/g, '').replace(/^【.*】\s*/, '').trim()
            if (fallbackLine && fallbackLine.length > 3 && !fallbackLine.startsWith('#')) visualItems.push(fallbackLine.substring(0, 80))
          }
        }
        if (!voiceText && visualItems.length) voiceText = title + '。' + visualItems.join('。')
        if (!visualItems.length) visualItems = [title]
        scenes.push({ title: title, visualItems: visualItems, items: visualItems.slice(), voiceText: voiceText })
      }
      this.collabVideoScenes = scenes.length >= 1 ? scenes : [{ title: this.topic || '教学视频', visualItems: ['请重新生成内容'], items: ['请重新生成内容'], voiceText: '请重新生成教学内容' }]
      this.collabVideoSceneIdx = 0
      this.collabVideoProgress = 0
      this.collabStopVideoTts()
    },
    // ── Agnes 真视频：提交 → 轮询 → 成片播放 ──
    collabGenerateAgnesVideo: async function () {
      if (this.collabAgnesSubmitting) return
      if (!this.topic || !this.topic.trim()) { this.$message.warning('请先输入教学主题'); return }
      this.collabResetAgnes()
      this.collabAgnesSubmitting = true
      try {
        var res = await resourceApi.generateVideoAgnes({ topic: this.topic.trim(), duration: this.collabAgnesDuration })
        if (res && res.code === 1 && res.response && res.response.taskId) {
          this.collabAgnesTaskId = res.response.taskId
          this.collabAgnesStatus = res.response.status || 'submitted'
          this.$message.success('已提交 Agnes 视频渲染任务，出片后自动嵌入播放')
          this.collabPollAgnesStatus()
        } else {
          this.$message.error((res && (res.message || (res.response && res.response.error))) || 'Agnes 任务提交失败')
        }
      } catch (e) {
        this.$message.error('Agnes 任务提交失败: ' + (e.message || '网络错误'))
      } finally {
        this.collabAgnesSubmitting = false
      }
    },
    collabPollAgnesStatus: function () {
      var self = this
      var tries = 0
      var maxTries = 60 // 8s × 60 ≈ 8 分钟上限
      var timerName = 'collabAgnesPollTimer'
      var clear = function () { if (self[timerName]) { clearTimeout(self[timerName]); self[timerName] = null } }
      clear()
      var tick = async function () {
        if (!self.collabAgnesTaskId) return
        tries += 1
        if (tries > maxTries) { self.collabAgnesStatus = 'timeout'; clear(); self.$message.warning('Agnes 渲染超时，可稍后重新生成'); return }
        try {
          var res = await resourceApi.queryVideoStatusAgnes(self.collabAgnesTaskId)
          var st = (res && res.response && res.response.status) || 'unknown'
          self.collabAgnesStatus = st
          var url = res && res.response && res.response.videoUrl
          if (url) {
            self.collabAgnesVideoUrl = url
            self.collabAgnesStatus = 'done'
            clear()
            self.$message.success('AI 真视频已生成完成')
            return
          }
          if (st === 'failed' || st === 'error') { clear(); self.$message.error('Agnes 渲染失败: ' + ((res.response && res.response.error) || '未知错误')); return }
        } catch (e) { /* 单次轮询失败不打断 */ }
        self[timerName] = setTimeout(tick, 8000)
      }
      tick()
    },
    collabResetAgnes: function () {
      if (this.collabAgnesPollTimer) { clearTimeout(this.collabAgnesPollTimer); this.collabAgnesPollTimer = null }
      this.collabAgnesTaskId = ''
      this.collabAgnesStatus = ''
      this.collabAgnesVideoUrl = ''
    },
    collabVideoPlayFromStart: function () {
      this.collabVideoSceneIdx = 0
      this.collabVideoProgress = 0
      this.collabStopVideoTts()
      if (this.collabVideoTtsOn) this.collabStartVideoNarration()
    },
    collabVideoSeek: function (e) {
      var rect = e.currentTarget.getBoundingClientRect()
      var pct = (e.clientX - rect.left) / rect.width
      var idx = Math.floor(pct * this.collabVideoScenes.length)
      this.collabVideoSceneIdx = Math.min(idx, this.collabVideoScenes.length - 1)
      this.collabVideoProgress = Math.round((this.collabVideoSceneIdx / this.collabVideoScenes.length) * 100)
    },
    collabStartVideoNarration: function () {
      if (!this.collabVideoScenes.length || !this.collabVideoTtsOn) return
      this.collabStopVideoTts()
      var self = this
      this.collabSpeakScene(0)
      this.collabVideoTimer = setInterval(function () {
        var next = self.collabVideoSceneIdx + 1
        if (next >= self.collabVideoScenes.length) { self.collabStopVideoTts(); return }
        self.collabVideoSceneIdx = next
        self.collabVideoProgress = Math.round((next / self.collabVideoScenes.length) * 100)
        self.collabSpeakScene(next)
      }, 8000)
    },
    collabSpeakScene: function (idx) {
      if (!this.collabVideoTtsOn || idx >= this.collabVideoScenes.length) return
      var scene = this.collabVideoScenes[idx]
      var text = scene.title + '。' + (scene.items || scene.visualItems || []).join('。')
      this.collabTtsEnqueue(text)
    },
    collabTtsEnqueue: function (text) {
      if (!window.speechSynthesis || !this.collabVideoTtsOn) return
      var clean = text.replace(/<[^>]+>/g, '').replace(/[#*`\-|>]/g, '').trim()
      if (clean.length < 3) return
      this.collabVideoTtsQueue.push(clean)
      if (!this.collabVideoTtsSpeaking) this.collabTtsProcess()
    },
    collabTtsProcess: function () {
      if (!this.collabVideoTtsOn || !this.collabVideoTtsQueue.length) { this.collabVideoTtsSpeaking = false; return }
      this.collabVideoTtsSpeaking = true
      var text = this.collabVideoTtsQueue.shift()
      if (this.collabVideoTtsQueue.length > 15) this.collabVideoTtsQueue = this.collabVideoTtsQueue.slice(-8)
      try {
        var u = new SpeechSynthesisUtterance(text)
        u.lang = 'zh-CN'; u.rate = 1.15; u.pitch = 1.05; u.volume = 0.9
        var voices = window.speechSynthesis.getVoices()
        var voice = voices.find(function (v) { return v.lang.startsWith('zh') && v.name.includes('Tingting') }) ||
          voices.find(function (v) { return v.lang.startsWith('zh') && v.name.includes('Meijia') }) ||
          voices.find(function (v) { return v.lang.startsWith('zh-CN') })
        if (voice) u.voice = voice
        var self = this
        u.onend = function () { self.collabTtsProcess() }
        u.onerror = function () { self.collabTtsProcess() }
        window.speechSynthesis.speak(u)
      } catch (e) { this.collabTtsProcess() }
    },
    collabStopVideoTts: function () {
      if (this.collabVideoTimer) { clearInterval(this.collabVideoTimer); this.collabVideoTimer = null }
      if (window.speechSynthesis) window.speechSynthesis.cancel()
      this.collabVideoTtsQueue = []
      this.collabVideoTtsSpeaking = false
    },
    addMessage (role, agent, content) {
      this.messages.push({ role: role, agentName: agent, content: content })
      this.scrollToBottom()
    },

    highlightActive () {
      if (!this.chart) return
      var option = this.chart.getOption()
      var active = this.activeNodes
      var nodes = option.series[0].data.map(function (n) {
        var copy = Object.assign({}, n)
        var baseSize = (n.name === 'Supervisor') ? 70 : (n.category === 2 ? 32 : 50)
        if (active.has(n.name)) {
          copy.symbolSize = baseSize + 16
          copy.itemStyle = Object.assign({}, copy.itemStyle || {}, {
            shadowBlur: 28,
            shadowColor: n.name === 'Supervisor' ? tca('acid', 0.85) : tca('acid', 0.85),
            borderColor: '#fff',
            borderWidth: 3
          })
        } else {
          copy.symbolSize = baseSize
          copy.itemStyle = Object.assign({}, copy.itemStyle || {}, { shadowBlur: 0, borderWidth: 0 })
        }
        return copy
      })
      var links = option.series[0].links.map(function (l) {
        var copy = Object.assign({}, l)
        var connected = active.has(l.source) || active.has(l.target)
        copy.lineStyle = connected
          ? { color: tc('acid'), width: 3, opacity: 0.9, curveness: 0.1 }
          : { color: tca('acid', 0.2), width: 1.5, opacity: 0.5, curveness: 0.1 }
        return copy
      })
      this.chart.setOption({ series: [{ data: nodes, links: links }] })
    },
    highlightNode (nodeName, active) {
      if (active) this.activeNodes.add(nodeName)
      else this.activeNodes.delete(nodeName)
      this.highlightActive()
    },
    resetAllNodes () {
      this.activeNodes = new Set()
      this.highlightActive()
    },

    scoreLabel (k) {
      return {
        accuracy: '准确性',
        difficulty_fit: '难度适配',
        completeness: '完整性',
        cross_modal_consistency: '跨模态一致性'
      }[k] || k
    },
    scoreClass (v) {
      if (v >= 8) return 'good'
      if (v >= 6) return 'ok'
      return 'bad'
    },
    scoreColor (v) {
      if (v >= 8) return 'linear-gradient(90deg,#10b981,#059669)'
      if (v >= 6) return 'linear-gradient(90deg,#f59e0b,#d97706)'
      return 'linear-gradient(90deg,#ef4444,#dc2626)'
    },

    scrollToBottom () {
      var self = this
      this.$nextTick(function () {
        if (self.$refs.chatList) {
          self.$refs.chatList.scrollTop = self.$refs.chatList.scrollHeight
        }
      })
    },
    handleResize () {
      if (this.chart) this.chart.resize()
    }
  }
}
</script>

<style scoped>
.collab-container {
  min-height: 100vh;
  background: var(--ink-3);
  padding: 30px;
  color: #1e293b;
}

.collab-header { text-align: center; margin-bottom: 24px; }
.collab-header h2 {
  font-size: 28px; font-weight: 700; margin: 0 0 8px;
  background: linear-gradient(135deg, var(--acid), var(--ice), var(--acid));
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.collab-header p { font-size: 14px; color: #94a3b8; margin: 0; }

.panel-title {
  display: flex; align-items: center; gap: 8px;
  font-size: 15px; font-weight: 600; color: var(--acid);
  margin-bottom: 16px; padding-bottom: 12px;
  border-bottom: 1px solid rgba(var(--acid-rgb), 0.1);
}
.panel-title i { font-size: 18px; color: var(--acid); }
.step-count { margin-left: auto; font-size: 12px; color: #64748b; font-weight: 400; }
.overall-badge {
  margin-left: auto; padding: 3px 12px; border-radius: 100px;
  background: linear-gradient(135deg,var(--acid),var(--ice)); color: #fff; font-size: 13px; font-weight: 600;
}

.collab-main { display: flex; gap: 20px; margin-bottom: 24px; }
.graph-panel {
  flex: 0 0 58%; background: #fff; border: 1px solid #e2e8f0;
  border-radius: 14px; padding: 20px;
}
.chat-panel {
  flex: 1; display: flex; flex-direction: column;
  background: #fff; border: 1px solid #e2e8f0; border-radius: 14px; padding: 20px;
}
.chat-messages {
  flex: 1; overflow-y: auto; padding: 10px; margin-bottom: 14px;
  max-height: 400px; min-height: 320px;
}
.chat-messages::-webkit-scrollbar { width: 4px; }
.chat-messages::-webkit-scrollbar-thumb { background: rgba(var(--acid-rgb), 0.3); border-radius: 2px; }

.msg { display: flex; gap: 10px; margin-bottom: 14px; animation: fadeIn 0.3s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
.msg-avatar {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 700; color: #fff; flex-shrink: 0;
}
.msg.assistant .msg-avatar { background: linear-gradient(135deg, var(--acid), var(--ice)); }
.msg-content { max-width: 80%; }
.msg-name { font-size: 11px; color: #64748b; margin-bottom: 4px; }
.msg-body {
  padding: 10px 14px; border-radius: 12px;
  font-size: 13px; line-height: 1.7; word-break: break-word; white-space: pre-wrap;
  background: rgba(var(--acid-rgb), 0.06);
  border: 1px solid rgba(var(--acid-rgb), 0.08); color: #1e293b;
}

.chat-input { display: flex; gap: 10px; }
.chat-input .el-input { flex: 1; }
.chat-input .el-button--primary {
  background: linear-gradient(135deg, var(--acid), var(--ice));
  border: none; border-radius: 10px; box-shadow: 0 4px 15px rgba(var(--acid-rgb), 0.3);
}

.review-panel {
  background: #fff; border: 1px solid #e2e8f0; border-radius: 14px;
  padding: 20px; margin-bottom: 24px;
}
.score-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.score-card {
  background: var(--ink-3); border: 1px solid #e2e8f0; border-radius: 12px; padding: 16px; text-align: center;
}
.score-label { font-size: 13px; color: #64748b; margin-bottom: 8px; }
.score-value { font-size: 28px; font-weight: 700; line-height: 1; }
.score-value span { font-size: 14px; color: #94a3b8; font-weight: 400; }
.score-value.good { color: #10b981; }
.score-value.ok { color: #f59e0b; }
.score-value.bad { color: #ef4444; }
.score-bar { height: 6px; background: var(--ink-3); border-radius: 100px; margin-top: 10px; overflow: hidden; }
.score-bar-fill { height: 100%; border-radius: 100px; transition: width 0.6s; }

/* ── PDF 整改新增面板样式 ── */
.feedback-input { margin-top: 10px; padding: 8px 4px; border-top: 1px dashed #e2e8f0; }
.feedback-form { display: flex; align-items: center; gap: 12px; margin-top: 8px; flex-wrap: wrap; }
.feedback-hint { font-size: 12px; color: #94a3b8; }

.diagnosis-panel {
  background: #fff; border: 1px solid #cffafe; border-radius: 14px;
  padding: 20px; margin-bottom: 24px; box-shadow: 0 2px 12px rgba(var(--ice-rgb), 0.06);
}
.diagnosis-content { display: flex; flex-direction: column; gap: 16px; }
.diagnosis-section { background: var(--ink-3); border-left: 3px solid var(--ice); padding: 12px 16px; border-radius: 8px; }
.section-title { font-size: 14px; font-weight: 600; color: #0e7490; margin-bottom: 8px; }
.gap-list { display: flex; flex-direction: column; gap: 6px; }
.gap-item { display: flex; align-items: center; gap: 10px; font-size: 13px; color: var(--grey); padding: 6px 0; }
.gap-severity { padding: 2px 8px; border-radius: 100px; font-size: 11px; font-weight: 600; color: #fff; }
.gap-severity.sev-high { background: #ef4444; }
.gap-severity.sev-medium { background: #f59e0b; }
.gap-severity.sev-low { background: #10b981; }
.gap-point { font-weight: 600; }
.gap-subject { color: #64748b; font-size: 12px; }
.gap-score { margin-left: auto; color: #94a3b8; font-size: 12px; }
.focus-list { margin: 0; padding-left: 20px; font-size: 13px; color: var(--grey); line-height: 1.8; }
.strategy-text { font-size: 13px; color: var(--grey); line-height: 1.7; }

/* 资源难度匹配曲线 */
.match-curve-chart { width: 100%; height: 260px; }
.curve-legend-hint { font-size: 11px; color: #94a3b8; line-height: 1.6; margin-top: 4px; }

.debate-panel {
  background: #fff; border: 1px solid #fecaca; border-radius: 14px;
  padding: 20px; margin-bottom: 24px; box-shadow: 0 2px 12px rgba(239,68,68,0.06);
}
.debate-content { display: flex; flex-direction: column; gap: 16px; }
.debate-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.stat-card {
  background: var(--ink-3); border: 1px solid #fecaca; border-radius: 12px; padding: 16px; text-align: center;
}
.stat-card.stat-success { background: var(--ink-3); border-color: var(--ink-5); }
.stat-card.stat-warning { background: var(--ink-3); border-color: var(--ink-5); }
.stat-value { font-size: 28px; font-weight: 700; color: #ef4444; line-height: 1; }
.stat-card.stat-success .stat-value { color: #10b981; }
.stat-card.stat-warning .stat-value { color: #f59e0b; }
.stat-label { font-size: 12px; color: #64748b; margin-top: 6px; }
.debate-summary .summary-text { font-size: 13px; color: var(--grey); line-height: 1.7; }
.badge-success { background: var(--ink-3); color: #065f46; }
.badge-warning { background: var(--ink-3); color: #92400e; }

/* ── 辩论证据链（幻觉裁决 / 矛盾仲裁 / 知识库溯源）── */
.evidence-section { display: flex; flex-direction: column; gap: 10px; }
.evidence-section .section-title { font-size: 13px; font-weight: 600; color: var(--grey); }
.hallucination-card, .contradiction-card {
  border: 1px solid #fecaca; border-left-width: 4px; border-radius: 10px;
  padding: 12px 14px; background: #fff; display: flex; flex-direction: column; gap: 8px;
}
.hallucination-card.sev-critical { border-color: #ef4444; background: var(--ink-3); }
.hallucination-card.sev-major { border-color: #f59e0b; background: var(--ink-3); }
.hallucination-card.sev-minor { border-color: var(--ink-5); }
.contradiction-card { border-color: var(--ink-5); background: var(--ink-3); }
.h-head { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.h-type {
  font-size: 12px; font-weight: 600; color: #b91c1c;
  background: var(--ink-3); border-radius: 6px; padding: 2px 8px;
}
.h-agent { font-size: 12px; color: var(--grey); font-weight: 500; }
.h-sev { margin-left: auto; font-size: 11px; border-radius: 999px; padding: 2px 10px; }
.sev-tag-critical { background: #ef4444; color: #fff; }
.sev-tag-major { background: #f59e0b; color: #fff; }
.sev-tag-minor { background: var(--ink-3); color: #92400e; }
.h-row { display: flex; gap: 10px; font-size: 13px; line-height: 1.65; }
.h-key { flex-shrink: 0; width: 92px; color: #94a3b8; font-weight: 500; }
.h-val { color: var(--grey); word-break: break-all; }
.h-val.bad { color: #b91c1c; text-decoration: line-through; text-decoration-color: rgba(185,28,28,.45); }
.h-val.good { color: #047857; }
.kb-evidence-item {
  display: flex; align-items: baseline; gap: 8px; font-size: 13px; line-height: 1.7;
  padding: 9px 12px; background: var(--ink-3); border: 1px dashed #cbd5e1; border-radius: 10px;
}
.kb-evidence-item i { color: #059669; }
.kb-claim { color: #1e293b; font-weight: 500; }
.kb-source { color: #64748b; font-size: 12px; white-space: nowrap; }

.adapt-panel {
  background: #fff; border: 1px solid #d1fae5; border-radius: 14px;
  padding: 20px; margin-bottom: 24px; box-shadow: 0 2px 12px rgba(16,185,129,0.06);
}
.adapt-content { display: flex; flex-direction: column; gap: 16px; }
.adapt-reason, .adapt-feedback { background: var(--ink-3); border-left: 3px solid #10b981; padding: 12px 16px; border-radius: 8px; }
.adapt-reason .section-title, .adapt-feedback .section-title { color: #047857; }
.reason-text, .feedback-text { font-size: 13px; color: var(--grey); line-height: 1.7; }
.decision-simplify { background: var(--ink-3); color: var(--acid-dim); }
.decision-challenge { background: var(--ink-3); color: #5b21b6; }
.decision-correct { background: var(--ink-3); color: #92400e; }
.decision-maintain { background: var(--ink-3); color: #065f46; }

.timeline-panel {
  background: #fff; border: 1px solid #e2e8f0; border-radius: 14px; padding: 20px; margin-bottom: 24px;
}
.timeline { display: flex; flex-wrap: wrap; gap: 10px; }
.timeline-item {
  display: flex; align-items: flex-start; gap: 12px;
  padding: 12px 16px; background: var(--ink-3); border: 1px solid #e2e8f0;
  border-radius: 10px; flex: 1 1 320px; min-width: 320px; transition: all 0.3s;
}
.timeline-item.completed { border-color: rgba(34, 197, 94, 0.25); }
.timeline-item.running { border-color: rgba(245, 158, 11, 0.45); box-shadow: 0 0 15px rgba(245,158,11,0.12); }
.timeline-item.failed { border-color: rgba(239, 68, 68, 0.4); }
.timeline-item.clickable {
  cursor: pointer;
}
.timeline-item.clickable:hover {
  border-color: rgba(var(--acid-rgb), 0.6);
  box-shadow: 0 4px 16px rgba(var(--acid-rgb), 0.15);
  transform: translateY(-1px);
}
.step-jump-hint {
  margin-left: auto;
  font-size: 11px;
  color: var(--acid);
  background: rgba(var(--acid-rgb), 0.1);
  border-radius: 100px;
  padding: 2px 8px;
  white-space: nowrap;
}

.step-index {
  width: 28px; height: 28px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700; color: #fff;
  background: linear-gradient(135deg, var(--acid), var(--ice)); flex-shrink: 0;
}
.step-agent { font-size: 13px; font-weight: 600; color: var(--acid); }
.step-info { flex: 1; min-width: 0; }
.step-top-row { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; }
.step-action { font-size: 12px; color: #94a3b8; margin-bottom: 4px; }
.step-io { display: flex; gap: 12px; font-size: 11px; color: #64748b; margin-top: 4px; }
.step-io .el-icon-bottom { color: #f59e0b; }
.step-io .el-icon-top { color: #22c55e; }
.step-duration { font-size: 11px; color: #64748b; font-family: 'Courier New', monospace; }
.step-status-badge span { padding: 2px 8px; border-radius: 100px; font-size: 11px; }
.badge-completed { background: rgba(34,197,94,0.15); color: #4ade80; }
.badge-running { background: rgba(245,158,11,0.15); color: #fbbf24; }
.badge-failed { background: rgba(239,68,68,0.15); color: #f87171; }

.result-panel {
  background: #fff; border: 1px solid #e2e8f0; border-radius: 14px; padding: 20px;
}
.result-body {
  white-space: pre-wrap; word-break: break-word;
  font-size: 13px; line-height: 1.8; color: #1e293b;
  max-height: 500px; overflow-y: auto; padding: 16px;
  background: var(--ink-3); border-radius: 10px; font-family: 'Fira Code', Consolas, monospace;
}

/* ── 各 Agent 产出 Tab 切换 ── */
.output-tabs { margin-top: 16px; }
.output-tab-bar {
  display: flex; gap: 4px; flex-wrap: wrap; padding: 4px;
  background: var(--ink-3); border-radius: 10px 10px 0 0;
}
.output-tab-item {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 16px; border-radius: 8px;
  font-size: 13px; font-weight: 600; color: #64748b;
  cursor: pointer; transition: all 0.2s ease;
  user-select: none;
}
.output-tab-item:hover { background: rgba(255,255,255,0.7); color: var(--grey); }
.output-tab-item.active {
  background: #fff; color: var(--acid);
  box-shadow: 0 2px 8px rgba(var(--acid-rgb), 0.15);
}
.output-tab-item i { font-size: 14px; }
.output-tab-content {
  background: var(--ink-3); border-radius: 0 0 10px 10px; padding: 4px;
  border: 1px solid #e2e8f0; border-top: none;
}
.output-text {
  white-space: pre-wrap; word-break: break-word;
  font-size: 13px; line-height: 1.85; color: #1e293b;
  max-height: 440px; overflow-y: auto; padding: 16px;
  margin: 0; font-family: 'Fira Code', Consolas, monospace;
}

.result-summary { margin-top: 16px; }
.summary-label {
  font-size: 13px; font-weight: 700; color: var(--acid); margin-bottom: 8px;
  display: flex; align-items: center; gap: 6px;
}

/* ── 查看详情按钮 ── */
.view-detail-btn { margin-left: auto; padding: 8px 12px; font-size: 12px; color: var(--acid); }

/* ── 全屏详情对话框 ── */
.detail-dialog { border-radius: 16px; overflow: hidden; }
.detail-dialog >>> .el-dialog__header {
  background: linear-gradient(135deg, var(--ink), #16284a);
  padding: 18px 24px;
}
.detail-dialog >>> .el-dialog__title { color: #fff; font-size: 16px; font-weight: 700; }
.detail-dialog >>> .el-dialog__headerbtn .el-dialog__close { color: #94a3b8; font-size: 18px; }
.detail-dialog >>> .el-dialog__body { padding: 0; }
.detail-dialog-body {
  max-height: 75vh; overflow-y: auto; padding: 28px 32px;
  background: var(--ink-3);
}

/* Markdown 渲染样式 */
.detail-markdown { font-size: 14px; line-height: 1.9; color: #1e293b; }
.detail-markdown .md-h1 { font-size: 22px; font-weight: 800; color: #0f172a; margin: 20px 0 12px; padding-bottom: 8px; border-bottom: 2px solid #e2e8f0; }
.detail-markdown .md-h2 { font-size: 18px; font-weight: 700; color: #1e293b; margin: 18px 0 10px; }
.detail-markdown .md-h3 { font-size: 15px; font-weight: 700; color: var(--grey); margin: 14px 0 8px; }
.detail-markdown .md-p { margin: 8px 0; }
.detail-markdown .md-ul { margin: 8px 0; padding-left: 24px; list-style: none; }
.detail-markdown .md-ul li { margin: 4px 0; position: relative; padding-left: 16px; }
.detail-markdown .md-ul li::before { content: '•'; position: absolute; left: 0; color: var(--acid); font-weight: 700; }
.detail-markdown .md-ul li.md-ol::before { content: ''; }
.detail-markdown .md-ul li.md-ol { list-style: decimal; padding-left: 4px; }
.detail-markdown strong { color: #0f172a; font-weight: 700; }
.detail-markdown em { color: var(--grey); font-style: italic; }
.detail-markdown .md-inline-code {
  background: var(--ink-3); color: #0369a1; padding: 2px 6px; border-radius: 4px;
  font-family: 'Fira Code', Consolas, monospace; font-size: 12px;
}
.detail-markdown .md-code-block {
  background: var(--ink-2); color: #e2e8f0; padding: 16px 20px; border-radius: 10px;
  overflow-x: auto; margin: 12px 0; font-size: 13px; line-height: 1.6;
  font-family: 'Fira Code', Consolas, monospace;
}
.detail-markdown .md-code-block code { background: none; color: inherit; }

/* 代码展示 */
.detail-code { background: var(--ink-2); border-radius: 12px; padding: 20px 24px; overflow: auto; max-height: 70vh; }
.detail-code pre { margin: 0; }
.detail-code code {
  color: #e2e8f0; font-size: 13px; line-height: 1.7;
  font-family: 'Fira Code', Consolas, 'Courier New', monospace;
}

/* 测试题卡片 */
.detail-quiz { display: flex; flex-direction: column; gap: 16px; }
.quiz-card {
  background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.quiz-num {
  display: inline-block; background: var(--ink-3); color: #fff;
  font-size: 12px; font-weight: 700; padding: 3px 10px; border-radius: 100px;
  margin-bottom: 10px;
}
.quiz-content { font-size: 14px; line-height: 1.8; color: #1e293b; }
.quiz-content .md-h3 { font-size: 15px; font-weight: 700; color: #0f172a; margin: 8px 0; }
.quiz-content .md-ul { padding-left: 20px; }
.quiz-content strong { color: var(--acid); }

/* ── 协作页富展示样式（与多智能体生成页保持一致）── */
.output-placeholder {
  padding: 40px 20px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
}
.mm-container { background: #fff; border-radius: 10px; padding: 8px; overflow: auto; }
.mm-svg-wrap svg { display: block; }

/* 练习题目 */
.quiz-card { position: relative; }
.quiz-card.correct { border-color: #34d399; box-shadow: 0 2px 12px rgba(52, 199, 89, 0.15); }
.quiz-card.wrong { border-color: #fb7185; box-shadow: 0 2px 12px rgba(251, 113, 133, 0.15); }
.quiz-q-type {
  display: inline-block; padding: 2px 10px; border-radius: 100px;
  color: #fff; font-size: 11px; font-weight: 600; margin-bottom: 8px;
}
.quiz-q-text { font-size: 14px; line-height: 1.8; color: #1e293b; margin-bottom: 10px; }
.quiz-options { display: flex; flex-direction: column; gap: 6px; margin-bottom: 10px; }
.quiz-option { padding: 6px 10px; border-radius: 8px; transition: background 0.2s; }
.quiz-option.opt-correct { background: rgba(52, 199, 89, 0.12); }
.quiz-option.opt-wrong { background: rgba(251, 113, 133, 0.12); }
.quiz-radio { white-space: normal; }
.quiz-input-area { margin-bottom: 10px; }
.quiz-feedback { background: var(--ink-3); border-radius: 8px; padding: 12px 14px; font-size: 13px; color: var(--grey); line-height: 1.7; }
.quiz-fb-row { margin-bottom: 4px; }
.quiz-fb-result { margin-top: 8px; }
.fb-tag { display: inline-block; padding: 2px 10px; border-radius: 100px; font-size: 12px; font-weight: 600; }
.fb-ok { background: rgba(52, 199, 89, 0.15); color: #059669; }
.fb-no { background: rgba(251, 113, 133, 0.15); color: #e11d48; }
.fb-skip { background: rgba(100, 116, 139, 0.12); color: #64748b; }
.quiz-score-bar { display: flex; align-items: center; gap: 12px; padding: 12px 16px; background: var(--ink-3); border-radius: 10px; }
.quiz-score-left { white-space: nowrap; }
.quiz-score-big { font-size: 22px; font-weight: 800; color: #1e293b; }
.quiz-score-pct { margin-left: 6px; color: #64748b; font-size: 13px; }

/* 代码展示 */
.code-tabs { display: flex; gap: 4px; flex-wrap: wrap; margin-bottom: 10px; }
.code-tab {
  display: flex; align-items: center; gap: 6px; padding: 6px 14px;
  border-radius: 8px 8px 0 0; background: #141A14; color: #94a3b8;
  font-size: 12px; cursor: pointer; border: 1px solid #334155; border-bottom: none;
}
.code-tab.active { background: var(--ink-2); color: #e2e8f0; }
.code-tab-lang { font-weight: 600; }
.code-tab-num { opacity: 0.6; }
.code-editor-pane { background: var(--ink-2); border-radius: 0 10px 10px 10px; overflow: hidden; }
.code-editor-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 14px; background: #141A14; border-bottom: 1px solid #334155;
}
.code-editor-lang { font-size: 12px; color: #94a3b8; font-weight: 600; }
.code-editor-actions { display: flex; gap: 6px; }
.code-textarea {
  width: 100%; min-height: 180px; background: var(--ink-2); color: #e2e8f0;
  border: none; outline: none; padding: 14px 16px; font-family: 'Fira Code', Consolas, monospace;
  font-size: 13px; line-height: 1.7; resize: vertical; box-sizing: border-box;
}
.code-result-pane { background: var(--ink-2); border-radius: 10px; margin-top: 10px; padding: 12px 14px; }
.code-result-header { font-size: 13px; color: #94a3b8; margin-bottom: 8px; }
.result-badge { display: inline-block; margin-left: 8px; padding: 1px 8px; border-radius: 100px; font-size: 11px; }
.result-badge.badge-ok { background: rgba(52, 199, 89, 0.15); color: #34d399; }
.result-badge.badge-err { background: rgba(251, 113, 133, 0.15); color: #fb7185; }
.code-console { background: var(--ink-2); border-radius: 8px; overflow: auto; }
.code-console-text { margin: 0; padding: 10px 12px; color: var(--ice); font-family: 'Fira Code', Consolas, monospace; font-size: 12px; white-space: pre-wrap; }
.code-console-placeholder { padding: 12px; color: #64748b; font-size: 12px; }

/* 视频脚本场景 */
.video-scenes { display: flex; flex-direction: column; gap: 14px; }
.video-scene-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 16px 18px; }
.scene-header { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.scene-badge { background: var(--ink-3); color: #fff; border-radius: 100px; padding: 2px 10px; font-size: 11px; font-weight: 700; }
.scene-title { margin: 0; font-size: 16px; color: #1e293b; }
.scene-body { display: flex; flex-direction: column; gap: 12px; }
.scene-section { background: var(--ink-3); border-radius: 10px; padding: 12px 14px; }
.scene-section-title { font-size: 13px; font-weight: 700; color: var(--grey); margin-bottom: 8px; }
.scene-item { display: flex; gap: 8px; align-items: flex-start; margin-bottom: 4px; }
.scene-bullet { width: 18px; height: 18px; flex-shrink: 0; border-radius: 50%; background: var(--ink-3); color: var(--acid); font-size: 11px; display: inline-flex; align-items: center; justify-content: center; }
.scene-text { font-size: 13px; color: var(--grey); line-height: 1.6; }
.voice-text { font-size: 13px; color: var(--grey); line-height: 1.7; }

/* ── 思维导图 / 视频（与多智能体生成页一致）── */
.mindmap-panel,
.video-panel {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
}
.mindmap-panel .panel-header,
.video-panel .panel-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e2e8f0;
  background: var(--ink-3);
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}
.mindmap-panel .panel-header i,
.video-panel .panel-header i { margin-right: 6px; color: #c8a45c; }
.mm-tools { margin-left: auto; display: flex; gap: 4px; }
.mm-badge {
  font-size: 10px; background: #007AFF; color: #fff;
  padding: 2px 8px; border-radius: 4px; font-weight: 600; margin-left: 8px;
  font-family: 'JetBrains Mono', monospace; letter-spacing: 0.5px;
}
.mm-hint-bar {
  display: flex; gap: 20px; padding: 8px 16px;
  background: var(--ink-3); border-radius: 8px 8px 0 0;
  font-size: 12px; color: #86868b; border-bottom: 1px solid #e8e8ed;
}
.mm-hint-bar b { color: #1d1d1f; font-weight: 600; }
.output-tab-content .mm-container {
  height: 520px;
  min-height: 500px;
  overflow: hidden;
  position: relative;
  background: var(--ink-3);
  border-radius: 0 0 12px 12px;
  cursor: grab;
  user-select: none;
  padding: 0;
}
.output-tab-content .mm-container:active { cursor: grabbing; }
.output-tab-content .mm-container svg { display: block; width: 100%; height: 100%; }
.mm-node { transition: opacity 0.2s ease; cursor: move; }
.mm-node:hover rect { filter: brightness(0.96); }
.mm-edge-flow { animation: mm-flow 1.2s linear infinite; }
@keyframes mm-flow { to { stroke-dashoffset: -10; } }
.mm-edit-input {
  width: 100%; height: 100%; border: 2px solid #007AFF; border-radius: 8px;
  padding: 4px 8px; font-size: 13px;
  font-family: -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  color: #1d1d1f; outline: none; background: #fff;
}

.video-panel .anim-hint { font-size: 12px; color: #94a3b8; margin-left: 8px; font-weight: 400; }
.video-panel .video-actions { margin-left: auto; display: flex; gap: 6px; }
.video-player-wrapper { background: var(--ink-2); padding: 20px; border-radius: 0 0 12px 12px; }
.video-scene-display { min-height: 320px; display: flex; align-items: center; justify-content: center; }
.video-scene-card {
  background: #141A14; border: 1px solid #334155; border-radius: 14px;
  padding: 36px 44px; width: 100%; max-width: 800px;
}
.video-scene-card .scene-header { text-align: center; margin-bottom: 28px; }
.video-scene-card .scene-badge {
  display: inline-block; padding: 3px 14px; background: rgba(var(--acid-rgb), 0.2);
  color: #a5b4fc; border-radius: 12px; font-size: 12px; font-weight: 500; margin-bottom: 10px;
}
.video-scene-card .scene-title { font-size: 24px; font-weight: 700; color: #f1f5f9; margin: 0; }
.scene-items { display: flex; flex-direction: column; gap: 14px; }
.video-scene-card .scene-item { display: flex; align-items: flex-start; gap: 14px; }
.video-scene-card .scene-bullet {
  width: 28px; height: 28px; border-radius: 50%; flex-shrink: 0;
  background: linear-gradient(135deg, var(--acid), var(--ice));
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700;
}
.video-scene-card .scene-text { font-size: 16px; color: #e2e8f0; line-height: 1.7; padding-top: 3px; }
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
.scene-fade-enter-active, .scene-fade-leave-active { transition: all 0.4s ease; }
.scene-fade-enter { transform: translateY(20px); opacity: 0; }
.scene-fade-leave-to { transform: translateY(-20px); opacity: 0; }

@media (max-width: 1000px) {
  .collab-main { flex-direction: column; }
  .graph-panel { flex: none; }
  .score-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>

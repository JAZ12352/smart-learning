<template>
  <div class="app-container">
    <el-card>
      <div slot="header">
        <span>教学意图理解与知识融合</span>
      </div>

      <!-- 输入区域 -->
      <div class="input-section">
        <el-form :model="form" label-width="80px">
          <!-- 教学意图输入 -->
          <el-form-item label="教学意图">
            <el-input
              v-model="form.teachingIntent"
              type="textarea"
              :rows="4"
              placeholder="请输入您的教学意图，例如：我要讲解人工智能的基础概念，重点介绍机器学习算法原理"
            />
          </el-form-item>

          <!-- 参考资料上传 -->
          <el-form-item label="参考资料">
            <el-upload
              class="upload-demo"
              :auto-upload="false"
              :on-change="handleFileChange"
              accept=".pdf,.doc,.docx,.ppt,.pptx,.jpg,.jpeg,.png,.gif,.mp4,.avi,.mov"
              :multiple="true"
              action="/api/admin/teaching-intent/upload"
            >
              <el-button size="small" type="warning">
                <i class="el-icon-upload2"></i> 上传参考资料
              </el-button>
              <div slot="tip" class="el-upload__tip">
                支持上传 PDF、Word、PPT、图片、视频等格式文件
              </div>
            </el-upload>
            <!-- 已上传文件列表 -->
            <div v-if="uploadedFiles.length > 0" class="uploaded-files" style="margin-top: 10px;">
              <el-tag
                v-for="(file, index) in uploadedFiles"
                :key="index"
                closable
                @close="removeFile(index)"
                style="margin-right: 10px; margin-top: 10px;"
              >
                {{ file.name }}
              </el-tag>
            </div>
          </el-form-item>

          <!-- 分析按钮 -->
          <el-form-item>
            <el-button
              type="primary"
              @click="analyzeTeachingIntent"
              :loading="analyzing"
            >
              分析教学意图与融合知识
            </el-button>
          </el-form-item>

          <!-- 生成功能按钮 -->
          <el-form-item v-if="analysisResult">
            <el-button type="success" @click="generatePPT" :loading="generatingPPT">
              <i class="el-icon-download"></i> 生成PPT课件
            </el-button>
            <el-button type="success" @click="generateWordLessonPlan" :loading="generatingWord" style="margin-left: 10px;">
              <i class="el-icon-download"></i> 生成Word教案
            </el-button>
            <el-button type="warning" @click="generateContent" :loading="generatingContent" style="margin-left: 10px;">
              <i class="el-icon-sunny"></i> 生成多样化内容
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 流式生成结果区域 -->
      <div v-if="streaming || streamingResult" class="streaming-section" style="margin-top: 20px;">
        <el-card>
          <div slot="header">
            <span>分析结果</span>
          </div>
          <div class="streaming-content" style="padding: 15px; background: #f9f9f9; border-radius: 4px; min-height: 200px; white-space: pre-wrap;">
            {{ streamingResult || '分析中...' }}
          </div>
        </el-card>
      </div>

      <!-- 分析结果区域 -->
      <div v-if="analysisResult && !streaming" class="result-section" style="margin-top: 20px;">
        <el-card>
          <div slot="header">
            <span>分析结果</span>
          </div>

          <!-- 教学要素提取 -->
          <el-collapse>
            <el-collapse-item title="1. 教学要素提取" name="1">
              <div class="result-content">
                <el-descriptions :column="1">
                  <el-descriptions-item label="知识点清单">
                    {{ Array.isArray(analysisResult.knowledgePoints) ? analysisResult.knowledgePoints.join('、') : analysisResult.knowledgePoints }}
                  </el-descriptions-item>
                  <el-descriptions-item label="逻辑顺序">
                    {{ analysisResult.logicalOrder }}
                  </el-descriptions-item>
                  <el-descriptions-item label="重点">
                    {{ Array.isArray(analysisResult.keyPoints) ? analysisResult.keyPoints.join('、') : analysisResult.keyPoints }}
                  </el-descriptions-item>
                  <el-descriptions-item label="难点">
                    {{ Array.isArray(analysisResult.difficultPoints) ? analysisResult.difficultPoints.join('、') : analysisResult.difficultPoints }}
                  </el-descriptions-item>
                </el-descriptions>
              </div>
            </el-collapse-item>

            <!-- 参考资料分析 -->
            <el-collapse-item title="2. 参考资料分析" name="2">
              <div class="result-content">
                <pre>{{ analysisResult.referenceAnalysis }}</pre>
              </div>
            </el-collapse-item>

            <!-- 知识融合结果 -->
            <el-collapse-item title="3. 知识融合结果" name="3">
              <div class="result-content">
                <p>{{ analysisResult.knowledgeIntegration }}</p>
              </div>
            </el-collapse-item>

            <!-- 课件生成指令集 -->
            <el-collapse-item title="4. 课件生成指令集" name="4">
              <div class="result-content">
                <pre>{{ analysisResult.lessonPlanInstructions }}</pre>
              </div>
            </el-collapse-item>

            <!-- 多样化内容结果 -->
            <el-collapse-item v-if="contentResult" title="5. 多样化内容" name="5">
              <div class="result-content">
                <div v-if="contentResult.rawContent" style="white-space: pre-wrap;">
                  {{ contentResult.rawContent }}
                </div>
                <div v-else>
                  <div v-if="contentResult.animationIdeas" class="content-section">
                    <h4>动画创意</h4>
                    <div class="animation-ideas">
                      <div v-for="(idea, index) in contentResult.animationIdeas" :key="index" class="animation-item">
                        <h5>{{ idea.title || idea }}</h5>
                        <p v-if="idea.description">{{ idea.description }}</p>
                        <div class="animation-action">
                          <el-button type="primary" size="small" @click="runAnimation(idea)">
                            <i class="el-icon-video-camera"></i> 运行动画
                          </el-button>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div v-if="contentResult.interactiveGames" class="content-section">
                    <h4>互动小游戏</h4>
                    <div class="interactive-games">
                      <div v-for="(game, index) in contentResult.interactiveGames" :key="index" class="game-item">
                        <h5>{{ game.title || game }}</h5>
                        <p v-if="game.description">{{ game.description }}</p>
                        <div class="game-action">
                          <el-button type="primary" size="small" @click="runGame(game)">
                            <i class="el-icon-magic-stick"></i> 运行游戏
                          </el-button>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div v-if="contentResult.creativeMethods" class="content-section">
                    <h4>创意教学方法</h4>
                    <ul>
                      <li v-for="(method, index) in contentResult.creativeMethods" :key="index">
                        {{ method }}
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </div>
    </el-card>

    <!-- HTML5 动画/游戏运行对话框 -->
    <el-dialog :title="htmlDialogTitle" :visible.sync="htmlDialogVisible" width="900px" custom-class="html-run-dialog" :destroy-on-close="true">
      <iframe ref="htmlIframe" class="html-iframe" sandbox="allow-scripts allow-same-origin"></iframe>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'TeachingIntentAnalyze',
  data () {
    return {
      form: {
        teachingIntent: ''
      },
      uploadedFiles: [],
      analyzing: false,
      analysisResult: null,
      uploadAction: '/api/admin/teaching-intent/upload',
      streaming: false,
      streamingResult: '',
      generatingPPT: false,
      generatingWord: false,
      generatingContent: false,
      contentResult: null,
      animationInterval: null,
      gameInterval: null,
      htmlDialogVisible: false,
      htmlDialogTitle: ''
    }
  },
  methods: {
    // 解析分析结果
    parseAnalysisResult (text) {
      if (!text || typeof text !== 'string') {
        return {
          knowledgePoints: [],
          logicalOrder: '',
          keyPoints: [],
          difficultPoints: [],
          referenceAnalysis: '',
          knowledgeIntegration: '',
          lessonPlanInstructions: '',
          rawResponse: text || ''
        }
      }
      return {
        knowledgePoints: [],
        logicalOrder: text.includes('逻辑') || text.includes('顺序') ? '已从流式内容提取' : '',
        keyPoints: [],
        difficultPoints: [],
        referenceAnalysis: text.includes('参考') ? '已从流式内容提取' : '',
        knowledgeIntegration: text.includes('融合') ? '已从流式内容提取' : '',
        lessonPlanInstructions: text.includes('教案') || text.includes('课件') ? '已从流式内容提取' : '',
        rawResponse: text
      }
    },

    // 处理文件上传
    handleFileChange (file) {
      this.uploadedFiles.push(file.raw)
      this.$message.success('文件已添加到参考资料')
    },

    // 移除上传的文件
    removeFile (index) {
      this.uploadedFiles.splice(index, 1)
      this.$message.success('文件已从参考资料中移除')
    },

    // 分析教学意图与融合知识
    async analyzeTeachingIntent () {
      if (!this.form.teachingIntent.trim() && this.uploadedFiles.length === 0) {
        this.$message.warning('请输入教学意图或上传参考资料')
        return
      }

      this.analyzing = true
      this.streaming = true
      this.streamingResult = ''
      this.analysisResult = null
      this.$message.info('正在分析教学意图和融合知识...')

      try {
        // 构建分析请求数据
        const analysisData = {
          teachingIntent: this.form.teachingIntent,
          referenceMaterials: this.uploadedFiles.map(file => ({
            name: file.name,
            type: file.type,
            size: file.size
          }))
        }

        console.log('分析请求数据:', analysisData)

        // 从localStorage获取token
        let token = localStorage.getItem('token')

        console.log('从localStorage获取到的token:', token)

        // 如果localStorage中没有token，尝试从cookie中获取
        if (!token) {
          function getCookie (name) {
            const value = `; ${document.cookie}`
            const parts = value.split(`; ${name}=`)
            if (parts.length === 2) return parts.pop().split(';').shift()
          }
          token = getCookie('xzs')
          console.log('从cookie获取到的token:', token)
          if (token) {
            localStorage.setItem('token', token)
            console.log('将cookie中的token存储到localStorage')
          }
        }

        // 使用原生fetch API处理流式响应，确保使用正确的代理路径
        const response = await fetch('/api/admin/teaching-intent/analyze', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'token': token || ''
          },
          credentials: 'include', // 确保携带 cookie
          body: JSON.stringify(analysisData)
        })

        if (!response.ok) {
          // 检查是否是401错误
          if (response.status === 401) {
            // 清除本地存储的token
            localStorage.removeItem('token')
            // 跳转到登录页面
            this.$router.push('/login')
            this.$message.error('登录已过期，请重新登录')
            return
          }
          throw new Error('API请求失败')
        }

        // 处理流式响应
        const reader = response.body.getReader()
        const decoder = new TextDecoder()

        let fullResponse = ''

        while (true) {
          const { done, value } = await reader.read()
          if (done) {
            break
          }

          const chunk = decoder.decode(value, { stream: true })
          fullResponse += chunk
          this.streamingResult += chunk

          // 滚动到底部
          this.$nextTick(() => {
            const streamingContent = document.querySelector('.streaming-content')
            if (streamingContent) {
              streamingContent.scrollTop = streamingContent.scrollHeight
            }
          })
        }

        // 流式响应已直接显示在界面上
        console.log('流式响应完成，内容长度:', fullResponse.length)
        this.analysisResult = this.parseAnalysisResult(fullResponse)
        this.$message.success('教学意图分析完成')
      } catch (error) {
        console.error('分析失败:', error)
        // 清除之前的分析结果
        this.analysisResult = null
        this.$message.error('分析失败，请重试')
      } finally {
        this.analyzing = false
        this.streaming = false
      }
    },

    // 生成PPT课件
    async generatePPT () {
      this.generatingPPT = true
      this.$message.info('正在生成PPT课件...')

      try {
        const pptData = {
          teachingIntent: this.form.teachingIntent,
          analysisResult: this.analysisResult
        }

        let token = localStorage.getItem('token')

        const response = await fetch('/api/admin/teaching-intent/generate-ppt', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'token': token
          },
          credentials: 'include',
          body: JSON.stringify(pptData)
        })

        if (!response.ok) {
          throw new Error('API请求失败')
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder()

        let fullResponse = ''

        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          const chunk = decoder.decode(value, { stream: true })
          fullResponse += chunk
        }

        this.$message.success('PPT课件内容生成成功！')
        this.streamingResult = fullResponse
        this.streaming = true
      } catch (error) {
        console.error('生成PPT失败:', error)
        this.$message.error('生成PPT失败，请重试')
      } finally {
        this.generatingPPT = false
      }
    },

    async generateWordLessonPlan () {
      this.generatingWord = true
      this.$message.info('正在生成Word教案...')

      try {
        const wordData = {
          teachingIntent: this.form.teachingIntent,
          analysisResult: this.analysisResult
        }

        let token = localStorage.getItem('token')

        const response = await fetch('/api/admin/teaching-intent/generate-word', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'token': token
          },
          credentials: 'include',
          body: JSON.stringify(wordData)
        })

        if (!response.ok) {
          throw new Error('API请求失败')
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder()

        let fullResponse = ''

        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          const chunk = decoder.decode(value, { stream: true })
          fullResponse += chunk
        }

        this.$message.success('Word教案内容生成成功！')
        this.streamingResult = fullResponse
        this.streaming = true
      } catch (error) {
        console.error('生成Word教案失败:', error)
        this.$message.error('生成Word教案失败，请重试')
      } finally {
        this.generatingWord = false
      }
    },

    // 生成多样化内容
    async generateContent () {
      this.generatingContent = true
      this.$message.info('正在生成多样化内容...')

      try {
        // 构建请求数据
        const contentData = {
          teachingIntent: this.form.teachingIntent,
          analysisResult: this.analysisResult
        }

        // 从localStorage获取token
        let token = localStorage.getItem('token')

        // 如果localStorage中没有token，尝试从cookie中获取
        if (!token) {
          function getCookie (name) {
            const value = `; ${document.cookie}`
            const parts = value.split(`; ${name}=`)
            if (parts.length === 2) return parts.pop().split(';').shift()
          }
          token = getCookie('xzs')
          if (token) {
            localStorage.setItem('token', token)
          }
        }

        // 调用后端的多样化内容生成接口
        const response = await fetch('/api/admin/teaching-intent/generate-content', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'token': token
          },
          credentials: 'include',
          body: JSON.stringify(contentData)
        })

        if (!response.ok) {
          throw new Error('API请求失败')
        }

        // 处理非流式响应
        const fullResponse = await response.text()
        console.log('完整响应:', fullResponse)

        // 解析响应
        try {
          const result = JSON.parse(fullResponse)
          if (result.code === 1) {
            this.$message.success('多样化内容生成成功！')
            this.showContentResult(result.response)
          } else {
            this.$message.error(result.message || '生成失败')
          }
        } catch (parseError) {
          console.error('解析响应失败:', parseError)
          this.$message.error('解析响应失败')
        }
      } catch (error) {
        console.error('生成多样化内容失败:', error)
        this.$message.error('生成多样化内容失败，请重试')
      } finally {
        this.generatingContent = false
      }
    },

    // 显示多样化内容结果
    showContentResult (result) {
      this.contentResult = result
      this.$message.info('多样化内容已生成，点击下方查看详情')
    },

    // 运行动画
    runAnimation (idea) {
      const title = idea.title || '动画演示'
      const htmlCode = idea.htmlCode || this.generateDefaultAnimation(idea)
      this.showHtmlDialog(title, htmlCode)
    },

    // 运行游戏
    runGame (game) {
      const title = game.title || '互动游戏'
      const htmlCode = game.htmlCode || this.generateDefaultGame(game)
      this.showHtmlDialog(title, htmlCode)
    },

    // 在对话框中运行 HTML5 内容（iframe 方式）
    showHtmlDialog (title, htmlCode) {
      this.htmlDialogTitle = title
      this.htmlDialogVisible = true
      this.$nextTick(() => {
        const iframe = this.$refs.htmlIframe
        if (iframe) {
          const doc = iframe.contentDocument || iframe.contentWindow.document
          doc.open()
          doc.write(`
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { font-family: 'Microsoft YaHei', sans-serif; background: var(--ink-3); padding: 20px; }
                canvas { display: block; margin: 0 auto; background: white; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
                .game-container { max-width: 800px; margin: 0 auto; }
                h2 { text-align: center; color: var(--grey); margin-bottom: 15px; }
                .btn { padding: 10px 24px; border: none; border-radius: 4px; cursor: pointer; font-size: 14px; margin: 5px; }
                .btn-primary { background: var(--ink-3); color: white; }
                .btn-success { background: #67c23a; color: white; }
                .btn-danger { background: #f56c6c; color: white; }
                .question { background: white; padding: 20px; border-radius: 8px; margin-bottom: 15px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                .options { margin-top: 10px; }
                .option { display: block; width: 100%; padding: 12px; margin: 8px 0; background: var(--ink-3); border: 2px solid #dcdfe6; border-radius: 6px; cursor: pointer; text-align: left; font-size: 14px; transition: all 0.3s; }
                .option:hover { border-color: var(--ink-5); background: var(--ink-3); }
                .option.selected { border-color: var(--ink-5); background: var(--ink-3); }
                .option.correct { border-color: #67c23a; background: var(--ink-3); }
                .option.wrong { border-color: #f56c6c; background: var(--ink-3); }
                .score { text-align: center; font-size: 18px; color: var(--acid); font-weight: bold; margin-top: 15px; }
                .feedback { text-align: center; font-size: 16px; margin-top: 10px; padding: 10px; border-radius: 4px; }
                .feedback.correct { background: var(--ink-3); color: #67c23a; }
                .feedback.wrong { background: var(--ink-3); color: #f56c6c; }
              </style>
            </head>
            <body>
              ${htmlCode}
            </body>
            </html>
          `)
          doc.close()
        }
      })
    },

    // 生成默认动画代码（当 AI 未提供时）
    generateDefaultAnimation (idea) {
      const title = idea.title || '动画演示'
      const desc = idea.description || ''
      return `
        <div class="game-container">
          <h2>${title}</h2>
          <canvas id="animCanvas" width="760" height="400"></canvas>
        </div>
        <script>
          const canvas = document.getElementById('animCanvas');
          const ctx = canvas.getContext('2d');
          let t = 0;
          const colors = ['#CCFF00', '#67c23a', '#e6a23c', '#f56c6c', '#909399'];
          function draw() {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            ctx.fillStyle = '#303133';
            ctx.font = 'bold 24px Microsoft YaHei';
            ctx.textAlign = 'center';
            ctx.fillText('${title}', canvas.width / 2, 40);
            for (let i = 0; i < 5; i++) {
              const x = canvas.width / 2 + Math.cos(t * 0.02 + i * 1.256) * 150;
              const y = 220 + Math.sin(t * 0.02 + i * 1.256) * 100;
              const r = 20 + Math.sin(t * 0.05 + i) * 10;
              ctx.beginPath();
              ctx.arc(x, y, r, 0, Math.PI * 2);
              ctx.fillStyle = colors[i];
              ctx.fill();
            }
            ctx.beginPath();
            ctx.strokeStyle = '#CCFF00';
            ctx.lineWidth = 2;
            for (let x = 0; x < canvas.width; x++) {
              const y = 300 + Math.sin(x * 0.02 + t * 0.03) * 30;
              if (x === 0) ctx.moveTo(x, y);
              else ctx.lineTo(x, y);
            }
            ctx.stroke();
            ctx.fillStyle = '#909399';
            ctx.font = '14px Microsoft YaHei';
            ctx.fillText('${desc || '动态动画演示'}', canvas.width / 2, 380);
            t++;
            requestAnimationFrame(draw);
          }
          draw();
        <\/script>
      `
    },

    // 生成默认游戏代码（当 AI 未提供时）
    generateDefaultGame (game) {
      const title = game.title || '知识问答'
      const desc = game.description || ''
      return `
        <div class="game-container">
          <h2>${title}</h2>
          <div id="gameArea">
            <div class="question" id="questionBox">
              <div id="questionText">加载中...</div>
              <div class="options" id="optionsBox"></div>
            </div>
            <div class="score" id="scoreText">得分: 0</div>
            <div class="feedback" id="feedback"></div>
          </div>
        </div>
        <script>
          const questions = [
            { q: '关于“${title}”的基本概念是什么？', options: ['核心定义', '无关内容', '错误概念', '模糊描述'], answer: 0 },
            { q: '“${title}”最重要的应用场景是？', options: ['无关场景', '关键应用', '过时用法', '错误场景'], answer: 1 },
            { q: '学习“${title}”的关键要点是？', options: ['不重要的', '次要的', '核心要点', '无关的'], answer: 2 },
          ];
          let current = 0, score = 0, answered = false;
          function render() {
            if (current >= questions.length) {
              document.getElementById('questionBox').innerHTML = '<h3>游戏结束!</h3><p class="score">最终得分: ' + score + '/' + questions.length + '</p><button class="btn btn-primary" onclick="current=0;score=0;answered=false;render()">再玩一次</button>';
              return;
            }
            const q = questions[current];
            document.getElementById('questionText').textContent = '第' + (current+1) + '题: ' + q.q;
            const box = document.getElementById('optionsBox');
            box.innerHTML = '';
            document.getElementById('feedback').textContent = '';
            document.getElementById('feedback').className = 'feedback';
            answered = false;
            q.options.forEach((opt, i) => {
              const btn = document.createElement('button');
              btn.className = 'option';
              btn.textContent = String.fromCharCode(65+i) + '. ' + opt;
              btn.onclick = () => selectOption(i);
              box.appendChild(btn);
            });
            document.getElementById('scoreText').textContent = '得分: ' + score;
          }
          function selectOption(idx) {
            if (answered) return;
            answered = true;
            const q = questions[current];
            const btns = document.querySelectorAll('.option');
            btns.forEach((b, i) => {
              if (i === q.answer) b.className = 'option correct';
              else if (i === idx && idx !== q.answer) b.className = 'option wrong';
            });
            const fb = document.getElementById('feedback');
            if (idx === q.answer) {
              score++;
              fb.textContent = '正确!';
              fb.className = 'feedback correct';
            } else {
              fb.textContent = '错误，正确答案是 ' + String.fromCharCode(65+q.answer);
              fb.className = 'feedback wrong';
            }
            document.getElementById('scoreText').textContent = '得分: ' + score;
            setTimeout(() => { current++; render(); }, 1500);
          }
          render();
        <\/script>
      `
    }
  }
}
</script>

<style scoped>
.input-section {
  margin-bottom: 20px;
}

.result-section {
  margin-top: 20px;
}

.result-content {
  padding: 10px;
  background: var(--ink-3);
  border-radius: 4px;
  margin-top: 10px;
}

.content-section {
  margin-bottom: 15px;
}

.content-section h4 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #333;
  font-size: 16px;
  font-weight: bold;
}

.content-section ul {
  margin: 0;
  padding-left: 20px;
}

.content-section li {
  margin-bottom: 5px;
  line-height: 1.5;
}

pre {
  white-space: pre-wrap;
  font-family: inherit;
  margin: 0;
}

.animation-ideas,
.interactive-games {
  margin-top: 10px;
}

.animation-item,
.game-item {
  margin-bottom: 20px;
  padding: 15px;
  background: var(--ink-3);
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.animation-item h5,
.game-item h5 {
  margin-top: 0;
  margin-bottom: 10px;
  color: var(--grey);
  font-size: 16px;
  font-weight: bold;
}

.animation-item p,
.game-item p {
  margin: 0 0 10px 0;
  color: var(--grey);
  line-height: 1.5;
}

.animation-preview {
  margin: 10px 0;
}

.preview-image {
  max-width: 100%;
  max-height: 200px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.animation-action,
.game-action {
  margin-top: 10px;
}

.animation-dialog,
.game-dialog {
  width: 850px !important;
  max-width: 90% !important;
}

.animation-dialog .el-dialog__body,
.game-dialog .el-dialog__body {
  padding: 0 !important;
  overflow: hidden;
}

.animation-dialog #animation-container,
.game-dialog #game-container {
  width: 100%;
  height: 650px;
  padding: 20px;
  background: var(--ink-3);
  position: relative;
}

.animation-dialog #animation-canvas,
.game-dialog #game-canvas {
  width: 100%;
  max-width: 760px;
  height: auto;
  border: 1px solid #ccc;
  background: white;
  margin: 0 auto;
  display: block;
}

/* HTML5 运行对话框 */
.html-run-dialog .el-dialog__body {
  padding: 0 !important;
}
.html-iframe {
  width: 100%;
  height: 600px;
  border: none;
}
</style>

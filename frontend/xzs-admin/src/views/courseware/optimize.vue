<template>
  <div class="app-container">
    <el-card>
      <div slot="header">
        <span>课件迭代优化与导出</span>
      </div>

      <!-- 课件预览区域 -->
      <div class="preview-section" style="margin-bottom: 20px;">
        <el-card>
          <div slot="header">
            <span>课件预览</span>
          </div>
          <div class="preview-content" style="padding: 20px; background: #f9f9f9; border-radius: 4px; min-height: 300px;">
            <div v-if="!currentCourseware" class="empty-preview">
              <el-empty description="请选择或创建课件" />
            </div>
            <div v-else class="courseware-content">
              <h3>{{ currentCourseware.title }}</h3>
              <div v-for="(slide, index) in currentCourseware.slides" :key="index" class="slide-item" style="margin-bottom: 20px; padding: 15px; background: #fff; border-radius: 4px;">
                <h4>幻灯片 {{ index + 1 }}: {{ slide.title }}</h4>
                <div v-html="slide.content" style="margin-top: 10px;"></div>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 修改意见输入区域 -->
      <div class="feedback-section" style="margin-bottom: 20px;">
        <el-form :model="form" label-width="80px">
          <el-form-item label="修改意见">
            <el-input
              v-model="form.feedback"
              type="textarea"
              :rows="4"
              placeholder="请输入修改意见，如：调整顺序、简化某页、增加一个案例"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="submitFeedback" :loading="submitting">
              提交修改意见
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 优化历史区域 -->
      <div class="history-section" style="margin-bottom: 20px;">
        <el-card>
          <div slot="header">
            <span>优化历史</span>
          </div>
          <el-timeline>
            <el-timeline-item
              v-for="(item, index) in optimizationHistory"
              :key="index"
              :timestamp="item.timestamp"
              type="primary"
              placement="top"
            >
              <el-card>
                <h4>修改意见</h4>
                <p>{{ item.feedback }}</p>
                <h4>优化结果</h4>
                <p>{{ item.result }}</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </div>

      <!-- 导出功能区域 -->
      <div class="export-section">
        <el-card>
          <div slot="header">
            <span>导出功能</span>
          </div>
          <div class="export-options" style="padding: 20px;">
            <el-button type="success" @click="exportCourseware('pptx')">
              <i class="el-icon-download"></i> 导出为PPT
            </el-button>
            <el-button type="success" @click="exportCourseware('docx')" style="margin-left: 10px;">
              <i class="el-icon-download"></i> 导出为Word
            </el-button>
            <el-button type="success" @click="exportCourseware('html')" style="margin-left: 10px;">
              <i class="el-icon-download"></i> 导出为HTML5
            </el-button>
            <el-button type="success" @click="exportCourseware('gif')" style="margin-left: 10px;">
              <i class="el-icon-download"></i> 导出为GIF
            </el-button>
            <el-button type="success" @click="exportCourseware('mp4')" style="margin-left: 10px;">
              <i class="el-icon-download"></i> 导出为MP4
            </el-button>
          </div>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script>
import PptxGenJS from 'pptxgenjs'
import { Document, Packer, Paragraph, HeadingLevel } from 'docx'
export default {
  name: 'CoursewareOptimize',
  data () {
    return {
      form: {
        feedback: ''
      },
      submitting: false,
      currentCourseware: {
        title: '人工智能基础课件',
        slides: [
          {
            title: '人工智能简介',
            content: '<p>人工智能（AI）是研究、开发用于模拟、延伸和扩展人的智能的理论、方法、技术及应用系统的一门新的技术科学。</p>'
          },
          {
            title: '机器学习',
            content: '<p>机器学习是人工智能的一个分支，它使计算机系统能够从数据中学习，而不需要被明确编程。</p>'
          },
          {
            title: '深度学习',
            content: '<p>深度学习是机器学习的一个分支，它使用多层神经网络来模拟人脑的学习过程。</p>'
          }
        ]
      },
      optimizationHistory: [
        {
          timestamp: '2026-03-28 10:00',
          feedback: '增加一个人工智能应用案例',
          result: '已在第二页增加了人工智能在医疗领域的应用案例'
        },
        {
          timestamp: '2026-03-28 11:00',
          feedback: '调整幻灯片顺序，将深度学习放在机器学习之前',
          result: '已调整幻灯片顺序，现在顺序为：人工智能简介、深度学习、机器学习'
        }
      ]
    }
  },
  methods: {
    // 提交修改意见
    async submitFeedback () {
      if (!this.form.feedback.trim()) {
        this.$message.warning('请输入修改意见')
        return
      }

      this.submitting = true
      this.$message.info('正在处理修改意见...')

      try {
        // 模拟智能体处理修改意见
        await new Promise(resolve => setTimeout(resolve, 2000))

        // 添加到优化历史
        this.optimizationHistory.unshift({
          timestamp: new Date().toLocaleString(),
          feedback: this.form.feedback,
          result: '已根据您的修改意见优化课件'
        })

        // 清空输入
        this.form.feedback = ''
        this.$message.success('修改意见已处理，课件已优化')
      } catch (error) {
        console.error('处理修改意见失败:', error)
        this.$message.error('处理修改意见失败，请重试')
      } finally {
        this.submitting = false
      }
    },

    // 导出课件
    exportCourseware (format) {
      if (!this.currentCourseware) {
        this.$message.warning('请先生成课件内容')
        return
      }
      this.$message.info(`正在导出为${this.getFormatName(format)}格式...`)

      const coursewareContent = this.currentCourseware
      switch (format) {
        case 'pptx':
          this.generatePPT(coursewareContent)
          break
        case 'docx':
          this.generateWord(coursewareContent)
          break
        case 'html':
          const htmlContent = this.buildHTMLContent(coursewareContent)
          const htmlBlob = new Blob([htmlContent], { type: 'text/html' })
          this.downloadBlob(htmlBlob, `${coursewareContent.title}.html`)
          this.$message.success('HTML5导出成功！')
          break
        case 'gif':
          this.exportAsGif(coursewareContent)
          break
        case 'mp4':
          this.exportAsMp4(coursewareContent)
          break
        default:
          const txtContent = this.buildTextContent(coursewareContent)
          const txtBlob = new Blob([txtContent], { type: 'text/plain' })
          this.downloadBlob(txtBlob, `${coursewareContent.title}.txt`)
      }
    },

    // 获取格式名称
    getFormatName (format) {
      const formatMap = {
        pptx: 'PPT',
        docx: 'Word',
        html: 'HTML5',
        gif: 'GIF',
        mp4: 'MP4'
      }
      return formatMap[format] || format
    },

    // 下载Blob文件
    downloadBlob (blob, fileName) {
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = fileName
      document.body.appendChild(link)
      link.click()
      setTimeout(() => {
        URL.revokeObjectURL(link.href)
        document.body.removeChild(link)
      }, 100)
    },

    // 将课件幻灯片渲染到Canvas上
    renderSlideToCanvas (slide, index, total, canvas) {
      const ctx = canvas.getContext('2d')
      const W = canvas.width
      const H = canvas.height

      const bgGrad = ctx.createLinearGradient(0, 0, W, H)
      bgGrad.addColorStop(0, '#667eea')
      bgGrad.addColorStop(1, '#764ba2')
      ctx.fillStyle = bgGrad
      ctx.fillRect(0, 0, W, H)

      ctx.globalAlpha = 0.1
      ctx.fillStyle = '#fff'
      ctx.beginPath()
      ctx.arc(W * 0.85, H * 0.15, 120, 0, Math.PI * 2)
      ctx.fill()
      ctx.beginPath()
      ctx.arc(W * 0.1, H * 0.8, 80, 0, Math.PI * 2)
      ctx.fill()
      ctx.globalAlpha = 1

      ctx.fillStyle = 'rgba(255,255,255,0.5)'
      ctx.font = '16px Microsoft YaHei'
      ctx.textAlign = 'right'
      ctx.fillText(`${index + 1} / ${total}`, W - 30, H - 20)

      ctx.textAlign = 'left'
      ctx.fillStyle = '#fff'
      ctx.font = 'bold 36px Microsoft YaHei'
      this.wrapText(ctx, slide.title || '无标题', 60, 80, W - 120, 48)

      ctx.fillStyle = '#CCFF00'
      ctx.fillRect(60, 140, 100, 4)

      const plainContent = slide.content ? slide.content.replace(/<[^>]*>/g, '') : ''
      ctx.fillStyle = 'rgba(255,255,255,0.9)'
      ctx.font = '22px Microsoft YaHei'
      this.wrapText(ctx, plainContent, 60, 180, W - 120, 32)
    },

    wrapText (ctx, text, x, y, maxWidth, lineHeight) {
      if (!text) return y
      const lines = text.split('\n')
      let currentY = y
      for (const line of lines) {
        let currentLine = ''
        for (let i = 0; i < line.length; i++) {
          const testLine = currentLine + line[i]
          const metrics = ctx.measureText(testLine)
          if (metrics.width > maxWidth && currentLine.length > 0) {
            ctx.fillText(currentLine, x, currentY)
            currentLine = line[i]
            currentY += lineHeight
            if (currentY > 520) { ctx.fillText('...', x, currentY); return currentY }
          } else {
            currentLine = testLine
          }
        }
        ctx.fillText(currentLine, x, currentY)
        currentY += lineHeight
        if (currentY > 520) break
      }
      return currentY
    },

    // 导出为GIF
    async exportAsGif (courseware) {
      const slides = courseware.slides && Array.isArray(courseware.slides) ? courseware.slides : []
      if (slides.length === 0) {
        this.$message.warning('没有课件内容可导出')
        return
      }
      this.$message.info('正在生成GIF动画，请稍候...')
      try {
        const GIF = (await import('gif.js')).default
        const canvas = document.createElement('canvas')
        canvas.width = 960
        canvas.height = 540
        const ctx = canvas.getContext('2d')
        const gif = new GIF({
          workers: 2,
          quality: 10,
          width: 960,
          height: 540,
          workerScript: '/admin/gif.worker.js'
        })
        for (let i = 0; i < slides.length; i++) {
          this.renderSlideToCanvas(slides[i], i, slides.length, canvas)
          const frameData = ctx.getImageData(0, 0, 960, 540)
          // 淡入帧
          for (let alpha = 1; alpha <= 3; alpha++) {
            const fadeFrame = ctx.createImageData(960, 540)
            const opacity = alpha / 3
            for (let j = 0; j < frameData.data.length; j += 4) {
              fadeFrame.data[j] = Math.round(frameData.data[j] * opacity + (102 * (1 - opacity)))
              fadeFrame.data[j + 1] = Math.round(frameData.data[j + 1] * opacity + (126 * (1 - opacity)))
              fadeFrame.data[j + 2] = Math.round(frameData.data[j + 2] * opacity + (234 * (1 - opacity)))
              fadeFrame.data[j + 3] = 255
            }
            ctx.putImageData(fadeFrame, 0, 0)
            gif.addFrame(ctx, { copy: true, delay: 100 })
          }
          ctx.putImageData(frameData, 0, 0)
          gif.addFrame(ctx, { copy: true, delay: 2000 })
        }
        gif.on('finished', (blob) => {
          this.downloadBlob(blob, `${courseware.title || '课件'}.gif`)
          this.$message.success('GIF动画导出成功！')
        })
        gif.render()
      } catch (error) {
        console.error('GIF导出失败:', error)
        this.$message.error('GIF导出失败：' + error.message)
      }
    },

    // 导出为MP4视频
    async exportAsMp4 (courseware) {
      const slides = courseware.slides && Array.isArray(courseware.slides) ? courseware.slides : []
      if (slides.length === 0) {
        this.$message.warning('没有课件内容可导出')
        return
      }
      this.$message.info('正在生成视频，请稍候...')
      try {
        const canvas = document.createElement('canvas')
        canvas.width = 960
        canvas.height = 540
        const ctx = canvas.getContext('2d')

        let mimeType = 'video/webm;codecs=vp9'
        if (!MediaRecorder.isTypeSupported(mimeType)) mimeType = 'video/webm;codecs=vp8'
        if (!MediaRecorder.isTypeSupported(mimeType)) mimeType = 'video/webm'
        if (!MediaRecorder.isTypeSupported(mimeType)) {
          this.$message.error('当前浏览器不支持视频录制，请使用Chrome或Firefox')
          return
        }

        const stream = canvas.captureStream(30)
        const recorder = new MediaRecorder(stream, { mimeType })
        const chunks = []
        recorder.ondataavailable = (e) => { if (e.data.size > 0) chunks.push(e.data) }
        recorder.onstop = () => {
          const blob = new Blob(chunks, { type: 'video/webm' })
          this.downloadBlob(blob, `${courseware.title || '课件'}.webm`)
          this.$message.success('视频导出成功！')
        }

        recorder.start()
        const fps = 30
        const slideFrames = 3 * fps
        const transFrames = Math.floor(0.5 * fps)

        for (let i = 0; i < slides.length; i++) {
          this.renderSlideToCanvas(slides[i], i, slides.length, canvas)
          const currentData = ctx.getImageData(0, 0, 960, 540)
          let nextData = null
          if (i < slides.length - 1) {
            this.renderSlideToCanvas(slides[i + 1], i + 1, slides.length, canvas)
            nextData = ctx.getImageData(0, 0, 960, 540)
          }
          // 淡入
          for (let f = 0; f < transFrames; f++) {
            const alpha = f / transFrames
            ctx.fillStyle = '#667eea'
            ctx.fillRect(0, 0, 960, 540)
            ctx.globalAlpha = alpha
            ctx.putImageData(currentData, 0, 0)
            ctx.globalAlpha = 1
            await new Promise(resolve => requestAnimationFrame(resolve))
          }
          // 静止
          ctx.putImageData(currentData, 0, 0)
          for (let f = 0; f < slideFrames - transFrames; f++) {
            await new Promise(resolve => requestAnimationFrame(resolve))
          }
          // 过渡
          if (nextData) {
            for (let f = 0; f < transFrames; f++) {
              const alpha = f / transFrames
              ctx.globalAlpha = 1 - alpha
              ctx.putImageData(currentData, 0, 0)
              ctx.globalAlpha = alpha
              ctx.putImageData(nextData, 0, 0)
              ctx.globalAlpha = 1
              await new Promise(resolve => requestAnimationFrame(resolve))
            }
          }
        }
        recorder.stop()
      } catch (error) {
        console.error('视频导出失败:', error)
        this.$message.error('视频导出失败：' + error.message)
      }
    },

    // 生成PPT
    generatePPT (courseware) {
      try {
        const pptx = new PptxGenJS()
        const titleSlide = pptx.addSlide()
        titleSlide.addText(courseware.title || '无标题', {
          x: 0,
          y: 1.5,
          w: '100%',
          h: 2,
          align: 'center',
          valign: 'middle',
          fontSize: 24,
          bold: true,
          color: '363636'
        })
        const slides = courseware.slides && Array.isArray(courseware.slides) ? courseware.slides : []
        slides.forEach(slide => {
          const contentSlide = pptx.addSlide()
          contentSlide.addText(slide.title || '无标题', {
            x: 0, y: 0.5, w: '100%', h: 1, align: 'center', fontSize: 18, bold: true, color: '4CAF50'
          })
          const plainContent = slide.content ? slide.content.replace(/<[^>]*>/g, '') : ''
          contentSlide.addText(plainContent, {
            x: 0.5, y: 1.5, w: 9, h: 5, fontSize: 14, color: '333333'
          })
        })
        pptx.writeFile({ fileName: `${courseware.title || '无标题'}.pptx` })
        this.$message.success('PPT导出成功！')
      } catch (error) {
        console.error('PPT导出失败:', error)
        this.$message.error('PPT导出失败')
      }
    },

    // 生成Word
    generateWord (courseware) {
      try {
        const slides = courseware.slides && Array.isArray(courseware.slides) ? courseware.slides : []
        const doc = new Document({
          sections: [{
            properties: {},
            children: [
              new Paragraph({ text: courseware.title || '无标题', heading: HeadingLevel.HEADING_1, alignment: 'center' }),
              ...slides.map(slide => [
                new Paragraph({ text: slide.title || '无标题', heading: HeadingLevel.HEADING_2 }),
                new Paragraph({ text: slide.content ? slide.content.replace(/<[^>]*>/g, '') : '', spacing: { after: 200 } })
              ]).flat()
            ]
          }]
        })
        Packer.toBlob(doc).then(blob => {
          this.downloadBlob(blob, `${courseware.title || '无标题'}.docx`)
          this.$message.success('Word导出成功！')
        })
      } catch (error) {
        console.error('Word导出失败:', error)
        this.$message.error('Word导出失败')
      }
    },

    // 构建HTML内容
    buildHTMLContent (courseware) {
      const title = courseware.title || '无标题'
      const slides = courseware.slides && Array.isArray(courseware.slides) ? courseware.slides : []
      let content = `<!DOCTYPE html><html><head><title>${title}</title><style>body{font-family:Arial,sans-serif;margin:20px}h1{color:#333}h2{color:var(--acid);margin-top:30px}.slide{margin-bottom:40px;padding:20px;border:1px solid #ddd;border-radius:5px}.content{margin-top:10px;line-height:1.6}</style></head><body><h1>${title}</h1>`
      slides.forEach((slide, i) => {
        content += `<div class="slide"><h2>幻灯片 ${i + 1}: ${slide.title || ''}</h2><div class="content">${slide.content || ''}</div></div>`
      })
      content += '</body></html>'
      return content
    },

    // 构建文本内容
    buildTextContent (courseware) {
      const slides = courseware.slides && Array.isArray(courseware.slides) ? courseware.slides : []
      let content = `${courseware.title || ''}\n\n`
      slides.forEach((slide, i) => {
        content += `幻灯片 ${i + 1}: ${slide.title || ''}\n${slide.content ? slide.content.replace(/<[^>]*>/g, '') : ''}\n\n`
      })
      return content
    }
  }
}
</script>

<style scoped>
.preview-section {
  margin-bottom: 20px;
}

.feedback-section {
  margin-bottom: 20px;
}

.history-section {
  margin-bottom: 20px;
}

.export-section {
  margin-bottom: 20px;
}

.empty-preview {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.courseware-content {
  line-height: 1.6;
}

.slide-item {
  border-left: 4px solid var(--acid);
  padding-left: 15px;
}
</style>

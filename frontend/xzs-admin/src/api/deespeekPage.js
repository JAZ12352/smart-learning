import axios from 'axios'
import PptxGenJS from 'pptxgenjs'

// 后端API基础URL
const DEEPSEEK_PAGE_API_BASE_URL = '/api/admin/deepSeek/api/page'

// 创建Axios实例
const deespeekPageApi = axios.create({
  baseURL: DEEPSEEK_PAGE_API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  },
  timeout: 180000 // 延长超时时间到180秒
})

// 测试模式：直接在前端生成PPT
const testMode = false

// 生成AI内容
const generateAIContent = async (data) => {
  // 调用DeepSeek API生成内容
  const teachingContent = data.content || '无'
  const title = data.title || '教案标题'
  const gradeLevel = data.gradeLevel || 1
  const subjectId = data.subjectId || '数学'
  const type = data.type || 'image-text'

  // 测试模式：直接使用模拟数据
  if (testMode) {
    console.log('Test mode: Using mock content')
    return generateMockContent(data)
  }

  try {
    // 构建请求数据
    const requestData = {
      gradeLevel,
      subjectId,
      title,
      type,
      content: teachingContent
    }

    // 从 localStorage 获取 token
    const token = localStorage.getItem('token') || ''

    // 调用后端API获取AI生成内容
    const response = await deespeekPageApi.post('', requestData, {
      headers: {
        'token': token
      }
    })

    if (response.data && response.data.code === 1 && response.data.response && response.data.response.title) {
      return response.data.response.title
    } else {
      // 如果API调用失败，使用模拟数据
      console.error('DeepSeek API调用失败，使用模拟数据')
      return generateMockContent(data)
    }
  } catch (error) {
    console.error('生成AI内容失败:', error)
    // 如果出错，使用模拟数据
    return generateMockContent(data)
  }
}

// 生成模拟内容
const generateMockContent = (data) => {
  const teachingContent = data.content || '无'
  const title = data.title || '教案标题'

  // 根据标题和教学目标生成相关的知识点
  let aiContent = `# ${title}

## 教学目标
${teachingContent}

`

  // 根据标题生成特定的知识点
  if (title.includes('三角函数')) {
    aiContent += `## 三角函数核心知识点

### 基本概念
- **三角函数定义**：在直角三角形中，对于锐角θ，有
  - 正弦：sinθ = 对边/斜边
  - 余弦：cosθ = 邻边/斜边
  - 正切：tanθ = 对边/邻边

### 常用公式
- **基本关系式**：
  - sin²θ + cos²θ = 1
  - tanθ = sinθ/cosθ
  - cotθ = 1/tanθ = cosθ/sinθ

- **诱导公式**：
  - sin(π/2 - θ) = cosθ
  - cos(π/2 - θ) = sinθ
  - sin(π - θ) = sinθ
  - cos(π - θ) = -cosθ

- **和角公式**：
  - sin(α + β) = sinαcosβ + cosαsinβ
  - cos(α + β) = cosαcosβ - sinαsinβ
  - tan(α + β) = (tanα + tanβ)/(1 - tanαtanβ)

### 图像与性质
- **正弦函数**：y = sinx
  - 定义域：R
  - 值域：[-1, 1]
  - 周期：2π
  - 奇偶性：奇函数

- **余弦函数**：y = cosx
  - 定义域：R
  - 值域：[-1, 1]
  - 周期：2π
  - 奇偶性：偶函数

- **正切函数**：y = tanx
  - 定义域：{x | x ≠ kπ + π/2, k∈Z}
  - 值域：R
  - 周期：π
  - 奇偶性：奇函数

### 教学图示
1. **单位圆**：以原点为圆心，半径为1的圆，用于理解三角函数的几何意义
2. **三角函数图像**：展示sinx、cosx、tanx的图像，帮助学生理解函数性质
3. **直角三角形**：展示三角函数的定义，帮助学生记忆基本关系式

### 应用场景
- **测量高度**：利用三角函数测量建筑物、树木等的高度
- **导航**：利用三角函数计算方位角和距离
- **物理**：在力学、波动等领域的应用

`
  } else if (title.includes('二次函数')) {
    aiContent += `## 二次函数核心知识点

### 基本概念
- **二次函数定义**：形如y = ax² + bx + c (a ≠ 0)的函数

### 图像与性质
- **抛物线**：二次函数的图像是一条抛物线
  - 当a > 0时，抛物线开口向上
  - 当a < 0时，抛物线开口向下

- **顶点**：抛物线的最高点或最低点
  - 顶点坐标：(-b/2a, f(-b/2a))

- **对称轴**：直线x = -b/2a

- **截距**：
  - y截距：(0, c)
  - x截距：解方程ax² + bx + c = 0

### 解析式形式
- **一般式**：y = ax² + bx + c
- **顶点式**：y = a(x - h)² + k，其中(h, k)是顶点坐标
- **交点式**：y = a(x - x₁)(x - x₂)，其中x₁和x₂是x截距

### 教学图示
1. **抛物线图像**：展示不同a值对抛物线开口方向和大小的影响
2. **顶点与对称轴**：标记抛物线的顶点和对称轴
3. **截距**：标记抛物线与x轴和y轴的交点

### 应用场景
- **最值问题**：求最大值或最小值
- **实际问题**：如利润最大化、面积最大化等
- **物理**：自由落体运动、抛体运动等

`
  } else {
    // 通用知识点
    aiContent += `## 相关知识点

### 核心概念
- **基本概念**：与${title}相关的基本定义和术语
- **基本原理**：支撑${title}的理论基础

### 知识体系
- **主要内容**：${title}的主要内容和结构
- **相关知识**：与${title}相关的其他知识点

### 应用场景
- **实际应用**：${title}在实际中的应用
- **案例分析**：${title}的典型应用案例

### 教学图示
1. **概念图**：展示${title}的概念体系
2. **流程图**：展示${title}的学习流程
3. **实例图**：展示${title}的应用实例

`
  }

  // 添加通用部分
  aiContent += `
## 教学重难点
- **重点**：掌握与${title}相关的核心概念和原理
- **难点**：理解${title}的复杂知识点和应用场景

## 教学过程
1. **导入**：通过实际例子引入${title}，与教学目标建立联系
2. **讲解**：详细讲解与${title}相关的核心知识点
3. **练习**：通过练习巩固所学内容，强化教学目标的达成
4. **总结**：总结本次课程的重点内容，回顾教学目标的实现

## 教学方法
- 讲授法：讲解核心知识点
- 讨论法：组织学生讨论相关问题
- 案例分析法：通过实际案例加深理解
- 实验法：通过实验或演示帮助学生理解抽象概念

## 教学资源
- 多媒体课件：展示知识点和案例
- 实物教具：辅助理解抽象概念
- 网络资源：提供额外的学习材料
- 数学软件：如GeoGebra、Mathematica等，用于绘制函数图像和进行计算

## 作业布置
- 完成课后习题：巩固所学知识点
- 查阅相关资料：拓展知识视野
- 准备下次课程的讨论话题：深入理解教学目标
- 实践作业：应用所学知识解决实际问题

## 教学评估
- 课堂提问：检查学生对知识点的理解
- 课堂练习：评估学生的应用能力
- 课后作业：检验教学目标的达成情况
- 单元测试：全面评估学生的学习效果
`

  return aiContent
}

// 生成模拟的多样化内容
const generateMockDiverseContent = (data) => {
  const title = data.title || '教案标题'
  const teachingContent = data.content || '无'

  // 根据标题生成特定的多样化内容
  if (title.includes('三角函数')) {
    return {
      animationIdeas: [
        {
          title: '三角函数单位圆动画',
          description: '展示角度变化时，三角函数值的变化过程，帮助学生直观理解三角函数的周期性',
          animationCode: 'trigonometry-unit-circle',
          htmlCode: '<div class="game-container"><h2>三角函数单位圆动画</h2><canvas id="animCanvas" width="760" height="400"></canvas></div><script>const canvas=document.getElementById("animCanvas");const ctx=canvas.getContext("2d");let t=0;function draw(){ctx.clearRect(0,0,canvas.width,canvas.height);ctx.fillStyle="#303133";ctx.font="bold 20px Microsoft YaHei";ctx.textAlign="center";ctx.fillText("三角函数单位圆",canvas.width/2,30);const cx=250,cy=220,r=120;ctx.beginPath();ctx.arc(cx,cy,r,0,Math.PI*2);ctx.strokeStyle="#CCFF00";ctx.lineWidth=2;ctx.stroke();const x=cx+r*Math.cos(t*0.03);const y=cy-r*Math.sin(t*0.03);ctx.beginPath();ctx.moveTo(cx,cy);ctx.lineTo(x,y);ctx.strokeStyle="#f56c6c";ctx.lineWidth=3;ctx.stroke();ctx.beginPath();ctx.arc(x,y,6,0,Math.PI*2);ctx.fillStyle="#f56c6c";ctx.fill();ctx.beginPath();ctx.moveTo(x,y);ctx.lineTo(x,cy);ctx.strokeStyle="#67c23a";ctx.lineWidth=2;ctx.setLineDash([5,5]);ctx.stroke();ctx.setLineDash([]);ctx.beginPath();ctx.strokeStyle="#CCFF00";ctx.lineWidth=2;for(let i=0;i<canvas.width-450;i++){const sy=220+80*Math.sin(i*0.03+t*0.03);if(i===0)ctx.moveTo(450+i,sy);else ctx.lineTo(450+i,sy);}ctx.stroke();ctx.fillStyle="#909399";ctx.font="14px Microsoft YaHei";ctx.fillText("sin="+Math.sin(t*0.03).toFixed(3),cx,cy+r+30);ctx.fillText("cos="+Math.cos(t*0.03).toFixed(3),cx,cy+r+50);t++;requestAnimationFrame(draw);}draw();<\/script>'
        },
        {
          title: '三角函数图像生成动画',
          description: '从单位圆到三角函数图像的转化过程，展示sin、cos函数的图像形成',
          animationCode: 'trigonometry-graph-generation',
          htmlCode: '<div class="game-container"><h2>三角函数图像生成动画</h2><canvas id="animCanvas" width="760" height="400"></canvas></div><script>const canvas=document.getElementById("animCanvas");const ctx=canvas.getContext("2d");let t=0;function draw(){ctx.clearRect(0,0,canvas.width,canvas.height);ctx.fillStyle="#303133";ctx.font="bold 20px Microsoft YaHei";ctx.textAlign="center";ctx.fillText("三角函数图像",canvas.width/2,30);ctx.beginPath();ctx.strokeStyle="#CCFF00";ctx.lineWidth=2;for(let x=50;x<710;i++){const y=200+80*Math.sin(x*0.02);if(x===50)ctx.moveTo(x,y);else ctx.lineTo(x,y);}ctx.stroke();ctx.beginPath();ctx.strokeStyle="#67c23a";ctx.lineWidth=2;for(let x=50;x<710;x++){const y=200+80*Math.cos(x*0.02);if(x===50)ctx.moveTo(x,y);else ctx.lineTo(x,y);}ctx.stroke();const px=50+((t%660));ctx.beginPath();ctx.arc(px,200+80*Math.sin(px*0.02),5,0,Math.PI*2);ctx.fillStyle="#f56c6c";ctx.fill();ctx.fillStyle="#CCFF00";ctx.font="12px Microsoft YaHei";ctx.textAlign="left";ctx.fillText("蓝=sin 绿=cos 红=当前点",50,390);t++;requestAnimationFrame(draw);}draw();<\/script>'
        }
      ],
      interactiveGames: [
        {
          title: '三角函数记忆挑战',
          description: '通过游戏形式帮助学生记忆三角函数公式和特殊角的值',
          playable: true,
          gameCode: 'trigonometry-memory-challenge',
          htmlCode: '<div class="game-container"><h2>三角函数记忆挑战</h2><div id="gameArea"><div class="question" id="questionBox"><div id="questionText">加载中...</div><div class="options" id="optionsBox"></div></div><div class="score" id="scoreText">得分: 0</div><div class="feedback" id="feedback"></div></div></div><script>const questions=[{q:"sin(30°)=?",options:["0.5","0.707","0.866","1"],answer:0},{q:"cos(60°)=?",options:["1","0.866","0.5","0"],answer:2},{q:"tan(45°)=?",options:["0","0.5","1","√3"],answer:2},{q:"sin(90°)=?",options:["0","0.5","1","-1"],answer:2},{q:"cos(0°)=?",options:["0","0.5","1","-1"],answer:2}];let current=0,score=0,answered=false;function render(){if(current>=questions.length){document.getElementById("questionBox").innerHTML="<h3>游戏结束!</h3><p class=\"score\">最终得分: "+score+"/"+questions.length+"</p><button class=\"btn btn-primary\" onclick=\"current=0;score=0;answered=false;render()\">再玩一次</button>";return;}const q=questions[current];document.getElementById("questionText").textContent="第"+(current+1)+"题: "+q.q;const box=document.getElementById("optionsBox");box.innerHTML="";document.getElementById("feedback").textContent="";document.getElementById("feedback").className="feedback";answered=false;q.options.forEach((opt,i)=>{const btn=document.createElement("button");btn.className="option";btn.textContent=String.fromCharCode(65+i)+". "+opt;btn.onclick=()=>selectOption(i);box.appendChild(btn);});document.getElementById("scoreText").textContent="得分: "+score;}function selectOption(idx){if(answered)return;answered=true;const q=questions[current];const btns=document.querySelectorAll(".option");btns.forEach((b,i)=>{if(i===q.answer)b.className="option correct";else if(i===idx&&idx!==q.answer)b.className="option wrong";});const fb=document.getElementById("feedback");if(idx===q.answer){score++;fb.textContent="正确!";fb.className="feedback correct";}else{fb.textContent="错误，正确答案是 "+String.fromCharCode(65+q.answer);fb.className="feedback wrong";}document.getElementById("scoreText").textContent="得分: "+score;setTimeout(()=>{current++;render();},1500);}render();<\/script>'
        }
      ],
      creativeMethods: [
        '三角函数歌曲：将三角函数公式和性质编成歌曲，帮助学生记忆',
        '三角函数手势操：通过手势动作帮助学生理解三角函数的定义和性质',
        '三角函数实地测量：组织学生使用三角函数测量校园内建筑物的高度，增强实际应用能力'
      ]
    }
  } else if (title.includes('二次函数')) {
    return {
      animationIdeas: [
        {
          title: '二次函数图像变换动画',
          description: '展示a参数变化时，抛物线的形状和位置变化',
          animationCode: 'quadratic-function-transformation',
          htmlCode: '<div class="game-container"><h2>二次函数图像变换</h2><canvas id="animCanvas" width="760" height="400"></canvas></div><script>const canvas=document.getElementById("animCanvas");const ctx=canvas.getContext("2d");let t=0;function draw(){ctx.clearRect(0,0,canvas.width,canvas.height);ctx.fillStyle="#303133";ctx.font="bold 20px Microsoft YaHei";ctx.textAlign="center";ctx.fillText("二次函数 y=ax²+bx+c",canvas.width/2,30);const a=1+0.5*Math.sin(t*0.02);const b=0;const c=0;ctx.beginPath();ctx.strokeStyle="#CCFF00";ctx.lineWidth=2;for(let px=-380;px<=380;px++){const x=px/30;const y=a*x*x+b*x+c;const sy=200-y*30;if(px===-380)ctx.moveTo(380+px,sy);else ctx.lineTo(380+px,sy);}ctx.stroke();ctx.strokeStyle="#ddd";ctx.beginPath();ctx.moveTo(0,200);ctx.lineTo(760,200);ctx.stroke();ctx.beginPath();ctx.moveTo(380,0);ctx.lineTo(380,400);ctx.stroke();ctx.fillStyle="#CCFF00";ctx.font="14px Microsoft YaHei";ctx.fillText("a="+a.toFixed(2),100,380);t++;requestAnimationFrame(draw);}draw();<\/script>'
        },
        {
          title: '二次函数最值问题动画',
          description: '通过动画展示如何通过顶点找到二次函数的最大值或最小值',
          animationCode: 'quadratic-function-max-min',
          htmlCode: '<div class="game-container"><h2>二次函数最值演示</h2><canvas id="animCanvas" width="760" height="400"></canvas></div><script>const canvas=document.getElementById("animCanvas");const ctx=canvas.getContext("2d");function draw(){ctx.clearRect(0,0,canvas.width,canvas.height);ctx.fillStyle="#303133";ctx.font="bold 20px Microsoft YaHei";ctx.textAlign="center";ctx.fillText("二次函数最值",canvas.width/2,30);const a=1,b=-2,c=-3;const vx=-b/(2*a);const vy=a*vx*vx+b*vx+c;ctx.strokeStyle="#ddd";ctx.beginPath();ctx.moveTo(0,300);ctx.lineTo(760,300);ctx.stroke();ctx.beginPath();ctx.moveTo(380,0);ctx.lineTo(380,400);ctx.stroke();ctx.beginPath();ctx.strokeStyle="#CCFF00";ctx.lineWidth=2;for(let px=-380;px<=380;px++){const x=px/30;const y=a*x*x+b*x+c;const sy=300-y*20;if(px===-380)ctx.moveTo(380+px,sy);else ctx.lineTo(380+px,sy);}ctx.stroke();const svx=380+vx*30;const svy=300-vy*20;ctx.beginPath();ctx.arc(svx,svy,8,0,Math.PI*2);ctx.fillStyle="#f56c6c";ctx.fill();ctx.fillStyle="#f56c6c";ctx.font="14px Microsoft YaHei";ctx.fillText("顶点("+vx+","+vy+")",svx+20,svy-10);ctx.fillText("最小值="+vy,canvas.width/2,380);}draw();<\/script>'
        }
      ],
      interactiveGames: [
        {
          title: '二次函数知识问答',
          description: '通过问答形式测试学生对二次函数的理解',
          playable: true,
          gameCode: 'quadratic-function-quiz',
          htmlCode: '<div class="game-container"><h2>二次函数知识问答</h2><div id="gameArea"><div class="question" id="questionBox"><div id="questionText">加载中...</div><div class="options" id="optionsBox"></div></div><div class="score" id="scoreText">得分: 0</div><div class="feedback" id="feedback"></div></div></div><script>const questions=[{q:"二次函数y=x²的顶点是?",options:["(0,0)","(1,1)","(-1,1)","(0,1)"],answer:0},{q:"y=x²开口方向是?",options:["向下","向上","向左","向右"],answer:1},{q:"y=-x²+4的最大值是?",options:["0","2","4","8"],answer:2}];let current=0,score=0,answered=false;function render(){if(current>=questions.length){document.getElementById("questionBox").innerHTML="<h3>游戏结束!</h3><p class=\"score\">最终得分: "+score+"/"+questions.length+"</p><button class=\"btn btn-primary\" onclick=\"current=0;score=0;answered=false;render()\">再玩一次</button>";return;}const q=questions[current];document.getElementById("questionText").textContent="第"+(current+1)+"题: "+q.q;const box=document.getElementById("optionsBox");box.innerHTML="";document.getElementById("feedback").textContent="";document.getElementById("feedback").className="feedback";answered=false;q.options.forEach((opt,i)=>{const btn=document.createElement("button");btn.className="option";btn.textContent=String.fromCharCode(65+i)+". "+opt;btn.onclick=()=>selectOption(i);box.appendChild(btn);});document.getElementById("scoreText").textContent="得分: "+score;}function selectOption(idx){if(answered)return;answered=true;const q=questions[current];const btns=document.querySelectorAll(".option");btns.forEach((b,i)=>{if(i===q.answer)b.className="option correct";else if(i===idx&&idx!==q.answer)b.className="option wrong";});const fb=document.getElementById("feedback");if(idx===q.answer){score++;fb.textContent="正确!";fb.className="feedback correct";}else{fb.textContent="错误，正确答案是 "+String.fromCharCode(65+q.answer);fb.className="feedback wrong";}document.getElementById("scoreText").textContent="得分: "+score;setTimeout(()=>{current++;render();},1500);}render();<\/script>'
        }
      ],
      creativeMethods: [
        '二次函数艺术创作：学生使用二次函数图像创作艺术作品，如抛物线组成的图案',
        '二次函数实验：通过物理实验验证二次函数在自由落体运动中的应用',
        '二次函数商业模拟：学生使用二次函数模型模拟商业中的利润最大化问题'
      ]
    }
  } else {
    // 通用多样化内容
    return {
      animationIdeas: [
        {
          title: `${title}概念动画`,
          description: `通过动画展示${title}的核心概念和原理`,
          animationCode: `${title.toLowerCase().replace(/\s+/g, '-')}-concept-animation`,
          htmlCode: '<div class="game-container"><h2>' + title + '概念动画</h2><canvas id="animCanvas" width="760" height="400"></canvas></div><script>const canvas=document.getElementById("animCanvas");const ctx=canvas.getContext("2d");let t=0;function draw(){ctx.clearRect(0,0,canvas.width,canvas.height);ctx.fillStyle="#303133";ctx.font="bold 20px Microsoft YaHei";ctx.textAlign="center";ctx.fillText("' + title + '",canvas.width/2,30);const colors=["#CCFF00","#67c23a","#e6a23c","#f56c6c","#909399"];for(let i=0;i<5;i++){const x=canvas.width/2+Math.cos(t*0.02+i*1.256)*150;const y=200+Math.sin(t*0.02+i*1.256)*100;const r=20+Math.sin(t*0.05+i)*10;ctx.beginPath();ctx.arc(x,y,r,0,Math.PI*2);ctx.fillStyle=colors[i];ctx.fill();}ctx.beginPath();ctx.strokeStyle="#CCFF00";ctx.lineWidth=2;for(let x=0;x<canvas.width;x++){const y=300+Math.sin(x*0.02+t*0.03)*30;if(x===0)ctx.moveTo(x,y);else ctx.lineTo(x,y);}ctx.stroke();ctx.fillStyle="#909399";ctx.font="14px Microsoft YaHei";ctx.fillText("动态动画演示",canvas.width/2,380);t++;requestAnimationFrame(draw);}draw();<\/script>'
        }
      ],
      interactiveGames: [
        {
          title: `${title}知识问答`,
          description: `通过问答形式测试学生对${title}的理解`,
          playable: true,
          gameCode: `${title.toLowerCase().replace(/\s+/g, '-')}-knowledge-quiz`,
          htmlCode: '<div class="game-container"><h2>' + title + '知识问答</h2><div id="gameArea"><div class="question" id="questionBox"><div id="questionText">加载中...</div><div class="options" id="optionsBox"></div></div><div class="score" id="scoreText">得分: 0</div><div class="feedback" id="feedback"></div></div></div><script>const questions=[{q:"关于' + title + '的基本概念是什么？",options:["核心定义","无关内容","错误概念","模糊描述"],answer:0},{q:"' + title + '最重要的应用场景是？",options:["无关场景","关键应用","过时用法","错误场景"],answer:1},{q:"学习' + title + '的关键要点是？",options:["不重要的","次要的","核心要点","无关的"],answer:2}];let current=0,score=0,answered=false;function render(){if(current>=questions.length){document.getElementById("questionBox").innerHTML="<h3>游戏结束!</h3><p class=\"score\">最终得分: "+score+"/"+questions.length+"</p><button class=\"btn btn-primary\" onclick=\"current=0;score=0;answered=false;render()\">再玩一次</button>";return;}const q=questions[current];document.getElementById("questionText").textContent="第"+(current+1)+"题: "+q.q;const box=document.getElementById("optionsBox");box.innerHTML="";document.getElementById("feedback").textContent="";document.getElementById("feedback").className="feedback";answered=false;q.options.forEach((opt,i)=>{const btn=document.createElement("button");btn.className="option";btn.textContent=String.fromCharCode(65+i)+". "+opt;btn.onclick=()=>selectOption(i);box.appendChild(btn);});document.getElementById("scoreText").textContent="得分: "+score;}function selectOption(idx){if(answered)return;answered=true;const q=questions[current];const btns=document.querySelectorAll(".option");btns.forEach((b,i)=>{if(i===q.answer)b.className="option correct";else if(i===idx&&idx!==q.answer)b.className="option wrong";});const fb=document.getElementById("feedback");if(idx===q.answer){score++;fb.textContent="正确!";fb.className="feedback correct";}else{fb.textContent="错误，正确答案是 "+String.fromCharCode(65+q.answer);fb.className="feedback wrong";}document.getElementById("scoreText").textContent="得分: "+score;setTimeout(()=>{current++;render();},1500);}render();<\/script>'
        }
      ],
      creativeMethods: [
        `${title}歌曲创作：将${title}的知识点编成歌曲，帮助学生记忆`,
        `${title}实验探究：通过实验或探究活动帮助学生理解${title}的原理`,
        `${title}实地考察：组织学生实地考察${title}在实际中的应用`
      ]
    }
  }
}

// 在前端生成PPT
const generatePPTInFrontend = async (data, aiContent = null) => {
  // 生成AI内容
  const content = aiContent || await generateAIContent(data)

  // 创建PPT
  const pptx = new PptxGenJS()

  // 添加封面页
  const coverSlide = pptx.addSlide()
  const filteredTitle = (data.title || '教案标题').replace(/[\x00-\x1F\x7F]/g, '').replace(/[^\u4e00-\u9fa5a-zA-Z0-9\s，。！？；："\'（）【】《》、.,!?;:"\'()\[\]<>]/g, '')
  coverSlide.addText(filteredTitle, {
    x: 0,
    y: 0,
    w: '100%',
    h: '100%',
    align: 'center',
    valign: 'middle',
    font_size: 24,
    bold: true,
    color: '000000'
  })

  // 解析AI内容，提取章节
  const sections = []
  const lines = content.split('\n')
  let currentSection = null

  // 清理内容，移除多余的空白字符和特殊字符
  const cleanedLines = lines.map(line => {
    let cleaned = line.replace(/\s+/g, ' ').trim()
    cleaned = cleaned.replace(/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/g, '')
    return cleaned
  }).filter(line => line.length > 0)

  cleanedLines.forEach(line => {
    // 检查是否是一级标题（以#开头）
    if (line.startsWith('# ')) {
      // 创建新的章节
      currentSection = {
        title: line.replace(/^#\s*/, ''),
        content: []
      }
      sections.push(currentSection)
    } else if (currentSection) {
      // 添加内容到当前章节
      currentSection.content.push(line)
    }
  })

  // 如果没有章节，创建一个默认章节
  if (sections.length === 0) {
    currentSection = {
      title: '内容',
      content: cleanedLines
    }
    sections.push(currentSection)
  }

  // 添加目录页
  if (sections.length > 0) {
    const tocSlide = pptx.addSlide()
    tocSlide.addText('目录', {
      x: 0,
      y: 0,
      w: '100%',
      h: 1,
      align: 'center',
      font_size: 20,
      bold: true,
      color: '000000'
    })

    let yPosition = 1.5
    const maxLinesPerSlide = 8
    let currentTocSlide = tocSlide
    let lineCount = 0

    sections.forEach((section, index) => {
      if (lineCount >= maxLinesPerSlide) {
        // 创建新的目录页
        currentTocSlide = pptx.addSlide()
        currentTocSlide.addText('目录（续）', {
          x: 0,
          y: 0,
          w: '100%',
          h: 1,
          align: 'center',
          font_size: 18,
          bold: true,
          color: '000000'
        })
        yPosition = 1.5
        lineCount = 0
      }

      currentTocSlide.addText(`${index + 1}. ${section.title}`, {
        x: 1,
        y: yPosition,
        w: '80%',
        h: 0.5,
        align: 'left',
        font_size: 14,
        color: '000000'
      })
      yPosition += 0.6
      lineCount++
    })
  }

  // 添加内容页，确保每页内容分配均匀
  sections.forEach((section, sectionIndex) => {
    const maxCharsPerSlide = 150
    const maxLinesPerSlide = 5

    // 分割内容为多个幻灯片
    const slidesContent = []
    let currentContent = []
    let currentCharCount = 0

    section.content.forEach(line => {
      const lineCharCount = line.length

      // 对于较长的行，单独处理
      if (lineCharCount > maxCharsPerSlide / 2) {
        // 如果当前页已有内容，先保存
        if (currentContent.length > 0) {
          slidesContent.push(currentContent)
          currentContent = []
          currentCharCount = 0
        }

        // 将长行分割成多行
        const words = line.split(' ')
        let currentLine = ''

        words.forEach(word => {
          if (currentLine.length + word.length + 1 > maxCharsPerSlide) {
            currentContent.push(currentLine)
            currentLine = word
          } else {
            currentLine += (currentLine ? ' ' : '') + word
          }
        })

        if (currentLine) {
          currentContent.push(currentLine)
        }

        // 如果当前内容达到最大行数，保存并创建新页
        if (currentContent.length >= maxLinesPerSlide) {
          slidesContent.push(currentContent)
          currentContent = []
          currentCharCount = 0
        }
      } else {
        // 正常处理短行
        if (currentCharCount + lineCharCount > maxCharsPerSlide || currentContent.length >= maxLinesPerSlide) {
          // 当前页已满，添加到幻灯片内容列表
          slidesContent.push(currentContent)
          currentContent = [line]
          currentCharCount = lineCharCount
        } else {
          // 添加到当前页
          currentContent.push(line)
          currentCharCount += lineCharCount
        }
      }
    })

    // 添加最后一页内容
    if (currentContent.length > 0) {
      slidesContent.push(currentContent)
    }

    // 创建幻灯片
    slidesContent.forEach((slideContent, slideIndex) => {
      const slide = pptx.addSlide()
      const slideTitle = slideIndex === 0
        ? `${sectionIndex + 1}. ${section.title}`
        : `${sectionIndex + 1}. ${section.title} (${slideIndex + 1})`

      slide.addText(slideTitle, {
        x: 0,
        y: 0.5,
        w: '100%',
        h: 1,
        align: 'center',
        font_size: 18,
        bold: true,
        color: '000000'
      })

      slide.addText(slideContent.join('\n'), {
        x: 3,
        y: 2,
        w: '40%',
        h: '45%',
        align: 'left',
        font_size: 14,
        color: '000000'
      })
    })
  })

  // 添加总结页
  const summarySlide = pptx.addSlide()
  summarySlide.addText('总结', {
    x: 0,
    y: 0.5,
    w: '100%',
    h: 1,
    align: 'center',
    font_size: 20,
    bold: true,
    color: '000000'
  })

  // 提取各章节的核心内容作为总结
  let summaryContent = ''
  let summaryLineCount = 0
  const maxSummaryLines = 10 // 总结页最大行数，进一步减少以避免拥挤

  sections.forEach((section, index) => {
    // 检查是否超过最大行数
    if (summaryLineCount >= maxSummaryLines) {
      summaryContent += '...（内容过多，已省略部分章节）\n'
      return
    }

    summaryContent += `${index + 1}. ${section.title}\n`
    summaryLineCount++

    // 提取前1-2行作为总结
    const summaryLines = section.content.slice(0, 1) // 进一步减少每行提取的行数
    summaryLines.forEach(line => {
      if (line.trim() && summaryLineCount < maxSummaryLines) {
        summaryContent += `  - ${line}\n`
        summaryLineCount++
      }
    })

    if (summaryLineCount < maxSummaryLines) {
      summaryContent += '\n'
      summaryLineCount++
    }
  })

  summarySlide.addText(summaryContent, {
    x: 1.5,
    y: 2,
    w: '70%',
    h: '60%',
    align: 'left',
    font_size: 14,
    color: '000000'
  })

  // 生成PPT并下载
  pptx.writeFile({ fileName: `${data.title || '教案'}.pptx` })

  // 返回成功信息
  return {
    code: 1,
    message: 'success',
    response: {
      title: 'PPT生成成功',
      pptUrl: 'local'
    }
  }
}

/**
 * 发送消息并获取教案内容（流式响应）
 * @param {Object} data - 包含所有参数的对象
 * @param {Function} onChunk - 接收流式数据的回调函数
 * @returns {Promise<Object>} - 包含生成教案的响应对象
 */
export const sendMessage = async (data, onChunk) => {
  try {
    console.log('sendMessage called with data:', data)

    // 测试模式：直接在前端生成PPT
    if (testMode) {
      console.log('Testing mode enabled...')

      // 根据请求类型处理
      if (data.generatePPT) {
        // 在前端生成PPT
        console.log('Generating PPT in frontend...')
        if (onChunk) {
          onChunk('生成中...', '生成中...')
        }
        // 模拟延迟
        await new Promise(resolve => setTimeout(resolve, 1000))
        const result = await generatePPTInFrontend(data)
        console.log('PPT generated successfully:', result)
        return result
      } else if (data.generateContent) {
        // 生成模拟的多样化内容
        console.log('Generating diverse content in frontend...')
        const mockDiverseContent = generateMockDiverseContent(data)

        // 模拟流式生成效果
        if (onChunk) {
          let currentContent = JSON.stringify(mockDiverseContent)
          onChunk(currentContent, currentContent)
        }

        // 返回模拟的多样化内容响应
        const mockContentResponse = {
          code: 1,
          message: 'success',
          response: mockDiverseContent
        }
        console.log('Returning mock diverse content response:', mockContentResponse)
        return mockContentResponse
      } else {
        // 生成模拟的教案内容
        const mockContent = generateMockContent(data)

        // 模拟流式生成效果
        if (onChunk) {
          let currentContent = ''
          const words = mockContent.split('')
          for (let i = 0; i < words.length; i++) {
            currentContent += words[i]
            onChunk(words[i], currentContent)
            // 随机延迟，模拟真实的生成速度
            await new Promise(resolve => setTimeout(resolve, Math.random() * 50 + 10))
          }
        }

        // 返回模拟的教案生成响应
        const mockLessonResponse = {
          code: 1,
          message: 'success',
          response: {
            title: mockContent
          }
        }
        console.log('Returning mock lesson response:', mockLessonResponse)
        return mockLessonResponse
      }
    }

    console.log('Sending API request...')
    // 使用原生的 fetch API 来处理流式响应
    try {
      // 从 localStorage 获取 token
      const token = localStorage.getItem('token') || ''
      console.log('Token:', token)

      // 构建请求
      const response = await fetch(DEEPSEEK_PAGE_API_BASE_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'token': token
        },
        body: JSON.stringify(data)
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      let completeContent = ''

      // 检查是否是ReadableStream
      if (response.body && typeof response.body.getReader === 'function') {
        const reader = response.body.getReader()

        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          // 将 Uint8Array 转换为字符串
          const chunk = new TextDecoder('utf-8').decode(value, { stream: true })
          completeContent += chunk

          // 立即调用回调函数，实现流式展示
          if (onChunk) {
            onChunk(chunk, completeContent)
          }
        }
      } else {
        // 对于其他类型的响应，直接获取文本
        completeContent = await response.text()
        if (onChunk) {
          onChunk(completeContent, completeContent)
        }
      }

      // 对于PPT生成请求，直接使用完整内容创建PPT，不尝试解析JSON
      if (data.generatePPT) {
        // 直接使用完整内容创建PPT
        await generatePPTInFrontend(data, completeContent)
        return { code: 1, message: 'success', response: { title: 'PPT生成成功', pptUrl: 'local' } }
      }

      try {
        // 对于非PPT生成请求，尝试解析完整响应
        // 先检查响应内容是否以"{"开头，确保是有效的JSON格式
        if (completeContent.trim().startsWith('{')) {
          const parsedResponse = JSON.parse(completeContent)
          return parsedResponse
        } else {
          // 如果不是JSON格式，直接返回完整内容作为title
          return { code: 1, message: 'success', response: { title: completeContent } }
        }
      } catch (e) {
        console.error('Failed to parse response:', e)
        // 如果解析失败，返回完整内容作为title
        return { code: 1, message: 'success', response: { title: completeContent } }
      }
    } catch (error) {
      console.error('Stream error:', error)
      // 捕获所有错误，返回错误信息
      return { code: 0, message: '请求失败: ' + error.message }
    }
  } catch (error) {
    console.error('Failed to send message:', error)
    // 捕获所有错误，返回错误信息
    return { code: 0, message: '请求失败: ' + error.message }
  }
}

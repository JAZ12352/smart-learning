# 人工智能领域垂直知识库切片

> 用途：本知识库切片用于 RAG 检索增强生成，为多智能体协同决策系统提供「人工智能」垂直领域的专业知识底座。
> 覆盖范围：机器学习基础、深度学习、神经网络、自然语言处理、计算机视觉、强化学习、大语言模型。
> 适用场景：高等教育人工智能专业人才培养、企业 AI 工程师内训、转岗培训。

---

## 一、机器学习基础

### 1.1 监督学习（Supervised Learning）
监督学习是从带标签的训练数据中学习输入到输出的映射关系。核心任务包括分类（输出离散类别）和回归（输出连续值）。

**典型算法**：
- 线性回归（Linear Regression）：`y = wx + b`，通过最小化均方误差 MSE = (1/n)Σ(yi - ŷi)² 求解
- 逻辑回归（Logistic Regression）：用于二分类，输出概率 p = 1/(1+e^(-z))，其中 z = wx + b
- 决策树（Decision Tree）：基于特征划分递归构建树结构，信息增益 = H(D) - H(D|A)
- 支持向量机（SVM）：寻找最大间隔超平面，核函数将数据映射到高维空间
- 随机森林（Random Forest）：多棵决策树集成，通过 Bagging 降低方差

**评估指标**：
- 分类：准确率 Accuracy = (TP+TN)/(TP+TN+FP+FN)，精确率 Precision = TP/(TP+FP)，召回率 Recall = TP/(TP+FN)，F1 = 2·P·R/(P+R)
- 回归：MSE、RMSE、MAE、R² = 1 - SS_res/SS_tot

### 1.2 无监督学习（Unsupervised Learning）
无监督学习从无标签数据中发现内在结构。

**典型算法**：
- K-Means 聚类：最小化簇内平方和 SSE = ΣΣ||x - μ||²，需预先指定 K 值
- 层次聚类：自底向上（AGNES）或自顶向下（DIANA）构建聚类树
- DBSCAN：基于密度的聚类，可发现任意形状簇，无需指定 K
- PCA 降维：通过协方差矩阵特征值分解，保留前 k 个主成分

### 1.3 模型评估与选择
- 偏差-方差权衡：Bias² + Variance + 不可约误差 = 总误差
- 过拟合：训练误差低、测试误差高，解决方法包括正则化（L1/L2）、Dropout、数据增强、早停
- 交叉验证：K 折交叉验证评估模型泛化能力

---

## 二、深度学习

### 2.1 神经网络基础
**前馈神经网络（MLP）**：
- 每层计算：z = Wx + b，a = σ(z)
- 激活函数：ReLU(x) = max(0, x)，Sigmoid(x) = 1/(1+e^(-x))，Tanh(x) = (e^x - e^(-x))/(e^x + e^(-x))
- 反向传播：链式法则计算梯度 ∂L/∂W = ∂L/∂a · ∂a/∂z · ∂z/∂W

**损失函数**：
- 均方误差 MSE：L = (1/n)Σ(yi - ŷi)²
- 交叉熵 Cross-Entropy：L = -Σ yi·log(ŷi)
- 二元交叉熵 BCE：L = -[y·log(p) + (1-y)·log(1-p)]

### 2.2 优化算法
- SGD：θ ← θ - η·∇L，学习率 η 典型值 0.001~0.1
- Momentum：v ← βv + ∇L，θ ← θ - ηv，β 常取 0.9
- Adam：结合一阶矩和二阶矩估计，β1=0.9, β2=0.999, ε=1e-8

### 2.3 卷积神经网络（CNN）
**核心组件**：
- 卷积层：提取局部特征，输出尺寸 = (W - F + 2P)/S + 1
- 池化层：下采样，Max Pooling 取最大值，Average Pooling 取平均
- 经典架构：LeNet（1998）、AlexNet（2012）、VGG（2014）、ResNet（2015，残差连接解决梯度消失）

### 2.4 循环神经网络（RNN）
**基础结构**：h_t = tanh(W_hh·h_(t-1) + W_xh·x_t + b_h)
**问题**：梯度消失/爆炸，难以捕获长程依赖
**改进**：
- LSTM：引入门控机制（输入门、遗忘门、输出门）和细胞状态
- GRU：简化版 LSTM，重置门和更新门
- Transformer（2017）：基于自注意力机制，完全摒弃循环结构

---

## 三、Transformer 与大语言模型

### 3.1 自注意力机制（Self-Attention）
**公式**：Attention(Q, K, V) = softmax(QK^T/√d_k)·V
- Q = X·W_Q，K = X·W_K，V = X·W_V
- 缩放因子 √d_k 防止点积过大导致 softmax 梯度消失
- 多头注意力：并行多个 attention head，拼接后线性变换

### 3.2 Transformer 架构
- Encoder：6 层堆叠，每层含多头自注意力 + 前馈网络 + 残差连接 + LayerNorm
- Decoder：额外加入交叉注意力层，使用掩码防止看到未来信息
- 位置编码：正弦/余弦函数 PE(pos, 2i) = sin(pos/10000^(2i/d))

### 3.3 大语言模型（LLM）
**代表模型**：
- GPT 系列：Decoder-only，自回归生成，GPT-3 有 175B 参数
- BERT：Encoder-only，双向注意力，预训练任务为 MLM 和 NSP
- T5：Encoder-Decoder，统一为 text-to-text 框架
- LLaMA：开源模型，参数从 7B 到 70B

**训练技术**：
- 预训练：海量无监督文本，目标为下一 token 预测或掩码语言建模
- 指令微调（SFT）：使用指令-响应对进行监督微调
- RLHF：基于人类反馈的强化学习，使用 PPO 算法优化奖励模型
- LoRA：低秩适配，冻结预训练权重，仅训练低秩矩阵 ΔW = BA

### 3.4 提示工程（Prompt Engineering）
- Few-shot：在提示中提供少量示例引导模型
- Chain-of-Thought（CoT）：引导模型逐步推理，"Let's think step by step"
- ReAct：结合推理（Reasoning）和行动（Acting），调用外部工具

---

## 四、自然语言处理

### 4.1 文本表示
- 词袋模型（BoW）：忽略词序，统计词频
- TF-IDF：TF·IDF = (词频/文档长度) · log(总文档数/包含该词的文档数)
- Word2Vec：CBOW 和 Skip-gram，学习词向量
- 上下文向量：BERT、GPT 等基于上下文生成动态词向量

### 4.2 核心任务
- 文本分类：情感分析、主题分类
- 命名实体识别（NER）：识别文本中的人名、地名、机构名等
- 机器翻译：Seq2Seq + Attention
- 问答系统：抽取式问答、生成式问答
- 文本摘要：抽取式、生成式

---

## 五、计算机视觉

### 5.1 图像分类
- ImageNet 数据集：1000 类，130 万张图像
- 数据增强：随机裁剪、翻转、颜色抖动、MixUp、CutMix

### 5.2 目标检测
- 两阶段：R-CNN、Fast R-CNN、Faster R-CNN（RPN 生成候选框）
- 单阶段：YOLO（You Only Look Once）、SSD
- 评估指标：mAP = 平均精度均值，IoU = 交集面积/并集面积

### 5.3 图像分割
- 语义分割：FCN、U-Net、DeepLab
- 实例分割：Mask R-CNN
- 全景分割：合并语义分割和实例分割

---

## 六、强化学习

### 6.1 基础概念
- 马尔可夫决策过程（MDP）：(S, A, P, R, γ)
- 状态价值函数 V_π(s) = E[Σγ^t·r_t | s_0=s]
- 动作价值函数 Q_π(s,a) = E[Σγ^t·r_t | s_0=s, a_0=a]
- 贝尔曼方程：V(s) = Σ_a π(a|s) Σ_s' P(s'|s,a)[R(s,a,s') + γV(s')]

### 6.2 经典算法
- Q-Learning：Q(s,a) ← Q(s,a) + α[r + γ·max_a' Q(s',a') - Q(s,a)]
- DQN：用神经网络近似 Q 函数，经验回放 + 目标网络
- Policy Gradient：∇J(θ) = E[∇log π_θ(a|s)·G_t]
- PPO：Proximal Policy Optimization，Clipped Surrogate Objective
- AlphaGo/AlphaZero：蒙特卡洛树搜索 + 深度强化学习

---

## 七、AI 工程实践

### 7.1 模型部署
- ONNX：开放神经网络交换格式，跨框架部署
- TensorRT：NVIDIA 推理加速库
- 量化：FP32 → FP16/INT8，减少内存占用和推理延迟
- 蒸馏：大模型（教师）指导小模型（学生）训练

### 7.2 分布式训练
- 数据并行：每张卡处理不同数据批次，AllReduce 同步梯度
- 模型并行：将大模型拆分到多张卡
- 流水线并行：按层切分，micro-batch 流水线
- ZeRO 优化：分片优化器状态、梯度、参数

### 7.3 MLOps 工具链
- 实验跟踪：MLflow、Weights & Biases
- 数据版本：DVC
- 模型服务：TF Serving、TorchServe、Triton
- 特征存储：Feast

---

## 八、AI 伦理与安全

### 8.1 公平性
- 偏见检测：Demographic Parity、Equal Opportunity
- 缓解措施：数据重采样、对抗去偏见、公平约束优化

### 8.2 可解释性
- LIME：局部可解释模型-不可知解释
- SHAP：基于 Shapley 值的特征贡献
- Attention 可视化：分析 Transformer 注意力权重

### 8.3 安全与隐私
- 对抗样本：FGSM、PGD 攻击，对抗训练防御
- 差分隐私：在训练数据中添加噪声保护隐私
- 联邦学习：数据不动模型动，保护数据主权

### 8.4 大模型幻觉防控
- RAG 检索增强：基于外部知识库约束生成
- 多智能体交叉验证：多 Agent 辩论与事实核验
- 知识溯源：标注生成内容的来源依据
- 人类反馈：RLHF 对齐人类价值观

---

## 知识库元数据

- **领域**：人工智能（垂直领域）
- **适用对象**：高等教育 AI 专业学生、企业 AI 工程师转岗培训
- **知识深度**：基础到进阶
- **数据合规**：本知识库内容为公开技术知识，无个人隐私数据
- **版本**：v1.0
- **用途**：多智能体协同决策系统的 RAG 检索底座，为「领域专家 Agent」「辩论与交叉验证 Agent」提供事实核验依据

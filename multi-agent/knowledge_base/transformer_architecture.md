# Transformer 架构详解（垂直领域知识库切片 · 大语言模型分册）

> 用途：为 RAG 检索提供 Transformer 架构的权威定义、核心机制与训练方法，支撑事实性核验。
> 覆盖范围：自注意力机制、多头注意力、位置编码、编码器-解码器、GPT/BERT 架构。

---

## 一、Transformer 架构总览

### 1.1 核心创新
Transformer（Vaswani et al., 2017, "Attention is All You Need"）完全基于注意力机制，摒弃了 RNN/CNN 的序列依赖。核心优势：
- **并行计算**：所有位置可同时处理，训练速度远超 RNN
- **长距离依赖**：自注意力直接建模任意距离的 token 交互
- **可扩展性**：参数量和计算量随层数/维度线性增长

### 1.2 整体架构
原始 Transformer 为编码器-解码器结构：
- **编码器**：N 层堆叠，每层包含多头自注意力 + 前馈网络 + 残差连接 + 层归一化
- **解码器**：N 层堆叠，每层包含掩码多头自注意力 + 编码器-解码器注意力 + 前馈网络
- **N=6**（原始论文），隐藏维度 d_model=512，注意力头数 h=8

---

## 二、自注意力机制（Self-Attention）

### 2.1 缩放点积注意力
对于输入序列 X ∈ R^{n×d}，计算：

Q = XW_Q, K = XW_K, V = XW_V

注意力输出：Attention(Q, K, V) = softmax(QK^T / √d_k) V

其中：
- W_Q, W_K, W_V ∈ R^{d×d_k} 是可训练的投影矩阵
- d_k 是每个头的维度（d_k = d_model / h）
- 除以 √d_k 防止点积过大导致 softmax 梯度消失

### 2.2 注意力权重的含义
softmax(QK^T / √d_k) 的第 (i,j) 个元素表示：位置 i 对位置 j 的关注程度（注意力分数）。

**可视化**：热力图，横轴为被关注位置，纵轴为查询位置。

### 2.3 注意力复杂度
时间复杂度 O(n²·d)，空间复杂度 O(n²)（n 为序列长度）。这是 Transformer 处理长序列的主要瓶颈。

---

## 三、多头注意力（Multi-Head Attention）

### 3.1 核心思想
将注意力分成 h 个独立的"头"，各自在不同的子空间中学习不同的注意力模式。

**计算**：MultiHead(Q, K, V) = Concat(head_1, ..., head_h) W_O
其中 head_i = Attention(QW_Q^i, KW_K^i, VW_V^i)

### 3.2 为什么需要多头
- 不同的头可以关注不同类型的关系（如语法关系、语义关系）
- 原始论文实验：h=8 时效果最好
- 每个头的维度 d_k = d_model / h，保持总计算量不变

### 3.3 输出投影
W_O ∈ R^{d_model×d_model} 将多头拼接后的结果投影回 d_model 维。

---

## 四、位置编码（Positional Encoding）

### 4.1 为什么需要位置编码
自注意力是置换不变的（permutation invariant），即打乱输入顺序不影响输出。需要额外注入位置信息。

### 4.2 正弦位置编码（原始）
PE(pos, 2i) = sin(pos / 10000^{2i/d_model})
PE(pos, 2i+1) = cos(pos / 10000^{2i/d_model})

**优点**：可泛化到训练时未见过的序列长度。

### 4.3 可学习位置编码
BERT 和 GPT 使用可学习的位置嵌入（learned positional embedding），直接训练位置向量。
- 局限：最大长度固定（如 BERT 的 512）
- 优势：在固定长度内通常优于正弦编码

### 4.4 旋转位置编码（RoPE）
LLaMA、Qwen 等现代 LLM 使用 RoPE（Su et al., 2021）：
- 将位置信息编码为旋转矩阵，作用在 Q 和 K 上
- 优势：支持相对位置建模、长度外推

---

## 五、前馈网络与残差连接

### 5.1 位置式前馈网络（FFN）
每层注意力之后，对每个位置独立应用两层全连接网络：
FFN(x) = max(0, xW_1 + b_1)W_2 + b_2

**维度**：内部维度 d_ff = 4 × d_model = 2048（原始论文）

### 5.2 残差连接与层归一化
- 残差：output = LayerNorm(x + Sublayer(x))
- 层归一化：对每个样本的特征维度做归一化
- 作用：缓解深层网络的梯度消失/爆炸

### 5.3 Pre-Norm vs Post-Norm
- **Post-Norm**（原始）：LayerNorm(x + Sublayer(x)) → 训练不稳定
- **Pre-Norm**（GPT-2 起）：x + Sublayer(LayerNorm(x)) → 训练更稳定，现主流

---

## 六、Transformer 变体

### 6.1 仅编码器（BERT 风格）
- 双向注意力：所有位置互相关注
- 预训练任务：MLM + NSP
- 适合：理解任务（分类、NER、问答）

### 6.2 仅解码器（GPT 风格）
- 因果注意力：位置 i 只能关注 ≤i 的位置（下三角掩码）
- 预训练任务：自回归语言建模 P(x_t | x_{<t})
- 适合：生成任务（文本生成、代码生成）

### 6.3 编码器-解码器（T5/BART 风格）
- 编码器处理输入，解码器生成输出
- 适合：序列到序列任务（翻译、摘要）

---

## 七、现代 Transformer 优化

### 7.1 高效注意力
- **Flash Attention**（Dao et al., 2022）：利用 GPU 内存层次优化，速度提升 2-4×
- **GQA（Grouped Query Attention）**：LLaMA 2 使用，多个 Q 头共享 K/V 头，减少 KV 缓存
- **MQA（Multi-Query Attention）**：所有 Q 头共享一组 K/V，更极致的压缩

### 7.2 KV 缓存
推理时缓存已计算的 K/V 向量，避免重复计算。是自回归生成的关键优化。

### 7.3 混合专家（MoE）
将 FFN 层拆分为多个"专家"，每个 token 只激活部分专家。GPT-4 和 Mixtral 使用 MoE。

---

## 八、Transformer 在教育中的应用

### 8.1 智能教学系统
用 BERT/GPT 理解学生提问语义，提供精准的个性化回答。

### 8.2 自动出题
基于 Transformer 的文本生成能力，根据知识点自动生成选择题、填空题、简答题。

### 8.3 作文自动评分
用 BERT 编码作文语义，结合回归头预测分数，实现开放题自动评分。

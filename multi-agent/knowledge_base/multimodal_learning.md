# 多模态学习（垂直领域知识库切片 · 多模态分册）

> 用途：为 RAG 检索提供多模态学习的权威定义、跨模态对齐方法与典型模型，支撑事实性核验。
> 覆盖范围：多模态融合策略、CLIP、视觉语言模型、跨模态检索。

---

## 一、多模态学习基础

### 1.1 什么是多模态
多模态（Multimodal）指涉及多种信息形式（模态）的数据，如文本、图像、音频、视频、传感器数据等。

**常见模态组合**：
- 视觉-语言（Vision-Language）：图像+文本
- 视觉-音频（Visual-Audio）：视频+语音
- 语言-音频（Language-Audio）：文本+语音

### 1.2 多模态融合策略
| 策略 | 描述 | 适用场景 |
|---|---|---|
| 早期融合（Early Fusion） | 在输入层拼接各模态特征 | 模态间高度相关 |
| 晚期融合（Late Fusion） | 各模态独立编码后在决策层融合 | 模态间相对独立 |
| 注意力融合（Attention Fusion） | 用注意力机制动态加权各模态 | 模态间交互复杂 |

### 1.3 跨模态对齐（Cross-Modal Alignment）
将不同模态的表示映射到统一空间，使语义相似的不同模态表示距离相近。

**对比学习对齐**：
- 正样本对：(图像_i, 文本_i) — 同一实例的图文对
- 负样本对：(图像_i, 文本_j) — 不同实例的图文对
- 损失：InfoNCE L = -log exp(sim(v_i, t_i)/τ) / Σ_j exp(sim(v_i, t_j)/τ)

---

## 二、CLIP（Contrastive Language-Image Pre-training）

### 2.1 架构
CLIP（OpenAI, 2021）由两个编码器组成：
- 图像编码器：ViT（Vision Transformer）或 ResNet
- 文本编码器：Transformer

**训练方式**：在 4 亿图文对上对比学习。

### 2.2 对比学习目标
给定 batch 中的 N 个图文对 {(I_i, T_i)}：
- 图像→文本：L_I2T = -Σ_i log exp(sim(I_i, T_i)/τ) / Σ_j exp(sim(I_i, T_j)/τ)
- 文本→图像：L_T2I = -Σ_i log exp(sim(T_i, I_i)/τ) / Σ_j exp(sim(T_i, I_j)/τ)
- 总损失：L = (L_I2T + L_T2I) / 2

### 2.3 零样本分类（Zero-shot Classification）
无需微调即可做图像分类：
1. 为每个类别生成文本提示："a photo of a {class}"
2. 计算图像与所有类别文本的相似度
3. 选择相似度最高的类别作为预测

### 2.4 CLIP 的能力与局限
**能力**：
- 零样本/少样本分类性能接近监督学习
- 跨模态检索（以图搜文、以文搜图）
- 作为下游任务的视觉特征提取器

**局限**：
- 组合理解弱："a horse riding an astronaut" 和 "an astronaut riding a horse" 难以区分
- 细粒度属性理解不足（颜色、数量、空间关系）
- 训练数据偏差（网络图文对质量参差不齐）

---

## 三、视觉语言模型（VLM）

### 3.1 生成式 VLM

**LLaVA（Large Language and Vision Assistant）**：
- 架构：视觉编码器（CLIP ViT）+ 线性投影层 + LLM（Vicuna/LLaMA）
- 训练：两阶段 — ① 特征对齐预训练（冻结视觉编码器和LLM，只训练投影层）；② 视觉指令微调（解冻LLM）
- 输入：图像 token + 文本 token 拼接后送入 LLM

**Qwen-VL / InternVL**：
- 支持多图输入、视觉定位（grounding）、OCR
- 中文多模态能力领先

### 3.2 理解式 VLM
**ViLT（Vision-and-Language Transformer）**：
- 将图像 patch 和文本 token 统一为序列输入 Transformer
- 轻量级，但性能弱于 CLIP-based 方法

### 3.3 视频理解模型
**VideoLLaMA / VideoChat**：
- 在图像 VLM 基础上增加时序建模
- 关键挑战：视频帧数量大 → 计算开销高 → 需要帧采样策略

---

## 四、跨模态检索

### 4.1 任务定义
- 以文搜图（Text-to-Image Retrieval）：给定文本查询，检索最相关的图像
- 以图搜文（Image-to-Text Retrieval）：给定图像查询，检索最相关的文本

### 4.2 评估指标
- **Recall@K**：前 K 个结果中包含正确答案的比例
- **MRR**：正确答案排名的倒数均值
- **nDCG**：考虑排名位置的归一化折损累积增益

### 4.3 典型方法
- CLIP 双塔模型：分别编码图文，计算余弦相似度
- 融合模型：cross-attention 建模图文交互（如 ALBEF）
- 匹配损失：InfoNCE + 难负样本挖掘

---

## 五、多模态在教育中的应用

### 5.1 图文教学内容生成
用 VLM 根据教学文本自动生成配图，或为教材图片生成文字说明。

### 5.2 视频教学内容理解
从教学视频中提取关键帧和文字说明，辅助学生快速定位知识点。

### 5.3 多模态作业评估
对学生提交的图文混合作业（如实验报告中的图表+文字）进行综合评估。

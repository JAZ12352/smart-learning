-- =============================================
-- 高等教育学科管理 + 学科资源 迁移脚本
-- 学历层次: 1=大专 2=本科 3=硕士 4=博士
-- =============================================

-- 1. 给 t_subject 添加高等教育字段
ALTER TABLE t_subject
  ADD COLUMN education_level INT DEFAULT 2 COMMENT '学历层次: 1=大专 2=本科 3=硕士 4=博士',
  ADD COLUMN education_level_name VARCHAR(32) DEFAULT '本科' COMMENT '学历层次名称',
  ADD COLUMN category VARCHAR(50) COMMENT '学科门类(工学/理学/文学/管理学/医学/法学/教育学等)',
  ADD COLUMN description TEXT COMMENT '学科描述';

-- 2. 更新现有 K12 数据：把 1-6 年级设为大专，7-12 年级设为本科
UPDATE t_subject SET education_level = 1, education_level_name = '大专' WHERE level BETWEEN 1 AND 6 AND education_level IS NULL;
UPDATE t_subject SET education_level = 2, education_level_name = '本科' WHERE level >= 7 AND education_level IS NULL;

-- 3. 创建学科资源关联表
CREATE TABLE IF NOT EXISTS t_subject_resource (
  id INT AUTO_INCREMENT PRIMARY KEY,
  subject_id INT NOT NULL COMMENT '学科ID',
  source_type VARCHAR(50) NOT NULL DEFAULT 'manual' COMMENT '资源来源: generated/knowledge_base/document/url/manual',
  source_id INT COMMENT '关联的外部资源ID',
  title VARCHAR(500) NOT NULL COMMENT '资源标题',
  summary TEXT COMMENT '资源摘要',
  source VARCHAR(200) COMMENT '来源标识(agent名称/上传者/URL域名)',
  content LONGTEXT COMMENT '完整内容',
  format VARCHAR(20) DEFAULT 'markdown' COMMENT '内容格式: markdown/html/plain',
  click_count INT DEFAULT 0 COMMENT '点击次数',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted BIT(1) DEFAULT b'0',
  INDEX idx_subject_id (subject_id),
  INDEX idx_source (source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学科资源关联表';

-- 4. t_subject 加树形分层字段
ALTER TABLE t_subject
  ADD COLUMN parent_id INT DEFAULT 0 COMMENT '父级ID(0=根节点)',
  ADD COLUMN subject_type INT DEFAULT 4 COMMENT '节点类型: 1=学科门类 2=一级学科 3=专业 4=课程';

-- 5. 基础学科树种子数据（本科层次）
-- 学科门类 (subject_type=1)
INSERT INTO t_subject(id, name, education_level, education_level_name, subject_type, category, deleted) VALUES
(100, '工学', 2, '本科', 1, '工学', 0),
(101, '理学', 2, '本科', 1, '理学', 0),
(102, '管理学', 2, '本科', 1, '管理学', 0),
(103, '文学', 2, '本科', 1, '文学', 0),
(104, '医学', 2, '本科', 1, '医学', 0),
(105, '法学', 2, '本科', 1, '法学', 0),
(106, '经济学', 2, '本科', 1, '经济学', 0),
(107, '教育学', 2, '本科', 1, '教育学', 0)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 一级学科 (subject_type=2)
INSERT INTO t_subject(id, name, education_level, education_level_name, subject_type, parent_id, category, deleted) VALUES
(200, '计算机科学与技术', 2, '本科', 2, 100, '工学', 0),
(201, '软件工程', 2, '本科', 2, 100, '工学', 0),
(202, '电子信息工程', 2, '本科', 2, 100, '工学', 0),
(203, '机械工程', 2, '本科', 2, 100, '工学', 0),
(204, '土木工程', 2, '本科', 2, 100, '工学', 0),
(205, '自动化', 2, '本科', 2, 100, '工学', 0),
(206, '人工智能', 2, '本科', 2, 100, '工学', 0),
(207, '数学', 2, '本科', 2, 101, '理学', 0),
(208, '物理学', 2, '本科', 2, 101, '理学', 0),
(209, '化学', 2, '本科', 2, 101, '理学', 0),
(210, '统计学', 2, '本科', 2, 101, '理学', 0),
(211, '工商管理', 2, '本科', 2, 102, '管理学', 0),
(212, '管理科学与工程', 2, '本科', 2, 102, '管理学', 0)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 专业 (subject_type=3)
INSERT INTO t_subject(id, name, education_level, education_level_name, subject_type, parent_id, category, deleted) VALUES
(300, '软件工程', 2, '本科', 3, 200, '工学', 0),
(301, '网络工程', 2, '本科', 3, 200, '工学', 0),
(302, '信息安全', 2, '本科', 3, 200, '工学', 0),
(303, '数据科学与大数据技术', 2, '本科', 3, 200, '工学', 0),
(304, '智能科学与技术', 2, '本科', 3, 206, '工学', 0),
(305, '机器人工程', 2, '本科', 3, 206, '工学', 0),
(306, '应用数学', 2, '本科', 3, 207, '理学', 0),
(307, '信息与计算科学', 2, '本科', 3, 207, '理学', 0),
(308, '会计学', 2, '本科', 3, 211, '管理学', 0),
(309, '市场营销', 2, '本科', 3, 211, '管理学', 0)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 课程 (subject_type=4)
INSERT INTO t_subject(id, name, education_level, education_level_name, level, level_name, subject_type, parent_id, category, deleted) VALUES
(4001, '高等数学A', 2, '本科', 1, '第一年', 4, 300, '工学', 0),
(4002, '线性代数', 2, '本科', 1, '第一年', 4, 300, '工学', 0),
(4003, 'C语言程序设计', 2, '本科', 1, '第一年', 4, 300, '工学', 0),
(4004, '数据结构', 2, '本科', 2, '第二年', 4, 300, '工学', 0),
(4005, '面向对象程序设计', 2, '本科', 2, '第二年', 4, 300, '工学', 0),
(4006, '数据库原理', 2, '本科', 2, '第二年', 4, 300, '工学', 0),
(4007, '操作系统', 2, '本科', 3, '第三年', 4, 300, '工学', 0),
(4008, '计算机网络', 2, '本科', 3, '第三年', 4, 300, '工学', 0),
(4009, '软件工程导论', 2, '本科', 3, '第三年', 4, 300, '工学', 0),
(4010, '毕业设计', 2, '本科', 4, '第四年', 4, 300, '工学', 0),
(4011, '概率论与数理统计', 2, '本科', 1, '第一年', 4, 303, '工学', 0),
(4012, 'Python程序设计', 2, '本科', 1, '第一年', 4, 303, '工学', 0),
(4013, '机器学习', 2, '本科', 2, '第二年', 4, 303, '工学', 0),
(4014, '人工智能导论', 2, '本科', 1, '第一年', 4, 304, '工学', 0),
(4015, '深度学习', 2, '本科', 2, '第二年', 4, 304, '工学', 0),
(4016, '自然语言处理', 2, '本科', 3, '第三年', 4, 304, '工学', 0),
(4017, '会计学基础', 2, '本科', 1, '第一年', 4, 308, '管理学', 0),
(4018, '财务管理', 2, '本科', 2, '第二年', 4, 308, '管理学', 0),
(4019, '数学分析', 2, '本科', 1, '第一年', 4, 306, '理学', 0),
(4020, '高等代数', 2, '本科', 1, '第一年', 4, 306, '理学', 0)
ON DUPLICATE KEY UPDATE name=VALUES(name);
-- 哲学、经济学、法学、教育学、文学、历史学、理学、工学、农学、医学、管理学、艺术学、军事学、交叉学科

-- 6. 清理 K12 旧数据
UPDATE t_subject SET deleted = 1 WHERE education_level IS NULL OR education_level NOT IN (1,2,3,4);
UPDATE t_subject SET deleted = 1 WHERE level BETWEEN 1 AND 12 AND subject_type IS NULL;

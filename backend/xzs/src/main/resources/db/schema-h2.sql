-- ===========================================
-- H2 完整数据库初始化脚本（MySQL 兼容模式）
-- 由 application-h2.yml 自动加载执行
-- 包含所有核心表 + AI 扩展表
-- ===========================================

-- =============================================
-- 第一部分：核心系统表
-- =============================================

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_uuid VARCHAR(36),
  user_name VARCHAR(64) NOT NULL,
  password VARCHAR(512),
  real_name VARCHAR(64),
  age INT,
  sex INT DEFAULT 1,
  birth_day TIMESTAMP,
  user_level INT DEFAULT 1,
  phone VARCHAR(32),
  role INT DEFAULT 1,
  status INT DEFAULT 1,
  image_path VARCHAR(255),
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_active_time TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE,
  wx_open_id VARCHAR(128),
  major_id INT,
  UNIQUE (user_name)
);

-- 学科表
CREATE TABLE IF NOT EXISTS t_subject (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  education_level INT DEFAULT 2,
  education_level_name VARCHAR(32) DEFAULT '本科',
  level INT DEFAULT 1,
  level_name VARCHAR(32),
  category VARCHAR(50),
  description TEXT,
  item_order INT DEFAULT 0,
  parent_id INT DEFAULT 0,
  subject_type INT DEFAULT 1,
  deleted BOOLEAN DEFAULT FALSE
);

-- 文本内容表（题库/试卷共用）
CREATE TABLE IF NOT EXISTS t_text_content (
  id INT AUTO_INCREMENT PRIMARY KEY,
  content CLOB,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 试题表
CREATE TABLE IF NOT EXISTS t_question (
  id INT AUTO_INCREMENT PRIMARY KEY,
  question_type INT NOT NULL,
  subject_id INT,
  score INT DEFAULT 0,
  grade_level INT DEFAULT 1,
  difficult INT DEFAULT 1,
  correct VARCHAR(512),
  info_text_content_id INT,
  create_user INT,
  status INT DEFAULT 1,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE
);

-- 试卷表
CREATE TABLE IF NOT EXISTS t_exam_paper (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  subject_id INT,
  paper_type INT DEFAULT 1,
  grade_level INT DEFAULT 1,
  score INT DEFAULT 100,
  question_count INT DEFAULT 0,
  suggest_time INT DEFAULT 120,
  limit_start_time TIMESTAMP,
  limit_end_time TIMESTAMP,
  frame_text_content_id INT,
  create_user INT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE,
  task_exam_id INT
);

-- 试卷答题记录表
CREATE TABLE IF NOT EXISTS t_exam_paper_answer (
  id INT AUTO_INCREMENT PRIMARY KEY,
  exam_paper_id INT,
  paper_name VARCHAR(200),
  paper_type INT DEFAULT 1,
  subject_id INT,
  system_score INT DEFAULT 0,
  user_score INT DEFAULT 0,
  paper_score INT DEFAULT 0,
  question_correct INT DEFAULT 0,
  question_count INT DEFAULT 0,
  do_time INT DEFAULT 0,
  status INT DEFAULT 1,
  create_user INT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  task_exam_id INT
);

-- 试卷题目作答明细表
CREATE TABLE IF NOT EXISTS t_exam_paper_question_customer_answer (
  id INT AUTO_INCREMENT PRIMARY KEY,
  question_id INT,
  exam_paper_id INT,
  exam_paper_answer_id INT,
  question_type INT,
  subject_id INT,
  customer_score INT DEFAULT 0,
  question_score INT DEFAULT 0,
  question_text_content_id INT,
  answer VARCHAR(2048),
  text_content_id INT,
  do_right BOOLEAN DEFAULT FALSE,
  create_user INT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  item_order INT DEFAULT 0
);

-- 任务考试表
CREATE TABLE IF NOT EXISTS t_task_exam (
  id INT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200),
  grade_level INT DEFAULT 1,
  frame_text_content_id INT,
  create_user INT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE,
  create_user_name VARCHAR(64)
);

-- 任务考试作答表
CREATE TABLE IF NOT EXISTS t_task_exam_customer_answer (
  id INT AUTO_INCREMENT PRIMARY KEY,
  task_exam_id INT,
  create_user INT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  text_content_id INT
);

-- 消息表
CREATE TABLE IF NOT EXISTS t_message (
  id INT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200),
  content TEXT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  send_user_id INT,
  send_user_name VARCHAR(64),
  send_real_name VARCHAR(64),
  receive_user_count INT DEFAULT 0,
  read_count INT DEFAULT 0
);

-- 消息用户关联表
CREATE TABLE IF NOT EXISTS t_message_user (
  id INT AUTO_INCREMENT PRIMARY KEY,
  message_id INT,
  receive_user_id INT,
  receive_user_name VARCHAR(64),
  receive_real_name VARCHAR(64),
  readed BOOLEAN DEFAULT FALSE,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  read_time TIMESTAMP
);

-- 用户Token表
CREATE TABLE IF NOT EXISTS t_user_token (
  id INT AUTO_INCREMENT PRIMARY KEY,
  token VARCHAR(512),
  user_id INT,
  wx_open_id VARCHAR(128),
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  end_time TIMESTAMP,
  user_name VARCHAR(64)
);

-- 用户事件日志表
CREATE TABLE IF NOT EXISTS t_user_event_log (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT,
  user_name VARCHAR(64),
  real_name VARCHAR(64),
  content TEXT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- 第二部分：AI 扩展表
-- =============================================

-- 学生画像表
CREATE TABLE IF NOT EXISTS t_student_profile (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  knowledge_level TEXT,
  cognitive_style VARCHAR(50),
  weak_points TEXT,
  learning_goals TEXT,
  learning_pace VARCHAR(20),
  interests TEXT,
  learning_style VARCHAR(50),
  motivation_level INT DEFAULT 3,
  education_background VARCHAR(200),
  occupation_target VARCHAR(200),
  theory_test_result TEXT,
  profile_summary TEXT,
  profile_version INT DEFAULT 1,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE,
  UNIQUE (user_id)
);

-- 学习路径表
CREATE TABLE IF NOT EXISTS t_learning_path (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  subject_id INT,
  path_name VARCHAR(200),
  nodes TEXT,
  current_node_id VARCHAR(50),
  progress_percent DOUBLE DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE
);

-- 资源生成记录表
CREATE TABLE IF NOT EXISTS t_generated_resource (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT,
  subject_id INT,
  resource_type VARCHAR(50) NOT NULL,
  title VARCHAR(500),
  content CLOB,
  content_format VARCHAR(20) DEFAULT 'markdown',
  metadata TEXT,
  source_agent VARCHAR(50),
  quality_score DOUBLE,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE
);

-- 对话会话表
CREATE TABLE IF NOT EXISTS t_dialogue_session (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  session_type VARCHAR(50),
  messages CLOB,
  extracted_profile_delta TEXT,
  status VARCHAR(20) DEFAULT 'active',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Agent 执行日志表
CREATE TABLE IF NOT EXISTS t_agent_execution_log (
  id INT AUTO_INCREMENT PRIMARY KEY,
  request_id VARCHAR(100) NOT NULL,
  user_id INT,
  agent_name VARCHAR(100) NOT NULL,
  action VARCHAR(200),
  input_summary TEXT,
  output_summary TEXT,
  status VARCHAR(20) DEFAULT 'pending',
  duration_ms INT,
  parent_agent VARCHAR(100),
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_request_id ON t_agent_execution_log(request_id);

-- 知识库表
CREATE TABLE IF NOT EXISTS t_knowledge_base (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  description TEXT,
  subject VARCHAR(100),
  document_count INT DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE
);

-- 知识库文档表
CREATE TABLE IF NOT EXISTS t_kb_document (
  id INT AUTO_INCREMENT PRIMARY KEY,
  kb_id INT NOT NULL,
  name VARCHAR(500),
  type VARCHAR(50),
  size BIGINT DEFAULT 0,
  status VARCHAR(20) DEFAULT 'pending',
  content CLOB,
  upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_kb_id ON t_kb_document(kb_id);

-- 学科资源表
CREATE TABLE IF NOT EXISTS t_subject_resource (
  id INT AUTO_INCREMENT PRIMARY KEY,
  subject_id INT NOT NULL,
  source_type VARCHAR(50) DEFAULT 'manual',
  source_id INT,
  title VARCHAR(500) NOT NULL,
  summary TEXT,
  source VARCHAR(200),
  content CLOB,
  format VARCHAR(20) DEFAULT 'markdown',
  click_count INT DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_sr_subject_id ON t_subject_resource(subject_id);

-- 考勤记录表
CREATE TABLE IF NOT EXISTS t_attendance_record (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  user_name VARCHAR(100),
  real_name VARCHAR(100),
  attendance_date DATE NOT NULL,
  status VARCHAR(20) DEFAULT 'present',
  check_in_time TIMESTAMP,
  remark VARCHAR(500),
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_ar_user_date ON t_attendance_record(user_id, attendance_date);

-- 请假申请表
CREATE TABLE IF NOT EXISTS t_leave_request (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  user_name VARCHAR(100),
  real_name VARCHAR(100),
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  reason VARCHAR(1000),
  status VARCHAR(20) DEFAULT 'pending',
  approver_id INT,
  approver_name VARCHAR(100),
  approval_remark VARCHAR(500),
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  approve_time TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE
);

-- =============================================

-- 第三部分：种子数据
-- =============================================

-- 管理员账号: admin / 123456
-- 密码使用 RSA 公钥加密，与 application.yml 中的 pwdKey.publicKey 对应
-- 管理员: admin / 123456
INSERT INTO t_user (id, user_uuid, user_name, password, real_name, age, sex, user_level, phone, role, status, create_time, modify_time, deleted)
SELECT 1, RANDOM_UUID(), 'admin', 'WpTA6h4NC/N4rFD3QRye3+SGEYiCav0XNR73j/qjY/Wbsjx9PWwQYjrBArZICUBOUmbOb3bwSO7yJJoIwtcfIHeKy8NJBqfqyFij+gc76mjmHhk8bOlDto9qSvB5BZ1r7v/IEJIHft7TPwFEMeQ1j2NGjSe9VKFRlaJpxh2pC18=', '系统管理员', 30, 1, 1, '13800000001', 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE id = 1);

-- 测试学生: zhangming / 123456
INSERT INTO t_user (id, user_uuid, user_name, password, real_name, age, sex, user_level, phone, role, status, create_time, modify_time, deleted)
SELECT 2, RANDOM_UUID(), 'zhangming', 'WpTA6h4NC/N4rFD3QRye3+SGEYiCav0XNR73j/qjY/Wbsjx9PWwQYjrBArZICUBOUmbOb3bwSO7yJJoIwtcfIHeKy8NJBqfqyFij+gc76mjmHhk8bOlDto9qSvB5BZ1r7v/IEJIHft7TPwFEMeQ1j2NGjSe9VKFRlaJpxh2pC18=', '张明', 21, 1, 3, '13800138002', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE id = 2);

-- 学科种子数据
INSERT INTO t_subject (id, name, education_level, education_level_name, level, level_name, category, description, item_order, parent_id, subject_type, deleted)
SELECT 1, '高等数学', 2, '本科', 1, '本科一年级', '理学', '高等数学核心课程', 1, 0, 1, FALSE
WHERE NOT EXISTS (SELECT 1 FROM t_subject WHERE id = 1);

INSERT INTO t_subject (id, name, education_level, education_level_name, level, level_name, category, description, item_order, parent_id, subject_type, deleted)
SELECT 2, '程序设计', 2, '本科', 1, '本科一年级', '工学', '程序设计基础课程', 2, 0, 1, FALSE
WHERE NOT EXISTS (SELECT 1 FROM t_subject WHERE id = 2);

INSERT INTO t_subject (id, name, education_level, education_level_name, level, level_name, category, description, item_order, parent_id, subject_type, deleted)
SELECT 3, '计算机网络', 2, '本科', 2, '本科二年级', '工学', '计算机网络核心课程', 3, 0, 1, FALSE
WHERE NOT EXISTS (SELECT 1 FROM t_subject WHERE id = 3);

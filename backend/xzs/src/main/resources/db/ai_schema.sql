-- 学生画像表 (8 dimensions)
CREATE TABLE IF NOT EXISTS t_student_profile (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  knowledge_level TEXT COMMENT 'JSON: per-subject proficiency 0-100',
  cognitive_style VARCHAR(50) COMMENT 'visual/auditory/kinesthetic/reading-writing',
  weak_points TEXT COMMENT 'JSON: array of {subject, topic, severity}',
  learning_goals TEXT COMMENT 'JSON: array of goal strings',
  learning_pace VARCHAR(20) COMMENT 'fast/normal/slow',
  interests TEXT COMMENT 'JSON: array of interest strings',
  learning_style VARCHAR(50) COMMENT 'structured/exploratory/collaborative/independent',
  motivation_level INT DEFAULT 3 COMMENT '1-5 scale',
  profile_summary TEXT COMMENT 'LLM-generated natural language summary',
  profile_version INT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  modify_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT(1) DEFAULT b'0',
  UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 个性化学习路径表
CREATE TABLE IF NOT EXISTS t_learning_path (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  subject_id INT,
  path_name VARCHAR(200),
  nodes TEXT COMMENT 'JSON: array of path nodes',
  current_node_id VARCHAR(50),
  progress_percent DOUBLE DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  modify_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT(1) DEFAULT b'0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AI生成资源表
CREATE TABLE IF NOT EXISTS t_generated_resource (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT,
  subject_id INT,
  resource_type VARCHAR(50) NOT NULL COMMENT 'document/mind_map/exercise/extended_reading/video/animation/code_example',
  title VARCHAR(500),
  content LONGTEXT,
  content_format VARCHAR(20) DEFAULT 'markdown' COMMENT 'markdown/html/json',
  metadata TEXT COMMENT 'JSON: type-specific metadata',
  source_agent VARCHAR(50),
  quality_score DOUBLE,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted BIT(1) DEFAULT b'0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 对话会话表
CREATE TABLE IF NOT EXISTS t_dialogue_session (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  session_type VARCHAR(50) COMMENT 'profile_building/tutoring/general',
  messages LONGTEXT COMMENT 'JSON: array of {role, content, timestamp}',
  extracted_profile_delta TEXT COMMENT 'JSON: profile dimensions extracted',
  status VARCHAR(20) DEFAULT 'active' COMMENT 'active/completed',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  modify_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Agent执行日志表
CREATE TABLE IF NOT EXISTS t_agent_execution_log (
  id INT AUTO_INCREMENT PRIMARY KEY,
  request_id VARCHAR(100) NOT NULL,
  user_id INT,
  agent_name VARCHAR(100) NOT NULL,
  action VARCHAR(200),
  input_summary TEXT,
  output_summary TEXT,
  status VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/running/completed/error',
  duration_ms INT,
  parent_agent VARCHAR(100),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_request_id (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 知识库持久化表
CREATE TABLE IF NOT EXISTS t_knowledge_base (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  description TEXT,
  subject VARCHAR(100),
  document_count INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT(1) DEFAULT b'0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_kb_document (
  id INT AUTO_INCREMENT PRIMARY KEY,
  kb_id INT NOT NULL,
  name VARCHAR(500),
  type VARCHAR(50),
  size BIGINT DEFAULT 0,
  status VARCHAR(20) DEFAULT 'pending',
  content LONGTEXT,
  upload_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted BIT(1) DEFAULT b'0',
  INDEX idx_kb_id (kb_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

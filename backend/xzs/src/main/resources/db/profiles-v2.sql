-- ═══════════════════════════════════════════════════════════════
-- 差异化学习者画像种子数据 v2（对齐赛题 XH-202630 受众要求）
-- 由 test-data/learner_profiles.json 自动生成：python generate_profiles_sql.py
-- 覆盖在校生/转岗/在职进阶/企业内训等职业身份，含先验知识字段：
-- education_background / occupation_target / theory_test_result
-- ═══════════════════════════════════════════════════════════════

-- 兼容旧库：幂等补齐先验知识字段列
ALTER TABLE t_student_profile ADD COLUMN IF NOT EXISTS education_background VARCHAR(200);
ALTER TABLE t_student_profile ADD COLUMN IF NOT EXISTS occupation_target VARCHAR(200);
ALTER TABLE t_student_profile ADD COLUMN IF NOT EXISTS theory_test_result TEXT;

-- LP-01 张明（在校生（目标：AI算法岗/保研））
INSERT IGNORE INTO t_user (id, user_uuid, user_name, password, real_name, age, sex, user_level, phone, role, status, create_time, modify_time, deleted)
VALUES (21, UUID(), 'learner01', 'WpTA6h4NC/N4rFD3QRye3+SGEYiCav0XNR73j/qjY/Wbsjx9PWwQYjrBArZICUBOUmbOb3bwSO7yJJoIwtcfIHeKy8NJBqfqyFij+gc76mjmHhk8bOlDto9qSvB5BZ1r7v/IEJIHft7TPwFEMeQ1j2NGjSe9VKFRlaJpxh2pC18=', '张明', 21, 1, 3, '13913800021', 1, 1, NOW(), NOW(), 0);
INSERT IGNORE INTO t_student_profile (id, user_id, knowledge_level, cognitive_style, weak_points, learning_goals, learning_pace, interests, learning_style, motivation_level, education_background, occupation_target, theory_test_result, profile_summary, profile_version, create_time, modify_time, deleted)
VALUES (21, 21, '{"C语言": 85, "数据结构": 80, "高等数学": 55, "线性代数": 60, "计算机网络": 75}', 'visual', '["高等数学","线性代数"]', '["掌握AI算法","保研"]', 'normal', '["深度学习","算法竞赛"]', 'exploratory', 7, '全日制本科·计算机科学与技术·大三在读', '在校生（目标：AI算法岗/保研）', '{"C语言": 85, "数据结构": 80, "高等数学": 55, "线性代数": 60, "计算机网络": 75}', '全日制本科·计算机科学与技术·大三在读。理论摸底：C语言85分, 数据结构80分, 高等数学55分, 线性代数60分, 计算机网络75分。优势方向：C语言；待补强：高等数学、线性代数。视觉型学习者，节奏平稳，动机7/10。系统将围绕「掌握AI算法、保研」目标动态适配资源难度与形态。', 1, NOW(), NOW(), 0);

-- LP-02 李华（在校生（目标：数据服务岗就业））
INSERT IGNORE INTO t_user (id, user_uuid, user_name, password, real_name, age, sex, user_level, phone, role, status, create_time, modify_time, deleted)
VALUES (22, UUID(), 'learner02', 'WpTA6h4NC/N4rFD3QRye3+SGEYiCav0XNR73j/qjY/Wbsjx9PWwQYjrBArZICUBOUmbOb3bwSO7yJJoIwtcfIHeKy8NJBqfqyFij+gc76mjmHhk8bOlDto9qSvB5BZ1r7v/IEJIHft7TPwFEMeQ1j2NGjSe9VKFRlaJpxh2pC18=', '李华', 19, 0, 3, '13913800022', 1, 1, NOW(), NOW(), 0);
INSERT IGNORE INTO t_student_profile (id, user_id, knowledge_level, cognitive_style, weak_points, learning_goals, learning_pace, interests, learning_style, motivation_level, education_background, occupation_target, theory_test_result, profile_summary, profile_version, create_time, modify_time, deleted)
VALUES (22, 22, '{"Python": 50, "统计学": 40, "高等数学": 35, "数据库": 60}', 'kinesthetic', '["高等数学","统计学"]', '["顺利就业","考取1+x证书"]', 'slow', '["数据可视化"]', 'collaborative', 6, '全日制高职·大数据技术应用·大二在读', '在校生（目标：数据服务岗就业）', '{"Python": 50, "统计学": 40, "高等数学": 35, "数据库": 60}', '全日制高职·大数据技术应用·大二在读。理论摸底：Python50分, 统计学40分, 高等数学35分, 数据库60分。优势方向：数据库；待补强：高等数学、统计学。动觉型学习者，节奏较慢，动机6/10。系统将围绕「顺利就业、考取1+x证书」目标动态适配资源难度与形态。', 1, NOW(), NOW(), 0);

-- LP-03 王芳（在校生（目标：科研论文/AI研究员））
INSERT IGNORE INTO t_user (id, user_uuid, user_name, password, real_name, age, sex, user_level, phone, role, status, create_time, modify_time, deleted)
VALUES (23, UUID(), 'learner03', 'WpTA6h4NC/N4rFD3QRye3+SGEYiCav0XNR73j/qjY/Wbsjx9PWwQYjrBArZICUBOUmbOb3bwSO7yJJoIwtcfIHeKy8NJBqfqyFij+gc76mjmHhk8bOlDto9qSvB5BZ1r7v/IEJIHft7TPwFEMeQ1j2NGjSe9VKFRlaJpxh2pC18=', '王芳', 24, 1, 3, '13913800023', 1, 1, NOW(), NOW(), 0);
INSERT IGNORE INTO t_student_profile (id, user_id, knowledge_level, cognitive_style, weak_points, learning_goals, learning_pace, interests, learning_style, motivation_level, education_background, occupation_target, theory_test_result, profile_summary, profile_version, create_time, modify_time, deleted)
VALUES (23, 23, '{"高等数学": 90, "线性代数": 88, "机器学习": 92, "深度学习": 85}', 'read_write', '[]', '["发表顶会论文","博士深造"]', 'fast', '["大模型","多模态学习"]', 'independent', 5, '全日制硕士·人工智能方向·研二', '在校生（目标：科研论文/AI研究员）', '{"高等数学": 90, "线性代数": 88, "机器学习": 92, "深度学习": 85}', '全日制硕士·人工智能方向·研二。理论摸底：高等数学90分, 线性代数88分, 机器学习92分, 深度学习85分。优势方向：机器学习；待补强：暂无明显薄弱点。读写型学习者，节奏较快，动机5/10。系统将围绕「发表顶会论文、博士深造」目标动态适配资源难度与形态。', 1, NOW(), NOW(), 0);

-- LP-04 陈强（在职转岗：机械工程师 → AI视觉质检工程师）
INSERT IGNORE INTO t_user (id, user_uuid, user_name, password, real_name, age, sex, user_level, phone, role, status, create_time, modify_time, deleted)
VALUES (24, UUID(), 'learner04', 'WpTA6h4NC/N4rFD3QRye3+SGEYiCav0XNR73j/qjY/Wbsjx9PWwQYjrBArZICUBOUmbOb3bwSO7yJJoIwtcfIHeKy8NJBqfqyFij+gc76mjmHhk8bOlDto9qSvB5BZ1r7v/IEJIHft7TPwFEMeQ1j2NGjSe9VKFRlaJpxh2pC18=', '陈强', 34, 0, 3, '13913800024', 1, 1, NOW(), NOW(), 0);
INSERT IGNORE INTO t_student_profile (id, user_id, knowledge_level, cognitive_style, weak_points, learning_goals, learning_pace, interests, learning_style, motivation_level, education_background, occupation_target, theory_test_result, profile_summary, profile_version, create_time, modify_time, deleted)
VALUES (24, 24, '{"机械原理": 95, "C语言": 20, "Python": 25, "高等数学": 45}', 'kinesthetic', '["C语言","Python","深度学习"]', '["3个月内转岗AI质检","看懂检测模型报告"]', 'slow', '["工业检测","实操案例"]', 'collaborative', 5, '全日制本科·机械工程·毕业10年', '在职转岗：机械工程师 → AI视觉质检工程师', '{"机械原理": 95, "C语言": 20, "Python": 25, "高等数学": 45}', '全日制本科·机械工程·毕业10年。理论摸底：机械原理95分, C语言20分, Python25分, 高等数学45分。优势方向：机械原理；待补强：C语言、Python、深度学习。动觉型学习者，节奏较慢，动机5/10。系统将围绕「3个月内转岗AI质检、看懂检测模型报告」目标动态适配资源难度与形态。', 1, NOW(), NOW(), 0);

-- LP-05 赵磊（在职转岗：Java后端工程师 → 大模型应用开发工程师）
INSERT IGNORE INTO t_user (id, user_uuid, user_name, password, real_name, age, sex, user_level, phone, role, status, create_time, modify_time, deleted)
VALUES (25, UUID(), 'learner05', 'WpTA6h4NC/N4rFD3QRye3+SGEYiCav0XNR73j/qjY/Wbsjx9PWwQYjrBArZICUBOUmbOb3bwSO7yJJoIwtcfIHeKy8NJBqfqyFij+gc76mjmHhk8bOlDto9qSvB5BZ1r7v/IEJIHft7TPwFEMeQ1j2NGjSe9VKFRlaJpxh2pC18=', '赵磊', 30, 1, 3, '13913800025', 1, 1, NOW(), NOW(), 0);
INSERT IGNORE INTO t_student_profile (id, user_id, knowledge_level, cognitive_style, weak_points, learning_goals, learning_pace, interests, learning_style, motivation_level, education_background, occupation_target, theory_test_result, profile_summary, profile_version, create_time, modify_time, deleted)
VALUES (25, 25, '{"Java": 90, "计算机网络": 80, "数据库": 85, "Python": 65, "机器学习": 40}', 'visual', '["机器学习","深度学习"]', '["转型LLM应用开发","独立交付RAG系统"]', 'normal', '["RAG","Agent开发"]', 'exploratory', 4, '全日制本科·软件工程·毕业8年', '在职转岗：Java后端工程师 → 大模型应用开发工程师', '{"Java": 90, "计算机网络": 80, "数据库": 85, "Python": 65, "机器学习": 40}', '全日制本科·软件工程·毕业8年。理论摸底：Java90分, 计算机网络80分, 数据库85分, Python65分, 机器学习40分。优势方向：Java；待补强：机器学习、深度学习。视觉型学习者，节奏平稳，动机4/10。系统将围绕「转型LLM应用开发、独立交付RAG系统」目标动态适配资源难度与形态。', 1, NOW(), NOW(), 0);

-- LP-06 孙丽（企业内训：客服运营专员 → 智能客服运营）
INSERT IGNORE INTO t_user (id, user_uuid, user_name, password, real_name, age, sex, user_level, phone, role, status, create_time, modify_time, deleted)
VALUES (26, UUID(), 'learner06', 'WpTA6h4NC/N4rFD3QRye3+SGEYiCav0XNR73j/qjY/Wbsjx9PWwQYjrBArZICUBOUmbOb3bwSO7yJJoIwtcfIHeKy8NJBqfqyFij+gc76mjmHhk8bOlDto9qSvB5BZ1r7v/IEJIHft7TPwFEMeQ1j2NGjSe9VKFRlaJpxh2pC18=', '孙丽', 27, 0, 3, '13913800026', 1, 1, NOW(), NOW(), 0);
INSERT IGNORE INTO t_student_profile (id, user_id, knowledge_level, cognitive_style, weak_points, learning_goals, learning_pace, interests, learning_style, motivation_level, education_background, occupation_target, theory_test_result, profile_summary, profile_version, create_time, modify_time, deleted)
VALUES (26, 26, '{"办公自动化": 70, "业务流程管理": 85, "Python": 15, "统计学": 55}', 'auditory', '["Python","统计学"]', '["掌握智能客服平台运营","会看意图识别报表"]', 'normal', '["对话机器人","客户体验"]', 'exploratory', 6, '全日制本科·工商管理·毕业4年', '企业内训：客服运营专员 → 智能客服运营', '{"办公自动化": 70, "业务流程管理": 85, "Python": 15, "统计学": 55}', '全日制本科·工商管理·毕业4年。理论摸底：办公自动化70分, 业务流程管理85分, Python15分, 统计学55分。优势方向：业务流程管理；待补强：Python、统计学。听觉型学习者，节奏平稳，动机6/10。系统将围绕「掌握智能客服平台运营、会看意图识别报表」目标动态适配资源难度与形态。', 1, NOW(), NOW(), 0);

-- LP-07 周杰（应届求职（目标：初级开发岗））
INSERT IGNORE INTO t_user (id, user_uuid, user_name, password, real_name, age, sex, user_level, phone, role, status, create_time, modify_time, deleted)
VALUES (27, UUID(), 'learner07', 'WpTA6h4NC/N4rFD3QRye3+SGEYiCav0XNR73j/qjY/Wbsjx9PWwQYjrBArZICUBOUmbOb3bwSO7yJJoIwtcfIHeKy8NJBqfqyFij+gc76mjmHhk8bOlDto9qSvB5BZ1r7v/IEJIHft7TPwFEMeQ1j2NGjSe9VKFRlaJpxh2pC18=', '周杰', 20, 1, 3, '13913800027', 1, 1, NOW(), NOW(), 0);
INSERT IGNORE INTO t_student_profile (id, user_id, knowledge_level, cognitive_style, weak_points, learning_goals, learning_pace, interests, learning_style, motivation_level, education_background, occupation_target, theory_test_result, profile_summary, profile_version, create_time, modify_time, deleted)
VALUES (27, 27, '{"C语言": 65, "Java": 60, "数据库": 55, "计算机网络": 50}', 'visual', '["计算机网络","算法"]', '["秋招上岸","补齐算法短板"]', 'normal', '["Web开发"]', 'exploratory', 7, '全日制专科·软件技术·应届', '应届求职（目标：初级开发岗）', '{"C语言": 65, "Java": 60, "数据库": 55, "计算机网络": 50}', '全日制专科·软件技术·应届。理论摸底：C语言65分, Java60分, 数据库55分, 计算机网络50分。优势方向：C语言；待补强：计算机网络、算法。视觉型学习者，节奏平稳，动机7/10。系统将围绕「秋招上岸、补齐算法短板」目标动态适配资源难度与形态。', 1, NOW(), NOW(), 0);

-- LP-08 吴敏（在职进阶：数据分析师 → 机器学习算法工程师）
INSERT IGNORE INTO t_user (id, user_uuid, user_name, password, real_name, age, sex, user_level, phone, role, status, create_time, modify_time, deleted)
VALUES (28, UUID(), 'learner08', 'WpTA6h4NC/N4rFD3QRye3+SGEYiCav0XNR73j/qjY/Wbsjx9PWwQYjrBArZICUBOUmbOb3bwSO7yJJoIwtcfIHeKy8NJBqfqyFij+gc76mjmHhk8bOlDto9qSvB5BZ1r7v/IEJIHft7TPwFEMeQ1j2NGjSe9VKFRlaJpxh2pC18=', '吴敏', 29, 0, 3, '13913800028', 1, 1, NOW(), NOW(), 0);
INSERT IGNORE INTO t_student_profile (id, user_id, knowledge_level, cognitive_style, weak_points, learning_goals, learning_pace, interests, learning_style, motivation_level, education_background, occupation_target, theory_test_result, profile_summary, profile_version, create_time, modify_time, deleted)
VALUES (28, 28, '{"SQL": 88, "统计学": 75, "Python": 70, "机器学习": 55}', 'read_write', '["深度学习","模型部署"]', '["晋升算法岗","独立完成建模项目"]', 'normal', '["推荐系统","风控建模"]', 'exploratory', 5, '全日制本科·应用统计学·毕业5年', '在职进阶：数据分析师 → 机器学习算法工程师', '{"SQL": 88, "统计学": 75, "Python": 70, "机器学习": 55}', '全日制本科·应用统计学·毕业5年。理论摸底：SQL88分, 统计学75分, Python70分, 机器学习55分。优势方向：SQL；待补强：深度学习、模型部署。读写型学习者，节奏平稳，动机5/10。系统将围绕「晋升算法岗、独立完成建模项目」目标动态适配资源难度与形态。', 1, NOW(), NOW(), 0);

-- LP-09 郑浩（在校生（目标：ACM竞赛/算法岗））
INSERT IGNORE INTO t_user (id, user_uuid, user_name, password, real_name, age, sex, user_level, phone, role, status, create_time, modify_time, deleted)
VALUES (29, UUID(), 'learner09', 'WpTA6h4NC/N4rFD3QRye3+SGEYiCav0XNR73j/qjY/Wbsjx9PWwQYjrBArZICUBOUmbOb3bwSO7yJJoIwtcfIHeKy8NJBqfqyFij+gc76mjmHhk8bOlDto9qSvB5BZ1r7v/IEJIHft7TPwFEMeQ1j2NGjSe9VKFRlaJpxh2pC18=', '郑浩', 19, 1, 3, '13913800029', 1, 1, NOW(), NOW(), 0);
INSERT IGNORE INTO t_student_profile (id, user_id, knowledge_level, cognitive_style, weak_points, learning_goals, learning_pace, interests, learning_style, motivation_level, education_background, occupation_target, theory_test_result, profile_summary, profile_version, create_time, modify_time, deleted)
VALUES (29, 29, '{"高等数学": 92, "线性代数": 90, "概率论": 88, "C语言": 35}', 'read_write', '["编程实现"]', '["ACM区域赛获奖","算法实习"]', 'fast', '["组合优化","机器学习理论"]', 'independent', 4, '全日制本科·数学与应用数学·大二在读', '在校生（目标：ACM竞赛/算法岗）', '{"高等数学": 92, "线性代数": 90, "概率论": 88, "C语言": 35}', '全日制本科·数学与应用数学·大二在读。理论摸底：高等数学92分, 线性代数90分, 概率论88分, C语言35分。优势方向：高等数学；待补强：编程实现。读写型学习者，节奏较快，动机4/10。系统将围绕「ACM区域赛获奖、算法实习」目标动态适配资源难度与形态。', 1, NOW(), NOW(), 0);

-- LP-10 冯雪（企业内训：制造企业质量部工程师 → 质检智能化负责人）
INSERT IGNORE INTO t_user (id, user_uuid, user_name, password, real_name, age, sex, user_level, phone, role, status, create_time, modify_time, deleted)
VALUES (30, UUID(), 'learner10', 'WpTA6h4NC/N4rFD3QRye3+SGEYiCav0XNR73j/qjY/Wbsjx9PWwQYjrBArZICUBOUmbOb3bwSO7yJJoIwtcfIHeKy8NJBqfqyFij+gc76mjmHhk8bOlDto9qSvB5BZ1r7v/IEJIHft7TPwFEMeQ1j2NGjSe9VKFRlaJpxh2pC18=', '冯雪', 36, 0, 3, '13913800030', 1, 1, NOW(), NOW(), 0);
INSERT IGNORE INTO t_student_profile (id, user_id, knowledge_level, cognitive_style, weak_points, learning_goals, learning_pace, interests, learning_style, motivation_level, education_background, occupation_target, theory_test_result, profile_summary, profile_version, create_time, modify_time, deleted)
VALUES (30, 30, '{"质量管理体系": 90, "统计学": 55, "Python": 30, "视觉检测基础": 20}', 'kinesthetic', '["Python","深度学习"]', '["主导产线质检AI改造","评估供应商方案"]', 'slow', '["缺陷检测","验收标准"]', 'collaborative', 6, '全日制本科·工业工程·毕业12年', '企业内训：制造企业质量部工程师 → 质检智能化负责人', '{"质量管理体系": 90, "统计学": 55, "Python": 30, "视觉检测基础": 20}', '全日制本科·工业工程·毕业12年。理论摸底：质量管理体系90分, 统计学55分, Python30分, 视觉检测基础20分。优势方向：质量管理体系；待补强：Python、深度学习。动觉型学习者，节奏较慢，动机6/10。系统将围绕「主导产线质检AI改造、评估供应商方案」目标动态适配资源难度与形态。', 1, NOW(), NOW(), 0);

-- LP-11 许阳（在校生（目标：前沿科研/高校教职））
INSERT IGNORE INTO t_user (id, user_uuid, user_name, password, real_name, age, sex, user_level, phone, role, status, create_time, modify_time, deleted)
VALUES (31, UUID(), 'learner11', 'WpTA6h4NC/N4rFD3QRye3+SGEYiCav0XNR73j/qjY/Wbsjx9PWwQYjrBArZICUBOUmbOb3bwSO7yJJoIwtcfIHeKy8NJBqfqyFij+gc76mjmHhk8bOlDto9qSvB5BZ1r7v/IEJIHft7TPwFEMeQ1j2NGjSe9VKFRlaJpxh2pC18=', '许阳', 27, 1, 3, '13913800031', 1, 1, NOW(), NOW(), 0);
INSERT IGNORE INTO t_student_profile (id, user_id, knowledge_level, cognitive_style, weak_points, learning_goals, learning_pace, interests, learning_style, motivation_level, education_background, occupation_target, theory_test_result, profile_summary, profile_version, create_time, modify_time, deleted)
VALUES (31, 31, '{"高等数学": 95, "线性代数": 93, "强化学习": 90, "深度学习": 88}', 'read_write', '[]', '["顶刊发表","工业界研究院offer"]', 'fast', '["决策智能","具身智能"]', 'independent', 5, '全日制博士·控制科学与工程·博三', '在校生（目标：前沿科研/高校教职）', '{"高等数学": 95, "线性代数": 93, "强化学习": 90, "深度学习": 88}', '全日制博士·控制科学与工程·博三。理论摸底：高等数学95分, 线性代数93分, 强化学习90分, 深度学习88分。优势方向：高等数学；待补强：暂无明显薄弱点。读写型学习者，节奏较快，动机5/10。系统将围绕「顶刊发表、工业界研究院offer」目标动态适配资源难度与形态。', 1, NOW(), NOW(), 0);

-- LP-12 何静（在校生（目标：专升本考试））
INSERT IGNORE INTO t_user (id, user_uuid, user_name, password, real_name, age, sex, user_level, phone, role, status, create_time, modify_time, deleted)
VALUES (32, UUID(), 'learner12', 'WpTA6h4NC/N4rFD3QRye3+SGEYiCav0XNR73j/qjY/Wbsjx9PWwQYjrBArZICUBOUmbOb3bwSO7yJJoIwtcfIHeKy8NJBqfqyFij+gc76mjmHhk8bOlDto9qSvB5BZ1r7v/IEJIHft7TPwFEMeQ1j2NGjSe9VKFRlaJpxh2pC18=', '何静', 21, 0, 3, '13913800032', 1, 1, NOW(), NOW(), 0);
INSERT IGNORE INTO t_student_profile (id, user_id, knowledge_level, cognitive_style, weak_points, learning_goals, learning_pace, interests, learning_style, motivation_level, education_background, occupation_target, theory_test_result, profile_summary, profile_version, create_time, modify_time, deleted)
VALUES (32, 32, '{"C语言": 58, "数据结构": 48, "高等数学": 42, "大学英语": 65}', 'visual', '["高等数学","数据结构"]', '["专升本上岸"]', 'normal', '["前端页面设计"]', 'exploratory', 8, '全日制高职·计算机应用技术·大三（专升本备考）', '在校生（目标：专升本考试）', '{"C语言": 58, "数据结构": 48, "高等数学": 42, "大学英语": 65}', '全日制高职·计算机应用技术·大三（专升本备考）。理论摸底：C语言58分, 数据结构48分, 高等数学42分, 大学英语65分。优势方向：大学英语；待补强：高等数学、数据结构。视觉型学习者，节奏平稳，动机8/10。系统将围绕「专升本上岸」目标动态适配资源难度与形态。', 1, NOW(), NOW(), 0);

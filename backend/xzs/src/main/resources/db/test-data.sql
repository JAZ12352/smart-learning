-- ===========================================
-- 可复现验证案例测试数据
-- 创建测试学生"张明"和相关数据
-- ===========================================

-- 测试学生（仅在不存在时插入）
INSERT IGNORE INTO t_user (id, user_uuid, user_name, password, real_name, age, sex, user_level, phone, role, status, create_time, modify_time, deleted)
VALUES (2, UUID(), 'zhangming', 'WpTA6h4NC/N4rFD3QRye3+SGEYiCav0XNR73j/qjY/Wbsjx9PWwQYjrBArZICUBOUmbOb3bwSO7yJJoIwtcfIHeKy8NJBqfqyFij+gc76mjmHhk8bOlDto9qSvB5BZ1r7v/IEJIHft7TPwFEMeQ1j2NGjSe9VKFRlaJpxh2pC18=', '张明', 21, 1, 3, '13800138002', 1, 1, NOW(), NOW(), 0);

-- 学生画像（张明）
INSERT IGNORE INTO t_student_profile (id, user_id, knowledge_level, cognitive_style, weak_points, learning_goals, learning_pace, interests, learning_style, motivation_level, profile_summary, profile_version, create_time, modify_time, deleted)
VALUES (1, 2,
  '{"高等数学":75,"线性代数":82,"C语言":90,"大学英语":78,"数据结构":70,"计算机网络":60}',
  'visual',
  '["数学证明题","算法复杂度分析","网络协议细节记忆"]',
  '["考研985院校","从事AI方向","提升编程实战能力"]',
  'normal',
  '["机器学习","Web全栈开发","开源项目"]',
  'collaborative',
  7,
  '该生编程基础扎实(C语言90分)，对AI方向有强烈兴趣，但数学基础(高等数学75分)和算法分析能力是短板。偏好视频学习+动手实践，适合项目驱动的学习方式。考研目标明确，需要针对性地加强数学和算法训练。',
  1, NOW(), NOW(), 0);

-- 学习路径（张明 - 高等数学）
INSERT IGNORE INTO t_learning_path (id, user_id, subject_id, path_name, nodes, current_node_id, progress_percent, create_time, modify_time, deleted)
VALUES (1, 2, 1, '高等数学（考研方向）学习路径',
  '{"nodes":[{"id":"node_1","title":"函数与极限复习","description":"回顾函数基本概念、极限定义与计算","knowledgePoints":["函数定义域与值域","极限的ε-N定义","无穷小与无穷大"],"prerequisites":[],"estimatedHours":4,"difficulty":"easy","resources":["同济版高等数学§1"],"reason":"高数基础75分，需先巩固函数与极限基本功"},{"id":"node_2","title":"导数与微分深化","description":"系统学习导数概念、求导法则与高阶导数","knowledgePoints":["导数定义与几何意义","求导法则(四则/链式)","隐函数与参数方程求导"],"prerequisites":["node_1"],"estimatedHours":8,"difficulty":"medium","resources":["3Blue1Brown 微积分系列","习题集1"],"reason":"核心章节，需花较多时间建立直觉和计算能力"},{"id":"node_3","title":"微分中值定理","description":"罗尔定理+拉格朗日+柯西+泰勒","knowledgePoints":["罗尔定理","拉格朗日中值定理","泰勒公式"],"prerequisites":["node_2"],"estimatedHours":6,"difficulty":"hard","resources":["证明题专项训练册"],"reason":"张明薄弱点在数学证明，此章需重点突破"},{"id":"node_4","title":"不定积分","description":"原函数概念与积分法","knowledgePoints":["换元积分法","分部积分法","有理函数积分"],"prerequisites":["node_3"],"estimatedHours":6,"difficulty":"medium","resources":["积分技巧总结表"],"reason":"与导数互为逆运算，打好基础"},{"id":"node_5","title":"定积分及应用","description":"定积分定义、计算与几何应用","knowledgePoints":["定积分定义","牛顿-莱布尼茨公式","面积与体积计算"],"prerequisites":["node_4"],"estimatedHours":6,"difficulty":"medium","resources":["定积分应用案例集"],"reason":"工程应用核心，与考研重点吻合"},{"id":"node_6","title":"考研真题精练","description":"历年考研高数真题分类刷题","knowledgePoints":["极限计算","导数应用","积分计算","中值定理证明","综合题"],"prerequisites":["node_3","node_5"],"estimatedHours":12,"difficulty":"hard","resources":["2015-2025考研数学一真题"],"reason":"目标985考研，需大量练习提升应试能力"}]}',
  'node_2', 45.0, NOW(), NOW(), 0);

-- 学习路径（张明 - 程序设计）
INSERT IGNORE INTO t_learning_path (id, user_id, subject_id, path_name, nodes, current_node_id, progress_percent, create_time, modify_time, deleted)
VALUES (2, 2, 2, '程序设计（Python/AI方向）学习路径',
  '{"nodes":[{"id":"np1","title":"Python基础快速入门","description":"变量、控制流、函数、类","knowledgePoints":["Python语法","面向对象","文件操作"],"prerequisites":[],"estimatedHours":4,"difficulty":"easy","resources":["廖雪峰Python教程"],"reason":"C语言基础好，Python语法可快速上手"},{"id":"np2","title":"数据科学库实战","description":"NumPy+Pandas+Matplotlib","knowledgePoints":["NumPy数组操作","Pandas数据清洗","Matplotlib可视化"],"prerequisites":["np1"],"estimatedHours":8,"difficulty":"medium","resources":["Kaggle入门教程"],"reason":"AI方向必备工具链"},{"id":"np3","title":"机器学习入门","description":"监督学习基本算法","knowledgePoints":["线性回归","决策树","KNN","模型评估"],"prerequisites":["np2"],"estimatedHours":10,"difficulty":"medium","resources":["吴恩达ML课程"],"reason":"AI方向核心内容，从简单算法开始"},{"id":"np4","title":"深度学习基础","description":"神经网络与PyTorch","knowledgePoints":["神经网络原理","PyTorch基础","CNN入门"],"prerequisites":["np3"],"estimatedHours":12,"difficulty":"hard","resources":["d2l.ai动手学深度学习"],"reason":"研究生和就业方向需要深度学习能力"},{"id":"np5","title":"AI项目实战","description":"完整机器学习/深度学习项目","knowledgePoints":["数据预处理","模型训练调优","部署上线"],"prerequisites":["np4"],"estimatedHours":16,"difficulty":"hard","resources":["MNIST手写数字识别","推荐系统实战"],"reason":"项目经验是求职核心，需要作品集"}]}',
  'np3', 60.0, NOW(), NOW(), 0);

-- 学习路径（张明 - 计算机网络）
INSERT IGNORE INTO t_learning_path (id, user_id, subject_id, path_name, nodes, current_node_id, progress_percent, create_time, modify_time, deleted)
VALUES (3, 2, 3, '计算机网络学习路径',
  '{"nodes":[{"id":"nw1","title":"OSI/TCP-IP体系结构","description":"七层模型和四层模型对比","knowledgePoints":["OSI七层模型","TCP/IP四层模型","封装与解封装"],"prerequisites":[],"estimatedHours":4,"difficulty":"easy","resources":["计算机网络自顶向下§1"],"reason":"建立宏观网络架构认知"},{"id":"nw2","title":"传输层详解","description":"TCP/UDP协议机制","knowledgePoints":["TCP三次握手","拥塞控制","UDP vs TCP对比"],"prerequisites":["nw1"],"estimatedHours":8,"difficulty":"hard","resources":["Wireshark抓包实验"],"reason":"传输层是面试高频考点"},{"id":"nw3","title":"网络层与路由","description":"IP协议+路由算法","knowledgePoints":["IP地址与子网","路由协议OSPF/BGP","NAT原理"],"prerequisites":["nw2"],"estimatedHours":6,"difficulty":"medium","resources":["Cisco Packet Tracer实验"],"reason":"配合动手实验加深理解"},{"id":"nw4","title":"应用层协议实践","description":"HTTP/DNS/DHCP详解","knowledgePoints":["HTTP/HTTPS","DNS解析过程","DHCP工作流程"],"prerequisites":["nw3"],"estimatedHours":6,"difficulty":"medium","resources":["搭建个人网站实践"],"reason":"应用层与日常开发密切相关"}]}',
  'nw1', 15.0, NOW(), NOW(), 0);

-- ===========================================
-- 画像 2：李芳（文科转码、编程零基础、视觉型学习者）
-- ===========================================
INSERT IGNORE INTO t_user (id, user_uuid, user_name, password, real_name, age, sex, user_level, phone, role, status, create_time, modify_time, deleted)
VALUES (3, UUID(), 'lifang', 'WpTA6h4NC/N4rFD3QRye3+SGEYiCav0XNR73j/qjY/Wbsjx9PWwQYjrBArZICUBOUmbOb3bwSO7yJJoIwtcfIHeKy8NJBqfqyFij+gc76mjmHhk8bOlDto9qSvB5BZ1r7v/IEJIHft7TPwFEMeQ1j2NGjSe9VKFRlaJpxh2pC18=', '李芳', 20, 2, 3, '13800138003', 1, 1, NOW(), NOW(), 0);

INSERT IGNORE INTO t_student_profile (id, user_id, knowledge_level, cognitive_style, weak_points, learning_goals, learning_pace, interests, learning_style, motivation_level, profile_summary, profile_version, create_time, modify_time, deleted)
VALUES (2, 3,
  '{"高等数学":45,"线性代数":50,"C语言":30,"大学英语":92,"数据结构":35,"计算机网络":40}',
  'visual',
  '["编程思维建立","数学基础薄弱","抽象概念理解困难"]',
  '["转行互联网产品","掌握数据分析技能","提升逻辑思维"]',
  'slow',
  '["UI设计","数据分析","自媒体运营"]',
  'visual',
  5,
  '文科背景学生，英语能力强(92分)但编程零基础(C语言30分)，数学基础薄弱。视觉型学习者，偏好图表和视频教学。需要从零开始建立编程思维，适合图形化教学工具和案例驱动方式。转行动机明确但需要降低认知负荷，循序渐进。',
  1, NOW(), NOW(), 0);

-- 学习路径（李芳 - 编程入门）
INSERT IGNORE INTO t_learning_path (id, user_id, subject_id, path_name, nodes, current_node_id, progress_percent, create_time, modify_time, deleted)
VALUES (4, 3, 2, '编程零基础入门路径（可视化教学）',
  '{"nodes":[{"id":"lf1","title":"计算思维启蒙","description":"用生活案例理解编程概念","knowledgePoints":["算法概念","流程图","伪代码"],"prerequisites":[],"estimatedHours":6,"difficulty":"easy","resources":["Scratch图形化编程","算法图解"],"reason":"文科生需先建立计算思维，图形化工具降低门槛"},{"id":"lf2","title":"Python语法基础","description":"变量、条件、循环","knowledgePoints":["变量与数据类型","if-else条件","for/while循环"],"prerequisites":["lf1"],"estimatedHours":10,"difficulty":"easy","resources":["Python编程从入门到实践","B站入门视频"],"reason":"Python语法简洁，适合零基础"},{"id":"lf3","title":"数据处理入门","description":"Excel+Pandas基础","knowledgePoints":["Pandas DataFrame","数据清洗","简单可视化"],"prerequisites":["lf2"],"estimatedHours":8,"difficulty":"medium","resources":["Kaggle微课程"],"reason":"结合其英语优势，数据分析是转行产品方向的切入点"},{"id":"lf4","title":"数据分析项目","description":"完整数据分析案例","knowledgePoints":["数据采集","统计分析","图表报告"],"prerequisites":["lf3"],"estimatedHours":12,"difficulty":"medium","resources":["实战数据集"],"reason":"项目驱动学习，产出可展示作品"}]}',
  'lf1', 10.0, NOW(), NOW(), 0);

-- ===========================================
-- 画像 3：王强（工科背景、数学强、听觉型学习者）
-- ===========================================
INSERT IGNORE INTO t_user (id, user_uuid, user_name, password, real_name, age, sex, user_level, phone, role, status, create_time, modify_time, deleted)
VALUES (4, UUID(), 'wangqiang', 'WpTA6h4NC/N4rFD3QRye3+SGEYiCav0XNR73j/qjY/Wbsjx9PWwQYjrBArZICUBOUmbOb3bwSO7yJJoIwtcfIHeKy8NJBqfqyFij+gc76mjmHhk8bOlDto9qSvB5BZ1r7v/IEJIHft7TPwFEMeQ1j2NGjSe9VKFRlaJpxh2pC18=', '王强', 22, 1, 3, '13800138004', 1, 1, NOW(), NOW(), 0);

INSERT IGNORE INTO t_student_profile (id, user_id, knowledge_level, cognitive_style, weak_points, learning_goals, learning_pace, interests, learning_style, motivation_level, profile_summary, profile_version, create_time, modify_time, deleted)
VALUES (3, 4,
  '{"高等数学":95,"线性代数":90,"C语言":78,"大学英语":60,"数据结构":85,"计算机网络":82}',
  'auditory',
  '["英语阅读理解","技术文档撰写","团队协作沟通"]',
  '["保研985","发表AI论文","提升工程实践能力"]',
  'fast',
  '["深度学习","算法竞赛","数学建模"]',
  'independent',
  9,
  '工科尖子生，数学能力突出(高数95分、线代90分)，编程基础扎实(C语言78分、数据结构85分)。听觉型学习者，擅长通过讨论和讲解吸收知识。英语是短板(60分)，影响论文阅读。适合挑战性内容，可加速进阶，研究导向学习。',
  1, NOW(), NOW(), 0);

-- 学习路径（王强 - AI研究进阶）
INSERT IGNORE INTO t_learning_path (id, user_id, subject_id, path_name, nodes, current_node_id, progress_percent, create_time, modify_time, deleted)
VALUES (5, 4, 2, 'AI研究进阶路径（数学优势赋能）',
  '{"nodes":[{"id":"wq1","title":"数学基础强化","description":"线性代数+概率论+最优化","knowledgePoints":["矩阵分解","概率分布","梯度下降收敛性"],"prerequisites":[],"estimatedHours":6,"difficulty":"medium","resources":["MIT线性代数","凸优化理论"],"reason":"数学95分可快速进阶到ML数学基础"},{"id":"wq2","title":"深度学习理论","description":"神经网络数学原理与反向传播","knowledgePoints":["前向传播推导","反向传播链式法则","损失函数设计"],"prerequisites":["wq1"],"estimatedHours":8,"difficulty":"hard","resources":["CS231n","深度学习花书"],"reason":"从数学角度理解深度学习，发挥其优势"},{"id":"wq3","title":"Transformer与NLP","description":"注意力机制与预训练模型","knowledgePoints":["Self-Attention","位置编码","BERT/GPT架构"],"prerequisites":["wq2"],"estimatedHours":10,"difficulty":"hard","resources":["论文精读","HuggingFace教程"],"reason":"当前研究热点，为发论文做准备"},{"id":"wq4","title":"论文复现与实验","description":"经典论文代码实现与改进","knowledgePoints":["实验设计","消融实验","论文写作"],"prerequisites":["wq3"],"estimatedHours":16,"difficulty":"hard","resources":["Papers with Code","实验室项目"],"reason":"培养科研能力，产出论文成果"}]}',
  'wq2', 35.0, NOW(), NOW(), 0);

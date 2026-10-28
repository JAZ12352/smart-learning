import json, os, sys

path = 'C:/Users/20873/Downloads/zhihuibeike - 副本/backend/xzs/src/main/resources/course-data.json'
with open(path, 'r', encoding='utf-8') as f:
    courses = json.load(f)

# Common textbooks by category
textbooks = {
    '计算机科学': [
        {'name': '计算机科学概论（第13版）', 'author': 'J. Glenn Brookshear', 'publisher': '人民邮电出版社', 'edition': '第13版', 'chapters': 12, 'pages': 560},
        {'name': '深入理解计算机系统（第3版）', 'author': 'Randal E. Bryant', 'publisher': '机械工业出版社', 'edition': '第3版', 'chapters': 12, 'pages': 737},
    ],
    '数学': [
        {'name': '高等数学学习辅导', 'author': '同济大学数学系', 'publisher': '高等教育出版社', 'edition': '第7版配套', 'chapters': 12, 'pages': 480},
        {'name': '数学分析中的典型问题与方法', 'author': '裴礼文', 'publisher': '高等教育出版社', 'edition': '第3版', 'chapters': 17, 'pages': 1056},
    ],
    '物理': [
        {'name': '大学物理学习指导', 'author': '中国科大物理系', 'publisher': '中国科学技术大学出版社', 'edition': '第2版', 'chapters': 15, 'pages': 520},
    ],
    '英语': [
        {'name': '新视野大学英语综合训练', 'author': '郑树棠', 'publisher': '外语教学与研究出版社', 'edition': '第3版', 'chapters': 8, 'pages': 256},
    ],
    '经济学': [
        {'name': '西方经济学学习指导', 'author': '高鸿业', 'publisher': '中国人民大学出版社', 'edition': '第8版配套', 'chapters': 20, 'pages': 380},
    ],
    '生物': [
        {'name': '生命科学导论学习指导', 'author': '北京大学', 'publisher': '高等教育出版社', 'edition': '第3版配套', 'chapters': 12, 'pages': 320},
    ],
}

# Common exercises by category
common_exercises = {
    '计算机科学': [
        {'question': '算法的时间复杂度是指？', 'options': ['算法执行的时间', '算法执行时所需的基本操作次数', '算法的输入规模', '算法的代码行数'], 'answer': 'B'},
        {'question': '栈的插入和删除操作在什么位置进行？', 'options': ['栈顶', '栈底', '任意位置', '中间'], 'answer': 'A'},
        {'question': '在TCP协议中，三次握手用于？', 'options': ['建立连接', '释放连接', '流量控制', '差错控制'], 'answer': 'A'},
        {'question': '虚拟存储器的容量受什么限制？', 'options': ['物理内存大小', '磁盘空间大小', '地址总线宽度', 'CPU速度'], 'answer': 'C'},
        {'question': '关系数据库中的范式主要用于解决什么问题？', 'options': ['数据冗余', '查询速度', '安全性', '并发控制'], 'answer': 'A'},
        {'question': '面向对象编程的三大特性是？', 'options': ['封装、继承、多态', '抽象、继承、多态', '封装、继承、重载', '封装、抽象、多态'], 'answer': 'A'},
    ],
    '数学': [
        {'question': '函数f(x)在x=a处可导的充要条件是？', 'options': ['左右导数存在且相等', '函数在a处连续', '函数在a处有定义', '极限存在'], 'answer': 'A'},
        {'question': '下列哪个级数一定收敛？', 'options': ['调和级数', 'p>1的p级数', '几何级数|r|>=1', '交错级数'], 'answer': 'B'},
        {'question': '矩阵A可逆的充要条件是？', 'options': ['det(A)=0', 'det(A)≠0', 'A是方阵', 'A的秩小于n'], 'answer': 'B'},
    ],
    '物理': [
        {'question': '牛顿第一定律又称为什么定律？', 'options': ['惯性定律', '加速度定律', '作用与反作用定律', '万有引力定律'], 'answer': 'A'},
        {'question': '光的干涉现象证明了光的什么性质？', 'options': ['粒子性', '波动性', '量子性', '电磁性'], 'answer': 'B'},
    ],
    '英语': [
        {'question': 'The word "significant" most nearly means?', 'options': ['小的', '重要的', '明显的', '大量的'], 'answer': 'B'},
        {'question': 'Which tense is used to describe an action that happened before another past action?', 'options': ['Past Simple', 'Past Perfect', 'Present Perfect', 'Past Continuous'], 'answer': 'B'},
    ],
    '经济学': [
        {'question': '机会成本的定义是？', 'options': ['放弃的最高代价', '会计成本', '直接成本', '沉没成本'], 'answer': 'A'},
        {'question': 'GDP的全称是？', 'options': ['国民生产总值', '国内生产总值', '国民生产净值', '国内生产净值'], 'answer': 'B'},
    ],
    '生物': [
        {'question': 'DNA复制的方式是？', 'options': ['半保留复制', '全保留复制', '分散复制', '随机复制'], 'answer': 'A'},
        {'question': '有氧呼吸的主要场所是？', 'options': ['细胞核', '线粒体', '叶绿体', '内质网'], 'answer': 'B'},
    ],
}

# Common textbook resources for courses that don't have any
fallback_textbook = [
    {'name': '课程配套教材', 'author': '出版社', 'publisher': '高等教育出版社', 'edition': '最新版', 'chapters': 10, 'pages': 350},
]

for c in courses:
    cat = c.get('category', '其他')
    changed = False

    # Fix missing textbookResources
    if not c.get('textbookResources') or len(c['textbookResources']) == 0:
        matching_textbooks = textbooks.get(cat, fallback_textbook)
        c['textbookResources'] = matching_textbooks[:2]  # add up to 2
        changed = True

    # Fix missing/empty exercises
    if not c.get('exercises') or len(c['exercises']) < 3:
        existing_qs = set(e.get('question','') for e in c.get('exercises',[]))
        pool = common_exercises.get(cat, fallback_textbook)
        to_add = []
        for ex in pool:
            if ex['question'] not in existing_qs:
                to_add.append(ex)
                if len(to_add) + len(c.get('exercises',[])) >= 5:
                    break
        if 'exercises' not in c or not c['exercises']:
            c['exercises'] = to_add
        else:
            c['exercises'].extend(to_add)
        changed = True

    # Ensure each course has at least 2 ppt entries
    if not c.get('pptList') or len(c['pptList']) < 2:
        # Create fallback PPT entries based on chapter titles
        ch_titles = c.get('chapterTitles', ['第1章','第2章','第3章'])
        ppt_list = []
        for idx, ch in enumerate(ch_titles[:6]):
            ppt_list.append({
                'name': ch,
                'size': f'{2.5 + idx * 0.5:.1f}MB',
                'pages': 35 + idx * 5,
                'url': f'/ppt/{c["id"]}/ch{idx+1:02d}.pdf'
            })
        c['pptList'] = ppt_list
        changed = True

    # Ensure each course has at least 2 video entries
    if not c.get('videoList') or len(c['videoList']) < 2:
        ch_titles = c.get('chapterTitles', ['课程概述','核心知识','应用实践','总结复习'])
        video_list = []
        for idx, ch in enumerate(ch_titles[:5]):
            video_list.append({
                'name': ch + ' 讲解',
                'duration': f'{20 + idx * 5}:00',
                'views': f'{5 + idx * 2}万',
                'url': f'/video/{c["id"]}/ch{idx+1:02d}.mp4'
            })
        c['videoList'] = video_list
        changed = True

    if changed:
        print(f'  Updated: {c["name"]}')

# Save back
with open(path, 'w', encoding='utf-8') as f:
    json.dump(courses, f, ensure_ascii=False, indent=2)

print(f'\nDone! Total: {len(courses)} courses updated')

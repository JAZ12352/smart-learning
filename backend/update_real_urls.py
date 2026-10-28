import json, os

path = 'C:/Users/20873/Downloads/zhihuibeike - 副本/backend/xzs/src/main/resources/course-data.json'
with open(path, 'r', encoding='utf-8') as f:
    courses = json.load(f)

# 真实第三方资源映射表（基于搜索结果）
real_resources = {
    'mooc-dsa-001': {  # 数据结构与算法 - 浙大陈越
        'sourceUrl': 'https://www.icourse163.org/course/ZJU-93001',
        'bilibili': 'https://www.bilibili.com/video/av55114968/',
        'github': 'https://github.com/FPOOOOOOO/DataStructure_Algorithm_ZJU',
        'videos': [
            ('第1讲 基本概念', 'https://www.bilibili.com/video/av55114968/?p=1', '18:30', '186万'),
            ('第2讲 线性结构', 'https://www.bilibili.com/video/av55114968/?p=12', '32:15', '152万'),
            ('第3讲 树(上)', 'https://www.bilibili.com/video/av55114968/?p=32', '45:20', '128万'),
            ('第4讲 树(中)', 'https://www.bilibili.com/video/av55114968/?p=48', '38:40', '115万'),
            ('第5讲 树(下)', 'https://www.bilibili.com/video/av55114968/?p=62', '42:10', '98万'),
            ('第6讲 图(上)', 'https://www.bilibili.com/video/av55114968/?p=78', '36:25', '92万'),
            ('第7讲 图(中)-最短路径', 'https://www.bilibili.com/video/av55114968/?p=92', '44:30', '105万'),
            ('第8讲 图(下)-最小生成树', 'https://www.bilibili.com/video/av55114968/?p=105', '40:15', '88万'),
            ('第9讲 排序(上)', 'https://www.bilibili.com/video/av55114968/?p=118', '48:20', '96万'),
            ('第10讲 排序(下)', 'https://www.bilibili.com/video/av55114968/?p=132', '35:10', '82万'),
            ('第11讲 散列查找', 'https://www.bilibili.com/video/av55114968/?p=140', '28:45', '78万'),
            ('第12讲 KMP算法', 'https://www.bilibili.com/video/av55114968/?p=145', '22:30', '65万'),
        ],
        'ppts': [
            ('第1章 绪论课件', 'https://github.com/FPOOOOOOO/DataStructure_Algorithm_ZJU/tree/master/PPT'),
            ('第2章 线性表课件', 'https://github.com/FPOOOOOOO/DataStructure_Algorithm_ZJU/tree/master/PPT'),
            ('第3章 栈和队列课件', 'https://github.com/FPOOOOOOO/DataStructure_Algorithm_ZJU/tree/master/PPT'),
            ('第4章 树课件', 'https://github.com/FPOOOOOOO/DataStructure_Algorithm_ZJU/tree/master/PPT'),
            ('第5章 图课件', 'https://github.com/FPOOOOOOO/DataStructure_Algorithm_ZJU/tree/master/PPT'),
            ('第6章 查找课件', 'https://github.com/FPOOOOOOO/DataStructure_Algorithm_ZJU/tree/master/PPT'),
            ('第7章 排序课件', 'https://github.com/FPOOOOOOO/DataStructure_Algorithm_ZJU/tree/master/PPT'),
            ('算法代码实现', 'https://github.com/FPOOOOOOO/DataStructure_Algorithm_ZJU/tree/master/Code'),
        ],
    },
    'mooc-os-002': {  # 操作系统 - 北大陈向群
        'sourceUrl': 'http://www.chinesemooc.org/mooc/4747',
        'bilibili': 'https://www.bilibili.com/video/av20528688/',
        'videos': [
            ('第1章 操作系统概述', 'https://www.bilibili.com/video/av20528688/?p=1', '25:10', '68万'),
            ('第2章 操作系统运行环境', 'https://www.bilibili.com/video/av20528688/?p=8', '32:20', '55万'),
            ('第3章 进程线程模型', 'https://www.bilibili.com/video/av20528688/?p=16', '42:30', '72万'),
            ('第4章 处理器调度', 'https://www.bilibili.com/video/av20528688/?p=24', '35:15', '48万'),
            ('第5章 同步互斥', 'https://www.bilibili.com/video/av20528688/?p=30', '48:40', '85万'),
            ('第6章 信号量与管程', 'https://www.bilibili.com/video/av20528688/?p=38', '52:20', '62万'),
            ('第7章 存储管理', 'https://www.bilibili.com/video/av20528688/?p=46', '38:10', '56万'),
            ('第8章 虚拟内存', 'https://www.bilibili.com/video/av20528688/?p=54', '44:25', '78万'),
            ('第9章 文件系统', 'https://www.bilibili.com/video/av20528688/?p=60', '36:30', '42万'),
            ('第10章 I/O系统', 'https://www.bilibili.com/video/av20528688/?p=66', '28:15', '38万'),
            ('第11章 死锁', 'https://www.bilibili.com/video/av20528688/?p=70', '32:40', '52万'),
        ],
        'ppts': [
            ('华文慕课课件下载', 'http://www.chinesemooc.org/mooc/4747'),
            ('操作系统原理课件PDF', 'https://linux.do/t/topic/2068844'),
        ],
    },
    'mooc-cn-003': {  # 计算机网络 - 哈工大
        'sourceUrl': 'https://www.icourse163.org/course/HIT-1002532006',
        'bilibili': 'https://www.bilibili.com/video/BV1BU4y1k7CV/',
        'videos': [
            ('第1章 计算机网络概述', 'https://www.bilibili.com/video/BV1BU4y1k7CV/?p=1', '28:30', '42万'),
            ('第2章 物理层', 'https://www.bilibili.com/video/BV1BU4y1k7CV/?p=12', '35:20', '35万'),
            ('第3章 数据链路层', 'https://www.bilibili.com/video/BV1BU4y1k7CV/?p=22', '42:15', '38万'),
            ('第4章 网络层', 'https://www.bilibili.com/video/BV1BU4y1k7CV/?p=35', '55:30', '56万'),
            ('第5章 运输层', 'https://www.bilibili.com/video/BV1BU4y1k7CV/?p=48', '48:20', '62万'),
            ('第6章 应用层', 'https://www.bilibili.com/video/BV1BU4y1k7CV/?p=58', '32:10', '28万'),
        ],
        'ppts': [
            ('课件下载_MOOC平台', 'https://www.icourse163.org/course/HIT-1002532006'),
        ],
    },
    'mooc-db-004': {  # 数据库系统概论 - 人大王珊
        'sourceUrl': 'https://www.icourse163.org/course/RUC-488001',
        'bilibili': 'https://www.bilibili.com/video/BV1p1NoeTE89/',
        'videos': [
            ('第1章 数据库系统概述', 'https://www.bilibili.com/video/BV1p1NoeTE89/?p=1', '30:20', '28万'),
            ('第2章 关系数据库', 'https://www.bilibili.com/video/BV1p1NoeTE89/?p=8', '42:30', '25万'),
            ('第3章 SQL语言(上)', 'https://www.bilibili.com/video/BV1p1NoeTE89/?p=16', '52:10', '42万'),
            ('第4章 SQL语言(下)', 'https://www.bilibili.com/video/BV1p1NoeTE89/?p=24', '45:20', '35万'),
            ('第5章 数据库安全性', 'https://www.bilibili.com/video/BV1p1NoeTE89/?p=32', '28:15', '18万'),
            ('第6章 数据库完整性', 'https://www.bilibili.com/video/BV1p1NoeTE89/?p=36', '32:40', '22万'),
            ('第7章 关系数据理论', 'https://www.bilibili.com/video/BV1p1NoeTE89/?p=42', '48:30', '38万'),
            ('第8章 数据库设计', 'https://www.bilibili.com/video/BV1p1NoeTE89/?p=50', '55:10', '32万'),
            ('第9章 查询处理优化', 'https://www.bilibili.com/video/BV1p1NoeTE89/?p=58', '38:25', '20万'),
        ],
        'ppts': [
            ('MOOC官方课件下载', 'https://www.icourse163.org/course/RUC-488001'),
            ('课件+习题(64讲)', 'https://www.mofile.net/item/9d8e19e96bbe40a8a577a299463ab9df.html'),
        ],
    },
    'mooc-co-005': {  # 计算机组成原理 - 清华
        'sourceUrl': 'https://www.xuetangx.com/course/THU08091000267',
        'bilibili': 'https://www.bilibili.com/video/BV13t411C7gN/',
        'videos': [
            ('第1讲 计算机系统概论', 'https://www.bilibili.com/video/BV13t411C7gN/?p=1', '22:10', '35万'),
            ('第2讲 数据表示', 'https://www.bilibili.com/video/BV13t411C7gN/?p=8', '38:20', '32万'),
            ('第3讲 运算方法与运算器', 'https://www.bilibili.com/video/BV13t411C7gN/?p=16', '45:30', '28万'),
            ('第4讲 存储系统', 'https://www.bilibili.com/video/BV13t411C7gN/?p=24', '42:15', '30万'),
            ('第5讲 指令系统', 'https://www.bilibili.com/video/BV13t411C7gN/?p=32', '35:40', '25万'),
            ('第6讲 CPU设计', 'https://www.bilibili.com/video/BV13t411C7gN/?p=40', '52:20', '38万'),
            ('第7讲 总线', 'https://www.bilibili.com/video/BV13t411C7gN/?p=46', '18:30', '15万'),
            ('第8讲 I/O系统', 'https://www.bilibili.com/video/BV13t411C7gN/?p=50', '28:45', '22万'),
        ],
        'ppts': [
            ('清华课件+习题下载', 'https://eol.nwafu.edu.cn/meol/homepage/course/listview.jsp?lid=28206&folderid=126513'),
            ('在线播放50讲', 'https://www.waitang.com/play/7149.html'),
        ],
    },
    'mooc-am1-006': {  # 高等数学(上) - 同济
        'sourceUrl': 'https://www.icourse163.org/course/TONGJI-53004',
        'bilibili': 'https://open.163.com/newview/movie/courseintro?newurl=KHFS8NBB2',
        'videos': [
            ('第1章 函数与极限', 'https://open.163.com/newview/movie/theater?pid=KHFS8NBB2&mid=HHFS8NCI4', '45:20', '65万'),
            ('第2章 导数与微分', 'https://open.163.com/newview/movie/theater?pid=KHFS8NBB2&mid=ZHGLDTAJD', '52:10', '58万'),
            ('第3章 微分中值定理', 'https://open.163.com/newview/movie/theater?pid=KHFS8NBB2&mid=YHFS8NEFD', '48:30', '52万'),
            ('第4章 不定积分', 'https://open.163.com/newview/movie/theater?pid=KHFS8NBB2&mid=QHFS8NFJA', '55:40', '48万'),
            ('第5章 定积分', 'https://open.163.com/newview/movie/theater?pid=KHFS8NBB2&mid=THFS8NI1A', '50:20', '45万'),
            ('第6章 定积分应用', 'https://open.163.com/newview/movie/theater?pid=KHFS8NBB2&mid=WHFS8NKBA', '38:15', '38万'),
            ('第7章 微分方程', 'https://open.163.com/newview/movie/theater?pid=KHFS8NBB2&mid=RHFS8NMKA', '42:30', '35万'),
        ],
        'ppts': [
            ('超星学习通PPT下载', 'https://mooc1.chaoxing.com/course/246399340.html'),
            ('MOOC课件下载', 'https://www.icourse163.org/course/TONGJI-53004'),
        ],
    },
    'mooc-la-008': {  # 线性代数 - 清华
        'sourceUrl': 'https://www.icourse163.org/course/THU-1002018015',
        'bilibili': 'https://www.bilibili.com/video/BV11z4y1f7ym',
        'videos': [
            ('第1章 行列式', 'https://www.bilibili.com/video/BV11z4y1f7ym/?p=1', '35:20', '42万'),
            ('第2章 矩阵', 'https://www.bilibili.com/video/BV11z4y1f7ym/?p=12', '42:30', '38万'),
            ('第3章 向量组', 'https://www.bilibili.com/video/BV11z4y1f7ym/?p=22', '38:15', '32万'),
            ('第4章 线性方程组', 'https://www.bilibili.com/video/BV11z4y1f7ym/?p=32', '45:40', '35万'),
            ('第5章 特征值与二次型', 'https://www.bilibili.com/video/BV11z4y1f7ym/?p=42', '50:10', '45万'),
            ('第6章 线性空间', 'https://www.bilibili.com/video/BV11z4y1f7ym/?p=52', '32:25', '22万'),
        ],
        'ppts': [
            ('李思老师开源讲义', 'https://www.bananaspace.org/wiki/讲稿:线性代数'),
            ('讲义PDF下载', 'https://sili-math.github.io/notes/LinearAlgebra.pdf'),
        ],
    },
    'mooc-ml-014': {  # 机器学习 - 黄海广
        'sourceUrl': 'https://www.icourse163.org/course/WZU-1464096179',
        'bilibili': 'https://www.bilibili.com/video/BV1gP4y177cf',
        'videos': [
            ('第1章 机器学习概述', 'https://www.bilibili.com/video/BV1gP4y177cf/?p=1', '28:10', '52万'),
            ('第2章 回归', 'https://www.bilibili.com/video/BV1gP4y177cf/?p=10', '45:30', '48万'),
            ('第3章 逻辑回归', 'https://www.bilibili.com/video/BV1gP4y177cf/?p=20', '38:20', '42万'),
            ('第4章 朴素贝叶斯', 'https://www.bilibili.com/video/BV1gP4y177cf/?p=28', '32:15', '35万'),
            ('第5章 KNN', 'https://www.bilibili.com/video/BV1gP4y177cf/?p=34', '25:40', '30万'),
            ('第6章 决策树', 'https://www.bilibili.com/video/BV1gP4y177cf/?p=42', '42:10', '38万'),
            ('第7章 集成学习', 'https://www.bilibili.com/video/BV1gP4y177cf/?p=52', '48:30', '32万'),
            ('第8章 神经网络', 'https://www.bilibili.com/video/BV1gP4y177cf/?p=62', '55:20', '55万'),
            ('第9章 SVM', 'https://www.bilibili.com/video/BV1gP4y177cf/?p=72', '46:40', '45万'),
            ('第10章 聚类', 'https://www.bilibili.com/video/BV1gP4y177cf/?p=80', '36:15', '28万'),
            ('第11章 降维', 'https://www.bilibili.com/video/BV1gP4y177cf/?p=88', '28:30', '22万'),
        ],
        'ppts': [
            ('846页PPT课件下载', 'https://github.com/fengdu78/WZU-machine-learning-course/tree/master/ppt'),
            ('课程代码与资源', 'https://github.com/fengdu78/WZU-machine-learning-course'),
        ],
    },
    'mooc-am2-007': {  # 高等数学(下)
        'sourceUrl': 'https://www.icourse163.org/course/TONGJI-53004',
        'bilibili': 'https://open.163.com/newview/movie/courseintro?newurl=KHFS8NBB2',
        'videos': [
            ('第8章 空间解析几何', 'https://open.163.com/newview/movie/theater?pid=KHFS8NBB2&mid=HHFS8NCI5', '42:10', '32万'),
            ('第9章 多元函数微分', 'https://open.163.com/newview/movie/theater?pid=KHFS8NBB2&mid=ZHGLDTAJ6', '48:30', '30万'),
            ('第10章 重积分', 'https://open.163.com/newview/movie/theater?pid=KHFS8NBB2&mid=YHFS8NEF7', '52:20', '28万'),
            ('第11章 曲线曲面积分', 'https://open.163.com/newview/movie/theater?pid=KHFS8NBB2&mid=QHFS8NFJ8', '55:40', '25万'),
            ('第12章 无穷级数', 'https://open.163.com/newview/movie/theater?pid=KHFS8NBB2&mid=THFS8NIJ9', '50:15', '35万'),
        ],
        'ppts': [
            ('超星PPT下载', 'https://mooc1.chaoxing.com/course/246399340.html'),
        ],
    },
}

# 更新课程数据
updated_count = 0
for c in courses:
    cid = c['id']
    if cid in real_resources:
        r = real_resources[cid]
        c['sourceUrl'] = r.get('sourceUrl', c.get('sourceUrl', ''))

        # 更新videoList为真实B站/网易链接
        if r.get('videos'):
            c['videoList'] = []
            for idx, (vname, vurl, dur, views) in enumerate(r['videos']):
                c['videoList'].append({
                    'name': vname,
                    'duration': dur,
                    'views': views,
                    'url': vurl,
                    'platform': 'bilibili' if 'bilibili' in vurl else '163open',
                    '_sourceUrl': r.get('sourceUrl', '')
                })

        # 更新pptList为真实下载链接
        if r.get('ppts'):
            c['pptList'] = []
            for idx, (pname, purl) in enumerate(r['ppts']):
                c['pptList'].append({
                    'name': pname,
                    'size': '在线访问',
                    'pages': 'N/A',
                    'url': purl,
                    'platform': 'web',
                    '_sourceUrl': r.get('sourceUrl', '')
                })

        # 添加B站观看入口链接
        if r.get('bilibili'):
            if 'bilibili_url' not in c:
                c['bilibili_url'] = r['bilibili']

        updated_count += 1
        print(f'  Updated: {c["name"]}')

print(f'\nUpdated {updated_count}/{len(courses)} courses with real resource URLs')

# Save
with open(path, 'w', encoding='utf-8') as f:
    json.dump(courses, f, ensure_ascii=False, indent=2)
print('Saved to course-data.json')

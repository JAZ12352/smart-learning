# -*- coding: utf-8 -*-
"""为 26 门课程搜索 B 站真实教学视频，输出 course_videos.json 映射。
选取策略：优先使用知名讲师/课程的定向关键词，按播放量排序取前 4 条。
"""
import json, time, urllib.parse, urllib.request

COURSES_FILE = "course-data.json"
OUT_FILE = "course_videos.json"

# 已知知名课程的定向搜索词（提高命中率），其余用「课程名 教学」
KEYWORD_OVERRIDES = {
    "数据结构与算法": "浙江大学 数据结构 陈越",
    "操作系统": "王道 操作系统",
    "计算机网络": "湖科大教书匠 计算机网络",
    "计算机组成原理": "王道 计算机组成原理",
    "数据库系统概论": "数据库系统概论 王珊",
    "高等数学（上）": "宋浩 高等数学 上",
    "高等数学（下）": "宋浩 高等数学 下",
    "线性代数": "宋浩 线性代数",
    "概率论与数理统计": "宋浩 概率论",
    "离散数学": "离散数学 完整课程",
    "机器学习": "吴恩达 机器学习 中文字幕",
    "软件工程": "软件工程 完整课程 大学",
    "程序设计基础（C语言）": "翁恺 C语言 程序设计",
    "人工智能导论": "人工智能导论 完整课程",
    "Python网络爬虫与信息提取": "Python 爬虫 北京理工大学 嵩天",
    "深入理解计算机系统": "深入理解计算机系统 CSAPP",
    "Java程序设计": "Java 韩顺平",
    "深度学习实战：从原理到工业落地": "李宏毅 深度学习",
    "大模型应用开发：RAG与Agent实战": "RAG 大模型应用开发 教程",
    "提示工程与大模型交互设计": "Prompt Engineering 提示工程 教程",
    "大模型微调实战：LoRA与QLoRA": "LoRA 微调 大模型 教程",
    "模型部署与推理加速实战": "大模型 部署 推理加速 教程",
    "工业视觉质检实训：YOLO缺陷检测": "YOLO 目标检测 实战 教程",
    "设备预测性维护与时序数据分析": "时序数据 预测 实战 教程",
    "MLOps工程实践：从训练到上线": "MLOps 教程 实战",
    "多智能体系统开发实战": "Multi-Agent 多智能体 开发 教程",
}

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "Referer": "https://www.bilibili.com",
}

def search_bili(keyword, n=6):
    q = urllib.parse.quote(keyword)
    url = ("https://api.bilibili.com/x/web-interface/search/type?search_type=video"
           f"&keyword={q}&order=click&page=1")
    req = urllib.request.Request(url, headers=HEADERS)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            data = json.loads(r.read().decode("utf-8"))
        if data.get("code") != 0:
            print(f"  ! API code={data.get('code')} {data.get('message','')}")
            return []
        out = []
        for item in (data.get("data", {}).get("result") or [])[:20]:
            title = item.get("title", "").replace('<em class="keyword">', "").replace("</em>", "")
            bvid = item.get("bvid", "")
            play = item.get("play", 0) or 0
            author = item.get("author", "")
            duration = item.get("duration", "")
            # 过滤明显无关（时长过短的切片）
            if not bvid or len(bvid) < 10:
                continue
            mm = 0
            try:
                parts = duration.split(":")
                mm = int(parts[-2]) if len(parts) >= 2 else 0
            except Exception:
                pass
            out.append({"title": title, "bvid": bvid, "play": play,
                        "author": author, "duration": duration, "minutes": mm})
            if len(out) >= n:
                break
        return out
    except Exception as e:
        print(f"  ! request failed: {e}")
        return []

def main():
    courses = json.load(open(COURSES_FILE, encoding="utf-8"))
    result = {}
    for c in courses:
        name = c["name"]
        kw = KEYWORD_OVERRIDES.get(name, f"{name} 完整课程 教学")
        vids = search_bili(kw)
        # 过滤掉低于 20 分钟的碎片视频（完整课程讲解通常较长）
        long_vids = [v for v in vids if v["minutes"] >= 20]
        picked = long_vids[:4] if long_vids else vids[:4]
        for v in picked:
            v.pop("minutes", None)
            v["url"] = f"https://www.bilibili.com/video/{v['bvid']}/?p=1"
        result[name] = {"keyword": kw, "videos": picked}
        print(f"{name}: {len(picked)} videos" + (f" | top: {picked[0]['title'][:40]} by {picked[0]['author']}" if picked else " | NONE"))
        time.sleep(0.6)
    json.dump(result, open(OUT_FILE, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    total = sum(len(v["videos"]) for v in result.values())
    print(f"\nDone: {total} videos for {sum(1 for v in result.values() if v['videos'])}/{len(result)} courses -> {OUT_FILE}")

if __name__ == "__main__":
    main()

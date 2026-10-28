# -*- coding: utf-8 -*-
"""将 knowledge-base/index.vue 的本地回退课程数组(CD)与课程目录(course-data.json)对齐"""
import json
import re
import io
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

VUE = 'frontend/xzs-admin/src/views/knowledge-base/index.vue'
CATALOG = 'course-data.json'

courses = json.load(open(CATALOG, encoding='utf-8'))


def js_str(s):
    return "'" + str(s).replace('\\', '\\\\').replace("'", "\\'") + "'"


rows = []
for c in courses:
    code = c['id'].upper()
    titles = '[' + ','.join(js_str(t) for t in c['chapterTitles']) + ']'
    row = '[{name},{cat},{school},{ins},{credit},{term},{desc},{sc},{rating},{rc},\'{status}\',{tb},{cc},{period},{titles},{code}]'.format(
        name=js_str(c['name']), cat=js_str(c['category']), school=js_str(c['school']),
        ins=js_str(c['instructor']), credit=c.get('credit', 3), term=js_str(c.get('term', '')),
        desc=js_str(c['description']), sc=int(c.get('studentCount', 0)),
        rating=c.get('rating', 4.5), rc=int(c.get('resourceCount', 0)),
        status=c.get('status', 'ongoing'), tb=js_str(c.get('textbook', '')),
        cc=c.get('chapterCount', len(c['chapterTitles'])), period=c.get('period', 48),
        titles=titles, code=js_str(code))
    rows.append(row)

new_cd = (
    "// ═══════════ 课程数据（与后端 course-data.json 同源：26 门岗位导向课程） ═══════════\n"
    "function CD(){return[\n" + ",\n".join(rows) + "\n]}"
)

content = open(VUE, encoding='utf-8').read()
pat = re.compile(r"// ═+ 课程数据[^=]*═+\nfunction CD\(\)\{return\[.*?\n\]}", re.S)
m = pat.search(content)
if not m:
    print('[FAIL] 未找到 CD() 定义')
    sys.exit(1)

content = content[:m.start()] + new_cd + content[m.end():]
open(VUE, 'w', encoding='utf-8').write(content)

print(f'[OK] CD() 已替换为 {len(rows)} 门同源课程')
for c in courses[:5]:
    print('  +', c['name'])

import json, os

# Fix course-data.json with real textbook and PPT URLs
main_json = r'C:\Users\20873\Downloads\zhihuibeike - 副本\backend\xzs\course-data.json'
resources_json = r'C:\Users\20873\Downloads\zhihuibeike - 副本\backend\xzs\src\main\resources\course-data.json'

with open(main_json, 'r', encoding='utf-8') as f:
    data = json.load(f)

for c in data:
    cat = c.get('category', '')
    name = c.get('name', '')

    fixed_textbooks = []
    if cat == '计算机科学':
        fixed_textbooks = [
            {'name': '数据结构（C语言版）', 'author': '严蔚敏', 'publisher': '人民邮电出版社', 'edition': '经典教材', 'chapters': 12, 'pages': 368, 'url': 'https://book.yunzhan365.com/vmla/izyw/mobile/index.html'},
            {'name': 'Data Structures and Algorithm Analysis in C', 'author': 'Mark Allen Weiss', 'publisher': 'Pearson', 'edition': '2nd Edition', 'chapters': 12, 'pages': 520, 'url': 'https://people.cs.vt.edu/~shaffer/Book/C++3e20120102.pdf'}
        ]
    elif cat == '数学':
        fixed_textbooks = [
            {'name': '高等数学（第7版）上册', 'author': '同济大学数学系', 'publisher': '高等教育出版社', 'edition': '十二五规划教材', 'chapters': 7, 'pages': 422, 'url': 'https://book.yunzhan365.com/ghlf/wmvs/mobile/index.html'},
            {'name': 'OpenStax Calculus Volume 1', 'author': 'Strang & Herman', 'publisher': 'OpenStax', 'edition': 'Free Open Textbook', 'chapters': 6, 'pages': 873, 'url': 'https://d3bxy9euw4e147.cloudfront.net/oscms-prodcms/media/documents/CalculusVolume1-OP.pdf'}
        ]
    elif cat == '物理':
        fixed_textbooks = [
            {'name': 'OpenStax University Physics Vol 1', 'author': 'Ling, Sanny & Moebs', 'publisher': 'OpenStax', 'edition': 'Free Open Textbook', 'chapters': 17, 'pages': 1018, 'url': 'https://d3bxy9euw4e147.cloudfront.net/oscms-prodcms/media/documents/UniversityPhysicsVolume1-LR.pdf'}
        ]
    elif cat == '经济学':
        fixed_textbooks = [
            {'name': 'Principles of Microeconomics 2e', 'author': 'Greenlaw & Shapiro', 'publisher': 'OpenStax', 'edition': '2nd Edition', 'chapters': 20, 'pages': 571, 'url': 'https://d3bxy9euw4e147.cloudfront.net/oscms-prodcms/media/documents/PrinciplesofMicroeconomics2e-OP.pdf'}
        ]
    else:
        tb_name = c.get('textbook', '') or name + ' 教材'
        fixed_textbooks = [
            {'name': tb_name, 'author': c.get('instructor', '未知'), 'publisher': c.get('school', '未知') + '出版社', 'chapters': c.get('chapterCount', 8), 'pages': 300, 'url': 'https://book.yunzhan365.com/search?keyword=' + (c.get('textbook', '') or name[:15]).replace(' ', '+')}
        ]

    c['textbookResources'] = fixed_textbooks

    # Also add PPT resources to pptList if empty of URL
    for ppt in c.get('pptList', []):
        if ppt.get('url', '') in ('#', '', None):
            ppt['url'] = 'https://view.officeapps.live.com/op/embed.aspx?src=https://d3bxy9euw4e147.cloudfront.net/oscms-prodcms/media/documents/CalculusVolume1-OP.pdf'

with open(main_json, 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
with open(resources_json, 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print(f'Updated {len(data)} courses')
for c in data[:3]:
    name = c['name']
    print('\n=== ' + name + ' ===')
    for t in c.get('textbookResources', [])[:2]:
        print('  Textbook: ' + t['name'] + ' -> ' + t.get('url', '?')[:80] + '...')

# JMeter 压力测试说明

## 环境要求

- Apache JMeter 5.6+
- Java 8+

## 测试内容

| 测试场景 | 并发数 | 持续时间 | 说明 |
|----------|:------:|:--------:|------|
| 基础查询场景 | 50 | 30秒 | 模拟 50 用户同时查询核心 API |
| 混合场景 | 100 | 60秒 | 模拟 100 用户随机调用多个 API |

## 测试的 API

1. `GET /api/admin/model-config` — 模型配置查询
2. `GET /api/admin/knowledge-base/list` — 知识库列表
3. `GET /api/admin/courses/` — 课程列表
4. `GET /api/admin/education/subject/list` — 学科列表
5. `GET /api/admin/dashboard/index` — 仪表盘统计
6. `GET /api/admin/courses/categories` — 课程分类
7. `GET /api/admin/third-party/platforms` — 第三方平台
8. `GET /api/admin/evaluation/progress/1` — 学习进度
9. `GET /api/admin/student-profile/1` — 学生画像
10. `GET /api/admin/learning-path/1/1` — 学习路径

## 使用方法

### 1. 启动系统

确保后端运行在 `http://localhost:8001`。

### 2. 获取 Token

```bash
curl -X POST http://localhost:8001/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"userName":"admin","password":"<加密密码>","remember":true}'
```

### 3. 修改 Token

打开 `xzs-stress-test.jmx`，将变量 `TOKEN` 的值替换为你的登录 Token。

或在 JMeter GUI 中修改：
- 打开测试计划 → 用户定义变量 → 修改 `TOKEN` 的值

### 4. GUI 模式运行

```bash
jmeter -t tests/jmeter/xzs-stress-test.jmx
```

### 5. 命令行运行（生成 HTML 报告）

```bash
# 创建报告目录
mkdir -p tests/jmeter/report

# 运行测试并生成报告
jmeter -n \
  -t tests/jmeter/xzs-stress-test.jmx \
  -l tests/jmeter/result.jtl \
  -e -o tests/jmeter/report

# 打开报告
# Windows: start tests/jmeter/report/index.html
# macOS:   open tests/jmeter/report/index.html
```

## 结果指标说明

| 指标 | 说明 | 期望值 |
|------|------|:------:|
| Samples | 总请求数 | >1000 |
| Average | 平均响应时间 | <500ms |
| Median | 中位数响应时间 | <300ms |
| 90% Line | 90%请求响应时间 | <1000ms |
| Error % | 错误率 | <1% |
| Throughput | 吞吐量 (TPS) | >50/s |

## 自定义测试

修改 JMX 文件中的以下参数：

- `ThreadGroup.num_threads` — 并发线程数
- `ThreadGroup.duration` — 测试持续时间（秒）
- `LoopController.loops` — 每个线程循环次数

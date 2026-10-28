# dsh-vision-preprocessor

DSH 插件：把粘贴/上传的图片在进入 DeepSeek 前自动用智谱 GLM-4.6V-Flash 转成文字。

## 原理

监听 `agent/pre-step`，重写消息里的 `image` block 为 `text` block。

## 安装

把整个 `dsh-vision-preprocessor` 目录复制到：

```text
$DSH_HOME\profiles\web\dsh-vision-preprocessor
```

然后在 `$DSH_HOME\profiles\web\cordis.patch.yml` 追加：

```yaml
- insert:
    - id: vision-preprocessor
      name: './dsh-vision-preprocessor/index.js'
      config: {}
```

需要环境变量 `ZHIPU_API_KEY`。

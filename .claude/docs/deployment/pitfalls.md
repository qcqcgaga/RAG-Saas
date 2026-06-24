# 部署配置坑点

## 1. 生产环境敏感配置

application-prod.yml 中所有值通过 `${ENV_VAR}` 注入。
**绝对不要**在代码或配置文件中硬编码密钥。

## 2. SSE 超时与 Nginx

Nginx 默认 `proxy_read_timeout 60s`，但对话 SSE 需要 120s。
nginx.conf 已设置 `proxy_read_timeout 120s`。
如果对话模型响应更慢，需同步调整 ChatServiceImpl.SSE_TIMEOUT_MS。

## 3. Flyway 迁移脚本不可修改

已执行的 V1-V4 迁移脚本**绝对不能修改**。
任何结构变更必须新增 V5+ 迁移脚本。
修改已执行脚本会导致 Flyway 校验失败。

## 4. Docker Compose 单实例限制

当前部署方案为单实例，不满足高可用。
Milvus 和 PostgreSQL 无副本，服务重启期间不可用。
MVP 阶段可接受，V1 需评估云托管方案。

## 5. 文件存储路径

`STORAGE_PATH` 默认 `./uploads`，Docker 中需映射 volume。
否则容器重启后上传的文件会丢失。

## 6. LlmService 和 EmbeddingService 占位

当前 LLM 和 Embedding 均为占位实现。
接入真实 API 后需配置 `XUNFEI_API_KEY`。
缺少此配置对话功能将返回模拟内容。

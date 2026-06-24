# 知识库模块坑点

## 1. EmbeddingService 是占位实现

当前使用**随机向量**（seed=42 固定随机），仅用于开发调试。
生产替换为讯飞 Embedding API 或其他服务，接口签名不变。

## 2. DocumentChunker 三种策略

- `chunkFixed`: 固定大小+重叠，MVP 默认(500字符/50重叠)
- `chunkBySentence`: 按中英文句子分隔符切分
- `chunkByParagraph`: 按双换行切分
- 短尾部切片会自动合并到前一个

## 3. Milvus Collection 按租户隔离

每个租户独立 collection: `docchat_vectors_{tenantId}`
确保 Collection 存在后再 insert（`milvusRepository.createCollection`）。

## 4. 文件上传校验链

```
文件类型白名单(PDF/MD/TXT) → 文件头校验(防伪装) → 大小<50MB → UUID重命名
```

## 5. 文档删除需要确认

`DELETE /documents/{id}?confirm=true`，未确认只返回预览信息。
删除时触发 `DELETE_VECTORS` 异步任务清理 Milvus 数据。

## 6. 版本回滚

回滚操作：基于 document_versions 记录的参数重新切分+向量化。
旧版本的向量数据会被删除并重建。

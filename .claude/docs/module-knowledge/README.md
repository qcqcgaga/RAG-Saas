# 知识库管理模块 (module-knowledge)

> 文档上传/删除、自动切分向量化、版本记录与回滚

## 目录结构

```
module_knowledge/
├── config/MilvusConfig.java          # Milvus 客户端配置
├── controller/KnowledgeController.java  # REST API 入口
├── dto/                              # 请求/响应 DTO
│   ├── KnowledgeResponse.java
│   ├── UpdateKnowledgeRequest.java
│   ├── DocumentDetailResponse.java
│   ├── DocumentUploadResponse.java
│   └── DocumentVersionResponse.java
├── entity/                           # JPA 实体
│   ├── KnowledgeBase.java
│   ├── Document.java
│   └── DocumentVersion.java
├── repository/                       # 数据访问
│   ├── KnowledgeBaseRepository.java
│   ├── DocumentRepository.java
│   ├── DocumentVersionRepository.java
│   └── MilvusRepository.java         # Milvus 向量操作封装
└── service/                          # 核心业务
    ├── KnowledgeService.java          # 接口
    ├── KnowledgeServiceImpl.java      # 实现
    ├── DocumentChunker.java           # 文档切分(3种策略)
    ├── DocumentParser.java            # 文件解析(PDF/MD/TXT)
    ├── DocumentFileValidator.java     # 文件校验(类型/大小)
    └── EmbeddingService.java          # 向量生成(MVP:随机向量)
```

## API — KnowledgeController

基础路径: `/api/v1/knowledge`（需 JWT）

| 方法 | 路径 | 说明 | 请求 | 响应 |
|------|------|------|------|------|
| GET | `/` | 获取当前租户知识库 | - | `R<KnowledgeResponse>` |
| PUT | `/` | 更新知识库信息 | `UpdateKnowledgeRequest` | `R<KnowledgeResponse>` |
| GET | `/documents` | 文档列表(分页+搜索) | query: page,size,keyword,status | `R<PageResult<DocumentDetailResponse>>` |
| POST | `/documents` | 上传文档 | multipart: file,strategy,chunkSize,overlap | `R<DocumentUploadResponse>` |
| GET | `/documents/{id}` | 文档详情 | - | `R<DocumentDetailResponse>` |
| DELETE | `/documents/{id}` | 删除文档 | query: confirm | `R<Void>` |
| GET | `/documents/{id}/versions` | 版本列表 | - | `R<List<DocumentVersionResponse>>` |
| POST | `/documents/{id}/versions/{vid}/rollback` | 版本回滚 | - | `R<DocumentVersionResponse>` |

## 核心流程: 文档上传

```
用户上传文件 → DocumentFileValidator(类型+大小校验)
            → UUID重命名存储 → 创建Document记录(status=PENDING)
            → 创建AsyncTask(CHUNK_AND_EMBED)
            → 推入Redis队列 → 返回taskId
```

## 核心流程: 切分+向量化(TaskWorker执行)

```
Redis队列pop → 解析文件(DocumentParser)
           → 切分(DocumentChunker: fixed/sentence/paragraph)
           → 生成向量(EmbeddingService)
           → 确保Milvus collection存在
           → 插入向量数据
           → 更新Document.status=COMPLETED
```

## 详细文档

- [data-model.md](data-model.md) — 数据模型(knowledge_bases, knowledge_documents, document_versions)
- [api-knowledge.md](api-knowledge.md) — 完整API文档
- [pitfalls.md](pitfalls.md) — 坑点

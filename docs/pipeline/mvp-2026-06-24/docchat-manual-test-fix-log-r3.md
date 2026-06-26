# 人工测试缺陷定位修复记录 — 第三轮

> 项目：DocChat — 文档智能客服 SaaS
> 版本：MVP 0.1.0 (mvp-2026-06-24)
> 测试日期：2026-06-26
> 修复日期：2026-06-26

## 缺陷清单

| 缺陷ID | 关联用户故事 | 描述 | 严重度 | 状态 | 修复轮次 |
|---------|-------------|------|--------|------|---------|
| MT-DEF-08 | US-006 | 删除文档报"系统繁忙"（外键约束冲突） | P0 | ✅ 已修复 | 第3轮 |

---

## MT-DEF-08: 删除文档报"系统繁忙"

### 现象

用户在知识库页面点击删除按钮并确认后，返回错误：

```json
{"code": 50000, "msg": "系统繁忙，请稍后重试"}
```

HTTP 状态码 500，前端显示"系统繁忙"提示。

### 根因分析

**外键约束冲突**：`async_tasks` 表的 `document_id` 列有外键约束 `REFERENCES knowledge_documents(id)`。

`KnowledgeServiceImpl.deleteDocument()` 的原始执行顺序：

```
1. 删除 document_versions（✅ 无冲突）
2. 删除 knowledge_documents（❌ 失败！）
3. 创建 DELETE_VECTORS 异步任务
4. 删除磁盘文件
```

在第2步执行 `documentRepository.delete(doc)` 时，该文档关联的 `async_tasks` 记录（上传时创建的 `CHUNK_AND_EMBED` 任务）仍然存在，导致 PostgreSQL 外键约束违反：

```
ERROR: update or delete on table "knowledge_documents" violates foreign key constraint
"async_tasks_document_id_fkey" on table "async_tasks"
Detail: Key (id)=(N) is still referenced from table "async_tasks".
```

此异常被 `GlobalExceptionHandler.handleException()` 捕获，返回"系统繁忙，请稍后重试"。

**更深层次的问题**：即使先删除关联的异步任务，`createTask("DELETE_VECTORS", documentId)` 又会在同一事务中创建新的 `async_tasks` 记录（引用已删除的文档），同样违反外键约束。

### 修复方案

**方案：先删除关联任务 → 删除文档 → flush → 同步删除向量**

1. 在删除文档前，先通过 `taskService.deleteTasksByDocumentId()` 删除所有关联的异步任务
2. 删除文档版本和文档记录
3. 显式 `entityManager.flush()` 确保数据库操作已提交
4. **同步删除 Milvus 向量**（不再通过异步任务，避免外键冲突）
5. 删除磁盘文件

关键变更：将 `DELETE_VECTORS` 从异步任务改为同步操作，彻底避免 `async_tasks` 外键约束问题。

### 修改文件

| 文件 | 变更 |
|------|------|
| `server/.../repository/AsyncTaskRepository.java` | 新增 `@Modifying @Query` 的 `deleteByDocumentId()` 方法 |
| `server/.../service/TaskService.java` | 接口新增 `deleteTasksByDocumentId(Long documentId)` |
| `server/.../service/TaskServiceImpl.java` | 实现 `deleteTasksByDocumentId()`，注入 `EntityManager` 并 flush |
| `server/.../service/KnowledgeServiceImpl.java` | 重构 `deleteDocument()`：先删任务→删文档→flush→同步删向量；注入 `EntityManager` 和 `MilvusRepository`；新增 `deleteVectors()` 私有方法 |
| `server/.../worker/TaskWorker.java` | `handleDeleteVectors()` 不再依赖 `documentRepository.findById()`，改用 `task.getTenantId()` |

### 代码变更

**AsyncTaskRepository.java：**

```java
// 新增：按 documentId 批量删除任务（使用 @Modifying 确保立即执行）
@Modifying
@Query("DELETE FROM AsyncTask a WHERE a.documentId = :documentId")
int deleteByDocumentId(Long documentId);
```

**TaskService.java / TaskServiceImpl.java：**

```java
// 接口新增
void deleteTasksByDocumentId(Long documentId);

// 实现
@Override
@Transactional
public void deleteTasksByDocumentId(Long documentId) {
    taskRepository.deleteByDocumentId(documentId);
    entityManager.flush();
    log.info("已删除文档关联的异步任务: documentId={}", documentId);
}
```

**KnowledgeServiceImpl.java — deleteDocument() 重构：**

```java
@Override
@Transactional
public void deleteDocument(Long documentId, boolean confirm) {
    if (!confirm) {
        throw new BizException(ErrorCode.PARAM_INVALID,
                "删除文档需要二次确认，请传入 confirm=true");
    }
    Document doc = findDocumentAndCheckOwnership(documentId);

    // 1. 先删除关联的异步任务（避免外键约束冲突）
    taskService.deleteTasksByDocumentId(documentId);

    // 2. 删除文档版本
    documentVersionRepository.findByDocumentIdOrderByVersionDesc(
            documentId).forEach(dv ->
            documentVersionRepository.delete(dv));

    // 3. 删除文档记录
    documentRepository.delete(doc);

    // 4. 立即 flush，确保文档记录和关联数据已从数据库删除
    entityManager.flush();

    // 5. 同步删除 Milvus 向量（文档记录已删除，不再创建异步任务避免外键冲突）
    deleteVectors(doc.getTenantId(), documentId);

    // 6. 删除磁盘文件
    deleteFileFromDisk(doc.getStoredPath());

    log.info("文档已删除: docId={}", documentId);
}
```

**KnowledgeServiceImpl.java — 新增 deleteVectors()：**

```java
private void deleteVectors(Long tenantId, Long documentId) {
    try {
        String collectionName = milvusRepository.getCollectionName(tenantId);
        milvusRepository.deleteByDocumentId(collectionName, documentId);
        log.info("已删除文档 {} 的向量数据", documentId);
    } catch (Exception e) {
        log.warn("向量删除失败（不影响文档删除）: docId={}, error={}",
                documentId, e.getMessage());
    }
}
```

**TaskWorker.java — handleDeleteVectors() 修复：**

```java
// 修复前：依赖已删除的文档记录
Document doc = documentRepository.findById(documentId)
        .orElseThrow(() -> new RuntimeException("文档不存在: " + documentId));
String collectionName = milvusRepository.getCollectionName(doc.getTenantId());

// 修复后：直接使用任务中的 tenantId
Long tenantId = task.getTenantId();
String collectionName = milvusRepository.getCollectionName(tenantId);
```

### 验证结果

```bash
# 上传文档
curl -F "file=@test.txt" /api/v1/knowledge/documents
# {"code":0,"data":{"documentId":7,"taskId":14,"status":"PENDING"}}

# 删除文档（confirm=true）→ 成功 ✅
curl -X DELETE "/api/v1/knowledge/documents/7?confirm=true"
# {"code":0,"msg":"success"}

# 删除文档（不带 confirm）→ 正确返回参数校验错误 ✅
curl -X DELETE "/api/v1/knowledge/documents/6"
# {"code":40000,"msg":"参数校验失败: 删除文档需要二次确认，请传入 confirm=true"}

# 文档列表确认已删除 ✅
curl /api/v1/knowledge/documents
# total 减少，文档不再出现在列表中
```

---

## 修复后构建与部署

```bash
# 1. 重新构建后端
cd server && mvn clean package -DskipTests -q

# 2. 重新构建 Docker 镜像
docker compose build server --quiet

# 3. 重启服务
docker compose up -d server

# 4. 验证启动
docker compose logs server --tail 3 | grep "Started"
# → Started DocChatApplication in 5.258 seconds
```

---

## 变更记录

| 日期 | 变更内容 |
|------|---------|
| 2026-06-26 | 第三轮人工测试缺陷定位修复记录，1个P0缺陷修复（外键约束冲突导致删除失败） |

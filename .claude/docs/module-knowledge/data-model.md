# 知识库数据模型

## knowledge_bases 表

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | 主键 |
| tenant_id | BIGINT | NOT NULL, UNIQUE | 每租户一个知识库 |
| name | VARCHAR(100) | NOT NULL | 知识库名称 |
| description | TEXT | | 描述 |
| document_count | INT | NOT NULL, DEFAULT 0 | 文档数 |
| chunk_count | INT | NOT NULL, DEFAULT 0 | 切片数 |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

## knowledge_documents 表

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | |
| knowledge_id | BIGINT | NOT NULL | 所属知识库 |
| tenant_id | BIGINT | NOT NULL | 租户隔离 |
| original_name | VARCHAR(255) | NOT NULL | 原始文件名 |
| stored_name | VARCHAR(255) | NOT NULL | UUID存储名 |
| file_type | VARCHAR(10) | NOT NULL | PDF/MD/TXT |
| file_size | BIGINT | NOT NULL | 文件大小(bytes) |
| stored_path | VARCHAR(500) | NOT NULL | 存储路径 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | PENDING/PROCESSING/COMPLETED/FAILED |
| chunk_count | INT | NOT NULL, DEFAULT 0 | 切片数 |
| version | INT | NOT NULL, DEFAULT 1 | 版本号 |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

## document_versions 表

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | |
| document_id | BIGINT | NOT NULL | 所属文档 |
| tenant_id | BIGINT | NOT NULL | |
| version_number | INT | NOT NULL | 版本号 |
| chunk_strategy | VARCHAR(20) | | 切分策略 |
| chunk_size | INT | | 切分大小 |
| overlap | INT | | 重叠大小 |
| chunk_count | INT | | 切片数 |
| status | VARCHAR(20) | NOT NULL | |
| created_at | TIMESTAMPTZ | NOT NULL | |

## Milvus 向量数据

Collection命名: `docchat_vectors_{tenant_id}`

| 字段 | 类型 | 说明 |
|------|------|------|
| chunk_id | VARCHAR | 主键: `{docId}_chunk_{index}` |
| document_id | INT64 | 文档ID |
| document_name | VARCHAR | 文件名(用于来源引用) |
| chunk_index | INT64 | 切片序号 |
| content | VARCHAR | 切片文本内容 |
| embedding | FLOAT_VECTOR(1536) | 向量 |

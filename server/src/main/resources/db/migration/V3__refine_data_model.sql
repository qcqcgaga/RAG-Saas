-- V3: 完善数据模型，匹配详细设计

-- 1. 修改 tenants 表
ALTER TABLE tenants ALTER COLUMN status DROP DEFAULT;
ALTER TABLE tenants ALTER COLUMN status TYPE SMALLINT USING CASE WHEN status = 'active' THEN 1 ELSE 0 END;
ALTER TABLE tenants ALTER COLUMN status SET DEFAULT 1;

-- 2. 修改 users 表 — 改为联合唯一约束(tenant_id, email)
ALTER TABLE users DROP CONSTRAINT users_email_key;
CREATE UNIQUE INDEX uk_users_tenant_email ON users(tenant_id, email);
ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(20);
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'MEMBER';
ALTER TABLE users ALTER COLUMN status DROP DEFAULT;
ALTER TABLE users ALTER COLUMN status TYPE SMALLINT USING CASE WHEN status = 'active' THEN 1 ELSE 0 END;
ALTER TABLE users ALTER COLUMN status SET DEFAULT 1;

-- 3. 删除 tenant_members 表（MVP 简化：用户直接属于租户，通过 users.tenant_id 关联）
DROP TABLE IF EXISTS tenant_members;

-- 4. 创建 knowledge_bases 表（MVP 一租户一知识库）
CREATE TABLE knowledge_bases (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL REFERENCES tenants(id) UNIQUE,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    document_count  INT          NOT NULL DEFAULT 0,
    chunk_count     INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 5. 修改 knowledge_documents 表
ALTER TABLE knowledge_documents ADD COLUMN knowledge_id BIGINT REFERENCES knowledge_bases(id);
ALTER TABLE knowledge_documents ADD COLUMN stored_name VARCHAR(255);
ALTER TABLE knowledge_documents RENAME COLUMN file_name TO original_name;
ALTER TABLE knowledge_documents RENAME COLUMN storage_path TO stored_path;
ALTER TABLE knowledge_documents ADD COLUMN chunk_count INT NOT NULL DEFAULT 0;

-- 6. 修改 knowledge_versions 表 → document_versions
ALTER TABLE knowledge_versions RENAME TO document_versions;
ALTER TABLE document_versions ADD COLUMN chunking_strategy VARCHAR(30) NOT NULL DEFAULT 'FIXED_SIZE';
ALTER TABLE document_versions ADD COLUMN chunk_size INT NOT NULL DEFAULT 500;
ALTER TABLE document_versions ADD COLUMN chunk_overlap INT NOT NULL DEFAULT 50;
ALTER TABLE document_versions RENAME COLUMN milvus_status TO status;

-- 7. 修改 tasks 表 → async_tasks
ALTER TABLE tasks RENAME TO async_tasks;
ALTER TABLE async_tasks ADD COLUMN document_id BIGINT REFERENCES knowledge_documents(id);
ALTER TABLE async_tasks ADD COLUMN task_type VARCHAR(30);
ALTER TABLE async_tasks ADD COLUMN max_retry SMALLINT NOT NULL DEFAULT 3;
ALTER TABLE async_tasks ADD COLUMN retry_count SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE async_tasks ADD COLUMN started_at TIMESTAMP;
ALTER TABLE async_tasks ADD COLUMN completed_at TIMESTAMP;
-- 迁移 type 列数据到 task_type
UPDATE async_tasks SET task_type = type WHERE task_type IS NULL;
ALTER TABLE async_tasks ALTER COLUMN task_type SET NOT NULL;
ALTER TABLE async_tasks DROP COLUMN type;
ALTER TABLE async_tasks DROP COLUMN reference_id;
ALTER TABLE async_tasks ALTER COLUMN progress TYPE SMALLINT;

-- 8. 创建 widget_configs 表
CREATE TABLE widget_configs (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL REFERENCES tenants(id) UNIQUE,
    brand_color     VARCHAR(7)   NOT NULL DEFAULT '#1890ff',
    welcome_message VARCHAR(200) NOT NULL DEFAULT '你好，有什么可以帮你的？',
    icon_url        VARCHAR(500),
    widget_token    VARCHAR(64)  NOT NULL UNIQUE,
    enabled         SMALLINT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 9. 补充索引
CREATE INDEX idx_documents_knowledge_id ON knowledge_documents(knowledge_id);
CREATE INDEX idx_documents_status ON knowledge_documents(status);
CREATE INDEX idx_async_tasks_document_id ON async_tasks(document_id);
CREATE INDEX idx_async_tasks_status ON async_tasks(status);
CREATE INDEX idx_widget_configs_token ON widget_configs(widget_token);

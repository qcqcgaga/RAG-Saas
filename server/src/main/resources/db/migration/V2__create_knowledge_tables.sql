-- V2: 知识库与任务基础表
-- 骨架阶段：仅创建表结构占位，后续迭代中完善字段和约束

CREATE TABLE knowledge_documents (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL REFERENCES tenants(id),
    title           VARCHAR(200) NOT NULL,
    file_name       VARCHAR(200) NOT NULL,
    file_type       VARCHAR(20)  NOT NULL,
    file_size       BIGINT       NOT NULL,
    storage_path    VARCHAR(500) NOT NULL,
    version         INT          NOT NULL DEFAULT 1,
    status          VARCHAR(20)  NOT NULL DEFAULT 'pending',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE knowledge_versions (
    id              BIGSERIAL PRIMARY KEY,
    document_id     BIGINT       NOT NULL REFERENCES knowledge_documents(id),
    tenant_id       BIGINT       NOT NULL REFERENCES tenants(id),
    version         INT          NOT NULL,
    chunk_count     INT          NOT NULL DEFAULT 0,
    milvus_status   VARCHAR(20)  NOT NULL DEFAULT 'pending',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE tasks (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL REFERENCES tenants(id),
    type            VARCHAR(50)  NOT NULL,
    reference_id    BIGINT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'pending',
    progress        INT          NOT NULL DEFAULT 0,
    error_message   TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_knowledge_documents_tenant_id ON knowledge_documents(tenant_id);
CREATE INDEX idx_knowledge_versions_document_id ON knowledge_versions(document_id);
CREATE INDEX idx_tasks_tenant_id ON tasks(tenant_id);
CREATE INDEX idx_tasks_status ON tasks(status);

-- V5: Create V1 tables (API Keys, Chat Usage Logs, Tenant LLM Configs, Eval Sets, Eval Pairs, Eval Results)

-- API Keys
CREATE TABLE api_keys (
    id              BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    tenant_id       BIGINT       NOT NULL REFERENCES tenants(id),
    key_hash        VARCHAR(64)  NOT NULL UNIQUE,
    key_encrypted   TEXT         NOT NULL,
    key_prefix      VARCHAR(10)  NOT NULL,
    name            VARCHAR(50),
    status          SMALLINT     NOT NULL DEFAULT 1,
    last_used_at    TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at      TIMESTAMP
);

-- Chat Usage Logs
CREATE TABLE chat_usage_logs (
    id                  BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    tenant_id           BIGINT       NOT NULL REFERENCES tenants(id),
    api_key_id          BIGINT       REFERENCES api_keys(id),
    auth_type           VARCHAR(10)  NOT NULL,
    model_name          VARCHAR(50),
    prompt_tokens       INT          NOT NULL DEFAULT 0,
    completion_tokens   INT          NOT NULL DEFAULT 0,
    total_tokens        INT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tenant LLM Configs
CREATE TABLE tenant_llm_configs (
    id                  BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    tenant_id           BIGINT       NOT NULL UNIQUE REFERENCES tenants(id),
    api_url             VARCHAR(500) NOT NULL,
    api_key_encrypted   TEXT         NOT NULL,
    model_name          VARCHAR(50)  NOT NULL,
    status              SMALLINT     NOT NULL DEFAULT 1,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Eval Sets
CREATE TABLE eval_sets (
    id                  BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    tenant_id           BIGINT       NOT NULL REFERENCES tenants(id),
    name                VARCHAR(100) NOT NULL,
    description         TEXT,
    pair_count          INT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Eval Pairs
CREATE TABLE eval_pairs (
    id                  BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    eval_set_id         BIGINT       NOT NULL REFERENCES eval_sets(id) ON DELETE CASCADE,
    tenant_id           BIGINT       NOT NULL REFERENCES tenants(id),
    question            VARCHAR(500) NOT NULL,
    expected_document   VARCHAR(255) NOT NULL,
    sort_order          INT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Eval Results
CREATE TABLE eval_results (
    id                  BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    eval_set_id         BIGINT       NOT NULL REFERENCES eval_sets(id),
    tenant_id           BIGINT       NOT NULL REFERENCES tenants(id),
    hit_rate            DECIMAL(5,2) NOT NULL,
    total_pairs         INT          NOT NULL,
    hit_count           INT          NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'COMPLETED',
    detail_json         JSONB,
    duration_ms         INT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

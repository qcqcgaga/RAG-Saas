-- V7: Add V1 indexes

-- API Keys indexes
CREATE UNIQUE INDEX uk_ak_key_hash ON api_keys(key_hash);
CREATE INDEX idx_ak_tenant_id ON api_keys(tenant_id);
CREATE INDEX idx_ak_tenant_status ON api_keys(tenant_id, status);

-- Chat Usage Logs indexes
CREATE INDEX idx_cul_tenant_created ON chat_usage_logs(tenant_id, created_at);
CREATE INDEX idx_cul_tenant_auth_created ON chat_usage_logs(tenant_id, auth_type, created_at);
CREATE INDEX idx_cul_api_key_id ON chat_usage_logs(api_key_id);

-- Tenant LLM Configs unique index (already created by UNIQUE constraint)
-- No additional index needed

-- Eval Sets indexes
CREATE INDEX idx_es_tenant_id ON eval_sets(tenant_id);

-- Eval Pairs indexes
CREATE INDEX idx_ep_eval_set_id ON eval_pairs(eval_set_id);
CREATE INDEX idx_ep_tenant_id ON eval_pairs(tenant_id);

-- Eval Results indexes
CREATE INDEX idx_er_eval_set_id ON eval_results(eval_set_id);
CREATE INDEX idx_er_tenant_created ON eval_results(tenant_id, created_at);

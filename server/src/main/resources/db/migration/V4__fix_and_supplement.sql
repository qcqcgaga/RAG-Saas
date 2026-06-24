-- V4: 修复和补充数据模型
-- 1. 删除 knowledge_documents 中已废弃的 title 列（Entity 不再使用）
-- 2. 将 knowledge_documents.knowledge_id 设为 NOT NULL（Entity 定义 nullable=false）
-- 3. 收紧 knowledge_documents.file_type 长度为 VARCHAR(10)（Entity 定义 length=10）

-- 1. 删除已废弃的 title 列
ALTER TABLE knowledge_documents DROP COLUMN IF EXISTS title;

-- 2. knowledge_id 设为 NOT NULL（先更新可能的 NULL 值为安全默认值）
UPDATE knowledge_documents SET knowledge_id = 1 WHERE knowledge_id IS NULL;
ALTER TABLE knowledge_documents ALTER COLUMN knowledge_id SET NOT NULL;

-- 3. 收紧 file_type 长度
ALTER TABLE knowledge_documents ALTER COLUMN file_type TYPE VARCHAR(10);

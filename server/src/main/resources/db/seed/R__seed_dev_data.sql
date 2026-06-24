-- 开发环境种子数据（仅限 dev profile 使用）
-- 注意：此脚本为 Flyway repeatable migration，每次校验和变化时重新执行
-- 生产环境不加载此目录，不会执行

-- 1. 插入测试租户
INSERT INTO tenants (id, name, slug, status, created_at, updated_at)
VALUES (1, '测试租户', 'test-tenant', 1, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 2. 插入测试用户（密码: test1234，BCrypt 加密）
INSERT INTO users (id, tenant_id, email, password_hash, display_name, role, status, created_at, updated_at)
VALUES (1, 1, 'admin@test.com',
        '$2a$10$dXJ3SW6G7P50lGmMQgel6uVktDQd7hF1Rf0GyXqYqYqYqYqYqYqYq2',
        '测试管理员', 'ADMIN', 1, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 3. 插入知识库
INSERT INTO knowledge_bases (id, tenant_id, name, description, document_count, chunk_count, created_at, updated_at)
VALUES (1, 1, '默认知识库', '测试用知识库', 0, 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 4. 插入 Widget 配置
INSERT INTO widget_configs (id, tenant_id, brand_color, welcome_message, widget_token, enabled, created_at, updated_at)
VALUES (1, 1, '#1890ff', '你好，有什么可以帮你的？',
        'test-widget-token-dev-only', 1, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

# 公共层数据模型

## tenants 表

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | 主键 |
| name | VARCHAR(100) | NOT NULL | 租户名称 |
| slug | VARCHAR(50) | NOT NULL, UNIQUE | 租户标识 |
| status | SMALLINT | NOT NULL | 状态(0禁用/1启用) |
| created_at | TIMESTAMPTZ | NOT NULL | 创建时间 |
| updated_at | TIMESTAMPTZ | NOT NULL | 更新时间 |

## users 表

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | 主键 |
| tenant_id | BIGINT | NOT NULL | 所属租户 |
| email | VARCHAR(200) | NOT NULL | 邮箱(租户内唯一) |
| password_hash | VARCHAR | NOT NULL | BCrypt 哈希 |
| display_name | VARCHAR(100) | | 显示名 |
| role | VARCHAR(20) | NOT NULL | 角色(ADMIN/MEMBER/READONLY) |
| status | SMALLINT | NOT NULL | 状态 |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**唯一约束**: `(tenant_id, email)`

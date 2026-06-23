# 详细设计文档

> 项目：{项目名称}
> 日期：{YYYY-MM-DD}

## 1. 模块概述

{描述模块的整体结构和各子模块的职责}

## 2. 类图

```mermaid
classDiagram
    class UserService {
        +findById(id: int) User
        +create(dto: CreateUserDTO) User
        +update(id: int, dto: UpdateUserDTO) User
        +delete(id: int) void
        +list(query: QueryDTO) Page~User~
    }
    class UserRepository {
        +findOne(id: int) User
        +save(user: User) User
        +delete(id: int) void
        +findPage(query: QueryDTO) Page~User~
    }
    class User {
        +int id
        +string username
        +string email
        +datetime createdAt
    }
    UserService --> UserRepository
    UserService --> User
```

## 3. 时序图

### 3.1 {核心流程名称}

```mermaid
sequenceDiagram
    participant C as Client
    participant Ctrl as Controller
    participant Svc as Service
    participant Repo as Repository
    participant DB as Database

    C->>Ctrl: POST /api/v1/users
    Ctrl->>Ctrl: 参数校验
    Ctrl->>Svc: createUser(dto)
    Svc->>Svc: 业务校验(用户名唯一性等)
    Svc->>Repo: save(user)
    Repo->>DB: INSERT INTO users
    DB-->>Repo: 返回结果
    Repo-->>Svc: 返回User实体
    Svc-->>Ctrl: 返回UserDTO
    Ctrl-->>C: 201 Created
```

### 3.2 {异常流程名称}

```mermaid
sequenceDiagram
    participant C as Client
    participant Ctrl as Controller
    participant Svc as Service

    C->>Ctrl: POST /api/v1/users
    Ctrl->>Ctrl: 参数校验
    alt 参数校验失败
        Ctrl-->>C: 400 INVALID_PARAMETER
    else 用户名已存在
        Ctrl->>Svc: createUser(dto)
        Svc->>Svc: 校验用户名唯一性
        Svc-->>Ctrl: 抛出 UserAlreadyExistsException
        Ctrl-->>C: 409 USER_ALREADY_EXISTS
    end
```

## 4. 状态机

```mermaid
stateDiagram-v2
    [*] --> 待提交
    待提交 --> 审核中: 提交
    审核中 --> 已通过: 审核通过
    审核中 --> 已驳回: 审核驳回
    已驳回 --> 待提交: 修改后重新提交
    已通过 --> [*]
```

## 5. 关键算法

### 5.1 {算法名称}

**输入**：{输入描述}

**输出**：{输出描述}

**算法步骤**：

1. {步骤1}
2. {步骤2}
3. {步骤3}

**复杂度**：{时间复杂度} / {空间复杂度}

## 6. 错误处理策略

| 异常场景 | 错误码 | 处理方式 |
|----------|--------|---------|
| {场景} | {错误码} | {处理方式} |
| {场景} | {错误码} | {处理方式} |

## 7. 变更记录

| 日期 | 变更内容 |
|------|---------|
| {日期} | 初始版本 |

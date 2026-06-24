# Maven 本地仓库迁移与项目级配置

## 1. Maven 本地仓库路径配置的三种方式

### 1.1 全局 conf/settings.xml（最低优先级声明，最高覆盖范围）

位置：`{MAVEN_HOME}/conf/settings.xml`

```xml
<settings>
  <localRepository>D:/develop_lib/maven-repository</localRepository>
</settings>
```

- 影响范围：本机所有 Maven 项目
- 优先级：最低（可被下述方式覆盖）

### 1.2 用户级 ~/.m2/settings.xml

位置：`${user.home}/.m2/settings.xml`

```xml
<settings>
  <localRepository>D:/develop_lib/maven-repository</localRepository>
</settings>
```

- 影响范围：当前用户的所有 Maven 项目
- 优先级：高于全局 conf/settings.xml

### 1.3 项目级 .mvn/maven.config（最高优先级，推荐）

位置：`{project-root}/.mvn/maven.config`

```
-Dmaven.repo.local=D:/develop_lib/maven-repository-RAG-Saas
```

- 影响范围：仅当前项目
- 优先级：最高，覆盖所有 settings.xml 中的 `<localRepository>`
- **推荐理由**：项目隔离，不污染全局，可提交到 Git 共享给团队

> ⚠️ **优先级顺序**：`-Dmaven.repo.local`（命令行/maven.config）> `~/.m2/settings.xml` > `conf/settings.xml` > 默认 `~/.m2/repository`

---

## 2. 项目级 .mvn/ 目录配置详解

### 2.1 目录结构

```
server/
├── .mvn/
│   ├── maven.config        # Maven 启动参数（JVM 参数、命令行选项）
│   └── settings.xml        # 项目级 Maven 设置（镜像、仓库、Profile）
├── pom.xml
└── src/
```

### 2.2 maven.config 语法规则

**关键规则**：每行一个参数，`-s`/`--settings` 与其值必须分两行写。

```
-Dmaven.repo.local=D:/develop_lib/maven-repository-RAG-Saas
-s
D:/agent_project/RAG-Saas/server/.mvn/settings.xml
```

#### ❌ 常见错误写法

| 错误写法 | 问题 |
|----------|------|
| `-Dmaven.repo.local=xxx -s path`（单行） | Maven 将整行视为 `-Dmaven.repo.local` 的值，报 `Could not create local repository` |
| `-s .mvn/settings.xml`（相对路径） | Maven 解析相对路径时可能拼接错误，报 `Illegal char` 或 `file does not exist` |
| `-s D:/path settings.xml`（路径含空格未引号） | 路径被截断 |

#### ✅ 正确写法

```
-Dmaven.repo.local=D:/develop_lib/maven-repository-RAG-Saas
-s
D:/agent_project/RAG-Saas/server/.mvn/settings.xml
```

> **经验**：`.mvn/maven.config` 不支持相对路径引用 `-s`，必须使用绝对路径。如果项目目录会迁移，需同步更新此路径。

### 2.3 项目级 settings.xml

Maven **不会自动加载** `.mvn/settings.xml`，必须在 `maven.config` 中通过 `-s` 显式指定。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0
          https://maven.apache.org/xsd/settings-1.2.0.xsd">

  <localRepository>D:/develop_lib/maven-repository-RAG-Saas</localRepository>

  <mirrors>
    <mirror>
      <id>aliyunmaven</id>
      <mirrorOf>*</mirrorOf>
      <name>阿里云公共仓库</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>

</settings>
```

---

## 3. Maven 镜像配置（mirrorOf 详解）

### 3.1 mirrorOf 取值对比

| mirrorOf 值 | 代理范围 | 适用场景 |
|-------------|----------|----------|
| `central` | 仅 Maven Central 仓库 | 基础配置，第三方仓库仍走原源 |
| `*` | 所有仓库（包括 Central、插件仓库、第三方仓库） | **推荐**：国内开发全量加速 |
| `external:*` | 所有非 localhost/非 file 的仓库 | 排除本地仓库，适合有内网私服的场景 |
| `*,!my-private-repo` | 所有仓库但排除指定 ID | 有内网私服时排除私服地址 |

### 3.2 为什么推荐 mirrorOf=*

Spring Boot 项目通常依赖多个远程仓库：

| 仓库 | 默认 URL | mirrorOf=central 时 | mirrorOf=* 时 |
|------|----------|---------------------|---------------|
| Maven Central | `repo.maven.apache.org` | ✅ 走阿里云 | ✅ 走阿里云 |
| Spring Milestones | `repo.spring.io/milestone` | ❌ 走国外源 | ✅ 走阿里云 |
| Spring Snapshots | `repo.spring.io/snapshot` | ❌ 走国外源 | ✅ 走阿里云 |
| 第三方 pom 中声明的仓库 | 各异 | ❌ 走原源 | ✅ 走阿里云 |

阿里云公共仓库 `https://maven.aliyun.com/repository/public` 已聚合了 Central、Spring 仓库、JCenter 等，`mirrorOf=*` 可确保所有依赖都走国内镜像。

### 3.3 多镜像配置注意事项

- Maven 同一 `mirrorOf` 范围内**只生效第一个**匹配的 mirror，不会叠加
- `maven-default-http-blocker`（Maven 3.8.1+ 内置）会拦截所有 HTTP 仓库，必须保留
- 阿里云镜像应声明在 http-blocker **之后**，确保 blocker 优先拦截不安全的 HTTP 请求

```xml
<mirrors>
  <!-- HTTP 拦截器（Maven 内置，必须保留） -->
  <mirror>
    <id>maven-default-http-blocker</id>
    <mirrorOf>external:http:*</mirrorOf>
    <name>Pseudo repository to mirror external repositories initially using HTTP.</name>
    <url>http://0.0.0.0/</url>
    <blocked>true</blocked>
  </mirror>
  <!-- 阿里云镜像 -->
  <mirror>
    <id>aliyunmaven</id>
    <mirrorOf>*</mirrorOf>
    <name>阿里云公共仓库</name>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

---

## 4. 验证命令速查

| 目的 | 命令 |
|------|------|
| 查看当前生效的本地仓库路径 | `mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout` |
| 查看完整生效配置（含镜像、Profile） | `mvn help:effective-settings` |
| 验证依赖能否正常下载 | `mvn dependency:resolve` |
| 查看依赖树 | `mvn dependency:tree` |
| 查看特定依赖的冲突路径 | `mvn dependency:tree -Dverbose -Dincludes={groupId}:{artifactId}` |

---

## 5. 本项目最终配置方案

### 设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 仓库路径配置方式 | `.mvn/maven.config` 的 `-Dmaven.repo.local` | 优先级最高，确保一定生效 |
| 镜像配置方式 | 项目级 `.mvn/settings.xml` + `-s` 引用 | 跟项目走，换机器只需改 `-s` 路径 |
| mirrorOf 范围 | `*` | 全量代理，避免第三方仓库走国外源 |
| 本地仓库位置 | `D:/develop_lib/maven-repository-RAG-Saas` | 项目独立仓库，不污染全局 |

### 文件清单

| 文件 | 作用 |
|------|------|
| `server/.mvn/maven.config` | 指定本地仓库路径 + 引用项目级 settings.xml |
| `server/.mvn/settings.xml` | 阿里云镜像配置 + 本地仓库兜底声明 |

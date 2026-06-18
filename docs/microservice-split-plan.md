# 微服务拆分现状与执行计划

> 更新日期：2026-06-18
> 当前开发分支：`cloud`
> 状态：微服务拆分已完成（所有阶段），待后续物理拆库和深度测试

## 1. 当前结论

项目已完成渐进式微服务拆分，原单体应用已完全拆分为 7 个独立微服务 + Gateway：

```text
前端
  |
  v
gateway-service :8081
  |-- /api/ai/**                            -> ai-service (order -20)
  |-- /api/image/**、/api/file/**、/images/**   -> oss-service (order -10)
  |-- /api/auth/**、/api/user/**               -> user-service (order -5)
  |-- /api/admin/user/**、/api/admin/email/**  -> user-service (order -5)
  |-- /api/announcement/**、/api/admin/announcement/** -> announcement-service (order -4)
  |-- /api/notification/**                    -> notification-service (order -3)
  `-- /api/**                                  -> forum-service (default)
```

所有服务均通过 Nacos 注册发现，Gateway 统一鉴权、CORS 和身份透传。原单体已完全删除。

技术选型：

```text
Nacos                 服务注册与发现
Spring Cloud Gateway  统一入口、路由、CORS、鉴权
OpenFeign             跨服务同步调用（AI→Forum、OSS→User、Forum→User、Announcement→User）
RabbitMQ              异步事件（论坛评论 → 通知服务，notification.exchange）
Redis                 JWT 黑名单、封禁状态、论坛缓存和限流数据
Elasticsearch         论坛全文搜索
```

## 2. 工作进度

| 阶段 | 状态 | 结果 |
| --- | --- | --- |
| 业务准备 | 已完成 | 校园公告、帖子草稿及对应数据库数据已加入 |
| Gateway 接入 | 已完成 | 前端统一访问 `8081`，所有路由已配置 |
| Nacos 接入 | 已完成 | 全部 7 服务可通过服务名注册和发现 |
| CORS 收口 | 已完成 | Gateway 统一响应 CORS |
| Gateway 鉴权 | 已完成 | 校验 JWT、黑名单、封禁状态和管理员权限 |
| 用户上下文透传 | 已完成 | X-User-Id、X-Username、X-User-Roles |
| OSS 拆分 | 已完成 | 图片/头像/文本/读取独立路由，头像通过 Feign 更新 |
| 公共模块 | 已完成 | RestBean 等响应模型和网关常量 |
| AI 拆分 | 已完成 | 会话、聊天、SSE、工具调用 |
| 用户服务拆分 | 已完成 | 全部 /api/auth/** + /api/user/** + /api/admin/user/** |
| OSS 头像解耦 | 已完成 | OSS 不再直写 db_account |
| 通知服务拆分 | 已完成 | RabbitMQ 异步通知，端口 8087 |
| 公告服务拆分 | 已完成 | announcement-service，端口 8086，通过 Feign 获取用户名 |
| 论坛服务拆分 | 已完成 | forum-service，端口 8088，用户数据全部通过 Feign 获取 |
| 单体下线 | 已完成 | 原 my-project-backend 已完全删除 |

## 3. 当前模块

### 3.1 gateway-service

当前职责：

- 根据 Nacos 服务名进行负载均衡路由
- 统一处理浏览器 CORS
- 校验 JWT 签名和过期时间
- 查询 Redis JWT 黑名单
- 查询 Redis 用户封禁状态
- 对 `/api/admin/**` 校验 `ROLE_admin`
- 删除客户端伪造的身份请求头，再注入可信用户上下文

公开路径：

```text
/api/auth/**
/images/**
/actuator/**
/error
OPTIONS 请求
```

身份请求头：

```text
X-User-Id     用户数据库 ID
X-Username    用户名
X-User-Roles  逗号分隔的 Spring Security 权限，例如 ROLE_user,ROLE_admin
```

当前路由（按 order 优先级排列）：

```text
order -20  /api/ai/**                                      -> lb://ai-service
order -10  /api/image/**、/api/file/**、/images/**             -> lb://oss-service
order -5   /api/auth/**、/api/user/**、/api/admin/user/**     -> lb://user-service
order -5   /api/admin/email/**                              -> lb://user-service
order -4   /api/announcement/**、/api/admin/announcement/**    -> lb://announcement-service
order -3   /api/notification/**                             -> lb://notification-service
default    /api/**                                          -> lb://forum-service
```

### 3.2 forum-service（原 forum-monolith-service）

端口：`8088`，注册名：`forum-service`。

已迁移接口：

```text
/api/forum/**           论坛帖子、评论、互动、草稿、搜索、天气
/api/admin/forum/**      论坛后台管理（帖子、类型、违禁词、ES 同步）
/internal/forum/**      内部 API（供 AI 搜索和最近帖子查询）
```

已删除（迁移到其他服务）：
- 全部用户/认证 Controller、Service、Mapper、Entity、VO（→ user-service）
- 全部通知 Controller、Service、Mapper、Entity、VO（→ notification-service）
- 全部公告 Controller、Service、Mapper、Entity、VO（→ announcement-service）
- Account、AccountDetails、AccountPrivacy 实体和 Mapper（→ 全部改为 OpenFeign）
- JwtUtils、JwtAuthenticationFilter、Email Listeners、spring-boot-starter-mail

关键架构变更：

- `TopicServiceImpl` 已完全消除对用户表的直接数据库访问，改为通过 `UserInternalClient`（OpenFeign）批量查询用户数据
- `createComment()` 的用户名获取改为 `resolveUsername()` → Feign 调用
- `fillUserDetailsByPrivacy()` 改为 `fillUserDetailsFromMap()` → 从批量 Feign 结果填充
- `resolveToPreview()` 接受预取的批量用户数据 Map
- `listTopicByPage()`、`listAllTopicByPage()`、`comments()`、`getTopic()` 均先批量收集 UID，一次 Feign 调用获取所有用户数据
- 通知发送通过 RabbitMQ（`notification.exchange` → `notification.event`）
- CORS 默认关闭，由 Gateway 统一处理

依赖：

```text
MySQL (db_topic, db_topic_comment, db_topic_draft, db_topic_type, db_topic_interact_*)
Redis (缓存 + 限流 + 互动缓冲)
Elasticsearch (帖子全文搜索)
RabbitMQ (通知消息生产者)
OpenFeign → user-service (用户数据查询、禁言状态检查)
外部 API → QWeather (天气查询)
```

### 3.3 oss-service

端口 `8082`，注册名 `oss-service`。详见之前文档。

### 3.4 common-core

共享响应协议、请求头常量、通用常量。详见之前文档。

### 3.5 ai-service

端口 `8083`，注册名 `ai-service`。Feign 客户端 `ForumInternalClient` 已更新指向 `forum-service`。

### 3.6 user-service

端口 `8084`，注册名 `user-service`。详见 2026-06-18 拆分记录。

内部 API（供其他服务调用）：

```text
POST /internal/user/batch       批量查询用户数据（forum、announcement 使用）
GET  /internal/user/{id}        单个用户信息（forum 创建评论时使用）
GET  /internal/user/{id}/status 禁言/封禁状态（forum 发帖前检查）
PUT  /internal/user/{id}/avatar 更新头像（oss 调用，返回旧头像路径）
```

### 3.7 notification-service

端口 `8087`，注册名 `notification-service`。详见 2026-06-18 拆分记录。

### 3.8 announcement-service（新增 2026-06-18）

端口 `8086`，注册名 `announcement-service`。

已迁移接口：

```text
GET  /api/announcement/latest       最新公告
GET  /api/announcement/list         分页公告列表
GET  /api/announcement/detail       公告详情
GET  /api/admin/announcement/list   后台公告列表（含用户名、搜索、发布状态过滤）
POST /api/admin/announcement/create 创建公告
POST /api/admin/announcement/update 更新公告
POST /api/admin/announcement/publish 发布/取消发布
POST /api/admin/announcement/top    置顶/取消置顶
GET  /api/admin/announcement/delete 删除公告
```

模块组成（17 Java 文件）：

| 类别 | 文件数 | 说明 |
|------|--------|------|
| Controller | 4 | AnnouncementController + AnnouncementAdminController + ErrorPage + Validation |
| Service | 2 | AnnouncementService + AnnouncementServiceImpl |
| Mapper | 1 | AnnouncementMapper（MyBatis-Plus，表 db_announcement） |
| Entity (DTO) | 1 | Announcement |
| VO | 7 | 4 Request + 3 Response |
| Filter | 1 | GatewayIdentityFilter |
| Client | 1 | UserInternalClient（OpenFeign → user-service） |
| Config | 2 | SecurityConfiguration + WebConfiguration |

服务特点：

- 无 Redis、RabbitMQ、Elasticsearch 依赖，是依赖最少的服务
- 仅通过 OpenFeign 调用 user-service 的 `/internal/user/batch` 解析公告作者用户名
- Feign 调用失败时优雅降级（username 为 null）
- 角色校验：admin 端点需 ROLE_admin，公开端点需 ROLE_user 或 ROLE_admin

集成测试结果（2026-06-18）：

- 启动时间 2.65s，端口 8086
- Nacos 注册成功
- 无身份头 → 401 ✅
- 普通用户访问 admin → 403 ✅
- 管理员访问 admin 列表 → 200 ✅
- 用户名解析（user-service 不可用时优雅降级）✅

## 4. 目标服务与接口归属（最终状态）

```text
gateway-service :8081
  |-- user-service :8084           /api/auth/**、/api/user/**、/api/admin/user/**、/api/admin/email/**
  |-- notification-service :8087   /api/notification/**
  |-- announcement-service :8086   /api/announcement/**、/api/admin/announcement/**
  |-- forum-service :8088          /api/forum/**、/api/admin/forum/**、/internal/forum/**
  |-- oss-service :8082            /api/image/**、/api/file/**、/images/**
  `-- ai-service :8083             /api/ai/**
```

## 5. 数据库策略

当前所有服务仍共享 `study_main` MySQL 数据库，但代码层已实现严格的表所有权隔离：

```text
user-service          db_account、db_account_details、db_account_privacy、db_email_record
forum-service         db_topic、db_topic_comment、db_topic_draft、db_topic_type、db_topic_interact_*
announcement-service  db_announcement
notification-service  db_notification
oss-service           （MinIO 对象存储，无 MySQL 表）
ai-service            ai_conversation、ai_conversation_message
```

任何服务不得直接通过 Mapper 访问其他服务的表。跨服务数据获取必须通过 OpenFeign 或 RabbitMQ 事件。forum-service 已完成此项改造（不再直接访问 db_account 等表）。

## 6. 服务间调用关系

```text
OpenFeign（同步）：
  ai-service      → forum-service     (/internal/forum/search, /internal/forum/recent)
  oss-service     → user-service      (/internal/user/{id}/avatar)
  forum-service   → user-service      (/internal/user/batch, /internal/user/{id}, /internal/user/{id}/status)
  announcement-service → user-service (/internal/user/batch)

RabbitMQ（异步）：
  forum-service   → notification-service  (notification.exchange → notification.event)
```

所有内部调用均需携带 `X-Internal-Token` 请求头进行服务间认证。

## 7. 当前风险与技术债

- 内部服务若直接暴露端口，攻击者仍可能尝试绕过 Gateway。生产环境需通过网络策略限制。
- Gateway、user-service 对 Redis key 和 JWT claim 格式存在协议耦合，需契约测试。
- 当前共享 MySQL 数据库使物理边界仍弱。后续需物理拆库。
- 删除类接口仍有部分使用 GET，后续应调整为 DELETE。
- 尚未形成统一的服务日志、trace ID、指标和告警。
- OpenFeign 调用缺少统一超时、重试和降级策略。
- Feign 调用在目标服务不可用时会静默降级（返回空数据），需评估是否符合业务预期。
- forum-service 的 TopicPreviewVO 缓存包含从 user-service 获取的用户数据，用户信息变更后缓存不会主动失效。
- 天气 API 调用和 ES 同步失败时缺少告警。
- 默认配置中存在开发用密钥和密码，不能直接用于生产。

## 8. 后续建议

1. **物理拆库**：为每个服务创建独立数据库，使用 CDC 或双写过渡。
2. **契约测试**：覆盖 Feign 接口和 RabbitMQ 消息格式。
3. **集成测试**：为每个服务添加端到端 API 测试。
4. **服务治理**：统一超时配置、限流策略、熔断降级（Sentinel/Resilience4j）。
5. **可观测性**：集成 Micrometer Tracing、集中日志、Grafana 仪表板。
6. **安全加固**：密钥管理（Vault/K8s Secrets）、网络策略、HTTPS。
7. **CI/CD**：每个服务独立构建、测试和部署流水线。
8. **前端适配**：前端已统一通过 Gateway 访问，无需改动。

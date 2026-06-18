# 微服务拆分现状与执行计划

> 更新日期：2026-06-18
> 当前开发分支：`cloud`

## 1. 当前结论

项目采用渐进式拆分，不一次性重写单体：

```text
前端
  |
  v
gateway-service :8081
  |-- /api/image/**、/api/file/**、/images/** -> oss-service
  |-- /api/ai/**                            -> ai-service
  `-- /api/**                                -> forum-monolith-service
```

当前已经完成 Gateway、Nacos 服务发现、网关 JWT 鉴权、OSS 服务拆分和 AI 服务
拆分，并完成 `common-core` 公共模块。用户、论坛、公告和通知业务仍由单体服务
提供。

技术选型保持不变：

```text
Nacos                 服务注册与发现
Spring Cloud Gateway  统一入口、路由、CORS、鉴权
OpenFeign             AI 到单体、AI 到 OSS 的同步调用
Redis                 JWT 黑名单、封禁状态、缓存和限流数据
```

注意：当前只接入了 Nacos 服务注册与发现，尚未使用 Nacos 配置中心。

## 2. 工作进度

| 阶段 | 状态 | 结果 |
| --- | --- | --- |
| 业务准备 | 已完成 | 校园公告、帖子草稿及对应数据库数据已加入单体 |
| Gateway 接入 | 已完成 | 前端统一访问 `8081`，未拆接口转发给单体 |
| Nacos 接入 | 已完成 | Gateway、单体、OSS、AI 可通过服务名注册和发现 |
| CORS 收口 | 已完成 | Gateway 统一响应 CORS，单体默认关闭 CORS |
| Gateway 鉴权 | 已完成 | 校验 JWT、黑名单、封禁状态和管理员权限 |
| 用户上下文透传 | 已完成 | 使用 `X-User-Id`、`X-Username`、`X-User-Roles` |
| OSS 拆分 | 已完成并联调 | 图片上传、头像上传、文本解析、图片读取已独立路由 |
| 公共模块 | 已完成第一版 | `RestBean` 等响应模型和网关常量迁入 `common-core` |
| AI 拆分 | 已完成并联调 | 会话、聊天、SSE、工具调用和 `/api/ai/**` 路由已独立 |
| 单体重复代码清理 | 已完成 | 删除旧 AI/OSS Controller、Service、Mapper、Entity 和依赖 |
| 用户服务拆分 | 未开始 | 登录、账号、权限仍在单体 |
| 论坛等业务拆分 | 未开始 | 论坛、公告、通知仍在单体 |

历史工作中还修复了帖子详情页大图溢出问题。该修改不改变微服务边界，但应在
后续前端回归测试中保留。

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

当前实际路由：

```text
/api/image/**  -> lb://oss-service
/api/file/**   -> lb://oss-service
/images/**     -> lb://oss-service
/api/ai/**     -> lb://ai-service
/api/**        -> lb://forum-monolith-service
```

AI 和 OSS 路由优先级高于单体兜底路由。单体中的旧实现已经删除，不再承担备用
AI/OSS 处理。

当前未完成：

- 网关级限流
- 统一访问日志和链路追踪
- 动态路由配置
- 密钥集中管理和轮换

### 3.2 forum-monolith-service

单体目前继续承载：

```text
/api/auth/**
/api/user/**
/api/admin/user/**
/api/admin/email/**
/api/forum/**
/api/admin/forum/**
/api/announcement/**
/api/admin/announcement/**
/api/notification/**
```

单体已加入 `GatewayIdentityFilter`，可将 Gateway 注入的请求头恢复为原有
`SecurityContext` 和用户 ID 请求属性，避免立即重写现有 Controller。

单体仍提供仅供 AI 使用的 `/internal/forum/**` 查询接口。该接口要求一致的
`X-Internal-Token`，用于在论坛正式拆分前隔离 AI 对论坛数据的读取。

单体 CORS 默认关闭，防止 Gateway 和单体同时写入
`Access-Control-Allow-Origin`。仅在绕过 Gateway 本地调试时通过
`BACKEND_CORS_ENABLED=true` 临时开启。

### 3.3 oss-service

已迁移接口：

```text
POST /api/image/cache   上传帖子或富文本图片
POST /api/image/avatar  上传用户头像
POST /api/file/text     上传文本文件并提取内容
GET  /images/**         读取公开图片
```

服务特点：

- 注册名为 `oss-service`，默认端口 `8082`
- 不重复解析 JWT，依赖 Gateway 注入的用户上下文和内部服务凭证
- 使用 MinIO 保存对象
- 当前仍直接访问 MySQL 的账号和图片记录表
- 当前仍使用 Redis 处理原有上传限制逻辑
- `/api/**` 和 `/internal/**` 均校验 `X-Internal-Token`，不能只伪造用户头访问

这是过渡状态。OSS 服务可以拥有图片元数据，但不应长期直接修改用户服务拥有的
账号数据。头像更新后续应改为调用 `user-service`，或由用户服务编排头像上传和
资料更新。

### 3.4 common-core

当前包含：

```text
RestBean
PageRestBean
BaseData
GatewayHeaders
RedisKeys
RequestAttributes
```

使用边界：

- 可以放稳定的响应协议、请求头名称、通用常量和极少量基础类型
- 不放论坛、用户、AI、OSS 等业务 Entity、Mapper、Service
- 不把 `common-core` 演变成所有服务都依赖的大型共享业务包

业务 DTO 优先归属各自服务。确需跨服务共享的接口契约，后续单独建立轻量
`*-api` 模块，而不是共享数据库实体。

### 3.5 ai-service

AI 服务已经具备独立启动、Nacos 注册、MySQL 持久化、OpenFeign 调用和 Gateway
身份校验能力，默认端口为 `8083`。

目标接口：

```text
GET     /api/ai/conversations
POST    /api/ai/conversations
DELETE  /api/ai/conversations/{id}
GET     /api/ai/conversations/{id}/messages
POST    /api/ai/chat
POST    /api/ai/chat/{conversationId}
```

聊天接口使用 SSE，已验证 Gateway 不缓冲响应、连接不中断且正常响应能正确结束。
断连取消和上游异常场景仍需补充自动化测试。

已实现的边界：

- AI 服务只直接访问 `ai_conversation` 和 `ai_conversation_message`
- 论坛工具通过单体 `/internal/forum/**` 接口查询，不直连 Elasticsearch
- 附件和图片通过 OSS 内部接口读取，不下载任意 URL
- Gateway 覆盖客户端身份头并注入 `X-Internal-Token`
- 同一会话只允许一个生成任务，历史上下文限制为 20 条和 50,000 字符

2026-06-18 已通过完整联调：

- Gateway 登录、单体兜底路由、AI 和 OSS 服务发现正常
- 文本上传、对象内容回读、公开图片读取和跨用户越权拒绝正常
- DeepSeek SSE 普通聊天及附件内容读取正常
- OpenFeign 论坛工具调用正常
- Tavily 联网搜索正常
- SiliconFlow 图片识别和图片生成正常
- 外部 URL、错误内部凭证和伪造用户请求头均被拒绝

## 4. 目标服务与接口归属

### user-service

```text
/api/auth/**
/api/user/**
/api/admin/user/**
/api/admin/email/**
```

负责登录签发 JWT、注册、验证码、密码、用户资料、隐私设置、封禁和角色管理。
Gateway 只验证 token，不承载这些用户业务。

### forum-service

```text
/api/forum/**
/api/admin/forum/**
```

包括帖子、帖子类型、草稿、评论、互动、收藏、搜索和论坛后台管理。

`/api/forum/weather` 不属于论坛核心业务，拆分论坛时应迁移到公共信息服务，或先
保留在单体。

### announcement-service

```text
/api/announcement/**
/api/admin/announcement/**
```

负责校园公告列表、详情、发布、置顶和后台管理。

### notification-service

```text
/api/notification/**
```

负责站内通知。后续由论坛评论、互动、公告发布和系统管理事件触发通知。

### oss-service

```text
/api/image/**
/api/file/**
/images/**
```

负责对象上传、读取、校验和图片元数据。

### ai-service

```text
/api/ai/**
```

负责 AI 会话、消息持久化、模型调用和 SSE 流式响应。

## 5. 数据库策略

当前阶段各模块仍可连接同一个 `study_main` 数据库，以降低首次拆分风险，但需要
逐步形成表的服务所有权：

```text
user-service          用户、资料、隐私、角色、邮件验证
forum-service         帖子、类型、草稿、评论、互动、收藏
announcement-service  校园公告
notification-service  站内通知
oss-service           图片和文件元数据
ai-service            AI 会话和消息
```

不建议建立统一的 `database-service` 代理所有数据库读写。这样会把数据库访问
集中成新的单点和高耦合 RPC 层。正确方向是每个服务只读写自己拥有的表，跨领域
数据通过 API、OpenFeign 或异步事件获取。

在物理拆库之前，先执行“同库分表权”：代码层禁止跨服务直接使用其他领域的
Mapper。OSS 当前更新账号头像是已知的临时例外。

## 6. 修订后的执行顺序

### 阶段一：网关与基础设施

状态：已完成第一版。

- Nacos 注册发现
- Gateway 路由
- Gateway CORS
- JWT、黑名单、封禁和管理员权限校验
- 用户上下文透传
- 前端请求入口切换到 Gateway

进入生产环境前仍需：

- 通过网络策略禁止客户端直连内部服务端口
- 将 JWT 密钥和数据库密码移出默认配置
- 按生产前端域名配置 CORS；同域部署时由 Nginx 统一暴露前后端
- 增加 Gateway 请求日志、指标和基础限流

### 阶段二：边界清晰服务

状态：OSS 和 AI 已完成拆分、真实联调及单体重复代码清理。

AI 已完成：

1. 独立工程、Nacos、数据库、模型和 `common-core` 依赖。
2. 会话、消息、聊天、联网搜索、论坛工具和图片工具迁移。
3. Gateway 身份与内部服务凭证校验。
4. `/api/ai/**` 高优先级路由及 SSE 专用超时配置。
5. 论坛内部 API 和 OSS 安全对象读取接口。
6. 前端改用 `fileKey`、`imageKeys`，并兼容旧请求字段。

AI 后续增强：

1. 增加模型调用耗时、失败率、token 用量和工具调用指标。
2. 增加断连取消、五分钟超时和上游服务故障的自动化测试。
3. 为 OpenFeign 调用补充统一的错误映射和有限重试策略。

OSS 收尾步骤：

1. 增加上传、头像、文本解析和图片读取的集成测试。
2. 明确图片元数据表所有权。
3. 消除对账号表的直接写入。
4. 限制 `8082` 只允许 Gateway 或内网访问。
5. 增加对象类型、大小、配额和恶意文件检测策略。

### 阶段三：用户与认证服务

状态：未开始。

主要工作：

- 迁移 `/api/auth/**`、`/api/user/**`、`/api/admin/user/**` 和
  `/api/admin/email/**`
- 保持 JWT claim、Redis 黑名单 key 和 Gateway 校验逻辑兼容
- 提供内部用户摘要查询接口
- 将 OSS 的头像资料更新改为调用用户服务
- 决定 Gateway 只做本地 JWT 校验，还是对高风险操作回查用户服务

用户服务稳定后，其他服务不再直接查询账号表。

### 阶段四：论坛、公告和通知

状态：未开始。

建议顺序：

```text
notification-service
announcement-service
forum-service
```

论坛最后拆分，因为它依赖用户、通知、OSS、Elasticsearch 和 Redis，调用关系和
数据迁移风险最高。

同步查询可使用 OpenFeign，例如论坛查询用户摘要；评论、互动、公告发布等通知
更适合通过 RabbitMQ 事件异步处理，避免主业务被通知服务故障阻塞。

## 7. 下一阶段验收标准

下一阶段进入 `user-service`：

- 迁移 `/api/auth/**`、`/api/user/**`、`/api/admin/user/**` 和
  `/api/admin/email/**`
- 登录签发的 JWT 与 Gateway 当前 claim、签名和过期校验完全兼容
- Redis 黑名单、封禁状态和角色校验保持兼容
- OSS 不再直接写账号表，头像更新改由用户服务负责或编排
- 单体删除对应用户 Controller、Service、Mapper 后仍可通过 Gateway 完成登录、
  用户资料和后台用户管理回归
- 为其他服务提供最小化的内部用户摘要接口，不共享用户数据库实体

## 8. 当前风险与技术债

- 内部服务若直接暴露端口，攻击者仍可能尝试绕过 Gateway。
- 应用层已增加 `X-Internal-Token` 校验，但生产环境仍需通过网络策略关闭服务
  端口的公网访问。
- Gateway、单体和用户相关模块对 Redis key、JWT claim 存在协议耦合，需要契约测试。
- 当前共享数据库使服务边界仍可被 Mapper 跨表访问，需要代码审查约束。
- 默认配置中存在开发用密钥和密码，不能直接用于生产环境。
- 删除类接口仍有部分使用 GET，后续接口治理应调整为 DELETE。
- 尚未形成统一的服务日志、trace ID、指标、告警和超时策略。
- OpenFeign 已落地，仍需统一连接超时、读取超时、错误映射和有限重试策略。

## 9. 最终目标

```text
gateway-service
  |-- user-service
  |-- forum-service
  |-- announcement-service
  |-- notification-service
  |-- oss-service
  `-- ai-service
```

Gateway 是唯一公网后端入口；各服务拥有自己的业务和数据表；同步调用使用明确的
内部 API，业务事件优先异步传递。拆分过程始终保留单体兜底路由，按服务逐个迁移、
验证和下线旧实现。

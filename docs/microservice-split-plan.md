# 微服务拆分方案与接口归属

## 1. 总体方案

当前项目建议拆分为以下模块：

```text
gateway-service
user-service
forum-service
announcement-service
oss-service
ai-service
notification-service
```

基础设施选型：

```text
Nacos                 服务注册、服务发现、配置中心
Spring Cloud Gateway  统一入口、路由、鉴权、限流、跨域
OpenFeign             服务间 HTTP 调用
```

推荐演进方式：

```text
先网关化，再逐步拆分业务服务
```

也就是先让当前单体后端注册到 Nacos，通过 Gateway 转发请求；确认网关、认证、路由链路稳定后，再逐步拆出 AI、OSS、用户、论坛、公告、通知等服务。

## 2. gateway-service

`gateway-service` 是统一入口，不承载具体业务数据。

主要职责：

- 路由转发
- JWT 校验
- 用户身份解析
- 管理端权限拦截
- 跨域处理
- 限流
- 请求日志
- 透传用户上下文，例如 `X-User-Id`、`X-User-Role`

对外统一入口：

```text
/api/**
/images/**
```

路由规则：

```text
/api/auth/**                -> user-service
/api/user/**                -> user-service
/api/admin/user/**          -> user-service

/api/forum/**               -> forum-service
/api/admin/forum/**         -> forum-service

/api/announcement/**        -> announcement-service
/api/admin/announcement/**  -> announcement-service

/api/image/**               -> oss-service
/api/file/**                -> oss-service
/images/**                  -> oss-service

/api/ai/**                  -> ai-service

/api/notification/**        -> notification-service

/api/admin/email/**         -> user-service 或 notification-service
```

说明：

- `/api/admin/**` 不建议作为单独的用户管理服务。
- `/api/admin/**` 更适合作为后台管理入口，网关按具体业务子路径转发到对应服务。
- Gateway 可以校验 token，但不应该承载注册、验证码、用户资料、密码修改等用户业务逻辑。

## 3. user-service

`user-service` 负责账号、认证、用户资料和权限相关能力。

认证接口：

```text
GET   /api/auth/ask-code
POST  /api/auth/register
POST  /api/auth/reset-confirm
POST  /api/auth/reset-password
POST  /api/auth/login
POST  /api/auth/logout
```

用户接口：

```text
GET   /api/user/info
GET   /api/user/details
POST  /api/user/save-details
POST  /api/user/modify-email
POST  /api/user/change-password
GET   /api/user/privacy
POST  /api/user/save-privacy
```

后台用户管理接口：

```text
GET   /api/admin/user/list
GET   /api/admin/user/detail
POST  /api/admin/user/save
POST  /api/admin/user/change-password
```

可选归属：

```text
GET   /api/admin/email/list
GET   /api/admin/email/resend
```

如果邮件记录主要服务账号验证码、注册、重置密码、修改邮箱等流程，建议放在 `user-service`。

## 4. forum-service

`forum-service` 负责论坛主业务。

帖子接口：

```text
GET   /api/forum/types
POST  /api/forum/create-topic
GET   /api/forum/list-topic
GET   /api/forum/top-topic
GET   /api/forum/topic
POST  /api/forum/update-topic
GET   /api/forum/delete-topic
GET   /api/forum/search-topic
GET   /api/forum/user-topic
```

帖子草稿接口：

```text
GET   /api/forum/topic-draft/list
GET   /api/forum/topic-draft/detail
POST  /api/forum/topic-draft/save
GET   /api/forum/topic-draft/delete
```

互动与收藏接口：

```text
GET   /api/forum/interact
GET   /api/forum/collects
```

评论接口：

```text
POST  /api/forum/add-comment
GET   /api/forum/comments
GET   /api/forum/delete-comment
```

后台论坛管理接口：

```text
GET   /api/admin/forum/list
GET   /api/admin/forum/delete
POST  /api/admin/forum/top
POST  /api/admin/forum/locked
POST  /api/admin/forum/invisible
GET   /api/admin/forum/prohibited-list
POST  /api/admin/forum/prohibited-save
POST  /api/admin/forum/update-type
GET   /api/admin/forum/delete-type
POST  /api/admin/forum/create-type
GET   /api/admin/forum/change-topic-type
GET   /api/admin/forum/sync-to-es
```

待调整接口：

```text
GET   /api/forum/weather
```

`/api/forum/weather` 不属于论坛核心业务。短期可以保留在 `forum-service`，后续如果有公共信息服务，可以迁移出去。

## 5. announcement-service

`announcement-service` 负责校园公告的前台展示和后台管理。

公开公告接口：

```text
GET   /api/announcement/latest
GET   /api/announcement/list
GET   /api/announcement/detail
```

后台公告管理接口：

```text
GET   /api/admin/announcement/list
POST  /api/admin/announcement/create
POST  /api/admin/announcement/update
POST  /api/admin/announcement/publish
POST  /api/admin/announcement/top
GET   /api/admin/announcement/delete
```

## 6. oss-service

`oss-service` 负责文件、图片和对象存储访问。

图片上传接口：

```text
POST  /api/image/cache
POST  /api/image/avatar
```

文本文件上传和解析接口：

```text
POST  /api/file/text
```

对象访问接口：

```text
GET   /images/**
```

说明：

- `/api/image/cache` 用于帖子、公告等富文本图片上传。
- `/api/image/avatar` 用于用户头像上传。
- `/api/file/text` 当前主要服务 AI 上传文本文件读取内容，但从能力上看更像通用文件服务。
- `/images/**` 是 MinIO 图片读取接口，必须和 OSS 服务放在一起。

## 7. ai-service

`ai-service` 负责 AI 会话和聊天。

会话接口：

```text
GET     /api/ai/conversations
POST    /api/ai/conversations
DELETE  /api/ai/conversations/{id}
GET     /api/ai/conversations/{id}/messages
```

聊天接口：

```text
POST    /api/ai/chat
POST    /api/ai/chat/{conversationId}
```

说明：

- `/api/ai/chat` 和 `/api/ai/chat/{conversationId}` 是流式响应接口。
- Gateway 需要支持 SSE 转发。
- 拆分时要重点验证流式响应不会被网关缓冲或中断。

## 8. notification-service

`notification-service` 负责站内通知。

通知接口：

```text
GET   /api/notification/list
GET   /api/notification/delete
GET   /api/notification/delete-all
```

后续可接入的事件来源：

```text
论坛评论通知
帖子互动通知
公告发布通知
系统通知
管理员操作通知
```

## 9. 服务注册与配置

使用 Nacos 作为基础设施。

职责：

- 服务注册
- 服务发现
- 配置中心
- 服务健康检查

建议服务注册名：

```text
gateway-service
user-service
forum-service
announcement-service
oss-service
ai-service
notification-service
```

Gateway 路由示例：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: forum-service
          uri: lb://forum-service
          predicates:
            - Path=/api/forum/**,/api/admin/forum/**
```

## 10. OpenFeign 服务间调用

服务之间使用 OpenFeign 调用。

典型调用关系：

```text
forum-service -> user-service
```

用于查询发帖人、评论人昵称、头像、角色等用户信息。

```text
forum-service -> notification-service
```

用于评论、点赞、收藏、管理操作后生成通知。

```text
announcement-service -> notification-service
```

用于公告发布后生成通知。

```text
ai-service -> oss-service
```

用于读取上传文件、图片或附件内容。

```text
gateway-service -> user-service
```

可选，用于复杂权限校验、token 黑名单校验、用户状态校验等。

## 11. 推荐拆分顺序

第一阶段：网关化。

```text
gateway-service + Nacos + 当前单体后端注册
```

目标是先跑通：

- 服务注册
- 网关路由
- JWT 校验
- 用户身份透传
- 前端访问路径不变

第二阶段：拆 AI 和 OSS。

```text
ai-service
oss-service
```

这两个服务边界清晰，和论坛主链路耦合相对低。

第三阶段：拆用户认证。

```text
user-service
```

这一阶段需要处理：

- 登录签发 token
- 网关鉴权
- 用户状态校验
- 管理员权限
- 服务间用户信息查询

第四阶段：拆业务主模块。

```text
forum-service
announcement-service
notification-service
```

论坛服务依赖用户、通知、OSS，耦合更高，建议后拆。

## 12. 结论

当前讨论的方案可行：

```text
Nacos + Spring Cloud Gateway + OpenFeign
```

但不建议一次性完全微服务化。更稳妥的路线是先建立网关和注册中心，把当前单体作为一个服务接入，然后逐步拆分边界清晰的服务。

最终接口归属建议：

```text
/api/auth/**                 user-service
/api/user/**                 user-service
/api/admin/user/**           user-service
/api/admin/email/**          user-service 或 notification-service

/api/forum/**                forum-service
/api/admin/forum/**          forum-service

/api/announcement/**         announcement-service
/api/admin/announcement/**   announcement-service

/api/image/**                oss-service
/api/file/**                 oss-service
/images/**                   oss-service

/api/ai/**                   ai-service

/api/notification/**         notification-service
```

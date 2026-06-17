# gateway-service

Spring Cloud Gateway 网关服务，当前第一版只负责把请求转发到现有单体后端。

## 启动顺序

1. 启动 Nacos，默认地址：

```text
localhost:8848
```

2. 启动现有单体后端：

```bash
cd my-project-backend
mvn spring-boot:run
```

单体后端注册服务名：

```text
forum-monolith-service
```

3. 启动 Gateway：

```bash
cd gateway-service
mvn spring-boot:run
```

Gateway 默认端口：

```text
8081
```

## 当前路由

```text
/api/**     -> lb://forum-monolith-service
/images/**  -> lb://forum-monolith-service
```

## 可配置项

```text
GATEWAY_SERVER_PORT  Gateway 端口，默认 8081
NACOS_SERVER_ADDR    Nacos 地址，默认 localhost:8848
NACOS_DISCOVERY_ENABLED  是否启用 Nacos 注册发现，默认 true
GATEWAY_CORS_ALLOWED_ORIGIN  CORS 允许来源，默认 http://127.0.0.1:5273
GATEWAY_CORS_ALLOWED_ORIGIN_ALT  CORS 备用允许来源，默认 http://localhost:5273
```

示例：

```bash
NACOS_SERVER_ADDR=127.0.0.1:8848 GATEWAY_SERVER_PORT=8081 mvn spring-boot:run
```

只验证 Gateway 启动、不连接 Nacos 时：

```bash
NACOS_DISCOVERY_ENABLED=false mvn spring-boot:run
```

## 当前边界

这一版 Gateway 负责统一处理 CORS，但不做 JWT 鉴权，认证和权限仍由单体后端处理。后续可以逐步把 token 校验、用户身份透传和后台权限拦截前移到 Gateway。

单体后端的 CORS 默认关闭，避免和 Gateway 重复写入响应头。需要绕过 Gateway 直连后端调试时，可以用下面的环境变量临时开启：

```bash
BACKEND_CORS_ENABLED=true mvn spring-boot:run
```

# 垃圾回收系统后端（garbage_recycling）

> 文档最近更新：2026-05-05
>
> 本仓库为 Spring Boot 后端服务，包含：微信（含测试模式）登录鉴权、垃圾分类与图片资源、地址管理、个人/高校/企业下单、订单管理等。
>
> 说明：历史上 README 主要用于记录“个人下单回收模块”的演进过程。为方便新同学快速上手，现将 README 扩展为「项目级使用说明 + 变更记录附录」。

---

## 目录

1. [项目概览](#1-项目概览)
2. [技术栈与运行环境](#2-技术栈与运行环境)
3. [快速启动](#3-快速启动)
4. [配置说明（application.yml）](#4-配置说明applicationyml)
5. [接口与功能一览](#5-接口与功能一览)
6. [Swagger / HTTP 文件测试](#6-swagger--http-文件测试)
7. [常见问题排查](#7-常见问题排查)
8. [附录：模块演进记录（历史文档原文）](#8-附录模块演进记录历史文档原文)

---

## 1. 项目概览

- 服务名：`garbage_recycling`
- 默认端口：`8080`
- 统一前缀：`/api`（来自 `server.servlet.context-path`）
- 接口文档：`http://localhost:8080/api/swagger-ui/index.html`

核心业务模块：

- **认证/用户**：`/api/auth/*`（微信登录、Token 校验、用户信息完善）
- **垃圾分类**：`/api/category/*`（一级/二级分类、详情）
- **图片资源**：`/api/identity/*`（身份背景图）、分类图片由服务端拼装 URL
- **地址管理（推荐）**：`/api/address/*`（对应 `user_address` 表）
- **个人订单**：`/api/order/*`
- **高校订单**：`/api/campusOrder/*`
- **企业订单/计划**：`/api/enterprise/*`
- **订单管理**：`/api/manage/order/*`（查询详情、修改、取消、再来一单等）
- **回收助手（AI）**：`/api/assistant/*`（LangChain4j：RAG 问答 + Tool 调用生成下单草稿 + 二次确认）

> 兼容说明：项目中还保留了部分“个人中心地址接口” `/api/user/center/addresses*`（对应 `address` 表）。如果你在做新功能或联调，优先使用 `/api/address/*` 这一套（`user_address` 表）。

---

## 2. 技术栈与运行环境

- Java：**17**（见 `pom.xml`）
- Spring Boot：**3.5.x**
- 数据访问：MyBatis-Plus
- 数据库：MySQL 8.x（项目未内置 SQL 初始化脚本，需自行准备表结构）
- 缓存：Redis（`spring.data.redis.*`，按业务使用情况决定是否必须）
- API 文档：springdoc-openapi（Swagger UI）

---

## 3. 快速启动

### 3.1 前置条件

1) 安装并配置：JDK 17、Maven、MySQL（以及可选的 Redis）。

2) 准备数据库（示例）：

- 创建数据库：`garbage_recycling`
- 导入表结构：**本仓库未提供 SQL 文件**。请按 `com.stu.entity.*` 以及现有线上/测试库结构创建表。

> 注意：表名包含 `order`（SQL 保留字），建表时通常需要使用反引号：`\`order\``。

### 3.2 本地配置建议（避免提交密钥）

`src/main/resources/application.yml` 当前包含连接信息与密钥字段（如数据库密码、微信密钥、JWT secret）。

本地开发建议：

- 新建 `src/main/resources/application-local.yml`（**不要提交到仓库**）覆盖敏感配置
- 启动时指定 profile：`SPRING_PROFILES_ACTIVE=local`

示例（仅示意，请按本地环境填写）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/garbage_recycling?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password: <your_password>

wechat:
  test:
    mode: true

jwt:
  secret: <change_me>
```

### 3.3 启动命令（PowerShell）

在项目根目录（包含 `pom.xml`）执行：

```powershell
mvn -DskipTests spring-boot:run
```

或先打包再运行：

```powershell
mvn -DskipTests package
java -jar .\target\garbage_recycling-0.0.1-SNAPSHOT.jar
```

启动成功后访问：

- Swagger UI：`http://localhost:8080/api/swagger-ui/index.html`

---

## 4. 配置说明（application.yml）

配置文件：`src/main/resources/application.yml`。

### 4.1 服务与序列化

- `server.port`：端口，默认 `8080`
- `server.servlet.context-path`：统一前缀，默认 `/api`
- `spring.jackson.date-format` / `spring.jackson.time-zone`：时间序列化格式与时区

### 4.2 数据源与 Redis

- `spring.datasource.*`：MySQL 连接信息（Hikari 连接池参数位于 `spring.datasource.hikari.*`）
- `spring.data.redis.*`：Redis 连接信息（如不使用 Redis，可按需关闭/替换）

### 4.8 Redis 高级缓存策略（本仓库已实现）

> 目标：在不引入 Elasticsearch 的前提下，先把高频“读多写少”的接口做缓存，并通过一些工程手段提升稳定性。

本项目已新增一个 Redis 缓存封装 Bean：`com.stu.util.RedisCacheClient`，并在 `com.stu.service.impl.CategoryServiceImpl` 中对“分类查询”做了缓存接入。

#### 4.8.1 覆盖的接口/查询点

当前主要缓存以下查询（高频、读多写少）：

- 一级分类列表：`GET /api/category/top`
- 二级分类列表：`GET /api/category/{parentId}/sub`
- 分类详情：`GET /api/category/{categoryId}`

#### 4.8.2 缓存 Key 设计

缓存 Key 常量位于：`com.stu.util.CacheConstants`

- 一级分类列表：`cache:category:top`
- 二级分类列表：`cache:category:sub:{parentId}`
- 分类详情：`cache:category:detail:{categoryId}`
- 缓存重建互斥锁：`lock:cache:{原缓存key}`

> 说明：锁 key 只用于“逻辑过期重建”场景（热点 Key），避免并发下大量线程同时回源数据库。

#### 4.8.3 策略说明（穿透 / 雪崩 / 击穿）

1) **缓存穿透（Penetration）**：缓存空值

- 当数据库查询返回 `null`（例如 `categoryId` 不存在）时，将在 Redis 中写入空字符串 `""` 作为占位符，并设置较短 TTL（默认 2 分钟）。
- 后续请求命中占位符时直接返回 `null`，避免每次都打到数据库。

2) **缓存雪崩（Avalanche）**：随机 TTL 抖动

- 写入缓存时，会在基础 TTL 上增加一个随机抖动（默认 0~300 秒），避免大量 key 同一时刻过期导致流量瞬间回源。

3) **缓存击穿（Breakdown / Hot Key）**：逻辑过期 + 异步重建

分类详情使用“逻辑过期”策略（`RedisData{expireTime,data}`）：

- 未过期：直接返回缓存
- 已过期：**先返回旧值**（保证可用性），同时尝试加锁，成功后异步回源数据库并刷新缓存

这种策略适合“热点 Key”场景：即便缓存过期，也不会让请求线程都阻塞等待回源。

#### 4.8.4 默认 TTL 参数（可按需调整）

目前默认值在 `com.stu.util.CacheConstants` 中：

- 分类列表缓存 TTL：30 分钟（带 TTL 抖动）
- 分类详情逻辑过期：30 分钟
- 分类详情物理 TTL：120 分钟（通常要 > 逻辑过期，防止 Redis 永不过期）
- 空值缓存 TTL：2 分钟
- TTL 抖动上限：300 秒
- 缓存重建锁 TTL：10 秒

> 建议：后续如果加了“后台修改分类/价格”的功能，需要在更新成功后主动删除对应缓存 key（或做更精细的 key 管理）。

#### 4.8.5 如何验证缓存是否生效

1) 启动 Redis（本机默认 `localhost:6379`，与 `application.yml` 一致）。

2) 启动后端，然后多次调用分类接口（例如 `/api/category/top`）。

3) 使用 `redis-cli`（如已安装）查看 key：

```bash
keys cache:category:*
ttl cache:category:top
get cache:category:detail:1
```

4) 验证“缓存空值”：请求一个不存在的分类 ID（例如 `999999`），然后检查 Redis 中是否写入了占位符 key，并且 TTL 较短。

> 注意：分类列表接口返回空列表属于“正常业务结果”，不会写入空字符串占位符；空字符串占位符仅用于数据库返回 `null` 的场景。

### 4.9 AI 回收助手（LangChain4j）配置（可选）

本项目提供一个“回收助手”模块：RAG 检索增强问答 + Tool 调用生成下单草稿。

默认使用 OpenAI（可切换为 Ollama）。建议通过环境变量注入密钥与模型配置，避免写入仓库。

#### 4.9.1 OpenAI（默认）

设置环境变量：

- `AI_PROVIDER=openai`
- `OPENAI_API_KEY=<your_key>`
- `OPENAI_MODEL=gpt-4o-mini`（可选）

#### 4.9.2 Ollama（本地模型）

如果本机已安装并启动 Ollama：

- `AI_PROVIDER=ollama`
- `OLLAMA_BASE_URL=http://localhost:11434`
- `OLLAMA_MODEL=qwen2.5:7b`

#### 4.9.3 RAG 知识库

知识库文件位于：`src/main/resources/rag/recycling_knowledge.md`

> 约束：知识库只存公共规则/流程，不要放用户手机号、详细地址等隐私数据；助手输出也应做脱敏。

#### 4.9.4 模块结构（关键类一览）

AI 模块代码位于：`src/main/java/com/stu/ai/*`，核心组件如下：

- 配置：
  - `com.stu.ai.config.AiProperties`：读取 `ai.*` 配置
  - `com.stu.ai.config.AiConfig`：按 `ai.provider` 构建 `ChatLanguageModel`；按 `ai.rag.enabled` 构建本地 Embedding + Retriever
- RAG 初始化：
  - `com.stu.ai.rag.KnowledgeBaseInitializer`：启动时加载 `recycling_knowledge.md` → 切分 → 向量化 → 写入 `EmbeddingStore`
- Assistant 编排与对话：
  - `com.stu.ai.service.RecyclingAssistant`：LangChain4j AI Service（系统提示词定义安全边界）
  - `com.stu.ai.service.RecyclingAssistantOrchestrator`：每次请求构建 assistant 实例（用于注入“与 userId 绑定”的工具对象）
- Tool（工具函数）：
  - `com.stu.ai.tools.AddressAssistantTools`：`list_my_addresses`（只返回脱敏信息）
  - `com.stu.ai.tools.OrderDraftAssistantTools`：`create_personal_order_draft`（只生成草稿，不创建真实订单）
- 草稿服务（两阶段提交）：
  - `com.stu.ai.service.OrderDraftService`：草稿写入 Redis + 二次确认提交创建真实订单
- Controller：
  - `com.stu.ai.controller.AssistantController`：`/assistant/chat`、`/assistant/order/draft/confirm`

#### 4.9.5 RAG（检索增强生成）工作流程

本项目的 RAG 目标：让助手回答“垃圾分类/下单流程/接口用法/注意事项”等问题时，能够**基于项目知识库**给出更贴合本系统的答案，而不是泛泛而谈。

流程如下：

1) **知识库编写**：
   - 文件：`src/main/resources/rag/recycling_knowledge.md`
   - 内容范围：公共规则、接口说明、流程提示、常见问答
   - 禁止内容：用户手机号、详细地址、身份证等 PII

2) **启动加载与切分**（`KnowledgeBaseInitializer`）：
   - 启动时读取 knowledge markdown
   - 当前切分策略：按空行分段（段落级 chunk）

3) **向量化与存储**（本地 Embedding）：
   - Embedding 模型：`AllMiniLmL6V2EmbeddingModel`（本地，**不依赖外部 key**）
   - EmbeddingStore：`InMemoryEmbeddingStore`（内存型，适合本地开发/演示）

4) **检索与注入**（ContentRetriever）：
   - 对用户输入进行向量检索，取回相似片段（`ai.rag.max-results`）
   - 过滤低相似度片段（`ai.rag.min-score`）
   - LangChain4j 会将检索结果作为上下文提供给 LLM，以提升回答准确性

> 说明：当前 EmbeddingStore 采用内存实现，重启应用会重新加载知识库并向量化；如需生产化，可替换为持久化向量库（如 pgvector、Milvus、Elastic vector、Redis vector 等）。

#### 4.9.6 Tool Calling（工具调用）设计：只生成“下单草稿”

助手具备“工具调用”能力：当用户表达明确意图（例如“帮我下单/用默认地址下单/预约明天上午”等），LLM 可以调用后端工具函数获取信息或生成草稿。

本项目只开放两类工具（白名单）：

1) `list_my_addresses`
   - 实现：`AddressAssistantTools#listMyAddresses`
   - 用途：让助手能拿到当前用户的地址 id 列表（已脱敏），从而建议/选择 `addressId`

2) `create_personal_order_draft`
   - 实现：`OrderDraftAssistantTools#createPersonalOrderDraft`
   - 用途：生成个人订单“草稿”，返回 `draftId + summary`
   - 注意：**不会创建真实订单**，不会触发支付

为什么只做草稿？

- LLM 可能产生误操作或被 prompt 注入诱导，直接创建真实订单有交易/安全风险
- 草稿 + 二次确认可以把“写操作”变成显式用户行为，满足可审计、可控的业务要求

#### 4.9.7 二次确认（Two-step Confirmation）与草稿存储

草稿存储与提交逻辑位于：`OrderDraftService`

- 草稿写入 Redis：
  - key：`draft:order:{draftId}`
  - value：`OrderDraft{userId,draftType,orderData,createdAt}` 的 JSON
  - TTL：30 分钟（草稿过期后无法提交）

- 草稿提交（创建真实订单）：
  - 接口：`POST /api/assistant/order/draft/confirm`
  - 入参：`{ "draftId": "..." }`
  - 服务端校验：
    1) 草稿是否存在/未过期
    2) 草稿 `userId` 是否与当前 token 用户一致（防越权）
    3) 草稿类型是否支持提交（当前仅 `personal`）
    4) 草稿中的 `addressId` 是否属于当前用户（草稿生成阶段已校验，提交阶段再次校验也可加固）
  - 创建订单：最终调用 `PersonOrderService#createPersonalOrder` 复用既有下单逻辑

> 行为约束：草稿提交成功后会删除 Redis 草稿 key；提交失败则保留草稿，方便用户调整后再次确认。

#### 4.9.8 安全与隐私（必须了解）

1) **工具白名单**：仅暴露“地址列表（脱敏）/生成草稿”两类工具；不提供“取消订单/修改订单/支付”等高风险工具。

2) **服务端强校验**：
   - 草稿与地址都绑定 `userId` 校验，避免越权。

3) **隐私脱敏**：
   - `AddressAssistantTools` 对详细地址做截断脱敏（仅保留前 6 个字符）。
   - `RecyclingAssistant` 的系统提示词明确禁止输出用户手机号、完整地址等 PII。

4) **防 Prompt Injection 的基本策略**（建议面试/答辩可讲）：
   - 把“可调用工具”限定在最小集合，并在工具层做权限校验。
   - 对所有写操作引入二次确认（本项目已落地）。
   - 不把敏感信息写入知识库。

#### 4.9.9 调试与模型选择建议

- **OpenAI**：函数/工具调用能力相对稳定，适合演示 Tool Calling。
- **Ollama（本地模型）**：可用于本地对话，但不同模型对 tool/function calling 支持程度不同；若出现“只聊天不调用工具”的情况，建议：
  1) 换更支持工具调用的模型
  2) 调整提示词，让用户意图更明确（例如显式给出 addressId）

#### 4.9.10 如何关闭 AI 模块

如果你暂时不需要 AI（或本地没有模型/密钥），可以在 `application.yml` 中设置：

```yaml
ai:
  enabled: false
```

关闭后：`/api/assistant/*` 接口不会注册，应用仍可正常启动与使用其他业务模块。

### 4.3 MyBatis-Plus

- `mybatis-plus.mapper-locations`：XML 映射位置（`classpath:mapper/*.xml`）
- `mybatis-plus.type-aliases-package`：实体别名包（`com.stu.entity`）
- `mybatis-plus.configuration.map-underscore-to-camel-case`：下划线转驼峰
- `mybatis-plus.global-config.db-config.logic-delete-*`：逻辑删除字段配置

### 4.4 微信登录（含测试模式）

- `wechat.app-id` / `wechat.app-secret`：正式环境微信配置
- `wechat.auth-url`：`jscode2session` 请求模板
- `wechat.test.mode`：测试模式开关（`true/false`）

测试模式下，可用 `code`：

- `test_code_person`：个人测试用户
- `test_code_campus`：校园测试用户
- `test_code_enterprise`：企业测试用户（会在登录接口中设置 `userType=enterprise` 相关字段）
- 以及任何 `test_code*` 前缀的动态 code（会自动生成 mock openid）

### 4.5 JWT

- `jwt.secret`：签名密钥（建议本地/生产分别配置，并避免泄露）
- `jwt.expiration`：过期时间（毫秒）
- `jwt.header`：请求头字段名（默认 `Authorization`）

### 4.6 文件上传与图片目录

- `app.upload-dir`：上传根目录（默认 `./uploads`）
- `app.images.category-path`：分类图片目录
- `app.images.identity-bg-path`：身份背景图目录

### 4.7 高校价格配置（示例）

- `recycling.price.base.*`：基础价格
- `recycling.price.pilot.period`：是否启用试点期溢价
- `recycling.price.pilot.premium-rate`：溢价比例
- `recycling.price.pilot.<category>`：试点期分类价格

---

## 5. 接口与功能一览

> 下文路径均默认带上 `/api` 前缀。

### 5.1 认证接口（`/auth`）

- `POST /auth/wechat/login`：微信登录（测试模式支持 `test_code_*`）
- `PUT /auth/completeUserInfo`：完善用户信息（需 Bearer Token）
- `GET /auth/currentUser`：获取当前用户信息（需 Bearer Token）
- `GET /auth/checkToken`：验证 Token

### 5.2 地址管理（推荐，`/address`，对应 `user_address` 表）

- `POST /address/create`
- `GET /address/list`
- `POST /address/default/{id}`

### 5.3 个人订单（`/order`）

- `POST /order/personal`：创建个人回收订单
- `GET /order/list?page=1&size=10`：订单列表
- `POST /order/cancel/{orderId}`：取消订单

### 5.4 高校订单（`/campusOrder`）

- `POST /campusOrder/textbook`：教材回收订单
- `POST /campusOrder/dormitory`：宿舍集中回收订单

### 5.5 企业订单（`/enterprise`）

- `POST /enterprise/orders/bulk`：企业批量回收下单
- `POST /enterprise/plans`：创建企业定期回收计划

### 5.6 垃圾分类（`/category`）

- `GET /category/top`：一级分类
- `GET /category/{parentId}/sub`：二级分类
- `GET /category/{categoryId}`：分类详情

### 5.7 身份背景图（`/identity`）

- `GET /identity/all`：获取所有身份类型背景图信息（返回 URL）
- `GET /identity/proxy/{identityType}`：代理读取图片二进制（适合前端直接展示）

### 5.8 订单管理（`/manage/order`）

该部分接口偏“订单管理/增强操作”，路径组合中存在历史遗留的 `manage` 前缀，请以 Swagger 文档为准：

- `GET /manage/order/detail/{orderId}`：订单详情
- `PUT /manage/order/modify/{orderId}`：修改订单（addressId/scheduledTime）
- `POST /manage/order/manage/cancel/{orderId}`：取消订单
- `POST /manage/order/repeat/{orderId}`：再来一单
- `GET /manage/order/manage/list?page=1&size=10&status=`：订单列表（可按状态筛选）

### 5.9 回收助手（LangChain4j，`/assistant`）

> 本模块用于演示/落地：RAG 知识检索增强问答 + Tool 调用生成“下单草稿” + 二次确认提交流程。
>
> 安全策略：助手**只能生成草稿**，不会直接创建真实订单；真实订单必须由用户显式调用确认接口提交草稿。

- `POST /assistant/chat`：与回收助手对话（需 Bearer Token）
  - 输入：`sessionId`（可选，不传则后端生成并返回）、`message`
  - 输出：`{ sessionId, answer }`
  - 说明：当用户表达“我要下单/帮我下单”意图时，助手可通过 Tool 调用生成个人订单草稿，并在回答中返回 `draftId`。

- `POST /assistant/order/draft/confirm`：二次确认并提交草稿（需 Bearer Token）
  - 输入：`{ draftId }`
  - 输出：与 `POST /order/personal` 一致的 `Result`（包含 `orderId/orderNo` 等）

示例（生成草稿 → 二次确认提交）：

1) 对话生成草稿

```http
POST http://localhost:8080/api/assistant/chat
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "sessionId": "demo_session_1",
  "message": "我想下一个个人回收订单，地址用默认地址，预约明天上午10点，物品是废纸5kg"
}
```

2) 复制助手返回的 `draftId`，提交草稿

```http
POST http://localhost:8080/api/assistant/order/draft/confirm
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "draftId": "<assistant_returned_draftId>"
}
```

---

## 6. Swagger / HTTP 文件测试

### 6.1 Swagger

1) 启动服务后访问：`http://localhost:8080/api/swagger-ui/index.html`
2) 先调用 `POST /auth/wechat/login` 获取 token
3) Swagger 页面右上角 **Authorize** 填：`Bearer {token}`（注意 `Bearer` 后有空格）
4) 测试其他需要鉴权的接口

### 6.2 IntelliJ HTTP Client

测试脚本：`src/test/java/OrderTest.http`

- 环境变量文件：`src/test/java/http-client.private.env.json`
- README 中示例变量名建议：`personal_token` / `campus_token` / `token`（企业）等

示例 env（请自行替换 token；时间字段可按需调整）：

```json
{
  "dev": {
    "personal_token": "<personal_jwt>",
    "campus_token": "<campus_jwt>",
    "token": "<enterprise_jwt>",
    "nextHour": "2026-05-02T18:00:00",
    "nextDay": "2026-05-03"
  }
}
```

> 编码提示：如果你打开 `OrderTest.http` 出现中文乱码，请在 IDE 中将文件编码设置为 UTF-8 并重新保存（历史版本可能以非 UTF-8 保存）。

---

## 7. 常见问题排查

1) **Swagger 访问 404**

- 请确认 `server.servlet.context-path: /api` 是否生效
- 正确地址：`/api/swagger-ui/index.html`

2) **401 / 提示未提供有效 Token**

- 请求头必须包含：`Authorization: Bearer <token>`
- 注意大小写与空格

3) **数据库连接失败**

- 检查 `spring.datasource.url/username/password`
- 检查 MySQL 网络与权限

4) **插入订单报 items 默认值问题**

- 历史问题：`Field 'items' doesn't have a default value`
- 若仍遇到：确认数据库字段允许空/有默认值，或确保服务层/前端传入 `items`（项目已做多层兜底）

5) **YAML/中文编码导致启动异常**

- 建议所有配置文件统一 UTF-8
- IDEA 启动可在 VM options 加：`-Dfile.encoding=UTF-8`

---

## 8. 附录：模块演进记录（历史文档原文）

> 下文为历史版本 README 内容（用于记录“个人下单 → 高校下单 → 地址管理 → 价格计算 → 企业下单”等演进），保留以便追溯设计决策。

---

# 个人下单回收模块代码修改说明 (更新版)

> 最近更新日期：2025-10-11
>
> 本文在原始“个人下单回收模块”完成后，补充记录后续新增的高校下单、地址管理、价格计算、实体字段扩展、企业下单相关接口与测试方式等所有改动，便于团队成员同步最新设计与代码演进脉络。

---
## 目录
1. 初版回顾（个人下单功能）
2. 后续新增与调整总览
3. 新增/修改的核心模块与文件清单
4. 实体与数据库结构最新说明（含用户企业字段）
5. 订单创建/金额/地址/鉴权流程最新说明
6. 已修复问题与设计权衡
7. 测试脚本与调试方式（Swagger 与 HTTP 文件）
8. 待改进 / 下一步规划
9. 版本里程碑与时间线
10. 附：关键 API 列表（更新）

---
## 1. 初版回顾（个人下单功能）
初版交付内容（已在之前文档中说明）：
- 支持个人用户：创建订单 / 查询订单列表 / 取消订单
- 依赖：MyBatis-Plus、JWT 登录态、微信模拟登录（WeChatUtil 测试code）
- 核心类：`PersonOrderService(Impl)`、`OrderController`、`Order`、`OrderMapper`、`Result`、`JwtInterceptor` / `WebMvcConfig`

> 注意：初版文档中提及的 `OrderService / OrderServiceImpl` 命名与当前代码不完全一致，实际已采用更明确的 `PersonOrderService / PersonOrderServiceImpl`。

---
## 2. 后续新增与调整总览
| 类别 | 说明 | 状态 |
|------|------|------|
| 高校下单模块 | 新增教材回收 / 宿舍批量回收 | 已完成 |
| 地址管理模块 | 新增地址实体、创建/列表/设置默认接口 | 已完成 |
| 价格计算模块 | PriceConfig + PriceCalculationService(Impl) | 已完成（高校使用，个人暂为 0） |
| 订单实体扩展 | campus_type / campus_info / estimated_amount 默认逻辑 / items 默认值 | 已完成 |
| DTO 扩展 | TextbookOrderDTO / DormitoryOrderDTO + items 可选字段 | 已完成 |
| WeChat 测试能力 | test_mode + 动态 test_code 支持 | 已完成 |
| 时间戳自动填充 | MyMetaObjectHandler | 已完成 |
| 全局参数校验 | 加入 jakarta validation + @Valid | 已完成 |
| BUG 修复 | Field 'items' doesn't have a default value | 已修复（多层兜底） |
| HTTP 测试脚本 | 全链路包含：登录→建地址→个人下单→高校教材→高校宿舍 | 已更新 |
| 统一返回 | 高校/企业下单返回 estimatedAmount（企业可置空，后续运营填写） | 已完成 |
| 企业下单模块 | 企业批量下单 / 定期回收计划 | 已完成（接口） |

---
## 3. 新增/修改的核心模块与文件清单

### 3.1 新增文件（节选）
| 文件 | 作用 |
|------|------|
| `CampusOrderService` / `CampusOrderServiceImpl` | 高校订单创建（教材 / 宿舍）与高校身份校验 |
| `TextbookOrderDTO` / `DormitoryOrderDTO` | 高校订单专用请求数据载体（含 items 可选） |
| `UserAddress` | 用户地址实体，对应 `user_address` 表 |
| `UserAddressMapper` / `UserAddressService(Impl)` | 地址数据访问与归属校验 |
| `AddressController` | 地址创建 / 列表 / 设置默认接口 |
| `PriceConfig` | 基础价格 + 试点期价格配置（内置初值，可扩展 YAML 配置） |
| `PriceCalculationService` / `PriceCalculationServiceImpl` | 高校订单金额估算逻辑（教材 / 宿舍） |
| `MyMetaObjectHandler` | MyBatis-Plus 自动填充 createdAt / updatedAt |
| `SnowflakeIdGenerator` | 高校订单号（CAMP 前缀）生成 |
| `OrderTest.http`（更新） | 集成测试脚本，覆盖全流程 |
| `AddressCreateDTO` | 创建地址请求体 |
| `EnterpriseOrderController` | 企业订单接口（批量下单、回收计划） |
| `BulkOrderRequest` / `RecyclingPlanRequest` | 企业批量下单 / 定期回收计划请求模型 |
| `OrderItem` / `InvoiceInfo` | 企业订单项与发票信息模型 |
| `OrderResponse` / `RecyclingPlanResponse` / `ApiResponse` | 企业接口统一响应模型 |

### 3.2 重要修改文件（节选）
| 文件 | 修改要点 |
|------|-----------|
| `Order.java` | 新增 `campusType`、`campusInfo`、为 `items` 增加默认值`"[]"`，保留金额字段；增加中文注释 |
| `CampusOrderController` | 新增高校教材/宿舍下单 API，启用 @Valid |
| `OrderController` | 个人下单、订单列表、取消订单接口，增加 @Operation 注解（Swagger） |
| `PersonOrderServiceImpl` | 地址合法性校验；个人预估金额固定 0；日志增强 |
| `EnterpriseOrderController` | 新增企业批量下单与定期回收计划接口（Swagger 注解完整） |
| `EnterprisePriceService`（如启用） | 企业订单估价逻辑，支持吨/公斤转换与批量折扣 |
| `application.yml` | 统一 UTF-8、修正 mybatis-plus 包路径、加入价格配置、数据库 URL 规范化；context-path 为 `/api` |
| `pom.xml` | 添加 validation 依赖、编码属性、编译插件编码设置 |
| `GlobalExceptionHandler`（若存在） | 处理参数校验异常（收集 message） |
| `Result.java` | 支持链式 put；统一响应格式（个人模块使用） |

---
## 4. 实体与数据库结构最新说明（含用户企业字段）
### 4.1 `order` 表（新增 / 关注字段）
| 字段 | 说明 | 备注 |
|------|------|------|
| order_no | 订单号 | 个人：`ORD+UUID16`；高校：`CAMP+雪花ID`；企业：`ENT+规则`（视实现） |
| order_type | 1=个人 2=高校 3=企业 | 用于业务分流 |
| campus_type | 1=教材 2=宿舍 | 高校子类型 |
| campus_info | 高校扩展 JSON | 存教材/宿舍特征字段 |
| items | JSON（默认 "[]"） | 个人/高校/企业：物品明细 |
| estimated_amount | 预估金额 | 高校：由价格模块赋值；个人：0；企业：可置空，后续运营填写 |
| final_amount | 最终金额 | 后续结算使用 |

### 4.2 `user_address` 表
现有结构：`id,user_id,address_label,detail_address,contact_name,contact_phone,location,is_default,created_at,updated_at`
> 无 province/city/district 字段，实体已对齐；location 以 JSON 字符串存储（可后续加 TypeHandler）。

### 4.3 `user` 表新增企业相关字段
执行的 DDL（摘要）：
- `user_type` ENUM('personal','campus','enterprise') NOT NULL DEFAULT 'personal' 注：企业接口需要 `enterprise`；
- `company_name` VARCHAR(100)
- `unified_social_credit_code` VARCHAR(20)
- `invoice_title` VARCHAR(100)
- `tax_number` VARCHAR(20)

影响评估：
- 向后兼容：新增字段允许 NULL（除 user_type 有默认值），不会影响现有个人/高校流程；
- 企业接口会校验 `user_type=enterprise`，不具备资质则无法下企业单；
- 开票相关字段用于回填/默认值，具体以请求体 `invoiceInfo`/`invoiceConfig` 为准。

---
## 5. 最新业务流程说明（含个人与企业）
### 5.1 个人下单流程与接口
- 控制器：`OrderController`
- 路径前缀：`/api`（来自 `server.servlet.context-path`）
- 认证：`Authorization: Bearer {token}`

1) 创建个人回收订单
- 方法与路径：`POST /api/order/personal`
- 请求头：`Authorization: Bearer {token}`
- 请求体：JSON（当前使用 `Map<String,Object>` 承载）
  - 常见字段：
    - `addressId` Long 必填（需归属校验）
    - `items` 数组 可选（不传也可，后端有兜底）
    - `appointmentTime` String 可选（格式 `yyyy-MM-dd HH:mm:ss`）
    - 其他字段按页面表单填充
- 返回：`Result{ code, message, data }`
  - 成功时 `code=200`，`data` 通常包含 `orderId`、`orderNo` 等
- 规则：
  - 预估金额固定 0（个人不计价）
  - 地址必须是当前用户的默认或指定地址；否则返回错误信息

2) 获取用户订单列表
- 方法与路径：`GET /api/order/list?page=1&size=10`
- 返回：`Result{ code, message, data }`，按创建时间倒序

3) 取消订单
- 方法与路径：`POST /api/order/cancel/{orderId}`
- 规则：仅允许取消状态为“待接单”的订单

示例请求（创建个人订单）：
```
POST /api/order/personal
Authorization: Bearer {token}
Content-Type: application/json

{
  "addressId": 123,
  "items": [{"category":"waste_paper","quantity":2,"weightKg":5}],
  "appointmentTime": "2025-10-12 10:00:00"
}
```
成功标志：HTTP 200 且响应 `code=200`。

### 5.2 高校订单（教材/宿舍）
- 控制器：`CampusOrderController`
- 路径前缀：`/api/campusOrder`
- Swagger 分组：`校园订单下单`
- 认证：`Authorization: Bearer {token}`

1) 创建教材回收订单
- 方法与路径：`POST /api/campusOrder/textbook`
- 请求体模型：`TextbookOrderDTO`
  - `addressId` Long 必填
  - `scheduledTime` Date 必填（`@Future`，建议格式 `yyyy-MM-dd HH:mm:ss`）
  - `images` List<String> 可选
  - `textbookType` String 必填
  - `quantity` Integer 必填（>=1）
  - `unit` String 可选
  - `condition` String 必填
  - `pickupLocation` String 必填
  - `schoolName` String 可选
  - `department` String 可选
  - `items` List<Map<String,Object>> 可选（直接写入订单 items 字段）
- 响应：`Result{ code, message, data }`
- 规则：
  - 地址归属校验（须为当前用户地址）
  - 金额估算：已接入高校价格模块（如配置开启）

示例请求：
```
POST /api/campusOrder/textbook
Authorization: Bearer {token}
Content-Type: application/json

{
  "addressId": 123,
  "scheduledTime": "2025-10-12 09:30:00",
  "images": ["http://example.com/a.jpg"],
  "textbookType": "通识教材",
  "quantity": 20,
  "unit": "本",
  "condition": "八成新",
  "pickupLocation": "一号教学楼北门",
  "schoolName": "某某大学",
  "department": "计算机学院",
  "items": [{"category":"waste_paper","weightKg":10}]
}
```
成功标志：HTTP 200 且响应 `code=200`，`data` 中包含订单标识。

2) 创建宿舍回收订单
- 方法与路径：`POST /api/campusOrder/dormitory`
- 请求体模型：`DormitoryOrderDTO`
  - `addressId` Long 必填
  - `scheduledTime` Date 必填（`@Future`，建议格式 `yyyy-MM-dd HH:mm:ss`）
  - `images` List<String> 可选
  - `dormitoryBuilding` String 必填
  - `roomRange` String 必填（如："1-5层，501-520"）
  - `recyclingCategories` List<String> 必填（至少一项）
  - `contactPerson` String 必填
  - `contactPhone` String 必填（大陆手机号正则校验）
  - `schoolName` String 可选
  - `department` String 可选
  - `items` List<Map<String,Object>> 可选（统一写入订单 items 字段）
- 响应：`Result{ code, message, data }`
- 规则：
  - 地址归属校验
  - 可结合价格模块估算金额（按配置）

示例请求：
```
POST /api/campusOrder/dormitory
Authorization: Bearer {token}
Content-Type: application/json

{
  "addressId": 123,
  "scheduledTime": "2025-10-12 14:00:00",
  "images": [],
  "dormitoryBuilding": "东区3号楼",
  "roomRange": "301-320",
  "recyclingCategories": ["waste_paper","plastic_bottle"],
  "contactPerson": "张三",
  "contactPhone": "13800000000",
  "schoolName": "某某大学",
  "department": "材料学院",
  "items": [{"category":"plastic_bottle","weightKg":30}]
}
```
成功标志：HTTP 200 且响应 `code=200`，`data` 中包含订单标识。

### 5.3 企业批量回收下单
- 控制器：`EnterpriseOrderController`
- 方法与路径：`POST /api/enterprise/orders/bulk`
- 认证：`Authorization: Bearer {token}`（需要企业资质）
- 请求体模型：`BulkOrderRequest`
  - `items` List<OrderItem> 必填
  - `totalWeight` BigDecimal 必填，且 > 0（`@DecimalMin("0.1")`）
  - `weightUnit` WeightUnit 可选，默认 `KG`（支持 `KG`、`TON` 等）
  - `invoiceRequired` Boolean 可选，默认 `false`
  - `pickupAddress` String 必填
  - `contactPerson` String 必填
  - `contactPhone` String 必填，中国大陆手机号正则校验
  - `scheduleTime` LocalDateTime 可选，若传必须晚于当前时间
  - `invoiceInfo` InvoiceInfo 可选，含 `invoiceTitle`、`taxNumber`
- 响应：`ApiResponse<OrderResponse>`
  - 关键字段：`id`、`orderNo`、`orderType`、`estimatedAmount`（当前实现可为空，后续由运营填入）、`status`
- 说明：
  - 资质校验：`enterpriseService.validateEnterpriseQualification(token)` 校验 `user_type=enterprise` 等条件
  - 价格：默认不强制计算。如需启用系统估价，切换为 `EnterprisePriceService.calculateEnterpriseAmount(...)`

示例请求：
```
POST /api/enterprise/orders/bulk
Authorization: Bearer {token}
Content-Type: application/json

{
  "items": [
    {"category": "waste_paper", "quantity": 10, "weightKg": 100},
    {"category": "plastic_bottle", "quantity": 5, "weightKg": 50}
  ],
  "totalWeight": 150,
  "weightUnit": "KG",
  "invoiceRequired": true,
  "pickupAddress": "上海市浦东新区世纪大道100号",
  "contactPerson": "李四",
  "contactPhone": "13812345678",
  "scheduleTime": "2025-10-12T11:00:00",
  "invoiceInfo": {"invoiceTitle": "某某环保科技有限公司", "taxNumber": "91310000MA1K123456"}
}
```
成功标志：HTTP 200 且响应 `code=200`，`data.orderNo` 非空。

### 5.4 企业定期回收计划
- 控制器：`EnterpriseOrderController`
- 方法与路径：`POST /api/enterprise/plans`
- 认证：`Authorization: Bearer {token}`（需要企业资质）
- 请求体模型：`RecyclingPlanRequest`
  - `planName` String 必填
  - `cycleType` String 必填（`WEEKLY/BIWEEKLY/MONTHLY`）
  - `totalCycles` Integer 可选（>0），为空代表不限次数
  - `nextScheduleDate` LocalDate 必填（`@FutureOrPresent`）
  - `endDate` LocalDate 可选
  - `itemsConfig` String 必填（JSON 字符串）
  - `invoiceConfig` String 可选（JSON 字符串）
  - `startImmediately` Boolean 可选，若 true 将生成首单，并自动计算下一次执行日期
- 响应：`ApiResponse<RecyclingPlanResponse>`，包含 `id`、`planNo`、`planName`、`cycleType`、`status`

示例请求：
```
POST /api/enterprise/plans
Authorization: Bearer {token}
Content-Type: application/json

{
  "planName": "每周回收计划",
  "cycleType": "WEEKLY",
  "totalCycles": 12,
  "nextScheduleDate": "2025-10-13",
  "endDate": null,
  "itemsConfig": "{\"waste_paper\":\"100kg\",\"plastic_bottle\":\"50kg\"}",
  "invoiceConfig": "{\"invoiceTitle\":\"某某环保科技有限公司\",\"taxNumber\":\"91310000MA1K123456\"}",
  "startImmediately": true
}
```
成功标志：HTTP 200 且响应 `code=200`，`data.planNo` 非空。

---
## 6. 已修复问题与设计权衡
| 问题 | 现象 | 处理方案 | 备注 |
|------|------|---------|------|
| application.yml 编码异常 | 启动 YAML 解析失败 | 统一 UTF-8 + VM `-Dfile.encoding=UTF-8` | 已稳定 |
| Field 'items' doesn't have a default value | 高校/企业订单插入失败 | 三层兜底：服务层 setItems / 实体默认值 / 前端可传 | 已解决 |
| 地址外键约束失败 | 地址不存在或归属不符 | 下单前校验 + 明确错误提示 | 支持多用户并发 |
| 高校/企业订单金额控制 | 估价策略多变 | 引入配置与服务分层，可切换“系统估价/运营后填” | 便于灰度 |
| Swagger 访问路径 | context-path 影响 | Swagger UI：`/api/swagger-ui/index.html` | 详见下节 |

---
## 7. 测试脚本与调试方式（Swagger 与 HTTP 文件）
- Swagger 测试步骤：
  1) 启动服务后访问：`http://localhost:8080/api/swagger-ui/index.html`
  2) 右上角“Authorize”填入 `Bearer {token}`（含空格）；token 可通过 `/auth/wechat/login` 获取测试登录态
  3) 在“订单管理接口”中测试个人接口：`POST /order/personal`、`GET /order/list`、`POST /order/cancel/{orderId}`
  4) 在“校园订单下单”中测试高校接口：`POST /campusOrder/textbook`、`POST /campusOrder/dormitory`
  5) 在“企业订单接口”中测试企业接口：`POST /enterprise/orders/bulk`、`POST /enterprise/plans`
  6) 成功标准：HTTP 200 且响应体 `code=200`，`data` 中关键标识（如 `orderNo`/`planNo`）非空

- HTTP 文件（IDEA）动态变量：
  - `OrderTest.http` 中出现的 `{{token}}`、`{{nextHour}}`、`{{nextDay}}` 需在 `http-client.private.env.json` 中配置。
  - 示例：
    ```json
    {
      "dev": {
        "token": "替换为实际Bearer后半段或完整串",
        "nextHour": "2025-10-12T11:00:00",
        "nextDay": "2025-10-13"
      }
    }
    ```
  - 运行前选择对应环境（如 dev），避免“未替换的变量”报错。

- 企业测试账号：
  - 企业接口会校验资质。建议新建或转换一个用户 `user_type=enterprise`，并可按需补充 `company_name`、`unified_social_credit_code`、`invoice_title`、`tax_number`。
  - 不想改动现有个人用户时，可新增一条用户记录或提供模拟登录 code 绑定到新企业用户。

---
## 8. 待改进 / 下一步规划
| 优先级 | 事项 | 说明 |
|--------|------|------|
| 高 | 统一订单状态/类型枚举 | 消除魔法数字，集中常量管理 |
| 高 | 订单详情接口 | 解析 campusInfo & items 返回结构化数据 |
| 中 | 企业订单定价链路 | 开关化系统估价与运营定价、记录策略版本 |
| 中 | 地址修改/删除接口 | 支持地址维护生命周期 |
| 中 | 日志 & 审计 | AOP 记录下单 / 取消 / 估价关键参数 |
| 低 | 统一异常码 | 规范化 code（如 400/401/403/500） |
| 低 | 国际化支持 | message 外置化 |
| 低 | 单元 / 集成测试完善 | 覆盖 PriceCalculation / CampusOrder / EnterpriseOrder |

---
## 9. 版本里程碑与时间线
| 日期 | 里程碑 | 说明 |
|------|--------|------|
| 初版（之前） | 个人下单模块 | 基础 CRUD + JWT 鉴权 |
| 2025-09-28 (上午) | 价格计算草案接入 | 高校订单初步计算逻辑接入 |
| 2025-09-28 (中午) | 高校教材/宿舍接口完成 | DTO + 校验 + 金额 + campusInfo |
| 2025-09-28 (下午) | 地址管理模块上线 | create / list / default + 校验链路打通 |
| 2025-09-28 (下午) | items NOT NULL BUG 修复 | 三层兜底策略完成 |
| 2025-10-11 (晚) | 企业批量/计划接口补充文档 | Swagger 测试说明与字段对齐 |

---
## 10. 附：关键 API 列表（更新）
| 接口 | 方法 | 说明 |
|------|------|------|
| /auth/wechat/login | POST | 获取 token（支持测试 code） |
| /address/create | POST | 创建地址 |
| /address/list | GET | 地址列表 |
| /order/personal | POST | 个人下单 |
| /order/list | GET | 个人订单列表 |
| /order/cancel/{id} | POST | 取消个人订单 |
| /campusOrder/textbook | POST | 高校教材订单创建 |
| /campusOrder/dormitory | POST | 高校宿舍订单创建 |
| /enterprise/orders/bulk | POST | 企业批量回收下单（需企业资质） |
| /enterprise/plans | POST | 创建企业定期回收计划（需企业资质） |

---
> 若本文件与代码存在偏差，请以最新代码为准，并及时回写文档保持一致性。

---

## 11. 技术栈与中间件（汇总）

> 说明：以下为本项目在代码与 `pom.xml` / `application.yml` 中实际使用到的技术与组件清单（以当前仓库为准）。

### 11.1 语言与构建

- Java 17
- Maven（构建与依赖管理）

### 11.2 后端框架与基础能力

- Spring Boot 3.x
- Spring Web（REST API）
- Bean Validation（`spring-boot-starter-validation`，Jakarta Validation）

### 11.3 数据访问与持久化

- MyBatis-Plus（ORM/DAO）
- MySQL Connector/J（数据库驱动）
- HikariCP（默认连接池，随 Spring Boot 引入）

### 11.4 中间件 / 外部依赖

- MySQL 8.x（核心业务数据存储）
- Redis（`spring-boot-starter-data-redis`，用于缓存/会话等，具体视业务使用情况）
- 微信 `jscode2session`（外部 HTTP 接口；项目内置测试模式可本地绕过真实微信调用）

### 11.5 鉴权与安全

- JWT：JJWT（`io.jsonwebtoken:jjwt-*`）

### 11.6 API 文档

- springdoc-openapi（Swagger UI）

### 11.7 JSON 与通用工具

- Jackson（随 Spring Boot，用于序列化/反序列化）
- Fastjson 2（项目内用于部分 JSON 处理，如微信响应解析）
- Lombok（减少样板代码）

### 11.8 文件与静态资源

- 本地文件系统存储（默认目录：`./uploads`，包含分类图片与身份背景图等资源）


# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

System2 是一个 AI 驱动的通用型管理平台，通过元数据驱动和 AI Agent 实现模块的动态注册与自然语言操作。"产业大脑"是其应用场景之一。包含三个独立模块：

- **Spring Boot 后端** — Java 21、Spring Boot 3.2.5、MyBatis-Plus 3.5.5、MySQL
- **Vue 3 前端** — Vite 7、Element Plus、ECharts，TS/JS 混合使用
- **Python AI 服务** — FastAPI、Ollama (gemma4:26b)、SSE 流式传输

## 构建与运行命令

### Spring Boot（`springboot/`）
```bash
mvn clean package          # 构建
mvn spring-boot:run        # 运行（端口 9096）
mvn test                   # 测试（目前无测试文件）
```

### Vue（`vue/`）
```bash
npm install                # 安装依赖
npm run dev                # 开发服务器（Vite）
npm run build              # 生产构建
npm run preview            # 预览生产构建
```
Node 版本要求：`^20.19.0 || >=22.12.0`

### Python AI（`pythonAI/`）
```bash
python main.py             # 运行（端口 8081）
# 或使用 start.bat（会先终止 8081 端口上的已有进程）
```
需要 Ollama 在端口 11434 运行，并加载 `gemma4:26b` 模型。Pydantic 版本为 v1（有升级 v2 的 TODO）。

### 全栈启动顺序
1. MySQL（数据库 `system2`）
2. Spring Boot 后端（端口 9096）
3. Python AI 服务（端口 8081）
4. Vue 开发服务器

## 架构

```
Vue 3 前端  ——HTTP (axios)——>  Spring Boot API  ——JDBC——>  MySQL
     |                              |
     | SSE (原生 fetch)             | HTTP POST (RestTemplate, 120s 超时)
     v                              v
Python FastAPI  <————————————————>  Ollama (Gemma 4 26B)
     |
     | HTTP POST /ai/execute (通用操作执行)
     v
  Spring Boot
```

- Vue 通过 axios 调用 Spring Boot 进行所有 CRUD 操作（`src/utils/request.js`，基础 URL 来自 `VITE_BASE_URL` 环境变量）
- Vue 直接调用 Python 进行 AI 聊天流式传输（`POST /api/v1/algo/chat/stream`，绕过 axios 使用原生 fetch）
- Spring Boot 通过 RestTemplate 代理分析请求到 Python（`POST /api/v1/algo/analyze`）
- Python 回调 Spring Boot 执行 AI 操作：改名用 `PUT /admin/ai-update-name`，通用操作用 `POST /ai/execute`（均排除在登录拦截器之外）

## 模块结构

### Spring Boot（`springboot/src/main/java/com/example/`）
```
controller/         REST 端点（Admin、Department、DictType、DictData、AuditLog、Role 均继承 GenericController；
                    AiController 代理 AI 请求；ModuleController 暴露模块元数据；LoginController、TestController）
service/            服务接口（各业务 Service 均 extends GenericService）
service/impl/       服务实现（均 extends GenericServiceImpl；AiDispatchService 路由 AI 操作 — 构造器注入，BeanUtils.copyProperties）
mapper/             MyBatis-Plus Mapper 接口（BaseMapper + 自定义 XML 查询）
entity/             实体类（Account 基类，Admin 继承 Account；Department、DictType、DictData、AuditLog、Role — 用 @ModuleMeta/@FieldMeta 标注元数据）
dto/                请求 DTO（AdminDto、LoginDto、AiActionDto，含 Jakarta 校验注解）
vo/                 响应 VO（AdminVo — 不含 password）
common/             Result、PageResult、GenericController、GenericService、GenericServiceImpl、
                    ModuleRegistry、ModuleInfo、FieldInfo、OptionItem、Constants、枚举
annotations/        @ModuleMeta（模块元数据：name/title/icon/features）、@FieldMeta（字段元数据：label/searchable/showInTable/showInForm/type/required/dictCode 等）
common/config/      CorsConfig（允许所有来源）
config/             RestTemplateConfig、WebMvcConfig、LoginInterceptor、PasswordEncoderConfig、MyBatisPlusConfig
common/JwtUtil      JWT 工具类（jjwt 0.12.5，HMAC-SHA）
common/AuditAspect  AOP 审计切面（拦截 GenericController 的 add/update/delete/batchDelete 操作，记录到 sys_audit_log）
exception/          CustomException、GlobalExceptionHandler（@ControllerAdvice）
```
资源文件：`resources/mapper/AdminMapper.xml`（自定义 SQL）、`resources/application.yml`

### Vue（`vue/src/`）
```
views/Manager.vue           管理后台布局（侧边栏菜单、面包屑、用户下拉菜单）
views/manager/Home.vue      仪表盘（ECharts、统计、活动动态）
views/manager/Admin.vue     管理员 CRUD 表格（搜索/筛选/弹窗）
views/manager/GenericCrud.vue  通用 CRUD 组件（根据路由参数动态渲染模块）
views/manager/AIchat.vue    三栏式 AI 聊天（历史记录、SSE 流式、Agent 状态监控）
views/Login.vue             登录页（表单校验、token 存储）
views/Front.vue             公共页头部
views/404.vue               动画 404 页面
api/manager/admin.ts        管理员 CRUD 接口函数
api/manager/chat.ts         聊天接口（代理 + 直连 SSE + 确认）
api/manager/login.ts        登录/登出接口
api/manager/module.ts       模块元数据接口（fetchModuleList、fetchModuleInfo）
config/modules.ts           模块配置注册表（从后端拉取模块元数据，通过 moduleConverter 转换，支持 localOverrides 覆盖）
types/manager/admin.ts      TypeScript 接口定义（Result、PageResult、AdminVo、AdminDto）
types/module.ts             模块类型定义（ModuleConfig、ColumnDef、FieldDef、ModuleFeatures、FieldInfoDto、ModuleInfoDto）
utils/request.js            Axios 实例（含请求/响应拦截器，token 注入，401 重定向）
utils/crudApi.ts            通用 CRUD API 工厂（createCrudApi 生成标准 CRUD 方法）
utils/moduleConverter.ts    后端 ModuleInfoDto → 前端 ModuleConfig 转换器（根据 showInTable/showInForm/searchable 映射列/表单/搜索字段）
utils/markdown.js           轻量级 Markdown 转 HTML 工具
assets/css/index.scss       SCSS 主题（主色：#5f56e7）
```
路由：`src/router/index.js` — 路由前缀 `/manager/*` 和 `/front/*`，含 `/manager/:module` 动态路由
环境配置：`.env.development`（localhost:9096）、`.env.production`（生产 IP）

### Python AI（`pythonAI/app/`）
```
core/config.py              Ollama 模型（gemma4:latest）、基础 URL、温度、Java 服务地址
core/llm_engine.py          LLM 交互：agent decision（工具选择 JSON）、free chat（流式/非流式）
core/tool_registry.py       工具注册表（7 个工具：create_entity/query_entity/update_entity_field/update_entity_status/delete_entity/analyze_data/chat）
core/java_client.py         HTTP 客户端（POST 到 Java /ai/execute 执行通用操作）
core/module_metadata.py     启动时从 Java /module/list 拉取模块元数据缓存，生成 LLM 系统提示中的实体/字段描述
core/logging_config.py      日志配置（抑制 httpx/httpcore/uvicorn.access 噪音）
schemas/analyzer.py         Pydantic v1 模型（ChartPoint、CompanyCard、AIAnalysisOutput、JavaInputRequest 等）
api/Management/manager/
  AIchat.py                 路由：/analyze、/chat/stream（三级路由：正则快速路径 → 启发式 → LLM 工具选择）
  handlers/
    analyze_handler.py      分析处理器（透传 analysis_data 为 CHART_DATA）
    chat_handler.py         聊天处理器
    update_name_handler.py  改名处理器（回调 Java PUT /admin/ai-update-name）
```

## 核心模式

- **元数据驱动的模块系统**：实体类用 `@ModuleMeta` 和 `@FieldMeta` 注解标注，`ModuleRegistry` 启动时自动扫描注册，解析 `dictCode` 从字典表加载选项。前端通过 `/module/list` 获取元数据，`moduleConverter.ts` 转换为 `ModuleConfig`（列/搜索/表单字段），`GenericCrud.vue` 根据路由参数动态渲染 CRUD 界面。Python 端 `module_metadata.py` 也拉取同一份元数据注入 LLM 系统提示。新增业务模块只需：(1) 创建带注解的实体，(2) 继承 GenericController/GenericService/ServiceImpl，(3) 在 `modules.ts` 添加可选的 localOverrides。
- **字典系统**：`DictType`/`DictData` 表存储键值对选项。实体字段的 `@FieldMeta(dictCode="xxx")` 引用字典类型，`ModuleRegistry` 启动时自动关联并加载为 `OptionItem` 列表，前端 select 字段自动渲染为下拉选项。
- **模板方法模式**：`GenericServiceImpl` 提供默认 CRUD，子类可覆盖 `beforeAdd`、`afterAdd`、`beforeUpdate`、`buildKeywordCondition`、`toEntity`、`toVo` 等钩子方法。
- **统一响应封装**：所有 Spring Boot 端点返回 `Result<T>`（code 为 String 类型，如 "200"、"A001"）。分页响应用 `PageResult<T>`。
- **实体分层**：`entity/`（数据库模型）→ `dto/`（请求）→ `vo/`（响应）。`BeanUtils.copyProperties` 做层间转换。
- **MyBatis-Plus**：继承 `BaseMapper<T>` 获得标准 CRUD，复杂查询使用 `resources/mapper/AdminMapper.xml` 自定义 XML。`MyBatisPlusConfig` 配置自动填充 createTime/updateTime。
- **逻辑删除**：`sys_admin.deleted` 字段（0/1），MyBatis-Plus `@TableLogic` 自动处理。
- **认证拦截器**：`LoginInterceptor` 校验 `Authorization: Bearer <token>` 头，通过 `JwtUtil` 解析。排除路径：`/login`、`/test/**`、`/admin/ai-update-name`（Python 回调用）、`/ai/execute`（AI 操作执行）、`/module/**`（模块元数据）。OPTIONS 预检请求直接放行。
- **AI 工具注册表**：Python 端使用声明式工具定义（`tool_registry.py`），每个工具有描述、参数 schema 和示例，注入 LLM 系统提示。LLM 返回结构化 JSON 选择工具和参数。三级路由：(1) 正则快速路径匹配改名指令，(2) 启发式快速路径（≤15 字符无关键词直接 CHAT），(3) LLM 工具选择。
- **AI 操作分发**：Spring Boot `AiDispatchService` 根据 entityType 路由 AI 操作（update_field/update_status/delete）到对应的 `GenericService`。Python 通过 `POST /ai/execute` 调用。
- **SSE 流式传输**：Python 使用 `StreamingResponse` 配合 `text/event-stream`。事件格式：`{"type":"start"|"token"|"json"|"error", ...}`，以 `data: [DONE]` 结束。
- **审计日志**：`AuditAspect` 通过 AOP `@Around` 拦截 GenericController 的 add/update/delete/batchDelete 方法，自动记录操作到 `sys_audit_log` 表。
- **角色权限控制**：`sys_role.module_perms` 存储逗号分隔的模块名列表。前端 `Manager.vue` 根据用户角色的 `modulePerms` 过滤侧边栏菜单项。

## 数据库

- MySQL 数据库名：`system2`
- 主表：`sys_admin`（id、admin_code、username、password、real_name、avatar、gender、mobile、email、status、dept_id、role_ids、remark、create_time、update_time、deleted）
- `admin_code` 是唯一业务主键（非 `id`）
- `role_ids` 存储为逗号分隔字符串（如 "1,2,3"），DTO 中为 `List<Long>`
- `sys_department` — 部门表
- `sys_role` — 角色表（`module_perms` 存储逗号分隔的模块名，控制菜单可见性）
- `sys_dict_type` / `sys_dict_data` — 字典系统（字段 @FieldMeta 的 dictCode 引用字典类型，ModuleRegistry 启动时加载选项）
- `sys_audit_log` — 审计日志（AuditAspect AOP 自动记录 CRUD 操作）
- DDL 和种子数据见 `sql/phase3_tables.sql`

## 端口一览

| 服务          | 端口 |
|---------------|------|
| Spring Boot   | 9096 |
| Python AI     | 8081 |
| MySQL         | 3306 |
| Ollama        | 11434 |

## 备注

- Spring Boot（`CorsConfig.java`）和 FastAPI（`CORSMiddleware`）均完全开放 CORS。
- `vue/` 和 `pythonAI/` 目录各有独立的 `.git` 文件夹，是独立仓库。
- Vue 中 TS/JS 混用：API 文件和类型定义使用 `.ts`，路由和工具函数使用 `.js`，视图组件使用 `<script setup lang="ts">`。无 tsconfig.json。
- `LoginController` 直接注入 `AdminMapper` 绕过 Service 层，与其余架构不一致。
- `Admin extends Account` 但重新声明了几乎所有父类字段并覆盖 getter/setter，实际未受益于继承。
- `AiDispatchService.executeUpdateField` 尚未完全实现（返回占位字符串），通用字段反射更新功能不完整。
- 项目无测试文件、无 CI/CD、无 Docker 配置、无代码格式化/lint 配置。

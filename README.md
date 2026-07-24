# System2 - AI 驱动的通用管理平台

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

System2 是一个 AI 驱动的通用型管理平台，通过元数据驱动架构和 AI Agent 实现模块的动态注册与自然语言操作。只需定义实体类并添加注解，即可自动生成完整的 CRUD 界面和 API。

## 项目结构

```
manageSystem01/
├── springboot/     # Java 21 后端 (Spring Boot 3.2.5, MyBatis-Plus, MySQL)
├── vue/            # Vue 3 前端 (Vite 7, Element Plus, ECharts)
├── pythonAI/       # Python AI 微服务 (FastAPI, Ollama, SSE)
├── sql/            # 数据库建表脚本
├── docker-compose.yml
└── .env.example    # 环境变量模板
```

## 架构概览

```
Vue SPA (5174) ──HTTP──→ Spring Boot (9096) ──→ MySQL (3306)
     │
     └──SSE──→ Python AI (8081) ──→ Ollama (11434)
                    └──callback──→ Spring Boot (9096)
```

## 技术栈

| 模块 | 技术 |
|------|------|
| 后端 | Java 21、Spring Boot 3.2.5、MyBatis-Plus 3.5.5、MySQL 8.0 |
| 前端 | Vue 3、Vite 7、Element Plus、ECharts、Pinia |
| AI | FastAPI、Ollama (gemma4)、SSE 流式传输 |
| 部署 | Docker Compose、Nginx |

## 快速启动

### 方式一：Docker Compose（推荐）

```bash
# 1. 复制并填写环境变量
cp .env.example .env
# 编辑 .env 设置数据库密码、JWT 密钥等

# 2. 启动所有服务
docker-compose up -d

# 3. 访问
# 前端: http://localhost
# 后端 API: http://localhost:9096
# Swagger: http://localhost:9096/swagger-ui.html
```

### 方式二：本地开发

#### 前置依赖
- Java 21+
- Node.js 20+
- Python 3.10+
- MySQL 8.0
- Ollama (需安装 gemma4 模型)

#### 1. 数据库
```bash
mysql -u root -p < sql/phase3_tables.sql
```

#### 2. 配置环境变量
```bash
cp .env.example .env
# 编辑 .env 填写实际值
```

#### 3. 启动后端（端口 9096）
```bash
cd springboot
mvn spring-boot:run
```

#### 4. 启动 AI 服务（端口 8081）
```bash
cd pythonAI
pip install -r requirements.txt
python main.py
```

#### 5. 启动前端（端口 5174）
```bash
cd vue
npm install
npm run dev
```

## 核心功能

### 元数据驱动 CRUD
通过 `@ModuleMeta` 和 `@FieldMeta` 注解定义模块，系统自动注册并生成完整的增删改查界面，无需手写 Controller/Service 前端代码。

```java
@ModuleMeta(name = "product", title = "产品管理", icon = "ShoppingCart")
@TableName("sys_product")
public class Product {
    @FieldMeta(label = "产品名称", searchable = true, required = true)
    private String name;

    @FieldMeta(label = "价格", type = "number")
    private BigDecimal price;

    @FieldMeta(label = "状态", type = "select", options = "上架:1,下架:0")
    private Integer status;
}
```

### AI 自然语言操作
用户可通过自然语言与系统交互，AI Agent 自动解析意图并执行对应操作：
- 查询数据：「查看本月新增的部门」
- 修改数据：「把张三的状态改为禁用」
- 数据分析：「分析各部门人数分布」
- 批量操作：「批量删除测试数据」

### 其他特性
- **RBAC 权限控制** - 基于角色的模块级权限管理
- **审计日志** - 自动记录所有关键操作（AOP 拦截）
- **回收站** - 逻辑删除 + 回收站恢复机制
- **数据字典** - 统一管理下拉选项
- **工作流引擎** - 支持自定义审批流程
- **仪表盘** - ECharts 数据可视化
- **文件管理** - 文件上传/下载/预览
- **数据导出** - Excel 导入导出（EasyExcel）
- **备份恢复** - 数据库备份与一键恢复

## 端口一览

| 服务 | 端口 | 说明 |
|------|------|------|
| Vue 前端 | 5174 (开发) / 80 (Docker) | Vite 开发服务器 / Nginx |
| Spring Boot | 9096 | REST API + WebSocket |
| Python AI | 8081 | AI 微服务 (SSE) |
| MySQL | 3306 | 数据库 |
| Ollama | 11434 | LLM 推理服务 |

## API 文档

启动后端后访问 Swagger UI：http://localhost:9096/swagger-ui.html

## 环境变量

参见 `.env.example` 了解所有可配置项。关键变量：

| 变量 | 说明 | 必填 |
|------|------|------|
| `DB_PASSWORD` | 数据库密码 | 是 |
| `JWT_SECRET` | JWT 签名密钥（32+ 字符） | 是 |
| `AI_CALLBACK_SECRET` | AI 服务回调密钥 | 是 |
| `CORS_ALLOWED_ORIGINS` | CORS 允许的源（逗号分隔） | 否 |
| `OLLAMA_MODEL` | Ollama 模型名 | 否 |

## License

MIT

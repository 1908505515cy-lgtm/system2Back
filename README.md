# System2 - AI 驱动的通用管理平台

System2 是一个 AI 驱动的通用型管理平台，通过元数据驱动和 AI Agent 实现模块的动态注册与自然语言操作。

## 项目结构

```
manageSystem01/
├── springboot/     # Spring Boot 后端 (Java 21)
├── vue/            # Vue 3 前端
├── pythonAI/       # Python AI 服务
└── sql/            # 数据库脚本
```

## 技术栈

| 模块 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2.5、MyBatis-Plus 3.5.5、MySQL |
| 前端 | Vue 3、Vite 7、Element Plus、ECharts |
| AI | FastAPI、Ollama (gemma4:26b)、SSE 流式传输 |

## 快速启动

### 1. 数据库
```sql
-- 创建数据库并执行 sql/phase3_tables.sql
```

### 2. Spring Boot 后端 (端口 9096)
```bash
cd springboot
mvn spring-boot:run
```

### 3. Python AI 服务 (端口 8081)
```bash
cd pythonAI
pip install -r requirements.txt
python main.py
```

### 4. Vue 前端
```bash
cd vue
npm install
npm run dev
```

## 功能特性

- **元数据驱动** - 通过注解定义模块，自动生成 CRUD 界面
- **AI 智能操作** - 自然语言交互，支持数据查询、修改、分析
- **权限控制** - 基于角色的模块权限管理
- **审计日志** - 自动记录关键操作

## 端口一览

| 服务 | 端口 |
|------|------|
| Spring Boot | 9096 |
| Python AI | 8081 |
| MySQL | 3306 |
| Ollama | 11434 |

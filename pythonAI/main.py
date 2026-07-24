from app.core.config import settings

from app.core.logging_config import setup_logging
setup_logging(settings.LOG_LEVEL)

import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api.Management.manager.AIchat import router as api_v1_router
from app.core.module_metadata import load_modules
from app.core.auth import verify_api_key

app = FastAPI(title=settings.PROJECT_NAME)


@app.on_event("startup")
async def startup_event():
    """启动时加载模块元数据和同步知识库"""
    load_modules()
    # 异步同步知识库（不阻塞启动）
    try:
        from app.core.rag import sync_documents
        import threading
        threading.Thread(target=sync_documents, daemon=True).start()
    except Exception:
        pass


# CORS 配置（从环境变量读取允许的来源，默认仅允许本地开发）
import os
_cors_origins = os.getenv("CORS_ALLOWED_ORIGINS", "http://localhost:5174")
allow_origins = [o.strip() for o in _cors_origins.split(",") if o.strip()]

app.add_middleware(
    CORSMiddleware,
    allow_origins=allow_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 健康检查端点（Docker HEALTHCHECK 使用）
@app.get("/health")
async def health_check():
    return {"status": "ok"}

# 注册路由。拼接后的完整外发请求路径依旧是：/api/v1/algo/analyze
app.include_router(api_v1_router, prefix="/api/v1/algo")

if __name__ == "__main__":
    # 开启自动热重载，修改代码自动重载
    uvicorn.run("main:app", host="0.0.0.0", port=8081, reload=True)
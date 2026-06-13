from app.core.config import settings

from app.core.logging_config import setup_logging
setup_logging(settings.LOG_LEVEL)

import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api.Management.manager.AIchat import router as api_v1_router
from app.core.module_metadata import load_modules

app = FastAPI(title=settings.PROJECT_NAME)


@app.on_event("startup")
async def startup_event():
    """启动时加载模块元数据"""
    load_modules()


# 全局跨域扫清障碍
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由。拼接后的完整外发请求路径依旧是：/api/v1/algo/analyze
app.include_router(api_v1_router, prefix="/api/v1/algo")

if __name__ == "__main__":
    # 开启自动热重载，修改代码自动重载
    uvicorn.run("main:app", host="127.0.0.1", port=8081, reload=True)
"""
Java 后端通用回调客户端
用于 AI Agent 执行操作时回调 Java 后端
"""
import asyncio
import httpx
import logging
from app.core.config import settings

logger = logging.getLogger(__name__)

JAVA_BASE = settings.JAVA_SERVICE_BASE_URL

# 复用连接的异步客户端
_async_client: httpx.AsyncClient | None = None


def _get_client() -> httpx.AsyncClient:
    global _async_client
    if _async_client is None or _async_client.is_closed:
        _async_client = httpx.AsyncClient(base_url=JAVA_BASE, timeout=10.0)
    return _async_client


async def execute_on_java_async(entity_type: str, action: str, params: dict) -> dict:
    """异步版本：用于 async 端点"""
    payload = {
        "entityType": entity_type,
        "action": action,
        "params": params
    }
    try:
        client = _get_client()
        response = await client.post("/ai/execute", json=payload)
        response.raise_for_status()
        return response.json()
    except httpx.HTTPStatusError as e:
        logger.error("Java 回调 HTTP 错误: %s %s", e.response.status_code, e.response.text[:200])
        return {"code": e.response.status_code, "msg": f"Java 接口返回错误: {e.response.status_code}"}
    except Exception as e:
        logger.error("Java 回调失败: %s", str(e))
        return {"code": 500, "msg": f"Java 回调失败: {str(e)}"}


def execute_on_java(entity_type: str, action: str, params: dict) -> dict:
    """同步版本：用于同步上下文（如 SSE 流式生成器）"""
    try:
        with httpx.Client(base_url=JAVA_BASE, timeout=10.0) as client:
            response = client.post("/ai/execute", json={
                "entityType": entity_type,
                "action": action,
                "params": params
            })
            response.raise_for_status()
            return response.json()
    except httpx.HTTPStatusError as e:
        logger.error("Java 回调 HTTP 错误: %s %s", e.response.status_code, e.response.text[:200])
        return {"code": e.response.status_code, "msg": f"Java 接口返回错误: {e.response.status_code}"}
    except Exception as e:
        logger.error("Java 回调失败: %s", str(e))
        return {"code": 500, "msg": f"Java 回调失败: {str(e)}"}

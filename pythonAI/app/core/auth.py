import hmac
from fastapi import HTTPException, Security
from fastapi.security import APIKeyHeader
from app.core.config import settings

API_KEY_HEADER = APIKeyHeader(name="X-API-Key")


async def verify_api_key(api_key: str = Security(API_KEY_HEADER)):
    """验证 API Key（常量时间比较，防止时序攻击）"""
    if not settings.AI_CALLBACK_SECRET:
        raise HTTPException(status_code=500, detail="AI_CALLBACK_SECRET 未配置")
    if not hmac.compare_digest(api_key, settings.AI_CALLBACK_SECRET):
        raise HTTPException(status_code=401, detail="API Key 无效")
    return True

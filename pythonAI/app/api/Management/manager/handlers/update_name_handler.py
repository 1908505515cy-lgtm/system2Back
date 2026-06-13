import re
import logging
import httpx
from fastapi import HTTPException
from app.core.config import settings

logger = logging.getLogger(__name__)


def _extract_param(tool_params: dict, candidates: list) -> str:
    """从 tool_params 中按候选键名列表依次查找，返回第一个非空值"""
    for key in candidates:
        val = tool_params.get(key)
        if val is not None and str(val).strip():
            return str(val).strip()
    return ""


def handle_update_name(tool_params: dict) -> dict:
    """
    独立分支处理器：处理大模型识别出来的管理员改名动作
    """
    if not tool_params:
        raise HTTPException(status_code=400, detail="大模型未提取到有效工具参数")

    logger.info("LLM extracted params: %s", tool_params)

    # 兼容大模型可能输出的各种键名变体
    admin_code = _extract_param(tool_params, [
        "adminCode", "admin_code", "admin_id", "adminId",
        "userCode", "user_code", "userId", "user_id",
        "code", "id", "target_code", "targetCode"
    ])
    new_name = _extract_param(tool_params, [
        "newName", "new_name", "name", "adminName", "admin_name",
        "newUsername", "new_username", "userName", "user_name",
        "targetName", "target_name", "updatedName", "updated_name",
        "rename", "value"
    ])

    # 如果标准键名匹配失败，尝试从所有值中智能识别
    if not admin_code or not new_name:
        all_values = [str(v).strip() for v in tool_params.values() if v and str(v).strip()]
        logger.warning("Standard key match failed, fallback values: %s", all_values)

        code_pattern = re.compile(r'(admin|code|id|_\d+)', re.IGNORECASE)
        for v in all_values:
            if not admin_code and code_pattern.search(v):
                admin_code = v
            elif not new_name and not code_pattern.search(v) and len(v) <= 20:
                new_name = v

        if (not admin_code or not new_name) and len(all_values) >= 2:
            if not admin_code:
                admin_code = all_values[0]
            if not new_name:
                new_name = all_values[1]

    if not admin_code or not new_name:
        logger.error("Param extraction failed: admin_code=%s, new_name=%s, raw=%s",
                     admin_code, new_name, tool_params)
        return {"code": 400, "msg": "大模型提取的参数不完整，取消调用", "type": "TEXT_FEEDBACK"}

    java_url = f"{settings.JAVA_SERVICE_BASE_URL}/admin/ai-update-name"
    java_payload = {
        "adminCode": admin_code,
        "newName": new_name
    }

    logger.info("Calling Java update: url=%s, payload=%s", java_url, java_payload)

    try:
        with httpx.Client(timeout=10.0) as client:
            java_resp = client.put(java_url, json=java_payload,
                                   headers={"X-API-Key": settings.AI_CALLBACK_SECRET})

        if java_resp.status_code != 200:
            logger.error("Java returned error: status=%s, body=%s",
                         java_resp.status_code, java_resp.text)
            return {"code": 500, "msg": f"Java 接口返回错误: {java_resp.status_code}", "type": "TEXT_FEEDBACK"}

        resp_data = java_resp.json()
        if resp_data.get("code") == 401:
            logger.error("Java API Key 认证失败")
            return {"code": 401, "msg": "回调认证失败，请检查 AI_CALLBACK_SECRET 配置", "type": "TEXT_FEEDBACK"}

        return {
            "code": 200,
            "msg": f"【AI 智能体物理执行成功】已自动调用后端将编码为 {admin_code} 的用户更名为: {new_name}",
            "type": "TEXT_FEEDBACK"
        }

    except Exception as e:
        logger.exception("Failed to call Java backend")
        return {"code": 500, "msg": f"反向调用中枢失败: {str(e)}", "type": "TEXT_FEEDBACK"}

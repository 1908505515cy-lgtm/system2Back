import re
import json
import logging
from fastapi import APIRouter, HTTPException
from starlette.responses import StreamingResponse
from app.schemas.analyzer import JavaInputRequest
from app.core.llm_engine import get_gemma_agent_decision, chat_with_gemma_stream, chat_with_gemma
from app.core.tool_registry import get_tool_by_name
from app.core.java_client import execute_on_java, execute_on_java_async

from app.api.Management.manager.handlers.analyze_handler import handle_analyze
from app.api.Management.manager.handlers.chat_handler import handle_chat
from app.api.Management.manager.handlers.update_name_handler import handle_update_name


def _build_confirm_response(tool_name: str, description: str, params: dict) -> dict:
    """构建通用确认预览响应"""
    tool = get_tool_by_name(tool_name)
    action = tool["action"] if tool else tool_name
    return {
        "code": 200,
        "type": "CONFIRM_REQUIRED",
        "msg": description,
        "data": {
            "tool": tool_name,
            "action": action,
            "params": params
        }
    }


logger = logging.getLogger(__name__)
router = APIRouter()

# 改名指令的正则快速匹配模式
_UPDATE_NAME_PATTERN = re.compile(
    r'(?:帮我)?(?:把|将)\s*'
    r'(?:管理员|用户|账号)?\s*'
    r'(?P<code>[a-zA-Z0-9_]+)\s*'
    r'(?:的?(?:名字|用户名|登录名|名称|账号名))?\s*'
    r'(?:改(?:成|名(?:叫|为)?)|修改为?|更名为?|重命名为?|变成)\s*'
    r'(?P<name>\S+)',
    re.UNICODE
)

_UPDATE_NAME_PATTERN_ALT = re.compile(
    r'(?:帮我)?(?:把|将)?\s*'
    r'(?:管理员|用户|账号)?\s*'
    r'(?P<code>[a-zA-Z0-9_]+)\s*'
    r'(?:改(?:成|名(?:叫|为)?)|修改为?|更名为?|重命名为?)\s*'
    r'(?P<name>\S+)',
    re.UNICODE
)


def _is_obvious_chat(text: str) -> bool:
    """判断是否为明显的闲聊"""
    if len(text) > 15:
        return False
    action_keywords = ['分析', '报告', '数据', '行业', '产业', '技术', '趋势', '市场', '增长',
                       '改名', '改成', '改叫', '修改', '更名', '重命名', '用户名', '登录名',
                       '禁用', '启用', '删除', '查', '列表', '新增', '创建', '添加', '新建',
                       '部门', '管理员', '查询', '搜索', '找']
    return not any(kw in text for kw in action_keywords)


def fast_path_update_name(text: str) -> dict | None:
    """正则快速匹配改名指令"""
    for pattern in [_UPDATE_NAME_PATTERN, _UPDATE_NAME_PATTERN_ALT]:
        m = pattern.search(text)
        if m:
            admin_code = m.group("code").strip()
            new_name = m.group("name").strip().strip("'\"""''")
            if admin_code and new_name:
                logger.info("Fast path: adminCode=%s, newName=%s", admin_code, new_name)
                return {
                    "tool": "update_entity_field",
                    "params": {
                        "entity_type": "admin",
                        "target_id": admin_code,
                        "field_name": "username",
                        "new_value": new_name
                    }
                }
    return None


def _dispatch_tool(tool_name: str, params: dict, raw_text: str) -> dict:
    """根据工具名分发到对应处理器"""
    tool = get_tool_by_name(tool_name) if tool_name else None

    if tool_name == "analyze_data" or (tool and tool["action"] == "analyze"):
        analysis_data = params.get("analysis_data") or params
        return handle_analyze(analysis_data)

    elif tool_name == "chat" or tool_name is None:
        return handle_chat(raw_text)

    elif tool_name == "query_entity":
        return _execute_query(params)

    elif tool and tool.get("requires_confirm"):
        description = _build_description(tool_name, params)
        return _build_confirm_response(tool_name, description, params)

    else:
        return handle_chat(raw_text)


def _execute_query(params: dict) -> dict:
    """直接执行查询操作"""
    entity_type = params.get("entity_type", "")
    fields = params.get("fields", {})
    result = execute_on_java(entity_type, "query", fields)
    return {
        "code": 200,
        "type": "QUERY_RESULT",
        "msg": result.get("msg", result.get("data", "查询完成")),
        "data": result
    }


def _build_description(tool_name: str, params: dict) -> str:
    """根据工具和参数生成人类可读的操作描述"""
    entity_type = params.get("entity_type", "实体")
    target_id = params.get("target_id", "")

    if tool_name == "update_entity_field":
        field_name = params.get("field_name", "")
        new_value = params.get("new_value", "")
        return f"即将把{entity_type} [{target_id}] 的 {field_name} 修改为 [{new_value}]，请确认？"
    elif tool_name == "update_entity_status":
        status = params.get("status", 1)
        status_text = "启用" if status == 1 else "禁用"
        return f"即将{status_text}{entity_type} [{target_id}]，请确认？"
    elif tool_name == "delete_entity":
        return f"即将删除{entity_type} [{target_id}]，请确认？"
    elif tool_name == "create_entity":
        fields = params.get("fields", {})
        fields_str = ", ".join(f"{k}={v}" for k, v in fields.items())
        return f"即将新增{entity_type}：{fields_str}，请确认？"
    else:
        return f"即将执行操作：{tool_name}，参数：{json.dumps(params, ensure_ascii=False)}"


@router.post("/analyze")
async def analyze_industry_data(payload: JavaInputRequest):
    try:
        raw_text = payload.raw_text.strip()

        # 快速路径1：正则匹配改名指令
        fast_result = fast_path_update_name(raw_text)
        if fast_result:
            logger.info("Fast path: UPDATE_NAME")
            params = fast_result["params"]
            description = _build_description("update_entity_field", params)
            return _build_confirm_response("update_entity_field", description, params)

        # 快速路径2：明显闲聊
        if _is_obvious_chat(raw_text):
            logger.info("Fast path: CHAT")
            return handle_chat(raw_text)

        # 常规路径：LLM 工具选择
        agent_result = get_gemma_agent_decision(payload.industry_keyword or "", raw_text)
        tool_name = agent_result.get("tool")
        params = agent_result.get("params", {})
        logger.info("LLM decision: tool=%s, params=%s", tool_name, params)

        return _dispatch_tool(tool_name, params, raw_text)

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


def _sse_event(data: dict) -> str:
    """格式化单条 SSE 事件"""
    return f"data: {json.dumps(data, ensure_ascii=False)}\n\n"


def _stream_generator(raw_text: str):
    """SSE 流式生成器（同步，FastAPI StreamingResponse 支持）"""
    try:
        # 快速路径1：正则匹配改名指令
        fast_result = fast_path_update_name(raw_text)
        if fast_result:
            params = fast_result["params"]
            description = _build_description("update_entity_field", params)
            result = _build_confirm_response("update_entity_field", description, params)
            yield _sse_event({"type": "json", "data": result})
            yield "data: [DONE]\n\n"
            return

        # 快速路径2：明显闲聊
        if _is_obvious_chat(raw_text):
            yield _sse_event({"type": "start", "intent": "CHAT"})
            for token in chat_with_gemma_stream(raw_text):
                yield _sse_event({"type": "token", "content": token})
            yield "data: [DONE]\n\n"
            return

        # 常规路径：LLM 工具选择
        agent_result = get_gemma_agent_decision("", raw_text)
        tool_name = agent_result.get("tool")
        params = agent_result.get("params", {})
        logger.info("Stream LLM decision: tool=%s", tool_name)

        tool = get_tool_by_name(tool_name) if tool_name else None

        if tool_name == "analyze_data" or (tool and tool["action"] == "analyze"):
            analysis_data = params.get("analysis_data") or params
            result = handle_analyze(analysis_data)
            yield _sse_event({"type": "json", "data": result})

        elif tool_name == "query_entity":
            result = asyncio.run(_execute_query(params))
            yield _sse_event({"type": "json", "data": result})

        elif tool and tool.get("requires_confirm"):
            description = _build_description(tool_name, params)
            result = _build_confirm_response(tool_name, description, params)
            yield _sse_event({"type": "json", "data": result})

        else:
            yield _sse_event({"type": "start", "intent": "CHAT"})
            for token in chat_with_gemma_stream(raw_text):
                yield _sse_event({"type": "token", "content": token})

        yield "data: [DONE]\n\n"

    except Exception as e:
        logger.error("Stream error: %s", str(e))
        yield _sse_event({"type": "error", "msg": f"智能体处理异常：{str(e)}"})
        yield "data: [DONE]\n\n"


@router.post("/chat/stream")
async def chat_stream(payload: JavaInputRequest):
    """SSE 流式对话端点"""
    raw_text = (payload.raw_text or "").strip()
    if not raw_text:
        raise HTTPException(status_code=400, detail="raw_text 不能为空")

    return StreamingResponse(
        _stream_generator(raw_text),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        }
    )


@router.post("/execute")
async def execute_action(payload: dict):
    """确认执行端点：前端确认后调用"""
    tool_name = payload.get("tool")
    params = payload.get("params", {})
    tool = get_tool_by_name(tool_name) if tool_name else None
    action = tool["action"] if tool else tool_name

    entity_type = params.get("entity_type", "")

    if tool_name == "update_entity_field":
        return handle_update_name(params)

    elif action in ("create", "update_field", "update_status", "delete"):
        exec_params = {k: v for k, v in params.items() if k != "entity_type"}
        result = await execute_on_java_async(entity_type, action, exec_params)
        return result

    else:
        raise HTTPException(status_code=400, detail=f"不支持的工具: {tool_name}")

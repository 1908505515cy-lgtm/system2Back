import json
import logging
import ollama
from app.core.config import settings
from app.core.tool_registry import get_tools_description, get_output_format_examples
from app.core.module_metadata import get_modules_description

logger = logging.getLogger(__name__)

# 创建带超时的 Ollama 客户端
_ollama_client = ollama.Client(host=settings.OLLAMA_BASE_URL, timeout=120)


def get_gemma_agent_decision(keyword: str, text: str) -> dict:
    """
    让本地大模型扮演 Agent 控制器，基于工具注册表选择合适的工具
    """
    tools_desc = get_tools_description()
    format_examples = get_output_format_examples()
    modules_desc = get_modules_description()

    system_prompt = (
        "你是一个智能管理平台的 Agent 控制器。\n"
        "你的职责是阅读用户输入，选择最合适的工具并提取参数。\n\n"
        "【可用工具列表】\n"
        f"{tools_desc}\n"
        f"{modules_desc}\n\n"
        "【铁律一】：你必须且只能输出合法的纯 JSON 字符串，禁止任何自然语言解释！\n"
        "【铁律二】：params 中的所有值必须从用户原文中逐字提取，严禁翻译、改写、替换、编造！\n"
        "【铁律三】：entity_type 必须使用上述实体类型列表中的英文标识符。\n"
        "【铁律四】：如果用户输入不属于任何工具操作，使用 chat 工具。\n"
    )

    user_prompt = (
        f"上下文提示：{keyword}\n"
        f"用户输入：{text}\n\n"
        "请严格按以下 JSON 格式响应（选择一个工具）：\n"
        f"{format_examples}"
    )

    response = _ollama_client.chat(
        model=settings.OLLAMA_MODEL,
        messages=[
            {'role': 'system', 'content': system_prompt},
            {'role': 'user', 'content': user_prompt}
        ],
        options={
            'temperature': settings.MODEL_TEMPERATURE,
            'num_ctx': settings.MODEL_NUM_CTX
        },
        format='json'
    )

    raw_content = response['message']['content'].strip()
    logger.info("Ollama response: %s", raw_content[:200])

    # 剥离 markdown 代码块（仅处理 ```json ... ``` 包裹的情况）
    if "```" in raw_content:
        import re
        match = re.search(r'```(?:json)?\s*\n?(.*?)\n?\s*```', raw_content, re.DOTALL)
        if match:
            raw_content = match.group(1).strip()
        else:
            raw_content = raw_content.split("```")[1].strip()
            if raw_content.startswith("json"):
                raw_content = raw_content[4:].strip()

    try:
        parsed = json.loads(raw_content)
    except json.JSONDecodeError as e:
        logger.error("LLM 返回非法 JSON: %s, raw: %s", str(e), raw_content[:500])
        return {"tool": "chat", "params": {}, "_parse_error": True}

    # 兼容旧格式（intent-based）和新格式（tool-based）
    if "intent" in parsed and "tool" not in parsed:
        parsed = _convert_legacy_format(parsed)

    # 验证必要字段
    if "tool" not in parsed:
        logger.warning("LLM 响应缺少 tool 字段: %s", parsed)
        return {"tool": "chat", "params": {}, "_parse_error": True}

    return parsed


def _convert_legacy_format(parsed: dict) -> dict:
    """将旧的 intent 格式转换为新的 tool 格式"""
    intent = parsed.get("intent", "")
    tool_params = parsed.get("tool_params")
    analysis_data = parsed.get("analysis_data")

    if intent == "ANALYZE":
        return {
            "tool": "analyze_data",
            "params": {"analysis_data": analysis_data}
        }
    elif intent == "UPDATE_NAME" and tool_params:
        return {
            "tool": "update_entity_field",
            "params": {
                "entity_type": "admin",
                "target_id": tool_params.get("adminCode", ""),
                "field_name": "username",
                "new_value": tool_params.get("newName", "")
            }
        }
    else:
        return {"tool": "chat", "params": {}}


def chat_with_gemma(user_text: str) -> str:
    """自由对话模式"""
    response = _ollama_client.chat(
        model=settings.OLLAMA_MODEL,
        messages=[
            {'role': 'system', 'content': (
                '你是一个友好的 AI 助手，请用简洁清晰的中文回答用户的问题。\n'
                '【格式要求】请使用 Markdown 格式组织回答：\n'
                '- 用 ## 作为小标题分隔不同段落\n'
                '- 用 **加粗** 强调关键信息\n'
                '- 用 - 列出要点\n'
                '- 段落之间用空行分隔\n'
                '- 回答要精炼，避免冗长'
            )},
            {'role': 'user', 'content': user_text}
        ],
        options={
            'temperature': 0.7,
            'num_ctx': settings.MODEL_NUM_CTX
        }
    )
    return response['message']['content'].strip()


def chat_with_gemma_stream(user_text: str):
    """流式对话模式"""
    response = _ollama_client.chat(
        model=settings.OLLAMA_MODEL,
        messages=[
            {'role': 'system', 'content': (
                '你是一个友好的 AI 助手，请用简洁清晰的中文回答用户的问题。\n'
                '【格式要求】请使用 Markdown 格式组织回答：\n'
                '- 用 ## 作为小标题分隔不同段落\n'
                '- 用 **加粗** 强调关键信息\n'
                '- 用 - 列出要点\n'
                '- 段落之间用空行分隔\n'
                '- 回答要精炼，避免冗长'
            )},
            {'role': 'user', 'content': user_text}
        ],
        options={
            'temperature': 0.7,
            'num_ctx': settings.MODEL_NUM_CTX
        },
        stream=True
    )
    for chunk in response:
        token = chunk['message']['content']
        if token:
            yield token

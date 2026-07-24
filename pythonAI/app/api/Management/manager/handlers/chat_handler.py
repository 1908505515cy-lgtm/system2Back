from app.core.llm_engine import chat_with_gemma


def handle_chat(raw_text: str, history: list = None) -> dict:
    """
    自由对话处理器：将用户输入直接发给大模型，返回自然语言回答
    支持传入对话历史以维持上下文
    """
    reply = chat_with_gemma(raw_text, history)
    return {
        "code": 200,
        "msg": reply,
        "type": "CHAT"
    }

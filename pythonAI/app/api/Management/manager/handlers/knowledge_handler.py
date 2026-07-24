"""
知识库查询处理器
从 ChromaDB 检索相关文档，结合 LLM 生成回答
"""
import logging
from app.core.rag import search_knowledge, get_knowledge_context
from app.core.llm_engine import chat_with_gemma

logger = logging.getLogger(__name__)


def handle_query_knowledge(params: dict, history: list = None) -> dict:
    """
    知识库问答
    1. 从知识库检索相关文档
    2. 将文档作为上下文发给 LLM
    3. 返回 LLM 的回答
    """
    query = params.get("query", "")
    if not query:
        return {
            "code": 400,
            "msg": "请提供查询问题",
            "type": "ERROR"
        }

    # 检索知识库
    docs = search_knowledge(query, top_k=3)

    if not docs:
        return {
            "code": 200,
            "type": "CHAT",
            "msg": "知识库中暂无相关信息，请尝试其他关键词或联系管理员补充知识库。"
        }

    # 构建带知识库上下文的 prompt
    context = get_knowledge_context(query)
    enhanced_query = (
        f"请根据以下知识库内容回答用户的问题。"
        f"如果知识库中没有相关信息，请如实说明。\n\n"
        f"{context}\n\n"
        f"用户问题：{query}"
    )

    # 调用 LLM 生成回答
    reply = chat_with_gemma(enhanced_query, history)

    return {
        "code": 200,
        "type": "CHAT",
        "msg": reply,
        "data": {
            "sources": [{"title": d.get("title", ""), "docType": d.get("docType", "")} for d in docs]
        }
    }

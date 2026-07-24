"""
RAG 知识库模块
使用 ChromaDB 进行文档向量化和语义检索
"""
import logging
import hashlib
import chromadb
from app.core.config import settings
from app.core.java_client import execute_on_java

logger = logging.getLogger(__name__)

# ChromaDB 客户端（持久化存储）
_chroma_client = None
_collection = None

COLLECTION_NAME = "knowledge_base"


def _get_collection():
    """获取或创建 ChromaDB collection"""
    global _chroma_client, _collection
    if _collection is not None:
        return _collection

    try:
        _chroma_client = chromadb.PersistentClient(path="./chroma_data")
        _collection = _chroma_client.get_or_create_collection(
            name=COLLECTION_NAME,
            metadata={"hnsw:space": "cosine"}
        )
        logger.info("ChromaDB collection 已就绪，当前文档数: %d", _collection.count())
    except Exception as e:
        logger.error("ChromaDB 初始化失败: %s", str(e))
        _collection = None
    return _collection


def sync_documents():
    """
    从 Java 后端同步文档到 ChromaDB
    拉取所有启用的文档，检查向量化状态，新增未处理的文档
    """
    collection = _get_collection()
    if collection is None:
        logger.warning("ChromaDB 不可用，跳过同步")
        return {"synced": 0, "error": "ChromaDB 不可用"}

    try:
        # 从 Java 后端获取所有启用的文档
        result = execute_on_java("document", "query", {"status": 1})
        data = result.get("data", {})
        documents = data.get("records", []) if isinstance(data, dict) else []

        if not documents:
            return {"synced": 0, "message": "没有待处理的文档"}

        synced = 0
        for doc in documents:
            doc_id = str(doc.get("id", ""))
            title = doc.get("title", "")
            content = doc.get("content", "")
            doc_type = doc.get("docType", "other")

            if not doc_id or not content:
                continue

            # 检查是否已存在
            existing = collection.get(ids=[doc_id])
            if existing and existing["ids"]:
                continue

            # 分块处理（每块约 500 字符）
            chunks = _split_text(content, max_length=500)
            for i, chunk in enumerate(chunks):
                chunk_id = f"{doc_id}_{i}"
                collection.add(
                    ids=[chunk_id],
                    documents=[chunk],
                    metadatas=[{
                        "doc_id": doc_id,
                        "title": title,
                        "doc_type": doc_type,
                        "chunk_index": i,
                    }]
                )
            synced += 1

            # 更新 Java 后端的向量化状态
            try:
                execute_on_java("document", "update_field", {
                    "id": int(doc_id),
                    "fieldName": "vectorStatus",
                    "value": 1
                })
            except Exception as e:
                logger.debug("更新文档 %s 向量化状态失败: %s", doc_id, str(e))

        logger.info("知识库同步完成，新增 %d 篇文档", synced)
        return {"synced": synced, "total": len(documents)}

    except Exception as e:
        logger.error("知识库同步失败: %s", str(e))
        return {"synced": 0, "error": str(e)}


def search_knowledge(query: str, top_k: int = 3) -> list[dict]:
    """
    语义检索知识库
    返回最相关的文档片段
    """
    collection = _get_collection()
    if collection is None or collection.count() == 0:
        return []

    try:
        results = collection.query(
            query_texts=[query],
            n_results=min(top_k, collection.count())
        )

        documents = []
        if results and results["documents"] and results["metadatas"]:
            for doc, meta in zip(results["documents"][0], results["metadatas"][0]):
                documents.append({
                    "content": doc,
                    "title": meta.get("title", ""),
                    "docType": meta.get("docType", ""),
                    "docId": meta.get("doc_id", ""),
                })
        return documents

    except Exception as e:
        logger.error("知识库检索失败: %s", str(e))
        return []


def get_knowledge_context(query: str) -> str:
    """
    获取知识库上下文（用于注入 LLM prompt）
    返回格式化的相关文档片段
    """
    docs = search_knowledge(query, top_k=3)
    if not docs:
        return ""

    lines = ["【知识库参考】"]
    for i, doc in enumerate(docs, 1):
        title = doc.get("title", "未知")
        content = doc.get("content", "")[:300]
        lines.append(f"{i}. [{title}] {content}")

    return "\n".join(lines)


def _split_text(text: str, max_length: int = 500) -> list[str]:
    """将长文本按段落分割为 chunks"""
    if len(text) <= max_length:
        return [text]

    chunks = []
    paragraphs = text.split("\n")
    current = ""

    for para in paragraphs:
        para = para.strip()
        if not para:
            continue
        if len(current) + len(para) + 1 > max_length:
            if current:
                chunks.append(current)
            current = para
        else:
            current = current + "\n" + para if current else para

    if current:
        chunks.append(current)

    return chunks if chunks else [text[:max_length]]

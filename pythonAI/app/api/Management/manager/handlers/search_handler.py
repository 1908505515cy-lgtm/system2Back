"""
智能搜索处理器
跨模块搜索实体记录
"""
import logging
from app.core.java_client import execute_on_java
from app.core.module_metadata import get_modules_list

logger = logging.getLogger(__name__)


def handle_smart_search(params: dict) -> dict:
    """
    跨模块智能搜索
    参数：
        keyword: 搜索关键词
    遍历所有模块，用关键词搜索，汇总结果
    """
    keyword = params.get("keyword", "")
    if not keyword:
        return {
            "code": 400,
            "msg": "请提供搜索关键词",
            "type": "ERROR"
        }

    modules = get_modules_list()
    if not modules:
        return {
            "code": 500,
            "msg": "模块元数据未加载",
            "type": "ERROR"
        }

    results = []
    for mod in modules:
        module_name = mod.get("name", "")
        module_title = mod.get("title", module_name)
        try:
            result = execute_on_java(module_name, "query", {"keyword": keyword})
            data = result.get("data", {})
            records = data.get("records", []) if isinstance(data, dict) else []
            if records:
                for record in records[:3]:  # 每个模块最多返回3条
                    results.append({
                        "module": module_name,
                        "moduleTitle": module_title,
                        "data": record
                    })
        except Exception as e:
            logger.debug("搜索模块 %s 失败: %s", module_name, str(e))

    return {
        "code": 200,
        "type": "SEARCH_RESULT",
        "msg": f"找到 {len(results)} 条相关记录",
        "data": {
            "keyword": keyword,
            "total": len(results),
            "results": results
        }
    }

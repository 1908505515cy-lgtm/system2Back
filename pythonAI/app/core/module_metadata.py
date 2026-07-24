"""
模块元数据客户端
启动时从 Java 后端拉取所有模块的字段信息，注入 LLM 系统提示
"""
import httpx
import logging
from app.core.config import settings

logger = logging.getLogger(__name__)

JAVA_BASE = settings.JAVA_SERVICE_BASE_URL

# 缓存的模块元数据
_modules_cache: list[dict] = []
_loaded = False


def load_modules() -> list[dict]:
    """从 Java 后端加载所有模块元数据"""
    global _modules_cache, _loaded
    if _loaded:
        return _modules_cache

    try:
        with httpx.Client(base_url=JAVA_BASE, timeout=5.0) as client:
            resp = client.get("/module/list")
            resp.raise_for_status()
            data = resp.json()
            if data.get("code") == 200 or data.get("code") == "200":
                _modules_cache = data.get("data", [])
                logger.info("已加载 %d 个模块元数据", len(_modules_cache))
            else:
                logger.warning("加载模块元数据失败: %s", data.get("msg"))
    except Exception as e:
        logger.warning("无法连接 Java 后端获取模块元数据: %s", str(e))

    _loaded = True
    return _modules_cache


def get_modules_description() -> str:
    """生成模块描述文本，用于注入 LLM 系统 prompt"""
    modules = load_modules()
    if not modules:
        return ""

    lines = ["【可用实体类型及字段】"]
    for mod in modules:
        name = mod.get("name", "")
        title = mod.get("title", "")
        fields = mod.get("fields", [])

        visible_fields = [
            f for f in fields
            if f.get("showInForm", True) and f.get("prop") not in ("id", "createTime", "updateTime", "deleted")
        ]

        field_desc = ", ".join(
            f"{f.get('prop', '?')}({f.get('label', '?')})" +
            (f"[{f.get('type', 'text')}]" if f.get('type', 'text') != 'text' else "")
            for f in visible_fields
        )
        lines.append(f"- {name}（{title}）: {field_desc}")

    return "\n".join(lines)


def get_modules_list() -> list[dict]:
    """返回已缓存的模块列表（原始数据）"""
    return load_modules()


def refresh_modules():
    """强制刷新模块缓存"""
    global _loaded
    _loaded = False
    load_modules()

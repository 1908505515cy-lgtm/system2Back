"""
工具注册表
定义 AI Agent 可用的所有工具，替代硬编码的 3 意图系统
新增工具只需在此添加定义，无需改框架代码
"""

TOOLS = [
    {
        "name": "create_entity",
        "description": "新增一条实体记录。适用于新增管理员、新增部门等。",
        "action": "create",
        "requires_confirm": True,
        "params_schema": {
            "entity_type": "str, 实体类型，如 admin, department",
            "fields": "dict, 字段名和值的字典，如 {\"username\": \"张三\", \"realName\": \"张三\"}"
        },
        "example": "新增一个管理员，用户名 zhangsan，姓名张三 → entity_type='admin', fields={\"username\": \"zhangsan\", \"realName\": \"张三\"}"
    },
    {
        "name": "query_entity",
        "description": "查询/搜索实体记录。适用于查找管理员、查看部门列表等。无需确认，直接返回结果。",
        "action": "query",
        "requires_confirm": False,
        "params_schema": {
            "entity_type": "str, 实体类型，如 admin, department",
            "fields": "dict, 查询条件，如 {\"status\": 1} 或 {\"username\": \"zhangsan\"}"
        },
        "example": "查一下有哪些启用的管理员 → entity_type='admin', fields={\"status\": 1}"
    },
    {
        "name": "update_entity_field",
        "description": "修改指定实体的某个字段值。适用于管理员改名、修改邮箱、修改手机号等。",
        "action": "update_field",
        "requires_confirm": True,
        "params_schema": {
            "entity_type": "str, 实体类型，如 admin",
            "target_id": "str or int, 目标记录的 ID 或业务编码（如 admin_01）",
            "field_name": "str, 要修改的字段名（如 username, email, mobile）",
            "new_value": "str, 新值"
        },
        "example": "把 admin_01 的名字改成张三 → entity_type='admin', target_id='admin_01', field_name='username', new_value='张三'"
    },
    {
        "name": "update_entity_status",
        "description": "启用或禁用指定实体。适用于禁用/启用管理员账号等。",
        "action": "update_status",
        "requires_confirm": True,
        "params_schema": {
            "entity_type": "str, 实体类型",
            "target_id": "str or int, 目标记录的 ID 或业务编码",
            "status": "int, 1=启用, 0=禁用"
        },
        "example": "禁用 admin_01 → entity_type='admin', target_id='admin_01', status=0"
    },
    {
        "name": "delete_entity",
        "description": "删除指定实体记录。",
        "action": "delete",
        "requires_confirm": True,
        "params_schema": {
            "entity_type": "str, 实体类型",
            "target_id": "str or int, 目标记录的 ID"
        },
        "example": "删除管理员 5 号 → entity_type='admin', target_id=5"
    },
    {
        "name": "analyze_data",
        "description": "分析行业/产业数据，生成图表、企业卡片、关键指标。适用于用户提交长篇行业报告时。",
        "action": "analyze",
        "requires_confirm": False,
        "params_schema": {
            "keyword": "str, 行业关键词",
            "text": "str, 原始分析文本"
        },
        "example": "分析一下新能源汽车行业 → keyword='新能源汽车', text='...'"
    },
    {
        "name": "generate_chart",
        "description": "根据数据生成可视化图表。适用于用户要求画图、生成柱状图/饼图/折线图等。无需确认，直接返回 ECharts 配置。",
        "action": "generate_chart",
        "requires_confirm": False,
        "params_schema": {
            "chart_type": "str, 图表类型: bar/pie/line/radar",
            "title": "str, 图表标题",
            "data": "list, 数据列表，每项包含 name 和 value"
        },
        "example": "画一个部门人数饼图 → chart_type='pie', title='部门人数分布', data=[{\"name\":\"技术部\",\"value\":15}, ...]"
    },
    {
        "name": "smart_search",
        "description": "跨模块智能搜索。根据关键词在所有模块中搜索相关记录。无需确认。",
        "action": "smart_search",
        "requires_confirm": False,
        "params_schema": {
            "keyword": "str, 搜索关键词"
        },
        "example": "搜索张三 → keyword='张三'"
    },
    {
        "name": "query_knowledge",
        "description": "查询知识库。从知识库中检索相关文档来回答问题。适用于用户提问操作手册、常见问题、政策法规等。",
        "action": "query_knowledge",
        "requires_confirm": False,
        "params_schema": {
            "query": "str, 查询问题"
        },
        "example": "如何重置密码 → query='如何重置密码'"
    },
    {
        "name": "chat",
        "description": "自由对话、闲聊、提问。当用户的输入不属于任何工具操作时使用。",
        "action": "chat",
        "requires_confirm": False,
        "params_schema": {},
        "example": "你好 / 今天天气怎么样 / 你是谁"
    },
]


def get_tools_description() -> str:
    """生成工具描述文本，用于注入 LLM 系统 prompt"""
    lines = []
    for i, tool in enumerate(TOOLS, 1):
        lines.append(f"{i}. 【{tool['name']}】{tool['description']}")
        lines.append(f"   action: {tool['action']}")
        if tool.get("params_schema"):
            params_desc = ", ".join(f"{k}: {v}" for k, v in tool["params_schema"].items())
            lines.append(f"   参数: {{{params_desc}}}")
        if tool.get("example"):
            lines.append(f"   示例: {tool['example']}")
        lines.append("")
    return "\n".join(lines)


def get_tool_by_action(action: str) -> dict | None:
    """根据 action 查找工具定义"""
    for tool in TOOLS:
        if tool["action"] == action:
            return tool
    return None


def get_tool_by_name(name: str) -> dict | None:
    """根据 name 查找工具定义"""
    for tool in TOOLS:
        if tool["name"] == name:
            return tool
    return None


def get_output_format_examples() -> str:
    """生成输出格式示例，用于 LLM prompt"""
    return (
        '【格式：新增实体】\n'
        '{"tool": "create_entity", "params": {"entity_type": "admin", "fields": {"username": "zhangsan", "realName": "张三"}}}\n\n'
        '【格式：查询实体】\n'
        '{"tool": "query_entity", "params": {"entity_type": "admin", "fields": {"status": 1}}}\n\n'
        '【格式：修改字段】\n'
        '{"tool": "update_entity_field", "params": {"entity_type": "admin", "target_id": "admin_01", "field_name": "username", "new_value": "张三"}}\n\n'
        '【格式：修改状态】\n'
        '{"tool": "update_entity_status", "params": {"entity_type": "admin", "target_id": "admin_01", "status": 0}}\n\n'
        '【格式：删除实体】\n'
        '{"tool": "delete_entity", "params": {"entity_type": "admin", "target_id": 5}}\n\n'
        '【格式：分析类】\n'
        '{"tool": "analyze_data", "params": {"keyword": "新能源汽车", "analysis_data": {...}}}\n\n'
        '【格式：生成图表】\n'
        '{"tool": "generate_chart", "params": {"chart_type": "pie", "title": "部门人数分布", "data": [{"name": "技术部", "value": 15}, {"name": "产品部", "value": 8}]}}\n\n'
        '【格式：智能搜索】\n'
        '{"tool": "smart_search", "params": {"keyword": "张三"}}\n\n'
        '【格式：闲聊/无法识别】\n'
        '{"tool": "chat", "params": {}}'
    )

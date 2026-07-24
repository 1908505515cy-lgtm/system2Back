"""
图表生成处理器
根据用户描述的数据生成 ECharts 配置
"""
import logging

logger = logging.getLogger(__name__)


def handle_generate_chart(params: dict) -> dict:
    """
    生成 ECharts 图表配置
    参数：
        chart_type: 图表类型 (bar/pie/line/radar)
        title: 图表标题
        data: 数据列表 [{name, value}, ...]
    """
    chart_type = params.get("chart_type", "bar")
    title = params.get("title", "数据图表")
    data = params.get("data", [])

    if not data:
        return {
            "code": 400,
            "msg": "缺少数据，请提供 data 参数",
            "type": "ERROR"
        }

    # 根据图表类型生成 ECharts 配置
    if chart_type == "pie":
        echarts_config = _build_pie(title, data)
    elif chart_type == "line":
        echarts_config = _build_line(title, data)
    elif chart_type == "radar":
        echarts_config = _build_radar(title, data)
    else:
        echarts_config = _build_bar(title, data)

    return {
        "code": 200,
        "type": "CHART_DATA",
        "msg": f"已生成 {title} 图表",
        "result": echarts_config
    }


def _build_bar(title: str, data: list) -> dict:
    """柱状图配置"""
    categories = [item.get("name", "") for item in data]
    values = [item.get("value", 0) for item in data]
    return {
        "chartType": "bar",
        "title": title,
        "option": {
            "title": {"text": title, "left": "center"},
            "tooltip": {"trigger": "axis"},
            "xAxis": {"type": "category", "data": categories},
            "yAxis": {"type": "value"},
            "series": [{"data": values, "type": "bar", "itemStyle": {"borderRadius": [4, 4, 0, 0]}}]
        }
    }


def _build_pie(title: str, data: list) -> dict:
    """饼图配置"""
    pie_data = [{"name": item.get("name", ""), "value": item.get("value", 0)} for item in data]
    return {
        "chartType": "pie",
        "title": title,
        "option": {
            "title": {"text": title, "left": "center"},
            "tooltip": {"trigger": "item", "formatter": "{b}: {c} ({d}%)"},
            "legend": {"orient": "vertical", "left": "left"},
            "series": [{
                "type": "pie",
                "radius": "55%",
                "data": pie_data,
                "emphasis": {"itemStyle": {"shadowBlur": 10, "shadowOffsetX": 0, "shadowColor": "rgba(0, 0, 0, 0.5)"}}
            }]
        }
    }


def _build_line(title: str, data: list) -> dict:
    """折线图配置"""
    categories = [item.get("name", "") for item in data]
    values = [item.get("value", 0) for item in data]
    return {
        "chartType": "line",
        "title": title,
        "option": {
            "title": {"text": title, "left": "center"},
            "tooltip": {"trigger": "axis"},
            "xAxis": {"type": "category", "data": categories, "boundaryGap": False},
            "yAxis": {"type": "value"},
            "series": [{"data": values, "type": "line", "smooth": True, "areaStyle": {"opacity": 0.3}}]
        }
    }


def _build_radar(title: str, data: list) -> dict:
    """雷达图配置"""
    indicators = [{"name": item.get("name", ""), "max": item.get("max", 100)} for item in data]
    values = [item.get("value", 0) for item in data]
    return {
        "chartType": "radar",
        "title": title,
        "option": {
            "title": {"text": title, "left": "center"},
            "tooltip": {},
            "radar": {"indicator": indicators},
            "series": [{"type": "radar", "data": [{"value": values, "name": title}]}]
        }
    }

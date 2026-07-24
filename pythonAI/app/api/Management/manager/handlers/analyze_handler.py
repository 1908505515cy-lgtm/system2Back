import logging
import re
from typing import Dict, Any, List, Optional
from collections import Counter
from datetime import datetime

logger = logging.getLogger(__name__)


def handle_analyze(analysis_data: dict) -> dict:
    """
    数据分析处理器：支持统计分析、趋势分析、图表生成、多维对比
    """
    try:
        if not analysis_data:
            return {"code": 400, "msg": "分析数据为空", "type": "ERROR", "result": None}

        data_list = analysis_data.get("data", [])
        if not data_list:
            return {"code": 200, "msg": "暂无数据可分析", "type": "EMPTY", "result": None}

        analysis_type = analysis_data.get("type", "auto")

        if analysis_type == "trend":
            date_field = analysis_data.get("date_field")
            result = _trend_analysis(data_list, date_field)
        elif analysis_type == "compare":
            group_field = analysis_data.get("group_field")
            value_field = analysis_data.get("value_field")
            result = _compare_analysis(data_list, group_field, value_field)
        elif analysis_type == "chart":
            chart_type = analysis_data.get("chart_type", "bar")
            label_field = analysis_data.get("label_field")
            value_field = analysis_data.get("value_field")
            result = _chart_data(data_list, chart_type, label_field, value_field)
        else:
            result = _auto_analysis(data_list)

        return {"code": 200, "msg": "分析完成", "type": "CHART_DATA", "result": result}

    except Exception as e:
        logger.error("数据分析失败: %s", str(e))
        return {"code": 500, "msg": f"分析失败: {str(e)}", "type": "ERROR", "result": None}


def _auto_analysis(data_list: List[Dict[str, Any]]) -> Dict[str, Any]:
    """自动分析：统计各字段分布 + 自动生成图表建议"""
    field_stats = _field_statistics(data_list)
    charts = _suggest_charts(data_list, field_stats)
    return {
        "total_count": len(data_list),
        "field_statistics": field_stats,
        "suggested_charts": charts,
    }


def _field_statistics(data_list: List[Dict[str, Any]]) -> Dict[str, Any]:
    """统计各字段的分布情况"""
    result = {}
    all_keys = set()
    for item in data_list:
        all_keys.update(item.keys())

    for key in all_keys:
        values = [item.get(key) for item in data_list if item.get(key) is not None]
        if not values:
            continue

        value_counter = Counter(str(v) for v in values)
        top_values = value_counter.most_common(10)
        field_type = _detect_field_type(values)

        field_stat = {
            "type": field_type,
            "count": len(values),
            "unique_count": len(value_counter),
            "top_values": [{"value": v, "count": c} for v, c in top_values]
        }

        if field_type == "numeric":
            numeric_values = [float(v) for v in values if _is_numeric(v)]
            if numeric_values:
                field_stat["min"] = min(numeric_values)
                field_stat["max"] = max(numeric_values)
                field_stat["avg"] = round(sum(numeric_values) / len(numeric_values), 2)
                field_stat["sum"] = round(sum(numeric_values), 2)

        result[key] = field_stat

    return result


def _suggest_charts(data_list: List[Dict[str, Any]], field_stats: Dict) -> List[Dict]:
    """根据字段类型自动推荐图表"""
    suggestions = []
    for key, stat in field_stats.items():
        if stat["type"] == "text" and 2 <= stat["unique_count"] <= 15:
            suggestions.append({
                "chart_type": "pie",
                "label_field": key,
                "description": f"按 {key} 分布的饼图",
            })
            suggestions.append({
                "chart_type": "bar",
                "label_field": key,
                "description": f"按 {key} 数量的柱状图",
            })
        if stat["type"] == "date":
            suggestions.append({
                "chart_type": "line",
                "date_field": key,
                "description": f"按 {key} 的趋势折线图",
            })
    return suggestions[:5]


def _trend_analysis(data_list: List[Dict[str, Any]], date_field: Optional[str] = None) -> Dict[str, Any]:
    """趋势分析：按日期字段分组统计"""
    if not date_field:
        # 自动查找日期字段
        for item in data_list:
            for k, v in item.items():
                if v and _is_date(str(v)):
                    date_field = k
                    break
            if date_field:
                break

    if not date_field:
        return {"error": "未找到日期字段，无法进行趋势分析"}

    date_counter = Counter()
    for item in data_list:
        raw = item.get(date_field)
        if raw:
            date_str = str(raw)[:10]  # 取 YYYY-MM-DD
            date_counter[date_str] += 1

    sorted_dates = sorted(date_counter.keys())
    return {
        "type": "line",
        "title": f"按 {date_field} 的趋势",
        "labels": sorted_dates,
        "data": [date_counter[d] for d in sorted_dates],
        "date_field": date_field,
    }


def _compare_analysis(data_list: List[Dict[str, Any]], group_field: Optional[str],
                      value_field: Optional[str] = None) -> Dict[str, Any]:
    """多维对比分析"""
    if not group_field:
        # 自动选择分类字段
        for item in data_list:
            for k, v in item.items():
                if v and _detect_field_type([item.get(k) for item in data_list if item.get(k)]) == "text":
                    unique = len(set(str(item.get(k)) for item in data_list if item.get(k)))
                    if 2 <= unique <= 15:
                        group_field = k
                        break
            if group_field:
                break

    if not group_field:
        return {"error": "未找到合适的分类字段"}

    groups = Counter()
    for item in data_list:
        g = str(item.get(group_field, "未知"))
        groups[g] += 1

    sorted_groups = groups.most_common(20)
    return {
        "type": "bar",
        "title": f"按 {group_field} 分组对比",
        "labels": [g[0] for g in sorted_groups],
        "data": [g[1] for g in sorted_groups],
        "group_field": group_field,
    }


def _chart_data(data_list: List[Dict[str, Any]], chart_type: str,
                label_field: Optional[str], value_field: Optional[str]) -> Dict[str, Any]:
    """生成指定类型的图表数据"""
    if chart_type == "pie" or chart_type == "bar":
        if not label_field:
            return {"error": "饼图/柱状图需要指定 label_field"}
        counter = Counter(str(item.get(label_field, "未知")) for item in data_list if item.get(label_field))
        sorted_items = counter.most_common(20)
        return {
            "type": chart_type,
            "title": f"按 {label_field} 分布",
            "labels": [i[0] for i in sorted_items],
            "data": [i[1] for i in sorted_items],
        }

    if chart_type == "line":
        date_field = label_field
        if not date_field:
            return {"error": "折线图需要指定 date_field (label_field)"}
        return _trend_analysis(data_list, date_field)

    return {"error": f"不支持的图表类型: {chart_type}"}


def _detect_field_type(values: List[Any]) -> str:
    """
    检测字段类型
    """
    if not values:
        return "unknown"

    # 检查是否为数值类型
    numeric_count = sum(1 for v in values if _is_numeric(v))
    if numeric_count > len(values) * 0.8:
        return "numeric"

    # 检查是否为日期类型
    date_count = sum(1 for v in values if _is_date(str(v)))
    if date_count > len(values) * 0.8:
        return "date"

    # 检查是否为布尔类型
    bool_values = {"true", "false", "0", "1", "yes", "no"}
    bool_count = sum(1 for v in values if str(v).lower() in bool_values)
    if bool_count > len(values) * 0.8:
        return "boolean"

    return "text"


def _is_numeric(value: Any) -> bool:
    """
    判断是否为数值
    """
    try:
        float(value)
        return True
    except (ValueError, TypeError):
        return False


def _is_date(value: str) -> bool:
    """判断是否为日期格式"""
    date_patterns = [
        r'\d{4}-\d{2}-\d{2}',
        r'\d{4}/\d{2}/\d{2}',
        r'\d{2}-\d{2}-\d{4}',
        r'\d{2}/\d{2}/\d{4}'
    ]
    return any(re.match(pattern, value) for pattern in date_patterns)
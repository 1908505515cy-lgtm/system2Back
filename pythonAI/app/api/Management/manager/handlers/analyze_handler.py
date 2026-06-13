def handle_analyze(analysis_data: dict) -> dict:
    """
    独立分支处理器：处理大模型识别出来的图表技术分析
    """
    return {
        "code": 200,
        "msg": "success",
        "type": "CHART_DATA",
        "result": analysis_data
    }
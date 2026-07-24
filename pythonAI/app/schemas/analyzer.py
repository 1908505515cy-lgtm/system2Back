from pydantic import BaseModel, Field
from typing import List, Optional


class ChartPoint(BaseModel):
    year: str = Field(..., description="年份")
    growth_rate: float = Field(..., description="行业增长率数据")


class CompanyCard(BaseModel):
    name: str = Field(..., description="企业名称")
    code: str = Field(..., description="股票代码")
    value: str = Field(..., description="市值，单位：亿")


class KeyMetric(BaseModel):
    label: str = Field(..., description="指标名称")
    value: str = Field(..., description="指标数值")
    growth: str = Field(..., description="同比增速")


class AIAnalysisOutput(BaseModel):
    chart_data: List[ChartPoint]
    companies: List[CompanyCard]
    key_metrics: List[KeyMetric]


class ChatHistoryItem(BaseModel):
    role: str = Field(..., description="user 或 ai")
    content: str = Field(..., description="消息内容")


class JavaInputRequest(BaseModel):
    industry_keyword: Optional[str] = ""
    raw_text: str = Field(..., description="用户或管理员输入的原始指令/长文本")
    history: Optional[List[ChatHistoryItem]] = Field(default=[], description="对话历史")
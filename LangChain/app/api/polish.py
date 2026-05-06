"""文字润化 API 路由（同步，直接返回结果）"""
from typing import List

from fastapi import APIRouter
from pydantic import BaseModel, Field

from app.chains.polish_chain import polish_text

router = APIRouter()


class ContextItem(BaseModel):
    node_id: str = Field("", description="上游节点 ID")
    label: str = Field("", description="节点名称")
    content: str = Field(..., description="节点内容")


class PolishRequest(BaseModel):
    text: str = Field(..., description="待润化的文本")
    context: List[ContextItem] = Field(default=[], description="上游节点上下文列表")


class PolishResponse(BaseModel):
    polished: str = Field(..., description="润化后的文本")


@router.post("/text", response_model=PolishResponse, summary="文字润化")
def polish(req: PolishRequest):
    ctx = [{"label": c.label, "content": c.content} for c in req.context if c.content]
    result = polish_text(req.text, ctx or None)
    return PolishResponse(polished=result)

"""文字润化 API 路由（同步，直接返回结果）"""
import logging
from typing import List

from fastapi import APIRouter, HTTPException
from openai import RateLimitError, AuthenticationError, APIConnectionError
from pydantic import BaseModel, Field

from app.chains.polish_chain import polish_text

logger = logging.getLogger(__name__)

router = APIRouter()


class ContextItem(BaseModel):
    node_id: str = Field("", description="上游节点 ID")
    label: str = Field("", description="节点名称")
    content: str = Field(..., description="节点内容")


class PolishRequest(BaseModel):
    text: str = Field(..., description="待润化的文本")
    context: List[ContextItem] = Field(default=[], description="上游节点上下文列表")
    api_key: str = Field("", description="模型 API Key（不传则使用服务端默认配置）")
    base_url: str = Field("", description="模型 API Base URL")
    model_name: str = Field("", description="模型名称，如 gpt-4o / deepseek-chat")


class PolishResponse(BaseModel):
    polished: str = Field(..., description="润化后的文本")


@router.post("/text", response_model=PolishResponse, summary="文字润化")
def polish(req: PolishRequest):
    ctx = [{"label": c.label, "content": c.content} for c in req.context if c.content]
    try:
        result = polish_text(
            req.text,
            ctx or None,
            api_key=req.api_key or None,
            base_url=req.base_url or None,
            model_name=req.model_name or None,
        )
    except ValueError as e:
        logger.warning("[polish] 参数错误: %s", e)
        raise HTTPException(status_code=400, detail=str(e))
    except RateLimitError as e:
        logger.warning("[polish] 限流/过载: %s", e)
        raise HTTPException(status_code=429, detail=f"模型服务繁忙，请稍后重试（{e}）")
    except AuthenticationError as e:
        logger.error("[polish] 认证失败: %s", e)
        raise HTTPException(status_code=401, detail="API Key 无效或已过期，请检查模型配置")
    except APIConnectionError as e:
        logger.error("[polish] 连接失败: %s", e)
        raise HTTPException(status_code=503, detail="无法连接到模型服务，请检查 API 地址是否正确")
    except Exception as e:
        logger.exception("[polish] 未预期错误: %s", e)
        raise HTTPException(status_code=500, detail=f"服务内部错误：{type(e).__name__}: {e}")
    return PolishResponse(polished=result)

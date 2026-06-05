"""文字生图 API 路由"""
from typing import Literal, Optional

from fastapi import APIRouter
from pydantic import BaseModel, Field

from app.tasks.task_store import create_task
from app.tasks.worker import run_t2i

router = APIRouter()


class T2IRequest(BaseModel):
    prompt: str = Field(..., description="生成描述（中英文均可）")
    negative_prompt: str = Field("", description="负向提示词，留空由 AI 自动生成")
    model: Literal["dalle3", "flux", "sdxl"] = Field("flux", description="图像模型")
    style: Literal["default", "realistic", "anime", "cyberpunk", "ink", "cinematic"] = Field(
        "default", description="风格预设"
    )
    width: int = Field(1024, ge=512, le=2048, description="图片宽度（px）")
    height: int = Field(1024, ge=512, le=2048, description="图片高度（px）")
    num_images: int = Field(1, ge=1, le=4, description="生成数量")
    enhance_prompt: bool = Field(True, description="是否启用 LangChain 提示词增强")
    llm_api_key: str = Field("", description="提示词增强用的 LLM API Key（空则用 .env 默认值）")
    llm_base_url: str = Field("", description="提示词增强用的 LLM API 地址")
    llm_model_name: str = Field("", description="提示词增强用的 LLM 模型名")
    img_api_key: str = Field("", description="生图模型 API Key（空则用 .env 默认值）")
    img_base_url: str = Field("", description="生图模型 API 地址")
    img_model_name: str = Field("", description="生图模型名（如 doubao-seedream-5-0-260128）")


class TaskResponse(BaseModel):
    task_id: str
    message: str = "任务已提交，请通过 /api/v1/tasks/{task_id} 查询进度"


@router.post("/generate", response_model=TaskResponse, summary="文字生图")
def generate(req: T2IRequest):
    """
    提交文字生图异步任务：
    1. 在 Redis 中创建任务记录（status=pending）
    2. 通过 Celery .delay() 将任务发送到 broker（异步，立即返回）
    3. 返回 task_id，前端通过 GET /api/v1/tasks/{task_id} 轮询进度
    """
    params = req.model_dump()  # 将 Pydantic 模型序列化为 dict，传递给 Celery 任务
    task_id = create_task("t2i", params)
    run_t2i.delay(task_id, params)  # 异步提交到 Celery，不阻塞当前请求
    return TaskResponse(task_id=task_id)

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


class TaskResponse(BaseModel):
    task_id: str
    message: str = "任务已提交，请通过 /api/v1/tasks/{task_id} 查询进度"


@router.post("/generate", response_model=TaskResponse, summary="文字生图")
def generate(req: T2IRequest):
    params = req.model_dump()
    task_id = create_task("t2i", params)
    run_t2i.delay(task_id, params)
    return TaskResponse(task_id=task_id)

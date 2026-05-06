"""图生视频 API 路由"""
from typing import Literal, Optional

from fastapi import APIRouter
from pydantic import BaseModel, Field, model_validator

from app.tasks.task_store import create_task
from app.tasks.worker import run_i2v

router = APIRouter()


class I2VRequest(BaseModel):
    image_url: str = Field("", description="参考图片 URL（与 image_base64 二选一）")
    image_base64: str = Field("", description="参考图片 Base64（与 image_url 二选一）")
    prompt: str = Field("", description='运动描述，如"镜头缓慢向前推进"')
    model: Literal["kling", "wan", "minimax"] = Field("kling", description="图生视频模型")
    duration: int = Field(5, ge=3, le=10, description="视频时长（秒）")
    motion_strength: float = Field(0.5, ge=0.0, le=1.0, description="运动幅度 0~1")
    fps: int = Field(24, description="帧率")

    @model_validator(mode="after")
    def check_image_input(self):
        if not self.image_url and not self.image_base64:
            raise ValueError("必须提供 image_url 或 image_base64")
        return self


class TaskResponse(BaseModel):
    task_id: str
    message: str = "任务已提交，请通过 /api/v1/tasks/{task_id} 查询进度"


@router.post("/generate", response_model=TaskResponse, summary="图生视频")
def generate(req: I2VRequest):
    params = req.model_dump()
    task_id = create_task("i2v", params)
    run_i2v.delay(task_id, params)
    return TaskResponse(task_id=task_id)

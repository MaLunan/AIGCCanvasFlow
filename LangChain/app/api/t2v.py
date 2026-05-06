"""文字生视频 API 路由"""
from typing import Literal, Optional

from fastapi import APIRouter
from pydantic import BaseModel, Field

from app.tasks.task_store import create_task
from app.tasks.worker import run_t2v

router = APIRouter()


class T2VRequest(BaseModel):
    prompt: str = Field(..., description="视频描述（中英文均可）")
    model: Literal["kling", "wan", "minimax"] = Field("kling", description="视频生成模型")
    duration: int = Field(5, ge=3, le=10, description="视频时长（秒）")
    resolution: Literal["480p", "720p", "1080p"] = Field("720p", description="视频分辨率")
    aspect_ratio: Literal["16:9", "9:16", "1:1"] = Field("16:9", description="宽高比")
    enhance_prompt: bool = Field(True, description="是否启用 LangChain 提示词增强")


class ScriptT2VRequest(BaseModel):
    """脚本拆镜 + 批量生成"""
    script: str = Field(..., description="完整视频脚本（自动拆分为分镜）")
    model: Literal["kling", "wan", "minimax"] = Field("kling")
    duration_per_shot: int = Field(5, ge=3, le=10, description="每个分镜时长（秒）")
    aspect_ratio: Literal["16:9", "9:16", "1:1"] = Field("16:9")


class TaskResponse(BaseModel):
    task_id: str
    message: str = "任务已提交，请通过 /api/v1/tasks/{task_id} 查询进度"


class BatchTaskResponse(BaseModel):
    task_ids: list[str]
    shot_count: int
    message: str


@router.post("/generate", response_model=TaskResponse, summary="文字生视频")
def generate(req: T2VRequest):
    params = req.model_dump()
    task_id = create_task("t2v", params)
    run_t2v.delay(task_id, params)
    return TaskResponse(task_id=task_id)


@router.post("/generate/script", response_model=BatchTaskResponse, summary="脚本拆镜批量生成")
def generate_from_script(req: ScriptT2VRequest):
    """将完整脚本拆分为分镜，为每个分镜提交独立的生成任务"""
    from app.chains.script_splitter import split_script

    shots = split_script(req.script, target_duration=req.duration_per_shot)
    task_ids = []
    for shot in shots:
        params = {
            "prompt": shot["prompt"],
            "model": req.model,
            "duration": shot.get("duration", req.duration_per_shot),
            "aspect_ratio": req.aspect_ratio,
            "enhance_prompt": False,  # 脚本拆镜已增强过
        }
        task_id = create_task("t2v", params)
        run_t2v.delay(task_id, params)
        task_ids.append(task_id)

    return BatchTaskResponse(
        task_ids=task_ids,
        shot_count=len(shots),
        message=f"已拆分为 {len(shots)} 个分镜并提交生成",
    )

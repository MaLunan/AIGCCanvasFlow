"""任务状态查询路由"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional

from app.tasks.task_store import get_task

router = APIRouter()


class TaskResult(BaseModel):
    task_id: str
    task_type: str
    status: str             # pending | processing | succeeded | failed
    progress: int           # 0~100
    result_url: Optional[str] = None
    error: Optional[str] = None


@router.get("/{task_id}", response_model=TaskResult, summary="查询任务状态")
def query_task(task_id: str):
    data = get_task(task_id)
    if not data:
        raise HTTPException(status_code=404, detail="任务不存在或已过期")
    return TaskResult(
        task_id=data["task_id"],
        task_type=data.get("task_type", ""),
        status=data["status"],
        progress=data["progress"],
        result_url=data.get("result_url") or None,
        error=data.get("error") or None,
    )

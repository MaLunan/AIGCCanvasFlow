"""Redis 任务状态管理"""
import json
import uuid
from enum import Enum
from typing import Optional

import redis

from app.config import settings

_redis = redis.from_url(settings.redis_url, decode_responses=True)


class TaskStatus(str, Enum):
    PENDING = "pending"
    PROCESSING = "processing"
    SUCCEEDED = "succeeded"
    FAILED = "failed"


def _key(task_id: str) -> str:
    return f"aigc:task:{task_id}"


def create_task(task_type: str, params: dict) -> str:
    """创建任务，写入 Redis，返回 task_id"""
    task_id = str(uuid.uuid4())
    payload = {
        "task_id": task_id,
        "task_type": task_type,
        "status": TaskStatus.PENDING,
        "progress": 0,
        "result_url": "",
        "error": "",
        "params": json.dumps(params),
    }
    _redis.hset(_key(task_id), mapping=payload)
    _redis.expire(_key(task_id), settings.task_expire_seconds)
    return task_id


def get_task(task_id: str) -> Optional[dict]:
    data = _redis.hgetall(_key(task_id))
    if not data:
        return None
    data["progress"] = int(data.get("progress", 0))
    if data.get("params"):
        data["params"] = json.loads(data["params"])
    return data


def update_task(task_id: str, **kwargs):
    """更新任务字段，支持 status / progress / result_url / error"""
    _redis.hset(_key(task_id), mapping={k: str(v) if v is not None else "" for k, v in kwargs.items()})
    _redis.expire(_key(task_id), settings.task_expire_seconds)

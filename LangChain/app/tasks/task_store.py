"""Redis 任务状态管理"""
import json
import uuid
from enum import Enum
from typing import Optional

import redis

from app.config import settings

# 使用同步 Redis 客户端（Celery worker 运行在独立线程中，无需异步）
# decode_responses=True：Redis 返回字符串而非 bytes
_redis = redis.from_url(settings.redis_url, decode_responses=True)


class TaskStatus(str, Enum):
    """任务生命周期状态枚举（继承 str 使其可直接序列化为 JSON）"""
    PENDING = "pending"        # 已提交、等待 Celery worker 领取
    PROCESSING = "processing"  # worker 正在执行
    SUCCEEDED = "succeeded"    # 执行成功，result_url 有值
    FAILED = "failed"          # 执行失败，error 有值


def _key(task_id: str) -> str:
    """构造 Redis Hash key，格式：aigc:task:{uuid}"""
    return f"aigc:task:{task_id}"


def create_task(task_type: str, params: dict) -> str:
    """
    创建新任务：生成 UUID 作为 task_id，以 Redis Hash 存储初始状态。
    params 序列化为 JSON 字符串存储，避免 Redis Hash 嵌套。
    设置 TTL = task_expire_seconds（默认 24h），到期自动清理。
    """
    task_id = str(uuid.uuid4())
    payload = {
        "task_id": task_id,
        "task_type": task_type,
        "status": TaskStatus.PENDING,
        "progress": 0,
        "result_url": "",
        "error": "",
        "params": json.dumps(params),  # 嵌套 dict 需手动 JSON 序列化
    }
    _redis.hset(_key(task_id), mapping=payload)
    _redis.expire(_key(task_id), settings.task_expire_seconds)
    return task_id


def get_task(task_id: str) -> Optional[dict]:
    """
    读取任务完整状态。
    - hgetall 返回空 dict 时说明 key 不存在或已过期，返回 None
    - progress 强制转 int（Redis 存的是字符串）
    - params 反序列化回 dict
    """
    data = _redis.hgetall(_key(task_id))
    if not data:
        return None
    data["progress"] = int(data.get("progress", 0))
    if data.get("params"):
        data["params"] = json.loads(data["params"])
    return data


def update_task(task_id: str, **kwargs):
    """
    增量更新任务字段（支持 status / progress / result_url / error）。
    所有值统一转为字符串（Redis Hash 值必须为字符串），None 存为空串。
    每次更新都重置 TTL，保持活跃任务不过期。
    """
    _redis.hset(_key(task_id), mapping={k: str(v) if v is not None else "" for k, v in kwargs.items()})
    _redis.expire(_key(task_id), settings.task_expire_seconds)

"""Celery Worker 配置与任务定义"""
from celery import Celery

from app.config import settings

celery_app = Celery(
    "aigc",
    broker=settings.redis_url,
    backend=settings.redis_url,
)
celery_app.conf.update(
    task_serializer="json",
    result_serializer="json",
    accept_content=["json"],
    timezone="Asia/Shanghai",
    task_track_started=True,
)


# ─── 文字生图任务 ──────────────────────────────────────────────────────────────
@celery_app.task(name="tasks.run_t2i")
def run_t2i(task_id: str, params: dict):
    from app.tasks.task_store import update_task, TaskStatus
    from app.chains.t2i_chain import T2IChain

    try:
        update_task(task_id, status=TaskStatus.PROCESSING, progress=10)
        chain = T2IChain()
        result_url = chain.run(task_id=task_id, **params)
        update_task(task_id, status=TaskStatus.SUCCEEDED, progress=100, result_url=result_url)
    except Exception as e:
        update_task(task_id, status=TaskStatus.FAILED, error=str(e))
        raise


# ─── 文字生视频任务 ────────────────────────────────────────────────────────────
@celery_app.task(name="tasks.run_t2v")
def run_t2v(task_id: str, params: dict):
    from app.tasks.task_store import update_task, TaskStatus
    from app.chains.t2v_chain import T2VChain

    try:
        update_task(task_id, status=TaskStatus.PROCESSING, progress=10)
        chain = T2VChain()
        result_url = chain.run(task_id=task_id, **params)
        update_task(task_id, status=TaskStatus.SUCCEEDED, progress=100, result_url=result_url)
    except Exception as e:
        update_task(task_id, status=TaskStatus.FAILED, error=str(e))
        raise


# ─── 图生视频任务 ──────────────────────────────────────────────────────────────
@celery_app.task(name="tasks.run_i2v")
def run_i2v(task_id: str, params: dict):
    from app.tasks.task_store import update_task, TaskStatus
    from app.chains.i2v_chain import I2VChain

    try:
        update_task(task_id, status=TaskStatus.PROCESSING, progress=10)
        chain = I2VChain()
        result_url = chain.run(task_id=task_id, **params)
        update_task(task_id, status=TaskStatus.SUCCEEDED, progress=100, result_url=result_url)
    except Exception as e:
        update_task(task_id, status=TaskStatus.FAILED, error=str(e))
        raise

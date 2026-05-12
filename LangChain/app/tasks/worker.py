"""
Celery Worker 配置与异步任务定义
Broker/Backend 均使用 Redis（redis_url 配置项）
启动 worker 命令：celery -A app.tasks.worker.celery_app worker --loglevel=info
任务执行流程：API 路由 → .delay() 发消息到 Redis → Celery Worker 消费 → 更新 task_store
"""
from celery import Celery

from app.config import settings

# Celery 实例，broker 接收任务消息，backend 存储任务结果（此处均用 Redis）
celery_app = Celery(
    "aigc",
    broker=settings.redis_url,
    backend=settings.redis_url,
)
celery_app.conf.update(
    task_serializer="json",       # 任务参数序列化格式
    result_serializer="json",     # 任务结果序列化格式
    accept_content=["json"],      # 只接受 JSON 内容
    timezone="Asia/Shanghai",     # 时区（影响定时任务）
    task_track_started=True,      # 任务开始时立即更新状态（STARTED）
)


# ─── 文字生图任务 ──────────────────────────────────────────────────────────────
@celery_app.task(name="tasks.run_t2i")
def run_t2i(task_id: str, params: dict):
    """
    Celery 任务：执行文字生图流水线
    流程：进度 10% → T2IChain.run()（提示词增强 + 模型生成 + 存储）→ 进度 100%
    失败时将 error 写入 task_store 并重新抛出（Celery 会记录为 FAILURE）
    """
    # 延迟导入避免循环依赖（chain 模块在 worker 模块加载时不一定初始化完毕）
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
    """
    Celery 任务：执行文字生视频流水线
    流程：进度 10% → T2VChain.run()（提示词增强 + 提交平台任务 + 轮询结果 + 存储）→ 进度 100%
    视频生成耗时较长（30~180s），由 chain 内部的轮询循环推进进度
    """
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
    """
    Celery 任务：执行图生视频流水线
    流程：进度 10% → I2VChain.run()（图片预处理 + 提交平台任务 + 轮询结果 + 存储）→ 进度 100%
    图片输入支持 URL 或 base64，chain 内部统一处理为 URL 后提交给视频模型
    """
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

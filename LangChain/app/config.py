"""
全局配置管理 —— 基于 pydantic-settings，从 .env 文件读取环境变量
所有字段均有默认值（空字符串），未配置的服务功能不可用
使用方式：from app.config import settings
"""
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # 从项目根目录的 .env 文件读取配置，UTF-8 编码
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")

    # ── OpenAI ────────────────────────────────────────────────────────────────
    openai_api_key: str = ""
    openai_base_url: str = "https://api.openai.com/v1"
    openai_model: str = "gpt-4o"

    # ── 可灵（Kling）─────────────────────────────────────────────────────────
    kling_access_key: str = ""
    kling_secret_key: str = ""
    kling_api_url: str = "https://api.klingai.com"

    # ── 万象（DashScope / Wan）────────────────────────────────────────────────
    dashscope_api_key: str = ""

    # ── Replicate（Flux / SVD）────────────────────────────────────────────────
    replicate_api_token: str = ""

    # ── Runway ────────────────────────────────────────────────────────────────
    runway_api_key: str = ""

    # ── MiniMax ───────────────────────────────────────────────────────────────
    minimax_api_key: str = ""
    minimax_group_id: str = ""

    # ── 对象存储 ──────────────────────────────────────────────────────────────
    minio_endpoint: str = "localhost:9000"
    minio_access_key: str = ""
    minio_secret_key: str = ""
    minio_bucket: str = "aigc-outputs"
    minio_secure: bool = False

    # ── Redis / Celery ────────────────────────────────────────────────────────
    redis_url: str = "redis://localhost:6379/0"

    # ── 应用 ──────────────────────────────────────────────────────────────────
    debug: bool = False
    task_expire_seconds: int = 86400   # 任务结果保留 24h（Redis key TTL）


# 全局单例，其他模块通过 from app.config import settings 直接使用
settings = Settings()

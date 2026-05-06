"""对象存储工具 —— 支持 MinIO / 本地磁盘回退"""
import io
import os
import uuid
from pathlib import Path
from typing import Optional

import httpx

from app.config import settings

# ─── 本地回退目录（MinIO 未配置时使用）────────────────────────────────────────
_LOCAL_DIR = Path(__file__).parent.parent.parent / "outputs"
_LOCAL_DIR.mkdir(exist_ok=True)


def _minio_client():
    """懒加载 MinIO 客户端"""
    from minio import Minio
    client = Minio(
        settings.minio_endpoint,
        access_key=settings.minio_access_key,
        secret_key=settings.minio_secret_key,
        secure=settings.minio_secure,
    )
    if not client.bucket_exists(settings.minio_bucket):
        client.make_bucket(settings.minio_bucket)
    return client


def _use_minio() -> bool:
    return bool(settings.minio_endpoint and settings.minio_access_key)


def upload_bytes(data: bytes, filename: str) -> str:
    """上传字节数据，返回可访问 URL"""
    if _use_minio():
        client = _minio_client()
        client.put_object(
            settings.minio_bucket,
            filename,
            io.BytesIO(data),
            length=len(data),
        )
        scheme = "https" if settings.minio_secure else "http"
        return f"{scheme}://{settings.minio_endpoint}/{settings.minio_bucket}/{filename}"
    else:
        # 本地存储
        path = _LOCAL_DIR / filename
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
        return f"/outputs/{filename}"


def upload_from_url(source_url: str, prefix: str = "misc", ext: Optional[str] = None) -> str:
    """从 URL 下载内容并上传到存储，返回持久化 URL"""
    if not source_url:
        return source_url

    resp = httpx.get(source_url, follow_redirects=True, timeout=60)
    resp.raise_for_status()

    content_type = resp.headers.get("content-type", "")
    if ext is None:
        ext = _guess_ext(content_type)

    filename = f"{prefix}/{uuid.uuid4().hex}.{ext}"
    return upload_bytes(resp.content, filename)


def _guess_ext(content_type: str) -> str:
    mapping = {
        "image/jpeg": "jpg",
        "image/png":  "png",
        "image/webp": "webp",
        "video/mp4":  "mp4",
        "video/webm": "webm",
    }
    for mime, ext in mapping.items():
        if mime in content_type:
            return ext
    return "bin"

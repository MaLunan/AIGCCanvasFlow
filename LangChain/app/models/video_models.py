"""视频模型适配器 —— 统一接口封装可灵 / Wan / MiniMax / Runway"""
import hashlib
import hmac
import time
from abc import ABC, abstractmethod
from typing import Optional

import httpx

from app.config import settings


class BaseVideoModel(ABC):
    @abstractmethod
    def submit(self, **kwargs) -> str:
        """提交生成任务，返回平台侧 task_id"""

    @abstractmethod
    def query(self, platform_task_id: str) -> dict:
        """查询任务状态，返回 {status, progress, result_url, error}"""


# ─── 可灵（Kling）─────────────────────────────────────────────────────────────
class KlingVideoModel(BaseVideoModel):
    """可灵文字生视频 & 图生视频"""

    def _jwt_token(self) -> str:
        """
        生成可灵 API 鉴权 JWT（每次请求前实时生成，有效期 30 分钟）
        iss: access_key 作为签发者；exp: 30min 后过期；nbf: 5s 前即可生效（容忍时钟偏差）
        """
        import jwt  # PyJWT

        payload = {
            "iss": settings.kling_access_key,
            "exp": int(time.time()) + 1800,  # 30 分钟有效期
            "nbf": int(time.time()) - 5,     # 5 秒容错（防止服务器时钟偏差导致 not-before 失败）
        }
        return jwt.encode(payload, settings.kling_secret_key, algorithm="HS256")

    def _headers(self) -> dict:
        return {
            "Authorization": f"Bearer {self._jwt_token()}",
            "Content-Type": "application/json",
        }

    def submit_t2v(self, prompt: str, negative_prompt: str = "",
                   duration: int = 5, aspect_ratio: str = "16:9",
                   model_name: str = "kling-v2") -> str:
        body = {
            "model_name": model_name,
            "prompt": prompt,
            "negative_prompt": negative_prompt,
            "cfg_scale": 0.5,
            "mode": "std",
            "duration": str(duration),
            "aspect_ratio": aspect_ratio,
        }
        resp = httpx.post(
            f"{settings.kling_api_url}/v1/videos/text2video",
            headers=self._headers(),
            json=body,
            timeout=30,
        )
        resp.raise_for_status()
        data = resp.json()
        return data["data"]["task_id"]

    def submit_i2v(self, image_url: str, prompt: str = "",
                   duration: int = 5, motion_strength: float = 0.5,
                   model_name: str = "kling-v2") -> str:
        body = {
            "model_name": model_name,
            "image": image_url,
            "prompt": prompt,
            "cfg_scale": 0.5,
            "mode": "std",
            "duration": str(duration),
            "static_mask": "",
            "dynamic_mask": "",
        }
        resp = httpx.post(
            f"{settings.kling_api_url}/v1/videos/image2video",
            headers=self._headers(),
            json=body,
            timeout=30,
        )
        resp.raise_for_status()
        return resp.json()["data"]["task_id"]

    def submit(self, mode: str = "t2v", **kwargs) -> str:
        if mode == "t2v":
            return self.submit_t2v(**kwargs)
        return self.submit_i2v(**kwargs)

    def query(self, platform_task_id: str) -> dict:
        resp = httpx.get(
            f"{settings.kling_api_url}/v1/videos/text2video/{platform_task_id}",
            headers=self._headers(),
            timeout=15,
        )
        resp.raise_for_status()
        data = resp.json()["data"]
        # 将可灵平台状态映射为内部统一状态（processing/succeeded/failed）
        status_map = {
            "submitted": "processing",  # 已提交，等待处理
            "processing": "processing", # 处理中
            "succeed":    "succeeded",  # 注意：可灵用 "succeed"（非 "succeeded"）
            "failed":     "failed",
        }
        # 从 task_result.videos 数组取第一个视频的 URL
        works = data.get("task_result", {}).get("videos", [])
        result_url = works[0]["url"] if works else None
        return {
            "status": status_map.get(data["task_status"], "processing"),
            # 进度：处理中暂估 50%，成功为 100%，其余为 0
            "progress": 50 if data["task_status"] == "processing" else (100 if data["task_status"] == "succeed" else 0),
            "result_url": result_url,
            "error": data.get("task_status_msg"),
        }


# ─── 万象 Wan（DashScope）─────────────────────────────────────────────────────
class WanVideoModel(BaseVideoModel):
    """阿里云通义万象视频生成模型（wanx2.1 系列）"""
    BASE_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/video-generation"

    def _headers(self) -> dict:
        return {
            "Authorization": f"Bearer {settings.dashscope_api_key}",
            "Content-Type": "application/json",
            "X-DashScope-Async": "enable",  # 启用异步模式（否则接口会同步阻塞等待）
        }

    def submit_t2v(self, prompt: str, negative_prompt: str = "",
                   resolution: str = "1280*720", duration: int = 5) -> str:
        body = {
            "model": "wanx2.1-t2v-turbo",
            "input": {"prompt": prompt, "negative_prompt": negative_prompt},
            "parameters": {"resolution": resolution, "duration": duration},
        }
        resp = httpx.post(
            f"{self.BASE_URL}/generation",
            headers=self._headers(),
            json=body,
            timeout=30,
        )
        resp.raise_for_status()
        return resp.json()["output"]["task_id"]

    def submit_i2v(self, image_url: str, prompt: str = "",
                   resolution: str = "1280*720", duration: int = 5) -> str:
        body = {
            "model": "wanx2.1-i2v-turbo",
            "input": {"image_url": image_url, "prompt": prompt},
            "parameters": {"resolution": resolution, "duration": duration},
        }
        resp = httpx.post(
            f"{self.BASE_URL}/generation",
            headers=self._headers(),
            json=body,
            timeout=30,
        )
        resp.raise_for_status()
        return resp.json()["output"]["task_id"]

    def submit(self, mode: str = "t2v", **kwargs) -> str:
        if mode == "t2v":
            return self.submit_t2v(**kwargs)
        return self.submit_i2v(**kwargs)

    def query(self, platform_task_id: str) -> dict:
        # DashScope 任务查询使用独立的通用任务 API（非视频专属）
        resp = httpx.get(
            f"https://dashscope.aliyuncs.com/api/v1/tasks/{platform_task_id}",
            headers={"Authorization": f"Bearer {settings.dashscope_api_key}"},
            timeout=15,
        )
        resp.raise_for_status()
        output = resp.json()["output"]
        # 将 DashScope 全大写状态映射为内部状态
        status_map = {
            "PENDING": "processing",
            "RUNNING": "processing",
            "SUCCEEDED": "succeeded",
            "FAILED": "failed",
        }
        return {
            "status": status_map.get(output["task_status"], "processing"),
            # task_metrics.SUCCEEDED 为已完成的子任务数（0 或 1），乘以 10 作为粗略进度
            "progress": output.get("task_metrics", {}).get("SUCCEEDED", 0) * 10,
            "result_url": output.get("video_url"),
            "error": output.get("message"),
        }


# ─── MiniMax ──────────────────────────────────────────────────────────────────
class MiniMaxVideoModel(BaseVideoModel):
    def _headers(self) -> dict:
        return {
            "Authorization": f"Bearer {settings.minimax_api_key}",
            "Content-Type": "application/json",
        }

    def submit(self, prompt: str = "", mode: str = "t2v", **kwargs) -> str:
        body = {
            "model": "video-01",
            "prompt": prompt,
        }
        resp = httpx.post(
            "https://api.minimax.chat/v1/video_generation",
            headers=self._headers(),
            json=body,
            timeout=30,
        )
        resp.raise_for_status()
        return resp.json()["task_id"]

    def query(self, platform_task_id: str) -> dict:
        resp = httpx.get(
            f"https://api.minimax.chat/v1/query/video_generation?task_id={platform_task_id}",
            headers=self._headers(),
            timeout=15,
        )
        resp.raise_for_status()
        data = resp.json()
        status_map = {"Queueing": "processing", "Processing": "processing",
                      "Success": "succeeded", "Fail": "failed"}
        return {
            "status": status_map.get(data["status"], "processing"),
            "progress": 50 if data["status"] == "Processing" else (100 if data["status"] == "Success" else 0),
            "result_url": data.get("file_id"),  # MiniMax 返回 file_id，需另调下载接口获取实际 URL
            "error": data.get("base_resp", {}).get("status_msg"),
        }


# ─── 工厂函数 ──────────────────────────────────────────────────────────────────
_VIDEO_REGISTRY = {
    "kling":    KlingVideoModel,
    "wan":      WanVideoModel,
    "minimax":  MiniMaxVideoModel,
}


def get_video_model(model_name: str) -> BaseVideoModel:
    """工厂函数：根据 model_name 字符串实例化对应的视频模型适配器"""
    cls = _VIDEO_REGISTRY.get(model_name)
    if not cls:
        raise ValueError(f"不支持的视频模型: {model_name}，可选: {list(_VIDEO_REGISTRY.keys())}")
    return cls()

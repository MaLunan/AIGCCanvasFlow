"""文字生视频完整 Chain：提示词增强 → 提交任务 → 轮询结果 → 存储"""
import time

from app.chains.prompt_enhance import enhance_t2v_prompt
from app.models.video_models import get_video_model
from app.tasks.task_store import update_task
from app.utils.storage import upload_from_url

_POLL_INTERVAL = 5   # 每次轮询间隔（秒），避免过于频繁请求平台 API
_POLL_TIMEOUT  = 600 # 最长等待 10 分钟（视频生成通常 30~180s，给予足够余量）


class T2VChain:
    def run(
        self,
        task_id: str,
        prompt: str,
        model: str = "kling",
        duration: int = 5,
        resolution: str = "1080p",
        aspect_ratio: str = "16:9",
        enhance_prompt: bool = True,
        **kwargs,
    ) -> str:
        # Step 1: 提示词增强
        update_task(task_id, progress=10)
        if enhance_prompt and prompt:
            enhanced = enhance_t2v_prompt(prompt)
            final_prompt   = enhanced.get("prompt", prompt)
            final_negative = enhanced.get("negative_prompt", "")
        else:
            final_prompt   = prompt
            final_negative = ""

        # Step 2: 提交视频生成任务
        update_task(task_id, progress=20)
        video_model = get_video_model(model)
        platform_task_id = video_model.submit(
            mode="t2v",
            prompt=final_prompt,
            negative_prompt=final_negative,
            duration=duration,
            aspect_ratio=aspect_ratio,
            resolution=_resolution_str(resolution),
            **kwargs,
        )

        # Step 3: 轮询直到完成
        result_url = self._poll(task_id, video_model, platform_task_id)

        # Step 4: 存储
        update_task(task_id, progress=90)
        stored_url = upload_from_url(result_url, prefix=f"t2v/{task_id}", ext="mp4")
        return stored_url

    @staticmethod
    def _poll(task_id: str, model, platform_task_id: str) -> str:
        """
        轮询平台任务状态直到完成或超时。
        进度条从 25 线性增长到 85（最终 100 由 worker 在成功后设置）。
        每隔 _POLL_INTERVAL 秒查询一次，超过 _POLL_TIMEOUT 则抛出 TimeoutError。
        """
        deadline = time.time() + _POLL_TIMEOUT
        progress_start = 25  # 轮询起始进度（提交任务后已到 20%）

        while time.time() < deadline:
            result = model.query(platform_task_id)
            if result["status"] == "succeeded":
                return result["result_url"]
            if result["status"] == "failed":
                raise RuntimeError(f"视频生成失败: {result.get('error')}")

            # 每次轮询进度 +3，上限 85（留 15% 给后续存储步骤）
            progress_start = min(progress_start + 3, 85)
            update_task(task_id, progress=progress_start)
            time.sleep(_POLL_INTERVAL)

        raise TimeoutError("视频生成超时，请稍后重试")


def _resolution_str(resolution: str) -> str:
    """将 '1080p'/'720p' 转换为模型需要的宽x高字符串"""
    mapping = {
        "1080p": "1920*1080",
        "720p":  "1280*720",
        "480p":  "854*480",
    }
    return mapping.get(resolution, "1280*720")

"""
FastAPI 应用入口 —— AIGC LangChain 服务
路由结构：
  POST /api/v1/t2i/generate         文字生图（异步，返回 task_id）
  POST /api/v1/t2v/generate         文字生视频（异步）
  POST /api/v1/t2v/generate/script  脚本拆镜批量生成（异步）
  POST /api/v1/i2v/generate         图生视频（异步）
  GET  /api/v1/tasks/{task_id}      查询任务状态（前端轮询）
  POST /api/v1/polish/text          文字润化（同步）
  GET  /health                      健康检查（Spring Boot 存活探针）
启动命令：uvicorn app.main:app --host 0.0.0.0 --port 8888
"""
from pathlib import Path

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from app.api import t2i, t2v, i2v, tasks, polish

OUTPUTS_DIR = Path(__file__).parent.parent / "outputs"
OUTPUTS_DIR.mkdir(exist_ok=True)

app = FastAPI(
    title="AIGC LangChain Service",
    description="文字生图 / 文字生视频 / 图生视频 AI 服务",
    version="1.0.0",
)

# 允许所有来源跨域（Spring Boot 网关已做 CORS，此处为本地联调保险起见）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由，prefix 对应 Spring Boot LangChainClient 中的 baseUrl 路径
app.include_router(t2i.router,    prefix="/api/v1/t2i",    tags=["文字生图"])
app.include_router(t2v.router,    prefix="/api/v1/t2v",    tags=["文字生视频"])
app.include_router(i2v.router,    prefix="/api/v1/i2v",    tags=["图生视频"])
app.include_router(tasks.router,  prefix="/api/v1/tasks",  tags=["任务查询"])
app.include_router(polish.router, prefix="/api/v1/polish", tags=["文字润化"])
app.mount("/outputs", StaticFiles(directory=OUTPUTS_DIR), name="outputs")


@app.get("/health", tags=["健康检查"])
def health():
    """健康检查端点，Spring Boot 启动时探测 Python 服务是否就绪"""
    return {"status": "ok"}

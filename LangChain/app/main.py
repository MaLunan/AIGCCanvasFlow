from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api import t2i, t2v, i2v, tasks, polish

app = FastAPI(
    title="AIGC LangChain Service",
    description="文字生图 / 文字生视频 / 图生视频 AI 服务",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(t2i.router,   prefix="/api/v1/t2i",   tags=["文字生图"])
app.include_router(t2v.router,   prefix="/api/v1/t2v",   tags=["文字生视频"])
app.include_router(i2v.router,   prefix="/api/v1/i2v",   tags=["图生视频"])
app.include_router(tasks.router,  prefix="/api/v1/tasks",  tags=["任务查询"])
app.include_router(polish.router, prefix="/api/v1/polish", tags=["文字润化"])


@app.get("/health", tags=["健康检查"])
def health():
    return {"status": "ok"}

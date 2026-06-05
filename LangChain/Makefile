.PHONY: venv install dev worker all sync

# 创建虚拟环境
venv:
	uv venv --python 3.10
	@echo "✅ 虚拟环境创建完成"

# 安装依赖
install:
	uv sync

# 同步依赖并锁定（生成 uv.lock）
sync:
	uv lock
	uv sync

# 启动 FastAPI 开发服务器
dev:
	uv run uvicorn app.main:app --reload --host 0.0.0.0 --port 8888

# 启动 Celery Worker
worker:
	uv run celery -A app.tasks.worker.celery_app worker --loglevel=info --concurrency=4

# 同时启动 API + Worker
all:
	uv run honcho start

# 本地启动 Nacos
nacos:
	docker run -d \
	--name nacos \
	-p 8848:8848 \
	-p 9848:9848 \
	-p 9849:9849 \
	-e MODE=standalone \
	-e NACOS_AUTH_ENABLE=false \
	--platform linux/amd64 \
	--restart always \
	nacos/nacos-server:v2.1.2
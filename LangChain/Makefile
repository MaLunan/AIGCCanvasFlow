.PHONY: venv install dev worker

# 创建虚拟环境
venv:
	python3 -m venv .venv
	@echo "✅ 虚拟环境创建完成，运行: source .venv/bin/activate"

# 安装依赖（需先激活虚拟环境）
install:
	pip3 install -r requirements.txt

# 启动 FastAPI 开发服务器
dev:
	uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# 启动 Celery Worker（另开一个终端）
worker:
	celery -A app.tasks.worker.celery_app worker --loglevel=info --concurrency=4

# 同时启动 API + Worker（需已 pip install -r requirements.txt，见 .venv/bin/honcho）
all:
	.venv/bin/honcho start
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

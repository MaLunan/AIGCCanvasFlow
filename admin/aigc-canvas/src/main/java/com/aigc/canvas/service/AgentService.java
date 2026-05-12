package com.aigc.canvas.service;

import com.aigc.canvas.dto.AgentGenerateRequest;
import com.aigc.canvas.dto.AgentGenerateResponse;
import com.aigc.canvas.dto.PolishRequest;

/**
 * AI 智能体服务接口：定义生成任务提交、任务查询、文字润化三个能力
 * 实现类：AgentServiceImpl（通过 LangChainClient 调用 Python AI 服务）
 */
public interface AgentService {
    /** 提交生图 / 生视频异步任务，返回 taskId（前端轮询状态） */
    AgentGenerateResponse generate(Long userId, AgentGenerateRequest request);

    /** 查询任务状态（前端轮询调用），返回进度和结果 URL */
    AgentGenerateResponse queryTask(String taskId);

    /** 文字润化（同步，直接返回润化后的文本） */
    String polish(Long userId, PolishRequest request);
}

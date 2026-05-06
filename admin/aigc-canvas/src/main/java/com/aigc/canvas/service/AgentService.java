package com.aigc.canvas.service;

import com.aigc.canvas.dto.AgentGenerateRequest;
import com.aigc.canvas.dto.AgentGenerateResponse;
import com.aigc.canvas.dto.PolishRequest;

public interface AgentService {
    /** 提交生图 / 生视频异步任务 */
    AgentGenerateResponse generate(Long userId, AgentGenerateRequest request);

    /** 查询任务状态 */
    AgentGenerateResponse queryTask(String taskId);

    /** 文字润化（同步，直接返回结果） */
    String polish(Long userId, PolishRequest request);
}

package com.aigc.canvas.service;

import com.aigc.canvas.dto.AgentGenerateRequest;
import com.aigc.canvas.dto.AgentGenerateResponse;

public interface AgentService {
    AgentGenerateResponse generate(Long userId, AgentGenerateRequest request);
    AgentGenerateResponse queryTask(String taskId);
}

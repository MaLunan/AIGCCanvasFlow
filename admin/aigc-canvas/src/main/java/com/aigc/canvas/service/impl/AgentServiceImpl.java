package com.aigc.canvas.service.impl;

import com.aigc.canvas.dto.AgentGenerateRequest;
import com.aigc.canvas.dto.AgentGenerateResponse;
import com.aigc.canvas.service.AgentService;
import com.aigc.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    // TODO: 替换为 Redis 或数据库存储任务状态
    private final ConcurrentHashMap<String, AgentGenerateResponse> taskStore = new ConcurrentHashMap<>();

    @Override
    public AgentGenerateResponse generate(Long userId, AgentGenerateRequest request) {
        String taskId = UUID.randomUUID().toString().replace("-", "");

        // TODO: 接入实际大模型 API（Kling / Sora / Runway 等）
        // 1. 根据 request.getModelKey() 选择模型
        // 2. 调用对应 API，提交异步任务
        // 3. 记录 taskId 与回调 URL
        log.info("Agent generate: userId={}, prompt={}, model={}", userId, request.getPrompt(), request.getModelKey());

        AgentGenerateResponse response = AgentGenerateResponse.builder()
                .taskId(taskId)
                .status("pending")
                .estimatedSeconds(estimateSeconds(request))
                .costPoints(estimateCost(request))
                .build();

        taskStore.put(taskId, response);
        return response;
    }

    @Override
    public AgentGenerateResponse queryTask(String taskId) {
        AgentGenerateResponse resp = taskStore.get(taskId);
        if (resp == null) throw new BusinessException(404, "任务不存在");
        return resp;
    }

    private int estimateSeconds(AgentGenerateRequest req) {
        int base = "video".equals(req.getTargetType()) ? 30 : 10;
        if (req.getDuration() != null) base += req.getDuration() * 2;
        return base;
    }

    private int estimateCost(AgentGenerateRequest req) {
        if ("video".equals(req.getTargetType())) {
            int dur = req.getDuration() != null ? req.getDuration() : 5;
            return dur * 10;
        }
        return 5;
    }
}

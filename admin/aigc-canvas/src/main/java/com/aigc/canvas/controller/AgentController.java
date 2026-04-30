package com.aigc.canvas.controller;

import com.aigc.canvas.dto.AgentGenerateRequest;
import com.aigc.canvas.dto.AgentGenerateResponse;
import com.aigc.canvas.service.AgentService;
import com.aigc.common.constant.CommonConstants;
import com.aigc.common.model.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/canvas/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /**
     * 提交 AI 生成任务（异步）
     * 支持用户手动触发，也支持 OpenClaw / 外部 AI 智能体通过 API Key 调用
     */
    @PostMapping("/generate")
    public R<AgentGenerateResponse> generate(
            @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) Long userId,
            @Valid @RequestBody AgentGenerateRequest request) {
        // userId 为空时允许通过 API Key 匿名调用（网关层鉴权）
        Long uid = userId != null ? userId : 0L;
        return R.ok(agentService.generate(uid, request));
    }

    /**
     * 查询任务状态 / 结果
     */
    @GetMapping("/task/{taskId}")
    public R<AgentGenerateResponse> queryTask(@PathVariable String taskId) {
        return R.ok(agentService.queryTask(taskId));
    }
}

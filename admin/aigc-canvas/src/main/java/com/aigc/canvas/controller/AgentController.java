package com.aigc.canvas.controller;

import com.aigc.canvas.dto.AgentGenerateRequest;
import com.aigc.canvas.dto.AgentGenerateResponse;
import com.aigc.canvas.dto.PolishRequest;
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
     * 提交 AI 生图 / 生视频任务（异步）
     */
    @PostMapping("/generate")
    public R<AgentGenerateResponse> generate(
            @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) Long userId,
            @Valid @RequestBody AgentGenerateRequest request) {
        Long uid = userId != null ? userId : 0L;
        return R.ok(agentService.generate(uid, request));
    }

    /**
     * 查询任务状态 / 结果（前端轮询）
     */
    @GetMapping("/task/{taskId}")
    public R<AgentGenerateResponse> queryTask(@PathVariable String taskId) {
        return R.ok(agentService.queryTask(taskId));
    }

    /**
     * 文字润化（同步，直接返回润化结果）
     */
    @PostMapping("/polish")
    public R<String> polish(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @Valid @RequestBody PolishRequest request) {
        return R.ok(agentService.polish(userId, request));
    }
}

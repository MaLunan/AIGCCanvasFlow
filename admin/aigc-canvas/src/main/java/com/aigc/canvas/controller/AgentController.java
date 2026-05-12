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

/**
 * AI 智能体控制器
 * - /canvas/agent/generate：提交异步生图/生视频任务
 * - /canvas/agent/task/{taskId}：查询任务状态（前端轮询）
 * - /canvas/agent/polish：文字润化（同步返回）
 * 用户 ID 由网关解析 JWT 后注入 X-User-Id 请求头，无需在请求体中传递
 */
@RestController
@RequestMapping("/canvas/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /**
     * 提交 AI 生图 / 生视频任务（异步）
     * POST /canvas/agent/generate
     * 返回 taskId，前端通过 /task/{taskId} 轮询进度
     * userId 标注为 required=false，兼容未登录场景（使用默认模型时无 userId）
     */
    @PostMapping("/generate")
    public R<AgentGenerateResponse> generate(
            @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) Long userId,
            @Valid @RequestBody AgentGenerateRequest request) {
        // userId 为 null 时（未登录）降级为 0L，使用平台默认模型
        Long uid = userId != null ? userId : 0L;
        return R.ok(agentService.generate(uid, request));
    }

    /**
     * 查询任务状态 / 结果（前端轮询）
     * GET /canvas/agent/task/{taskId}
     * 返回 status（pending/processing/success/failed）+ progress（0~100）+ resultUrl
     * 前端每隔 1~2 秒轮询一次，status=success 时停止轮询并展示结果
     */
    @GetMapping("/task/{taskId}")
    public R<AgentGenerateResponse> queryTask(@PathVariable String taskId) {
        return R.ok(agentService.queryTask(taskId));
    }

    /**
     * 文字润化（同步，直接返回润化结果）
     * POST /canvas/agent/polish
     * 用于 TextNode 中的"AI 润化"按钮，等待期间前端显示 loading 状态
     */
    @PostMapping("/polish")
    public R<String> polish(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @Valid @RequestBody PolishRequest request) {
        return R.ok(agentService.polish(userId, request));
    }
}

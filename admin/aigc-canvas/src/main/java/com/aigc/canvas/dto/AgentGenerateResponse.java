package com.aigc.canvas.dto;

import lombok.Builder;
import lombok.Data;

/**
 * AI 生图/生视频任务响应 DTO（提交任务 和 轮询状态 复用同一结构）
 * 提交成功：taskId 有值，status=pending，resultUrl=null
 * 轮询中：status=processing，progress=0~100，estimatedSeconds 剩余估计秒数
 * 完成：status=success，resultUrl 为可直接访问的媒体 URL
 * 失败：status=failed，error 为错误描述
 */
@Data
@Builder
public class AgentGenerateResponse {
    /** 异步任务 ID */
    private String taskId;
    /** 任务状态：pending / processing / success / failed */
    private String status;
    /** 预估等待秒数 */
    private Integer estimatedSeconds;
    /** 结果 URL（status=success 时有值） */
    private String resultUrl;
    /** 消耗算力点 */
    private Integer costPoints;
    /** 进度 0~100（轮询时使用） */
    private Integer progress;
    /** 错误信息（status=failed 时有值） */
    private String error;
}

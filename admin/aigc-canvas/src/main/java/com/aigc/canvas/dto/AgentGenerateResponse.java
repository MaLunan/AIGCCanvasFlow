package com.aigc.canvas.dto;

import lombok.Builder;
import lombok.Data;

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
}

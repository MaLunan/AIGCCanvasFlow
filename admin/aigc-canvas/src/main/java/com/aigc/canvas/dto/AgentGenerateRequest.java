package com.aigc.canvas.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AgentGenerateRequest {

    @NotBlank(message = "生成提示词不能为空")
    private String prompt;

    /** 目标类型：video / image */
    private String targetType = "video";

    /** 使用的模型 key，不传则系统自动选择 */
    private String modelKey;

    /** 比例：16:9 / 9:16 / 1:1 等 */
    private String aspect = "16:9";

    /** 时长（秒，仅视频） */
    private Integer duration = 5;

    /** 清晰度：720p / 1080p / 4K */
    private String resolution = "1080p";

    /** 音频：sound / mute */
    private String audio = "sound";

    /** 回调 Webhook URL（Agent 调用时传入） */
    private String webhookUrl;

    /** 用户模型库中选用的模型 ID */
    private Long libraryModelId;

    /** 上游节点上下文，用于 AI 参考 */
    private List<ContextItem> context = new ArrayList<>();
}

package com.aigc.canvas.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 文字润化请求 DTO
 * 调用同步接口 POST /canvas/agent/polish，直接返回润化后文本（非异步任务）
 * libraryModelId 指定用户模型库中的文本类模型；不传则系统自动选择默认文本模型
 */
@Data
public class PolishRequest {

    @NotBlank(message = "文本内容不能为空")
    private String text;

    /** 选用的模型库 ID（用户模型库中的文本类模型） */
    private Long libraryModelId;

    /** 上游节点上下文，用于 AI 参考 */
    private List<ContextItem> context = new ArrayList<>();
}

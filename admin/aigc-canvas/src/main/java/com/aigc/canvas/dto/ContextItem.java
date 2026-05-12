package com.aigc.canvas.dto;

import lombok.Data;

/**
 * AI 上下文条目：代表画布中一个上游节点的内容
 * 在文字润化（polish）和 AI 生成时，将上游节点的内容作为参考上下文传给 LangChain
 * 前端通过 getUpstreamContext()（BFS 遍历）收集当前节点的所有上游节点内容
 */
@Data
public class ContextItem {
    /** 节点 ID（VueFlow 节点的唯一标识） */
    private String nodeId;
    /** 节点标签（边上显示的数据流标签，如"文字输出"） */
    private String label;
    /** 节点内容（TextNode 的文本内容或其他节点的输出值） */
    private String content;
}

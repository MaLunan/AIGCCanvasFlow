package com.aigc.canvas.controller;

import com.aigc.canvas.dto.AiModelVO;
import com.aigc.canvas.service.AiModelService;
import com.aigc.common.model.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/canvas/models")
@RequiredArgsConstructor
public class AiModelController {

    private final AiModelService aiModelService;

    /** 查询模型列表，type: video / image / audio，不传返回全部 */
    @GetMapping
    public R<List<AiModelVO>> list(@RequestParam(required = false) String type) {
        return R.ok(aiModelService.list(type));
    }

    /** 按 modelKey 查询模型详情 */
    @GetMapping("/key/{modelKey}")
    public R<AiModelVO> getByKey(@PathVariable String modelKey) {
        return R.ok(aiModelService.getByKey(modelKey));
    }
}

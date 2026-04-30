package com.aigc.notify.controller;

import com.aigc.common.constant.CommonConstants;
import com.aigc.common.model.R;
import com.aigc.notify.dto.MessageDTO;
import com.aigc.notify.dto.SendMessageRequest;
import com.aigc.notify.service.MessageService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notify/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /** 发送消息（内部调用或管理端使用） */
    @PostMapping("/send")
    public R<Void> send(@Valid @RequestBody SendMessageRequest request) {
        messageService.send(request);
        return R.ok();
    }

    /** 获取当前用户消息列表 */
    @GetMapping("/list")
    public R<Page<MessageDTO>> list(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(messageService.listByReceiver(userId, current, size));
    }

    /** 未读消息数量 */
    @GetMapping("/unread-count")
    public R<Long> unreadCount(@RequestHeader(CommonConstants.HEADER_USER_ID) Long userId) {
        return R.ok(messageService.countUnread(userId));
    }

    /** 标记单条已读 */
    @PutMapping("/{id}/read")
    public R<Void> markRead(
            @PathVariable Long id,
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId) {
        messageService.markRead(id, userId);
        return R.ok();
    }

    /** 全部标记已读 */
    @PutMapping("/read-all")
    public R<Void> markAllRead(@RequestHeader(CommonConstants.HEADER_USER_ID) Long userId) {
        messageService.markAllRead(userId);
        return R.ok();
    }
}

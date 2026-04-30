package com.aigc.user.controller;

import com.aigc.common.model.R;
import com.aigc.user.dto.UserCreateRequest;
import com.aigc.user.dto.UserVO;
import com.aigc.user.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/user")
    public R<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return R.ok(userService.create(request));
    }

    @GetMapping("/user/{id}")
    public R<UserVO> getById(@PathVariable Long id) {
        return R.ok(userService.getById(id));
    }

    @GetMapping("/user")
    public R<Page<UserVO>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        return R.ok(userService.page(current, size));
    }

    @DeleteMapping("/user/{id}")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok();
    }

    /** 内部接口，供 auth 服务 Feign 调用，不经过 Gateway 鉴权 */
    @GetMapping("/user/inner/loadByUsername")
    public R<Map<String, Object>> loadByUsername(@RequestParam("username") String username) {
        return R.ok(userService.loadByUsername(username));
    }
}

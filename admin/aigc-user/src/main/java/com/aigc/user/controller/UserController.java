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

/**
 * 用户控制器：提供用户注册、查询、分页、删除功能
 * aigc-user 服务通过 Spring Cloud 注册，不直接对外暴露（通过网关转发）
 * /user/inner/** 接口供内部 Feign 调用，不经过网关 JWT 鉴权
 */
@RestController
@RequestMapping
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户注册（无需登录，网关白名单）
     * POST /user/register
     * 与 /user 的 create 共享同一 Service 方法，语义区分：register 供前端/公众调用
     */
    @PostMapping("/user/register")
    public R<UserVO> register(@Valid @RequestBody UserCreateRequest request) {
        return R.ok(userService.create(request));
    }

    /**
     * 管理端创建用户
     * POST /user（需管理员权限，当前未加权限注解）
     */
    @PostMapping("/user")
    public R<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return R.ok(userService.create(request));
    }

    /** 根据 ID 查询用户信息：GET /user/{id} */
    @GetMapping("/user/{id}")
    public R<UserVO> getById(@PathVariable Long id) {
        return R.ok(userService.getById(id));
    }

    /** 分页查询所有用户（管理端使用）：GET /user?current=1&size=10 */
    @GetMapping("/user")
    public R<Page<UserVO>> page(
            @RequestParam(value = "current", defaultValue = "1") int current,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return R.ok(userService.page(current, size));
    }

    /** 删除用户：DELETE /user/{id} */
    @DeleteMapping("/user/{id}")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok();
    }

    /**
     * 内部接口，供 aigc-auth 服务通过 Feign 调用
     * GET /user/inner/loadByUsername?username=xxx
     * 返回用户原始信息 Map（含加密密码），用于登录时的密码验证
     * 该接口不经过网关鉴权，直接服务间调用（内网安全）
     */
    @GetMapping("/user/inner/loadByUsername")
    public R<Map<String, Object>> loadByUsername(@RequestParam("username") String username) {
        return R.ok(userService.loadByUsername(username));
    }
}

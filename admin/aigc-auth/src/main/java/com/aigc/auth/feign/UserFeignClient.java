package com.aigc.auth.feign;

import com.aigc.common.model.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "aigc-user", path = "/user/inner")
public interface UserFeignClient {

    @GetMapping("/loadByUsername")
    R<Map<String, Object>> loadUserByUsername(@RequestParam("username") String username);
}

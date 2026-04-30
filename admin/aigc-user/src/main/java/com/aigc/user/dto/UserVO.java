package com.aigc.user.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private Integer status;
    private LocalDateTime createTime;
}

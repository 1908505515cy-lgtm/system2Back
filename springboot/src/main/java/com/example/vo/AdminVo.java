// src/main/java/com/example/vo/AdminVo.java
package com.example.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统管理员响应 VO（输出层）
 */
@Data
public class AdminVo {

    private Long id;
    private String adminCode;
    private String username;
    private String realName;
    private String avatar;
    private Integer gender;
    private String mobile;
    private String email;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private Integer loginCount;
    private Long deptId;
    private String roleIds;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
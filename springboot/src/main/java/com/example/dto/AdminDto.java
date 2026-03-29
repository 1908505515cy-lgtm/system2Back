// src/main/java/com/example/dto/AdminDto.java
package com.example.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

/**
 * 系统管理员请求 DTO（输入层）
 * 用于接收前端新增、修改管理员时的请求参数
 */
@Data
public class AdminDto {

    /** 管理员ID（修改时必传，新增时不需要） */
    private Long id;

    /** 管理员编码 */
    @NotBlank(message = "管理员编码不能为空")
    private String adminCode;

    /** 登录用户名 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码（新增时必填，修改时可选） */
    private String password;

    /** 真实姓名 */
    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    /** 头像URL */
    private String avatar;

    /** 性别：0-未知 1-男 2-女 */
    private Integer gender;

    /** 手机号 */
    private String mobile;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 状态：0-禁用 1-正常 */
    private Integer status;

    /** 所属部门ID */
    private Long deptId;

    /** 角色ID集合 */
    private List<Long> roleIds;

    /** 备注 */
    private String remark;
}
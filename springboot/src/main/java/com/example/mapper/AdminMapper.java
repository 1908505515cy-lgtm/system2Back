package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统管理员 Mapper 接口
 * 继承 BaseMapper 获得标准 CRUD，自定义方法保留 XML 实现
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {

    // ==================== 分页查询（多字段模糊搜索，需自定义 XML） ====================
    List<Admin> selectAdminPage(String keyword, Integer status, Long offset, Long pageSize);

    long selectCount(String keyword, Integer status);

    // ==================== 状态修改 ====================
    int updateStatus(Long id, Integer status);

    // ==================== 唯一性校验 ====================
    int countByAdminCode(String adminCode);

    int countByUsername(String username);

    int countByEmail(String email);

    int countByMobile(String mobile);

    // ==================== AI Agent 专用 ====================
    int updateUserNameByAdminCode(@Param("adminCode") String adminCode, @Param("userName") String userName);

    // ==================== 密码重置 ====================
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    // ==================== 登录查询 ====================
    Admin selectByUsername(@Param("username") String username);
}

// src/main/java/com/example/mapper/AdminMapper.java
package com.example.mapper;

import com.example.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 系统管理员 Mapper 接口（纯 MyBatis）
 */
@Mapper
public interface AdminMapper {

    int insert(Admin admin);

    int updateById(Admin admin);

    int deleteById(Long id);

    Admin selectById(Long id);

    List<Admin> selectAdminPage(String keyword, Integer status, Long offset, Long pageSize);

    long selectCount(String keyword, Integer status);

    int updateStatus(Long id, Integer status);

    // ==================== 主动校验方法 ====================
    int countByAdminCode(String adminCode);

    int countByUsername(String username);

    int countByEmail(String email);

    int countByMobile(String mobile);
}
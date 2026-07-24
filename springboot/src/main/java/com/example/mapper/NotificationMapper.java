package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    @Select("SELECT COUNT(*) FROM sys_notification WHERE deleted = 0 AND is_read = 0 " +
            "AND (target_user IS NULL OR target_user = #{username})")
    int countUnread(String username);
}

package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.common.GenericServiceImpl;
import com.example.entity.Notification;
import com.example.mapper.NotificationMapper;
import com.example.service.NotificationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl extends GenericServiceImpl<Notification, Notification, Notification>
        implements NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationMapper mapper, JdbcTemplate jdbcTemplate) {
        super(mapper, jdbcTemplate);
        this.notificationMapper = mapper;
    }

    @Override
    protected void buildKeywordCondition(QueryWrapper<Notification> wrapper, String keyword) {
        wrapper.and(w -> w
                .like("title", keyword)
                .or().like("content", keyword)
        );
    }

    @Override
    protected void buildTrashKeywordCondition(StringBuilder whereClause, java.util.List<Object> params, String keyword) {
        whereClause.append(" AND (title LIKE ? OR content LIKE ?)");
        String pattern = "%" + keyword + "%";
        params.add(pattern);
        params.add(pattern);
    }

    @Override
    public List<Notification> getMyNotifications(String username) {
        return notificationMapper.selectList(
                new QueryWrapper<Notification>()
                        .and(w -> w.isNull("target_user").or().eq("target_user", username))
                        .orderByDesc("create_time")
                        .last("LIMIT 20"));
    }

    @Override
    public int getUnreadCount(String username) {
        return notificationMapper.countUnread(username);
    }

    @Override
    public void markAsRead(Long id) {
        Notification n = new Notification();
        n.setId(id);
        n.setIsRead(1);
        notificationMapper.updateById(n);
    }

    @Override
    public void markAllAsRead(String username) {
        notificationMapper.update(null,
                new UpdateWrapper<Notification>()
                        .set("is_read", 1)
                        .eq("is_read", 0)
                        .and(w -> w.isNull("target_user").or().eq("target_user", username)));
    }
}

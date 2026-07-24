package com.example.service;

import com.example.common.GenericService;
import com.example.entity.Notification;

import java.util.List;

public interface NotificationService extends GenericService<Notification, Notification, Notification> {

    List<Notification> getMyNotifications(String username);

    int getUnreadCount(String username);

    void markAsRead(Long id);

    void markAllAsRead(String username);
}

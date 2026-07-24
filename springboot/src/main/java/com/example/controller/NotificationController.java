package com.example.controller;

import com.example.common.GenericController;
import com.example.common.Result;
import com.example.entity.Notification;
import com.example.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification")
public class NotificationController extends GenericController<Notification, Notification, Notification> {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    protected NotificationService getService() {
        return notificationService;
    }

    /** 获取当前用户的通知列表 */
    @GetMapping("/my")
    public Result myNotifications(HttpServletRequest request) {
        String username = (String) request.getAttribute("currentUsername");
        return Result.success(notificationService.getMyNotifications(username));
    }

    /** 获取未读通知数量 */
    @GetMapping("/unread-count")
    public Result unreadCount(HttpServletRequest request) {
        String username = (String) request.getAttribute("currentUsername");
        return Result.success(notificationService.getUnreadCount(username));
    }

    /** 标记单条通知为已读 */
    @PutMapping("/read/{id}")
    public Result markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return Result.success("已标记为已读");
    }

    /** 标记所有通知为已读 */
    @PutMapping("/read-all")
    public Result markAllAsRead(HttpServletRequest request) {
        String username = (String) request.getAttribute("currentUsername");
        notificationService.markAllAsRead(username);
        return Result.success("已全部标记为已读");
    }
}

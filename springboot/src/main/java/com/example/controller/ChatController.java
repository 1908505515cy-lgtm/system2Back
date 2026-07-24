package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.Result;
import com.example.entity.Admin;
import com.example.entity.ChatMessage;
import com.example.entity.ChatSession;
import com.example.mapper.AdminMapper;
import com.example.mapper.ChatMessageMapper;
import com.example.mapper.ChatSessionMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final AdminMapper adminMapper;

    public ChatController(ChatSessionMapper sessionMapper, ChatMessageMapper messageMapper, AdminMapper adminMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.adminMapper = adminMapper;
    }

    /** 创建新会话（userId 从当前登录用户获取） */
    @PostMapping("/session")
    public Result createSession(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        ChatSession session = new ChatSession();
        session.setTitle((String) body.getOrDefault("title", "新对话"));
        Long userId = getCurrentUserId(request);
        if (userId != null) {
            session.setUserId(userId);
        }
        sessionMapper.insert(session);
        return Result.success(session);
    }

    /** 获取当前用户的会话列表（按更新时间倒序） */
    @GetMapping("/sessions")
    public Result listSessions(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        QueryWrapper<ChatSession> wrapper = new QueryWrapper<ChatSession>().orderByDesc("update_time");
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        List<ChatSession> sessions = sessionMapper.selectList(wrapper);
        return Result.success(sessions);
    }

    /** 删除会话（逻辑删除） */
    @DeleteMapping("/session/{id}")
    public Result deleteSession(@PathVariable Long id) {
        sessionMapper.deleteById(id);
        messageMapper.delete(new QueryWrapper<ChatMessage>().eq("session_id", id));
        return Result.success("会话已删除");
    }

    /** 更新会话标题 */
    @PutMapping("/session/{id}")
    public Result updateSession(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ChatSession session = sessionMapper.selectById(id);
        if (session == null) return Result.error("会话不存在");
        String title = (String) body.get("title");
        if (title != null) session.setTitle(title);
        sessionMapper.updateById(session);
        return Result.success(session);
    }

    /** 获取某会话的所有消息 */
    @GetMapping("/messages/{sessionId}")
    public Result listMessages(@PathVariable Long sessionId) {
        List<ChatMessage> messages = messageMapper.selectBySessionId(sessionId);
        return Result.success(messages);
    }

    /** 保存一条消息 */
    @PostMapping("/message")
    public Result saveMessage(@RequestBody Map<String, Object> body) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(Long.valueOf(body.get("sessionId").toString()));
        msg.setRole((String) body.get("role"));
        msg.setContent((String) body.get("content"));
        msg.setConfirmData((String) body.get("confirmData"));
        msg.setConfirmStatus((String) body.get("confirmStatus"));
        messageMapper.insert(msg);

        ChatSession session = new ChatSession();
        session.setId(msg.getSessionId());
        sessionMapper.updateById(session);

        return Result.success(msg);
    }

    /** 批量保存消息 */
    @PostMapping("/messages/batch")
    @Transactional(rollbackFor = Exception.class)
    public Result saveMessages(@RequestBody List<Map<String, Object>> messages) {
        for (Map<String, Object> body : messages) {
            ChatMessage msg = new ChatMessage();
            msg.setSessionId(Long.valueOf(body.get("sessionId").toString()));
            msg.setRole((String) body.get("role"));
            msg.setContent((String) body.get("content"));
            msg.setConfirmData((String) body.get("confirmData"));
            msg.setConfirmStatus((String) body.get("confirmStatus"));
            messageMapper.insert(msg);
        }
        return Result.success("保存成功");
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String username = (String) request.getAttribute("currentUsername");
        if (username == null) return null;
        Admin admin = adminMapper.selectByUsername(username);
        return admin != null ? admin.getId() : null;
    }
}

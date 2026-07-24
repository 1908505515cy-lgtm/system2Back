package com.example.controller;

import com.example.common.JwtUtil;
import com.example.common.Result;
import com.example.common.TokenBlacklist;
import com.example.dto.ForgotPasswordDto;
import com.example.dto.LoginDto;
import com.example.dto.RegisterDto;
import com.example.entity.Admin;
import com.example.mapper.AdminMapper;
import com.example.service.AdminService;
import com.example.service.RoleService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
public class LoginController {

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_MINUTES = 15;

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RoleService roleService;
    private final TokenBlacklist tokenBlacklist;
    private final AdminService adminService;

    /**
     * 登录失败计数器：username → 失败次数
     * 失败计数 15 分钟后自动清除（窗口重置）
     */
    private final Cache<String, AtomicInteger> loginFailures = Caffeine.newBuilder()
            .expireAfterWrite(LOCKOUT_MINUTES, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    /**
     * 锁定状态：username → 锁定到期时间戳
     */
    private final Cache<String, Long> lockedAccounts = Caffeine.newBuilder()
            .expireAfterWrite(LOCKOUT_MINUTES, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    public LoginController(AdminMapper adminMapper, PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil, RoleService roleService, TokenBlacklist tokenBlacklist,
                           AdminService adminService) {
        this.adminMapper = adminMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.roleService = roleService;
        this.tokenBlacklist = tokenBlacklist;
        this.adminService = adminService;
    }

    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginDto dto) {
        String username = dto.getUsername();

        // 检查账号是否被锁定
        Long lockExpiry = lockedAccounts.getIfPresent(username);
        if (lockExpiry != null) {
            if (System.currentTimeMillis() < lockExpiry) {
                long remainingSeconds = (lockExpiry - System.currentTimeMillis()) / 1000;
                return Result.error("5007", "账号已被锁定，请 " + remainingSeconds + " 秒后再试");
            } else {
                // 锁定已过期，清除
                lockedAccounts.invalidate(username);
                loginFailures.invalidate(username);
            }
        }

        Admin admin = adminMapper.selectByUsername(username);
        if (admin == null) {
            recordFailure(username);
            return Result.error("5003", "账号或密码错误");
        }

        if (admin.getStatus() != null && admin.getStatus() == 0) {
            return Result.error("A006", "账号已被禁用");
        }

        if (!passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            recordFailure(username);
            return Result.error("5003", "账号或密码错误");
        }

        // 登录成功，清除失败计数
        loginFailures.invalidate(username);
        lockedAccounts.invalidate(username);

        String token = jwtUtil.generateToken(admin.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(admin.getUsername());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("refreshToken", refreshToken);
        Map<String, Object> user = buildUserInfo(admin);
        data.put("user", user);

        return Result.success(data);
    }

    /**
     * 记录登录失败，达到阈值则锁定账号
     */
    private void recordFailure(String username) {
        AtomicInteger count = loginFailures.get(username, k -> new AtomicInteger(0));
        int current = count.incrementAndGet();
        if (current >= MAX_LOGIN_ATTEMPTS) {
            lockedAccounts.put(username, System.currentTimeMillis() + LOCKOUT_MINUTES * 60 * 1000);
            loginFailures.invalidate(username);
        }
    }

    /** 刷新 Token */
    @PostMapping("/refresh")
    public Result refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return Result.error("401", "refreshToken 不能为空");
        }

        if (tokenBlacklist.contains(refreshToken)) {
            return Result.error("401", "refreshToken 已失效");
        }

        if (!jwtUtil.isValid(refreshToken) || jwtUtil.isTokenExpired(refreshToken)) {
            return Result.error("401", "refreshToken 已过期");
        }

        String username = jwtUtil.parseToken(refreshToken);
        String newToken = jwtUtil.generateToken(username);

        Map<String, Object> data = new HashMap<>();
        data.put("token", newToken);
        return Result.success(data);
    }

    /** 登出 */
    @PostMapping("/logout")
    public Result logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                         @RequestBody(required = false) Map<String, String> body) {
        // 将当前 token 加入黑名单
        if (authorization != null && authorization.startsWith("Bearer ")) {
            tokenBlacklist.add(authorization.substring(7));
        }
        // 将 refreshToken 加入黑名单
        if (body != null && body.get("refreshToken") != null) {
            tokenBlacklist.add(body.get("refreshToken"));
        }
        return Result.success("已登出");
    }

    /** 注册 */
    @PostMapping("/register")
    public Result register(@Valid @RequestBody RegisterDto dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            return Result.error("5001", "两次密码不一致");
        }
        adminService.register(dto);
        return Result.success("注册成功");
    }

    /** 忘记密码 - 获取安全问题 */
    @PostMapping("/forgot-password/question")
    public Result getSecurityQuestion(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        if (username == null || username.isEmpty()) {
            return Result.error("5001", "用户名不能为空");
        }
        Admin admin = adminMapper.selectByUsername(username);
        if (admin == null) {
            return Result.error("5003", "用户不存在");
        }
        if (admin.getSecurityQuestion() == null || admin.getSecurityQuestion().isEmpty()) {
            return Result.error("5004", "该用户未设置安全问题，请联系管理员重置密码");
        }
        Map<String, String> data = new HashMap<>();
        data.put("securityQuestion", admin.getSecurityQuestion());
        return Result.success(data);
    }

    /** 忘记密码 - 验证答案并重置密码 */
    @PostMapping("/forgot-password/reset")
    public Result resetPasswordBySecurity(@Valid @RequestBody ForgotPasswordDto dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return Result.error("5001", "两次密码不一致");
        }
        Admin admin = adminMapper.selectByUsername(dto.getUsername());
        if (admin == null) {
            return Result.error("5003", "用户不存在");
        }
        if (admin.getSecurityAnswer() == null) {
            return Result.error("5004", "该用户未设置安全问题");
        }
        if (!passwordEncoder.matches(dto.getSecurityAnswer(), admin.getSecurityAnswer())) {
            return Result.error("5005", "安全答案错误");
        }
        // 重置密码
        String encoded = passwordEncoder.encode(dto.getNewPassword());
        adminMapper.updatePassword(admin.getId(), encoded);
        return Result.success("密码重置成功");
    }

    private Map<String, Object> buildUserInfo(Admin admin) {
        Map<String, Object> user = new HashMap<>();
        user.put("id", admin.getId());
        user.put("username", admin.getUsername());
        user.put("realName", admin.getRealName());
        user.put("avatar", admin.getAvatar());
        user.put("roleIds", admin.getRoleIds());

        List<Long> roleIdList = parseRoleIds(admin.getRoleIds());
        if (!roleIdList.isEmpty()) {
            List<String> modulePerms = roleService.getUserModulePerms(roleIdList);
            user.put("modulePerms", modulePerms);
            List<String> buttonPerms = roleService.getUserButtonPerms(roleIdList);
            user.put("buttonPerms", buttonPerms);
            List<String> roleNames = roleService.getByIds(roleIdList).stream()
                    .map(r -> r.getName())
                    .collect(Collectors.toList());
            user.put("roleNames", roleNames);
        } else {
            user.put("modulePerms", List.of());
            user.put("buttonPerms", List.of());
            user.put("roleNames", List.of());
        }
        return user;
    }

    private List<Long> parseRoleIds(String roleIds) {
        if (roleIds == null || roleIds.trim().isEmpty()) return List.of();
        return Arrays.stream(roleIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }
}

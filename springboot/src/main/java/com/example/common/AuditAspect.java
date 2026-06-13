package com.example.common;

import com.example.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 审计日志 AOP 切面
 * 拦截 GenericController 的增删改操作，自动记录审计日志
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogService auditLogService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditAspect(AuditLogService auditLogService, JwtUtil jwtUtil) {
        this.auditLogService = auditLogService;
        this.jwtUtil = jwtUtil;
    }

    @Pointcut("execution(* com.example.common.GenericController.add(..)) || " +
              "execution(* com.example.common.GenericController.update(..)) || " +
              "execution(* com.example.common.GenericController.delete(..)) || " +
              "execution(* com.example.common.GenericController.batchDelete(..)) || " +
              "execution(* com.example.common.GenericController.updateStatus(..)) || " +
              "execution(* com.example.common.GenericController.updateField(..))")
    public void crudPointcut() {}

    @Around("crudPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String action = mapAction(methodName);
        String module = extractModule(joinPoint);
        String operator = getOperator();
        String ip = getClientIp();

        Object result = joinPoint.proceed();

        Long recordId = extractRecordId(joinPoint, methodName);
        String detail = buildDetail(joinPoint, methodName);

        try {
            auditLogService.log(operator, action, module, recordId, detail, ip);
        } catch (Exception e) {
            log.warn("审计日志记录失败: {}", e.getMessage());
        }

        return result;
    }

    private String mapAction(String methodName) {
        return switch (methodName) {
            case "add" -> "create";
            case "update", "updateStatus", "updateField" -> "update";
            case "delete", "batchDelete" -> "delete";
            default -> methodName;
        };
    }

    private String extractModule(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        return className.replace("Controller", "").toLowerCase();
    }

    private String getOperator() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                String token = request.getHeader("Authorization");
                if (token != null && token.startsWith("Bearer ")) {
                    String username = jwtUtil.parseToken(token.substring(7));
                    return username != null ? username : "unknown";
                }
            }
        } catch (Exception e) {
            log.debug("解析审计操作人失败: {}", e.getMessage());
        }
        return "system";
    }

    private String getClientIp() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception ignored) {}
        return "unknown";
    }

    private Long extractRecordId(ProceedingJoinPoint joinPoint, String methodName) {
        Object[] args = joinPoint.getArgs();
        if ("delete".equals(methodName) && args.length > 0 && args[0] instanceof Long) {
            return (Long) args[0];
        }
        if ("batchDelete".equals(methodName) && args.length > 0 && args[0] instanceof java.util.List) {
            return null;
        }
        // 对于 add/update/updateStatus/updateField，尝试从第一个参数提取 id
        if (args.length > 0) {
            try {
                var method = args[0].getClass().getMethod("getId");
                Object id = method.invoke(args[0]);
                if (id instanceof Long) return (Long) id;
            } catch (Exception ignored) {}
            // updateStatus/updateField 的第一个参数是 Long id
            if (args[0] instanceof Long) return (Long) args[0];
        }
        return null;
    }

    private String buildDetail(ProceedingJoinPoint joinPoint, String methodName) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args.length > 0) {
                Object arg = args[0];
                // 脱敏：移除密码字段
                if (arg != null) {
                    try {
                        var pwdField = arg.getClass().getDeclaredField("password");
                        pwdField.setAccessible(true);
                        pwdField.set(arg, "***");
                    } catch (Exception ignored) {}
                }
                return objectMapper.writeValueAsString(arg);
            }
        } catch (Exception ignored) {}
        return methodName;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}

package com.example.common;

/**
 * AI 操作执行结果
 */
public class AiResult {

    private boolean success;
    private String message;
    private Object data;

    public static AiResult ok(String message) {
        AiResult r = new AiResult();
        r.success = true;
        r.message = message;
        return r;
    }

    public static AiResult ok(String message, Object data) {
        AiResult r = new AiResult();
        r.success = true;
        r.message = message;
        r.data = data;
        return r;
    }

    public static AiResult fail(String message) {
        AiResult r = new AiResult();
        r.success = false;
        r.message = message;
        return r;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}

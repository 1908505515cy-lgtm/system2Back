package com.example.controller;

import com.example.common.GenericController;
import com.example.common.Result;
import com.example.entity.WfTask;
import com.example.service.WfTaskService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/wfTask")
public class WfTaskController extends GenericController<WfTask, WfTask, WfTask> {

    private final WfTaskService wfTaskService;

    public WfTaskController(WfTaskService wfTaskService) {
        this.wfTaskService = wfTaskService;
    }

    @Override
    protected WfTaskService getService() {
        return wfTaskService;
    }

    /** 通过审批 */
    @PostMapping("/approve/{id}")
    public Result approve(@PathVariable Long id, @RequestBody(required = false) Map<String, String> params,
                           HttpServletRequest request) {
        String username = (String) request.getAttribute("currentUsername");
        String comment = params != null ? params.getOrDefault("comment", "") : "";
        wfTaskService.approve(id, username, comment);
        return Result.success("审批已通过");
    }

    /** 驳回审批 */
    @PostMapping("/reject/{id}")
    public Result reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> params,
                          HttpServletRequest request) {
        String username = (String) request.getAttribute("currentUsername");
        String comment = params != null ? params.getOrDefault("comment", "") : "";
        wfTaskService.reject(id, username, comment);
        return Result.success("审批已驳回");
    }

    /** 转办 */
    @PostMapping("/transfer/{id}")
    public Result transfer(@PathVariable Long id, @RequestBody Map<String, String> params,
                            HttpServletRequest request) {
        String username = (String) request.getAttribute("currentUsername");
        String transferTo = params.get("transferTo");
        String comment = params.getOrDefault("comment", "");
        if (transferTo == null || transferTo.isBlank()) {
            return Result.error("转办目标不能为空");
        }
        wfTaskService.transfer(id, username, transferTo, comment);
        return Result.success("已转办给 " + transferTo);
    }

    /** 我的待办 */
    @GetMapping("/pending")
    public Result myPending(HttpServletRequest request) {
        String username = (String) request.getAttribute("currentUsername");
        return Result.success(wfTaskService.getMyPending(username));
    }

    /** 我的已办 */
    @GetMapping("/done")
    public Result myDone(HttpServletRequest request) {
        String username = (String) request.getAttribute("currentUsername");
        return Result.success(wfTaskService.getMyDone(username));
    }

    /** 待办数量 */
    @GetMapping("/pending-count")
    public Result pendingCount(HttpServletRequest request) {
        String username = (String) request.getAttribute("currentUsername");
        return Result.success(wfTaskService.getPendingCount(username));
    }
}

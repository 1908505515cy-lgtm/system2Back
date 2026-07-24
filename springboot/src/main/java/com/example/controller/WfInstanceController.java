package com.example.controller;

import com.example.common.GenericController;
import com.example.common.Result;
import com.example.entity.WfInstance;
import com.example.entity.WfInstanceNode;
import com.example.service.WfInstanceNodeService;
import com.example.service.WfInstanceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wfInstance")
public class WfInstanceController extends GenericController<WfInstance, WfInstance, WfInstance> {

    private final WfInstanceService wfInstanceService;
    private final WfInstanceNodeService instanceNodeService;

    public WfInstanceController(WfInstanceService wfInstanceService,
                                 WfInstanceNodeService instanceNodeService) {
        this.wfInstanceService = wfInstanceService;
        this.instanceNodeService = instanceNodeService;
    }

    @Override
    protected WfInstanceService getService() {
        return wfInstanceService;
    }

    /** 发起流程 */
    @PostMapping("/start")
    public Result start(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        String username = (String) request.getAttribute("currentUsername");
        Long definitionId = Long.valueOf(params.get("definitionId").toString());
        String title = (String) params.get("title");
        String businessType = (String) params.get("businessType");
        Long businessId = params.get("businessId") != null ?
                Long.valueOf(params.get("businessId").toString()) : null;

        WfInstance instance = wfInstanceService.startInstance(definitionId, username, title,
                businessType, businessId);
        return Result.success("流程已发起", instance);
    }

    /** 撤回流程 */
    @PostMapping("/cancel/{id}")
    public Result cancel(@PathVariable Long id, HttpServletRequest request) {
        String username = (String) request.getAttribute("currentUsername");
        wfInstanceService.cancelInstance(id, username);
        return Result.success("流程已撤回");
    }

    /** 我发起的 */
    @GetMapping("/my")
    public Result myInstances(HttpServletRequest request) {
        String username = (String) request.getAttribute("currentUsername");
        return Result.success(wfInstanceService.getMyInstances(username));
    }

    /** 获取实例的节点状态列表 */
    @GetMapping("/nodes/{instanceId}")
    public Result getInstanceNodes(@PathVariable Long instanceId) {
        List<WfInstanceNode> nodes = instanceNodeService.getByInstanceId(instanceId);
        return Result.success(nodes);
    }
}

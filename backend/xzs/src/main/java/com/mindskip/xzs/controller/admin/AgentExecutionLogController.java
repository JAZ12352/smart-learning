package com.mindskip.xzs.controller.admin;

import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.domain.AgentExecutionLog;
import com.mindskip.xzs.service.AgentExecutionLogService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/admin/agent-log")
public class AgentExecutionLogController {

    private final AgentExecutionLogService agentExecutionLogService;

    public AgentExecutionLogController(AgentExecutionLogService agentExecutionLogService) {
        this.agentExecutionLogService = agentExecutionLogService;
    }

    @PostMapping("/save")
    public RestResponse save(@RequestBody AgentExecutionLog log) {
        if (StringUtils.isBlank(log.getRequestId()) || StringUtils.isBlank(log.getAgentName())) {
            return RestResponse.fail(2, "requestId 和 agentName 不能为空");
        }
        log.setCreateTime(new Date());
        agentExecutionLogService.insertByFilter(log);
        return RestResponse.ok();
    }

    @GetMapping("/list")
    public RestResponse<List<AgentExecutionLog>> list(@RequestParam(required = false) String requestId,
                                                       @RequestParam(required = false) Integer userId) {
        List<AgentExecutionLog> logs;
        if (StringUtils.isNotBlank(requestId)) {
            logs = agentExecutionLogService.getLogsByRequestId(requestId);
        } else if (userId != null) {
            logs = agentExecutionLogService.getLogsByUserId(userId);
        } else {
            logs = agentExecutionLogService.getAllLogs();
        }
        return RestResponse.ok(logs);
    }

    @GetMapping("/request/{requestId}")
    public RestResponse<List<AgentExecutionLog>> getByRequestId(@PathVariable String requestId) {
        List<AgentExecutionLog> logs = agentExecutionLogService.getLogsByRequestId(requestId);
        return RestResponse.ok(logs);
    }
}

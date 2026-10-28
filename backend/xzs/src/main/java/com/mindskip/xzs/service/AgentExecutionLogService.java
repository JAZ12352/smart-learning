package com.mindskip.xzs.service;

import com.mindskip.xzs.domain.AgentExecutionLog;

import java.util.List;

public interface AgentExecutionLogService extends BaseService<AgentExecutionLog> {

    List<AgentExecutionLog> getLogsByRequestId(String requestId);

    List<AgentExecutionLog> getLogsByUserId(Integer userId);

    List<AgentExecutionLog> getAllLogs();
}

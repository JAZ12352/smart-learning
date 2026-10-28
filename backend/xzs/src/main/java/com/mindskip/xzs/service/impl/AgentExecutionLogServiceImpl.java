package com.mindskip.xzs.service.impl;

import com.mindskip.xzs.domain.AgentExecutionLog;
import com.mindskip.xzs.repository.AgentExecutionLogMapper;
import com.mindskip.xzs.service.AgentExecutionLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentExecutionLogServiceImpl extends BaseServiceImpl<AgentExecutionLog> implements AgentExecutionLogService {

    private final AgentExecutionLogMapper agentExecutionLogMapper;

    @Autowired
    public AgentExecutionLogServiceImpl(AgentExecutionLogMapper agentExecutionLogMapper) {
        super(agentExecutionLogMapper);
        this.agentExecutionLogMapper = agentExecutionLogMapper;
    }

    @Override
    public List<AgentExecutionLog> getLogsByRequestId(String requestId) {
        return agentExecutionLogMapper.selectByRequestId(requestId);
    }

    @Override
    public List<AgentExecutionLog> getLogsByUserId(Integer userId) {
        return agentExecutionLogMapper.selectByUserId(userId);
    }

    @Override
    public List<AgentExecutionLog> getAllLogs() {
        return agentExecutionLogMapper.selectAll();
    }
}

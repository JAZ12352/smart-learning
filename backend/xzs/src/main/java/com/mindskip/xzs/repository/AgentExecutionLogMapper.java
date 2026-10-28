package com.mindskip.xzs.repository;

import com.mindskip.xzs.domain.AgentExecutionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentExecutionLogMapper extends BaseMapper<AgentExecutionLog> {

    /**
     * selectByRequestId
     *
     * @param requestId requestId
     * @return List<AgentExecutionLog>
     */
    List<AgentExecutionLog> selectByRequestId(@Param("requestId") String requestId);

    /**
     * selectByUserId
     *
     * @param userId userId
     * @return List<AgentExecutionLog>
     */
    List<AgentExecutionLog> selectByUserId(@Param("userId") Integer userId);

    List<AgentExecutionLog> selectAll();
}

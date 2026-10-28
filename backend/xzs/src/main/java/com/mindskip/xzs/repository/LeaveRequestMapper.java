package com.mindskip.xzs.repository;

import com.mindskip.xzs.domain.LeaveRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LeaveRequestMapper extends BaseMapper<LeaveRequest> {

    List<LeaveRequest> page(@Param("userName") String userName,
                            @Param("status") String status);

    List<LeaveRequest> selectByUserId(@Param("userId") Integer userId);
}

package com.mindskip.xzs.repository;

import com.mindskip.xzs.domain.AttendanceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface AttendanceRecordMapper extends BaseMapper<AttendanceRecord> {

    List<AttendanceRecord> page(@Param("userName") String userName,
                                @Param("status") String status,
                                @Param("startDate") String startDate,
                                @Param("endDate") String endDate);

    List<AttendanceRecord> selectByUserIdAndDate(@Param("userId") Integer userId,
                                                  @Param("date") Date date);

    /** 出勤统计: 按状态分组计数 */
    List<Map<String, Object>> statistics(@Param("startDate") String startDate,
                                         @Param("endDate") String endDate);

    /** 缺勤预警: 连续3天未签到的学生 */
    List<Map<String, Object>> warningList(@Param("days") Integer days);
}

package com.mindskip.xzs.service;

import com.mindskip.xzs.domain.AttendanceRecord;
import com.mindskip.xzs.domain.LeaveRequest;
import com.github.pagehelper.PageInfo;

import java.util.List;
import java.util.Map;

public interface AttendanceService {

    /** 考勤记录 */
    PageInfo<AttendanceRecord> pageList(String userName, String status, String startDate, String endDate, Integer pageIndex, Integer pageSize);

    AttendanceRecord add(AttendanceRecord record);

    AttendanceRecord update(AttendanceRecord record);

    void delete(Integer id);

    /** 今日签到 */
    AttendanceRecord checkToday(Integer userId);

    /** 请假管理 */
    PageInfo<LeaveRequest> leavePageList(String userName, String status, Integer pageIndex, Integer pageSize);

    LeaveRequest applyLeave(LeaveRequest request);

    LeaveRequest approveLeave(Integer id, String status, String approvalRemark, Integer approverId, String approverName);

    /** 统计 */
    List<Map<String, Object>> statistics(String startDate, String endDate);

    List<Map<String, Object>> warningList();
}

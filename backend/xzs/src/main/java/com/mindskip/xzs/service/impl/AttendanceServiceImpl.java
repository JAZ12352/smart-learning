package com.mindskip.xzs.service.impl;

import com.mindskip.xzs.domain.AttendanceRecord;
import com.mindskip.xzs.domain.LeaveRequest;
import com.mindskip.xzs.repository.AttendanceRecordMapper;
import com.mindskip.xzs.repository.LeaveRequestMapper;
import com.mindskip.xzs.service.AttendanceService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRecordMapper attendanceRecordMapper;
    private final LeaveRequestMapper leaveRequestMapper;

    @Autowired
    public AttendanceServiceImpl(AttendanceRecordMapper attendanceRecordMapper, LeaveRequestMapper leaveRequestMapper) {
        this.attendanceRecordMapper = attendanceRecordMapper;
        this.leaveRequestMapper = leaveRequestMapper;
    }

    @Override
    public PageInfo<AttendanceRecord> pageList(String userName, String status, String startDate, String endDate,
                                                Integer pageIndex, Integer pageSize) {
        return PageHelper.startPage(pageIndex, pageSize, "attendance_date desc")
                .doSelectPageInfo(() -> attendanceRecordMapper.page(userName, status, startDate, endDate));
    }

    @Override
    @Transactional
    public AttendanceRecord add(AttendanceRecord record) {
        Date now = new Date();
        record.setCreateTime(now);
        if (record.getCheckInTime() == null) {
            record.setCheckInTime(now);
        }
        if (record.getStatus() == null) {
            record.setStatus("present");
        }
        attendanceRecordMapper.insertSelective(record);
        return record;
    }

    @Override
    public AttendanceRecord update(AttendanceRecord record) {
        attendanceRecordMapper.updateByPrimaryKeySelective(record);
        return attendanceRecordMapper.selectByPrimaryKey(record.getId());
    }

    @Override
    public void delete(Integer id) {
        attendanceRecordMapper.deleteByPrimaryKey(id);
    }

    @Override
    public AttendanceRecord checkToday(Integer userId) {
        // Check if already checked in today
        Date today = new Date();
        List<AttendanceRecord> records = attendanceRecordMapper.selectByUserIdAndDate(userId, today);
        if (records != null && !records.isEmpty()) {
            AttendanceRecord existing = records.get(0);
            existing.setCheckInTime(new Date());
            attendanceRecordMapper.updateByPrimaryKeySelective(existing);
            return existing;
        }
        // Create new check-in record
        AttendanceRecord record = new AttendanceRecord();
        record.setUserId(userId);
        record.setAttendanceDate(today);
        record.setStatus("present");
        record.setCheckInTime(new Date());
        record.setCreateTime(new Date());
        attendanceRecordMapper.insertSelective(record);
        return record;
    }

    @Override
    public PageInfo<LeaveRequest> leavePageList(String userName, String status, Integer pageIndex, Integer pageSize) {
        return PageHelper.startPage(pageIndex, pageSize, "create_time desc")
                .doSelectPageInfo(() -> leaveRequestMapper.page(userName, status));
    }

    @Override
    @Transactional
    public LeaveRequest applyLeave(LeaveRequest request) {
        request.setStatus("pending");
        request.setCreateTime(new Date());
        leaveRequestMapper.insertSelective(request);
        return request;
    }

    @Override
    @Transactional
    public LeaveRequest approveLeave(Integer id, String status, String approvalRemark,
                                      Integer approverId, String approverName) {
        LeaveRequest leave = leaveRequestMapper.selectByPrimaryKey(id);
        if (leave == null) {
            throw new RuntimeException("请假申请不存在");
        }
        leave.setStatus(status);
        leave.setApproverId(approverId);
        leave.setApproverName(approverName);
        leave.setApprovalRemark(approvalRemark);
        leave.setApproveTime(new Date());
        leaveRequestMapper.updateByPrimaryKeySelective(leave);

        // If approved, mark attendance as leave for those dates
        if ("approved".equals(status)) {
            // Create leave attendance records for the date range (simplified)
        }
        return leave;
    }

    @Override
    public List<Map<String, Object>> statistics(String startDate, String endDate) {
        return attendanceRecordMapper.statistics(startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> warningList() {
        return attendanceRecordMapper.warningList(3); // 3 days threshold
    }
}

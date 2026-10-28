package com.mindskip.xzs.controller.admin;

import com.mindskip.xzs.base.BaseApiController;
import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.domain.AttendanceRecord;
import com.mindskip.xzs.domain.LeaveRequest;
import com.mindskip.xzs.domain.User;
import com.mindskip.xzs.service.AttendanceService;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 考勤管理控制器
 * 提供考勤签到、请假审批、出勤统计、缺勤预警等功能
 */
@RestController("AdminAttendanceController")
@RequestMapping(value = "/api/admin/attendance")
public class AttendanceController extends BaseApiController {

    private final AttendanceService attendanceService;

    @Autowired
    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    /**
     * 考勤记录分页查询
     */
    @RequestMapping(value = "/page", method = RequestMethod.POST)
    public RestResponse<PageInfo<AttendanceRecord>> pageList(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer pageIndex,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<AttendanceRecord> pageInfo = attendanceService.pageList(userName, status, startDate, endDate, pageIndex, pageSize);
        return RestResponse.ok(pageInfo);
    }

    /**
     * 添加考勤记录
     */
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public RestResponse<AttendanceRecord> add(@RequestBody AttendanceRecord record) {
        AttendanceRecord result = attendanceService.add(record);
        return RestResponse.ok(result);
    }

    /**
     * 更新考勤记录
     */
    @RequestMapping(value = "/update", method = RequestMethod.PUT)
    public RestResponse<AttendanceRecord> update(@RequestBody AttendanceRecord record) {
        AttendanceRecord result = attendanceService.update(record);
        return RestResponse.ok(result);
    }

    /**
     * 删除考勤记录
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public RestResponse<Void> delete(@PathVariable Integer id) {
        attendanceService.delete(id);
        return RestResponse.ok();
    }

    /**
     * 今日签到
     */
    @RequestMapping(value = "/check-today", method = RequestMethod.POST)
    public RestResponse<AttendanceRecord> checkToday() {
        User user = getCurrentUser();
        AttendanceRecord record = attendanceService.checkToday(user.getId());
        record.setUserName(user.getUserName());
        record.setRealName(user.getRealName());
        return RestResponse.ok(record);
    }

    // ═══════════════════════════════════════════
    // 请假管理
    // ═══════════════════════════════════════════

    /**
     * 请假列表分页
     */
    @RequestMapping(value = "/leave/page", method = RequestMethod.POST)
    public RestResponse<PageInfo<LeaveRequest>> leavePageList(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageIndex,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<LeaveRequest> pageInfo = attendanceService.leavePageList(userName, status, pageIndex, pageSize);
        return RestResponse.ok(pageInfo);
    }

    /**
     * 申请请假
     */
    @RequestMapping(value = "/leave/apply", method = RequestMethod.POST)
    public RestResponse<LeaveRequest> applyLeave(@RequestBody LeaveRequest request) {
        User user = getCurrentUser();
        request.setUserId(user.getId());
        request.setUserName(user.getUserName());
        request.setRealName(user.getRealName());
        LeaveRequest result = attendanceService.applyLeave(request);
        return RestResponse.ok(result);
    }

    /**
     * 审批请假
     */
    @RequestMapping(value = "/leave/approve", method = RequestMethod.POST)
    public RestResponse<LeaveRequest> approveLeave(@RequestBody Map<String, Object> body) {
        Integer id = Integer.valueOf(body.get("id").toString());
        String status = (String) body.get("status");
        String approvalRemark = (String) body.getOrDefault("approvalRemark", "");
        User approver = getCurrentUser();
        LeaveRequest result = attendanceService.approveLeave(id, status, approvalRemark, approver.getId(), approver.getRealName());
        return RestResponse.ok(result);
    }

    // ═══════════════════════════════════════════
    // 统计与预警
    // ═══════════════════════════════════════════

    /**
     * 出勤统计
     */
    @RequestMapping(value = "/statistics", method = RequestMethod.POST)
    public RestResponse<List<Map<String, Object>>> statistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        List<Map<String, Object>> stats = attendanceService.statistics(startDate, endDate);
        return RestResponse.ok(stats);
    }

    /**
     * 缺勤预警（连续 3 天未签到）
     */
    @RequestMapping(value = "/warning", method = RequestMethod.POST)
    public RestResponse<List<Map<String, Object>>> warningList() {
        List<Map<String, Object>> warnings = attendanceService.warningList();
        return RestResponse.ok(warnings);
    }
}

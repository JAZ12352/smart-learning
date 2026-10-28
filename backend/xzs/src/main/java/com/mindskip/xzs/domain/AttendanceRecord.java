package com.mindskip.xzs.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 考勤记录实体
 */
public class AttendanceRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;

    private Integer userId;

    private String userName;

    private String realName;

    private Date attendanceDate;

    /** 状态: present(出勤) / late(迟到) / absent(缺勤) / leave(请假) */
    private String status;

    private Date checkInTime;

    private String remark;

    private Date createTime;

    private Boolean deleted;

    public Integer getId() { return id; }

    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }

    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUserName() { return userName; }

    public void setUserName(String userName) { this.userName = userName == null ? null : userName.trim(); }

    public String getRealName() { return realName; }

    public void setRealName(String realName) { this.realName = realName == null ? null : realName.trim(); }

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    public Date getAttendanceDate() { return attendanceDate; }

    public void setAttendanceDate(Date attendanceDate) { this.attendanceDate = attendanceDate; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status == null ? null : status.trim(); }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    public Date getCheckInTime() { return checkInTime; }

    public void setCheckInTime(Date checkInTime) { this.checkInTime = checkInTime; }

    public String getRemark() { return remark; }

    public void setRemark(String remark) { this.remark = remark == null ? null : remark.trim(); }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    public Date getCreateTime() { return createTime; }

    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Boolean getDeleted() { return deleted; }

    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}

package com.mindskip.xzs.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 请假申请实体
 */
public class LeaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;

    private Integer userId;

    private String userName;

    private String realName;

    private Date startDate;

    private Date endDate;

    /** 请假原因 */
    private String reason;

    /** 状态: pending(待审批) / approved(已批准) / rejected(已拒绝) */
    private String status;

    /** 审批人ID */
    private Integer approverId;

    private String approverName;

    /** 审批意见 */
    private String approvalRemark;

    private Date createTime;

    private Date approveTime;

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
    public Date getStartDate() { return startDate; }

    public void setStartDate(Date startDate) { this.startDate = startDate; }

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    public Date getEndDate() { return endDate; }

    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public String getReason() { return reason; }

    public void setReason(String reason) { this.reason = reason == null ? null : reason.trim(); }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status == null ? null : status.trim(); }

    public Integer getApproverId() { return approverId; }

    public void setApproverId(Integer approverId) { this.approverId = approverId; }

    public String getApproverName() { return approverName; }

    public void setApproverName(String approverName) { this.approverName = approverName == null ? null : approverName.trim(); }

    public String getApprovalRemark() { return approvalRemark; }

    public void setApprovalRemark(String approvalRemark) { this.approvalRemark = approvalRemark == null ? null : approvalRemark.trim(); }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    public Date getCreateTime() { return createTime; }

    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    public Date getApproveTime() { return approveTime; }

    public void setApproveTime(Date approveTime) { this.approveTime = approveTime; }

    public Boolean getDeleted() { return deleted; }

    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}

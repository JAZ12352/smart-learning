package com.mindskip.xzs.domain;

import java.io.Serializable;
import java.util.Date;

public class StudentProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;

    private Integer userId;

    private String knowledgeLevel;

    private String cognitiveStyle;

    private String weakPoints;

    private String learningGoals;

    private String learningPace;

    private String interests;

    private String learningStyle;

    private Integer motivationLevel;

    private String educationBackground;

    private String occupationTarget;

    private String theoryTestResult;

    private String profileSummary;

    private Integer profileVersion;

    private Date createTime;

    private Date modifyTime;

    private Boolean deleted;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getKnowledgeLevel() {
        return knowledgeLevel;
    }

    public void setKnowledgeLevel(String knowledgeLevel) {
        this.knowledgeLevel = knowledgeLevel == null ? null : knowledgeLevel.trim();
    }

    public String getCognitiveStyle() {
        return cognitiveStyle;
    }

    public void setCognitiveStyle(String cognitiveStyle) {
        this.cognitiveStyle = cognitiveStyle == null ? null : cognitiveStyle.trim();
    }

    public String getWeakPoints() {
        return weakPoints;
    }

    public void setWeakPoints(String weakPoints) {
        this.weakPoints = weakPoints == null ? null : weakPoints.trim();
    }

    public String getLearningGoals() {
        return learningGoals;
    }

    public void setLearningGoals(String learningGoals) {
        this.learningGoals = learningGoals == null ? null : learningGoals.trim();
    }

    public String getLearningPace() {
        return learningPace;
    }

    public void setLearningPace(String learningPace) {
        this.learningPace = learningPace == null ? null : learningPace.trim();
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests == null ? null : interests.trim();
    }

    public String getLearningStyle() {
        return learningStyle;
    }

    public void setLearningStyle(String learningStyle) {
        this.learningStyle = learningStyle == null ? null : learningStyle.trim();
    }

    public Integer getMotivationLevel() {
        return motivationLevel;
    }

    public void setMotivationLevel(Integer motivationLevel) {
        this.motivationLevel = motivationLevel;
    }

    public String getEducationBackground() {
        return educationBackground;
    }

    public void setEducationBackground(String educationBackground) {
        this.educationBackground = educationBackground == null ? null : educationBackground.trim();
    }

    public String getOccupationTarget() {
        return occupationTarget;
    }

    public void setOccupationTarget(String occupationTarget) {
        this.occupationTarget = occupationTarget == null ? null : occupationTarget.trim();
    }

    public String getTheoryTestResult() {
        return theoryTestResult;
    }

    public void setTheoryTestResult(String theoryTestResult) {
        this.theoryTestResult = theoryTestResult == null ? null : theoryTestResult.trim();
    }

    public String getProfileSummary() {
        return profileSummary;
    }

    public void setProfileSummary(String profileSummary) {
        this.profileSummary = profileSummary == null ? null : profileSummary.trim();
    }

    public Integer getProfileVersion() {
        return profileVersion;
    }

    public void setProfileVersion(Integer profileVersion) {
        this.profileVersion = profileVersion;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(Date modifyTime) {
        this.modifyTime = modifyTime;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}

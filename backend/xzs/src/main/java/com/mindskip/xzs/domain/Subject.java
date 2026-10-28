package com.mindskip.xzs.domain;

import java.io.Serializable;

public class Subject implements Serializable {

    private static final long serialVersionUID = 8058095034457106502L;

    private Integer id;

    /**
     * 语文 数学 英语 等
     */
    private String name;

    /**
     * 学历层次: 1=大专 2=本科 3=硕士 4=博士
     */
    private Integer educationLevel;

    /**
     * 学历层次名称: 大专/本科/硕士/博士
     */
    private String educationLevelName;

    /**
     * 年级/学期序号 (如: 1,2,3,4)
     */
    private Integer level;

    /**
     * 年级名称 (大一/大二/研一/博一/第一学期 等)
     */
    private String levelName;

    /**
     * 学科门类: 工学/理学/文学/管理学/医学/法学 等
     */
    private String category;

    /**
     * 学科描述
     */
    private String description;

    /**
     * 排序
     */
    private Integer itemOrder;

    /**
     * 父级ID（用于树形分层）
     */
    private Integer parentId;

    /**
     * 节点类型: 1=学科门类 2=一级学科 3=专业 4=课程
     */
    private Integer subjectType;

    private Boolean deleted;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public Integer getEducationLevel() {
        return educationLevel;
    }

    public void setEducationLevel(Integer educationLevel) {
        this.educationLevel = educationLevel;
    }

    public String getEducationLevelName() {
        return educationLevelName;
    }

    public void setEducationLevelName(String educationLevelName) {
        this.educationLevelName = educationLevelName == null ? null : educationLevelName.trim();
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getLevelName() {
        return levelName;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName == null ? null : levelName.trim();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category == null ? null : category.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }

    public Integer getItemOrder() {
        return itemOrder;
    }

    public void setItemOrder(Integer itemOrder) {
        this.itemOrder = itemOrder;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public Integer getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(Integer subjectType) {
        this.subjectType = subjectType;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
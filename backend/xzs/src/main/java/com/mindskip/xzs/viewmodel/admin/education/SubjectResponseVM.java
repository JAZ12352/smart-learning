package com.mindskip.xzs.viewmodel.admin.education;

import com.mindskip.xzs.viewmodel.BaseVM;

public class SubjectResponseVM extends BaseVM {
    private Integer id;
    private String name;
    private Integer educationLevel;
    private String educationLevelName;
    private Integer level;
    private String levelName;
    private String category;
    private String description;
    private Integer parentId;
    private Integer subjectType;
    private Boolean deleted;
    /** 子节点数量（前端树形用） */
    private Integer childrenCount;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getEducationLevel() { return educationLevel; }
    public void setEducationLevel(Integer educationLevel) { this.educationLevel = educationLevel; }
    public String getEducationLevelName() { return educationLevelName; }
    public void setEducationLevelName(String educationLevelName) { this.educationLevelName = educationLevelName; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public String getLevelName() { return levelName; }
    public void setLevelName(String levelName) { this.levelName = levelName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }
    public Integer getSubjectType() { return subjectType; }
    public void setSubjectType(Integer subjectType) { this.subjectType = subjectType; }
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
    public Integer getChildrenCount() { return childrenCount; }
    public void setChildrenCount(Integer childrenCount) { this.childrenCount = childrenCount; }
}

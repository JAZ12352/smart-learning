package com.mindskip.xzs.viewmodel.admin.education;

import com.mindskip.xzs.base.BasePage;



public class SubjectPageRequestVM extends BasePage {
    private Integer id;
    private Integer educationLevel;
    private Integer level;
    private String category;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getEducationLevel() {
        return educationLevel;
    }

    public void setEducationLevel(Integer educationLevel) {
        this.educationLevel = educationLevel;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}

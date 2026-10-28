package com.mindskip.xzs.service;

import com.mindskip.xzs.domain.Subject;
import com.mindskip.xzs.viewmodel.admin.education.SubjectPageRequestVM;
import com.github.pagehelper.PageInfo;

import java.util.List;
import java.util.Map;

public interface SubjectService extends BaseService<Subject> {

    List<Subject> getSubjectByLevel(Integer level);

    List<Subject> getSubjectByEducationLevel(Integer educationLevel);

    List<Subject> getByParentId(Integer parentId);

    List<Subject> getBySubjectType(Integer subjectType);

    /**
     * 获取树形学科列表（门类→一级学科→专业→课程）
     * @param educationLevel 学历层次过滤（可选）
     */
    List<Map<String, Object>> getSubjectTree(Integer educationLevel);

    List<Subject> allSubject();

    Integer levelBySubjectId(Integer id);

    PageInfo<Subject> page(SubjectPageRequestVM requestVM);
}

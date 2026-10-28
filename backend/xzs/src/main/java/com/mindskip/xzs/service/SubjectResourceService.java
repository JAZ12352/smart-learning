package com.mindskip.xzs.service;

import com.mindskip.xzs.domain.SubjectResource;

import java.util.List;

public interface SubjectResourceService extends BaseService<SubjectResource> {

    List<SubjectResource> getResourcesBySubjectId(Integer subjectId);

    int incrementClickCount(Integer id);

    /**
     * 从 t_generated_resource 同步资源到 t_subject_resource
     */
    int syncFromGeneratedResource(Integer subjectId);
}

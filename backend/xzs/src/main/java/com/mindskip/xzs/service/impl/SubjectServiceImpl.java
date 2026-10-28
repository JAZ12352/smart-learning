package com.mindskip.xzs.service.impl;

import com.mindskip.xzs.domain.Subject;
import com.mindskip.xzs.repository.SubjectMapper;
import com.mindskip.xzs.service.SubjectService;
import com.mindskip.xzs.viewmodel.admin.education.SubjectPageRequestVM;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SubjectServiceImpl extends BaseServiceImpl<Subject> implements SubjectService {

    private final SubjectMapper subjectMapper;

    @Autowired
    public SubjectServiceImpl(SubjectMapper subjectMapper) {
        super(subjectMapper);
        this.subjectMapper = subjectMapper;
    }

    @Override
    public Subject selectById(Integer id) {
        return super.selectById(id);
    }

    @Override
    public int updateByIdFilter(Subject record) {
        return super.updateByIdFilter(record);
    }

    @Override
    public List<Subject> getSubjectByLevel(Integer level) {
        return subjectMapper.getSubjectByLevel(level);
    }

    @Override
    public List<Subject> getSubjectByEducationLevel(Integer educationLevel) {
        return subjectMapper.getSubjectByEducationLevel(educationLevel);
    }

    @Override
    public List<Subject> getByParentId(Integer parentId) {
        return subjectMapper.selectByParentId(parentId);
    }

    @Override
    public List<Subject> getBySubjectType(Integer subjectType) {
        return subjectMapper.selectBySubjectType(subjectType);
    }

    @Override
    public List<Map<String, Object>> getSubjectTree(Integer educationLevel) {
        List<Subject> all = educationLevel != null
                ? subjectMapper.getSubjectByEducationLevel(educationLevel)
                : subjectMapper.allSubject();

        Map<Integer, List<Subject>> byParent = new HashMap<>();
        for (Subject s : all) {
            int pid = s.getParentId() != null ? s.getParentId() : 0;
            byParent.computeIfAbsent(pid, k -> new ArrayList<>()).add(s);
        }

        List<Map<String, Object>> tree = new ArrayList<>();
        List<Subject> roots = byParent.getOrDefault(0, Collections.emptyList());
        for (Subject root : roots) {
            tree.add(buildNode(root, byParent));
        }
        return tree;
    }

    private Map<String, Object> buildNode(Subject subject, Map<Integer, List<Subject>> byParent) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", subject.getId());
        node.put("name", subject.getName());
        node.put("educationLevel", subject.getEducationLevel());
        node.put("educationLevelName", subject.getEducationLevelName());
        node.put("category", subject.getCategory());
        node.put("subjectType", subject.getSubjectType());
        node.put("parentId", subject.getParentId());
        node.put("level", subject.getLevel());
        node.put("levelName", subject.getLevelName());
        node.put("description", subject.getDescription());

        List<Subject> children = byParent.getOrDefault(subject.getId(), Collections.emptyList());
        if (!children.isEmpty()) {
            List<Map<String, Object>> childNodes = new ArrayList<>();
            for (Subject child : children) {
                childNodes.add(buildNode(child, byParent));
            }
            node.put("children", childNodes);
        } else {
            node.put("children", Collections.emptyList());
        }
        return node;
    }

    @Override
    public List<Subject> allSubject() {
        return subjectMapper.allSubject();
    }

    @Override
    public Integer levelBySubjectId(Integer id) {
        return this.selectById(id).getLevel();
    }

    @Override
    public PageInfo<Subject> page(SubjectPageRequestVM requestVM) {
        return PageHelper.startPage(requestVM.getPageIndex(), requestVM.getPageSize(), "education_level, level, item_order").doSelectPageInfo(() ->
                subjectMapper.page(requestVM)
        );
    }
}

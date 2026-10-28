package com.mindskip.xzs.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindskip.xzs.domain.Subject;
import com.mindskip.xzs.repository.SubjectMapper;
import com.mindskip.xzs.service.SubjectCatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;

@Service
public class SubjectCatalogServiceImpl implements SubjectCatalogService {

    private static final Logger log = LoggerFactory.getLogger(SubjectCatalogServiceImpl.class);

    private final SubjectMapper subjectMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 门类固定 ID 偏移
     */
    private static final int CATEGORY_ID_BASE = 100;      // 100~113
    private static final int GROUP_ID_BASE   = 1000;       // 1000+
    private static final int MAJOR_ID_BASE   = 10000;      // 10000+

    public SubjectCatalogServiceImpl(SubjectMapper subjectMapper) {
        this.subjectMapper = subjectMapper;
    }

    @Override
    @Transactional
    public Map<String, Integer> importCatalog(Integer educationLevel) {
        if (educationLevel == null) educationLevel = 2; // default 本科
        List<CatalogCategory> catalog = loadCatalog();

        int catCount = 0, groupCount = 0, majorCount = 0;

        for (int ci = 0; ci < catalog.size(); ci++) {
            CatalogCategory cat = catalog.get(ci);
            int catId = CATEGORY_ID_BASE + ci;

            Subject catSubject = buildSubject(catId, cat.门类, educationLevel, "本科", 1, null, cat.门类, 0);
            if (subjectMapper.selectByPrimaryKey(catId) == null) {
                subjectMapper.insert(catSubject);
                catCount++;
            }

            for (int gi = 0; gi < cat.专业类.size(); gi++) {
                CatalogGroup group = cat.专业类.get(gi);
                int groupId = GROUP_ID_BASE + ci * 100 + gi;

                Subject grpSubject = buildSubject(groupId, group.名称, educationLevel, "本科", 2, catId, cat.门类, gi);
                if (subjectMapper.selectByPrimaryKey(groupId) == null) {
                    subjectMapper.insert(grpSubject);
                    groupCount++;
                }

                for (int mi = 0; mi < group.专业.size(); mi++) {
                    String majorName = group.专业.get(mi);
                    int majorId = MAJOR_ID_BASE + ci * 1000 + gi * 100 + mi;

                    Subject mjrSubject = buildSubject(majorId, majorName, educationLevel, "本科", 3, groupId, cat.门类, mi);
                    if (subjectMapper.selectByPrimaryKey(majorId) == null) {
                        subjectMapper.insert(mjrSubject);
                        majorCount++;
                    }
                }
            }
        }

        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("categories", catCount);
        stats.put("groups", groupCount);
        stats.put("majors", majorCount);
        return stats;
    }

    @Override
    public Map<String, Integer> getCatalogStats() {
        List<CatalogCategory> catalog = loadCatalog();
        int categories = catalog.size();
        int groups = 0;
        int majors = 0;
        for (CatalogCategory cat : catalog) {
            groups += cat.专业类.size();
            for (CatalogGroup g : cat.专业类) {
                majors += g.专业.size();
            }
        }
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("categories", categories);
        stats.put("groups", groups);
        stats.put("majors", majors);
        return stats;
    }

    private Subject buildSubject(Integer id, String name, Integer eduLevel, String eduLevelName,
                                 Integer subjectType, Integer parentId, String category, int order) {
        Subject s = new Subject();
        s.setId(id);
        s.setName(name);
        s.setEducationLevel(eduLevel);
        s.setEducationLevelName(eduLevelName);
        s.setLevel(subjectType == 4 ? 1 : null);  // 课程默认第一年, 其他类型不设年级
        s.setLevelName(eduLevelName + (subjectType == 4 ? "第一年" : ""));
        s.setSubjectType(subjectType);
        s.setParentId(parentId);
        s.setCategory(category);
        s.setItemOrder(order);
        s.setDeleted(false);
        return s;
    }

    private List<CatalogCategory> loadCatalog() {
        try {
            InputStream is = new ClassPathResource("catalog/本科专业目录.json").getInputStream();
            return objectMapper.readValue(is, new TypeReference<List<CatalogCategory>>() {});
        } catch (Exception e) {
            log.error("加载学科目录 JSON 失败", e);
            return Collections.emptyList();
        }
    }

    // ── JSON 映射类 ──
    private static class CatalogCategory {
        public String 门类;
        public List<CatalogGroup> 专业类;
    }
    private static class CatalogGroup {
        public String 名称;
        public List<String> 专业;
    }
}

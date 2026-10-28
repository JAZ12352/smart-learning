package com.mindskip.xzs.service;

import java.util.Map;

/**
 * 学科目录导入服务 — 从 JSON 资源文件加载教育部本科专业目录
 */
public interface SubjectCatalogService {

    /**
     * 将 catalog/本科专业目录.json 导入数据库
     * @param educationLevel 学历层次 (默认2=本科)
     * @return 统计: { categories, groups, majors }
     */
    Map<String, Integer> importCatalog(Integer educationLevel);

    /**
     * 获取导入统计（不执行导入）
     */
    Map<String, Integer> getCatalogStats();
}

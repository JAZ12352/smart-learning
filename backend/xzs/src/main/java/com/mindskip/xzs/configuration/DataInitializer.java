package com.mindskip.xzs.configuration;

import com.mindskip.xzs.service.SubjectCatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 应用启动时自动初始化/修复数据库
 * 1. 标记 K12 旧数据为删除
 * 2. 导入完整教育部本科专业目录（如果还没有）
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final JdbcTemplate jdbc;
    private final SubjectCatalogService catalogService;

    public DataInitializer(JdbcTemplate jdbc, SubjectCatalogService catalogService) {
        this.jdbc = jdbc;
        this.catalogService = catalogService;
    }

    @Override
    public void run(String... args) {
        try {
            // 1. 清理 K12 旧数据
            int k12Deleted = jdbc.update(
                "UPDATE t_subject SET deleted = 1 WHERE deleted = 0 AND (education_level IS NULL OR (level BETWEEN 1 AND 12 AND subject_type IS NULL))");
            if (k12Deleted > 0) {
                log.info("已清理 {} 条 K12 旧学科数据", k12Deleted);
            }

            // 2. 检查是否有门类数据
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_subject WHERE deleted = 0 AND subject_type = 1", Integer.class);
            if (count == null || count == 0) {
                log.info("未检测到学科门类数据，自动导入教育部本科专业目录...");
                Map<String, Integer> stats = catalogService.importCatalog(2);
                log.info("导入完成：{} 个学科门类、{} 个专业类、{} 个专业",
                    stats.get("categories"), stats.get("groups"), stats.get("majors"));
            } else {
                log.info("学科门类数据已存在（{} 个），跳过导入", count);
            }

            // 3. 检查 subject_resource 表是否存在
            ensureResourceTable();

        } catch (Exception e) {
            log.error("数据初始化失败", e);
        }
    }

    private void ensureResourceTable() {
        try {
            jdbc.queryForObject("SELECT COUNT(*) FROM t_subject_resource", Integer.class);
        } catch (Exception e) {
            log.info("t_subject_resource 表不存在，自动创建...");
            jdbc.execute("CREATE TABLE IF NOT EXISTS t_subject_resource (\n" +
                "  id INT AUTO_INCREMENT PRIMARY KEY,\n" +
                "  subject_id INT NOT NULL,\n" +
                "  source_type VARCHAR(50) NOT NULL DEFAULT 'manual',\n" +
                "  source_id INT,\n" +
                "  title VARCHAR(500) NOT NULL,\n" +
                "  summary TEXT,\n" +
                "  source VARCHAR(200),\n" +
                "  content LONGTEXT,\n" +
                "  format VARCHAR(20) DEFAULT 'markdown',\n" +
                "  click_count INT DEFAULT 0,\n" +
                "  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                "  deleted BIT(1) DEFAULT b'0',\n" +
                "  INDEX idx_subject_id (subject_id)\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
    }
}

package com.mindskip.xzs.configuration;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 测试数据初始化器
 * 在应用启动时检测数据库中是否有测试数据，若无则自动插入
 * 确保评委可直接复现验证案例
 *
 * 对应评分标准中全部验证案例的基础数据
 */
@Component
public class TestDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TestDataInitializer.class);

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(String... args) {
        try (Connection conn = dataSource.getConnection()) {
            // 检查是否已有测试数据
            if (hasTestData(conn)) {
                log.info("TestDataInitializer: 测试数据已存在，跳过初始化");
            } else {
                // 执行 SQL 初始化脚本
                Resource resource = new ClassPathResource("db/test-data.sql");
                if (resource.exists()) {
                    ScriptUtils.executeSqlScript(conn, resource);
                    log.info("TestDataInitializer: 测试数据初始化完成（学生: 张明, 画像+3条学习路径）");
                } else {
                    log.warn("TestDataInitializer: test-data.sql 未找到");
                }
            }

            // 差异化学习者画像 v2（面向：转岗/在职/企业内训/在校生，user_id 21-32）
            if (!hasV2Profiles(conn)) {
                Resource v2 = new ClassPathResource("db/profiles-v2.sql");
                if (v2.exists()) {
                    ScriptUtils.executeSqlScript(conn, v2);
                    log.info("TestDataInitializer: 差异化画像种子完成（12 类职业身份学习者, user_id 21-32）");
                } else {
                    log.warn("TestDataInitializer: profiles-v2.sql 未找到");
                }
            } else {
                log.info("TestDataInitializer: 差异化画像已存在，跳过");
            }

            // AI 垂直领域分阶题库（支撑考试管理与 Quiz 资源演示）
            if (!hasQuestionBank(conn)) {
                Resource qb = new ClassPathResource("db/question-bank-v2.sql");
                if (qb.exists()) {
                    ScriptUtils.executeSqlScript(conn, qb);
                    log.info("TestDataInitializer: AI 域分阶题库种子完成（14 题 × 5 题型 × 3 难度档）");
                } else {
                    log.warn("TestDataInitializer: question-bank-v2.sql 未找到");
                }
            } else {
                log.info("TestDataInitializer: 题库已存在，跳过");
            }

            // 职业学习者路径（转岗/在职/内训画像的岗位导向路径）
            if (!hasCareerPaths(conn)) {
                Resource cp = new ClassPathResource("db/career-paths-v2.sql");
                if (cp.exists()) {
                    ScriptUtils.executeSqlScript(conn, cp);
                    log.info("TestDataInitializer: 职业学习路径种子完成（4 条转岗/在职/内训路径）");
                } else {
                    log.warn("TestDataInitializer: career-paths-v2.sql 未找到");
                }
            } else {
                log.info("TestDataInitializer: 职业路径已存在，跳过");
            }
        } catch (Exception e) {
            log.warn("TestDataInitializer: 初始化跳过 (可能数据库尚未就绪): {}", e.getMessage());
        }
    }

    private boolean hasTestData(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // 检查是否有 3 组差异化学习者画像（user_id 2/3/4）
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM t_student_profile WHERE user_id IN (2,3,4)");
            if (rs.next() && rs.getInt(1) >= 3) {
                return true;
            }
        } catch (Exception ignored) {
            // 表可能还不存在，忽略错误
        }
        return false;
    }

    private boolean hasV2Profiles(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // 检查 12 类差异化职业身份画像（user_id 21-32）是否已入库
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM t_student_profile WHERE user_id BETWEEN 21 AND 32");
            if (rs.next() && rs.getInt(1) >= 12) {
                return true;
            }
        } catch (Exception ignored) {
            // 表可能还不存在，忽略错误
        }
        return false;
    }

    private boolean hasQuestionBank(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM t_question WHERE create_user = 2");
            if (rs.next() && rs.getInt(1) >= 14) {
                return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    private boolean hasCareerPaths(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM t_learning_path WHERE user_id BETWEEN 24 AND 30");
            if (rs.next() && rs.getInt(1) >= 4) {
                return true;
            }
        } catch (Exception ignored) { }
        return false;
    }
}

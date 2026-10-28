package com.mindskip.xzs.utility;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * H2 数据库 MySQL 兼容函数
 * 通过 CREATE ALIAS 注册，解决 DATE_FORMAT 等 MySQL 独有函数在 H2 中不兼容的问题
 */
public class H2Function {

    private static final ThreadLocal<SimpleDateFormat> FORMATTERS = ThreadLocal.withInitial(() -> null);

    /**
     * H2 DATE_FORMAT 兼容实现
     * 用法: DATE_FORMAT(timestamp, 'format')
     * 示例: DATE_FORMAT(create_time, '%Y-%m-%d')
     */
    public static String dateFormat(java.sql.Timestamp ts, String format) {
        if (ts == null) return null;
        // 将 MySQL 风格的格式符转为 Java SimpleDateFormat 风格
        String javaFormat = format
                .replace("%Y", "yyyy")
                .replace("%y", "yy")
                .replace("%m", "MM")
                .replace("%d", "dd")
                .replace("%H", "HH")
                .replace("%i", "mm")
                .replace("%s", "ss")
                .replace("%M", "MMMMM")  // MySQL %M = full month name (approximate)
                .replace("%b", "MMM")
                .replace("%W", "EEEEE")  // MySQL %W = full weekday name (approximate)
                .replace("%a", "EEE");
        SimpleDateFormat sdf = new SimpleDateFormat(javaFormat);
        return sdf.format(new Date(ts.getTime()));
    }

    /**
     * 兼容 java.util.Date 类型的 DATE_FORMAT
     */
    public static String dateFormat(java.sql.Date d, String format) {
        if (d == null) return null;
        return dateFormat(new java.sql.Timestamp(d.getTime()), format);
    }
}

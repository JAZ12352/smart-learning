package com.mindskip.xzs.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 学科资源关联表
 * 记录学科下的所有资源（AI生成、知识库文档、外部链接等）
 */
public class SubjectResource implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;

    private Integer subjectId;

    /**
     * 资源来源类型: generated/knowledge_base/document/url
     */
    private String sourceType;

    /**
     * 关联的外部资源ID（如 t_generated_resource.id / t_kb_document.id）
     */
    private Integer sourceId;

    private String title;

    private String summary;

    private String source;

    /**
     * 完整的资源内容（冗余存储，方便快速展示）
     */
    private String content;

    /**
     * 内容格式: markdown/html/plain
     */
    private String format;

    private Integer clickCount;

    private Date createTime;

    private Boolean deleted;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType == null ? null : sourceType.trim();
    }

    public Integer getSourceId() {
        return sourceId;
    }

    public void setSourceId(Integer sourceId) {
        this.sourceId = sourceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? null : title.trim();
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary == null ? null : summary.trim();
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source == null ? null : source.trim();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format == null ? null : format.trim();
    }

    public Integer getClickCount() {
        return clickCount;
    }

    public void setClickCount(Integer clickCount) {
        this.clickCount = clickCount;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}

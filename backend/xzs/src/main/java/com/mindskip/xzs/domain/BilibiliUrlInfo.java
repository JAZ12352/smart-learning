package com.mindskip.xzs.domain;

/**
 * B站视频URL解析结果
 */
public class BilibiliUrlInfo {

    private String aid;          // AV号，如 "55114968"
    private String bvid;         // BV号，如 "BV1BU4y1k7CV"
    private int page = 1;        // 分P页码，默认第1P
    private String originalUrl;  // 原始URL
    private boolean valid;       // 是否为有效的B站视频链接

    public String getAid() {
        return aid;
    }

    public void setAid(String aid) {
        this.aid = aid;
    }

    public String getBvid() {
        return bvid;
    }

    public void setBvid(String bvid) {
        this.bvid = bvid;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    @Override
    public String toString() {
        return "BilibiliUrlInfo{" +
                "aid='" + aid + '\'' +
                ", bvid='" + bvid + '\'' +
                ", page=" + page +
                ", valid=" + valid +
                '}';
    }
}

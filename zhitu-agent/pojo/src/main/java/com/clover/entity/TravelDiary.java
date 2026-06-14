package com.clover.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 旅行日记实体类
 */
@Data
public class TravelDiary {
    /**
     * 日记ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 日记标题
     */
    private String title;

    /**
     * 日记内容
     */
    private String content;

    /**
     * 旅行地点
     */
    private String location;

    /**
     * 旅行日期
     */
    private LocalDate travelDate;

    /**
     * 天气状况
     */
    private String weather;

    /**
     * 心情
     */
    private String mood;

    /**
     * 图片URL列表(JSON字符串)
     */
    private String images;

    /**
     * 标签(逗号分隔)
     */
    private String tags;

    /**
     * 封面图片URL
     */
    private String coverImage;

    /**
     * 是否公开: 0-私密, 1-公开
     */
    private Integer isPublic;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 点赞次数
     */
    private Integer likeCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

package com.clover.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 旅行日记DTO - 用于创建/更新旅行日记
 */
@Data
public class TravelDiaryDTO {
    /**
     * 日记ID（更新时使用）
     */
    private Long id;

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
     * 图片URL列表
     */
    private List<String> images;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 封面图片URL
     */
    private String coverImage;

    /**
     * 是否公开: 0-私密, 1-公开
     */
    private Integer isPublic;
}

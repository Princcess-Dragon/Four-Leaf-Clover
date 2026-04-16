package com.clover.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 任务实体类
 */
@Data
public class Task {
    /**
     * 任务ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 任务标题
     */
    private String title;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 状态: 0-待完成, 1-已完成, 2-已取消
     */
    private Integer status;

    /**
     * 优先级: 0-普通, 1-重要, 2-紧急
     */
    private Integer priority;

    /**
     * 截止时间
     */
    private LocalDateTime dueDate;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 状态名称
     */
    public String getStatusName() {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待完成";
            case 1: return "已完成";
            case 2: return "已取消";
            default: return "未知";
        }
    }

    /**
     * 优先级名称
     */
    public String getPriorityName() {
        if (priority == null) return "普通";
        switch (priority) {
            case 0: return "普通";
            case 1: return "重要";
            case 2: return "紧急";
            default: return "普通";
        }
    }
}

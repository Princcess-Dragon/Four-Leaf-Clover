package com.clover.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 任务管理服务
 */
@Slf4j
@Service
public class TaskService {

    // 存储用户任务
    private final Map<String, List<Task>> userTasks = new ConcurrentHashMap<>();
    private final AtomicLong taskIdGenerator = new AtomicLong(1);

    record Task(Long id, String description, boolean completed, LocalDateTime createdAt, LocalDateTime dueDate) {
    }

    @PostConstruct
    public void init() {
        log.info("任务管理服务初始化完成");
    }

    /**
     * 处理任务相关请求
     */
    public String handleTaskRequest(String message) {
        String lowerMessage = message.toLowerCase();
        StringBuilder result = new StringBuilder();

        try {
            // 创建任务
            if (lowerMessage.contains("创建") || lowerMessage.contains("添加") || lowerMessage.contains("新增")) {
                result.append(createTask(message));
            }
            // 查询任务
            else if (lowerMessage.contains("查询") || lowerMessage.contains("查看") || lowerMessage.contains("列表")) {
                result.append(listTasks("default"));
            }
            // 完成任务
            else if (lowerMessage.contains("完成") || lowerMessage.contains("标记")) {
                result.append(completeTask(message));
            }
            // 删除任务
            else if (lowerMessage.contains("删除") || lowerMessage.contains("移除")) {
                result.append(deleteTask(message));
            }
            // 默认查询
            else {
                result.append(listTasks("default"));
            }
        } catch (Exception e) {
            log.error("处理任务请求失败", e);
            result.append("处理任务请求时出错：").append(e.getMessage());
        }

        return result.toString();
    }

    /**
     * 创建任务
     */
    private String createTask(String message) {
        // 简单提取任务描述
        String description = message.replaceAll(".*(创建|添加|新增)\\s*(任务)?\\s*", "");
        if (description.equals(message)) {
            description = message.replaceAll(".*(帮我|请|要)\\s*", "");
        }

        Task task = new Task(
            taskIdGenerator.getAndIncrement(),
            description.isEmpty() ? "新任务" : description,
            false,
            LocalDateTime.now(),
            null
        );

        userTasks.computeIfAbsent("default", k -> new ArrayList<>()).add(task);

        return String.format("✅ 任务已创建：\n- ID: %d\n- 描述: %s\n- 时间: %s",
            task.id(), task.description(),
            task.createdAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    /**
     * 列出任务
     */
    private String listTasks(String userId) {
        List<Task> tasks = userTasks.getOrDefault(userId, new ArrayList<>());

        if (tasks.isEmpty()) {
            return "📋 当前没有任务。";
        }

        StringBuilder result = new StringBuilder("📋 你的任务清单：\n\n");
        for (Task task : tasks) {
            String status = task.completed() ? "✅" : "⏳";
            result.append(String.format("%s [%d] %s\n", status, task.id(), task.description()));
        }

        result.append(String.format("\n总计: %d 个任务", tasks.size()));
        return result.toString();
    }

    /**
     * 完成任务
     */
    private String completeTask(String message) {
        // 提取任务ID
        Long taskId = extractTaskId(message);
        if (taskId == null) {
            return "请提供要完成的任务ID。";
        }

        List<Task> tasks = userTasks.getOrDefault("default", new ArrayList<>());
        Optional<Task> taskOpt = tasks.stream()
            .filter(t -> t.id().equals(taskId))
            .findFirst();

        if (taskOpt.isPresent()) {
            Task oldTask = taskOpt.get();
            Task newTask = new Task(oldTask.id(), oldTask.description(), true, oldTask.createdAt(), oldTask.dueDate());
            tasks.set(tasks.indexOf(oldTask), newTask);
            return String.format("✅ 任务 %d 已标记为完成！", taskId);
        }

        return String.format("❌ 未找到ID为 %d 的任务。", taskId);
    }

    /**
     * 删除任务
     */
    private String deleteTask(String message) {
        Long taskId = extractTaskId(message);
        if (taskId == null) {
            return "请提供要删除的任务ID。";
        }

        List<Task> tasks = userTasks.getOrDefault("default", new ArrayList<>());
        boolean removed = tasks.removeIf(t -> t.id().equals(taskId));

        if (removed) {
            return String.format("🗑️ 任务 %d 已删除。", taskId);
        }

        return String.format("❌ 未找到ID为 %d 的任务。", taskId);
    }

    /**
     * 从消息中提取任务ID
     */
    private Long extractTaskId(String message) {
        try {
            String[] parts = message.split("\\s+");
            for (String part : parts) {
                try {
                    return Long.parseLong(part.replaceAll("[^0-9]", ""));
                } catch (NumberFormatException e) {
                    // 继续尝试
                }
            }
        } catch (Exception e) {
            log.warn("提取任务ID失败", e);
        }
        return null;
    }
}

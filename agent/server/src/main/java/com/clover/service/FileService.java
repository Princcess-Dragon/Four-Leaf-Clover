package com.clover.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件操作服务
 */
@Slf4j
@Service
public class FileService {

    private static final String BASE_DIR = "agent-files";
    private final Map<String, String> fileHistory = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            Path basePath = Paths.get(BASE_DIR);
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
            }
            log.info("文件服务初始化完成，目录: {}", basePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("创建文件目录失败", e);
        }
    }

    /**
     * 处理文件操作请求
     */
    public String handleFileRequest(String message) {
        String lowerMessage = message.toLowerCase();
        StringBuilder result = new StringBuilder();

        try {
            // 创建文件
            if (lowerMessage.contains("创建") && lowerMessage.contains("文件")) {
                result.append(createFile(message));
            }
            // 读取文件
            else if (lowerMessage.contains("读取") || lowerMessage.contains("查看") || lowerMessage.contains("打开")) {
                result.append(readFile(message));
            }
            // 列出文件
            else if (lowerMessage.contains("列表") || lowerMessage.contains("所有")) {
                result.append(listFiles());
            }
            // 默认列出文件
            else {
                result.append(listFiles());
            }
        } catch (Exception e) {
            log.error("处理文件请求失败", e);
            result.append("处理文件请求时出错：").append(e.getMessage());
        }

        return result.toString();
    }

    /**
     * 创建文件
     */
    private String createFile(String message) {
        try {
            // 提取文件名和内容
            String fileName = extractFileName(message);
            String content = extractFileContent(message);

            if (fileName == null || fileName.isEmpty()) {
                fileName = "file_" + System.currentTimeMillis() + ".txt";
            }

            if (content == null || content.isEmpty()) {
                content = "创建时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }

            Path filePath = Paths.get(BASE_DIR, fileName);
            // Java 8 compatible file writing
            try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(filePath)) {
                writer.write(content);
            }

            fileHistory.put(fileName, content);

            return String.format("📄 文件已创建：\n- 文件名: %s\n- 大小: %d 字节\n- 路径: %s",
                fileName, content.length(), filePath.toAbsolutePath());

        } catch (Exception e) {
            log.error("创建文件失败", e);
            return "创建文件失败：" + e.getMessage();
        }
    }

    /**
     * 读取文件
     */
    private String readFile(String message) {
        try {
            String fileName = extractFileName(message);
            if (fileName == null || fileName.isEmpty()) {
                return "请提供要读取的文件名。";
            }

            Path filePath = Paths.get(BASE_DIR, fileName);
            if (!Files.exists(filePath)) {
                return String.format("❌ 文件 %s 不存在。", fileName);
            }

            String content = new String(Files.readAllBytes(filePath));
            return String.format("📄 文件 %s 内容：\n\n%s", fileName, content);

        } catch (Exception e) {
            log.error("读取文件失败", e);
            return "读取文件失败：" + e.getMessage();
        }
    }

    /**
     * 列出文件
     */
    private String listFiles() {
        try {
            Path basePath = Paths.get(BASE_DIR);
            if (!Files.exists(basePath)) {
                return "📁 文件目录不存在。";
            }

            List<Path> files = Files.list(basePath)
                .filter(Files::isRegularFile)
                .collect(java.util.stream.Collectors.toList());

            if (files.isEmpty()) {
                return "📁 当前没有文件。";
            }

            StringBuilder result = new StringBuilder("📁 文件列表：\n\n");
            for (Path file : files) {
                try {
                    long size = Files.size(file);
                    result.append(String.format("- %s (%d 字节)\n", file.getFileName(), size));
                } catch (IOException e) {
                    result.append(String.format("- %s\n", file.getFileName()));
                }
            }

            result.append(String.format("\n总计: %d 个文件", files.size()));
            return result.toString();

        } catch (Exception e) {
            log.error("列出文件失败", e);
            return "列出文件失败：" + e.getMessage();
        }
    }

    /**
     * 提取文件名
     */
    private String extractFileName(String message) {
        // 简单提取，实际可以使用更复杂的NLP
        String[] patterns = {"文件", "名为", "叫做", "file"};
        for (String pattern : patterns) {
            int index = message.indexOf(pattern);
            if (index != -1) {
                String after = message.substring(index + pattern.length()).trim();
                // 提取第一个词作为文件名
                String[] words = after.split("\\s+");
                if (words.length > 0) {
                    return words[0].replaceAll("[^a-zA-Z0-9._-]", "");
                }
            }
        }
        return null;
    }

    /**
     * 提取文件内容
     */
    private String extractFileContent(String message) {
        // 简单提取内容
        int contentIndex = message.indexOf("内容");
        if (contentIndex != -1) {
            return message.substring(contentIndex + 2).trim();
        }
        
        contentIndex = message.indexOf("为");
        if (contentIndex != -1) {
            return message.substring(contentIndex + 1).trim();
        }

        return null;
    }
}

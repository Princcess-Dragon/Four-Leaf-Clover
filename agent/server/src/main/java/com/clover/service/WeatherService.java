package com.clover.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 天气查询服务
 */
@Slf4j
@Service
public class WeatherService {

    @Value("${weather.api.key:}")
    private String apiKey;

    @Value("${weather.api.url:https://api.openweathermap.org/data/2.5/weather}")
    private String apiUrl;

    private WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    // 模拟天气数据（当没有API key时使用）
    private static final String[] WEATHER_CONDITIONS = {"晴", "多云", "阴", "小雨", "中雨", "大雨", "雪"};

    @PostConstruct
    public void init() {
        webClient = WebClient.builder()
            .baseUrl(apiUrl)
            .build();
        log.info("天气服务初始化完成");
    }

    /**
     * 处理天气查询请求
     */
    public String handleWeatherRequest(String message) {
        try {
            // 提取城市名
            String city = extractCity(message);
            if (city == null || city.isEmpty()) {
                city = "北京"; // 默认城市
            }

            log.info("查询天气，城市: {}", city);

            // 如果有API key，调用真实API
            if (apiKey != null && !apiKey.isEmpty()) {
                return getRealWeather(city);
            } else {
                // 否则使用模拟数据
                return getMockWeather(city);
            }

        } catch (Exception e) {
            log.error("查询天气失败", e);
            return "查询天气时出错：" + e.getMessage();
        }
    }

    /**
     * 获取真实天气数据
     */
    private String getRealWeather(String city) {
        try {
            String url = String.format("%s?q=%s&appid=%s&units=metric&lang=zh_cn",
                apiUrl, city, apiKey);

            String response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode jsonNode = objectMapper.readTree(response);

            String description = jsonNode.path("weather").get(0).path("description").asText();
            double temp = jsonNode.path("main").path("temp").asDouble();
            int humidity = jsonNode.path("main").path("humidity").asInt();
            double windSpeed = jsonNode.path("wind").path("speed").asDouble();

            return formatWeatherInfo(city, description, temp, humidity, windSpeed);

        } catch (Exception e) {
            log.error("获取真实天气失败", e);
            return getMockWeather(city); // 降级到模拟数据
        }
    }

    /**
     * 获取模拟天气数据
     */
    private String getMockWeather(String city) {
        String condition = WEATHER_CONDITIONS[random.nextInt(WEATHER_CONDITIONS.length)];
        double temp = 15 + random.nextInt(20); // 15-35度
        int humidity = 40 + random.nextInt(50); // 40-90%
        double windSpeed = 1 + random.nextInt(10); // 1-10 m/s

        return formatWeatherInfo(city, condition, temp, humidity, windSpeed);
    }

    /**
     * 格式化天气信息
     */
    private String formatWeatherInfo(String city, String condition, double temp, int humidity, double windSpeed) {
        String travelAdvice = generateTravelAdvice(condition, temp);

        return String.format(
            "🌤️ %s天气\n" +
            "━━━━━━━━━━━━━━\n" +
            "天气: %s\n" +
            "温度: %.1f°C\n" +
            "湿度: %d%%\n" +
            "风速: %.1f m/s\n" +
            "━━━━━━━━━━━━━━\n" +
            "💡 出行建议: %s\n" +
            "更新时间: %s",
            city, condition, temp, humidity, windSpeed,
            travelAdvice,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
    }

    /**
     * 生成出行建议
     */
    private String generateTravelAdvice(String condition, double temp) {
        StringBuilder advice = new StringBuilder();

        // 根据天气条件给建议
        if (condition.contains("雨")) {
            advice.append("记得带伞！");
        } else if (condition.contains("雪")) {
            advice.append("路滑小心，注意保暖！");
        } else if (condition.contains("晴")) {
            advice.append("天气不错，适合出行！");
        }

        // 根据温度给建议
        if (temp < 10) {
            advice.append(" 天气较冷，注意添衣保暖。");
        } else if (temp > 30) {
            advice.append(" 天气炎热，注意防暑降温。");
        }

        return advice.length() > 0 ? advice.toString() : "适宜出行。";
    }

    /**
     * 提取城市名
     */
    private String extractCity(String message) {
        // 简单提取城市名
        String[] patterns = {"天气", "城市", "地方"};
        for (String pattern : patterns) {
            int index = message.indexOf(pattern);
            if (index != -1) {
                // 提取前面的词作为城市名
                String before = message.substring(0, index).trim();
                String[] words = before.split("\\s+");
                if (words.length > 0) {
                    return words[words.length - 1];
                }
            }
        }
        return null;
    }
}

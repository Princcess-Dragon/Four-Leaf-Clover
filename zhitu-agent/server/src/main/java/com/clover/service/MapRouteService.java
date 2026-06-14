package com.clover.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 地图路线服务
 * 使用高德地图API提供路线规划功能
 */
@Slf4j
@Service
public class MapRouteService {

    @Value("${amap.api.key:your-amap-api-key}")
    private String amapApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取两个地点之间的驾车路线
     * @param origin 起点（经纬度或地址）
     * @param destination 终点（经纬度或地址）
     * @return 路线信息
     */
    public Map<String, Object> getDrivingRoute(String origin, String destination) {
        try {
            log.info("开始规划驾车路线: {} -> {}", origin, destination);
            
            // 如果输入的是地址，先进行地理编码
            String originCoords = geocodeAddress(origin);
            String destCoords = geocodeAddress(destination);

            if (originCoords == null || destCoords == null) {
                String errorMsg = buildGeocodeErrorMessage(origin, destination, originCoords, destCoords);
                return createErrorResponse(errorMsg);
            }

            // 调用高德地图驾车路线规划API
            String url = String.format(
                "https://restapi.amap.com/v3/direction/driving?origin=%s&destination=%s&key=%s&extensions=all",
                originCoords, destCoords, amapApiKey
            );

            log.debug("调用驾车路线API: {}", url);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String responseBody = response.getBody();
            log.info("驾车路线API响应: {}", responseBody);
            
            JsonNode rootNode = objectMapper.readTree(responseBody);

            if (rootNode.get("status").asInt() != 1) {
                String errorMsg = rootNode.has("info") ? rootNode.get("info").asText() : "未知错误";
                log.error("驾车路线API返回错误 - status: {}, info: {}", rootNode.get("status").asText(), errorMsg);
                return createErrorResponse("路线规划失败：" + errorMsg);
            }

            log.info("驾车路线规划成功");
            return parseDrivingRoute(rootNode);

        } catch (Exception e) {
            log.error("获取驾车路线失败", e);
            return createErrorResponse("获取路线失败：" + e.getMessage());
        }
    }

    /**
     * 获取步行路线
     * @param origin 起点
     * @param destination 终点
     * @return 路线信息
     */
    public Map<String, Object> getWalkingRoute(String origin, String destination) {
        try {
            log.info("开始规划步行路线: {} -> {}", origin, destination);
            
            String originCoords = geocodeAddress(origin);
            String destCoords = geocodeAddress(destination);

            if (originCoords == null || destCoords == null) {
                String errorMsg = buildGeocodeErrorMessage(origin, destination, originCoords, destCoords);
                return createErrorResponse(errorMsg);
            }

            String url = String.format(
                "https://restapi.amap.com/v3/direction/walking?origin=%s&destination=%s&key=%s&extensions=all",
                originCoords, destCoords, amapApiKey
            );

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode rootNode = objectMapper.readTree(response.getBody());

            if (rootNode.get("status").asInt() != 1) {
                return createErrorResponse("路线规划失败：" + rootNode.get("info").asText());
            }

            log.info("步行路线规划成功");
            return parseWalkingRoute(rootNode);

        } catch (Exception e) {
            log.error("获取步行路线失败", e);
            return createErrorResponse("获取路线失败：" + e.getMessage());
        }
    }

    /**
     * 生成高德地图静态图URL
     * @param origin 起点
     * @param destination 终点
     * @param width 图片宽度（像素）
     * @param height 图片高度（像素）
     * @return 静态图URL
     */
    public String generateStaticMapUrl(String origin, String destination, int width, int height) {
        try {
            log.info("生成静态地图: {} -> {}", origin, destination);
            
            String originCoords = geocodeAddress(origin);
            String destCoords = geocodeAddress(destination);

            if (originCoords == null || destCoords == null) {
                log.warn("无法解析地址，返回空URL");
                return null;
            }

            // 使用高德地图静态图API
            // 文档：https://lbs.amap.com/api/webservice/guide/api/staticmaps
            String staticMapUrl = String.format(
                "https://restapi.amap.com/v3/staticmap?origin=%s&destination=%s&zoom=13&size=%dx%d&markers=mid,,A:%s|mid,,B:%s&path=0.6,0xFF0000,5,:%s,%s&key=%s",
                originCoords,
                destCoords,
                width,
                height,
                originCoords,
                destCoords,
                originCoords,
                destCoords,
                amapApiKey
            );

            log.info("静态地图URL生成成功");
            return staticMapUrl;

        } catch (Exception e) {
            log.error("生成静态地图失败", e);
            return null;
        }
    }

    /**
     * 获取公交路线
     * @param origin 起点
     * @param destination 终点
     * @param city 城市代码或名称
     * @return 路线信息
     */
    public Map<String, Object> getTransitRoute(String origin, String destination, String city) {
        try {
            log.info("开始规划公交路线: {} -> {}, 城市: {}", origin, destination, city);
            
            String originCoords = geocodeAddress(origin);
            String destCoords = geocodeAddress(destination);

            if (originCoords == null || destCoords == null) {
                String errorMsg = buildGeocodeErrorMessage(origin, destination, originCoords, destCoords);
                return createErrorResponse(errorMsg);
            }

            String url = String.format(
                "https://restapi.amap.com/v3/direction/transit/integrated?origin=%s&destination=%s&city=%s&key=%s&extensions=all",
                originCoords, destCoords, city, amapApiKey
            );

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode rootNode = objectMapper.readTree(response.getBody());

            if (rootNode.get("status").asInt() != 1) {
                return createErrorResponse("路线规划失败：" + rootNode.get("info").asText());
            }

            log.info("公交路线规划成功");
            return parseTransitRoute(rootNode);

        } catch (Exception e) {
            log.error("获取公交路线失败", e);
            return createErrorResponse("获取路线失败：" + e.getMessage());
        }
    }

    /**
     * 地理编码：将地址转换为经纬度
     * @param address 地址
     * @return 经纬度字符串（格式：经度,纬度）
     */
    private String geocodeAddress(String address) {
        try {
            log.info("开始地理编码，地址: {}", address);
            
            // 检查API Key是否配置
            if (amapApiKey == null || amapApiKey.equals("your-amap-api-key")) {
                log.error("高德地图API Key未配置！请在application.yml中配置amap.api.key或设置环境变量AMAP_API_KEY");
                return null;
            }
            
            // 检查是否已经是经纬度格式
            if (address.matches("-?\\d+\\.\\d+,-?\\d+\\.\\d+")) {
                log.info("地址已是经纬度格式: {}", address);
                return address;
            }

            String url = String.format(
                "https://restapi.amap.com/v3/geocode/geo?address=%s&key=%s",
                address, amapApiKey
            );
            
            log.debug("调用地理编码API: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String responseBody = response.getBody();
            log.debug("地理编码API响应: {}", responseBody);
            
            JsonNode rootNode = objectMapper.readTree(responseBody);

            // 检查API返回状态
            int status = rootNode.get("status").asInt();
            String info = rootNode.get("info").asText();
            
            if (status != 1) {
                log.error("地理编码API返回错误 - status: {}, info: {}", status, info);
                
                // 401表示API Key无效
                if (status == 0 && "INVALID_USER_KEY".equals(info)) {
                    log.error("API Key无效！请检查高德地图API Key是否正确");
                }
                
                return null;
            }
            
            // 检查是否有地理编码结果
            if (rootNode.get("geocodes").size() == 0) {
                log.warn("无法解析地址: {}，API返回空结果。请尝试使用更详细的地址", address);
                return null;
            }
            
            String location = rootNode.get("geocodes").get(0).get("location").asText();
            log.info("地理编码成功: {} -> {}", address, location);
            return location;
            
        } catch (Exception e) {
            log.error("地理编码失败: {}", address, e);
            return null;
        }
    }

    /**
     * 解析驾车路线结果
     */
    private Map<String, Object> parseDrivingRoute(JsonNode rootNode) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "driving");

        // 检查是否有route数据
        if (!rootNode.has("route") || rootNode.get("route") == null) {
            log.error("API返回数据缺少route字段");
            return createErrorResponse("路线规划失败：API返回数据格式错误");
        }

        JsonNode routeNode = rootNode.get("route");
        
        // 检查是否有paths数组
        if (!routeNode.has("paths") || routeNode.get("paths") == null || routeNode.get("paths").size() == 0) {
            String errorMsg = routeNode.has("info") ? routeNode.get("info").asText() : "未找到可用路线";
            log.error("API返回数据缺少paths数组: {}", errorMsg);
            return createErrorResponse("路线规划失败：" + errorMsg);
        }

        JsonNode route = routeNode.get("paths").get(0);
        
        // 检查必要字段是否存在
        if (!route.has("distance") || !route.has("duration") || !route.has("steps")) {
            log.error("路线数据缺少必要字段");
            return createErrorResponse("路线规划失败：数据不完整");
        }
        
        // 基本信息
        result.put("distance", route.get("distance").asText() + "米");
        result.put("duration", formatDuration(route.get("duration").asInt()));
        
        // 路线步骤
        List<Map<String, String>> steps = new ArrayList<>();
        JsonNode stepsNode = route.get("steps");
        for (JsonNode step : stepsNode) {
            Map<String, String> stepInfo = new HashMap<>();
            stepInfo.put("instruction", step.has("instruction") ? step.get("instruction").asText() : "");
            stepInfo.put("distance", step.has("distance") ? step.get("distance").asText() + "米" : "");
            stepInfo.put("duration", step.has("duration") ? formatDuration(step.get("duration").asInt()) : "");
            steps.add(stepInfo);
        }
        result.put("steps", steps);

        // 路径坐标（用于地图显示）
        if (route.has("polyline")) {
            result.put("path", route.get("polyline").asText());
        } else {
            result.put("path", "");
            log.warn("路线数据缺少polyline字段，无法在地图上显示");
        }

        return result;
    }

    /**
     * 解析步行路线结果
     */
    private Map<String, Object> parseWalkingRoute(JsonNode rootNode) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "walking");

        // 检查是否有route数据
        if (!rootNode.has("route") || rootNode.get("route") == null) {
            log.error("API返回数据缺少route字段");
            return createErrorResponse("路线规划失败：API返回数据格式错误");
        }

        JsonNode routeNode = rootNode.get("route");
        
        // 检查是否有paths数组
        if (!routeNode.has("paths") || routeNode.get("paths") == null || routeNode.get("paths").size() == 0) {
            String errorMsg = routeNode.has("info") ? routeNode.get("info").asText() : "未找到可用路线";
            log.error("API返回数据缺少paths数组: {}", errorMsg);
            return createErrorResponse("路线规划失败：" + errorMsg);
        }

        JsonNode route = routeNode.get("paths").get(0);
        
        // 检查必要字段
        if (!route.has("distance") || !route.has("duration") || !route.has("steps")) {
            log.error("路线数据缺少必要字段");
            return createErrorResponse("路线规划失败：数据不完整");
        }
        
        result.put("distance", route.get("distance").asText() + "米");
        result.put("duration", formatDuration(route.get("duration").asInt()));
        
        List<Map<String, String>> steps = new ArrayList<>();
        JsonNode stepsNode = route.get("steps");
        for (JsonNode step : stepsNode) {
            Map<String, String> stepInfo = new HashMap<>();
            stepInfo.put("instruction", step.has("instruction") ? step.get("instruction").asText() : "");
            stepInfo.put("distance", step.has("distance") ? step.get("distance").asText() + "米" : "");
            stepInfo.put("duration", step.has("duration") ? formatDuration(step.get("duration").asInt()) : "");
            steps.add(stepInfo);
        }
        result.put("steps", steps);

        if (route.has("polyline")) {
            result.put("path", route.get("polyline").asText());
        } else {
            result.put("path", "");
        }

        return result;
    }

    /**
     * 解析公交路线结果
     */
    private Map<String, Object> parseTransitRoute(JsonNode rootNode) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "transit");

        // 检查是否有route数据
        if (!rootNode.has("route") || rootNode.get("route") == null) {
            log.error("API返回数据缺少route字段");
            return createErrorResponse("路线规划失败：API返回数据格式错误");
        }

        JsonNode routeNode = rootNode.get("route");
        
        // 检查是否有transits数组
        if (!routeNode.has("transits") || routeNode.get("transits") == null || routeNode.get("transits").size() == 0) {
            String errorMsg = routeNode.has("info") ? routeNode.get("info").asText() : "未找到可用路线";
            log.error("API返回数据缺少transits数组: {}", errorMsg);
            return createErrorResponse("路线规划失败：" + errorMsg);
        }

        JsonNode route = routeNode.get("transits").get(0);
        
        // 检查必要字段
        if (!route.has("distance") || !route.has("duration")) {
            log.error("路线数据缺少必要字段");
            return createErrorResponse("路线规划失败：数据不完整");
        }
        
        result.put("distance", route.get("distance").asText() + "米");
        result.put("duration", formatDuration(route.get("duration").asInt()));
        
        List<Map<String, String>> segments = new ArrayList<>();
        
        if (route.has("segments") && route.get("segments") != null) {
            JsonNode segmentsNode = route.get("segments");
            for (JsonNode segment : segmentsNode) {
                Map<String, String> segmentInfo = new HashMap<>();
                
                // 处理公交/地铁段
                if (segment.has("bus") && segment.get("bus") != null && segment.get("bus").size() > 0) {
                    JsonNode busNode = segment.get("bus").get(0);
                    if (busNode != null && busNode.has("name")) {
                        segmentInfo.put("type", "公交");
                        segmentInfo.put("line", busNode.get("name").asText());
                        segmentInfo.put("instruction", "乘坐" + busNode.get("name").asText());
                    }
                }
                // 处理步行段
                else if (segment.has("walking") && segment.get("walking") != null && segment.get("walking").size() > 0) {
                    JsonNode walkingNode = segment.get("walking").get(0);
                    if (walkingNode != null) {
                        segmentInfo.put("type", "步行");
                        segmentInfo.put("instruction", walkingNode.has("instruction") ? walkingNode.get("instruction").asText() : "步行");
                        segmentInfo.put("distance", walkingNode.has("distance") ? walkingNode.get("distance").asText() + "米" : "");
                    }
                }
                // 处理其他交通方式（如地铁、铁路等）
                else if (segment.has("railway") && segment.get("railway") != null && segment.get("railway").size() > 0) {
                    JsonNode railwayNode = segment.get("railway").get(0);
                    if (railwayNode != null && railwayNode.has("name")) {
                        segmentInfo.put("type", "地铁/火车");
                        segmentInfo.put("line", railwayNode.get("name").asText());
                        segmentInfo.put("instruction", "乘坐" + railwayNode.get("name").asText());
                    }
                }
                
                // 只添加有内容的段
                if (!segmentInfo.isEmpty()) {
                    segments.add(segmentInfo);
                }
            }
        }
        
        result.put("segments", segments);

        return result;
    }

    /**
     * 格式化时间为可读字符串
     */
    private String formatDuration(int seconds) {
        if (seconds < 60) {
            return seconds + "秒";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟";
        } else {
            int hours = seconds / 3600;
            int minutes = (seconds % 3600) / 60;
            return hours + "小时" + (minutes > 0 ? minutes + "分钟" : "");
        }
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        return error;
    }

    /**
     * 构建地理编码错误信息
     */
    private String buildGeocodeErrorMessage(String origin, String destination, 
                                           String originCoords, String destCoords) {
        StringBuilder sb = new StringBuilder();
        
        // 检查API Key是否配置
        if (amapApiKey == null || amapApiKey.equals("your-amap-api-key")) {
            sb.append("❌ 高德地图API Key未配置\n\n");
            sb.append("请按以下步骤配置：\n");
            sb.append("1. 访问 https://console.amap.com/ 注册并获取Web服务API Key\n");
            sb.append("2. 在 application.yml 中配置：\n");
            sb.append("   amap:\n");
            sb.append("     api:\n");
            sb.append("       key: 你的API-Key\n");
            sb.append("3. 或者设置环境变量：AMAP_API_KEY=你的API-Key\n");
            sb.append("4. 重启应用");
            return sb.toString();
        }
        
        sb.append("❌ 无法解析地址\n\n");
        
        if (originCoords == null) {
            sb.append("起点地址无法解析：").append(origin).append("\n");
        }
        if (destCoords == null) {
            sb.append("终点地址无法解析：").append(destination).append("\n");
        }
        
        sb.append("\n可能的原因：\n");
        sb.append("1. 地址不够详细，请尝试使用更完整的地址（如：北京市东城区景山前街4号）\n");
        sb.append("2. 地址名称有误或不存在\n");
        sb.append("3. API Key权限不足，请确认已开通'地理编码'服务\n");
        sb.append("4. API调用次数已达上限\n");
        sb.append("\n建议：\n");
        sb.append("• 使用'城市+区+街道+门牌号'的格式\n");
        sb.append("• 使用知名地标名称（如：北京故宫、上海东方明珠）\n");
        sb.append("• 查看后端日志获取详细错误信息");
        
        return sb.toString();
    }
}

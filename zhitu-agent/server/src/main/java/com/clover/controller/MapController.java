package com.clover.controller;

import com.clover.result.Result;
import com.clover.service.MapRouteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 地图路线控制器
 * 提供地图路线规划相关接口
 */
@Slf4j
@RestController
@RequestMapping("/map")
public class MapController {

    @Autowired
    private MapRouteService mapRouteService;

    /**
     * 获取驾车路线
     */
    @PostMapping("/driving")
    public Result<Map<String, Object>> getDrivingRoute(@RequestBody Map<String, String> request) {
        try {
            String origin = request.get("origin");
            String destination = request.get("destination");

            if (origin == null || destination == null) {
                return Result.error("起点和终点不能为空");
            }

            Map<String, Object> route = mapRouteService.getDrivingRoute(origin, destination);

            if (route.containsKey("error")) {
                return Result.error((String) route.get("message"));
            }

            return Result.success(route);

        } catch (Exception e) {
            log.error("获取驾车路线失败", e);
            return Result.error("获取路线失败：" + e.getMessage());
        }
    }

    /**
     * 获取步行路线
     */
    @PostMapping("/walking")
    public Result<Map<String, Object>> getWalkingRoute(@RequestBody Map<String, String> request) {
        try {
            String origin = request.get("origin");
            String destination = request.get("destination");

            if (origin == null || destination == null) {
                return Result.error("起点和终点不能为空");
            }

            Map<String, Object> route = mapRouteService.getWalkingRoute(origin, destination);

            if (route.containsKey("error")) {
                return Result.error((String) route.get("message"));
            }

            return Result.success(route);

        } catch (Exception e) {
            log.error("获取步行路线失败", e);
            return Result.error("获取路线失败：" + e.getMessage());
        }
    }

    /**
     * 获取公交路线
     */
    @PostMapping("/transit")
    public Result<Map<String, Object>> getTransitRoute(@RequestBody Map<String, String> request) {
        try {
            String origin = request.get("origin");
            String destination = request.get("destination");
            String city = request.getOrDefault("city", "北京");

            if (origin == null || destination == null) {
                return Result.error("起点和终点不能为空");
            }

            Map<String, Object> route = mapRouteService.getTransitRoute(origin, destination, city);

            if (route.containsKey("error")) {
                return Result.error((String) route.get("message"));
            }

            return Result.success(route);

        } catch (Exception e) {
            log.error("获取公交路线失败", e);
            return Result.error("获取路线失败：" + e.getMessage());
        }
    }
}

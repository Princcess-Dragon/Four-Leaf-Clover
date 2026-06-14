package com.clover.controller;

import com.clover.dto.TravelDiaryDTO;
import com.clover.result.Result;
import com.clover.service.TravelDiaryService;
import com.clover.vo.TravelDiaryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 旅行日记控制器
 */
@Slf4j
@RestController
@RequestMapping("/diary")
public class TravelDiaryController {

    @Autowired
    private TravelDiaryService travelDiaryService;

    /**
     * 创建旅行日记
     */
    @PostMapping
    public Result<TravelDiaryVO> createDiary(
            @RequestParam(required = false) String userId,
            @RequestBody TravelDiaryDTO dto) {
        try {
            if (dto.getTitle() == null || dto.getTitle().isEmpty()) {
                return Result.error("日记标题不能为空");
            }
            if (dto.getContent() == null || dto.getContent().isEmpty()) {
                return Result.error("日记内容不能为空");
            }

            TravelDiaryVO vo = travelDiaryService.createDiary(userId, dto);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("创建旅行日记失败", e);
            return Result.error("创建失败：" + e.getMessage());
        }
    }

    /**
     * 更新旅行日记
     */
    @PutMapping("/{id}")
    public Result<TravelDiaryVO> updateDiary(
            @PathVariable Long id,
            @RequestParam(required = false) String userId,
            @RequestBody TravelDiaryDTO dto) {
        try {
            dto.setId(id);
            TravelDiaryVO vo = travelDiaryService.updateDiary(userId, dto);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("更新旅行日记失败", e);
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * 删除旅行日记
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteDiary(
            @PathVariable Long id,
            @RequestParam(required = false) String userId) {
        try {
            travelDiaryService.deleteDiary(userId, id);
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除旅行日记失败", e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 获取日记详情
     */
    @GetMapping("/{id}")
    public Result<TravelDiaryVO> getDiaryDetail(
            @PathVariable Long id,
            @RequestParam(required = false) String userId) {
        try {
            TravelDiaryVO vo = travelDiaryService.getDiaryDetail(userId, id);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取日记详情失败", e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户的旅行日记列表（分页）
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getUserDiaries(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Map<String, Object> result = travelDiaryService.getUserDiaries(userId, tag, location, startDate, endDate, page, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取旅行日记列表失败", e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }

    /**
     * 获取公开的旅行日记（发现页）
     */
    @GetMapping("/public")
    public Result<Map<String, Object>> getPublicDiaries(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Map<String, Object> result = travelDiaryService.getPublicDiaries(tag, location, page, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取公开日记失败", e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }

    /**
     * 搜索旅行日记
     */
    @GetMapping("/search")
    public Result<Map<String, Object>> searchDiaries(
            @RequestParam(required = false) String userId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            if (keyword == null || keyword.isEmpty()) {
                return Result.error("搜索关键词不能为空");
            }
            Map<String, Object> result = travelDiaryService.searchDiaries(userId, keyword, page, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            log.error("搜索旅行日记失败", e);
            return Result.error("搜索失败：" + e.getMessage());
        }
    }

    /**
     * 点赞/取消点赞
     */
    @PostMapping("/{id}/like")
    public Result<Integer> toggleLike(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean like) {
        try {
            int likeCount = travelDiaryService.toggleLike(id, like);
            return Result.success(likeCount);
        } catch (Exception e) {
            log.error("点赞操作失败", e);
            return Result.error("操作失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户的标签列表
     */
    @GetMapping("/tags")
    public Result<List<String>> getUserTags(@RequestParam(required = false) String userId) {
        try {
            List<String> tags = travelDiaryService.getUserTags(userId);
            return Result.success(tags);
        } catch (Exception e) {
            log.error("获取标签列表失败", e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户的旅行地点列表
     */
    @GetMapping("/locations")
    public Result<List<String>> getUserLocations(@RequestParam(required = false) String userId) {
        try {
            List<String> locations = travelDiaryService.getUserLocations(userId);
            return Result.success(locations);
        } catch (Exception e) {
            log.error("获取地点列表失败", e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户的日记统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getUserStats(@RequestParam(required = false) String userId) {
        try {
            Map<String, Object> stats = travelDiaryService.getUserStats(userId);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }
}

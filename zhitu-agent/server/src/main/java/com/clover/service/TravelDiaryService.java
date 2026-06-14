package com.clover.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.clover.context.BaseContext;
import com.clover.dto.TravelDiaryDTO;
import com.clover.entity.TravelDiary;
import com.clover.mapper.TravelDiaryMapper;
import com.clover.vo.TravelDiaryVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 旅行日记服务
 */
@Slf4j
@Service
public class TravelDiaryService {

    @Autowired
    private TravelDiaryMapper travelDiaryMapper;

    /**
     * 创建旅行日记
     */
    public TravelDiaryVO createDiary(String userId, TravelDiaryDTO dto) {
        userId = resolveUserId(userId);

        TravelDiary diary = new TravelDiary();
        BeanUtils.copyProperties(dto, diary);
        diary.setUserId(userId);
        diary.setViewCount(0);
        diary.setLikeCount(0);

        // 将列表转换为JSON字符串存储
        if (dto.getImages() != null) {
            diary.setImages(JSON.toJSONString(dto.getImages()));
        }
        if (dto.getTags() != null) {
            diary.setTags(String.join(",", dto.getTags()));
        }
        if (dto.getIsPublic() == null) {
            diary.setIsPublic(1); // 默认公开
        }

        travelDiaryMapper.insert(diary);
        log.info("用户 [{}] 创建了旅行日记: {}", userId, diary.getTitle());

        return toVO(diary);
    }

    /**
     * 更新旅行日记
     */
    public TravelDiaryVO updateDiary(String userId, TravelDiaryDTO dto) {
        userId = resolveUserId(userId);

        // 校验日记是否存在且属于当前用户
        TravelDiary existing = travelDiaryMapper.findByIdAndUserId(dto.getId(), userId);
        if (existing == null) {
            throw new RuntimeException("日记不存在或无权修改");
        }

        TravelDiary diary = new TravelDiary();
        BeanUtils.copyProperties(dto, diary);
        diary.setUserId(userId);

        if (dto.getImages() != null) {
            diary.setImages(JSON.toJSONString(dto.getImages()));
        }
        if (dto.getTags() != null) {
            diary.setTags(String.join(",", dto.getTags()));
        }

        travelDiaryMapper.update(diary);
        log.info("用户 [{}] 更新了旅行日记: {}", userId, dto.getId());

        TravelDiary updated = travelDiaryMapper.findById(dto.getId());
        return toVO(updated);
    }

    /**
     * 删除旅行日记
     */
    public void deleteDiary(String userId, Long diaryId) {
        userId = resolveUserId(userId);

        int rows = travelDiaryMapper.deleteByIdAndUserId(diaryId, userId);
        if (rows == 0) {
            throw new RuntimeException("日记不存在或无权删除");
        }
        log.info("用户 [{}] 删除了旅行日记: {}", userId, diaryId);
    }

    /**
     * 获取日记详情（并增加浏览次数）
     */
    public TravelDiaryVO getDiaryDetail(String userId, Long diaryId) {
        TravelDiary diary = travelDiaryMapper.findById(diaryId);
        if (diary == null) {
            throw new RuntimeException("日记不存在");
        }

        // 如果是私密日记，校验是否属于当前用户
        if (diary.getIsPublic() == 0) {
            userId = resolveUserId(userId);
            if (!diary.getUserId().equals(userId)) {
                throw new RuntimeException("该日记为私密日记，无权查看");
            }
        }

        // 增加浏览次数
        travelDiaryMapper.incrementViewCount(diaryId);

        return toVO(diary);
    }

    /**
     * 分页查询用户的旅行日记列表
     */
    public Map<String, Object> getUserDiaries(String userId, String tag, String location,
                                               LocalDate startDate, LocalDate endDate,
                                               int page, int pageSize) {
        userId = resolveUserId(userId);

        PageHelper.startPage(page, pageSize);
        List<TravelDiary> diaries = travelDiaryMapper.findByUserId(userId, tag, location, startDate, endDate);

        Page<TravelDiary> pageResult = (Page<TravelDiary>) diaries;
        List<TravelDiaryVO> voList = diaries.stream().map(this::toVO).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", pageResult.getTotal());
        result.put("pages", pageResult.getPages());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("records", voList);

        return result;
    }

    /**
     * 分页查询公开的旅行日记（发现页）
     */
    public Map<String, Object> getPublicDiaries(String tag, String location, int page, int pageSize) {
        PageHelper.startPage(page, pageSize);
        List<TravelDiary> diaries = travelDiaryMapper.findPublicDiaries(tag, location);

        Page<TravelDiary> pageResult = (Page<TravelDiary>) diaries;
        List<TravelDiaryVO> voList = diaries.stream().map(this::toVO).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", pageResult.getTotal());
        result.put("pages", pageResult.getPages());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("records", voList);

        return result;
    }

    /**
     * 搜索旅行日记
     */
    public Map<String, Object> searchDiaries(String userId, String keyword, int page, int pageSize) {
        userId = resolveUserId(userId);

        PageHelper.startPage(page, pageSize);
        List<TravelDiary> diaries = travelDiaryMapper.search(userId, keyword);

        Page<TravelDiary> pageResult = (Page<TravelDiary>) diaries;
        List<TravelDiaryVO> voList = diaries.stream().map(this::toVO).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", pageResult.getTotal());
        result.put("pages", pageResult.getPages());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("records", voList);

        return result;
    }

    /**
     * 点赞/取消点赞
     */
    public int toggleLike(Long diaryId, boolean like) {
        TravelDiary diary = travelDiaryMapper.findById(diaryId);
        if (diary == null) {
            throw new RuntimeException("日记不存在");
        }

        if (like) {
            travelDiaryMapper.incrementLikeCount(diaryId);
        } else {
            travelDiaryMapper.decrementLikeCount(diaryId);
        }

        TravelDiary updated = travelDiaryMapper.findById(diaryId);
        return updated.getLikeCount();
    }

    /**
     * 获取用户的所有标签（去重）
     */
    public List<String> getUserTags(String userId) {
        userId = resolveUserId(userId);

        List<String> tagStrings = travelDiaryMapper.findAllTagsByUserId(userId);
        Set<String> allTags = new LinkedHashSet<>();

        for (String tagStr : tagStrings) {
            if (tagStr != null && !tagStr.isEmpty()) {
                String[] tags = tagStr.split(",");
                for (String tag : tags) {
                    String trimmed = tag.trim();
                    if (!trimmed.isEmpty()) {
                        allTags.add(trimmed);
                    }
                }
            }
        }

        return new ArrayList<>(allTags);
    }

    /**
     * 获取用户的所有旅行地点（去重）
     */
    public List<String> getUserLocations(String userId) {
        userId = resolveUserId(userId);
        return travelDiaryMapper.findAllLocationsByUserId(userId);
    }

    /**
     * 获取用户的日记统计
     */
    public Map<String, Object> getUserStats(String userId) {
        userId = resolveUserId(userId);

        int totalDiaries = travelDiaryMapper.countByUserId(userId);
        List<String> tags = getUserTags(userId);
        List<String> locations = getUserLocations(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDiaries", totalDiaries);
        stats.put("totalTags", tags.size());
        stats.put("totalLocations", locations.size());
        stats.put("tags", tags);
        stats.put("locations", locations);

        return stats;
    }

    /**
     * 实体转VO
     */
    private TravelDiaryVO toVO(TravelDiary diary) {
        TravelDiaryVO vo = new TravelDiaryVO();
        BeanUtils.copyProperties(diary, vo);

        // 将JSON字符串转回列表
        if (diary.getImages() != null && !diary.getImages().isEmpty()) {
            try {
                List<String> imageList = JSON.parseObject(diary.getImages(), new TypeReference<List<String>>() {});
                vo.setImages(imageList);
            } catch (Exception e) {
                vo.setImages(new ArrayList<>());
            }
        } else {
            vo.setImages(new ArrayList<>());
        }

        // 将逗号分隔的标签转为列表
        if (diary.getTags() != null && !diary.getTags().isEmpty()) {
            List<String> tagList = Arrays.stream(diary.getTags().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            vo.setTags(tagList);
        } else {
            vo.setTags(new ArrayList<>());
        }

        return vo;
    }

    /**
     * 解析用户ID（如果为空则从上下文获取）
     */
    private String resolveUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            String currentId = BaseContext.getCurrentId();
            if (currentId != null && !currentId.isEmpty()) {
                userId = currentId;
            } else {
                throw new RuntimeException("用户ID不能为空");
            }
        }
        return userId;
    }
}

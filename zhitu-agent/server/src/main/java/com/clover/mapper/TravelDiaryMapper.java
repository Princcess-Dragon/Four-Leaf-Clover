package com.clover.mapper;

import com.clover.entity.TravelDiary;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 旅行日记Mapper接口
 */
@Mapper
public interface TravelDiaryMapper {

    /**
     * 插入旅行日记
     */
    int insert(TravelDiary travelDiary);

    /**
     * 更新旅行日记
     */
    int update(TravelDiary travelDiary);

    /**
     * 根据ID查询旅行日记
     */
    @Select("SELECT * FROM travel_diary WHERE id = #{id}")
    TravelDiary findById(@Param("id") Long id);

    /**
     * 根据ID和用户ID查询旅行日记（权限校验）
     */
    @Select("SELECT * FROM travel_diary WHERE id = #{id} AND user_id = #{userId}")
    TravelDiary findByIdAndUserId(@Param("id") Long id, @Param("userId") String userId);

    /**
     * 查询用户的旅行日记列表（分页）
     */
    List<TravelDiary> findByUserId(@Param("userId") String userId,
                                   @Param("tag") String tag,
                                   @Param("location") String location,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    /**
     * 查询公开的旅行日记列表（分页）
     */
    List<TravelDiary> findPublicDiaries(@Param("tag") String tag,
                                        @Param("location") String location);

    /**
     * 删除旅行日记
     */
    @Delete("DELETE FROM travel_diary WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") String userId);

    /**
     * 增加浏览次数
     */
    @Update("UPDATE travel_diary SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    /**
     * 增加点赞次数
     */
    @Update("UPDATE travel_diary SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") Long id);

    /**
     * 取消点赞
     */
    @Update("UPDATE travel_diary SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{id}")
    int decrementLikeCount(@Param("id") Long id);

    /**
     * 查询用户的所有标签（去重）
     */
    @Select("SELECT DISTINCT tags FROM travel_diary WHERE user_id = #{userId} AND tags IS NOT NULL AND tags != ''")
    List<String> findAllTagsByUserId(@Param("userId") String userId);

    /**
     * 查询用户去重的地点列表
     */
    @Select("SELECT DISTINCT location FROM travel_diary WHERE user_id = #{userId} AND location IS NOT NULL AND location != '' ORDER BY location")
    List<String> findAllLocationsByUserId(@Param("userId") String userId);

    /**
     * 统计用户日记数量
     */
    @Select("SELECT COUNT(*) FROM travel_diary WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") String userId);

    /**
     * 搜索旅行日记（关键词搜索标题和内容）
     */
    List<TravelDiary> search(@Param("userId") String userId,
                             @Param("keyword") String keyword);
}

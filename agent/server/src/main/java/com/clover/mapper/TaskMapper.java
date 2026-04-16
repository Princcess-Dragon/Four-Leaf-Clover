package com.clover.mapper;

import com.clover.entity.Task;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

/**
 * 任务Mapper接口
 */
@Mapper
public interface TaskMapper {

    /**
     * 插入任务
     */
    @Insert("INSERT INTO task (user_id, title, description, status, priority, due_date) " +
            "VALUES (#{userId}, #{title}, #{description}, #{status}, #{priority}, #{dueDate})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Task task);

    /**
     * 根据ID查询任务
     */
    @Select("SELECT * FROM task WHERE id = #{id}")
    Task findById(Long id);

    /**
     * 查询用户的任务列表
     */
    @Select("SELECT * FROM task WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Task> findByUserId(String userId);

    /**
     * 查询用户指定状态的任务
     */
    @Select("SELECT * FROM task WHERE user_id = #{userId} AND status = #{status} ORDER BY created_at DESC")
    List<Task> findByUserIdAndStatus(@Param("userId") String userId, @Param("status") Integer status);

    /**
     * 更新任务状态
     */
    @Update("UPDATE task SET status = #{status}, completed_at = #{completedAt} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("completedAt") String completedAt);

    /**
     * 更新任务
     */
    @Update("UPDATE task SET title = #{title}, description = #{description}, priority = #{priority}, " +
            "due_date = #{dueDate} WHERE id = #{id}")
    int update(Task task);

    /**
     * 删除任务
     */
    @Delete("DELETE FROM task WHERE id = #{id} AND user_id = #{userId}")
    int deleteById(@Param("id") Long id, @Param("userId") String userId);

    /**
     * 统计用户任务数量
     */
    @Select("SELECT COUNT(*) FROM task WHERE user_id = #{userId}")
    int countByUserId(String userId);

    /**
     * 统计用户各状态任务数量
     */
    @Select("SELECT status, COUNT(*) as count FROM task WHERE user_id = #{userId} GROUP BY status")
    List<Map<String, Object>> countByUserIdAndStatus(String userId);

    /**
     * 查询用户的任务（支持分页和筛选）
     */
    @Select("<script>" +
            "SELECT * FROM task WHERE user_id = #{userId} " +
            "<if test='status != null'> AND status = #{status} </if>" +
            "<if test='priority != null'> AND priority = #{priority} </if>" +
            "ORDER BY created_at DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<Task> findByUserIdWithPagination(Map<String, Object> params);

    /**
     * 清理过期任务
     */
    @Delete("DELETE FROM task WHERE created_at < #{beforeDate}")
    int deleteOldTasks(String beforeDate);
}

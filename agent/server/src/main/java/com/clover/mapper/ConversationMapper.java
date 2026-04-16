package com.clover.mapper;

import com.clover.entity.Conversation;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 对话历史Mapper接口
 */
@Mapper
public interface ConversationMapper {

    /**
     * 插入对话记录
     */
    @Insert("INSERT INTO conversation (user_id, session_id, role, content, message_type, tool_calls) " +
            "VALUES (#{userId}, #{sessionId}, #{role}, #{content}, #{messageType}, #{toolCalls})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Conversation conversation);

    /**
     * 批量插入对话记录
     */
    int insertBatch(@Param("list") List<Conversation> conversations);

    /**
     * 查询用户的对话历史
     */
    @Select("SELECT * FROM conversation WHERE user_id = #{userId} AND session_id = #{sessionId} " +
            "ORDER BY created_at ASC LIMIT #{limit}")
    List<Conversation> findBySessionId(@Param("userId") String userId, 
                                       @Param("sessionId") String sessionId,
                                       @Param("limit") Integer limit);

    /**
     * 查询用户最近的对话
     */
    @Select("SELECT * FROM conversation WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC LIMIT #{limit}")
    List<Conversation> findRecentByUserId(@Param("userId") String userId, @Param("limit") Integer limit);

    /**
     * 查询用户的会话列表
     */
    @Select("SELECT DISTINCT session_id, MIN(created_at) as created_at " +
            "FROM conversation WHERE user_id = #{userId} " +
            "GROUP BY session_id ORDER BY created_at DESC")
    List<SessionInfo> findSessionsByUserId(String userId);

    /**
     * 删除会话
     */
    @Delete("DELETE FROM conversation WHERE user_id = #{userId} AND session_id = #{sessionId}")
    int deleteBySessionId(@Param("userId") String userId, @Param("sessionId") String sessionId);

    /**
     * 删除用户所有对话
     */
    @Delete("DELETE FROM conversation WHERE user_id = #{userId}")
    int deleteByUserId(String userId);

    /**
     * 统计用户对话数量
     */
    @Select("SELECT COUNT(*) FROM conversation WHERE user_id = #{userId}")
    int countByUserId(String userId);

    /**
     * 清理过期对话
     */
    @Delete("DELETE FROM conversation WHERE created_at < #{beforeDate}")
    int deleteOldConversations(String beforeDate);

    /**
     * 会话信息
     */
    class SessionInfo {
        private String sessionId;
        private String createdAt;

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }
}

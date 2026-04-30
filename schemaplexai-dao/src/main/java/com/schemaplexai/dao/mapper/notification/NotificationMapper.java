package com.schemaplexai.dao.mapper.notification;

import com.schemaplexai.dao.mapper.BaseMapperX;
import com.schemaplexai.model.entity.notification.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper extends BaseMapperX<Notification> {

    @Update("UPDATE sf_notification SET read = TRUE, updated_at = NOW() " +
            "WHERE id = #{id} AND user_id = #{userId} AND deleted = 0")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE sf_notification SET read = TRUE, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND read = FALSE AND deleted = 0")
    int markAllAsRead(@Param("userId") Long userId);
}

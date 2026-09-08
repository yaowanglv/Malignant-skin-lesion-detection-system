package com.example.springb.mapper;

import com.example.springb.entity.UserConfig;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserConfigMapper {

    @Select("SELECT * FROM user_config WHERE user_id = #{userId} AND config_key = #{configKey} LIMIT 1")
    UserConfig selectByUserIdAndKey(@Param("userId") Integer userId, @Param("configKey") String configKey);

    @Insert("INSERT INTO user_config (user_id, config_key, config_value, description) " +
            "VALUES (#{userId}, #{configKey}, #{configValue}, #{description}) " +
            "ON DUPLICATE KEY UPDATE " +
            "config_value = VALUES(config_value), " +
            "description = VALUES(description), " +
            "updated_at = NOW()")
    int upsert(UserConfig userConfig);
}

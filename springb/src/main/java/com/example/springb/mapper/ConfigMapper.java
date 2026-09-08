package com.example.springb.mapper;

import com.example.springb.entity.ModelConfig;
import com.example.springb.entity.SystemConfig;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ConfigMapper {
    List<ModelConfig> selectModelsByPageType(@Param("pageType") String pageType);

    ModelConfig selectModelByPageTypeAndPath(@Param("pageType") String pageType,
                                             @Param("modelPath") String modelPath);

    int insertModel(ModelConfig model);

    int updateModel(ModelConfig model);

    @Update("UPDATE model_config SET display_name = #{displayName}, updated_at = NOW() WHERE id = #{id}")
    int updateModelDisplayName(@Param("id") Long id, @Param("displayName") String displayName);

    @Delete("DELETE FROM model_config WHERE id = #{id}")
    int deleteModelById(@Param("id") Long id);

    @Delete("DELETE FROM model_config WHERE page_type = #{pageType} AND folder_path = #{folderPath}")
    int deleteByPageTypeAndFolder(@Param("pageType") String pageType,
                                  @Param("folderPath") String folderPath);

    @Select("SELECT config_value FROM system_config WHERE config_key = #{key} LIMIT 1")
    String selectValueByKey(@Param("key") String key);

    int upsertConfig(@Param("key") String key,
                     @Param("value") String value,
                     @Param("description") String description);

    List<SystemConfig> selectAllConfigs();
}

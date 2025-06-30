package com.bytevault.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bytevault.app.model.BackgroundImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BackgroundImageMapper extends BaseMapper<BackgroundImage> {
    
    /**
     * 获取用户的所有背景图片
     * @param userId 用户ID
     * @return 背景图片列表
     */
    @Select("SELECT * FROM user_background_images WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY upload_time DESC")
    List<BackgroundImage> getUserBackgroundImages(Long userId);
    
    /**
     * 获取背景图片详情
     * @param id 背景图片ID
     * @return 背景图片信息
     */
    @Select("SELECT * FROM user_background_images WHERE id = #{id} AND is_deleted = 0")
    BackgroundImage getBackgroundImageById(Long id);
} 
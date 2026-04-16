package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.ArticleDraft;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArticleDraftMapper extends BaseMapper<ArticleDraft> {

    @Select("SELECT id FROM article_draft WHERE user_id = #{userId} AND deleted = 0 ORDER BY update_time ASC LIMIT 1")
    Long selectOldestDraftId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM article_draft WHERE user_id = #{userId} AND deleted = 0")
    int countByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM article_draft WHERE user_id = #{userId} AND deleted = 0 ORDER BY update_time DESC")
    List<ArticleDraft> selectByUserIdOrderByUpdateTimeDesc(@Param("userId") Long userId);
}

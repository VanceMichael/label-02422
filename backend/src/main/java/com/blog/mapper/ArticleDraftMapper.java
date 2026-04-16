package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.ArticleDraft;
import com.blog.vo.ArticleDraftVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArticleDraftMapper extends BaseMapper<ArticleDraft> {

    List<ArticleDraftVO> selectDraftList(@Param("userId") Long userId);

    ArticleDraftVO selectDraftById(@Param("id") Long id);

    int selectDraftCount(@Param("userId") Long userId);

    ArticleDraft selectOldestDraft(@Param("userId") Long userId);
}

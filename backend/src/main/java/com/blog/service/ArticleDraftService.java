package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.constant.ArticleStatusConstant;
import com.blog.dto.ArticleDTO;
import com.blog.dto.ArticleDraftSaveDTO;
import com.blog.entity.Article;
import com.blog.entity.ArticleDraft;
import com.blog.entity.ArticleTag;
import com.blog.exception.BusinessException;
import com.blog.mapper.ArticleDraftMapper;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.ArticleTagMapper;
import com.blog.vo.ArticleDraftVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticleDraftService {

    private static final int MAX_DRAFT_COUNT = 20;

    @Autowired
    private ArticleDraftMapper articleDraftMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long saveDraft(ArticleDraftSaveDTO dto, Long userId) {
        int draftCount = articleDraftMapper.countByUserId(userId);
        if (draftCount >= MAX_DRAFT_COUNT) {
            Long oldestDraftId = articleDraftMapper.selectOldestDraftId(userId);
            if (oldestDraftId != null) {
                articleDraftMapper.deleteById(oldestDraftId);
            }
        }

        ArticleDraft draft = new ArticleDraft();
        BeanUtils.copyProperties(dto, draft);
        draft.setUserId(userId);

        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            String tagIdsStr = dto.getTagIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            draft.setTagIds(tagIdsStr);
        }

        articleDraftMapper.insert(draft);
        return draft.getId();
    }

    public List<ArticleDraftVO> getMyDrafts(Long userId) {
        List<ArticleDraft> drafts = articleDraftMapper.selectByUserIdOrderByUpdateTimeDesc(userId);
        return drafts.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    public ArticleDraftVO getDraftById(Long id, Long userId) {
        ArticleDraft draft = articleDraftMapper.selectById(id);
        if (draft == null) {
            throw new BusinessException("草稿不存在");
        }
        if (!draft.getUserId().equals(userId)) {
            throw new BusinessException("无权访问此草稿");
        }
        return convertToVO(draft);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(Long id, Long userId) {
        ArticleDraft draft = articleDraftMapper.selectById(id);
        if (draft == null) {
            throw new BusinessException("草稿不存在");
        }
        if (!draft.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此草稿");
        }
        articleDraftMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteDrafts(List<Long> ids, Long userId) {
        for (Long id : ids) {
            ArticleDraft draft = articleDraftMapper.selectById(id);
            if (draft != null && draft.getUserId().equals(userId)) {
                articleDraftMapper.deleteById(id);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Long publishDraft(Long draftId, Long userId) {
        ArticleDraft draft = articleDraftMapper.selectById(draftId);
        if (draft == null) {
            throw new BusinessException("草稿不存在");
        }
        if (!draft.getUserId().equals(userId)) {
            throw new BusinessException("无权发布此草稿");
        }

        ArticleDTO articleDTO = new ArticleDTO();
        articleDTO.setTitle(draft.getTitle() != null ? draft.getTitle() : "未命名文章");
        articleDTO.setContent(draft.getContent() != null ? draft.getContent() : "");
        articleDTO.setCategoryId(draft.getCategoryId());
        articleDTO.setCoverImage(draft.getCoverImage());
        articleDTO.setStatus(ArticleStatusConstant.PUBLISHED);

        if (draft.getTagIds() != null && !draft.getTagIds().isEmpty()) {
            List<Long> tagIds = Arrays.stream(draft.getTagIds().split(","))
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
            articleDTO.setTagIds(tagIds);
        }

        Article article = new Article();
        BeanUtils.copyProperties(articleDTO, article);
        article.setUserId(userId);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);

        articleMapper.insert(article);

        if (articleDTO.getTagIds() != null && !articleDTO.getTagIds().isEmpty()) {
            for (Long tagId : articleDTO.getTagIds()) {
                ArticleTag articleTag = new ArticleTag();
                articleTag.setArticleId(article.getId());
                articleTag.setTagId(tagId);
                articleTagMapper.insert(articleTag);
            }
        }

        articleDraftMapper.deleteById(draftId);

        return article.getId();
    }

    private ArticleDraftVO convertToVO(ArticleDraft draft) {
        ArticleDraftVO vo = new ArticleDraftVO();
        BeanUtils.copyProperties(draft, vo);

        if (draft.getTagIds() != null && !draft.getTagIds().isEmpty()) {
            List<Long> tagIds = Arrays.stream(draft.getTagIds().split(","))
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
            vo.setTagIds(tagIds);
        }

        return vo;
    }
}

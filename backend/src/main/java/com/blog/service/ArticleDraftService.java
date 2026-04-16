package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.constant.ArticleStatusConstant;
import com.blog.dto.ArticleDTO;
import com.blog.dto.ArticleDraftDTO;
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
    public Long saveDraft(ArticleDraftDTO dto, Long userId) {
        checkAndDeleteOldestDraftIfNeeded(userId);

        ArticleDraft draft = new ArticleDraft();
        draft.setUserId(userId);
        draft.setArticleId(dto.getArticleId());
        draft.setCategoryId(dto.getCategoryId());
        draft.setTitle(dto.getTitle());
        draft.setContent(dto.getContent());
        draft.setCoverImage(dto.getCoverImage());
        
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            draft.setTagIds(dto.getTagIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
        }

        articleDraftMapper.insert(draft);
        return draft.getId();
    }

    private void checkAndDeleteOldestDraftIfNeeded(Long userId) {
        int draftCount = articleDraftMapper.selectDraftCount(userId);
        if (draftCount >= MAX_DRAFT_COUNT) {
            ArticleDraft oldestDraft = articleDraftMapper.selectOldestDraft(userId);
            if (oldestDraft != null) {
                articleDraftMapper.deleteById(oldestDraft.getId());
            }
        }
    }

    public List<ArticleDraftVO> getDraftList(Long userId) {
        List<ArticleDraftVO> drafts = articleDraftMapper.selectDraftList(userId);
        drafts.forEach(draft -> {
            if (draft.getTagIds() != null && !draft.getTagIds().isEmpty()) {
                draft.setTagIdList(Arrays.stream(draft.getTagIds().split(","))
                        .map(Long::parseLong)
                        .collect(Collectors.toList()));
            }
        });
        return drafts;
    }

    public ArticleDraftVO getDraftById(Long id, Long userId) {
        ArticleDraftVO draft = articleDraftMapper.selectDraftById(id);
        if (draft == null) {
            throw new BusinessException("草稿不存在");
        }

        LambdaQueryWrapper<ArticleDraft> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleDraft::getId, id).eq(ArticleDraft::getUserId, userId);
        if (articleDraftMapper.selectCount(wrapper) == 0) {
            throw new BusinessException("无权访问此草稿");
        }

        if (draft.getTagIds() != null && !draft.getTagIds().isEmpty()) {
            draft.setTagIdList(Arrays.stream(draft.getTagIds().split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList()));
        }
        return draft;
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
            deleteDraft(id, userId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Long restoreDraft(Long draftId, Long userId, boolean publish) {
        ArticleDraft draft = articleDraftMapper.selectById(draftId);
        if (draft == null) {
            throw new BusinessException("草稿不存在");
        }

        if (!draft.getUserId().equals(userId)) {
            throw new BusinessException("无权恢复此草稿");
        }

        ArticleDTO articleDTO = new ArticleDTO();
        articleDTO.setTitle(draft.getTitle());
        articleDTO.setContent(draft.getContent());
        articleDTO.setCategoryId(draft.getCategoryId());
        articleDTO.setCoverImage(draft.getCoverImage());
        articleDTO.setStatus(publish ? ArticleStatusConstant.PUBLISHED : ArticleStatusConstant.DRAFT);

        if (draft.getTagIds() != null && !draft.getTagIds().isEmpty()) {
            articleDTO.setTagIds(Arrays.stream(draft.getTagIds().split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList()));
        }

        Long articleId;
        if (draft.getArticleId() != null) {
            Article existingArticle = articleMapper.selectById(draft.getArticleId());
            if (existingArticle != null && existingArticle.getUserId().equals(userId)) {
                updateExistingArticle(draft.getArticleId(), articleDTO, userId);
                articleId = draft.getArticleId();
            } else {
                articleId = createNewArticle(articleDTO, userId);
            }
        } else {
            articleId = createNewArticle(articleDTO, userId);
        }

        articleDraftMapper.deleteById(draftId);
        return articleId;
    }

    private Long createNewArticle(ArticleDTO dto, Long userId) {
        Article article = new Article();
        BeanUtils.copyProperties(dto, article);
        article.setUserId(userId);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);
        articleMapper.insert(article);

        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            for (Long tagId : dto.getTagIds()) {
                ArticleTag articleTag = new ArticleTag();
                articleTag.setArticleId(article.getId());
                articleTag.setTagId(tagId);
                articleTagMapper.insert(articleTag);
            }
        }
        return article.getId();
    }

    private void updateExistingArticle(Long articleId, ArticleDTO dto, Long userId) {
        Article article = articleMapper.selectById(articleId);
        BeanUtils.copyProperties(dto, article);
        articleMapper.updateById(article);

        LambdaQueryWrapper<ArticleTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleTag::getArticleId, articleId);
        articleTagMapper.delete(wrapper);

        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            for (Long tagId : dto.getTagIds()) {
                ArticleTag articleTag = new ArticleTag();
                articleTag.setArticleId(articleId);
                articleTag.setTagId(tagId);
                articleTagMapper.insert(articleTag);
            }
        }
    }
}

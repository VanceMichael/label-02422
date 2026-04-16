package com.blog.controller;

import com.blog.annotation.OperationLog;
import com.blog.dto.ArticleDraftSaveDTO;
import com.blog.service.ArticleDraftService;
import com.blog.utils.JwtUtil;
import com.blog.vo.ArticleDraftVO;
import com.blog.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drafts")
public class ArticleDraftController {

    @Autowired
    private ArticleDraftService articleDraftService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @OperationLog("保存草稿")
    public Result<Long> saveDraft(@RequestBody ArticleDraftSaveDTO dto, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Long draftId = articleDraftService.saveDraft(dto, userId);
        return Result.success("草稿保存成功", draftId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<List<ArticleDraftVO>> getMyDrafts(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<ArticleDraftVO> drafts = articleDraftService.getMyDrafts(userId);
        return Result.success(drafts);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<ArticleDraftVO> getDraftById(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        ArticleDraftVO draft = articleDraftService.getDraftById(id, userId);
        return Result.success(draft);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @OperationLog("删除草稿")
    public Result<Object> deleteDraft(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        articleDraftService.deleteDraft(id, userId);
        return Result.success("删除成功");
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @OperationLog("批量删除草稿")
    public Result<Object> batchDeleteDrafts(@RequestBody Map<String, List<Long>> request, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<Long> ids = request.get("ids");
        articleDraftService.batchDeleteDrafts(ids, userId);
        return Result.success("批量删除成功");
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @OperationLog("发布草稿")
    public Result<Long> publishDraft(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Long articleId = articleDraftService.publishDraft(id, userId);
        return Result.success("发布成功", articleId);
    }
}

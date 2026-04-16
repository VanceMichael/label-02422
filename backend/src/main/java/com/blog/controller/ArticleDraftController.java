package com.blog.controller;

import com.blog.annotation.OperationLog;
import com.blog.dto.ArticleDraftDTO;
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

@RestController
@RequestMapping("/api/drafts")
public class ArticleDraftController {

    @Autowired
    private ArticleDraftService articleDraftService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @OperationLog("保存文章草稿")
    public Result<Long> saveDraft(@RequestBody ArticleDraftDTO dto, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Long draftId = articleDraftService.saveDraft(dto, userId);
        return Result.success("保存成功", draftId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<List<ArticleDraftVO>> getDraftList(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(articleDraftService.getDraftList(userId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<ArticleDraftVO> getDraftById(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(articleDraftService.getDraftById(id, userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @OperationLog("删除文章草稿")
    public Result<Object> deleteDraft(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        articleDraftService.deleteDraft(id, userId);
        return Result.success("删除成功");
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @OperationLog("批量删除文章草稿")
    public Result<Object> batchDeleteDrafts(@RequestBody List<Long> ids, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        articleDraftService.batchDeleteDrafts(ids, userId);
        return Result.success("批量删除成功");
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @OperationLog("恢复文章草稿")
    public Result<Long> restoreDraft(@PathVariable Long id,
                                     @RequestParam(defaultValue = "false") Boolean publish,
                                     Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Long articleId = articleDraftService.restoreDraft(id, userId, publish);
        String message = publish ? "发布成功" : "恢复成功";
        return Result.success(message, articleId);
    }
}

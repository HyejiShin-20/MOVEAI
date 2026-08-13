package com.moveai.moderation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moveai.moderation.dto.ModerationDto;
import com.moveai.moderation.service.ModerationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/moderation/drafts")
public class ModerationController {

    private final ModerationService moderationService;

    public ModerationController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @GetMapping
    public List<ModerationDto.DraftSummary> list(@RequestParam(required = false) String status) {
        return moderationService.findDrafts(status);
    }

    @GetMapping("/{id}")
    public ModerationDto.DraftDetail detail(@PathVariable Long id) {
        return moderationService.findDraft(id);
    }

    /** 무수정 승인이면 본문을 생략할 수 있다. */
    @PostMapping("/{id}/approve")
    public ModerationDto.ApproveResult approve(
            @PathVariable Long id,
            @RequestBody(required = false) ModerationDto.ApproveRequest request) {
        return moderationService.approve(id, request == null ? null : request.editedPayload());
    }

    @PostMapping("/{id}/reject")
    public ModerationDto.RejectResult reject(
            @PathVariable Long id, @Valid @RequestBody ModerationDto.RejectRequest request) {
        return moderationService.reject(id, request.reason());
    }
}

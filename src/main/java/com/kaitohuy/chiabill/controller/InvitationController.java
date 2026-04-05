package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.dto.response.InvitationResponse;
import com.kaitohuy.chiabill.dto.response.InviteInfoResponse;
import com.kaitohuy.chiabill.dto.response.TripResponse;
import com.kaitohuy.chiabill.security.UserPrincipal;
import com.kaitohuy.chiabill.service.interfaces.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    // Create an invite link for a trip
    @PostMapping("/trips/{tripId}/invites")
    public ApiResponse<InvitationResponse> createInvite(
            @PathVariable Long tripId,
            @RequestBody(required = false) com.kaitohuy.chiabill.dto.request.CreateInviteRequest request,
            Authentication authentication) {

        Long callerId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<InvitationResponse>builder()
                .success(true)
                .data(invitationService.createInvite(tripId, callerId, request))
                .build();
    }

    // Get active invite for a trip
    @GetMapping("/trips/{tripId}/invites/active")
    public ApiResponse<InvitationResponse> getActiveInvite(
            @PathVariable Long tripId,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<InvitationResponse>builder()
                .success(true)
                .data(invitationService.getActiveInvite(tripId, userId))
                .build();
    }

    // Get info about an invite link (PUBLIC)
    @GetMapping("/invites/{inviteId}")
    public ApiResponse<InviteInfoResponse> getInviteInfo(@PathVariable String inviteId) {

        return ApiResponse.<InviteInfoResponse>builder()
                .success(true)
                .data(invitationService.getInviteInfo(inviteId))
                .build();
    }

    // Join a trip via an invite link
    @PostMapping("/invites/{inviteId}/join")
    public ApiResponse<TripResponse> joinByInvite(
            @PathVariable String inviteId,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<TripResponse>builder()
                .success(true)
                .data(invitationService.joinByInvite(inviteId, userId))
                .build();
    }
}

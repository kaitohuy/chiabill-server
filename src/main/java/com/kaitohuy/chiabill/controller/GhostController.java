package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.request.CreateGhostMembersRequest;
import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.dto.response.GhostMemberResponse;
import com.kaitohuy.chiabill.dto.response.UserResponse;
import com.kaitohuy.chiabill.security.UserPrincipal;
import com.kaitohuy.chiabill.service.interfaces.GhostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GhostController {

    private final GhostService ghostService;

    @PostMapping("/trips/{tripId}/ghost-members")
    public ApiResponse<List<GhostMemberResponse>> createGhostMembers(
            @PathVariable Long tripId,
            @Valid @RequestBody CreateGhostMembersRequest request,
            Authentication authentication) {

        Long callerId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<List<GhostMemberResponse>>builder()
                .success(true)
                .data(ghostService.createGhostMembers(tripId, callerId, request))
                .build();
    }

    @PutMapping("/users/ghost/{ghostId}/claim")
    public ApiResponse<UserResponse> claimGhost(
            @PathVariable Long ghostId,
            Authentication authentication) {

        Long realUserId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<UserResponse>builder()
                .success(true)
                .data(ghostService.claimGhost(ghostId, realUserId))
                .build();
    }
}

package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.request.ActivateFundRequest;
import com.kaitohuy.chiabill.dto.request.RequiredContributionRequest;
import com.kaitohuy.chiabill.dto.request.UpdateTreasurerRequest;
import com.kaitohuy.chiabill.dto.request.VoluntaryContributionRequest;
import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.dto.response.FundContributionResponse;
import com.kaitohuy.chiabill.dto.response.FundResponse;
import com.kaitohuy.chiabill.security.UserPrincipal;
import com.kaitohuy.chiabill.service.interfaces.GroupFundService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips/{tripId}/fund")
@RequiredArgsConstructor
public class GroupFundController {

    private final GroupFundService fundService;

    @GetMapping
    public ApiResponse<FundResponse> getFund(
            @PathVariable Long tripId,
            Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ApiResponse.<FundResponse>builder()
                .success(true)
                .data(fundService.getFundByTrip(tripId, userId))
                .build();
    }

    @PostMapping("/activate")
    public ApiResponse<FundResponse> activateFund(
            @PathVariable Long tripId,
            @RequestBody ActivateFundRequest request,
            Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ApiResponse.<FundResponse>builder()
                .success(true)
                .data(fundService.activateFund(tripId, userId, request))
                .build();
    }

    @PutMapping("/treasurer")
    public ApiResponse<FundResponse> updateTreasurer(
            @PathVariable Long tripId,
            @RequestBody UpdateTreasurerRequest request,
            Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ApiResponse.<FundResponse>builder()
                .success(true)
                .data(fundService.updateTreasurer(tripId, userId, request))
                .build();
    }

    @PostMapping("/contributions/required")
    public ApiResponse<List<FundContributionResponse>> createRequiredContribution(
            @PathVariable Long tripId,
            @RequestBody RequiredContributionRequest request,
            Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ApiResponse.<List<FundContributionResponse>>builder()
                .success(true)
                .data(fundService.createRequiredContribution(tripId, userId, request))
                .build();
    }

    @PostMapping("/contributions/voluntary")
    public ApiResponse<FundContributionResponse> createVoluntaryContribution(
            @PathVariable Long tripId,
            @RequestBody VoluntaryContributionRequest request,
            Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ApiResponse.<FundContributionResponse>builder()
                .success(true)
                .data(fundService.createVoluntaryContribution(tripId, userId, request))
                .build();
    }

    @GetMapping("/contributions")
    public ApiResponse<List<FundContributionResponse>> getContributions(
            @PathVariable Long tripId,
            Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ApiResponse.<List<FundContributionResponse>>builder()
                .success(true)
                .data(fundService.getContributions(tripId, userId))
                .build();
    }

    @PostMapping("/contributions/{contributionId}/confirm")
    public ApiResponse<FundContributionResponse> confirmContribution(
            @PathVariable Long tripId,
            @PathVariable Long contributionId,
            Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ApiResponse.<FundContributionResponse>builder()
                .success(true)
                .data(fundService.confirmContribution(contributionId, userId))
                .build();
    }
}

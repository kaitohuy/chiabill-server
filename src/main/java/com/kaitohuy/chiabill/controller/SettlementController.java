package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.dto.response.SettlementResponse;
import com.kaitohuy.chiabill.security.UserPrincipal;
import com.kaitohuy.chiabill.service.interfaces.SettlementService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping("/trip/{tripId}")
    public ApiResponse<List<SettlementResponse>> getSettlement(
            @PathVariable Long tripId,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<List<SettlementResponse>>builder()
                .success(true)
                .data(settlementService.calculateSettlement(tripId, userId))
                .build();
    }
}
package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.dto.response.PageResponse;
import com.kaitohuy.chiabill.dto.response.TripHistoryResponse;
import com.kaitohuy.chiabill.service.interfaces.TripHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripHistoryController {

    private final TripHistoryService tripHistoryService;

    @GetMapping("/{tripId}/history")
    public ApiResponse<com.kaitohuy.chiabill.dto.response.PageResponse<TripHistoryResponse>> getTripHistory(
            @PathVariable Long tripId,
            @RequestParam(required = false) List<String> actions,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<TripHistoryResponse> historyPage = tripHistoryService.getTripHistoryPaginated(tripId, actions, startDate, endDate, pageable);
        
        PageResponse<TripHistoryResponse> pageResponse = PageResponse.from(historyPage);
        return ApiResponse.<PageResponse<TripHistoryResponse>>builder()
                .success(true)
                .message("Fetch trip history success")
                .data(pageResponse)
                .build();
    }
}

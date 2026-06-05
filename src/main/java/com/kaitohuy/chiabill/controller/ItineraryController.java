package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.request.SaveItineraryRequest;
import com.kaitohuy.chiabill.dto.request.UpdateItineraryAlarmSettingRequest;
import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.dto.response.ItineraryItemResponse;
import com.kaitohuy.chiabill.dto.response.ItineraryAlarmSettingResponse;
import com.kaitohuy.chiabill.entity.ItineraryAlarmSetting;
import com.kaitohuy.chiabill.security.UserPrincipal;
import com.kaitohuy.chiabill.service.interfaces.ItineraryService;
import com.kaitohuy.chiabill.service.interfaces.ExportService;
import com.kaitohuy.chiabill.service.interfaces.ItineraryNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequestMapping("/api/trips/{tripId}/itinerary")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryService itineraryService;
    private final ExportService exportService;
    private final ItineraryNotificationService itineraryNotificationService;

    @GetMapping
    public ApiResponse<List<ItineraryItemResponse>> getItinerary(
            @PathVariable Long tripId,
            Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ApiResponse.<List<ItineraryItemResponse>>builder()
                .success(true)
                .data(itineraryService.getItinerary(tripId, userId))
                .build();
    }

    @PostMapping("/bulk")
    public ApiResponse<List<ItineraryItemResponse>> saveItineraryBulk(
            @PathVariable Long tripId,
            @RequestBody List<SaveItineraryRequest> requests,
            Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ApiResponse.<List<ItineraryItemResponse>>builder()
                .success(true)
                .data(itineraryService.saveItineraryBulk(tripId, userId, requests))
                .build();
    }

    @PostMapping("/item")
    public ApiResponse<ItineraryItemResponse> saveItineraryItem(
            @PathVariable Long tripId,
            @RequestBody SaveItineraryRequest request,
            Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ApiResponse.<ItineraryItemResponse>builder()
                .success(true)
                .data(itineraryService.saveItineraryItem(tripId, userId, request))
                .build();
    }

    @DeleteMapping("/{itemId}")
    public ApiResponse<?> deleteItineraryItem(
            @PathVariable Long tripId,
            @PathVariable Long itemId,
            Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        itineraryService.deleteItineraryItem(tripId, itemId, userId);
        return ApiResponse.builder()
                .success(true)
                .message("Deleted itinerary activity successfully")
                .build();
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @PathVariable Long tripId,
            Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        byte[] data = exportService.exportItineraryToExcel(tripId, userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=itinerary_trip_" + tripId + ".xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/settings")
    public ApiResponse<ItineraryAlarmSettingResponse> getSettings(
            @PathVariable Long tripId,
            Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        ItineraryAlarmSetting setting = itineraryNotificationService.getSettings(tripId, userId);
        return ApiResponse.<ItineraryAlarmSettingResponse>builder()
                .success(true)
                .data(ItineraryAlarmSettingResponse.builder()
                        .alarmEnabled(setting.getAlarmEnabled())
                        .alarmValue(setting.getAlarmValue())
                        .alarmUnit(setting.getAlarmUnit())
                        .build())
                .build();
    }

    @PutMapping("/settings")
    public ApiResponse<ItineraryAlarmSettingResponse> updateSettings(
            @PathVariable Long tripId,
            @RequestBody UpdateItineraryAlarmSettingRequest request,
            Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        ItineraryAlarmSetting setting = itineraryNotificationService.saveSettings(
                tripId,
                userId,
                request.getAlarmEnabled(),
                request.getAlarmValue(),
                request.getAlarmUnit()
        );
        return ApiResponse.<ItineraryAlarmSettingResponse>builder()
                .success(true)
                .data(ItineraryAlarmSettingResponse.builder()
                        .alarmEnabled(setting.getAlarmEnabled())
                        .alarmValue(setting.getAlarmValue())
                        .alarmUnit(setting.getAlarmUnit())
                        .build())
                .message("Cập nhật cài đặt báo thức thành công")
                .build();
    }

    @PostMapping("/import-from/{sourceTripId}")
    public ApiResponse<List<ItineraryItemResponse>> importFromOtherTrip(
            @PathVariable Long tripId,
            @PathVariable Long sourceTripId,
            Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ApiResponse.<List<ItineraryItemResponse>>builder()
                .success(true)
                .data(itineraryService.cloneItinerary(tripId, userId, sourceTripId))
                .message("Sao chép lịch trình từ chuyến đi khác thành công")
                .build();
    }
}

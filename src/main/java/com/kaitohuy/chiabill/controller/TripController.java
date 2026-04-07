package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.request.AddMemberDirectRequest;
import com.kaitohuy.chiabill.dto.request.CreateTripRequest;
import com.kaitohuy.chiabill.dto.request.UpdateTripRequest;
import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.dto.response.TripResponse;
import com.kaitohuy.chiabill.security.UserPrincipal;
import com.kaitohuy.chiabill.service.interfaces.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.kaitohuy.chiabill.dto.response.PageResponse;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.kaitohuy.chiabill.service.interfaces.ExportService;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final ExportService exportService;

    @PostMapping
    public ApiResponse<TripResponse> createTrip(
            @RequestBody CreateTripRequest request,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<TripResponse>builder()
                .success(true)
                .data(tripService.createTrip(userId, request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<TripResponse>> getMyTrips(Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<List<TripResponse>>builder()
                .success(true)
                .data(tripService.getMyTrips(userId))
                .build();
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<TripResponse>> getMyTripsPaginated(
            @RequestParam(required = false) String keyword,
            org.springframework.data.domain.Pageable pageable,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<com.kaitohuy.chiabill.dto.response.PageResponse<TripResponse>>builder()
                .success(true)
                .data(tripService.getMyTripsPaginated(userId, keyword, pageable))
                .build();
    }

    @GetMapping("/{tripId}")
    public ApiResponse<TripResponse> getTripDetail(@PathVariable Long tripId, Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ApiResponse.<TripResponse>builder()
                .success(true)
                .data(tripService.getTripDetail(tripId, userId))
                .build();
    }

    @PutMapping("/{tripId}")
    public ApiResponse<TripResponse> updateTrip(
            @PathVariable Long tripId,
            @RequestBody UpdateTripRequest request,
            Authentication authentication) {
        
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<TripResponse>builder()
                .success(true)
                .data(tripService.updateTrip(tripId, userId, request))
                .build();
    }

    @DeleteMapping("/{tripId}")
    public ApiResponse<?> deleteTrip(@PathVariable Long tripId, Authentication authentication) {
        
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        tripService.deleteTrip(tripId, userId);

        return ApiResponse.builder()
                .success(true)
                .message("Trip deleted")
                .build();
    }

    @PostMapping("/{tripId}/members/add")
    public ApiResponse<?> addDirectMember(
            @PathVariable Long tripId,
            @RequestBody AddMemberDirectRequest request,
            Authentication authentication) {

        Long ownerId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        tripService.addDirectMember(tripId, ownerId, request);

        return ApiResponse.builder()
                .success(true)
                .message("Thêm thành viên thành công!")
                .build();
    }

    @PutMapping("/{tripId}/transfer-owner")
    public ApiResponse<?> transferOwner(
            @PathVariable Long tripId,
            @RequestBody Map<String, Long> request,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        tripService.transferOwner(tripId, userId, request.get("newOwnerId"));

        return ApiResponse.builder()
                .success(true)
                .message("Chuyển quyền chủ phòng thành công")
                .build();
    }

    @PostMapping("/{tripId}/leave")
    public ApiResponse<?> leaveTrip(@PathVariable Long tripId, Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        tripService.leaveTrip(tripId, userId);

        return ApiResponse.builder()
                .success(true)
                .message("Rời nhóm thành công")
                .build();
    }

    @PutMapping("/{tripId}/members/{memberId}/disable")
    public ApiResponse<?> disableMember(
            @PathVariable Long tripId,
            @PathVariable Long memberId,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        tripService.disableMember(tripId, userId, memberId);

        return ApiResponse.builder()
                .success(true)
                .message("Đã tạm ngưng thành viên")
                .build();
    }

    @DeleteMapping("/{tripId}/members/{memberId}")
    public ApiResponse<?> kickMember(
            @PathVariable Long tripId,
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "false") boolean forgiveDebt,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        tripService.kickMember(tripId, userId, memberId, forgiveDebt);

        String msg = forgiveDebt ? "Đã đuổi và xóa nợ thành công" : "Đã đuổi thành viên khỏi nhóm";
        return ApiResponse.builder()
                .success(true)
                .message(msg)
                .build();
    }



    @PostMapping("/{tripId}/join")
    public ApiResponse<?> joinTrip(@PathVariable Long tripId, Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        tripService.joinTrip(tripId, userId);

        return ApiResponse.builder()
                .success(true)
                .message("Joined trip")
                .build();
    }

    @GetMapping("/{tripId}/export/excel")
    public ResponseEntity<byte[]> exportExcel(@PathVariable Long tripId, Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        byte[] data = exportService.exportTripToExcel(tripId, userId);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trip_report_" + tripId + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/{tripId}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long tripId, Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        byte[] data = exportService.exportTripToPdf(tripId, userId);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trip_report_" + tripId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }
}
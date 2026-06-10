package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.request.AddMemberDirectRequest;
import com.kaitohuy.chiabill.dto.request.CreateTripRequest;
import com.kaitohuy.chiabill.dto.request.UpdateTripRequest;
import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.dto.response.TripResponse;
import com.kaitohuy.chiabill.security.UserPrincipal;
import com.kaitohuy.chiabill.service.interfaces.TripService;
import jakarta.validation.Valid;
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
                        @Valid @RequestBody CreateTripRequest request,
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
                        @RequestParam(required = false) Integer month,
                        @RequestParam(required = false) Integer year,
                        org.springframework.data.domain.Pageable pageable,
                        Authentication authentication) {

                Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

                return ApiResponse.<com.kaitohuy.chiabill.dto.response.PageResponse<TripResponse>>builder()
                                .success(true)
                                .data(tripService.getMyTripsPaginated(userId, keyword, month, year, pageable))
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
                        @Valid @RequestBody UpdateTripRequest request,
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
                        .message("Đã chuyển chuyến đi vào thùng rác")
                        .build();
        }

        @GetMapping("/trash")
        public ApiResponse<List<TripResponse>> getDeletedTrips(Authentication authentication) {
                Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
                return ApiResponse.<List<TripResponse>>builder()
                        .success(true)
                        .data(tripService.getDeletedTrips(userId))
                        .build();
        }

        @PutMapping("/{tripId}/restore")
        public ApiResponse<?> restoreTrip(@PathVariable Long tripId, Authentication authentication) {
                Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
                tripService.restoreTrip(tripId, userId);
                return ApiResponse.builder()
                        .success(true)
                        .message("Đã phục hồi chuyến đi")
                        .build();
        }

        @DeleteMapping("/{tripId}/force")
        public ApiResponse<?> forceDeleteTrip(@PathVariable Long tripId, Authentication authentication) {
                Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
                tripService.forceDeleteTrip(tripId, userId);
                return ApiResponse.builder()
                        .success(true)
                        .message("Đã xóa vĩnh viễn chuyến đi")
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

        @PostMapping("/{tripId}/members/import")
        public ApiResponse<?> importMembers(
                        @PathVariable Long tripId,
                        @RequestBody com.kaitohuy.chiabill.dto.request.ImportMembersRequest request,
                        Authentication authentication) {

                Long ownerId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
                tripService.importMembers(tripId, ownerId, request);

                return ApiResponse.builder()
                                .success(true)
                                .message("Đã nhập thành viên thành công!")
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
        public ResponseEntity<byte[]> exportExcel(
                        @PathVariable Long tripId,
                        @RequestParam(defaultValue = "false") boolean includeDetails,
                        @RequestParam(defaultValue = "false") boolean includeSettlement,
                        Authentication authentication) {
                Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
                byte[] data = exportService.exportTripToExcel(tripId, userId, includeDetails, includeSettlement);
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=trip_report_" + tripId + ".xlsx")
                                .contentType(MediaType.parseMediaType(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                .body(data);
        }

        @GetMapping("/{tripId}/export/pdf")
        public ResponseEntity<byte[]> exportPdf(
                        @PathVariable Long tripId,
                        @RequestParam(defaultValue = "false") boolean includeDetails,
                        @RequestParam(defaultValue = "false") boolean includeSettlement,
                        Authentication authentication) {
                Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
                byte[] data = exportService.exportTripToPdf(tripId, userId, includeDetails, includeSettlement);
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=trip_report_" + tripId + ".pdf")
                                .contentType(MediaType.APPLICATION_PDF)
                                .body(data);
        }

        @PutMapping(value = "/{tripId}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ApiResponse<TripResponse> updateTripCover(
                        @PathVariable Long tripId,
                        @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                        Authentication authentication) {

                Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

                return ApiResponse.<TripResponse>builder()
                                .success(true)
                                .data(tripService.updateTripCover(tripId, userId, file))
                                .build();
        }
}
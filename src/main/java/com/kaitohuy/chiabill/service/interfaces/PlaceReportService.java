package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.request.PlaceReportRequest;

public interface PlaceReportService {
    void submitReport(Long placeId, PlaceReportRequest request, Long userId);
    org.springframework.data.domain.Page<com.kaitohuy.chiabill.dto.response.PlaceReportResponse> getReports(org.springframework.data.domain.Pageable pageable);
    void approveReport(Long id);
    void rejectReport(Long id);
}

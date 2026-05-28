package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.request.PlaceReportRequest;

public interface PlaceReportService {
    void submitReport(Long placeId, PlaceReportRequest request, Long userId);
}

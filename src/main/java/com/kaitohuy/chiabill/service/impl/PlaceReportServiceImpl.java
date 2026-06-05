package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.request.PlaceReportRequest;
import com.kaitohuy.chiabill.entity.Place;
import com.kaitohuy.chiabill.entity.PlaceReport;
import com.kaitohuy.chiabill.entity.User;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.repository.PlaceReportRepository;
import com.kaitohuy.chiabill.repository.PlaceRepository;
import com.kaitohuy.chiabill.repository.UserRepository;
import com.kaitohuy.chiabill.service.interfaces.PlaceReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceReportServiceImpl implements PlaceReportService {

    private final PlaceReportRepository placeReportRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void submitReport(Long placeId, PlaceReportRequest request, Long userId) {
        Place place = placeRepository.findByIdAndIsDeletedFalse(placeId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy địa điểm"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng"));

        PlaceReport report = PlaceReport.builder()
                .place(place)
                .user(user)
                .reportType(request.getReportType())
                .description(request.getDescription())
                .status("PENDING")
                .build();

        placeReportRepository.save(report);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.kaitohuy.chiabill.dto.response.PlaceReportResponse> getReports(org.springframework.data.domain.Pageable pageable) {
        return placeReportRepository.findAllByIsDeletedFalse(pageable)
                .map(report -> com.kaitohuy.chiabill.dto.response.PlaceReportResponse.builder()
                        .id(report.getId())
                        .placeId(report.getPlace().getId())
                        .placeName(report.getPlace().getName())
                        .placeCategory(report.getPlace().getCategory())
                        .placeCity(report.getPlace().getCity())
                        .userId(report.getUser().getId())
                        .userName(report.getUser().getName())
                        .reportType(report.getReportType())
                        .description(report.getDescription())
                        .status(report.getStatus())
                        .createdAt(report.getCreatedAt())
                        .build());
    }

    @Override
    @Transactional
    public void approveReport(Long id) {
        PlaceReport report = placeReportRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy báo cáo"));
        report.setStatus("APPROVED");

        Place place = report.getPlace();
        place.setIsDeleted(true);
        placeRepository.save(place);

        placeReportRepository.save(report);
    }

    @Override
    @Transactional
    public void rejectReport(Long id) {
        PlaceReport report = placeReportRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy báo cáo"));
        report.setStatus("REJECTED");
        placeReportRepository.save(report);
    }
}

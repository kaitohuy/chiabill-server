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

@Service
@RequiredArgsConstructor
public class PlaceReportServiceImpl implements PlaceReportService {

    private final PlaceReportRepository placeReportRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;

    @Override
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
}

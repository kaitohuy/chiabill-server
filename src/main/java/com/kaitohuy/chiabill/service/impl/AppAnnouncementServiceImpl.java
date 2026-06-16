package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.request.AppAnnouncementRequest;
import com.kaitohuy.chiabill.dto.response.AppAnnouncementResponse;
import com.kaitohuy.chiabill.dto.response.PageResponse;
import com.kaitohuy.chiabill.entity.AppAnnouncement;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.mapper.AppAnnouncementMapper;
import com.kaitohuy.chiabill.repository.AppAnnouncementRepository;
import com.kaitohuy.chiabill.service.interfaces.AppAnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppAnnouncementServiceImpl implements AppAnnouncementService {

    private final AppAnnouncementRepository announcementRepository;
    private final AppAnnouncementMapper announcementMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AppAnnouncementResponse> getActiveAnnouncements(AppAnnouncement.Platform platform) {
        List<AppAnnouncement> announcements = announcementRepository
                .findActiveAnnouncements(platform, LocalDateTime.now());
        return announcements.stream()
                .map(announcementMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AppAnnouncementResponse> getAllAnnouncements(Pageable pageable) {
        Page<AppAnnouncement> page = announcementRepository
                .findAllByIsDeletedFalseOrderByPriorityDescCreatedAtDesc(pageable);
        return PageResponse.from(page.map(announcementMapper::toResponse));
    }

    @Override
    @Transactional
    public AppAnnouncementResponse createAnnouncement(Long adminId, AppAnnouncementRequest request) {
        AppAnnouncement announcement = AppAnnouncement.builder()
                .type(request.getType())
                .title(request.getTitle())
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .actionType(request.getActionType() != null ? request.getActionType() : AppAnnouncement.ActionType.NONE)
                .actionUrl(request.getActionUrl())
                .actionLabel(request.getActionLabel())
                // Update fields
                .minVersion(request.getMinVersion())
                .latestVersion(request.getLatestVersion())
                .isForceUpdate(request.getIsForceUpdate() != null && request.getIsForceUpdate())
                // Payment / Donate fields
                .qrImageUrl(request.getQrImageUrl())
                .bankInfo(request.getBankInfo())
                .suggestedAmount(request.getSuggestedAmount())
                // Display
                .platform(request.getPlatform() != null ? request.getPlatform() : AppAnnouncement.Platform.ALL)
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .isDismissible(request.getIsDismissible() == null || request.getIsDismissible())
                .displayMode(request.getDisplayMode() != null ? request.getDisplayMode() : AppAnnouncement.DisplayMode.ONCE)
                // Time
                .isActive(request.getIsActive() == null || request.getIsActive())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .createdBy(adminId)
                .build();

        return announcementMapper.toResponse(announcementRepository.save(announcement));
    }

    @Override
    @Transactional
    public AppAnnouncementResponse updateAnnouncement(Long id, AppAnnouncementRequest request) {
        AppAnnouncement announcement = findActiveOrThrow(id);

        if (request.getType() != null) announcement.setType(request.getType());
        if (request.getTitle() != null) announcement.setTitle(request.getTitle());
        if (request.getContent() != null) announcement.setContent(request.getContent());
        if (request.getImageUrl() != null) announcement.setImageUrl(request.getImageUrl());
        if (request.getActionType() != null) announcement.setActionType(request.getActionType());
        if (request.getActionUrl() != null) announcement.setActionUrl(request.getActionUrl());
        if (request.getActionLabel() != null) announcement.setActionLabel(request.getActionLabel());
        if (request.getMinVersion() != null) announcement.setMinVersion(request.getMinVersion());
        if (request.getLatestVersion() != null) announcement.setLatestVersion(request.getLatestVersion());
        if (request.getIsForceUpdate() != null) announcement.setIsForceUpdate(request.getIsForceUpdate());
        if (request.getQrImageUrl() != null) announcement.setQrImageUrl(request.getQrImageUrl());
        if (request.getBankInfo() != null) announcement.setBankInfo(request.getBankInfo());
        if (request.getSuggestedAmount() != null) announcement.setSuggestedAmount(request.getSuggestedAmount());
        if (request.getPlatform() != null) announcement.setPlatform(request.getPlatform());
        if (request.getPriority() != null) announcement.setPriority(request.getPriority());
        if (request.getIsDismissible() != null) announcement.setIsDismissible(request.getIsDismissible());
        if (request.getDisplayMode() != null) announcement.setDisplayMode(request.getDisplayMode());
        if (request.getIsActive() != null) announcement.setIsActive(request.getIsActive());
        if (request.getStartAt() != null) announcement.setStartAt(request.getStartAt());
        if (request.getEndAt() != null) announcement.setEndAt(request.getEndAt());

        return announcementMapper.toResponse(announcementRepository.save(announcement));
    }

    @Override
    @Transactional
    public AppAnnouncementResponse toggleActive(Long id) {
        AppAnnouncement announcement = findActiveOrThrow(id);
        announcement.setIsActive(!announcement.getIsActive());
        return announcementMapper.toResponse(announcementRepository.save(announcement));
    }

    @Override
    @Transactional
    public void deleteAnnouncement(Long id) {
        AppAnnouncement announcement = findActiveOrThrow(id);
        announcement.setIsDeleted(true);
        announcementRepository.save(announcement);
    }

    private AppAnnouncement findActiveOrThrow(Long id) {
        return announcementRepository.findById(id)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông báo"));
    }
}

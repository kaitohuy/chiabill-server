package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.request.SystemFeedbackRequest;
import com.kaitohuy.chiabill.dto.response.PageResponse;
import com.kaitohuy.chiabill.dto.response.SystemFeedbackResponse;
import com.kaitohuy.chiabill.entity.SystemFeedback;
import com.kaitohuy.chiabill.entity.User;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.mapper.SystemFeedbackMapper;
import com.kaitohuy.chiabill.repository.SystemFeedbackRepository;
import com.kaitohuy.chiabill.repository.UserRepository;
import com.kaitohuy.chiabill.service.interfaces.SystemFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemFeedbackServiceImpl implements SystemFeedbackService {

    private final SystemFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final SystemFeedbackMapper feedbackMapper;

    @Override
    @Transactional
    public SystemFeedbackResponse saveFeedback(Long userId, SystemFeedbackRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        SystemFeedback feedback = SystemFeedback.builder()
                .user(user)
                .content(request.getContent())
                .build();

        SystemFeedback saved = feedbackRepository.save(feedback);
        return feedbackMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SystemFeedbackResponse> getFeedbacks(Pageable pageable) {
        Page<SystemFeedback> entityPage = feedbackRepository.findAllByIsDeletedFalse(pageable);
        Page<SystemFeedbackResponse> responsePage = entityPage.map(feedbackMapper::toResponse);
        return PageResponse.from(responsePage);
    }

    @Override
    @Transactional
    public void deleteFeedback(Long id) {
        SystemFeedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phản hồi"));
        feedback.setIsDeleted(true);
        feedbackRepository.save(feedback);
    }
}

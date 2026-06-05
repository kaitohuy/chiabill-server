package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.request.SystemFeedbackRequest;
import com.kaitohuy.chiabill.dto.response.SystemFeedbackResponse;
import com.kaitohuy.chiabill.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface SystemFeedbackService {
    SystemFeedbackResponse saveFeedback(Long userId, SystemFeedbackRequest request);
    PageResponse<SystemFeedbackResponse> getFeedbacks(Pageable pageable);
    void deleteFeedback(Long id);
}

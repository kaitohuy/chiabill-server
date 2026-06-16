package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.request.AppAnnouncementRequest;
import com.kaitohuy.chiabill.dto.response.AppAnnouncementResponse;
import com.kaitohuy.chiabill.dto.response.PageResponse;
import com.kaitohuy.chiabill.entity.AppAnnouncement;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AppAnnouncementService {

    /** [Client] Lấy danh sách thông báo đang active cho đúng platform */
    List<AppAnnouncementResponse> getActiveAnnouncements(AppAnnouncement.Platform platform);

    /** [Admin] Lấy tất cả thông báo có phân trang */
    PageResponse<AppAnnouncementResponse> getAllAnnouncements(Pageable pageable);

    /** [Admin] Tạo thông báo mới */
    AppAnnouncementResponse createAnnouncement(Long adminId, AppAnnouncementRequest request);

    /** [Admin] Chỉnh sửa thông báo */
    AppAnnouncementResponse updateAnnouncement(Long id, AppAnnouncementRequest request);

    /** [Admin] Bật/tắt thông báo */
    AppAnnouncementResponse toggleActive(Long id);

    /** [Admin] Xoá mềm thông báo */
    void deleteAnnouncement(Long id);
}

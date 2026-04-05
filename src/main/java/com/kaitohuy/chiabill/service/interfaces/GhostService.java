package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.request.CreateGhostMembersRequest;
import com.kaitohuy.chiabill.dto.response.GhostMemberResponse;
import com.kaitohuy.chiabill.dto.response.UserResponse;

import java.util.List;

public interface GhostService {

    /**
     * Tạo danh sách ghost members và tự động thêm vào trip.
     * Chỉ OWNER của trip mới được gọi API này.
     */
    List<GhostMemberResponse> createGhostMembers(Long tripId, Long callerId, CreateGhostMembersRequest request);

    /**
     * Real user "claim" ghost account — chuyển toàn bộ dữ liệu nợ từ ghost sang real user.
     * Caller phải là member trong cùng trip với ghost.
     */
    UserResponse claimGhost(Long ghostId, Long realUserId);
}

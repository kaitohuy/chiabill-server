package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.request.CreateInviteRequest;
import com.kaitohuy.chiabill.dto.response.InvitationResponse;
import com.kaitohuy.chiabill.dto.response.InviteInfoResponse;
import com.kaitohuy.chiabill.dto.response.TripResponse;

public interface InvitationService {

    InvitationResponse createInvite(Long tripId, Long callerId, CreateInviteRequest request);

    InvitationResponse getActiveInvite(Long tripId, Long userId);

    InviteInfoResponse getInviteInfo(String inviteId);

    TripResponse joinByInvite(String inviteId, Long userId);
}

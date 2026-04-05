package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.request.AddMemberDirectRequest;
import com.kaitohuy.chiabill.dto.request.CreateTripRequest;
import com.kaitohuy.chiabill.dto.request.UpdateTripRequest;
import com.kaitohuy.chiabill.dto.response.PageResponse;
import com.kaitohuy.chiabill.dto.response.TripResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TripService {

    TripResponse createTrip(Long userId, CreateTripRequest request);

    List<TripResponse> getMyTrips(Long userId);

    PageResponse<TripResponse> getMyTripsPaginated(Long userId, String keyword, Pageable pageable);

    TripResponse getTripDetail(Long tripId, Long userId);

    void addMember(Long tripId, Long userId, Long targetUserId);

    void addDirectMember(Long tripId, Long ownerId, AddMemberDirectRequest request);

    void joinTrip(Long tripId, Long userId);

    TripResponse updateTrip(Long tripId, Long userId, UpdateTripRequest request);

    void deleteTrip(Long tripId, Long userId);

    void leaveTrip(Long tripId, Long userId);

    void transferOwner(Long tripId, Long ownerId, Long newOwnerId);

    void disableMember(Long tripId, Long moderatorId, Long targetUserId);

    void kickMember(Long tripId, Long ownerId, Long targetUserId, boolean forgiveDebt);

    void activateMember(Long tripId, Long ownerId, Long targetUserId);
}
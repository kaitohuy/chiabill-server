package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.request.CreateInviteRequest;
import com.kaitohuy.chiabill.dto.response.InvitationResponse;
import com.kaitohuy.chiabill.dto.response.InviteInfoResponse;
import com.kaitohuy.chiabill.dto.response.TripResponse;
import com.kaitohuy.chiabill.entity.Trip;
import com.kaitohuy.chiabill.entity.TripInvitation;
import com.kaitohuy.chiabill.entity.TripMember;
import com.kaitohuy.chiabill.entity.User;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.repository.TripInvitationRepository;
import com.kaitohuy.chiabill.repository.TripMemberRepository;
import com.kaitohuy.chiabill.repository.TripRepository;
import com.kaitohuy.chiabill.repository.UserRepository;
import com.kaitohuy.chiabill.service.interfaces.InvitationService;
import com.kaitohuy.chiabill.service.interfaces.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    private final TripInvitationRepository invitationRepository;
    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final UserRepository userRepository;
    private final TripService tripService;

    @Override
    @Transactional
    public InvitationResponse createInvite(Long tripId, Long callerId, CreateInviteRequest request) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException("Trip not found"));

        if (Boolean.TRUE.equals(trip.getIsDeleted())) {
            throw new BusinessException("Trip has been deleted");
        }

        // Must be a member of the trip to invite
        if (!tripMemberRepository.existsByTripIdAndUserId(tripId, callerId)) {
            throw new BusinessException("Only members can create invite links");
        }

        User creator = userRepository.findById(callerId)
                .orElseThrow(() -> new BusinessException("User not found"));

        String inviteId;

        if (request != null && request.getCustomCode() != null && !request.getCustomCode().trim().isEmpty()) {
            inviteId = request.getCustomCode().trim();
            if (invitationRepository.existsById(inviteId)) {
                throw new BusinessException("Custom code already exists. Please choose another one.");
            }
        } else {
            inviteId = UUID.randomUUID().toString().substring(0, 8);
            while (invitationRepository.existsById(inviteId)) {
                inviteId = UUID.randomUUID().toString().substring(0, 8);
            }
        }
        
        TripInvitation invitation = TripInvitation.builder()
                .id(inviteId)
                .trip(trip)
                .createdBy(creator)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .isActive(true)
                .build();

        invitationRepository.save(invitation);

        List<TripMember> members = tripMemberRepository.findByTripIdAndIsActiveTrue(tripId);

        return InvitationResponse.builder()
                .inviteCode(inviteId)
                .inviteLink("chiabill://join/" + inviteId)
                .tripId(tripId)
                .tripName(trip.getName())
                .memberCount(members.size())
                .expiresAt(invitation.getExpiresAt())
                .build();
    }

    @Override
    public InvitationResponse getActiveInvite(Long tripId, Long userId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException("Trip not found"));

        if (!tripMemberRepository.existsByTripIdAndUserId(tripId, userId)) {
            throw new BusinessException("Only members can view the invite link");
        }

        TripInvitation invitation = invitationRepository.findFirstByTripIdAndIsActiveTrueOrderByCreatedAtDesc(tripId)
                .orElse(null);

        if (invitation == null || invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            return null; // Không có link nào đang mờ hoặc link đã hết hạn
        }

        List<TripMember> members = tripMemberRepository.findByTripIdAndIsActiveTrue(tripId);

        return InvitationResponse.builder()
                .inviteCode(invitation.getId())
                .inviteLink("chiabill://join/" + invitation.getId())
                .tripId(tripId)
                .tripName(trip.getName())
                .memberCount(members.size())
                .expiresAt(invitation.getExpiresAt())
                .build();
    }

    @Override
    public InviteInfoResponse getInviteInfo(String inviteId) {

        TripInvitation invitation = invitationRepository.findByIdAndIsActiveTrue(inviteId)
                .orElseThrow(() -> new BusinessException("Invalid or inactive invite link"));

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Invite link has expired");
        }

        Trip trip = invitation.getTrip();

        if (Boolean.TRUE.equals(trip.getIsDeleted())) {
            throw new BusinessException("Trip has been deleted");
        }

        List<TripMember> members = tripMemberRepository.findByTripIdAndIsActiveTrue(trip.getId());

        return InviteInfoResponse.builder()
                .tripName(trip.getName())
                .description(trip.getDescription())
                .memberCount(members.size())
                .createdByName(invitation.getCreatedBy().getName())
                .expiresAt(invitation.getExpiresAt())
                .build();
    }

    @Override
    @Transactional
    public TripResponse joinByInvite(String inviteId, Long userId) {

        TripInvitation invitation = invitationRepository.findByIdAndIsActiveTrue(inviteId)
                .orElseThrow(() -> new BusinessException("Invalid or inactive invite link"));

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Invite link has expired");
        }

        Trip trip = invitation.getTrip();
        if (Boolean.TRUE.equals(trip.getIsDeleted())) {
            throw new BusinessException("Trip has been deleted");
        }

        // Using the existing join logic from TripService
        tripService.joinTrip(trip.getId(), userId);

        return tripService.getTripDetail(trip.getId(), userId);
    }
}

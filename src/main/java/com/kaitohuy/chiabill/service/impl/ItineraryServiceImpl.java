package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.request.SaveItineraryRequest;
import com.kaitohuy.chiabill.dto.response.ItineraryItemResponse;
import com.kaitohuy.chiabill.entity.ItineraryItem;
import com.kaitohuy.chiabill.entity.Trip;
import com.kaitohuy.chiabill.entity.TripMember;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.repository.ItineraryItemRepository;
import com.kaitohuy.chiabill.repository.TripMemberRepository;
import com.kaitohuy.chiabill.repository.TripRepository;
import com.kaitohuy.chiabill.service.interfaces.ItineraryService;
import com.kaitohuy.chiabill.service.interfaces.ItineraryNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItineraryServiceImpl implements ItineraryService {

    private final ItineraryItemRepository itineraryItemRepository;
    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final ItineraryNotificationService itineraryNotificationService;

    @Override
    public List<ItineraryItemResponse> getItinerary(Long tripId, Long userId) {
        validateActiveMember(tripId, userId);

        List<ItineraryItem> items = itineraryItemRepository.findActiveItineraryByTripId(tripId);
        return items.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ItineraryItemResponse> saveItineraryBulk(Long tripId, Long userId, List<SaveItineraryRequest> requests) {
        validateActiveMember(tripId, userId);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException("Trip not found"));

        // Xóa sạch toàn bộ lịch trình cũ để ghi đè hàng loạt (tối ưu cho import Excel)
        itineraryItemRepository.hardDeleteByTripId(tripId);

        if (requests == null || requests.isEmpty()) {
            itineraryNotificationService.rescheduleAlarmsForTrip(tripId);
            return List.of();
        }

        List<ItineraryItem> itemsToSave = requests.stream()
                .map(req -> ItineraryItem.builder()
                        .trip(trip)
                        .dayNumber(req.getDayNumber())
                        .timeRange(req.getTimeRange())
                        .activity(req.getActivity())
                        .location(req.getLocation())
                        .note(req.getNote())
                        .estimatedCost(req.getEstimatedCost())
                        .build())
                .collect(Collectors.toList());

        List<ItineraryItem> savedItems = itineraryItemRepository.saveAll(itemsToSave);

        itineraryNotificationService.rescheduleAlarmsForTrip(tripId);

        return savedItems.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ItineraryItemResponse saveItineraryItem(Long tripId, Long userId, SaveItineraryRequest request) {
        validateActiveMember(tripId, userId);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException("Trip not found"));

        ItineraryItem item;
        if (request.getId() != null) {
            // Cập nhật hoạt động lẻ
            item = itineraryItemRepository.findById(request.getId())
                    .orElseThrow(() -> new BusinessException("Itinerary activity not found"));

            if (!item.getTrip().getId().equals(tripId)) {
                throw new BusinessException("Itinerary activity does not belong to this trip");
            }

            item.setDayNumber(request.getDayNumber());
            item.setTimeRange(request.getTimeRange());
            item.setActivity(request.getActivity());
            item.setLocation(request.getLocation());
            item.setNote(request.getNote());
            item.setEstimatedCost(request.getEstimatedCost());
        } else {
            // Thêm mới hoạt động lẻ
            item = ItineraryItem.builder()
                    .trip(trip)
                    .dayNumber(request.getDayNumber())
                    .timeRange(request.getTimeRange())
                    .activity(request.getActivity())
                    .location(request.getLocation())
                    .note(request.getNote())
                    .estimatedCost(request.getEstimatedCost())
                    .build();
        }

        ItineraryItem saved = itineraryItemRepository.save(item);
        itineraryNotificationService.rescheduleAlarmsForTrip(tripId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteItineraryItem(Long tripId, Long itemId, Long userId) {
        validateActiveMember(tripId, userId);

        ItineraryItem item = itineraryItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("Itinerary activity not found"));

        if (!item.getTrip().getId().equals(tripId)) {
            throw new BusinessException("Itinerary activity does not belong to this trip");
        }

        itineraryItemRepository.delete(item);
        itineraryNotificationService.rescheduleAlarmsForTrip(tripId);
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private void validateActiveMember(Long tripId, Long userId) {
        TripMember member = tripMemberRepository.findByTripIdAndUserId(tripId, userId)
                .orElseThrow(() -> new BusinessException("User is not a member of this trip"));

        if (!Boolean.TRUE.equals(member.getIsActive())) {
            throw new BusinessException("You are not active in this trip");
        }
    }

    private ItineraryItemResponse mapToResponse(ItineraryItem item) {
        return ItineraryItemResponse.builder()
                .id(item.getId())
                .dayNumber(item.getDayNumber())
                .timeRange(item.getTimeRange())
                .activity(item.getActivity())
                .location(item.getLocation())
                .note(item.getNote())
                .estimatedCost(item.getEstimatedCost())
                .build();
    }
}

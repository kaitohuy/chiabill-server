package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.request.SaveItineraryRequest;
import com.kaitohuy.chiabill.dto.response.ItineraryItemResponse;

import java.util.List;

public interface ItineraryService {
    List<ItineraryItemResponse> getItinerary(Long tripId, Long userId);
    List<ItineraryItemResponse> saveItineraryBulk(Long tripId, Long userId, List<SaveItineraryRequest> requests);
    ItineraryItemResponse saveItineraryItem(Long tripId, Long userId, SaveItineraryRequest request);
    void deleteItineraryItem(Long tripId, Long itemId, Long userId);
}

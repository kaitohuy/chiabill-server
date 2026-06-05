package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.request.PlaceRequest;
import java.util.List;

public interface PlaceSeedService {
    void seedPlacesAsync(List<PlaceRequest> requests);
}

package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.response.SettlementResponse;

import java.util.List;

public interface SettlementService {

    List<SettlementResponse> calculateSettlement(Long tripId, Long userId);
}
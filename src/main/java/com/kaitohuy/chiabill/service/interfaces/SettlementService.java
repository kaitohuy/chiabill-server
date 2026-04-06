package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.response.SettlementResponse;
import com.kaitohuy.chiabill.dto.response.SettlementSummaryResponse;

import java.util.List;

public interface SettlementService {

    List<SettlementResponse> calculateSettlement(Long tripId, Long userId);

    SettlementSummaryResponse getSettlementSummary(Long userId);
}
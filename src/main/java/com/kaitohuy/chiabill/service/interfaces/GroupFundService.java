package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.request.ActivateFundRequest;
import com.kaitohuy.chiabill.dto.request.RequiredContributionRequest;
import com.kaitohuy.chiabill.dto.request.UpdateTreasurerRequest;
import com.kaitohuy.chiabill.dto.request.VoluntaryContributionRequest;
import com.kaitohuy.chiabill.dto.response.FundContributionResponse;
import com.kaitohuy.chiabill.dto.response.FundResponse;

import java.util.List;

public interface GroupFundService {
    FundResponse getFundByTrip(Long tripId, Long actorId);
    FundResponse activateFund(Long tripId, Long actorId, ActivateFundRequest request);
    FundResponse updateTreasurer(Long tripId, Long actorId, UpdateTreasurerRequest request);
    List<FundContributionResponse> createRequiredContribution(Long tripId, Long actorId, RequiredContributionRequest request);
    FundContributionResponse createVoluntaryContribution(Long tripId, Long actorId, VoluntaryContributionRequest request);
    List<FundContributionResponse> getContributions(Long tripId, Long actorId);
    FundContributionResponse confirmContribution(Long contributionId, Long actorId);
}

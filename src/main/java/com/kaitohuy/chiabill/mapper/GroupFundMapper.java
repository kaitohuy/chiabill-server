package com.kaitohuy.chiabill.mapper;

import com.kaitohuy.chiabill.dto.response.FundContributionResponse;
import com.kaitohuy.chiabill.dto.response.FundResponse;
import com.kaitohuy.chiabill.entity.GroupFund;
import com.kaitohuy.chiabill.entity.GroupFundContribution;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface GroupFundMapper {

    @Mapping(source = "trip.id", target = "tripId")
    @Mapping(source = "treasurer", target = "treasurer")
    FundResponse toResponse(GroupFund fund);

    @Mapping(source = "groupFund.id", target = "fundId")
    @Mapping(source = "contributor", target = "contributor")
    FundContributionResponse toContributionResponse(GroupFundContribution contribution);
}

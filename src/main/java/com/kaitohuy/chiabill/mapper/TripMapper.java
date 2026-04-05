package com.kaitohuy.chiabill.mapper;

import com.kaitohuy.chiabill.dto.response.*;
import com.kaitohuy.chiabill.entity.Trip;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface TripMapper {

    @Mapping(target = "members", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    TripResponse toResponse(Trip trip);
}
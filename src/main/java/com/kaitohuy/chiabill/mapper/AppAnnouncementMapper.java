package com.kaitohuy.chiabill.mapper;

import com.kaitohuy.chiabill.dto.response.AppAnnouncementResponse;
import com.kaitohuy.chiabill.entity.AppAnnouncement;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AppAnnouncementMapper {

    AppAnnouncementResponse toResponse(AppAnnouncement announcement);
}

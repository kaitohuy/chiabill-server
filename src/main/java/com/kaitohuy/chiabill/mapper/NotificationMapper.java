package com.kaitohuy.chiabill.mapper;

import com.kaitohuy.chiabill.dto.response.NotificationResponse;
import com.kaitohuy.chiabill.entity.Notification;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationResponse toResponse(Notification notification);
    List<NotificationResponse> toResponseList(List<Notification> notifications);
}

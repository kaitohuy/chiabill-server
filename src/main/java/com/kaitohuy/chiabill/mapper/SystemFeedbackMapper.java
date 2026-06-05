package com.kaitohuy.chiabill.mapper;

import com.kaitohuy.chiabill.dto.response.SystemFeedbackResponse;
import com.kaitohuy.chiabill.entity.SystemFeedback;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface SystemFeedbackMapper {

    SystemFeedbackResponse toResponse(SystemFeedback feedback);
}

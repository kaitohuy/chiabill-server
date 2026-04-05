package com.kaitohuy.chiabill.mapper;

import com.kaitohuy.chiabill.dto.response.UserResponse;
import com.kaitohuy.chiabill.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
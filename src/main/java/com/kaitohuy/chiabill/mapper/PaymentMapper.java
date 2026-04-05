package com.kaitohuy.chiabill.mapper;

import com.kaitohuy.chiabill.dto.response.PaymentResponse;
import com.kaitohuy.chiabill.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(source = "payment.id", target = "id")
    @Mapping(source = "payment.trip.id", target = "tripId")
    @Mapping(source = "payment.fromUser.id", target = "fromUserId")
    @Mapping(source = "payment.fromUser.name", target = "fromUserName")
    @Mapping(source = "payment.toUser.id", target = "toUserId")
    @Mapping(source = "payment.toUser.name", target = "toUserName")
    PaymentResponse toResponse(Payment payment);
}

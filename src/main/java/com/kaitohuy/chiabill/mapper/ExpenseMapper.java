package com.kaitohuy.chiabill.mapper;

import com.kaitohuy.chiabill.dto.response.*;
import com.kaitohuy.chiabill.entity.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ExpenseMapper {

    @Mapping(source = "payer", target = "payer")
    @Mapping(source = "trip.id", target = "tripId")
    @Mapping(source = "currency", target = "currency")
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "category.icon", target = "categoryIcon")
    ExpenseResponse toResponse(Expense expense);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.name", target = "userName")
    SplitResponse toSplitResponse(ExpenseSplit split);
}
package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.response.ExpenseCategoryResponse;

import java.util.List;

public interface CategoryService {
    List<ExpenseCategoryResponse> getCategories(Long tripId);
    ExpenseCategoryResponse createCustomCategory(Long tripId, Long userId, String name, String icon);
    void seedDefaultCategories();
}

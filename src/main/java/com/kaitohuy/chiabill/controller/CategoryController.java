package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.dto.response.ExpenseCategoryResponse;
import com.kaitohuy.chiabill.security.UserPrincipal;
import com.kaitohuy.chiabill.service.interfaces.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/trip/{tripId}")
    public ApiResponse<List<ExpenseCategoryResponse>> getCategories(@PathVariable Long tripId) {
        return ApiResponse.<List<ExpenseCategoryResponse>>builder()
                .success(true)
                .data(categoryService.getCategories(tripId))
                .build();
    }

    @PostMapping("/trip/{tripId}")
    public ApiResponse<ExpenseCategoryResponse> createCustomCategory(
            @PathVariable Long tripId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        String name = request.get("name");
        String icon = request.get("icon");

        return ApiResponse.<ExpenseCategoryResponse>builder()
                .success(true)
                .data(categoryService.createCustomCategory(tripId, userId, name, icon))
                .build();
    }

    @PostMapping("/seed")
    public ApiResponse<String> seedDefaults() {
        categoryService.seedDefaultCategories();
        log.info("seedDefaults success");
        return ApiResponse.<String>builder()
                .success(true)
                .message("Default categories seeded")
                .build();
    }
}

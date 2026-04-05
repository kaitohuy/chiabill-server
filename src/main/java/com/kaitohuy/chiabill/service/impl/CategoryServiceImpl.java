package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.response.ExpenseCategoryResponse;
import com.kaitohuy.chiabill.entity.ExpenseCategory;
import com.kaitohuy.chiabill.entity.Trip;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.repository.ExpenseCategoryRepository;
import com.kaitohuy.chiabill.repository.TripRepository;
import com.kaitohuy.chiabill.service.interfaces.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final ExpenseCategoryRepository categoryRepository;
    private final TripRepository tripRepository;

    @Override
    public List<ExpenseCategoryResponse> getCategories(Long tripId) {
        return categoryRepository.findAllByTripIdOrSystem(tripId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ExpenseCategoryResponse createCustomCategory(Long tripId, Long userId, String name, String icon) {
        // 1. Kiểm tra trip & Quyền (Tạm thời cho phép ai cũng tạo được category trong trip mình tham dự)
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException("Trip not found"));

        // 2. Tạo mới
        ExpenseCategory category = ExpenseCategory.builder()
                .name(name)
                .icon(icon)
                .trip(trip)
                .build();

        return mapToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void seedDefaultCategories() {
        if (categoryRepository.findByTripIdIsNull().isEmpty()) {
            List<ExpenseCategory> defaults = List.of(
                    // 1. Nhóm Ăn uống (Food & Drink)
                    ExpenseCategory.builder().name("Ăn uống (Chung)").icon("🍽️").build(),
                    ExpenseCategory.builder().name("Cà phê & Nước").icon("☕").build(),
                    ExpenseCategory.builder().name("Nhậu & Party").icon("🍻").build(),
                    ExpenseCategory.builder().name("Ăn vặt / Lề đường").icon("🍢").build(),
                    ExpenseCategory.builder().name("Đi chợ / Siêu thị").icon("🛒").build(),

                    // 2. Nhóm Di chuyển (Transportation)
                    ExpenseCategory.builder().name("Vé máy bay / Tàu").icon("✈️").build(),
                    ExpenseCategory.builder().name("Xe khách / Bus").icon("🚌").build(),
                    ExpenseCategory.builder().name("Taxi / Grab").icon("🚕").build(),
                    ExpenseCategory.builder().name("Thuê xe").icon("🛵").build(),
                    ExpenseCategory.builder().name("Đổ xăng / Gửi xe").icon("⛽").build(),
                    ExpenseCategory.builder().name("Phí cầu đường / Trạm thu phí").icon("🛣️").build(),

                    // 3. Nhóm Lưu trú (Accommodation)
                    ExpenseCategory.builder().name("Khách sạn / Homestay").icon("🏨").build(),
                    ExpenseCategory.builder().name("Thuê lều / Cắm trại").icon("⛺").build(),

                    // 4. Nhóm Vui chơi & Giải trí (Entertainment)
                    ExpenseCategory.builder().name("Vé tham quan").icon("🎟️").build(),
                    ExpenseCategory.builder().name("Tour du lịch").icon("🗺️").build(),
                    ExpenseCategory.builder().name("Karaoke / Club").icon("🎤").build(),
                    ExpenseCategory.builder().name("Team Building").icon("🎯").build(),

                    // 5. Nhóm Khác (Miscellaneous)
                    ExpenseCategory.builder().name("Mua sắm / Quà cáp").icon("🎁").build(),
                    ExpenseCategory.builder().name("Y tế / Thuốc men").icon("💊").build(),
                    ExpenseCategory.builder().name("Đồ dùng cá nhân").icon("🧻").build(),
                    ExpenseCategory.builder().name("Tiền Tip / Bồi dưỡng").icon("💸").build(),
                    ExpenseCategory.builder().name("Chi phí phát sinh").icon("⚠").build()
            );
            categoryRepository.saveAll(defaults);
        }
    }

    private ExpenseCategoryResponse mapToResponse(ExpenseCategory ec) {
        return ExpenseCategoryResponse.builder()
                .id(ec.getId())
                .name(ec.getName())
                .icon(ec.getIcon())
                .tripId(ec.getTrip() != null ? ec.getTrip().getId() : null)
                .build();
    }
}

package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.response.ExpenseCategoryResponse;
import com.kaitohuy.chiabill.entity.ExpenseCategory;
import com.kaitohuy.chiabill.entity.Trip;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.repository.ExpenseCategoryRepository;
import com.kaitohuy.chiabill.repository.TripRepository;
import com.kaitohuy.chiabill.repository.TripMemberRepository;
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
    private final TripMemberRepository tripMemberRepository;

    @Override
    public List<ExpenseCategoryResponse> getCategories(Long tripId, Long userId) {
        boolean isMember = tripMemberRepository.existsByTripIdAndUserId(tripId, userId);
        if (!isMember) {
            throw new BusinessException("Access denied: not a member of this trip");
        }
        return categoryRepository.findAllByTripIdOrSystem(tripId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ExpenseCategoryResponse createCustomCategory(Long tripId, Long userId, String name, String icon) {
        boolean isMember = tripMemberRepository.existsByTripIdAndUserId(tripId, userId);
        if (!isMember) {
            throw new BusinessException("Access denied: not a member of this trip");
        }
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
                    ExpenseCategory.builder().name("Ăn uống (Chung)").nameEn("Food & Dining").icon("🍽️").build(),
                    ExpenseCategory.builder().name("Cà phê & Nước").nameEn("Coffee & Drinks").icon("☕").build(),
                    ExpenseCategory.builder().name("Nhậu & Party").nameEn("Drinks & Party").icon("🍻").build(),
                    ExpenseCategory.builder().name("Ăn vặt / Lề đường").nameEn("Street Food / Snacks").icon("🍢").build(),
                    ExpenseCategory.builder().name("Đi chợ / Siêu thị").nameEn("Groceries").icon("🛒").build(),

                    // 2. Nhóm Di chuyển (Transportation)
                    ExpenseCategory.builder().name("Vé máy bay / Tàu").nameEn("Flights / Trains").icon("✈️").build(),
                    ExpenseCategory.builder().name("Xe khách / Bus").nameEn("Bus / Coach").icon("🚌").build(),
                    ExpenseCategory.builder().name("Taxi / Grab").nameEn("Taxi / Grab").icon("🚕").build(),
                    ExpenseCategory.builder().name("Thuê xe").nameEn("Vehicle Rental").icon("🛵").build(),
                    ExpenseCategory.builder().name("Đổ xăng / Gửi xe").nameEn("Gas / Parking").icon("⛽").build(),
                    ExpenseCategory.builder().name("Phí cầu đường / Trạm thu phí").nameEn("Tolls").icon("🛣️").build(),

                    // 3. Nhóm Lưu trú (Accommodation)
                    ExpenseCategory.builder().name("Khách sạn / Homestay").nameEn("Hotel / Homestay").icon("🏨").build(),
                    ExpenseCategory.builder().name("Thuê lều / Cắm trại").nameEn("Camping / Tent").icon("⛺").build(),

                    // 4. Nhóm Vui chơi & Giải trí (Entertainment)
                    ExpenseCategory.builder().name("Vé tham quan").nameEn("Sightseeing Tickets").icon("🎟️").build(),
                    ExpenseCategory.builder().name("Tour du lịch").nameEn("Tour / Guide").icon("🗺️").build(),
                    ExpenseCategory.builder().name("Karaoke / Club").nameEn("Karaoke / Club").icon("🎤").build(),
                    ExpenseCategory.builder().name("Team Building").nameEn("Team Building").icon("🎯").build(),

                    // 5. Nhóm Khác (Miscellaneous)
                    ExpenseCategory.builder().name("Mua sắm / Quà cáp").nameEn("Shopping / Gifts").icon("🎁").build(),
                    ExpenseCategory.builder().name("Y tế / Thuốc men").nameEn("Medical / Pharmacy").icon("💊").build(),
                    ExpenseCategory.builder().name("Đồ dùng cá nhân").nameEn("Personal Care").icon("🧻").build(),
                    ExpenseCategory.builder().name("Tiền Tip / Bồi dưỡng").nameEn("Tips").icon("💸").build(),
                    ExpenseCategory.builder().name("Chi phí phát sinh").nameEn("Incidental Expenses").icon("⚠").build(),
                    ExpenseCategory.builder().name("Quỹ chung").nameEn("Group Fund").icon("💰").build()
            );
            categoryRepository.saveAll(defaults);
        }
    }

    private ExpenseCategoryResponse mapToResponse(ExpenseCategory ec) {
        return ExpenseCategoryResponse.builder()
                .id(ec.getId())
                .name(ec.getName())
                .nameEn(ec.getNameEn())
                .icon(ec.getIcon())
                .tripId(ec.getTrip() != null ? ec.getTrip().getId() : null)
                .build();
    }
}

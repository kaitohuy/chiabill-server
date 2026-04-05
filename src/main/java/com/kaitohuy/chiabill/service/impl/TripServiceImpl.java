package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.request.AddMemberDirectRequest;
import com.kaitohuy.chiabill.dto.request.CreateTripRequest;
import com.kaitohuy.chiabill.dto.request.UpdateTripRequest;
import com.kaitohuy.chiabill.dto.response.PageResponse;
import com.kaitohuy.chiabill.dto.response.TripMemberResponse;
import com.kaitohuy.chiabill.dto.response.TripResponse;
import com.kaitohuy.chiabill.entity.*;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.mapper.TripMapper;
import com.kaitohuy.chiabill.mapper.UserMapper;
import com.kaitohuy.chiabill.repository.*;
import com.kaitohuy.chiabill.repository.specification.TripSpecification;
import com.kaitohuy.chiabill.service.interfaces.EmailService;
import com.kaitohuy.chiabill.service.interfaces.PaymentService;
import com.kaitohuy.chiabill.service.interfaces.SettlementService;
import com.kaitohuy.chiabill.service.interfaces.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final UserRepository userRepository;
    private final TripInvitationRepository invitationRepository;
    private final EmailService emailService;
    private final SettlementService settlementService;
    private final PaymentService paymentService;

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final ExpenseCategoryRepository categoryRepository;

    private final TripMapper tripMapper;
    private final UserMapper userMapper;

    // =========================
    // CREATE TRIP
    // =========================
    @Override
    @Transactional
    public TripResponse createTrip(Long userId, CreateTripRequest request) {

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        Trip trip = Trip.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(creator)
                .build();

        tripRepository.save(trip);

        // add creator vào member
        TripMember member = TripMember.builder()
                .trip(trip)
                .user(creator)
                .role("OWNER")
                .build();

        tripMemberRepository.save(member);

        return buildTripResponse(trip);
    }

    // =========================
    // GET MY TRIPS
    // =========================
    @Override
    public List<TripResponse> getMyTrips(Long userId) {

        List<Trip> trips = tripRepository.findAllByUserId(userId);

        if (trips.isEmpty()) {
            return List.of();
        }

        List<Long> tripIds = trips.stream()
                .map(Trip::getId)
                .toList();

        List<TripMember> allMembers = tripMemberRepository
                .findAllByTripIdsWithUser(tripIds);

        Map<Long, List<TripMemberResponse>> memberMap = allMembers.stream()
                .collect(Collectors.groupingBy(
                        tm -> tm.getTrip().getId(),
                        Collectors.mapping(
                                tm -> TripMemberResponse.builder()
                                        .id(tm.getUser().getId())
                                        .name(tm.getUser().getName())
                                        .avatarUrl(tm.getUser().getAvatarUrl())
                                        .role(tm.getRole())
                                        .status(tm.getStatus().name())
                                        .build(),
                                Collectors.toList()
                        )
                ));

        return trips.stream()
                .map(trip -> {
                    TripResponse res = tripMapper.toResponse(trip);
                    List<TripMemberResponse> members = memberMap.getOrDefault(trip.getId(), List.of());
                    res.setMembers(members);
                    
                    // Gán Owner Id cho trip response
                    members.stream()
                            .filter(m -> "OWNER".equals(m.getRole()))
                            .findFirst()
                            .ifPresent(o -> res.setOwnerId(o.getId()));
                            
                    return res;
                })
                .toList();
    }

    @Override
    public PageResponse<TripResponse> getMyTripsPaginated(Long userId, String keyword, Pageable pageable) {
        // Query dữ liệu với specification
        Page<Trip> tripPage = tripRepository.findAll(
                TripSpecification.filter(userId, keyword),
                pageable
        );

        // Map to response
        List<TripResponse> content = tripPage.getContent().stream()
                .map(this::buildTripResponse)
                .toList();

        return PageResponse.<TripResponse>builder()
                .content(content)
                .pageNumber(tripPage.getNumber())
                .pageSize(tripPage.getSize())
                .totalElements(tripPage.getTotalElements())
                .totalPages(tripPage.getTotalPages())
                .last(tripPage.isLast())
                .build();
    }

    // =========================
    // GET TRIP DETAIL
    // =========================
    @Override
    public TripResponse getTripDetail(Long tripId, Long userId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException("Trip not found"));

        validateUserInTrip(tripId, userId);

        return buildTripResponse(trip);
    }

    // =========================
    // ADD MEMBER
    // =========================
    @Override
    @Transactional
    public void addMember(Long tripId, Long userId, Long targetUserId) {

        // validateOwner đã bao gồm check user có trong trip không
        validateOwner(tripId, userId);

        if (tripMemberRepository.existsByTripIdAndUserId(tripId, targetUserId)) {
            throw new BusinessException("User already in trip");
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException("Trip not found"));

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException("User not found"));

        TripMember member = TripMember.builder()
                .trip(trip)
                .user(user)
                .role("MEMBER")
                .build();

        tripMemberRepository.save(member);
    }

    // =========================
    // JOIN TRIP (link)
    // =========================
    @Override
    @Transactional
    public void joinTrip(Long tripId, Long userId) {

        Optional<TripMember> existing = tripMemberRepository
                .findByTripIdAndUserId(tripId, userId);

        if (existing.isPresent()) {
            TripMember member = existing.get();
            if (!member.getIsActive()) {
                member.setIsActive(true);
                tripMemberRepository.save(member);
            }
            return;
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException("Trip not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        TripMember member = TripMember.builder()
                .trip(trip)
                .user(user)
                .role("MEMBER")
                .build();

        tripMemberRepository.save(member);
    }

    // =========================
    // ADD DIRECT MEMBER (Module 1)
    // =========================
    @Override
    @Transactional
    public void addDirectMember(Long tripId, Long ownerId, AddMemberDirectRequest request) {
        validateOwner(tripId, ownerId);

        String email = (request.getEmail() != null) ? request.getEmail().trim() : null;
        String phone = (request.getPhone() != null) ? request.getPhone().trim() : null;

        if ((email == null || email.isEmpty()) && (phone == null || phone.isEmpty())) {
            throw new BusinessException("Vui lòng cung cấp Email hoặc Số điện thoại để tìm kiếm thành viên.");
        }

        User targetUser = userRepository.findByEmailOrPhone(email, phone)
                .orElseThrow(() -> new BusinessException("Người dùng không tồn tại trong hệ thống."));

        if (tripMemberRepository.existsByTripIdAndUserId(tripId, targetUser.getId())) {
             throw new BusinessException("Người dùng đã là thành viên của chuyến đi.");
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException("Trip not found"));

        if (Boolean.TRUE.equals(targetUser.getAllowAutoAdd())) {
            TripMember member = TripMember.builder()
                    .trip(trip)
                    .user(targetUser)
                    .role("MEMBER")
                    .build();
            tripMemberRepository.save(member);
        } else {
            // Send email
            String inviteCode = getOrCreateInviteCode(trip, ownerId);
            emailService.sendInviteEmail(targetUser.getEmail(), trip.getName(), inviteCode);
            throw new BusinessException("Người dùng này không cho phép thêm tự động. Hệ thống đã gửi email lời mời.");
        }
    }

    private String getOrCreateInviteCode(Trip trip, Long userId) {
        return invitationRepository.findFirstByTripIdAndIsActiveTrueOrderByCreatedAtDesc(trip.getId())
                .map(TripInvitation::getId)
                .orElseGet(() -> {
                     String code = UUID.randomUUID().toString().substring(0, 8);
                     User creator = userRepository.getReferenceById(userId);
                     TripInvitation invitation = TripInvitation.builder()
                             .id(code)
                             .trip(trip)
                             .createdBy(creator)
                             .expiresAt(LocalDateTime.now().plusDays(7))
                             .isActive(true)
                             .build();
                     invitationRepository.save(invitation);
                     return code;
                });
    }

    // =========================
    // UPDATE TRIP
    // =========================
    @Override
    @Transactional
    public TripResponse updateTrip(Long tripId, Long userId, UpdateTripRequest request) {
        
        validateOwner(tripId, userId);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException("Trip not found"));

        if (Boolean.TRUE.equals(trip.getIsDeleted())) {
            throw new BusinessException("Trip has been deleted");
        }

        trip.setName(request.getName());
        trip.setDescription(request.getDescription());

        tripRepository.save(trip);

        return buildTripResponse(trip);
    }

    // =========================
    // DELETE TRIP (Soft Delete)
    // =========================
    @Override
    @Transactional
    public void deleteTrip(Long tripId, Long userId) {

        validateOwner(tripId, userId);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException("Trip not found"));

        if (Boolean.TRUE.equals(trip.getIsDeleted())) {
            return;
        }

        trip.setIsDeleted(true);
        tripRepository.save(trip);
    }

    @Override
    @Transactional
    public void leaveTrip(Long tripId, Long userId) {
        
        TripMember member = tripMemberRepository.findByTripIdAndUserId(tripId, userId)
                .orElseThrow(() -> new BusinessException("Bạn không thuộc chuyến đi này"));

        if (!member.getIsActive()) {
            return; // Đã rời rồi
        }

        // 1. Kiểm tra nếu là Owner
        if ("OWNER".equals(member.getRole())) {
            long activeCount = tripMemberRepository.findByTripIdAndIsActiveTrue(tripId).size();
            if (activeCount > 1) {
                throw new BusinessException("Chủ nhà không thể rời nhóm khi vẫn còn thành viên khác. Hãy chuyển quyền (Transfer Owner) cho người khác trước.");
            }
        }

        // 2. Kiểm tra nợ nần khi TỰ RỜI NHÓM (Vẫn nên tất toán để minh bạch)
        var settlements = settlementService.calculateSettlement(tripId, userId);
        boolean hasDebt = settlements.stream().anyMatch(s -> 
            s.getFromUserId().equals(userId) || s.getToUserId().equals(userId)
        );

        if (hasDebt) {
            throw new BusinessException("Bạn vẫn còn khoản nợ chưa thanh toán. Vui lòng tất toán trước khi tự rời nhóm. Nếu bị đuổi, nợ sẽ xử lý theo yêu cầu của Chủ phòng.");
        }

        // 3. Rời nhóm (Soft delete member)
        member.setIsActive(false);
        member.setStatus(com.kaitohuy.chiabill.entity.MemberStatus.DISABLED);
        tripMemberRepository.save(member);
    }

    @Override
    @Transactional
    public void transferOwner(Long tripId, Long ownerId, Long newOwnerId) {
        validateOwner(tripId, ownerId);

        TripMember currentOwner = tripMemberRepository.findByTripIdAndUserId(tripId, ownerId)
                .orElseThrow(() -> new BusinessException("Owner not found"));
        TripMember targetMember = tripMemberRepository.findByTripIdAndUserId(tripId, newOwnerId)
                .orElseThrow(() -> new BusinessException("Người nhận quyền không thuộc chuyến đi này"));

        if (!targetMember.getIsActive()) {
            throw new BusinessException("Không thể chuyển quyền cho người đã rời nhóm");
        }

        currentOwner.setRole("MEMBER");
        targetMember.setRole("OWNER");

        tripMemberRepository.save(currentOwner);
        tripMemberRepository.save(targetMember);
    }

    @Override
    @Transactional
    public void disableMember(Long tripId, Long moderatorId, Long targetUserId) {
        validateOwner(tripId, moderatorId);

        TripMember member = tripMemberRepository.findByTripIdAndUserId(tripId, targetUserId)
                .orElseThrow(() -> new BusinessException("Thành viên không tồn tại"));

        member.setStatus(com.kaitohuy.chiabill.entity.MemberStatus.DISABLED);
        tripMemberRepository.save(member);
    }

    @Override
    @Transactional
    public void activateMember(Long tripId, Long ownerId, Long targetUserId) {
        validateOwner(tripId, ownerId);

        TripMember member = tripMemberRepository.findByTripIdAndUserId(tripId, targetUserId)
                .orElseThrow(() -> new BusinessException("Thành viên không tồn tại"));

        member.setIsActive(true);
        member.setStatus(com.kaitohuy.chiabill.entity.MemberStatus.ACTIVE);
        tripMemberRepository.save(member);
    }

    @Override
    @Transactional
    public void kickMember(Long tripId, Long ownerId, Long targetUserId, boolean forgiveDebt) {
        validateOwner(tripId, ownerId);

        if (ownerId.equals(targetUserId)) {
            throw new BusinessException("Bạn không thể tự đuổi chính mình");
        }

        TripMember member = tripMemberRepository.findByTripIdAndUserId(tripId, targetUserId)
                .orElseThrow(() -> new BusinessException("Thành viên không tồn tại"));

        if (!member.getIsActive()) {
            return;
        }

        if (forgiveDebt) {
            // Xoá nợ bằng cách chia lại cho những người còn lại
            var settlements = settlementService.calculateSettlement(tripId, targetUserId);
            
            // Tìm các thành viên ACTIVE còn lại (Trừ người bị đuổi)
            List<TripMember> activeMembers = tripMemberRepository.findEnabledMembersWithUser(tripId).stream()
                    .filter(m -> !m.getUser().getId().equals(targetUserId))
                    .toList();

            if (!activeMembers.isEmpty()) {
                for (var s : settlements) {
                    if (s.getFromUserId().equals(targetUserId) && s.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                        // Trường hợp 1: Người bị đuổi ĐANG NỢ người khác -> Tạo Expense để nhóm gánh thay
                        User creditor = userRepository.findById(s.getToUserId())
                                .orElseThrow(() -> new BusinessException("Creditor not found"));
                        
                        // Tìm category "Điều chỉnh" hoặc dùng mặc định
                        ExpenseCategory systemCategory = categoryRepository.findAllByTripIdOrSystem(tripId).stream()
                                .filter(c -> "Điều chỉnh".equals(c.getName()) || "Khác".equals(c.getName()))
                                .findFirst()
                                .orElse(null);

                        Expense debtRedistribution = Expense.builder()
                                .trip(member.getTrip())
                                .payer(creditor)
                                .totalAmount(s.getAmount())
                                .description("Gánh nợ thay cho " + member.getUser().getName())
                                .category(systemCategory)
                                .expenseDate(java.time.LocalDateTime.now())
                                .build();
                        
                        expenseRepository.save(debtRedistribution);

                        // Chia đều khoản gánh nợ này cho tất cả thành viên còn lại
                        java.math.BigDecimal splitAmount = s.getAmount().divide(
                                java.math.BigDecimal.valueOf(activeMembers.size()), 2, java.math.RoundingMode.HALF_UP
                        );

                        List<ExpenseSplit> splits = activeMembers.stream()
                                .map(m -> ExpenseSplit.builder()
                                        .expense(debtRedistribution)
                                        .user(m.getUser())
                                        .amount(splitAmount)
                                        .build())
                                .toList();
                        expenseSplitRepository.saveAll(splits);
                    }
                    // Nếu người bị đuổi ĐANG ĐƯỢC NỢ (Balance > 0), ta tạm để yên hoặc người bị đuổi tự donate (Clear credit)
                    // Ở đây tôi coi như họ mất quyền đòi nợ (Tạo payment ảo clear nợ hoặc đơn giản là để họ ra đi)
                }
            }
        }

        member.setIsActive(false);
        member.setStatus(com.kaitohuy.chiabill.entity.MemberStatus.DISABLED);
        tripMemberRepository.save(member);
    }

    // =========================
    // HELPER
    // =========================

    private void validateUserInTrip(Long tripId, Long userId) {
        boolean exists = tripMemberRepository.existsByTripIdAndUserId(tripId, userId);
        if (!exists) {
            throw new BusinessException("User not in trip");
        }
    }

    private TripResponse buildTripResponse(Trip trip) {
        TripResponse res = tripMapper.toResponse(trip);
        res.setCreatedAt(trip.getCreatedAt()); // Đảm bảo lấy từ BaseEntity
        
        List<TripMember> members = tripMemberRepository.findActiveMembersWithUser(trip.getId());

        List<TripMemberResponse> memberResponses = members.stream()
                .map(tm -> {
                    // Xác định Owner Id (người có role OWNER trong nhóm)
                    if ("OWNER".equals(tm.getRole())) {
                        res.setOwnerId(tm.getUser().getId());
                    }
                    return TripMemberResponse.builder()
                            .id(tm.getUser().getId())
                            .name(tm.getUser().getName())
                            .avatarUrl(tm.getUser().getAvatarUrl())
                            .role(tm.getRole())
                            .status(tm.getStatus().name())
                            .build();
                })
                .toList();

        res.setMembers(memberResponses);
        return res;
    }

    private void validateOwner(Long tripId, Long userId) {

        TripMember member = tripMemberRepository
                .findByTripIdAndUserId(tripId, userId)
                .orElseThrow(() -> new BusinessException("User not in trip"));

        if (!member.getIsActive()) {
            throw new BusinessException("User is not active in trip");
        }

        if (!"OWNER".equals(member.getRole())) {
            throw new BusinessException("Permission denied");
        }
    }
}
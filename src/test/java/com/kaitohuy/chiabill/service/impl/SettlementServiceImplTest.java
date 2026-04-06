package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.response.SettlementResponse;
import com.kaitohuy.chiabill.dto.response.SettlementSummaryResponse;
import com.kaitohuy.chiabill.entity.*;
import com.kaitohuy.chiabill.repository.ExpenseRepository;
import com.kaitohuy.chiabill.repository.PaymentRepository;
import com.kaitohuy.chiabill.repository.TripMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceImplTest {

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private TripMemberRepository tripMemberRepository;

    @InjectMocks
    private SettlementServiceImpl settlementService;

    private User hong, hien, huy, hoang;
    private Trip trip;

    @BeforeEach
    void setUp() {
        trip = new Trip();
        trip.setId(1L);

        hong = createUser(1L, "Hồng");
        hien = createUser(2L, "Hiền");
        huy = createUser(3L, "Huy");
        hoang = createUser(4L, "Hoàng");
    }

    private User createUser(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }

    private TripMember createMember(User user) {
        TripMember member = new TripMember();
        member.setUser(user);
        member.setIsActive(true);
        return member;
    }

    private Expense createExpense(User payer, List<ExpenseSplit> splits) {
        Expense expense = new Expense();
        expense.setPayer(payer);
        expense.setSplits(splits);
        return expense;
    }

    private ExpenseSplit createSplit(User user, double amount) {
        ExpenseSplit split = new ExpenseSplit();
        split.setUser(user);
        split.setAmount(BigDecimal.valueOf(amount));
        return split;
    }

    @Test
    void calculateSettlement_ComplexOptimization_Success() {
        // GIVEN: 
        // 1. Huy owes Hiền 100k
        // 2. Hoàng owes Hiền 100k
        // 3. Hồng owes Hiền 100k
        // 4. Hiền owes Hồng 300k
        
        Expense exp1 = createExpense(hien, Arrays.asList(
                createSplit(huy, 100),
                createSplit(hoang, 100),
                createSplit(hong, 100)
        ));

        Expense exp2 = createExpense(hong, Collections.singletonList(
                createSplit(hien, 300)
        ));

        mockData(Arrays.asList(exp1, exp2), Collections.emptyList());

        // WHEN
        List<SettlementResponse> results = settlementService.calculateSettlement(1L, 1L);

        // THEN: 
        // Expected optimized: Huy -> Hong (100), Hoang -> Hong (100)
        assertEquals(2, results.size());
        assertDebt(results, 3L, 1L, 100);
        assertDebt(results, 4L, 1L, 100);
    }

    @Test
    void calculateSettlement_BilateralNetting_Success() {
        // GIVEN: 
        // 1. Hồng pays 200, Hiền owes 100
        // 2. Hiền pays 100, Hồng owes 50
        // Net: Hiền owes Hồng 50
        
        Expense exp1 = createExpense(hong, Collections.singletonList(createSplit(hien, 100)));
        Expense exp2 = createExpense(hien, Collections.singletonList(createSplit(hong, 50)));

        mockData(Arrays.asList(exp1, exp2), Collections.emptyList());

        List<SettlementResponse> results = settlementService.calculateSettlement(1L, 1L);

        assertEquals(1, results.size());
        assertDebt(results, 2L, 1L, 50);
    }

    @Test
    void calculateSettlement_ChainSettlement_Success() {
        // GIVEN: 
        // Huy owes Hoàng 100
        // Hoàng owes Hiền 100
        // Hiền owes Hồng 100
        // Expected: Huy owes Hồng 100.
        
        Expense exp1 = createExpense(hoang, Collections.singletonList(createSplit(huy, 100)));
        Expense exp2 = createExpense(hien, Collections.singletonList(createSplit(hoang, 100)));
        Expense exp3 = createExpense(hong, Collections.singletonList(createSplit(hien, 100)));

        mockData(Arrays.asList(exp1, exp2, exp3), Collections.emptyList());

        List<SettlementResponse> results = settlementService.calculateSettlement(1L, 1L);

        assertEquals(1, results.size());
        assertDebt(results, 3L, 1L, 100);
    }

    @Test
    void calculateSettlement_WithExistingPayments_Success() {
        // GIVEN: 
        // Huy owes Hiền 200
        // Huy already paid Hiền 150 (Approved)
        // Net: Huy owes Hiền 50
        
        Expense exp1 = createExpense(hien, Collections.singletonList(createSplit(huy, 200)));
        
        Payment pay1 = new Payment();
        pay1.setFromUser(huy);
        pay1.setToUser(hien);
        pay1.setAmount(BigDecimal.valueOf(150));
        pay1.setStatus(PaymentStatus.APPROVED);

        mockData(Collections.singletonList(exp1), Collections.singletonList(pay1));

        List<SettlementResponse> results = settlementService.calculateSettlement(1L, 1L);

        assertEquals(1, results.size());
        assertDebt(results, 3L, 2L, 50);
    }

    @Test
    void calculateSettlement_ZeroBalance_NoResult() {
        // GIVEN: A owes B 100, B pays C 100, C pays A 100
        // Everyone's balance is 0.
        
        Expense exp1 = createExpense(hien, Collections.singletonList(createSplit(huy, 100)));
        Expense exp2 = createExpense(hong, Collections.singletonList(createSplit(hien, 100)));
        Expense exp3 = createExpense(huy, Collections.singletonList(createSplit(hong, 100)));

        mockData(Arrays.asList(exp1, exp2, exp3), Collections.emptyList());

        List<SettlementResponse> results = settlementService.calculateSettlement(1L, 1L);

        assertTrue(results.isEmpty(), "Results should be empty when everyone is balanced");
    }

    @Test
    void getSettlementSummary_MultipleTrips_Success() {
        // Trip 1: User 1 (Hồng) owes User 2 (Hiền) 100k
        // Trip 2: User 3 (Huy) owes User 1 (Hồng) 150k
        // Expected Summary for Hồng: Total Owed = 100, Total Receivable = 150
        
        Trip t1 = new Trip(); t1.setId(1L);
        Trip t2 = new Trip(); t2.setId(2L);
        
        TripMember m1_t1 = createMember(hong); m1_t1.setTrip(t1);
        TripMember m1_t2 = createMember(hong); m1_t2.setTrip(t2);
        
        when(tripMemberRepository.findByUserIdAndIsActiveTrue(hong.getId()))
                .thenReturn(Arrays.asList(m1_t1, m1_t2));
        
        // Mock Trip 1
        when(tripMemberRepository.findByTripId(1L)).thenReturn(Arrays.asList(createMember(hong), createMember(hien)));
        Expense exp1 = createExpense(hien, Collections.singletonList(createSplit(hong, 100)));
        when(expenseRepository.fetchAllDataForSettlement(1L)).thenReturn(Collections.singletonList(exp1));
        when(paymentRepository.findByTripIdAndStatus(1L, PaymentStatus.APPROVED)).thenReturn(Collections.emptyList());

        // Mock Trip 2
        when(tripMemberRepository.findByTripId(2L)).thenReturn(Arrays.asList(createMember(hong), createMember(huy)));
        Expense exp2 = createExpense(hong, Collections.singletonList(createSplit(huy, 150)));
        when(expenseRepository.fetchAllDataForSettlement(2L)).thenReturn(Collections.singletonList(exp2));
        when(paymentRepository.findByTripIdAndStatus(2L, PaymentStatus.APPROVED)).thenReturn(Collections.emptyList());

        SettlementSummaryResponse summary = settlementService.getSettlementSummary(hong.getId());

        assertNotNull(summary);
        assertEquals(0, summary.getTotalOwed().compareTo(BigDecimal.valueOf(100)));
        assertEquals(0, summary.getTotalReceivable().compareTo(BigDecimal.valueOf(150)));
    }

    private void mockData(List<Expense> expenses, List<Payment> payments) {
        when(tripMemberRepository.existsByTripIdAndUserId(anyLong(), anyLong())).thenReturn(true);
        when(tripMemberRepository.findByTripId(anyLong())).thenReturn(Arrays.asList(
                createMember(hong), createMember(hien), createMember(huy), createMember(hoang)
        ));
        when(expenseRepository.fetchAllDataForSettlement(anyLong())).thenReturn(expenses);
        when(paymentRepository.findByTripIdAndStatus(anyLong(), any())).thenReturn(payments);
    }

    private void assertDebt(List<SettlementResponse> results, Long from, Long to, double amount) {
        boolean found = results.stream().anyMatch(r -> 
            r.getFromUserId().equals(from) && 
            r.getToUserId().equals(to) && 
            r.getAmount().setScale(2, RoundingMode.HALF_UP).compareTo(BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP)) == 0);
        assertTrue(found, "Expected debt from " + from + " to " + to + " of amount " + amount + " not found");
    }
}

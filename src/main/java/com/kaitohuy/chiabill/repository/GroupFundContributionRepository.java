package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.ContributionType;
import com.kaitohuy.chiabill.entity.GroupFundContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupFundContributionRepository extends JpaRepository<GroupFundContribution, Long> {
    List<GroupFundContribution> findByGroupFundIdAndIsDeletedFalseOrderByContributionDateDesc(Long fundId);
    List<GroupFundContribution> findByGroupFundIdAndTypeAndIsDeletedFalse(Long fundId, ContributionType type);
    List<GroupFundContribution> findByGroupFundIdAndIsDeletedFalse(Long fundId);
    List<GroupFundContribution> findByLinkedExpenseIdAndIsDeletedFalse(Long expenseId);
}

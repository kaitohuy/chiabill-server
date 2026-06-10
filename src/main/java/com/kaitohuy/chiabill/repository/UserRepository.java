package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE " +
           "(:email IS NOT NULL AND u.email = :email) OR " +
           "(:phone IS NOT NULL AND u.phone = :phone)")
    Optional<User> findByEmailOrPhone(@org.springframework.data.repository.query.Param("email") String email, 
                                      @org.springframework.data.repository.query.Param("phone") String phone);

    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    Optional<User> findByIdAndIsGhostTrue(Long id);

    long countByIsDeletedFalse();
    
    long countByIsGhostTrueAndIsDeletedFalse();
    
    long countByIsAnonymousTrueAndIsDeletedFalse();
    
    long countByIsAnonymousFalseAndIsGhostFalseAndIsDeletedFalse();

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.isDeleted = false AND " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    org.springframework.data.domain.Page<User> searchUsers(@org.springframework.data.repository.query.Param("keyword") String keyword, org.springframework.data.domain.Pageable pageable);
}
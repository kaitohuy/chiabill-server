package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.PlaceCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlaceCommentLikeRepository extends JpaRepository<PlaceCommentLike, Long> {
    boolean existsByUserIdAndCommentId(Long userId, Long commentId);
    Optional<PlaceCommentLike> findByUserIdAndCommentId(Long userId, Long commentId);
}

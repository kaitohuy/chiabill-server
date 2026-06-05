package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.PlaceComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceCommentRepository extends JpaRepository<PlaceComment, Long> {
    Page<PlaceComment> findAllByPlaceIdAndIsDeletedFalseAndParentIdIsNull(Long placeId, Pageable pageable);
    
    List<PlaceComment> findAllByParentIdAndIsDeletedFalse(Long parentId);
    
    java.util.Optional<PlaceComment> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    Page<PlaceComment> findAllByIsDeletedFalse(Pageable pageable);
}

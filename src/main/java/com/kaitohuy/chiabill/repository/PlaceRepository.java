package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.Place;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long>, JpaSpecificationExecutor<Place> {

    Optional<Place> findByIdAndIsDeletedFalse(Long id);

    Page<Place> findAllByIsDeletedFalse(Pageable pageable);

    Page<Place> findAllByIsDeletedFalseAndCategory(String category, Pageable pageable);

    Page<Place> findAllByIsDeletedFalseAndNameContainingIgnoreCase(String keyword, Pageable pageable);

    @Query(value = "SELECT * FROM (" +
            "SELECT p.*, " +
            "(6371 * acos(cos(radians(:latitude)) * cos(radians(p.latitude)) * " +
            "cos(radians(p.longitude) - radians(:longitude)) + " +
            "sin(radians(:latitude)) * sin(radians(p.latitude)))) AS distance " +
            "FROM places p WHERE p.is_deleted = false) AS sub " +
            "WHERE sub.distance < :radius " +
            "ORDER BY sub.distance ASC " +
            "LIMIT :limit", nativeQuery = true)
    List<Place> findPlacesNearby(
            @Param("latitude") double latitude, 
            @Param("longitude") double longitude, 
            @Param("radius") double radiusInKm, 
            @Param("limit") int limit);
}

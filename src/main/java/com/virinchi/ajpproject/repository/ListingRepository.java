package com.virinchi.ajpproject.repository;

import com.virinchi.ajpproject.model.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {
    @Query("SELECT l FROM Listing l WHERE " +
            "LOWER(l.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(l.category) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(l.breed) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(l.description) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(l.location) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Listing> searchByKeyword(@Param("q") String keyword);
}



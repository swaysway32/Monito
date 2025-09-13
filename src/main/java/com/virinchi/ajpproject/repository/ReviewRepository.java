package com.virinchi.ajpproject.repository;

import com.virinchi.ajpproject.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Find all reviews for a specific listing, ordered by creation date (newest first)
    List<Review> findByListingIdOrderByCreatedAtDesc(Long listingId);

    // Find all reviews for a specific listing, ordered by creation date (oldest first)
    List<Review> findByListingIdOrderByCreatedAtAsc(Long listingId);

    // Check if a user has already reviewed a specific listing
    Optional<Review> findByListingIdAndUserEmail(Long listingId, String userEmail);

    // Count total reviews for a listing
    long countByListingId(Long listingId);

    // Calculate average rating for a listing
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.listingId = :listingId")
    Double findAverageRatingByListingId(@Param("listingId") Long listingId);

    // Find reviews by user email
    List<Review> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    // Find reviews with rating greater than or equal to specified value
    List<Review> findByListingIdAndRatingGreaterThanEqualOrderByCreatedAtDesc(Long listingId, Integer rating);

    // Find reviews with rating less than or equal to specified value
    List<Review> findByListingIdAndRatingLessThanEqualOrderByCreatedAtDesc(Long listingId, Integer rating);
}

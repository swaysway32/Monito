package com.virinchi.ajpproject.repository;

import com.virinchi.ajpproject.model.Listing;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingRepository extends JpaRepository<Listing, Long> {
}



package com.virinchi.ajpproject.repository;

import com.virinchi.ajpproject.model.SellerApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SellerApplicationRepository extends JpaRepository<SellerApplication, Long> {
    
    List<SellerApplication> findByStatusOrderByApplicationDateDesc(String status);
    
    List<SellerApplication> findAllByOrderByApplicationDateDesc();
    
    SellerApplication findByEmail(String email);
}

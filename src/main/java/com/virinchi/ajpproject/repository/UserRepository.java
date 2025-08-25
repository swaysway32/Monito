package com.virinchi.ajpproject.repository;

import com.virinchi.ajpproject.model.user;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<user, Long> {

    // Find user by email for login
    user findByEmail(String email);

    // Check if email already exists during signup
    boolean existsByEmail(String email);

    // Find user by email and password for login validation
    user findByEmailAndPassword(String email, String password);
}
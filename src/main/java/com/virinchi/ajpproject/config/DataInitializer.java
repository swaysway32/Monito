package com.virinchi.ajpproject.config;

import com.virinchi.ajpproject.model.Admin;
import com.virinchi.ajpproject.repository.AdminRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AdminRepository adminRepository;

    @Override
    public void run(String... args) {
        // Create default admin if none exists
        if (adminRepository.count() == 0) {
            String defaultUsername = "admin@monito.com";
            String defaultPassword = "admin123";
            String hashedPassword = DigestUtils.sha256Hex(defaultPassword);
            
            Admin admin = new Admin(defaultUsername, hashedPassword);
            adminRepository.save(admin);
            
            System.out.println("Default admin account created:");
            System.out.println("Username: " + defaultUsername);
            System.out.println("Password: " + defaultPassword);
        }
    }
}
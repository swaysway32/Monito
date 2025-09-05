package com.virinchi.ajpproject.controller;

import com.virinchi.ajpproject.model.SellerApplication;
import com.virinchi.ajpproject.model.user;
import com.virinchi.ajpproject.repository.SellerApplicationRepository;
import com.virinchi.ajpproject.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SellerController {

    @Autowired
    private SellerApplicationRepository sellerApplicationRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/become-seller")
    public String showBecomeSellerPage(Model model, HttpSession session) {
        // Check if user is logged in and get seller status
        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        Boolean isSeller = (Boolean) session.getAttribute("isSeller");
        
        if (isLoggedIn != null && isLoggedIn) {
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("isSeller", isSeller != null && isSeller);
        } else {
            model.addAttribute("isLoggedIn", false);
            model.addAttribute("isSeller", false);
        }
        
        return "become-seller";
    }

    @PostMapping("/submit-seller-application")
    public String submitSellerApplication(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String businessName,
            @RequestParam String businessType,
            @RequestParam String businessDescription,
            @RequestParam String experience,
            @RequestParam String location,
            @RequestParam String reasonToSell,
            RedirectAttributes redirectAttributes) {

        try {
            // Check if user already has a pending or approved application
            SellerApplication existingApplication = sellerApplicationRepository.findByEmail(email);
            if (existingApplication != null) {
                if ("pending".equals(existingApplication.getStatus())) {
                    redirectAttributes.addFlashAttribute("error", "You already have a pending seller application. Please wait for review.");
                    return "redirect:/become-seller";
                } else if ("approved".equals(existingApplication.getStatus())) {
                    redirectAttributes.addFlashAttribute("error", "You are already an approved seller.");
                    return "redirect:/become-seller";
                }
            }

            // Create new seller application
            SellerApplication application = new SellerApplication(
                    firstName, lastName, email, phone, businessName, businessType,
                    businessDescription, experience, location, reasonToSell
            );

            sellerApplicationRepository.save(application);

            // Update user's seller status to pending if they exist
            user existingUser = userRepository.findByEmail(email);
            if (existingUser != null) {
                existingUser.setSellerStatus("pending");
                userRepository.save(existingUser);
            }

            redirectAttributes.addFlashAttribute("success", 
                "Your seller application has been submitted successfully! Our admin team will review it within 1-3 business days. You will receive an email notification about the decision.");

            return "redirect:/become-seller";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "There was an error submitting your application. Please try again.");
            return "redirect:/become-seller";
        }
    }
}

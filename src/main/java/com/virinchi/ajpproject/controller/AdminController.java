package com.virinchi.ajpproject.controller;

import com.virinchi.ajpproject.model.SellerApplication;
import com.virinchi.ajpproject.model.user;
import com.virinchi.ajpproject.repository.ListingRepository;
import com.virinchi.ajpproject.repository.SellerApplicationRepository;
import com.virinchi.ajpproject.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class AdminController {

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private SellerApplicationRepository sellerApplicationRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/admin")
    public String showAdminDashboard(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");

        // Check if user is logged in and is admin
        if (isLoggedIn == null || !isLoggedIn || isAdmin == null || !isAdmin) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in as an administrator to access this page.");
            return "redirect:/login";
        }

        model.addAttribute("isLoggedIn", true);
        model.addAttribute("isAdmin", true);

        long totalListings = listingRepository.count();
        model.addAttribute("totalListings", totalListings);

        model.addAttribute("listings", listingRepository.findAll());

        // Add seller applications data
        List<SellerApplication> pendingApplications = sellerApplicationRepository.findByStatusOrderByApplicationDateDesc("pending");
        List<SellerApplication> allApplications = sellerApplicationRepository.findAllByOrderByApplicationDateDesc();
        
        model.addAttribute("pendingApplications", pendingApplications);
        model.addAttribute("allApplications", allApplications);
        model.addAttribute("totalApplications", allApplications.size());
        model.addAttribute("pendingCount", pendingApplications.size());

        return "admin";
    }

    @PostMapping("/admin/listings/delete-all")
    public String deleteAllListings(RedirectAttributes redirectAttributes) {
        listingRepository.deleteAll();
        redirectAttributes.addFlashAttribute("message", "All products/listings have been deleted successfully.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/listings/{id}/delete")
    public String deleteListingById(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        if (listingRepository.existsById(id)) {
            listingRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "Product has been deleted.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Product not found or already deleted.");
        }
        return "redirect:/admin";
    }

    // Seller Application Management
    @PostMapping("/admin/seller-applications/{id}/approve")
    public String approveSellerApplication(@PathVariable("id") Long id, 
                                         @RequestParam(required = false) String adminNotes,
                                         RedirectAttributes redirectAttributes) {
        try {
            SellerApplication application = sellerApplicationRepository.findById(id).orElse(null);
            if (application == null) {
                redirectAttributes.addFlashAttribute("error", "Seller application not found.");
                return "redirect:/admin";
            }

            // Update application status
            application.setStatus("approved");
            application.setAdminNotes(adminNotes);
            application.setReviewDate(LocalDateTime.now());
            sellerApplicationRepository.save(application);

            // Update user's seller status if they exist
            user existingUser = userRepository.findByEmail(application.getEmail());
            if (existingUser != null) {
                existingUser.setSellerStatus("approved");
                userRepository.save(existingUser);
            }

            redirectAttributes.addFlashAttribute("message", 
                "Seller application for " + application.getFirstName() + " " + application.getLastName() + " has been approved.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error approving seller application.");
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/seller-applications/{id}/reject")
    public String rejectSellerApplication(@PathVariable("id") Long id, 
                                        @RequestParam(required = false) String adminNotes,
                                        RedirectAttributes redirectAttributes) {
        try {
            SellerApplication application = sellerApplicationRepository.findById(id).orElse(null);
            if (application == null) {
                redirectAttributes.addFlashAttribute("error", "Seller application not found.");
                return "redirect:/admin";
            }

            // Update application status
            application.setStatus("rejected");
            application.setAdminNotes(adminNotes);
            application.setReviewDate(LocalDateTime.now());
            sellerApplicationRepository.save(application);

            // Update user's seller status if they exist
            user existingUser = userRepository.findByEmail(application.getEmail());
            if (existingUser != null) {
                existingUser.setSellerStatus("rejected");
                userRepository.save(existingUser);
            }

            redirectAttributes.addFlashAttribute("message", 
                "Seller application for " + application.getFirstName() + " " + application.getLastName() + " has been rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error rejecting seller application.");
        }
        return "redirect:/admin";
    }
}



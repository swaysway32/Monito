package com.virinchi.ajpproject.controller;

import com.virinchi.ajpproject.model.user;
import com.virinchi.ajpproject.model.Admin;
import com.virinchi.ajpproject.repository.UserRepository;
import com.virinchi.ajpproject.repository.AdminRepository;
import org.apache.commons.codec.digest.DigestUtils;
import com.virinchi.ajpproject.repository.ListingRepository;
import com.virinchi.ajpproject.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired(required = false)
    private EmailService emailService;

    // Show login page
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    // Show signup page
    @GetMapping("/signup")
    public String showSignupPage() {
        return "signup";
    }

    // Handle login form submission
    @PostMapping("/login")
    public String handleLogin(@RequestParam String email,
                              @RequestParam String password,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {

        // Hash the password to compare with stored hash
        String hashedPassword = DigestUtils.sha256Hex(password);

        // First check if it's an admin login
        Admin admin = adminRepository.findByUsernameAndPassword(email, hashedPassword);
        if (admin != null) {
            // Clear any existing session data first
            session.removeAttribute("isSeller");
            session.removeAttribute("userEmail");
            
            // Store admin info in session
            session.setAttribute("loggedInUser", admin.getUsername());
            session.setAttribute("isLoggedIn", true);
            session.setAttribute("isAdmin", true);
            session.setAttribute("isSeller", false);

            // Admin login successful - redirect to admin page
            redirectAttributes.addFlashAttribute("message", "Welcome, Admin!");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/admin";
        } else {
            // Check if attempting admin login but failed
            if (email.endsWith("@monito.com")) {
                redirectAttributes.addFlashAttribute("error", "Invalid admin credentials!");
                return "redirect:/login";
            }
        }

        // Then check if it's a regular user login
        user user = userRepository.findByEmailAndPassword(email, hashedPassword);
        if (user != null) {
            // Clear any existing session data first
            session.removeAttribute("isAdmin");
            
            // Store user info in session
            session.setAttribute("loggedInUser", user.getFirstName());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("isLoggedIn", true);
            session.setAttribute("isAdmin", false);
            session.setAttribute("isSeller", "approved".equals(user.getSellerStatus()));

            // User login successful - redirect to home page
            redirectAttributes.addFlashAttribute("message", "Welcome back, " + user.getFirstName() + "!");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/";
        } else {
            // Login failed
            redirectAttributes.addFlashAttribute("error", "Invalid email or password!");
            return "redirect:/login";
        }
    }

    // Admin page is handled in AdminController

    // Handle signup form submission
    @PostMapping("/signup")
    public String handleSignup(@RequestParam String firstName,
                               @RequestParam String lastName,
                               @RequestParam String email,
                               @RequestParam String phone,
                               @RequestParam String password,
                               @RequestParam String confirmPassword,
                               @RequestParam String interests,
                               @RequestParam(required = false) boolean newsletter,
                               RedirectAttributes redirectAttributes) {

        // Validation
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match!");
            return "redirect:/signup";
        }

        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            redirectAttributes.addFlashAttribute("error", "Email already exists! Please use a different email or login.");
            return "redirect:/signup";
        }

        // Hash password before saving
        String hashedPassword = DigestUtils.sha256Hex(password);

        // Create new user
        user newUser = new user(firstName, lastName, email, phone, hashedPassword, interests, newsletter);

        try {
            userRepository.save(newUser);
            try {
                if (emailService != null) {
                    emailService.sendWelcomeEmail(email, firstName);
                }
            } catch (Exception ex) {
                log.warn("Welcome email failed for {}: {}", email, ex.getMessage());
            }
            redirectAttributes.addFlashAttribute("message", "Account created successfully! Please log in.");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating account. Please try again.");
            return "redirect:/signup";
        }
    }

    // Dashboard page (restricted to approved sellers only)
    @GetMapping("/dashboard")
    public String showDashboard(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        // Check if user is logged in
        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        
        // Redirect admin users to admin page
        if (isAdmin != null && isAdmin) {
            return "redirect:/admin";
        }
        
        // Check if user is logged in
        if (isLoggedIn == null || !isLoggedIn) {
            redirectAttributes.addFlashAttribute("error", "Please log in to access this page.");
            return "redirect:/login";
        }
        
        // Check if user is an approved seller
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail != null) {
            user currentUser = userRepository.findByEmail(userEmail);
            if (currentUser != null) {
                String sellerStatus = currentUser.getSellerStatus();
                if (!"approved".equals(sellerStatus)) {
                    if ("pending".equals(sellerStatus)) {
                        redirectAttributes.addFlashAttribute("error", "Your seller application is still pending review. Please wait for admin approval.");
                    } else if ("rejected".equals(sellerStatus)) {
                        redirectAttributes.addFlashAttribute("error", "Your seller application was rejected. Please contact support for more information.");
                    } else {
                        redirectAttributes.addFlashAttribute("error", "You need to be an approved seller to access the dashboard. Please apply to become a seller first.");
                    }
                    return "redirect:/become-seller";
                }
                
                // User is approved seller - show dashboard
                model.addAttribute("sellerStatus", sellerStatus);
                model.addAttribute("isApprovedSeller", true);
                model.addAttribute("isSeller", true);
            } else {
                redirectAttributes.addFlashAttribute("error", "User not found. Please log in again.");
                return "redirect:/login";
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Please log in to access this page.");
            return "redirect:/login";
        }
        
        return "dashboard";
    }

    // Home page (index)
    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        // Check if user is logged in
        String userName = (String) session.getAttribute("loggedInUser");
        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        Boolean isSeller = (Boolean) session.getAttribute("isSeller");

        if (isLoggedIn != null && isLoggedIn) {
            model.addAttribute("userName", userName);
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("isAdmin", isAdmin != null && isAdmin);
            model.addAttribute("isSeller", isSeller != null && isSeller);
        } else {
            model.addAttribute("isLoggedIn", false);
            model.addAttribute("isAdmin", false);
            model.addAttribute("isSeller", false);
        }

        model.addAttribute("listings", listingRepository.findAll());
        return "index";
    }

    // Logout
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        // Clear all session attributes explicitly
        session.removeAttribute("loggedInUser");
        session.removeAttribute("isLoggedIn");
        session.removeAttribute("isAdmin");
        session.removeAttribute("isSeller");
        session.removeAttribute("userEmail");
        
        // Invalidate the entire session
        session.invalidate();
        
        redirectAttributes.addFlashAttribute("message", "You have been logged out successfully!");
        return "redirect:/login";
    }
}
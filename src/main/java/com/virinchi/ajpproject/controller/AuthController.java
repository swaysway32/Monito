package com.virinchi.ajpproject.controller;

import com.virinchi.ajpproject.model.user;
import com.virinchi.ajpproject.model.Admin;
import com.virinchi.ajpproject.repository.UserRepository;
import com.virinchi.ajpproject.repository.AdminRepository;
import org.apache.commons.codec.digest.DigestUtils;
import com.virinchi.ajpproject.repository.ListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private AdminRepository adminRepository;

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
            // Store admin info in session
            session.setAttribute("loggedInUser", admin.getUsername());
            session.setAttribute("isLoggedIn", true);
            session.setAttribute("isAdmin", true);

            // Admin login successful - redirect to admin page
            redirectAttributes.addFlashAttribute("message", "Welcome, Admin!");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/admin";
        }

        // Then check if it's a regular user login
        user user = userRepository.findByEmailAndPassword(email, hashedPassword);
        if (user != null) {
            // Store user info in session
            session.setAttribute("loggedInUser", user.getFirstName());
            session.setAttribute("isLoggedIn", true);

            // User login successful - redirect to admin page
            redirectAttributes.addFlashAttribute("message", "Welcome back, " + user.getFirstName() + "!");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/admin";
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
            redirectAttributes.addFlashAttribute("message", "Account created successfully! Please log in.");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating account. Please try again.");
            return "redirect:/signup";
        }
    }

    // Dashboard page (after login)
    @GetMapping("/dashboard")
    public String showDashboard() {
        return "dashboard";
    }

    // Home page (index)
    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        // Check if user is logged in
        String userName = (String) session.getAttribute("loggedInUser");
        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");

        if (isLoggedIn != null && isLoggedIn) {
            model.addAttribute("userName", userName);
            model.addAttribute("isLoggedIn", true);
        } else {
            model.addAttribute("isLoggedIn", false);
        }

        model.addAttribute("listings", listingRepository.findAll());
        return "index";
    }

    // Logout
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("message", "You have been logged out successfully!");
        return "redirect:/login";
    }
}
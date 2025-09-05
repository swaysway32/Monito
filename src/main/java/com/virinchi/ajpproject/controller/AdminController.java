package com.virinchi.ajpproject.controller;

import com.virinchi.ajpproject.repository.ListingRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {

    @Autowired
    private ListingRepository listingRepository;

    @GetMapping("/admin")
    public String showAdminDashboard(Model model, HttpSession session) {
        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");

        model.addAttribute("isLoggedIn", isLoggedIn != null && isLoggedIn);
        model.addAttribute("isAdmin", isAdmin != null && isAdmin);

        long totalListings = listingRepository.count();
        model.addAttribute("totalListings", totalListings);

        model.addAttribute("listings", listingRepository.findAll());

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
}



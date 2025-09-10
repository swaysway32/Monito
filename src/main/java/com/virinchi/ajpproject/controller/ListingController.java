package com.virinchi.ajpproject.controller;

import com.virinchi.ajpproject.model.Listing;
import com.virinchi.ajpproject.model.user;
import com.virinchi.ajpproject.repository.ListingRepository;
import com.virinchi.ajpproject.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class ListingController {

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads";

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/seller-upload")
    public String showUploadPage(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        // Check if user is logged in
        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        if (isLoggedIn == null || !isLoggedIn) {
            redirectAttributes.addFlashAttribute("error", "Please log in to access seller features.");
            return "redirect:/login";
        }
        // Provide safe defaults for header flags
        model.addAttribute("isLoggedIn", true);
        model.addAttribute("isSeller", false);

        // Check if user is an approved seller
        String userEmail = (String) session.getAttribute("loggedInUser");
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
                        redirectAttributes.addFlashAttribute("error", "You need to apply to become a seller first. Please submit a seller application.");
                    }
                    return "redirect:/become-seller";
                }
                // User is approved seller
                model.addAttribute("isSeller", true);
            }
        }

        // Ensure listing backing object
        model.addAttribute("listing", new Listing());
        return "seller-upload";
    }

    @PostMapping("/upload-listing")
    public String handleUpload(
            @RequestParam("listingType") String listingType,
            @RequestParam("name") String name,
            @RequestParam("category") String category,
            @RequestParam(value = "breed", required = false) String breed,
            @RequestParam(value = "age", required = false) String age,
            @RequestParam("price") Double price,
            @RequestParam("description") String description,
            @RequestParam("location") String location,
            @RequestParam("contact") String contact,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        // Check if user is logged in and is an approved seller
        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        if (isLoggedIn == null || !isLoggedIn) {
            redirectAttributes.addFlashAttribute("error", "Please log in to upload listings.");
            return "redirect:/login";
        }

        String userEmail = (String) session.getAttribute("loggedInUser");
        if (userEmail != null) {
            user currentUser = userRepository.findByEmail(userEmail);
            if (currentUser != null && !"approved".equals(currentUser.getSellerStatus())) {
                redirectAttributes.addFlashAttribute("error", "You are not authorized to upload listings. Please apply to become a seller first.");
                return "redirect:/become-seller";
            }
        }
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            List<String> storedPaths = new ArrayList<>();
            if (images != null) {
                for (MultipartFile image : images) {
                    if (image != null && !image.isEmpty()) {
                        String original = StringUtils.cleanPath(image.getOriginalFilename());
                        String ext = "";
                        int dot = original.lastIndexOf('.');
                        if (dot >= 0) ext = original.substring(dot);
                        String filename = UUID.randomUUID() + "-" + Instant.now().toEpochMilli() + ext;
                        Path target = uploadPath.resolve(filename);
                        Files.copy(image.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                        storedPaths.add("/uploads/" + filename);
                    }
                }
            }

            Listing listing = new Listing();
            listing.setListingType(listingType);
            listing.setName(name);
            listing.setCategory(category);
            listing.setBreed(breed);
            listing.setAge(age);
            listing.setPrice(price);
            listing.setDescription(description);
            listing.setLocation(location);
            listing.setContact(contact);
            listing.setImagePaths(String.join(",", storedPaths));
            listing.setOwnerEmail(userEmail);

            listingRepository.save(listing);

            redirectAttributes.addFlashAttribute("success", "Listing uploaded successfully!");
            return "redirect:/seller-upload";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Upload failed. Please try again.");
            return "redirect:/seller-upload";
        }
    }

    @GetMapping("/api/search")
    @ResponseBody
    public List<Listing> searchListings(@RequestParam("q") String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return listingRepository.searchByKeyword(query.trim());
    }

    @GetMapping("/seller/manage")
    public String manageListings(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        if (isLoggedIn == null || !isLoggedIn) {
            redirectAttributes.addFlashAttribute("error", "Please log in to manage listings.");
            return "redirect:/login";
        }
        String userEmail = (String) session.getAttribute("loggedInUser");
        if (userEmail == null) {
            return "redirect:/login";
        }
        List<Listing> myListings = listingRepository.findByOwnerEmailOrderByIdDesc(userEmail);
        model.addAttribute("isLoggedIn", true);
        model.addAttribute("isSeller", true);
        model.addAttribute("listings", myListings);
        return "seller-manage";
    }

    @PostMapping("/seller/manage/update")
    public String updateListing(@RequestParam Long id,
                                @RequestParam String name,
                                @RequestParam String description,
                                @RequestParam Double price,
                                @RequestParam String location,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        String userEmail = (String) session.getAttribute("loggedInUser");
        if (userEmail == null) return "redirect:/login";
        Listing listing = listingRepository.findById(id).orElse(null);
        if (listing == null || !userEmail.equals(listing.getOwnerEmail())) {
            redirectAttributes.addFlashAttribute("error", "You can only edit your own listings.");
            return "redirect:/seller/manage";
        }
        listing.setName(name);
        listing.setDescription(description);
        listing.setPrice(price);
        listing.setLocation(location);
        listingRepository.save(listing);
        redirectAttributes.addFlashAttribute("success", "Listing updated.");
        return "redirect:/seller/manage";
    }

    @PostMapping("/seller/manage/delete")
    public String deleteListing(@RequestParam Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        String userEmail = (String) session.getAttribute("loggedInUser");
        if (userEmail == null) return "redirect:/login";
        Listing listing = listingRepository.findById(id).orElse(null);
        if (listing == null || !userEmail.equals(listing.getOwnerEmail())) {
            redirectAttributes.addFlashAttribute("error", "You can only delete your own listings.");
            return "redirect:/seller/manage";
        }
        listingRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Listing deleted.");
        return "redirect:/seller/manage";
    }

    @GetMapping("/listing/{id:\\d+}{slug:(?:-[a-z0-9-]+)?}")
    public String viewListing(@org.springframework.web.bind.annotation.PathVariable Long id,
                              @org.springframework.web.bind.annotation.PathVariable(required = false) String slug,
                              Model model) {
        Listing listing = listingRepository.findById(id).orElse(null);
        if (listing == null) {
            return "not-found";
        }
        model.addAttribute("listing", listing);
        return "listing-detail";
    }

    @GetMapping("/api/listing/{id}")
    @ResponseBody
    public Listing getListing(@org.springframework.web.bind.annotation.PathVariable Long id) {
        return listingRepository.findById(id).orElse(null);
    }
}



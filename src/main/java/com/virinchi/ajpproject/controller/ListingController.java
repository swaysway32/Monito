package com.virinchi.ajpproject.controller;

import com.virinchi.ajpproject.model.Listing;
import com.virinchi.ajpproject.model.Review;
import com.virinchi.ajpproject.model.user;
import com.virinchi.ajpproject.repository.ListingRepository;
import com.virinchi.ajpproject.repository.ReviewRepository;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @Autowired
    private ReviewRepository reviewRepository;

    @GetMapping("/browse-pets")
    public String browsePets(Model model, HttpSession session) {
        List<Listing> petListings = listingRepository.findByListingType("pet");
        model.addAttribute("listings", petListings);
        setupCommonModelAttributes(model, session);
        return "index";
    }

    @GetMapping("/browse-accessories")
    public String browseAccessories(Model model, HttpSession session) {
        List<Listing> accessoryListings = listingRepository.findByListingType("accessory");
        model.addAttribute("listings", accessoryListings);
        setupCommonModelAttributes(model, session);
        return "index";
    }

    private void setupCommonModelAttributes(Model model, HttpSession session) {
        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        Boolean isSeller = (Boolean) session.getAttribute("isSeller");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        String userName = (String) session.getAttribute("userName");

        model.addAttribute("isLoggedIn", isLoggedIn != null && isLoggedIn);
        model.addAttribute("isSeller", isSeller != null && isSeller);
        model.addAttribute("isAdmin", isAdmin != null && isAdmin);
        model.addAttribute("userName", userName);
    }

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
                              Model model, HttpSession session) {
        Listing listing = listingRepository.findById(id).orElse(null);
        if (listing == null) {
            return "not-found";
        }
        
        // Get reviews for this listing
        List<Review> reviews = new ArrayList<>();
        try {
            reviews = reviewRepository.findByListingIdOrderByCreatedAtDesc(id);
        } catch (Exception e) {
            // If reviews table doesn't exist yet, just use empty list
            reviews = new ArrayList<>();
        }
        
        // Calculate average rating
        Double averageRating = 0.0;
        try {
            averageRating = reviewRepository.findAverageRatingByListingId(id);
            if (averageRating == null) {
                averageRating = 0.0;
            }
        } catch (Exception e) {
            // If there's an issue with the query, default to 0.0
            averageRating = 0.0;
        }
        
        // Check if current user has already reviewed this listing
        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        Review userReview = null;
        if (isLoggedIn != null && isLoggedIn) {
            String userEmail = (String) session.getAttribute("userEmail");
            if (userEmail != null) {
                userReview = reviewRepository.findByListingIdAndUserEmail(id, userEmail).orElse(null);
            }
        }
        
        model.addAttribute("listing", listing);
        model.addAttribute("reviews", reviews);
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("totalReviews", reviews.size());
        model.addAttribute("isLoggedIn", isLoggedIn != null && isLoggedIn);
        model.addAttribute("userReview", userReview);
        
        return "listing-detail";
    }

    @GetMapping("/api/listing/{id}")
    @ResponseBody
    public Listing getListing(@org.springframework.web.bind.annotation.PathVariable Long id) {
        return listingRepository.findById(id).orElse(null);
    }

    @PostMapping("/listing/{id}/review")
    public String submitReview(@org.springframework.web.bind.annotation.PathVariable Long id,
                               @RequestParam Integer rating,
                               @RequestParam(required = false) String comment,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        
        // Check if user is logged in
        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        if (isLoggedIn == null || !isLoggedIn) {
            redirectAttributes.addFlashAttribute("error", "Please log in to leave a review.");
            return "redirect:/login";
        }
        
        // Validate rating
        if (rating == null || rating < 1 || rating > 5) {
            redirectAttributes.addFlashAttribute("error", "Please provide a valid rating between 1 and 5 stars.");
            return "redirect:/listing/" + id;
        }
        
        // Check if listing exists
        Listing listing = listingRepository.findById(id).orElse(null);
        if (listing == null) {
            redirectAttributes.addFlashAttribute("error", "Listing not found.");
            return "redirect:/";
        }
        
        String userEmail = (String) session.getAttribute("userEmail");
        String userName = (String) session.getAttribute("loggedInUser");
        
        if (userEmail == null || userName == null) {
            redirectAttributes.addFlashAttribute("error", "User information not found. Please log in again.");
            return "redirect:/login";
        }
        
        // Check if user has already reviewed this listing
        Review existingReview = reviewRepository.findByListingIdAndUserEmail(id, userEmail).orElse(null);
        if (existingReview != null) {
            // Update existing review
            existingReview.setRating(rating);
            existingReview.setComment(comment != null ? comment.trim() : "");
            existingReview.setCreatedAt(java.time.LocalDateTime.now());
            reviewRepository.save(existingReview);
            redirectAttributes.addFlashAttribute("success", "Your review has been updated successfully!");
        } else {
            // Create new review
            Review review = new Review(id, userEmail, userName, rating, comment != null ? comment.trim() : "");
            reviewRepository.save(review);
            redirectAttributes.addFlashAttribute("success", "Thank you for your review!");
        }
        
        return "redirect:/listing/" + id;
    }

    @GetMapping("/download-image/{listingId}/{imageIndex}")
    public ResponseEntity<byte[]> downloadImage(@PathVariable Long listingId, 
                                               @PathVariable int imageIndex) {
        try {
            // Get the listing
            Listing listing = listingRepository.findById(listingId).orElse(null);
            if (listing == null || listing.getImagePaths() == null || listing.getImagePaths().isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Split image paths and get the requested image
            String[] imagePaths = listing.getImagePaths().split(",");
            if (imageIndex < 0 || imageIndex >= imagePaths.length) {
                return ResponseEntity.notFound().build();
            }

            String imagePath = imagePaths[imageIndex].trim();
            
            // Remove leading slash if present and construct full path
            if (imagePath.startsWith("/")) {
                imagePath = imagePath.substring(1);
            }
            
            Path filePath = Paths.get(System.getProperty("user.dir"), imagePath);
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            // Read file content
            byte[] imageBytes = Files.readAllBytes(filePath);
            
            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Create filename
            String filename = listing.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + 
                             "_image_" + (imageIndex + 1) + 
                             getFileExtension(filePath);

            // Set headers for download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(imageBytes.length);

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String getFileExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex) : "";
    }
}



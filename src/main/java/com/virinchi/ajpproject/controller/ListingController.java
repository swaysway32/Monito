package com.virinchi.ajpproject.controller;

import com.virinchi.ajpproject.model.Listing;
import com.virinchi.ajpproject.repository.ListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/seller-upload")
    public String showUploadPage(Model model) {
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
            RedirectAttributes redirectAttributes
    ) {
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

            listingRepository.save(listing);

            redirectAttributes.addFlashAttribute("success", "Listing uploaded successfully!");
            return "redirect:/seller-upload";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Upload failed. Please try again.");
            return "redirect:/seller-upload";
        }
    }
}



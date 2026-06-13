package sneak_shop.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sneak_shop.service.CloudinaryStorageService;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicUploadController {

    private final CloudinaryStorageService cloudinaryStorageService;

    public PublicUploadController(CloudinaryStorageService cloudinaryStorageService) {
        this.cloudinaryStorageService = cloudinaryStorageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        String url = cloudinaryStorageService.uploadMedia(file);
        return ResponseEntity.ok(Map.of("url", url));
    }
}

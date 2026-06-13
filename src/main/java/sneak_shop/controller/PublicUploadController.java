package sneak_shop.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sneak_shop.service.CloudinaryStorageService;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicUploadController {

    private final CloudinaryStorageService storageService;

    public PublicUploadController(CloudinaryStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(Map.of("url", storageService.uploadMedia(file)));
    }
}

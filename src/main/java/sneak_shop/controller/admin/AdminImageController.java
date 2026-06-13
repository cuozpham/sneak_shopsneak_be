package sneak_shop.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sneak_shop.service.CloudinaryStorageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/images")
@PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
public class AdminImageController {

    private final CloudinaryStorageService cloudinaryStorageService;

    public AdminImageController(CloudinaryStorageService cloudinaryStorageService) {
        this.cloudinaryStorageService = cloudinaryStorageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        String url = cloudinaryStorageService.uploadMedia(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/upload-multiple")
    public ResponseEntity<Map<String, List<String>>> uploadMultiple(@RequestParam("files") List<MultipartFile> files) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(cloudinaryStorageService.uploadMedia(file));
        }
        return ResponseEntity.ok(Map.of("urls", urls));
    }
}

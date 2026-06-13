package sneak_shop.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sneak_shop.service.CloudinaryStorageService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/images")
@PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
public class AdminImageController {

    private final CloudinaryStorageService storageService;

    public AdminImageController(CloudinaryStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(Map.of("url", storageService.uploadMedia(file)));
    }

    @PostMapping("/upload-multiple")
    public ResponseEntity<Map<String, List<String>>> uploadMultiple(@RequestParam("files") List<MultipartFile> files) {
        List<String> urls = files.stream()
                .map(storageService::uploadMedia)
                .toList();
        return ResponseEntity.ok(Map.of("urls", urls));
    }
}

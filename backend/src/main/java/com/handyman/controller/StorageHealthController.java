package com.handyman.controller;
import com.handyman.service.CloudinaryStorage;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/storage")
public class StorageHealthController {
 private final CloudinaryStorage storage;
 public StorageHealthController(CloudinaryStorage storage) { this.storage=storage; }
 @GetMapping("/health") public ResponseEntity<Map<String,String>> health() { return storage.health(); }
}

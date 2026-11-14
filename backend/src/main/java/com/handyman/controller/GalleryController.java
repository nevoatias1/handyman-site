package com.handyman.controller;
import com.handyman.dto.GalleryDtos.*;
import com.handyman.service.GalleryService;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api")
public class GalleryController {
 private final GalleryService gallery;
 public GalleryController(GalleryService gallery) { this.gallery=gallery; }
 @GetMapping("/categories") public List<CategoryView> categories() { return gallery.categories(); }
 @GetMapping("/photos") public List<PhotoView> photos(@RequestParam(required=false) String category) { return gallery.photos(category); }
}

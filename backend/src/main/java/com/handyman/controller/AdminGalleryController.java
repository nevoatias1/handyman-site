package com.handyman.controller;
import com.handyman.dto.GalleryDtos.*;
import com.handyman.service.GalleryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/admin")
public class AdminGalleryController {
 private final GalleryService gallery;
 public AdminGalleryController(GalleryService gallery) { this.gallery=gallery; }
 @PostMapping("/categories") public CategoryView create(@Valid @RequestBody CategoryInput body) { return gallery.create(body); }
 @PatchMapping("/categories/{id}") public CategoryView rename(@PathVariable Long id,@Valid @RequestBody Rename body) { return gallery.rename(id,body); }
 @DeleteMapping("/categories/{id}") public void deleteCategory(@PathVariable Long id) { gallery.deleteCategory(id); }
 @PutMapping("/categories/reorder") public void orderCategories(@Valid @RequestBody Order body) { gallery.orderCategories(body.ids()); }
 @PostMapping(value="/photos",consumes="multipart/form-data") public List<PhotoView> upload(@RequestParam Long categoryId,@RequestPart("files") List<MultipartFile> files) { return gallery.upload(categoryId,files); }
 @PatchMapping("/photos/{id}") public PhotoView caption(@PathVariable Long id,@Valid @RequestBody Caption body) { return gallery.caption(id,body); }
 @DeleteMapping("/photos/{id}") public void deletePhoto(@PathVariable Long id) { gallery.deletePhoto(id); }
 @PutMapping("/categories/{id}/photos/reorder") public void orderPhotos(@PathVariable Long id,@Valid @RequestBody Order body) { gallery.orderPhotos(id,body.ids()); }
}

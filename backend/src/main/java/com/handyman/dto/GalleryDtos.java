package com.handyman.dto;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
public class GalleryDtos {
 public record CategoryInput(@NotBlank @Size(max=120) String name,
   @NotBlank @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max=100) String slug) {}
 public record Rename(@NotBlank @Size(max=120) String name) {}
 public record Caption(@Size(max=200) String caption) {}
 public record Order(@NotNull @Size(max=10000) List<@NotNull @Positive Long> ids) {}
 public record CategoryView(Long id,String name,String slug,int sortOrder,Instant createdAt) {}
 public record PhotoView(Long id,String url,String thumbUrl,String caption) {}
}

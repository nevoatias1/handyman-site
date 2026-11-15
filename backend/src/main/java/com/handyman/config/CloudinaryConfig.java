package com.handyman.config;
import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;

@Configuration
public class CloudinaryConfig {
 @Bean public Cloudinary cloudinary(ConfigurableEnvironment env) {
  String url = env.matchesProfiles("prod") ? (String)env.getSystemEnvironment().get("CLOUDINARY_URL") : env.getProperty("CLOUDINARY_URL");
  if (url != null) url = url.trim();
  if (url == null || url.isBlank()) {
   if (env.matchesProfiles("prod")) throw new IllegalStateException("CLOUDINARY_URL is required in prod");
   // Dev can boot without credentials; storage operations return a clear 503.
   return new Cloudinary();
  }
  try {
   if (!url.matches("^cloudinary://[^:@\\s/]+:[^@\\s/]+@[A-Za-z0-9_-]+$")) throw new IllegalArgumentException();
   return new Cloudinary(url);
  } catch (RuntimeException ex) { throw new IllegalStateException("CLOUDINARY_URL is invalid"); }
 }
}

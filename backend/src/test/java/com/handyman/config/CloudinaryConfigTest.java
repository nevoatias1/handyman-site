package com.handyman.config;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CloudinaryConfigTest {
 @Test void prodRequiresEnvironmentUrlEvenWithLocalProperty() {
  var env=new MockEnvironment() {
   @Override public Map<String,Object> getSystemEnvironment() { return Map.of(); }
  };
  env.setActiveProfiles("prod");
  env.setProperty("CLOUDINARY_URL","cloudinary://local:secret@test");
  assertThrows(IllegalStateException.class,()->new CloudinaryConfig().cloudinary(env));
 }
 @Test void invalidUrlIsRejectedWithoutLeakingCredentials() {
  var env=new MockEnvironment().withProperty("CLOUDINARY_URL","not-a-valid-secret-url");
  env.setActiveProfiles("dev");
  var ex=assertThrows(IllegalStateException.class,()->new CloudinaryConfig().cloudinary(env));
  assertEquals("CLOUDINARY_URL is invalid",ex.getMessage());
 }
}

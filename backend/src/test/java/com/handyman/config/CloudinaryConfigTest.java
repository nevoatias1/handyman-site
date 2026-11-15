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
 @Test void acceptsCloudNameWithUnderscore() {
  var env=new MockEnvironment().withProperty("CLOUDINARY_URL","cloudinary://test-key:test-secret@my_cloud-1");
  env.setActiveProfiles("dev");
  assertDoesNotThrow(()->new CloudinaryConfig().cloudinary(env));
 }
 @Test void trimsSurroundingWhitespaceBeforeConstructingClient() {
  var env=new MockEnvironment().withProperty("CLOUDINARY_URL"," \tcloudinary://test-key:test-secret@my-cloud-1\n ");
  env.setActiveProfiles("dev");
  var client=new CloudinaryConfig().cloudinary(env);
  assertEquals("my-cloud-1",client.config.cloudName);
  assertEquals("test-secret",client.config.apiSecret);
 }
 @Test void rejectsMissingSecret() {
  var env=new MockEnvironment().withProperty("CLOUDINARY_URL","cloudinary://test-key:@my_cloud");
  assertThrows(IllegalStateException.class,()->new CloudinaryConfig().cloudinary(env));
 }
 @Test void rejectsWrongScheme() {
  var env=new MockEnvironment().withProperty("CLOUDINARY_URL","https://test-key:test-secret@my_cloud");
  assertThrows(IllegalStateException.class,()->new CloudinaryConfig().cloudinary(env));
 }
 @Test void rejectsBlankProductionEnvironmentValue() {
  var env=new MockEnvironment() {
   @Override public Map<String,Object> getSystemEnvironment() { return Map.of("CLOUDINARY_URL"," \t\n "); }
  };
  env.setActiveProfiles("prod");
  var ex=assertThrows(IllegalStateException.class,()->new CloudinaryConfig().cloudinary(env));
  assertEquals("CLOUDINARY_URL is required in prod",ex.getMessage());
 }
}

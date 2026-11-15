package com.handyman.config;
import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CloudinaryConfigTest {
 private Cloudinary client(String value) {
  var env=new MockEnvironment().withProperty("CLOUDINARY_URL",value);
  env.setActiveProfiles("dev");
  return new CloudinaryConfig().cloudinary(env);
 }
 private void rejected(String input,String part) {
  var ex=assertThrows(IllegalStateException.class,()->client(input));
  assertEquals("CLOUDINARY_URL is invalid: "+part+" failed (cleaned value length="+input.length()+")",ex.getMessage());
  assertNull(ex.getCause());
  assertFalse(ex.getMessage().contains(input));
 }
 @Test void prodRequiresEnvironmentUrlEvenWithLocalProperty() {
  var env=new MockEnvironment() {
   @Override public Map<String,Object> getSystemEnvironment() { return Map.of(); }
  };
  env.setActiveProfiles("prod");
  env.setProperty("CLOUDINARY_URL","cloudinary://local:secret@test");
  assertThrows(IllegalStateException.class,()->new CloudinaryConfig().cloudinary(env));
 }
 @Test void acceptsCloudNameWithUnderscore() {
  var configured = assertDoesNotThrow(()->client("cloudinary://test-key:test-secret@my_cloud-1"));
  assertEquals("my_cloud-1", configured.config.cloudName);
  assertEquals("test-key", configured.config.apiKey);
  assertEquals("test-secret", configured.config.apiSecret);
  assertTrue(configured.config.secure);
 }
 @Test void trimsWhitespace() {
  assertEquals("my-cloud",client(" \tcloudinary://test-key:test-secret@my-cloud\n ").config.cloudName);
 }
 @Test void removesMatchingQuotes() {
  assertEquals("my-cloud",client("\"cloudinary://test-key:test-secret@my-cloud\"").config.cloudName);
  assertEquals("test-secret",client("'cloudinary://test-key:test-secret@my-cloud'").config.apiSecret);
 }
 @Test void removesPrefixAndQuotedWrappersInEitherOrder() {
  assertEquals("my-cloud",client(" CLOUDINARY_URL= 'cloudinary://test-key:test-secret@my-cloud' ").config.cloudName);
  assertEquals("test-key",client("\"CLOUDINARY_URL=cloudinary://test-key:test-secret@my-cloud\"").config.apiKey);
 }
 @Test void diagnosesMissingSecret() { rejected("cloudinary://test-key:@my-cloud","api secret"); }
 @Test void diagnosesWrongScheme() { rejected("https://test-key:test-secret@my-cloud","scheme"); }
 @Test void diagnosesMissingKey() { rejected("cloudinary://:test-secret@my-cloud","api key"); }
 @Test void diagnosesCloudName() { rejected("cloudinary://test-key:test-secret@","cloud name"); }
 @Test void diagnosesExtraCharacters() {
  rejected("cloudinary://test-key:test-secret@my-cloud/path","extra characters");
  rejected("cloudinary://test-key:test-secret@my-cloud?x=y","extra characters");
 }
 @Test void diagnosticsNeverContainCredentialParts() {
  var ex=assertThrows(IllegalStateException.class,()->client("cloudinary://sensitive-key:sensitive-secret@sensitive-cloud/path"));
  for(String part:new String[]{"sensitive-key","sensitive-secret","sensitive-cloud"}) assertFalse(ex.getMessage().contains(part));
  assertNull(ex.getCause());
 }
 @Test void rejectsBlankInProd() {
  for(String input:new String[]{" \t\n ","\"\"","CLOUDINARY_URL=''"}){
   var env=new MockEnvironment() {
    @Override public Map<String,Object> getSystemEnvironment() { return Map.of("CLOUDINARY_URL",input); }
   };
   env.setActiveProfiles("prod");
   var ex=assertThrows(IllegalStateException.class,()->new CloudinaryConfig().cloudinary(env));
   assertEquals("CLOUDINARY_URL is required in prod (cleaned value length=0)",ex.getMessage());
  }
 }
}

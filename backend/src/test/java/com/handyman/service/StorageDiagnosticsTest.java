package com.handyman.service;
import com.cloudinary.Cloudinary;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StorageDiagnosticsTest {
 StorageDiagnostics diagnostics=new StorageDiagnostics(new Cloudinary(Map.of("cloud_name","demo","api_key","private-key","api_secret","private-secret")));
 @Test void stripsCredentialsSignaturesUrlsAndRequests() {
  String message="Invalid image HTTP status 401 api_key=private-key api_secret=private-secret signature=opaque-signature https://host/path?api_secret=private-secret cloudinary://private-key:private-secret@demo request={headers: credentials}";
  String safe=diagnostics.sanitize(message);
  for(String value:List.of("private-key","private-secret","opaque-signature","https://host","cloudinary://","headers: credentials")) assertFalse(safe.contains(value),safe);
  assertTrue(safe.contains("Invalid image"));
  assertFalse(safe.contains("\n"));
 }
 @Test void stripsQuotedAndEncodedSecrets() {
  String input="{\"api_key\":\"private-key\",\"api_secret\":\"private-secret\",\"signature\":\"random-signature\"} "+Base64.getEncoder().encodeToString("private-key:private-secret".getBytes());
  String safe=diagnostics.sanitize(input);
  for(String value:List.of("private-key","private-secret","random-signature",Base64.getEncoder().encodeToString("private-key:private-secret".getBytes()))) assertFalse(safe.contains(value));
 }
 @Test void extractsOnlyAvailableHttpStatus() {
  assertEquals("401",diagnostics.status(new java.io.IOException("Server returned HTTP response code: 401")));
  assertEquals("unavailable",diagnostics.status(new java.io.IOException("Invalid image")));
 }
 @Test void removesSigningRequestDetails() {
  String safe=diagnostics.sanitize("Invalid Signature abcdef0123456789abcdef0123456789abcdef0123. String to sign - folder=internal&timestamp=123");
  assertFalse(safe.contains("abcdef0123456789abcdef0123456789abcdef0123"));
  assertFalse(safe.contains("folder=internal"));
 }
}

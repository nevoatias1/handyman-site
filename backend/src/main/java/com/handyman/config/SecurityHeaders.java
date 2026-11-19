package com.handyman.config;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;

public final class SecurityHeaders {
 private SecurityHeaders() {}
 public static String policy() {
  String hashes="";
  try {
   var resource=new ClassPathResource("static/index.html");
   if(resource.exists()) {
    String html=new String(resource.getContentAsByteArray(),StandardCharsets.UTF_8);
    var matcher=Pattern.compile("<script[^>]*type=\"application/ld\\+json\"[^>]*>(.*?)</script>",Pattern.DOTALL).matcher(html);
    while(matcher.find()) hashes+=" 'sha256-"+Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(matcher.group(1).getBytes(StandardCharsets.UTF_8)))+"'";
   }
  } catch(Exception ex) { throw new IllegalStateException("Unable to generate CSP"); }
  return "default-src 'none'; script-src 'self'"+hashes+"; style-src 'self'; img-src 'self' https://res.cloudinary.com blob: data:; font-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'";
 }
}

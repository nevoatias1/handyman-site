package com.handyman.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.util.regex.Pattern;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;

@Configuration
public class CloudinaryConfig {
 private static final Pattern URL = Pattern.compile(
     "^cloudinary://([^:@\\s/]+):([^@\\s/]+)@([A-Za-z0-9_-]+)$", Pattern.UNICODE_CHARACTER_CLASS);
 private static final Pattern KEY = Pattern.compile("[^:@\\s/]+", Pattern.UNICODE_CHARACTER_CLASS);
 private static final Pattern SECRET = Pattern.compile("[^@\\s/]+", Pattern.UNICODE_CHARACTER_CLASS);

 @Bean public Cloudinary cloudinary(ConfigurableEnvironment env) {
  String raw = env.matchesProfiles("prod") ? (String)env.getSystemEnvironment().get("CLOUDINARY_URL") : env.getProperty("CLOUDINARY_URL");
  String url = clean(raw);
  if (url.isEmpty()) {
   if (env.matchesProfiles("prod")) throw new IllegalStateException("CLOUDINARY_URL is required in prod (cleaned value length=0)");
   return new Cloudinary();
  }
  var parts = URL.matcher(url);
  if (!parts.matches()) throw invalid(failedPart(url), url.length());
  try {
   String apiKey = parts.group(1);
   String apiSecret = parts.group(2);
   String cloudName = parts.group(3);
   return new Cloudinary(ObjectUtils.asMap("cloud_name", cloudName, "api_key", apiKey,
       "api_secret", apiSecret, "secure", true));
  } catch (RuntimeException ex) {
   // Do not retain the SDK exception: its message/cause may contain credentials.
   throw invalid("extra characters (SDK rejected cleaned URL)", url.length());
  }
 }

 private static String clean(String value) {
  String result = value == null ? "" : value.strip();
  boolean prefixRemoved = false;
  boolean changed;
  do {
   changed = false;
   if (result.length() >= 2 && ((result.startsWith("\"") && result.endsWith("\""))
       || (result.startsWith("'") && result.endsWith("'")))) {
    result = result.substring(1, result.length()-1).strip();
    changed = true;
   }
   if (!prefixRemoved && result.startsWith("CLOUDINARY_URL=")) {
    result = result.substring("CLOUDINARY_URL=".length()).strip();
    prefixRemoved = true;
    changed = true;
   }
  } while (changed);
  return result;
 }

 private static String failedPart(String value) {
  if (value.indexOf('"') >= 0 || value.indexOf('\'') >= 0) return "extra characters";
  if (!value.startsWith("cloudinary://")) return "scheme";
  String rest = value.substring("cloudinary://".length());
  int at = rest.indexOf('@');
  if (at < 0) return "cloud name";
  if (at != rest.lastIndexOf('@')) return "extra characters";
  String credentials = rest.substring(0, at);
  int colon = credentials.indexOf(':');
  if (colon < 0 || !KEY.matcher(credentials.substring(0, colon)).matches()) return "api key";
  if (!SECRET.matcher(credentials.substring(colon+1)).matches()) return "api secret";
  String cloud = rest.substring(at+1);
  if (cloud.contains("/") || cloud.contains("?") || cloud.contains("#")) return "extra characters";
  return "cloud name";
 }

 private static IllegalStateException invalid(String part, int length) {
  return new IllegalStateException("CLOUDINARY_URL is invalid: " + part + " failed (cleaned value length=" + length + ")");
 }
}

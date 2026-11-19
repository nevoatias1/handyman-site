package com.handyman.config;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SecurityHeadersTest {
 @Test void policyAllowsOnlyRequiredSources() {
  String policy=SecurityHeaders.policy();
  assertTrue(policy.contains("script-src 'self'"));
  assertFalse(policy.contains("unsafe-inline"));assertFalse(policy.contains("unsafe-eval"));
  assertTrue(policy.contains("https://res.cloudinary.com blob: data:"));
  assertTrue(policy.contains("frame-ancestors 'none'"));
 }
}

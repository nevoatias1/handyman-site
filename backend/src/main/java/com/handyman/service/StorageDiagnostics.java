package com.handyman.service;

import com.cloudinary.Cloudinary;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import org.slf4j.*;
import org.springframework.stereotype.Component;

@Component
public class StorageDiagnostics {
 private static final Logger LOG=LoggerFactory.getLogger(StorageDiagnostics.class);
 private final Cloudinary cloud;
 public StorageDiagnostics(Cloudinary cloud) { this.cloud=cloud; }
 public String sanitize(String input) {
  String text=input==null?"No error details available":input;
  for(String secret:new String[]{cloud.config.apiKey,cloud.config.apiSecret}) {
   if(secret!=null && !secret.isEmpty()) {
    text=text.replace(secret,"[redacted]").replace(URLEncoder.encode(secret,StandardCharsets.UTF_8),"[redacted]");
    text=text.replace(Base64.getEncoder().encodeToString(secret.getBytes(StandardCharsets.UTF_8)),"[redacted]");
   }
  }
  if(cloud.config.apiKey!=null && cloud.config.apiSecret!=null) text=text.replace(
   Base64.getEncoder().encodeToString((cloud.config.apiKey+":"+cloud.config.apiSecret).getBytes(StandardCharsets.UTF_8)),"[redacted]");
  text=text.replaceAll("(?i)(?:cloudinary|https?)://[^\\s\\x22'<>]+","[URL redacted]");
  text=text.replaceAll("(?is)\\b(?:request|request_body|request_headers)\\s*[:=].*","[request redacted]");
  text=text.replaceAll("(?i)([\\x22']?(?:api[_-]?key|api[_-]?secret|cloudinary_url|signature|authorization|password|password_hash|admin_password_hash|db_password|token|x-xsrf-token)[\\x22']?\\s*[:=]\\s*)(?:\\x22[^\\x22]*\\x22|'[^']*'|[^\\s,;}]+)","$1[redacted]");
  text=text.replaceAll("(?i)jdbc:[^\\s\\x22'<>]+","[database URL redacted]");
  text=text.replaceAll("(?is)string to sign.*","[signing input redacted]");
  text=text.replaceAll("(?i)\\b[0-9a-f]{40,64}\\b","[signature redacted]");
  text=text.replaceAll("[\\r\\n\\t\\p{Cntrl}]"," ");
  return text.length()>1200?text.substring(0,1200)+"…":text;
 }
 public String status(Throwable ex) {
  Set<Throwable> visited=Collections.newSetFromMap(new IdentityHashMap<>());
  for(Throwable t=ex;t!=null && visited.add(t);t=t.getCause()) {
   for(String method:List.of("getStatusCode","getHttpStatus","getResponseCode")) {
    try {
     Object value=t.getClass().getMethod(method).invoke(t);
     if(value instanceof Number n && n.intValue()>=100 && n.intValue()<=599) return String.valueOf(n.intValue());
    } catch(ReflectiveOperationException | RuntimeException ignored) { }
   }
   Matcher m=Pattern.compile("(?i)(?:HTTP(?: response)?(?: status| code)?|status(?: code)?)[ :=]+([1-5][0-9]{2})\\b").matcher(Objects.toString(t.getMessage(),""));
   if(m.find()) return m.group(1);
   if(t.getCause()==t) break;
  }
  return "unavailable";
 }
 public void warn(String operation,Throwable ex) {
  LOG.warn("Cloudinary {} failed: exception={}, httpStatus={}, message={}",operation,
   ex.getClass().getName(),status(ex),sanitize(ex.getMessage()));
 }
 public void unexpected(Throwable ex) {
  LOG.error("Unexpected gallery upload failure",safeTrace(ex,new IdentityHashMap<>(),0));
 }
 private Throwable safeTrace(Throwable ex,Map<Throwable,Boolean> seen,int depth) {
  if(depth>8 || seen.put(ex,true)!=null) return new RuntimeException("[cause chain truncated]");
  var safe=new RuntimeException(ex.getClass().getName()+": [exception payload omitted to protect customer data]");
  safe.setStackTrace(Arrays.stream(ex.getStackTrace()).map(frame -> new StackTraceElement(
   sanitize(frame.getClassName()),sanitize(frame.getMethodName()),frame.getFileName()==null?null:sanitize(frame.getFileName()),frame.getLineNumber())).toArray(StackTraceElement[]::new));
  if(ex.getCause()!=null) safe.initCause(safeTrace(ex.getCause(),seen,depth+1));
  for(Throwable suppressed:ex.getSuppressed()) safe.addSuppressed(safeTrace(suppressed,seen,depth+1));
  return safe;
 }
}

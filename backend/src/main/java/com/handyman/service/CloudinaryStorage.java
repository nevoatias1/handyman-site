package com.handyman.service;
import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import java.io.IOException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryStorage {
 private final Cloudinary cloud;
 private final StorageDiagnostics diagnostics;
 public CloudinaryStorage(Cloudinary cloud,StorageDiagnostics diagnostics) { this.cloud=cloud; this.diagnostics=diagnostics; }
 private void configured() {
  if (cloud.config.cloudName==null || cloud.config.apiKey==null || cloud.config.apiSecret==null)
   throw new GalleryException(503,"שירות התמונות אינו מוגדר");
 }
 public String upload(MultipartFile file,String slug) {
  configured();
  try {
   Map result = cloud.uploader().upload(file.getBytes(), Map.of(
    "folder","handyman-site/"+slug,"public_id",UUID.randomUUID().toString(),
    "resource_type","image","overwrite",false,
    "transformation",new Transformation().width(2000).height(2000).crop("limit")));
   Object id = result.get("public_id");
   if (!(id instanceof String value) || value.isBlank()) throw new IOException();
   return value;
  } catch(IOException | RuntimeException ex) { diagnostics.warn("upload",ex); if(ex instanceof RuntimeException) diagnostics.unexpected(ex); throw new GalleryException(502,"העלאת התמונה נכשלה"); }
 }
 public void delete(String id) {
  configured();
  try {
   Object result = cloud.uploader().destroy(id,Map.of("resource_type","image","invalidate",true)).get("result");
   if (!"ok".equals(result) && !"not found".equals(result)) throw new IOException();
  } catch(IOException | RuntimeException ex) { diagnostics.warn("delete",ex); throw new GalleryException(502,"מחיקת התמונה נכשלה"); }
 }
 public String url(String id,boolean thumbnail) {
  if (cloud.config.cloudName==null) throw new GalleryException(503,"שירות התמונות אינו מוגדר");
  Transformation t = new Transformation().fetchFormat("auto").quality("auto");
  if (thumbnail) t.width(600).crop("limit");
  return cloud.url().secure(true).transformation(t).generate(id);
 }
 public org.springframework.http.ResponseEntity<Map<String,String>> health() {
  String name=diagnostics.sanitize(Objects.toString(cloud.config.cloudName,""));
  try {
   configured();
   Map result=cloud.api().ping(Map.of());
   if(!"ok".equals(result.get("status"))) throw new IOException("Cloudinary ping returned an unexpected status");
   return org.springframework.http.ResponseEntity.ok(Map.of("cloudinary","ok","cloudName",name));
  } catch(Exception ex) {
   diagnostics.warn("ping",ex);
   int status=ex instanceof GalleryException ge?ge.status:502;
   return org.springframework.http.ResponseEntity.status(status).body(Map.of("cloudinary","error","cloudName",name,"reason",diagnostics.sanitize(ex.getMessage())));
  }
 }
}

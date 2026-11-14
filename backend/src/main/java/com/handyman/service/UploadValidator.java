package com.handyman.service;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class UploadValidator {
 public static final long MAX_BYTES = 10L*1024*1024;
 public void validate(List<MultipartFile> files) {
  if (files==null || files.isEmpty() || files.size()>10) throw new GalleryException(400,"יש לבחור בין תמונה אחת ל־10 תמונות");
  for (MultipartFile f:files) {
   if (f.isEmpty()) throw new GalleryException(400,"קובץ התמונה ריק");
   if (f.getSize()>MAX_BYTES) throw new GalleryException(413,"גודל תמונה מרבי הוא 10MB");
   try (InputStream in=f.getInputStream()) {
    byte[] b=in.readNBytes(128);
    if (!supported(b)) throw new GalleryException(400,"סוג קובץ לא נתמך. יש להעלות JPEG, PNG, WebP או HEIC");
   } catch(IOException ex) { throw new GalleryException(400,"לא ניתן לקרוא את הקובץ"); }
  }
 }
 private boolean supported(byte[] b) {
  if (b.length>=3 && (b[0]&255)==255 && (b[1]&255)==216 && (b[2]&255)==255) return true;
  if (b.length>=8 && Arrays.equals(Arrays.copyOf(b,8),new byte[]{(byte)137,80,78,71,13,10,26,10})) return true;
  if (b.length>=12 && text(b,0,4).equals("RIFF") && text(b,8,4).equals("WEBP")) return true;
  if (b.length>=16 && text(b,4,4).equals("ftyp")) {
   long size = Integer.toUnsignedLong(java.nio.ByteBuffer.wrap(b,0,4).getInt());
   if (size<16 || size>b.length) return false;
   Set<String> brands=Set.of("heic","heix","hevc","hevx");
   if (brands.contains(text(b,8,4))) return true;
   for (int i=16;i+4<=size;i+=4) if(brands.contains(text(b,i,4))) return true;
  }
  return false;
 }
 private String text(byte[] b,int start,int length) { return new String(b,start,length,StandardCharsets.US_ASCII); }
}

package com.handyman.controller;
import java.util.Map;
import com.handyman.service.GalleryException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.*;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class GalleryErrors {
 private final com.handyman.service.StorageDiagnostics diagnostics;
 public GalleryErrors(com.handyman.service.StorageDiagnostics diagnostics) { this.diagnostics=diagnostics; }
 @ExceptionHandler(Exception.class) ResponseEntity<?> unexpected(Exception ex,jakarta.servlet.http.HttpServletRequest request) throws Exception {

  diagnostics.unexpected(ex);
  return ResponseEntity.status(500).body(Map.of("message","אירעה שגיאה בהעלאת התמונות"));
 }
 @ExceptionHandler({org.springframework.web.servlet.resource.NoResourceFoundException.class,org.springframework.web.servlet.NoHandlerFoundException.class}) ResponseEntity<?> notFound() { return ResponseEntity.status(404).contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(Map.of("message","המשאב לא נמצא")); }
 @ExceptionHandler(GalleryException.class) ResponseEntity<?> gallery(GalleryException e) { return ResponseEntity.status(e.status).body(Map.of("message",e.getMessage())); }
 @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<?> oversized() { return ResponseEntity.status(413).body(Map.of("message","גודל תמונה מרבי הוא 10MB ועד 10 תמונות בבקשה")); }
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> conflict() { return ResponseEntity.status(409).body(Map.of("message","הפעולה מתנגשת בנתונים קיימים")); }
 @ExceptionHandler({MethodArgumentNotValidException.class,ServletRequestBindingException.class,HttpMessageNotReadableException.class,MultipartException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
 ResponseEntity<?> invalid() { return ResponseEntity.badRequest().body(Map.of("message","פרטי הבקשה אינם תקינים")); }
}

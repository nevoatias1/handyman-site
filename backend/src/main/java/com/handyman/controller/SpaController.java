package com.handyman.controller;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestController
public class SpaController {
 @GetMapping("/")
 public ResponseEntity<Resource> home() throws NoResourceFoundException {
  Resource index=new ClassPathResource("static/index.html");
  if(!index.exists()) throw new NoResourceFoundException(HttpMethod.GET,"index.html");
  return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(index);
 }
}
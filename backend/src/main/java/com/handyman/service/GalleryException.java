package com.handyman.service;
public class GalleryException extends RuntimeException {
 public final int status;
 public GalleryException(int status,String message) { super(message); this.status=status; }
}

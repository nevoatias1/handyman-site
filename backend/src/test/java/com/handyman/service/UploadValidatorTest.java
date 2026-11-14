package com.handyman.service;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;
class UploadValidatorTest {
 private final UploadValidator validator=new UploadValidator();
 private void accepts(byte[] bytes) {
  assertDoesNotThrow(()->validator.validate(List.of(new MockMultipartFile("files","ignored.bin","application/octet-stream",bytes))));
 }
 @Test void acceptsSupportedSignaturesRegardlessOfNameOrMime() {
  accepts(new byte[]{(byte)255,(byte)216,(byte)255,1});
  accepts(new byte[]{(byte)137,80,78,71,13,10,26,10});
  accepts("RIFFabcdWEBP".getBytes(StandardCharsets.US_ASCII));
  byte[] heic=ByteBuffer.allocate(24).putInt(24).put("ftypmif1".getBytes(StandardCharsets.US_ASCII))
    .putInt(0).put("heicmif1".getBytes(StandardCharsets.US_ASCII)).array();
  accepts(heic);
 }
 @Test void rejectsAvifAndEmptyFiles() {
  byte[] avif=ByteBuffer.allocate(20).putInt(20).put("ftypavif".getBytes(StandardCharsets.US_ASCII))
    .putInt(0).put("avif".getBytes(StandardCharsets.US_ASCII)).array();
  assertThrows(GalleryException.class,()->validator.validate(List.of(new MockMultipartFile("files","fake.heic","image/heic",avif))));
  assertThrows(GalleryException.class,()->validator.validate(List.of(new MockMultipartFile("files",new byte[0]))));
 }
}

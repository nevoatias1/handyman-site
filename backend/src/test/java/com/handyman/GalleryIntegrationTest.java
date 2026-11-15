package com.handyman;
import com.cloudinary.*;
import com.handyman.model.*;
import com.handyman.repository.*;
import com.handyman.service.UploadValidator;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.Cookie;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("dev") @Import(GalleryIntegrationTest.CloudMock.class)
@Transactional
class GalleryIntegrationTest extends TestCredentials {
 @TestConfiguration static class CloudMock {
  @Bean Uploader mockUploader() { return mock(Uploader.class); }
  @Bean Api mockAdminApi() { return mock(Api.class); }
  @Bean @Primary Cloudinary mockedCloudinary(Uploader uploader,Api adminApi) {
   return new Cloudinary(Map.of("cloud_name","test-cloud","api_key","test-key","api_secret","test-secret")) {
    @Override public Uploader uploader() { return uploader; }
    @Override public Api api() { return adminApi; }
   };
  }
 }
 @Autowired MockMvc mvc;
 @Autowired Uploader uploader;
 @Autowired Api adminApi;
 @Autowired CategoryRepository categories;
 @Autowired PhotoRepository photos;
 MockHttpSession session;
 Cookie csrf;
 Long categoryId;
 @BeforeEach void setup() throws Exception {
  reset(uploader,adminApi);
  when(uploader.upload(any(),anyMap())).thenAnswer(inv -> {
   Map options=inv.getArgument(1);
   return Map.of("public_id",options.get("folder")+"/"+options.get("public_id"));
  });
  when(uploader.destroy(anyString(),anyMap())).thenReturn(Map.of("result","ok"));
  var t=mvc.perform(get("/api/auth/csrf")).andReturn().getResponse().getCookie("XSRF-TOKEN");
  session=(MockHttpSession)mvc.perform(post("/api/auth/login").cookie(t).header("X-XSRF-TOKEN",t.getValue())
   .contentType("application/json").content("{\"username\":\"test-admin\",\"password\":\""+TEST_PASSWORD+"\"}"))
   .andExpect(status().isOk()).andReturn().getRequest().getSession();
  csrf=mvc.perform(get("/api/auth/csrf").session(session)).andReturn().getResponse().getCookie("XSRF-TOKEN");
  categoryId=categories.findBySlug("plumbing").orElseThrow().id;
 }
 MockMultipartFile image() { return new MockMultipartFile("files","client.jpg","image/jpeg",new byte[]{(byte)255,(byte)216,(byte)255,1}); }
 @Test void publicEndpointsAndSeedWorkAnonymously() throws Exception {
  mvc.perform(get("/api/categories")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5));
  mvc.perform(get("/api/photos").param("category","plumbing")).andExpect(status().isOk()).andExpect(content().json("[]"));
 }
 @Test void anonymousUploadIs401() throws Exception {
  mvc.perform(multipart("/api/admin/photos").file(image()).param("categoryId",categoryId.toString()))
   .andExpect(status().isUnauthorized());
  verifyNoInteractions(uploader);
 }
 @Test void uploadWithoutCsrfIs403() throws Exception {
  mvc.perform(multipart("/api/admin/photos").file(image()).session(session).param("categoryId",categoryId.toString()))
   .andExpect(status().isForbidden());
  verifyNoInteractions(uploader);
 }
 @Test void disguisedFileRejected() throws Exception {
  var file=new MockMultipartFile("files","evil.jpg","image/jpeg","<script>bad</script>".getBytes());
  mvc.perform(multipart("/api/admin/photos").file(file).session(session).cookie(csrf)
   .header("X-XSRF-TOKEN",csrf.getValue()).param("categoryId",categoryId.toString())).andExpect(status().isBadRequest());
  verifyNoInteractions(uploader);
 }
 @Test void oversizeRejected() throws Exception {
  var file=new MockMultipartFile("files","huge.jpg","image/jpeg",new byte[(int)UploadValidator.MAX_BYTES+1]);
  mvc.perform(multipart("/api/admin/photos").file(file).session(session).cookie(csrf)
   .header("X-XSRF-TOKEN",csrf.getValue()).param("categoryId",categoryId.toString())).andExpect(status().isPayloadTooLarge());
  verifyNoInteractions(uploader);
 }
 @Test void countLimitCheckedBeforeAnyUpload() throws Exception {
  var request=multipart("/api/admin/photos");
  for(int i=0;i<11;i++) request.file(image());
  mvc.perform(request.session(session).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue()).param("categoryId",categoryId.toString()))
   .andExpect(status().isBadRequest());
  verifyNoInteractions(uploader);
 }
 @Test void uploadTransformsAndPublicUrlsAndDelete() throws Exception {
  mvc.perform(multipart("/api/admin/photos").file(image()).session(session).cookie(csrf)
   .header("Origin","http://localhost:5173")
   .header("X-XSRF-TOKEN",csrf.getValue()).param("categoryId",categoryId.toString())).andExpect(status().isOk())
   .andExpect(jsonPath("$[0].url").value(org.hamcrest.Matchers.containsString("f_auto")))
   .andExpect(jsonPath("$[0].thumbUrl").value(org.hamcrest.Matchers.containsString("w_600")));
  var options=org.mockito.ArgumentCaptor.forClass(Map.class);
  verify(uploader).upload(any(),options.capture());
  assertEquals("handyman-site/plumbing",options.getValue().get("folder"));
  assertFalse(options.getValue().get("public_id").toString().contains("client"));
  assertTrue(options.getValue().get("transformation").toString().contains("c_limit"));
  assertTrue(options.getValue().get("transformation").toString().contains("w_2000"));
  mvc.perform(get("/api/photos").param("category","plumbing")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
  Photo p=photos.findByCategoryIdOrderBySortOrderAscIdAsc(categoryId).getFirst();
  mvc.perform(delete("/api/admin/photos/"+p.id).session(session).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue()))
   .andExpect(status().isOk());
  verify(uploader).destroy(eq(p.cloudinaryPublicId),anyMap());
  assertFalse(photos.existsById(p.id));
 }
 @Test void categoryWithPhotosReturns409() throws Exception {
  Photo p=new Photo();p.category=categories.findById(categoryId).orElseThrow();p.cloudinaryPublicId="test-photo";
  photos.saveAndFlush(p);
  mvc.perform(delete("/api/admin/categories/"+categoryId).session(session).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue()))
   .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("לא ניתן למחוק קטגוריה המכילה תמונות"));
 }
 @Test void categoryCreateRenameReorderAndEmptyDelete() throws Exception {
  mvc.perform(post("/api/admin/categories").session(session).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue())
   .contentType("application/json").content("{\"name\":\"בדיקה\",\"slug\":\"test-category\"}"))
   .andExpect(status().isOk());
  Category c=categories.findBySlug("test-category").orElseThrow();
  mvc.perform(patch("/api/admin/categories/"+c.id).session(session).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue())
   .contentType("application/json").content("{\"name\":\"שם חדש\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.slug").value("test-category"));
  var ids=new ArrayList<>(categories.findAllByOrderBySortOrderAscIdAsc().stream().map(x->x.id).toList());
  Collections.reverse(ids);
  mvc.perform(put("/api/admin/categories/reorder").session(session).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue())
   .contentType("application/json").content("{\"ids\":"+ids+"}"))
   .andExpect(status().isOk());
  assertEquals(c.id,categories.findAllByOrderBySortOrderAscIdAsc().getFirst().id);
  mvc.perform(delete("/api/admin/categories/"+c.id).session(session).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue()))
   .andExpect(status().isOk());
 }
 @Test void photoCaptionAndOrderValidation() throws Exception {
  Photo p=new Photo();p.category=categories.findById(categoryId).orElseThrow();p.cloudinaryPublicId="caption-test";
  photos.saveAndFlush(p);
  mvc.perform(patch("/api/admin/photos/"+p.id).session(session).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue())
   .contentType("application/json").content("{\"caption\":\"תיקון בבית\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.caption").value("תיקון בבית"));
  mvc.perform(patch("/api/admin/photos/"+p.id).session(session).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue())
   .contentType("application/json").content("{\"caption\":\""+"x".repeat(201)+"\"}"))
   .andExpect(status().isBadRequest());
  mvc.perform(put("/api/admin/categories/"+categoryId+"/photos/reorder").session(session).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue())
   .contentType("application/json").content("{\"ids\":[]}"))
   .andExpect(status().isBadRequest());
  mvc.perform(put("/api/admin/categories/"+categoryId+"/photos/reorder").session(session).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue())
   .contentType("application/json").content("{\"ids\":["+p.id+"]}"))
   .andExpect(status().isOk());
 }
 @Test void cloudFailureDoesNotDeleteDatabaseRow() throws Exception {
  Photo p=new Photo();p.category=categories.findById(categoryId).orElseThrow();p.cloudinaryPublicId="retained";
  photos.saveAndFlush(p);
  when(uploader.destroy(anyString(),anyMap())).thenThrow(new java.io.IOException("mock failure"));
  mvc.perform(delete("/api/admin/photos/"+p.id).session(session).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue()))
   .andExpect(status().isBadGateway());
  assertTrue(photos.existsById(p.id));
 }
 @Test void storageHealthIsAdminOnly() throws Exception {
  mvc.perform(get("/api/admin/storage/health")).andExpect(status().isUnauthorized());
  verifyNoInteractions(adminApi);
 }
 @Test void storageHealthSuccess() throws Exception {
  var response=mock(com.cloudinary.api.ApiResponse.class);
  when(response.get("status")).thenReturn("ok");
  when(adminApi.ping(anyMap())).thenReturn(response);
  mvc.perform(get("/api/admin/storage/health").session(session)).andExpect(status().isOk())
   .andExpect(jsonPath("$.cloudinary").value("ok")).andExpect(jsonPath("$.cloudName").value("test-cloud"));
 }
 @Test void storageHealthFailureIsSanitized() throws Exception {
  when(adminApi.ping(anyMap())).thenThrow(new Exception("HTTP status 401 api_secret=test-secret api_key=test-key signature=signed-value"));
  var result=mvc.perform(get("/api/admin/storage/health").session(session)).andExpect(status().isBadGateway())
   .andExpect(jsonPath("$.cloudinary").value("error")).andExpect(jsonPath("$.cloudName").value("test-cloud")).andReturn();
  String body=result.getResponse().getContentAsString();
  for(String value:List.of("test-secret","test-key","signed-value")) assertFalse(body.contains(value));
 }
}

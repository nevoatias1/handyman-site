package com.handyman;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("dev")
class SpaIntegrationTest extends TestCredentials {
 @Autowired MockMvc mvc;
 @Test void deepLinksServeHtml() throws Exception {
  for(String path:new String[]{"/","/admin","/admin/login","/another/route"})
   mvc.perform(get(path)).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/html"));
 }
 @Test void missingApiAndFilesRemainJson404EvenWhenHtmlAccepted() throws Exception {
  for(String path:new String[]{"/api/unknown","/missing.js","/assets/missing.css"})
   mvc.perform(get(path).accept("text/html")).andExpect(status().isNotFound())
    .andExpect(content().contentTypeCompatibleWith("application/json"));
 }
}

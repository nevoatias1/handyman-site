package com.handyman.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class SpaConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/")
            .resourceChain(true).addResolver(new PathResourceResolver() {
                @Override protected Resource getResource(String path, Resource location) throws IOException {
                    Resource resource = super.getResource(path, location);
                    if (resource != null) return resource;
                    if (path.equals("api") || path.startsWith("api/") || path.contains(".")) return null;
                    return super.getResource("index.html", location);
                }
            });
    }
}

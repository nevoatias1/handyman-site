package com.handyman.config;

import java.util.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.*;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

public class ProductionEnvironmentValidator implements EnvironmentPostProcessor, Ordered {
    public static final List<String> REQUIRED = List.of("DB_URL", "DB_USERNAME", "DB_PASSWORD",
        "ADMIN_USERNAME", "ADMIN_PASSWORD_HASH", "CLOUDINARY_URL");
    @Override public int getOrder() { return ConfigDataEnvironmentPostProcessor.ORDER + 1; }
    @Override public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.matchesProfiles("prod")) return;
        List<String> missing = REQUIRED.stream().filter(key -> {
            Object value = environment.getSystemEnvironment().get(key);
            return !(value instanceof String text) || text.isBlank();
        }).toList();
        if (!missing.isEmpty()) throw new IllegalStateException("Missing required production environment variables: " + String.join(", ", missing));
    }
}

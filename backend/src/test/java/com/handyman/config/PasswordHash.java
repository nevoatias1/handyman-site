package com.handyman.config;
import java.io.Console;
import java.util.Arrays;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Local console utility; never pass a password as a command-line argument. */
public class PasswordHash {
    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) throw new IllegalStateException("Run from an interactive terminal");
        char[] password = console.readPassword("Admin password: ");
        if (password == null || password.length == 0) throw new IllegalArgumentException("Password required");
        try { console.printf("%s%n", new BCryptPasswordEncoder(12).encode(new String(password))); }
        finally { Arrays.fill(password, '\0'); }
    }
}

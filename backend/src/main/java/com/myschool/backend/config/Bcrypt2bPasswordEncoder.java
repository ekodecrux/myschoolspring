package com.myschool.backend.config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Handles $2b$ hashes created by Python's bcrypt library.
 * Spring's BCryptPasswordEncoder only understands $2a$, so we
 * swap the prefix before delegating to the standard encoder.
 */
public class Bcrypt2bPasswordEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        // New passwords are encoded with $2a$ (Spring default)
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isEmpty()) {
            return false;
        }
        // Normalise $2b$ -> $2a$ so Spring can verify legacy hashes
        String normalised = encodedPassword.startsWith("$2b$")
                ? "$2a$" + encodedPassword.substring(4)
                : encodedPassword;
        return delegate.matches(rawPassword, normalised);
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return false;
    }
}

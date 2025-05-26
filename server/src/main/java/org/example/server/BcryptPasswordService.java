package org.example.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.service.IPasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@org.springframework.stereotype.Service
public class BcryptPasswordService implements IPasswordService {
    private final BCryptPasswordEncoder encoder;
    private static final Logger logger = LogManager.getLogger(BcryptPasswordService.class);

    @Autowired(required = false)
    public BcryptPasswordService(int workFactor) {
        if (workFactor < 10 || workFactor > 31) {
            throw new IllegalArgumentException("Work factor should be between 10 and 31");
        }

        this.encoder = new BCryptPasswordEncoder(workFactor);
        logger.info("Initialized BcryptPasswordService with work factor " + workFactor);
    }

    public BcryptPasswordService() {
        this(12);
    }

    @Override
    public String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            logger.error("Hash for a null password");
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        try {
            String hash = encoder.encode(password);
            logger.debug("The password is hashed");
            return hash;
        } catch (Exception ex) {
            logger.error("Error at hashing the password", ex);
            throw new RuntimeException("Error at trying hashing the password", ex);
        }
    }

    @Override
    public boolean verifyPassword(String password, String passwordHash) {
        if (password == null || password.isEmpty()) {
            logger.error("Trying to hash a null password");
            throw new IllegalArgumentException("The password cannot be null or empty");
        }

        if (passwordHash == null || passwordHash.isEmpty()) {
            logger.error("Trying to hash a null or empty password hash");
            throw new IllegalArgumentException("The password hash cannot be null or empty");
        }

        try {
            boolean result = encoder.matches(password, passwordHash);
            logger.debug("Verify password: " + (result ? "true" : "false"));
            return result;
        } catch (Exception ex) {
            logger.error("Error at verifying the password", ex);
            return false;
        }
    }
}
package org.example.service;

public interface IPasswordService {
    String hashPassword(String password);

    boolean verifyPassword(String password, String passwordHash);
}

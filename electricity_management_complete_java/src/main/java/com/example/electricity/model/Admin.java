package com.example.electricity.model;

public class Admin extends User {
    private final String password;

    public Admin(String adminId, String password) {
        super(adminId);
        this.password = password;
    }

    public boolean authenticate(String correctId, String correctPassword) {
        return getUserId().equals(correctId) && password.equals(correctPassword);
    }

    @Override
    public String getRole() {
        return "Administrator";
    }
}

package com.example.electricity.model;

public class User {
    private final String userId;

    public User(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public String getRole() {
        return "User";
    }
}

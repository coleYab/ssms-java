package com.example.ssms.security;

public enum Role {
    SUPER_ADMIN(100),
    ADMIN(80),
    TEACHER(50),
    STUDENT(10);

    private final int rank;

    Role(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }
}

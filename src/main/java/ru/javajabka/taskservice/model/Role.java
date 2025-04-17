package ru.javajabka.taskservice.model;

public enum Role {

    USER("USER"),
    MANAGER("MANAGER");

    private String role;

    Role(String role) {
        this.role = role;
    }
}
package ru.javajabka.taskservice.model;

public enum TaskStatus {

    TO_DO("TO_DO"),
    IN_PROGRESS("IN_PROGRESS"),
    DONE("DONE"),
    DELETE("DELETE");

    private String status;

    TaskStatus(String status) {
        this.status = status;
    }
}
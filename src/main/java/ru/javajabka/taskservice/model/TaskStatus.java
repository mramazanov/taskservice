package ru.javajabka.taskservice.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import ru.javajabka.taskservice.exception.BadRequestException;

import java.util.Arrays;

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
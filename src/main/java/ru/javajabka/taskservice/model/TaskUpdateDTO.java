package ru.javajabka.taskservice.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class TaskUpdateDTO {
    private final Long id;
    private final String title;
    private final String description;
    private TaskStatus status;
    private final LocalDate deadLine;
    private final Long assignee;
}
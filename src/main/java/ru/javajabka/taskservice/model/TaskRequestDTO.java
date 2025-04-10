package ru.javajabka.taskservice.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class TaskRequestDTO {
    private final String title;
    private final String description;
    private final LocalDate deadLine;
    private final Long author;
    private final Long assignee;
}
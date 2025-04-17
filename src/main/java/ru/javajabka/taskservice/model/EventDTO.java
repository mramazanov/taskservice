package ru.javajabka.taskservice.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EventDTO {

    private final String eventName;
    private final Long taskId;
    private final String from;
    private final String to;
    private final LocalDateTime event_date_time;
}
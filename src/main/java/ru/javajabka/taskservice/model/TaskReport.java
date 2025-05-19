package ru.javajabka.taskservice.model;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class TaskReport {
    private final Long teamTasks;
    private final Map<String, Long> tasksWithStatus;
    private final Map<String, Long> mostActiveUser;
    private final Long averageTaskCompleteTime;
}
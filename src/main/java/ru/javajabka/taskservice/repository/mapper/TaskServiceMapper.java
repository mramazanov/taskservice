package ru.javajabka.taskservice.repository.mapper;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.javajabka.taskservice.model.Task;
import ru.javajabka.taskservice.model.TaskStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Component
public class TaskServiceMapper implements RowMapper<Task> {

    @Override
    public Task mapRow(ResultSet rs, int rowNum) throws SQLException {
        LocalDateTime updated_at = null;
        Timestamp timestamp = rs.getObject("updated_at", Timestamp.class);

        if (timestamp != null) {
            updated_at = timestamp.toLocalDateTime();
        }

        return Task.builder()
                .id(rs.getLong("id"))
                .title(rs.getString("title"))
                .description(rs.getString("description"))
                .status(TaskStatus.valueOf(rs.getString("status")))
                .deadLine(rs.getDate("dead_line").toLocalDate())
                .author(rs.getLong("author"))
                .assignee(rs.getLong("assignee"))
                .createdAt(rs.getObject("created_at", Timestamp.class).toLocalDateTime())
                .updatedAt(updated_at)
                .build();
    }
}
package ru.javajabka.taskservice.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.javajabka.taskservice.exception.BadRequestException;
import ru.javajabka.taskservice.model.Task;
import ru.javajabka.taskservice.model.TaskStatus;
import ru.javajabka.taskservice.repository.mapper.TaskServiceMapper;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TaskServiceRepository {

    private static final String INSERT = """
            INSERT INTO task_service.task (title, description, status, dead_line, author, assignee, created_at)
            VALUES (:title, :description, 'TO_DO', :deadLine, :author, :assignee, now())
            RETURNING *;
            """;

    private static final String GET_BY_ID = """
            SELECT * FROM task_service.task
            WHERE status != 'DELETE' AND id = :id
            """;

    private static final String UPDATE = """
            UPDATE task_service.task
            SET title = :title, description = :description, status = :status, dead_line = :deadLine, assignee = :assignee, updated_at = now()
            WHERE id = :id
            RETURNING *;
            """;

    private static final String GET_ALL = """
            SELECT * FROM task_service.task
            WHERE (:assignee::integer is null OR assignee = :assignee::integer)
            AND (:status::varchar is null OR status = :status::varchar)
            AND (:status = 'DELETE' OR status != 'DELETE')
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TaskServiceMapper taskServiceMapper;

    public Task create(final Task task) {
        try {
            return jdbcTemplate.queryForObject(INSERT, taskToSql(task), taskServiceMapper);
        } catch (DuplicateKeyException exc) {
            throw new BadRequestException(String.format("Задача с названием %s уже существует", task.getTitle()));
        }

    }

    public Task getById(final Long id) {
        try {
            return jdbcTemplate.queryForObject(GET_BY_ID, new MapSqlParameterSource("id", id), taskServiceMapper);
        } catch (EmptyResultDataAccessException exc) {
            throw new BadRequestException(String.format("Задача с id %d не найдена", id));
        }
    }

    public Task update(final Task task) {
        try {
            return jdbcTemplate.queryForObject(UPDATE, taskToSql(task), taskServiceMapper);
        } catch (DuplicateKeyException exc) {
            throw new BadRequestException(String.format("Задача с названием %s уже существует", task.getTitle()));
        } catch (EmptyResultDataAccessException exc) {
            throw new BadRequestException(String.format("Задача с id %d не найдена", task.getId()));
        }

    }

    public List<Task> getAll(final Optional<TaskStatus> status, final Optional<Long> assignee) {
        return jdbcTemplate.query(GET_ALL, taskToSql(status, assignee), taskServiceMapper);
    }

    private MapSqlParameterSource taskToSql(final Task task) {
        MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("id", task.getId());
        parameterSource.addValue("title", task.getTitle());
        parameterSource.addValue("description", task.getDescription());
        parameterSource.addValue("status", Optional.ofNullable(task.getStatus()).orElse(TaskStatus.TO_DO).toString());
        parameterSource.addValue("deadLine", task.getDeadLine());
        parameterSource.addValue("author", task.getAuthor());
        parameterSource.addValue("assignee", task.getAssignee());
        return parameterSource;
    }

    private MapSqlParameterSource taskToSql(Optional<TaskStatus> status, final Optional<Long> assignee) {
        MapSqlParameterSource parameterSource = new MapSqlParameterSource();

        status.ifPresentOrElse(
                (e) -> parameterSource.addValue("status", e.toString()),
                () -> parameterSource.addValue("status", null));

        assignee.ifPresentOrElse(
                (e) -> parameterSource.addValue("assignee",e),
                () -> parameterSource.addValue("assignee", null));

        return parameterSource;
    }
}
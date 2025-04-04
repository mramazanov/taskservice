package ru.javajabka.taskservice.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.javajabka.taskservice.dto.TaskUpdateDTO;
import ru.javajabka.taskservice.exception.BadRequestException;
import ru.javajabka.taskservice.model.TaskRequest;
import ru.javajabka.taskservice.model.TaskResponse;
import ru.javajabka.taskservice.model.TaskStatus;
import ru.javajabka.taskservice.repository.mapper.TaskServiceMapper;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TaskServiceRepository {

    private static final String INSERT = """
            INSERT INTO taskservice.task (title, description, status, dead_line, author, assignee, created_at)
            VALUES (:title, :description, 'TO_DO', :deadLine, :author, :assignee, now())
            RETURNING *;
            """;

    private static final String GET_BY_ID = """
            SELECT * FROM taskservice.task
            WHERE status != 'DELETE' AND id = :id
            """;

    private static final String UPDATE = """
            UPDATE taskservice.task
            SET title = :title, description = :description, status = :status, dead_line = :deadLine, assignee = :assignee, updated_at = now()
            WHERE id = :id
            RETURNING *;
            """;

    private static final String GET_ALL = """
            SELECT * FROM taskservice.task
            WHERE (:assignee::integer is null OR assignee = :assignee::integer)
            AND (:status::varchar is null OR status = :status::varchar)
            AND (:status = 'DELETE' OR status != 'DELETE')
            """;


    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TaskServiceMapper taskServiceMapper;


    public TaskResponse create(final TaskRequest taskRequest) {
        try {
            return jdbcTemplate.queryForObject(INSERT, taskToSql(null, taskRequest), taskServiceMapper);
        } catch (DuplicateKeyException exc) {
            throw new BadRequestException(String.format("Задача с названием %s уже существует", taskRequest.getTitle()));
        }

    }

    public TaskResponse getById(final Long id) {
        try {
            return jdbcTemplate.queryForObject(GET_BY_ID, taskToSql(id, null), taskServiceMapper);
        } catch (EmptyResultDataAccessException exc) {
            throw new BadRequestException(String.format("Задача с id %d не найдена", id));
        }

    }

    public TaskResponse update(final TaskUpdateDTO taskUpdateDTO) {
        try {
            return jdbcTemplate.queryForObject(UPDATE, taskToSql(taskUpdateDTO), taskServiceMapper);
        } catch (DuplicateKeyException exc) {
            throw new BadRequestException(String.format("Задача с названием %s уже существует", taskUpdateDTO.getTitle()));
        } catch (EmptyResultDataAccessException exc) {
            throw new BadRequestException(String.format("Задача с id %d не найдена", taskUpdateDTO.getId()));
        }

    }

    public List<TaskResponse> getAll(final Optional<TaskStatus> status, final Optional<Long> assignee) {
        return jdbcTemplate.query(GET_ALL, taskToSql(status, assignee), taskServiceMapper);
    }

    private MapSqlParameterSource taskToSql(final Long id, final TaskRequest taskRequest) {
        MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("id", id);

        if (taskRequest != null) {
            parameterSource.addValue("title", taskRequest.getTitle());
            parameterSource.addValue("description", taskRequest.getDescription());
            parameterSource.addValue("deadLine", taskRequest.getDeadLine());
            parameterSource.addValue("author", taskRequest.getAuthor());
            parameterSource.addValue("assignee", taskRequest.getAssignee());
        }

        return parameterSource;
    }

    private MapSqlParameterSource taskToSql(final TaskUpdateDTO taskUpdateDTO) {
        MapSqlParameterSource parameterSource = new MapSqlParameterSource();

        parameterSource.addValue("id", taskUpdateDTO.getId());
        parameterSource.addValue("title", taskUpdateDTO.getTitle());
        parameterSource.addValue("description", taskUpdateDTO.getDescription());
        parameterSource.addValue("status", taskUpdateDTO.getStatus().toString());
        parameterSource.addValue("deadLine", taskUpdateDTO.getDeadLine());
        parameterSource.addValue("assignee", taskUpdateDTO.getAssignee());

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
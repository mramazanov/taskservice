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
import ru.javajabka.taskservice.repository.mapper.TaskMapper;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    private static final String GET_ALL_BY_IDS = """
            SELECT count(1) FROM task_service.task
            WHERE assignee IN (:ids)
            AND ((updated_at BETWEEN :startDate AND :endDate)
            OR (created_at BETWEEN :startDate AND :endDate));
            """;

    private static final String GET_ALL_PER_STATUS = """         
            WITH tasks_with_status AS ( 
                SELECT *
                FROM task_service.task
                WHERE assignee IN  (:ids)
                AND status <> 'DELETE'
                AND ((updated_at BETWEEN :startDate AND :endDate)
                OR (created_at BETWEEN :startDate AND :endDate))
            )
            SELECT status, COUNT(*) AS count  FROM tasks_with_status GROUP BY status;
            """;

    private static final String GET_MOST_ACTIVE_MEMBERS = """
            WITH allTasksTeam2 AS (
                SELECT *
                FROM task_service.task
                WHERE assignee IN  (:ids)
                  AND ((updated_at BETWEEN :startDate AND :endDate) OR
                       (created_at BETWEEN :startDate AND :endDate))
            ), mostActiveMembers AS (
                SELECT assignee, SUM(1) AS taskCount FROM allTasksTeam2 GROUP BY assignee
            )
            
            SELECT assignee, taskCount FROM mostActiveMembers WHERE taskCount = (SELECT MAX(taskCount) FROM mostActiveMembers);
            """;

    private static final String GET_AVG_TASK_DURATION = """
            WITH allTasksTeam2 AS (
                SELECT *
                FROM task_service.task
                WHERE assignee IN  (:ids)
                  AND ((updated_at BETWEEN :startDate AND :endDate) OR
                       (created_at BETWEEN :startDate AND :endDate))
            ), avgTaskDuration AS (
               SELECT ROUND(AVG(extract(EPOCH FROM (updated_at - created_at))) / 60) AS timeElapsed
               FROM allTasksTeam2
               WHERE status = 'DONE'
            )
            SELECT timeElapsed FROM avgTaskDuration
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TaskMapper taskMapper;

    public Task create(final Task task) {
        try {
            return jdbcTemplate.queryForObject(INSERT, taskToSql(task), taskMapper);
        } catch (DuplicateKeyException exc) {
            throw new BadRequestException(String.format("Задача с названием %s уже существует", task.getTitle()));
        }

    }

    public Task getById(final Long id) {
        try {
            return jdbcTemplate.queryForObject(GET_BY_ID, new MapSqlParameterSource("id", id), taskMapper);
        } catch (EmptyResultDataAccessException exc) {
            throw new BadRequestException(String.format("Задача с id %d не найдена", id));
        }
    }

    public Task update(final Task task) {
        try {
            return jdbcTemplate.queryForObject(UPDATE, taskToSql(task), taskMapper);
        } catch (DuplicateKeyException exc) {
            throw new BadRequestException(String.format("Задача с названием %s уже существует", task.getTitle()));
        } catch (EmptyResultDataAccessException exc) {
            throw new BadRequestException(String.format("Задача с id %d не найдена", task.getId()));
        }

    }

    public List<Task> getAll(final Optional<TaskStatus> status, final Optional<Long> assignee) {
        return jdbcTemplate.query(GET_ALL, taskToSql(status, assignee), taskMapper);
    }

    public Map<String, Long> getTasksPerStatus(Set<Long> assigneeIds, LocalDate startDate, LocalDate endDate) {
        return jdbcTemplate.query(GET_ALL_PER_STATUS, getTaskByMembersAnsDateToSql(assigneeIds, startDate, endDate), (rs) -> {
            Map<String, Long> map = new HashMap<>();
            while (rs.next()) {
                map.put(rs.getString("status"), rs.getLong("count"));
            }
            return map;
        });
    }

    public Long getAllByIds(Set<Long> assigneeIds, LocalDate startDate, LocalDate endDate) {
        return jdbcTemplate.queryForObject(GET_ALL_BY_IDS, getTaskByMembersAnsDateToSql(assigneeIds, startDate, endDate), (rs, rowNum) -> rs.getLong("count"));
    }

    public Map<Long, Long> getMostActiveMembers(Set<Long> assigneeIds, LocalDate startDate, LocalDate endDate) {
        return jdbcTemplate.query(GET_MOST_ACTIVE_MEMBERS, getTaskByMembersAnsDateToSql(assigneeIds, startDate, endDate), (rs) -> {
            Map<Long, Long> map = new HashMap<>();
            while (rs.next()) {
                map.put(rs.getLong("assignee"), rs.getLong("taskCount"));
            }
            return map;
        });
    }

    public Long getAvgTaskDuration(Set<Long> assigneeIds, LocalDate startDate, LocalDate endDate) {
        return jdbcTemplate.queryForObject(GET_AVG_TASK_DURATION, getTaskByMembersAnsDateToSql(assigneeIds, startDate, endDate),
                    (rs, rowNum) -> rs.getLong("timeElapsed")
                );
    }

    private MapSqlParameterSource getTaskByMembersAnsDateToSql(Set<Long> ids, LocalDate startDate, LocalDate endDate) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ids", ids);
        params.addValue("startDate", startDate);
        params.addValue("endDate", endDate);
        return params;
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
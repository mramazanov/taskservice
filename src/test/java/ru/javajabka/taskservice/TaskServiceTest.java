package ru.javajabka.taskservice;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.javajabka.taskservice.dto.TaskUpdateDTO;
import ru.javajabka.taskservice.exception.BadRequestException;
import ru.javajabka.taskservice.model.TaskRequest;
import ru.javajabka.taskservice.model.TaskResponse;
import ru.javajabka.taskservice.model.TaskStatus;
import ru.javajabka.taskservice.repository.TaskServiceRepository;
import ru.javajabka.taskservice.service.TaskService;
import ru.javajabka.taskservice.service.UserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskServiceRepository taskServiceRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TaskService taskService;

    @Test
    public void shouldReturnTaskResponse_WhenCreateValid() {
        TaskRequest taskRequest = buildTaskRequest("Task 1", "Desc for task 1", LocalDate.of(2025, 5, 5), 1L, 2L);
        TaskResponse taskResponse = buildTaskResponse(
                1L,
                "Task 1",
                "Desc for task 1",
                TaskStatus.TO_DO,
                LocalDate.of(2025, 5, 5), 1L, 2L,
                LocalDateTime.of(2025, 5, 3, 12, 15, 15),
                null
                );
        Mockito.when(taskServiceRepository.create(taskRequest)).thenReturn(taskResponse);
        TaskResponse result = taskService.create(taskRequest);
        Assertions.assertEquals(taskResponse, result);
        Mockito.verify(taskServiceRepository).create(taskRequest);
    }

    @Test
    public void shouldReturnException_WhenCreateNameEmpty() {
        TaskRequest taskRequest = buildTaskRequest("", "Desc for task 1", LocalDate.of(2025, 5, 5), 1L, 2L);
        final BadRequestException badRequestException = Assertions.assertThrows(
                BadRequestException.class,
                () -> taskService.create(taskRequest)
        );
        Assertions.assertEquals("Введите название задачи", badRequestException.getMessage());
    }

    @Test
    public void shouldReturnException_WhenCreateUserNotFound() {
        TaskRequest taskRequest = buildTaskRequest("", "Desc for task 1", LocalDate.of(2025, 5, 5), 1L, 2L);
        final BadRequestException badRequestException = Assertions.assertThrows(
                BadRequestException.class,
                () -> taskService.create(taskRequest)
        );
        Assertions.assertEquals("Введите название задачи", badRequestException.getMessage());
    }

    @Test
    public void shouldReturnException_WhenUserNotFound() {
        TaskRequest taskRequest = buildTaskRequest("Task 1", "Desc for task 1", LocalDate.of(2025, 5, 5), 100L, 2L);
        Mockito.when(taskServiceRepository.create(taskRequest)).thenThrow(new BadRequestException("Пользователь с id 100 не найден"));
        final BadRequestException badRequestException = Assertions.assertThrows(BadRequestException.class, () -> taskService.create(taskRequest));
        Assertions.assertEquals("Пользователь с id 100 не найден", badRequestException.getMessage());
        Mockito.verify(taskServiceRepository).create(taskRequest);
    }

    @Test
    public void shouldReturnTaskResponse_WhenTaskGetById() {
        TaskRequest taskRequest = buildTaskRequest(
                "Task 1",
                "Desc for task 1",
                LocalDate.of(2025, 5, 5), 1L, 2L
        );

        TaskResponse taskResponse = buildTaskResponse(
                1L,
                "Task 1",
                "Desc for task 1",
                TaskStatus.TO_DO,
                LocalDate.of(2025, 5, 5), 1L, 2L,
                LocalDateTime.of(2025, 5, 3, 12, 30, 30),
                null
        );

        Mockito.when(taskServiceRepository.create(taskRequest)).thenReturn(taskResponse);
        TaskResponse result = taskService.create(taskRequest);
        Assertions.assertEquals(taskResponse, result);
        Mockito.verify(taskServiceRepository).create(taskRequest);
    }

    @Test
    public void shouldReturnException_WhenTaskGetByIdInvalid() {
        Mockito.when(taskServiceRepository.getById(100L)).thenThrow(new BadRequestException("Задача с id 100 не найдена"));
        final BadRequestException badRequestException = Assertions.assertThrows(
                BadRequestException.class,
                () -> taskService.getById(100L)
        );
        Assertions.assertEquals("Задача с id 100 не найдена", badRequestException.getMessage());
    }

    @Test
    public void shouldReturnTaskResponse_WhenTaskUpdate() {
        TaskUpdateDTO taskUpdateDTO = buildTaskUpdateDTO(1L, "Task 1", "Desc for task 1", TaskStatus.IN_PROGRESS, LocalDate.of(2025, 5, 5), 1L);
        TaskResponse taskResponse = buildTaskResponse(
                taskUpdateDTO.getId(),
                taskUpdateDTO.getTitle(),
                taskUpdateDTO.getDescription(),
                taskUpdateDTO.getStatus(),
                taskUpdateDTO.getDeadLine(),
                1L,
                taskUpdateDTO.getAssignee(),
                LocalDateTime.of(2025, 5, 3, 12, 30, 30),
                LocalDateTime.of(2025, 5, 3, 16, 30, 30)
        );

        Mockito.when(taskServiceRepository.getById(taskUpdateDTO.getId())).thenReturn(taskResponse);
        Mockito.when(taskServiceRepository.update(taskUpdateDTO)).thenReturn(taskResponse);

        TaskResponse result = taskService.update(taskUpdateDTO);
        Assertions.assertEquals(taskResponse, result);
        Mockito.verify(taskServiceRepository).update(taskUpdateDTO);
    }

    @Test
    public void shouldReturnListTasks_WhenTaskGetAll() {
        TaskResponse taskResponse_one = buildTaskResponse(
                1L,
                "Task 1",
                "Desc for task 1",
                TaskStatus.IN_PROGRESS,
                LocalDate.of(2025, 5, 5),
                1L,
                1L,
                LocalDateTime.of(2025, 5, 3, 12, 30, 30),
                null
        );

        TaskResponse taskResponse_two = buildTaskResponse(
                2L,
                "Task 2",
                "Desc for task 2",
                TaskStatus.IN_PROGRESS,
                LocalDate.of(2025, 5, 6),
                1L,
                1L,
                LocalDateTime.of(2025, 5, 3, 12, 30, 30),
                null
        );

        Mockito.when(taskServiceRepository.getAll(Optional.of(TaskStatus.TO_DO), null)).thenReturn(List.of(taskResponse_one, taskResponse_two));
        List<TaskResponse> result = taskService.getAll(Optional.of(TaskStatus.TO_DO), null);
        Assertions.assertEquals(List.of(taskResponse_one, taskResponse_two), result);
        Mockito.verify(taskServiceRepository).getAll(Optional.of(TaskStatus.TO_DO), null);
    }

    private TaskRequest buildTaskRequest(String title, String description, LocalDate deadLine, Long author, Long assignee) {
        return TaskRequest.builder()
                .title(title)
                .description(description)
                .deadLine(deadLine)
                .author(author)
                .assignee(assignee)
                .build();

    }

    private TaskResponse buildTaskResponse(Long id, String title, String description, TaskStatus status, LocalDate deadLine, Long author, Long assignee, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return TaskResponse.builder()
                .id(id)
                .title(title)
                .description(description)
                .status(status)
                .deadLine(deadLine)
                .author(author)
                .assignee(assignee)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    private TaskUpdateDTO buildTaskUpdateDTO(Long id, String title, String description, TaskStatus status, LocalDate deadLine, Long assignee) {
        return TaskUpdateDTO.builder()
                .id(id)
                .title(title)
                .description(description)
                .status(status)
                .deadLine(deadLine)
                .assignee(assignee)
                .build();
    }
}

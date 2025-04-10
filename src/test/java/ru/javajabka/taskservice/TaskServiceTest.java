package ru.javajabka.taskservice;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.javajabka.taskservice.model.TaskUpdateDTO;
import ru.javajabka.taskservice.exception.BadRequestException;
import ru.javajabka.taskservice.model.TaskRequestDTO;
import ru.javajabka.taskservice.model.Task;
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
    public void shouldReturnTask_WhenCreateValid() {
        TaskRequestDTO taskRequest = buildTaskRequest("Task 1", "Desc for task 1", LocalDate.of(2025, 5, 5), 1L, 2L);
        Task task = buildTaskResponse(
                null,
                taskRequest.getTitle(),
                taskRequest.getDescription(),
                null,
                taskRequest.getDeadLine(),
                1L,
                2L,
                null,
                null
                );

        Mockito.when(taskServiceRepository.create(task)).thenReturn(task);
        Task result = taskService.create(taskRequest);
        Assertions.assertEquals(task, result);
        Mockito.verify(taskServiceRepository).create(task);
    }

    @Test
    public void shouldReturnException_WhenCreate_And_NameEmpty() {
        TaskRequestDTO taskRequest = buildTaskRequest("", "Desc for task 1", LocalDate.of(2025, 5, 5), 1L, 2L);
        final BadRequestException badRequestException = Assertions.assertThrows(
                BadRequestException.class,
                () -> taskService.create(taskRequest)
        );
        Assertions.assertEquals("Введите название задачи", badRequestException.getMessage());
    }

    @Test
    public void shouldReturnException_WhenCreateUserNotFound() {
        TaskRequestDTO taskRequest = buildTaskRequest("", "Desc for task 1", LocalDate.of(2025, 5, 5), 1L, 2L);
        final BadRequestException badRequestException = Assertions.assertThrows(
                BadRequestException.class,
                () -> taskService.create(taskRequest)
        );
        Assertions.assertEquals("Введите название задачи", badRequestException.getMessage());
    }

    @Test
    public void shouldReturnException_WhenUserNotFound() {
        TaskRequestDTO taskRequest = buildTaskRequest("Task 1", "Desc for task 1", LocalDate.of(2025, 5, 5), 100L, 2L);
        Task task = buildTaskResponse(null, taskRequest.getTitle(), taskRequest.getDescription(), null, taskRequest.getDeadLine(), taskRequest.getAuthor(), taskRequest.getAssignee(), null, null);
        Mockito.when(taskServiceRepository.create(task)).thenThrow(new BadRequestException("Пользователь с id 100 не найден"));
        final BadRequestException badRequestException = Assertions.assertThrows(BadRequestException.class, () -> taskService.create(taskRequest));
        Assertions.assertEquals("Пользователь с id 100 не найден", badRequestException.getMessage());
        Mockito.verify(taskServiceRepository).create(task);
    }

    @Test
    public void shouldReturnTaskResponse_WhenTaskGetById() {
        Task task = buildTaskResponse(
                1L,
                "Task 1",
                "Desc for task 1",
                TaskStatus.TO_DO,
                LocalDate.of(2025, 5, 5), 1L, 2L,
                LocalDateTime.of(2025, 5, 3, 12, 30, 30),
                null
        );

        Mockito.when(taskServiceRepository.getById(1L)).thenReturn(task);
        Task result = taskService.getById(1L);
        Assertions.assertEquals(task, result);
        Mockito.verify(taskServiceRepository).getById(1L);
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
        Task task = Task.builder()
                .id(taskUpdateDTO.getId())
                .title(taskUpdateDTO.getTitle())
                .description(taskUpdateDTO.getDescription())
                .status(taskUpdateDTO.getStatus())
                .deadLine(taskUpdateDTO.getDeadLine())
                .assignee(taskUpdateDTO.getAssignee())
                .build();

        Mockito.when(taskServiceRepository.getById(taskUpdateDTO.getId())).thenReturn(task);
        Mockito.when(taskServiceRepository.update(task)).thenReturn(task);
        Task result = taskService.update(taskUpdateDTO);
        Assertions.assertEquals(task, result);
        Mockito.verify(taskServiceRepository).getById(1L);
        Mockito.verify(taskServiceRepository).update(task);
    }

    @Test
    public void shouldReturnListTasks_WhenTaskGetAll() {
        Task taskResponse_one = buildTaskResponse(
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

        Task taskResponse_two = buildTaskResponse(
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
        List<Task> result = taskService.getAll(Optional.of(TaskStatus.TO_DO), null);
        Assertions.assertEquals(List.of(taskResponse_one, taskResponse_two), result);
        Mockito.verify(taskServiceRepository).getAll(Optional.of(TaskStatus.TO_DO), null);
    }

    private TaskRequestDTO buildTaskRequest(String title, String description, LocalDate deadLine, Long author, Long assignee) {
        return TaskRequestDTO.builder()
                .title(title)
                .description(description)
                .deadLine(deadLine)
                .author(author)
                .assignee(assignee)
                .build();

    }

    private Task buildTaskResponse(Long id, String title, String description, TaskStatus status, LocalDate deadLine, Long author, Long assignee, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return Task.builder()
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

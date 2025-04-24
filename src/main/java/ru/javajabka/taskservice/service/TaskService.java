package ru.javajabka.taskservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.javajabka.taskservice.listener.NotificationProducer;
import ru.javajabka.taskservice.exception.BadRequestException;
import ru.javajabka.taskservice.model.EventDTO;
import ru.javajabka.taskservice.model.Role;
import ru.javajabka.taskservice.model.Task;
import ru.javajabka.taskservice.model.TaskRequestDTO;
import ru.javajabka.taskservice.model.TaskStatus;
import ru.javajabka.taskservice.model.TaskUpdateDTO;
import ru.javajabka.taskservice.model.User;
import ru.javajabka.taskservice.repository.TaskServiceRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final UserService userService;
    private final NotificationProducer notificationProducer;
    private final TaskServiceRepository taskServiceRepository;

    @Transactional(rollbackFor = Exception.class)
    public Task create(final TaskRequestDTO taskRequest) {
        validate(taskRequest);

        Task task = Task.builder()
                .title(taskRequest.getTitle())
                .description(taskRequest.getDescription())
                .deadLine(taskRequest.getDeadLine())
                .author(taskRequest.getAuthor())
                .assignee(taskRequest.getAssignee())
                .build();

        Task createdTask = taskServiceRepository.create(task);

        notificationProducer.send(List.of(
                EventDTO.builder()
                        .eventName("task_created")
                        .taskId(createdTask.getId())
                        .eventDateTime(LocalDateTime.now())
                        .build()
        ));

        return createdTask;
    }

    @Transactional(readOnly = true)
    public Task getById(final Long id) {
        return taskServiceRepository.getById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Task update(final TaskUpdateDTO taskUpdateDTO, final Long authorId) {
        Task validatedTask = validate(taskUpdateDTO, authorId);

        Task task = Task.builder()
                .id(taskUpdateDTO.getId())
                .title(Optional.ofNullable(taskUpdateDTO.getTitle()).orElse(validatedTask.getTitle()))
                .description(Optional.ofNullable(taskUpdateDTO.getDescription()).orElse(validatedTask.getDescription()))
                .status(Optional.ofNullable(taskUpdateDTO.getStatus()).orElse(validatedTask.getStatus()))
                .deadLine(Optional.ofNullable(taskUpdateDTO.getDeadLine()).orElse(validatedTask.getDeadLine()))
                .assignee(Optional.ofNullable(taskUpdateDTO.getAssignee()).orElse(validatedTask.getAssignee()))
                .build();

        Task savedTask = taskServiceRepository.update(task);
        sendEvent(validatedTask, task);
        return savedTask;
    }

    @Transactional(readOnly = true)
    public List<Task> getAll(
            final Optional<TaskStatus> status,
            final Optional<Long> assignee
    ) {
        return taskServiceRepository.getAll(status, assignee);
    }

    private void sendEvent(final Task oldTask, final Task newTask) {
        LocalDateTime changeTime = LocalDateTime.now();
        List<EventDTO> eventsDTO = new ArrayList<>();

        if (!oldTask.getTitle().equals(newTask.getTitle())) {
            eventsDTO.add(EventDTO.builder()
                    .eventName("title_changed")
                    .taskId(newTask.getId())
                    .from(oldTask.getTitle())
                    .to(newTask.getTitle())
                    .eventDateTime(changeTime)
                    .build());
        }

        if (!oldTask.getDescription().equals(newTask.getDescription())) {
            eventsDTO.add(EventDTO.builder()
                    .eventName("description_changed")
                    .taskId(newTask.getId())
                    .from(oldTask.getDescription())
                    .to(newTask.getDescription())
                    .eventDateTime(changeTime)
                    .build());
        }

        if (!oldTask.getStatus().equals(newTask.getStatus())) {
            eventsDTO.add(EventDTO.builder()
                    .eventName("status_changed")
                    .taskId(oldTask.getId())
                    .from(oldTask.getStatus().toString())
                    .to(newTask.getStatus().toString())
                    .eventDateTime(changeTime)
                    .build());
        }

        if (!oldTask.getDeadLine().equals(newTask.getDeadLine())) {
            eventsDTO.add(EventDTO.builder()
                    .eventName("deadline_changed")
                    .taskId(newTask.getId())
                    .from(oldTask.getDeadLine().toString())
                    .to(newTask.getDeadLine().toString())
                    .eventDateTime(changeTime)
                    .build());
        }

        if (!oldTask.getAssignee().equals(newTask.getAssignee())) {
            eventsDTO.add(EventDTO.builder()
                    .eventName("assignee_changed")
                    .taskId(newTask.getId())
                    .from(oldTask.getAssignee().toString())
                    .to(newTask.getAssignee().toString())
                    .eventDateTime(changeTime)
                    .build());
        }

        notificationProducer.send(eventsDTO);
    }

    private void checkStatus(TaskStatus fromStatus, TaskStatus toStatus) {
        Map<TaskStatus, Set<TaskStatus>> forbiddenStatus = new HashMap<>();
        forbiddenStatus.put(TaskStatus.TO_DO, Set.of(TaskStatus.DONE));
        forbiddenStatus.put(TaskStatus.IN_PROGRESS, Set.of(TaskStatus.TO_DO));
        forbiddenStatus.put(TaskStatus.DONE, Set.of(TaskStatus.IN_PROGRESS, TaskStatus.TO_DO));

        Set<TaskStatus> taskStatus = forbiddenStatus.get(fromStatus);
        if (taskStatus.contains(toStatus)) {
                throw new BadRequestException(String.format("Статус не может быть изменён с %s на %s", fromStatus, toStatus));
        }
    }

    private void validate(final TaskRequestDTO taskRequest) {
        if (taskRequest == null) {
            throw new BadRequestException("Введите значения для задачи");
        }

        if (!StringUtils.hasText(taskRequest.getTitle())) {
            throw new BadRequestException("Введите название задачи");
        }

        if (!StringUtils.hasText(taskRequest.getDescription())) {
            throw new BadRequestException("Введите описание задачи");
        }

        if (taskRequest.getDeadLine() == null || taskRequest.getDeadLine().isBefore(LocalDate.now().plusDays(1))) {
            throw new BadRequestException("Введите дату дедлайна позже текущей даты");
        }

        if (taskRequest.getAuthor() == null || taskRequest.getAuthor() <= 0) {
            throw new BadRequestException("Введите идентификатор автора больше нуля");
        }

        if (taskRequest.getAssignee() == null || taskRequest.getAssignee() <= 0) {
            throw new BadRequestException("Введите идентификатор ответственного больше нуля");
        }
        List<User> users = userService.checkAndGetUsers(List.of(taskRequest.getAuthor(), taskRequest.getAssignee()));

        checkUserRole(users, taskRequest.getAuthor());
    }

    private Task validate(final TaskUpdateDTO taskUpdateDTO, final Long authorId) {
        if (taskUpdateDTO == null) {
            throw new BadRequestException("Введите значения для обновления задачи");
        }

        if (taskUpdateDTO.getDeadLine() != null) {
            if (taskUpdateDTO.getDeadLine().isBefore(LocalDate.now().plusDays(1))) {
                throw new BadRequestException("Введите дату дедлайна позже текущей даты");
            }
        }

        if (taskUpdateDTO.getAssignee() != null) {
            List<User> users = userService.checkAndGetUsers(List.of(taskUpdateDTO.getAssignee(), authorId));
            checkUserRole(users, authorId);
        }

        Task foundTask = taskServiceRepository.getById(taskUpdateDTO.getId());
        if (taskUpdateDTO.getStatus() != null) {
            checkStatus(foundTask.getStatus(), taskUpdateDTO.getStatus());
        }

        return foundTask;
    }

    private void checkUserRole(List<User> users, Long editorId) {
        users.stream().filter(user -> user.getId().equals(editorId)).findFirst().ifPresent(user -> {
            if (!user.getRole().equals(Role.MANAGER)) {
                throw new BadRequestException(String.format("Пользователь с id %d не является менеджером", user.getId()));
            }
        });
    }
}
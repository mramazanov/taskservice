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
import java.util.*;

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

        EventDTO eventDTO = EventDTO.builder()
                .eventName("task_created")
                .taskId(createdTask.getId())
                .from(null)
                .to(null)
                .event_date_time(LocalDateTime.now())
                .build();

        notificationProducer.send(eventDTO);
        return createdTask;
    }

    @Transactional(readOnly = true)
    public Task getById(final Long id) {
        return taskServiceRepository.getById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Task update(final TaskUpdateDTO taskUpdateDTO, final Long authorId) {

        validate(taskUpdateDTO, authorId);
        Task foundTask = taskServiceRepository.getById(taskUpdateDTO.getId());

        if (taskUpdateDTO.getStatus() != null) {
            checkStatus(foundTask.getStatus(), taskUpdateDTO.getStatus());
        }

        Task task = Task.builder()
                .id(taskUpdateDTO.getId())
                .title(Optional.ofNullable(taskUpdateDTO.getTitle()).orElse(foundTask.getTitle()))
                .description(Optional.ofNullable(taskUpdateDTO.getDescription()).orElse(foundTask.getDescription()))
                .status(Optional.ofNullable(taskUpdateDTO.getStatus()).orElse(foundTask.getStatus()))
                .deadLine(Optional.ofNullable(taskUpdateDTO.getDeadLine()).orElse(foundTask.getDeadLine()))
                .assignee(Optional.ofNullable(taskUpdateDTO.getAssignee()).orElse(foundTask.getAssignee()))
                .build();

        sendEvent(foundTask, task);
        return taskServiceRepository.update(task);
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

        if (!oldTask.getTitle().equals(newTask.getTitle())) {
            EventDTO eventDTO = buildEventDTO("title_changed", newTask.getId(), oldTask.getTitle(), newTask.getTitle(), changeTime);
            notificationProducer.send(eventDTO);
        }

        if (!oldTask.getDescription().equals(newTask.getDescription())) {
            EventDTO eventDTO = buildEventDTO("description_changed", newTask.getId(), oldTask.getDescription(), newTask.getDescription(), changeTime);
            notificationProducer.send(eventDTO);
        }

        if (!oldTask.getStatus().equals(newTask.getStatus())) {
            EventDTO eventDTO = buildEventDTO("status_changed", newTask.getId(), oldTask.getStatus().toString(), newTask.getStatus().toString(), changeTime);
            notificationProducer.send(eventDTO);
        }

        if (!oldTask.getDeadLine().equals(newTask.getDeadLine())) {
            EventDTO eventDTO = buildEventDTO("deadline_changed", newTask.getId(), oldTask.getDeadLine().toString(), newTask.getDeadLine().toString(), changeTime);
            notificationProducer.send(eventDTO);
        }

        if (!oldTask.getAssignee().equals(newTask.getAssignee())) {
            EventDTO eventDTO = buildEventDTO("assignee_changed", newTask.getId(), oldTask.getAssignee().toString(), newTask.getAssignee().toString(), changeTime);
            notificationProducer.send(eventDTO);
        }
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
        List<User> users = userService.checkUserId(List.of(taskRequest.getAuthor(), taskRequest.getAssignee()));

        checkUserRole(users, taskRequest.getAuthor());
    }

    private void validate(final TaskUpdateDTO taskUpdateDTO, final Long authorId) {
        if (taskUpdateDTO != null && taskUpdateDTO.getDeadLine() != null) {
            if (taskUpdateDTO.getDeadLine().isBefore(LocalDate.now().plusDays(1))) {
                throw new BadRequestException("Введите дату дедлайна позже текущей даты");
            }
        }

        if (taskUpdateDTO != null && taskUpdateDTO.getAssignee() != null) {
            List<User> user = userService.checkUserId(List.of(taskUpdateDTO.getAssignee(), authorId));
            checkUserRole(user, authorId);
        }
    }

    private void checkUserRole(List<User> users, Long authorId) {
        users.stream().filter(user -> user.getId().equals(authorId)).findFirst().ifPresent(user -> {
            if (!user.getRole().equals(Role.MANAGER)) {
                throw new BadRequestException(String.format("Пользователь с id %d не является менеджером", user.getId()));
            }
        });
    }

    private EventDTO buildEventDTO(String eventName, Long taskId, String from, String to, LocalDateTime eventDateTime) {
        return EventDTO.builder()
                .eventName(eventName)
                .taskId(taskId)
                .from(from)
                .to(to)
                .event_date_time(eventDateTime)
                .build();
    }
}
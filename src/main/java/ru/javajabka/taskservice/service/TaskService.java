package ru.javajabka.taskservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.javajabka.taskservice.dto.TaskUpdateDTO;
import ru.javajabka.taskservice.exception.BadRequestException;
import ru.javajabka.taskservice.model.TaskRequest;
import ru.javajabka.taskservice.model.TaskResponse;
import ru.javajabka.taskservice.model.TaskStatus;
import ru.javajabka.taskservice.repository.TaskServiceRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final UserService userService;
    private final TaskServiceRepository taskServiceRepository;

    @Transactional(rollbackFor = Exception.class)
    public TaskResponse create(final TaskRequest taskRequest) {
        validate(taskRequest);

        userService.checkUserId(List.of(taskRequest.getAuthor(), taskRequest.getAssignee()));

        return taskServiceRepository.create(taskRequest);
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(final Long id) {
        return taskServiceRepository.getById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public TaskResponse update(final TaskUpdateDTO taskUpdateDTO) {
        validate(taskUpdateDTO);
        userService.checkUserId(List.of(taskUpdateDTO.getAssignee()));
        TaskResponse foundTask = taskServiceRepository.getById(taskUpdateDTO.getId());

        TaskUpdateDTO taskToUpdate = TaskUpdateDTO.builder()
                .id(foundTask.getId())
                .title(Optional.ofNullable(taskUpdateDTO.getTitle()).orElse(foundTask.getTitle()))
                .description(Optional.ofNullable(taskUpdateDTO.getDescription()).orElse(foundTask.getDescription()))
                .status(Optional.ofNullable(taskUpdateDTO.getStatus()).orElse(foundTask.getStatus()))
                .deadLine(Optional.ofNullable(taskUpdateDTO.getDeadLine() ).orElse(foundTask.getDeadLine()))
                .assignee(Optional.ofNullable(taskUpdateDTO.getAssignee()).orElse(foundTask.getAssignee()))
                .build();

        return taskServiceRepository.update(taskToUpdate);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAll(
            final Optional<TaskStatus> status,
            final Optional<Long> assignee
    ) {
        return taskServiceRepository.getAll(status, assignee);
    }

    private void validate(final TaskRequest taskRequest) {
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
    }

    private void validate(final TaskUpdateDTO taskUpdateDTO) {
        if (taskUpdateDTO != null && taskUpdateDTO.getDeadLine() != null) {
            if (taskUpdateDTO.getDeadLine().isBefore(LocalDate.now().plusDays(1))) {
                throw new BadRequestException("Введите дату дедлайна позже текущей даты");
            }
        }
    }
}
package ru.javajabka.taskservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.javajabka.taskservice.model.TaskUpdateDTO;
import ru.javajabka.taskservice.model.TaskRequestDTO;
import ru.javajabka.taskservice.model.Task;
import ru.javajabka.taskservice.model.TaskStatus;
import ru.javajabka.taskservice.service.TaskService;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/task")
@Tag(name = "Задача")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Создать задачу")
    public Task create(@RequestBody final TaskRequestDTO taskRequest) {
        return taskService.create(taskRequest);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить задачу")
    public Task get(@PathVariable final Long id) {
        return taskService.getById(id);
    }

    @PatchMapping
    @Operation(summary = "Обновить задачу")
    public Task update(@RequestBody final TaskUpdateDTO taskUpdateDTO) {
        return taskService.update(taskUpdateDTO);
    }

    @GetMapping
    @Operation(summary = "Поиск задач")
    public List<Task> findAll(
            @RequestParam(required = false) final Optional<TaskStatus> status,
            @RequestParam(required = false) final Optional<Long> assignee
    ) {
        return taskService.getAll(status, assignee);
    }
}
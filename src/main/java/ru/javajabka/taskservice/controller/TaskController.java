package ru.javajabka.taskservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.javajabka.taskservice.dto.TaskUpdateDTO;
import ru.javajabka.taskservice.dto.TaskRequestDTO;
import ru.javajabka.taskservice.model.TaskResponse;
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
    public TaskResponse create(@RequestBody final TaskRequestDTO taskRequest) {
        return taskService.create(taskRequest);
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable final Long id) {
        return taskService.getById(id);
    }

    @PatchMapping
    public TaskResponse update(@RequestBody final TaskUpdateDTO taskUpdateDTO) {
        return taskService.update(taskUpdateDTO);
    }

    @GetMapping
    public List<TaskResponse> getAll(
            @RequestParam(required = false) final Optional<TaskStatus> status,
            @RequestParam(required = false) final Optional<Long> assignee
    ) {
        return taskService.getAll(status, assignee);
    }
}
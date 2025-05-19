package ru.javajabka.taskservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.javajabka.taskservice.model.TaskReport;
import ru.javajabka.taskservice.model.TaskReportRequestDTO;
import ru.javajabka.taskservice.service.ReportService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/report")
@Tag(name = "Отчёт")
public class ReportController {

    private final ReportService reportService;;

    @GetMapping
    @Operation(summary = "Отчёт по задачам команды за период")
    public TaskReport getReport(final TaskReportRequestDTO taskReportRequestDTO) {
        return reportService.getReport(taskReportRequestDTO);
    }
}
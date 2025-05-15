package ru.javajabka.taskservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.javajabka.taskservice.model.TaskReport;
import ru.javajabka.taskservice.model.TaskReportRequestDTO;
import ru.javajabka.taskservice.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TaskService taskService;
    private final TeamService teamService;
    private final UserService userService;

    public TaskReport getReport(TaskReportRequestDTO taskReportRequestDTO) {
        validate(taskReportRequestDTO);
        Set<Long> teamMembers = new HashSet<>(teamService.getMembersOfTeam(taskReportRequestDTO.getTeamId()));
        Long taskCount = taskService.getTasksCountByPeriod(teamMembers, taskReportRequestDTO.getStartDate(), taskReportRequestDTO.getEndDate());
        Map<String, Long> taskPerStatus = taskService.getTasksPerStatus(teamMembers, taskReportRequestDTO.getStartDate(), taskReportRequestDTO.getEndDate());
        Map<Long, Long> mostActiveUsers = taskService.getMostAciveMembers(teamMembers, taskReportRequestDTO.getStartDate(), taskReportRequestDTO.getEndDate());
        List<User> users = userService.checkAndGetUsers(new ArrayList<>(mostActiveUsers.keySet()));
        Long averageTaskDuration = taskService.getAvgTaskDuration(teamMembers, taskReportRequestDTO.getStartDate(), taskReportRequestDTO.getEndDate());
        Map<String, Long> activeMembers = new HashMap<>();

        for (Map.Entry<Long, Long> entry : mostActiveUsers.entrySet()) {
            activeMembers.put(users.stream().filter(u -> u.getId().equals(entry.getKey())).findFirst().get().getUserName(), entry.getValue());
        }

        return TaskReport.builder()
                .teamTasks(taskCount)
                .tasksWithStatus(taskPerStatus)
                .mostActiveUser(activeMembers)
                .averageTaskCompleteTime(averageTaskDuration)
                .build();
    }

    private void validate(TaskReportRequestDTO taskReportRequestDTO) {
        if (taskReportRequestDTO == null) {
            throw new IllegalArgumentException("Введите данные для получения отчёта");
        }

        if (taskReportRequestDTO.getTeamId() == null || taskReportRequestDTO.getTeamId() < 1) {
            throw new IllegalArgumentException("Введите корректный id для команды");
        }

        if (taskReportRequestDTO.getStartDate() == null) {
            throw new IllegalArgumentException("Введите корректную дату старта");
        }

        if (taskReportRequestDTO.getEndDate() == null) {
            throw new IllegalArgumentException("Введите корректную дату окончания");
        }
    }
}

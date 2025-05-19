package ru.javajabka.taskservice;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.javajabka.taskservice.exception.BadRequestException;
import ru.javajabka.taskservice.model.TaskReport;
import ru.javajabka.taskservice.model.TaskReportRequestDTO;
import ru.javajabka.taskservice.model.User;
import ru.javajabka.taskservice.service.ReportService;
import ru.javajabka.taskservice.service.TaskService;
import ru.javajabka.taskservice.service.TeamService;
import ru.javajabka.taskservice.service.UserService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock
    private TeamService teamService;

    @Mock
    private TaskService taskService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReportService reportService;

    @Test
    public void createValidReport() {
        TaskReportRequestDTO taskReportRequestDTO = TaskReportRequestDTO.builder()
                        .teamId(1L)
                .startDate(LocalDate.of(2025, 05, 05))
                .endDate(LocalDate.of(2025, 05, 15))
                .build();

        Mockito.when(teamService.getMembersOfTeam(taskReportRequestDTO.getTeamId())).thenReturn(List.of(1L, 2L, 3L));
        Set<Long> teamMembers = new HashSet<>(teamService.getMembersOfTeam(1L));
        Mockito.when(taskService.getTasksCountByPeriod(teamMembers, taskReportRequestDTO.getStartDate(), taskReportRequestDTO.getEndDate())).thenReturn(10L);
        Mockito.when(taskService.getTasksPerStatus(teamMembers, taskReportRequestDTO.getStartDate(), taskReportRequestDTO.getEndDate())).thenReturn(buidTaskWithStatus());
        Mockito.when(taskService.getMostAciveMembers(teamMembers, taskReportRequestDTO.getStartDate(), taskReportRequestDTO.getEndDate())).thenReturn(buildMostActiveUsers());
        Map<Long, Long> mostActiveUsers = taskService.getMostAciveMembers(teamMembers, taskReportRequestDTO.getStartDate(), taskReportRequestDTO.getEndDate());
        Mockito.when(userService.checkAndGetUsers(new ArrayList<>(mostActiveUsers.keySet()))).thenReturn(buildUsers());
        Mockito.when(taskService.getAvgTaskDuration(teamMembers, taskReportRequestDTO.getStartDate(), taskReportRequestDTO.getEndDate())).thenReturn(350L);

        TaskReport taskReport = reportService.getReport(taskReportRequestDTO);
        Assertions.assertEquals(taskReport, buildTaskReport());
    }

    @Test
    public void errorCreateReport_whenTaskIdIncorrect() {
        TaskReportRequestDTO taskReportRequestDTO = TaskReportRequestDTO.builder().teamId(0L).build();
        final BadRequestException exception = Assertions.assertThrows(
                BadRequestException.class,
                () -> reportService.getReport(taskReportRequestDTO)
        );
        Assertions.assertEquals("Введите id для команды больше нуля", exception.getMessage());
    }

    private Map<String, Long> buidTaskWithStatus() {
        Map<String, Long> tasks = new HashMap<>();
        tasks.put("TO_DO", 4L);
        tasks.put("DONE", 6L);
        return tasks;
    }

    private List<User> buildUsers() {
        List<User> users = new ArrayList<>();

        users.add(
                User.builder()
                        .id(1L)
                        .userName("User1")
                        .build()
        );

        users.add(
                User.builder()
                        .id(2L)
                        .userName("User1")
                        .build()
        );

        users.add(
                User.builder()
                        .id(3L)
                        .userName("User1")
                        .build()
        );

        return users;
    }

    private Map<Long, Long> buildMostActiveUsers() {
        Map<Long, Long> activeUsers = new HashMap<>();
        activeUsers.put(1L, 3L);
        return activeUsers;
    }

    private TaskReport buildTaskReport() {
        Map<String, Long> mostActiveUsers = new HashMap<>();
        mostActiveUsers.put("User1", 3L);

        return TaskReport.builder()
                .teamTasks(10L)
                .tasksWithStatus(buidTaskWithStatus())
                .mostActiveUser(mostActiveUsers)
                .averageTaskCompleteTime(350L)
                .build();
    }
}
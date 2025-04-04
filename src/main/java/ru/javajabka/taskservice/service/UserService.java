package ru.javajabka.taskservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.javajabka.taskservice.exception.BadRequestException;
import ru.javajabka.taskservice.model.UserResponse;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final RestTemplate restTemplate;

    public List<UserResponse> checkUserId(final List<Long> userIds) {
        String stringUserIds = userIds.stream().map(Objects::toString).collect(Collectors.joining(","));
        List<UserResponse> users = restTemplate.getForObject("http://localhost:8081/api/v1/user?ids={ids}", List.class, stringUserIds);

        if (users == null || users.size() != userIds.size()) {
            throw new BadRequestException("Один или несколько полдьзователей не найдены");
        }

        return users;
    }
}
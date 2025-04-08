package ru.javajabka.taskservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.javajabka.taskservice.exception.BadRequestException;
import ru.javajabka.taskservice.model.User;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final RestTemplate restTemplate;

    @Value("${url.service.user}")
    private String userServiceUrl;

    public void checkUserId(final List<Long> userIds) {
        String url = UriComponentsBuilder
                .fromUriString(userServiceUrl + "/api/v1/user?ids[]={ids}")
                .queryParam("ids", userIds)
                .toUriString();

        ResponseEntity<List<User>> responseEntity =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        userIds.stream().filter(e -> !responseEntity.getBody().stream()
                .map(User::getId).toList()
                .contains(e)).findFirst()
                .ifPresent(
                    (id) -> {
                        throw new BadRequestException(String.format("Пользователь с id %d не найден", id));
                    }
        );
    }
}
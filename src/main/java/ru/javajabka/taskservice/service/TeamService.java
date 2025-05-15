package ru.javajabka.taskservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final RestTemplate restTemplate;

    @Value("${url.service.team}")
    private String teamServiceUrl;

    public List<Long> getMembersOfTeam(Long teamId) {
        String url = UriComponentsBuilder
                .fromUriString(teamServiceUrl + "/api/v1/team")
                .queryParam("id", teamId)
                .encode()
                .build()
                .toString();

        ResponseEntity<List<Long>> responseEntity =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        return responseEntity.getBody();
    }
}

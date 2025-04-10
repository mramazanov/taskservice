package ru.javajabka.taskservice.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class User {
    private final Long id;
    private final String userName;
}
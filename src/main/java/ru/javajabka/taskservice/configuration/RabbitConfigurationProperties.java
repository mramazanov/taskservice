package ru.javajabka.taskservice.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("app.rabbitmq")
public class RabbitConfigurationProperties {

    private String queue;
    private String exchange;
}
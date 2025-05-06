package ru.javajabka.taskservice.listener;

import ru.javajabka.taskservice.configuration.RabbitConfigurationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import ru.javajabka.taskservice.model.EventDTO;

import java.util.List;

@RequiredArgsConstructor
@Component
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitConfigurationProperties properties;

    public void send(List<EventDTO> events) {
        rabbitTemplate.convertAndSend(properties.getExchange(), properties.getQueue(), events);
    }
}
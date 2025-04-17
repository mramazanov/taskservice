package ru.javajabka.taskservice.listener;

import ru.javajabka.taskservice.configuration.RabbitConfigurationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import ru.javajabka.taskservice.model.EventDTO;

@RequiredArgsConstructor
@Component
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitConfigurationProperties properties;

    public void send(EventDTO event) {
        rabbitTemplate.convertAndSend(properties.getExchange(), properties.getQueue(), event);
    }
}
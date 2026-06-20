package com.healthconnect.notification;

import com.healthconnect.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    public EventPublisher(RabbitTemplate rabbitTemplate) { this.rabbitTemplate = rabbitTemplate; }

    public void publish(AppEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "event." + event.type(), event);
    }
}

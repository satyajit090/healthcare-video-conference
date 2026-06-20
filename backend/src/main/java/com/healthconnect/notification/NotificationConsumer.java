package com.healthconnect.notification;

import com.healthconnect.config.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Consumes events off RabbitMQ and (1) persists a notification and
 * (2) pushes a live notification to the recipient over WebSocket.
 */
@Component
public class NotificationConsumer {

    private final NotificationRepository repository;
    private final SimpMessagingTemplate messaging;

    public NotificationConsumer(NotificationRepository repository, SimpMessagingTemplate messaging) {
        this.repository = repository;
        this.messaging = messaging;
    }

    @RabbitListener(queues = RabbitConfig.NOTIFICATION_QUEUE)
    public void handle(AppEvent event) {
        if (event.recipientUserId() != null) {
            Notification n = repository.save(Notification.builder()
                    .recipientUserId(event.recipientUserId())
                    .type(event.type())
                    .title(event.title())
                    .message(event.message())
                    .build());
            messaging.convertAndSend(
                    "/topic/notifications/" + event.recipientUserId(), n);
        }
    }
}

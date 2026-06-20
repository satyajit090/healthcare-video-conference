package com.healthconnect.call;

import com.healthconnect.call.CallDtos.ChatMessage;
import com.healthconnect.call.CallDtos.SignalMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;

/**
 * Relays WebRTC signaling (SDP offers/answers and ICE candidates) and in-call
 * chat between the two peers of a room. The server only forwards messages; the
 * actual media flows peer-to-peer (open-source WebRTC, no third-party provider).
 */
@Controller
public class SignalController {

    private final SimpMessagingTemplate messaging;
    public SignalController(SimpMessagingTemplate messaging) { this.messaging = messaging; }

    // client sends to /app/signal/{roomId}; everyone subscribed to /topic/room/{roomId} receives it
    @MessageMapping("/signal/{roomId}")
    public void signal(@DestinationVariable String roomId, @Payload SignalMessage message) {
        messaging.convertAndSend("/topic/room/" + roomId, message);
    }

    @MessageMapping("/chat/{roomId}")
    public void chat(@DestinationVariable String roomId, @Payload ChatMessage message) {
        ChatMessage stamped = new ChatMessage(
                message.fromUserId(), message.fromName(), message.text(), Instant.now());
        messaging.convertAndSend("/topic/chat/" + roomId, stamped);
    }
}

package org.sarthak.echowavebackend.websocket;

import org.sarthak.echowavebackend.service.PresenceService;
import org.sarthak.echowavebackend.websocket.dto.PresenceEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.Set;

@Component
public class PresenceEventListener {

    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    public PresenceEventListener(PresenceService presenceService,
                                 SimpMessagingTemplate messagingTemplate) {
        this.presenceService = presenceService;
        this.messagingTemplate = messagingTemplate;
    }

    /* -------------------- JOIN -------------------- */

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(event.getMessage());

        if (!StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return;
        }

        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith("/topic/channel/")) {
            return;
        }

        String channelId =
                destination.substring("/topic/channel/".length());

        Principal principal = accessor.getUser();
        if (principal == null) return;

        String sessionId = accessor.getSessionId();
        String username = principal.getName();

        presenceService.userJoined(channelId, sessionId, username);

        messagingTemplate.convertAndSend(
                "/topic/channel/" + channelId,
                new PresenceEvent(
                        "JOIN",
                        username,
                        presenceService.getUsers(channelId)
                )
        );
    }

    /* -------------------- EXPLICIT LEAVE -------------------- */

    @MessageMapping("/channel/{channelId}/leave")
    public void handleLeave(@DestinationVariable String channelId,
                            org.springframework.messaging.Message<?> message) {

        var accessor =
                org.springframework.messaging.simp.SimpMessageHeaderAccessor
                        .wrap(message);

        Principal principal = accessor.getUser();
        if (principal == null) return;

        String sessionId = accessor.getSessionId();

        presenceService.userLeft(channelId, sessionId);

        messagingTemplate.convertAndSend(
                "/topic/channel/" + channelId,
                new PresenceEvent(
                        "LEAVE",
                        principal.getName(),
                        presenceService.getUsers(channelId)
                )
        );
    }

    /* -------------------- DISCONNECT -------------------- */

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {

        String sessionId = event.getSessionId();

        Set<String> channels =
                presenceService.removeSession(sessionId);

        for (String channelId : channels) {
            messagingTemplate.convertAndSend(
                    "/topic/channel/" + channelId,
                    new PresenceEvent(
                            "LEAVE",
                            null,
                            presenceService.getUsers(channelId)
                    )
            );
        }
    }
}

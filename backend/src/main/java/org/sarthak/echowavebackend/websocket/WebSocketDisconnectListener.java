package org.sarthak.echowavebackend.websocket;

import org.sarthak.echowavebackend.service.SpeakerService;
import org.sarthak.echowavebackend.websocket.dto.SpeakerEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
public class WebSocketDisconnectListener {

    private final SpeakerService speakerService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketDisconnectListener(
            SpeakerService speakerService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.speakerService = speakerService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(event.getMessage());

        Principal principal = accessor.getUser();
        if (principal == null) return;

        String username = principal.getName();

        // 1️⃣ Force release speaker if needed
        String channelId = speakerService.getChannelOfSpeaker(username);
        if (channelId != null) {
            speakerService.forceReleaseIfSpeaker(username);

            // 2️⃣ Notify channel that speaker left
            messagingTemplate.convertAndSend(
                    "/topic/channel/" + channelId,
                    new SpeakerEvent("SPEAKER_RELEASED", username)
            );
        }

        // (Optional) Presence LEAVE event can also go here
    }
}
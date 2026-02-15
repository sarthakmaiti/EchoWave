package org.sarthak.echowavebackend.controller;

import org.sarthak.echowavebackend.service.SpeakerService;
import org.sarthak.echowavebackend.websocket.dto.SpeakerEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class SpeakerController {

    private final SpeakerService speakerService;
    private final SimpMessagingTemplate messagingTemplate;

    public SpeakerController(SpeakerService speakerService,
                             SimpMessagingTemplate messagingTemplate) {
        this.speakerService = speakerService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/channel/{channelId}/speaker/request")
    public void requestSpeaker(@DestinationVariable String channelId,
                               Message<?> message) {

        Principal principal =
                SimpMessageHeaderAccessor.wrap(message).getUser();

        if (principal == null) {
            throw new IllegalStateException("Unauthenticated message");
        }

        String username = principal.getName();

        boolean granted =
                speakerService.requestSpeaker(channelId, username);

        if (granted) {
            messagingTemplate.convertAndSend(
                    "/topic/channel/" + channelId,
                    new SpeakerEvent("SPEAKER_GRANTED", username)
            );
        } else {
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/speaker-denied",
                    new SpeakerEvent("SPEAKER_DENIED", username)
            );
        }
    }

    @MessageMapping("/channel/{channelId}/speaker/release")
    public void releaseSpeaker(@DestinationVariable String channelId,
                               Message<?> message) {

        Principal principal =
                SimpMessageHeaderAccessor.wrap(message).getUser();

        if (principal == null) return;

        String username = principal.getName();

        if (speakerService.releaseSpeaker(channelId, username)) {
            messagingTemplate.convertAndSend(
                    "/topic/channel/" + channelId,
                    new SpeakerEvent("SPEAKER_RELEASED", username)
            );
        }
    }
}

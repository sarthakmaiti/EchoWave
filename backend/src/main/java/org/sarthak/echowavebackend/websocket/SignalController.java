package org.sarthak.echowavebackend.websocket;

import org.sarthak.echowavebackend.service.SpeakerService;
import org.sarthak.echowavebackend.websocket.dto.IceCandidateMessage;
import org.sarthak.echowavebackend.websocket.dto.SdpMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class SignalController {

    private final SimpMessagingTemplate messagingTemplate;
    private final SpeakerService speakerService;

    public SignalController(SimpMessagingTemplate messagingTemplate,
                            SpeakerService speakerService) {
        this.messagingTemplate = messagingTemplate;
        this.speakerService = speakerService;
    }

    // ================= SDP =================

    @MessageMapping("/webrtc/sdp")
    public void handleSdp(Message<SdpMessage> message) {

        SimpMessageHeaderAccessor accessor =
                SimpMessageHeaderAccessor.wrap(message);

        Principal principal = accessor.getUser();
        if (principal == null) return;

        String sender = principal.getName();
        SdpMessage payload = message.getPayload();

        // Security check: sender spoofing
        if (!sender.equals(payload.getFrom())) {
            return;
        }

        // OFFER rule: only current speaker can send offer
        if ("offer".equalsIgnoreCase(payload.getType())) {
            String channelId = speakerService.getChannelOfSpeaker(sender);
            String currentSpeaker =
                    speakerService.getCurrentSpeaker(channelId);

            if (!sender.equals(currentSpeaker)) {
                return;
            }
        }

        // Point-to-point routing
        messagingTemplate.convertAndSendToUser(
                payload.getTo(),
                "/queue/webrtc",
                payload
        );
    }

    // ================= ICE =================

    @MessageMapping("/webrtc/ice")
    public void handleIce(Message<IceCandidateMessage> message) {

        SimpMessageHeaderAccessor accessor =
                SimpMessageHeaderAccessor.wrap(message);

        Principal principal = accessor.getUser();
        if (principal == null) return;

        String sender = principal.getName();
        IceCandidateMessage payload = message.getPayload();

        // Security check
        if (!sender.equals(payload.getFrom())) {
            return;
        }

        messagingTemplate.convertAndSendToUser(
                payload.getTo(),
                "/queue/webrtc",
                payload
        );
    }
}
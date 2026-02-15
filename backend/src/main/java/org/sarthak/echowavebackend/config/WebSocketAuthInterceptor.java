package org.sarthak.echowavebackend.config;

import org.sarthak.echowavebackend.websocket.security.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public WebSocketAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (token != null) {
                try {
                    String username = jwtService.extractUsername(token);
                    if (username != null) {
                        Principal principal = new UsernamePasswordAuthenticationToken(
                                username, null, List.of());
                        accessor.setUser(principal);
                    }
                } catch (Exception e) {
                    // Token invalid or expired
                }
            }
        }
        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // Try STOMP header first (native WebSocket)
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String token = authHeaders.get(0);
            if (token.startsWith("Bearer ")) {
                return token.substring(7);
            }
            return token;
        }

        // Fallback: query parameter (SockJS)
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            return (String) sessionAttributes.get("token");
        }

        return null;
    }
}

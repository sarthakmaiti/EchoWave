package org.sarthak.echowavebackend.websocket.dto;

public class SpeakerEvent {
    private String type;
    private String user;

    public SpeakerEvent(String type, String user) {
        this.type = type;
        this.user = user;
    }

    public String getType() {
        return type;
    }

    public String getUser() {
        return user;
    }
}

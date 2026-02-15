package org.sarthak.echowavebackend.websocket.dto;

public class ChannelEvent {
    private String type;
    private String user;

    public ChannelEvent(String type, String user) {
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

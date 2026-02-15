package org.sarthak.echowavebackend.websocket.dto;

import java.util.Set;

public class PresenceEvent {

    private String type;        // JOIN or LEAVE
    private String user;
    private Set<String> users;  // current users

    public PresenceEvent(String type, String user, Set<String> users) {
        this.type = type;
        this.user = user;
        this.users = users;
    }

    public String getType() {
        return type;
    }

    public String getUser() {
        return user;
    }

    public Set<String> getUsers() {
        return users;
    }
}
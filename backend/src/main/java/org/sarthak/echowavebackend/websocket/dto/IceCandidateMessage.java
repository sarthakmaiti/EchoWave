package org.sarthak.echowavebackend.websocket.dto;

public class IceCandidateMessage {
    private String from;
    private String to;       // 👈 REQUIRED
    private Object candidate;

    public IceCandidateMessage(String from, String to, Object candidate) {
        this.from = from;
        this.to = to;
        this.candidate = candidate;
    }

    public String getFrom() { return from; }
    public String getTo() { return to; }
    public Object getCandidate() { return candidate; }
}

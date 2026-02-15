package org.sarthak.echowavebackend.websocket.dto;

public class SdpMessage {
    private String type;   // "offer" | "answer"
    private String from;
    private String to;     // 👈 REQUIRED
    private String sdp;

    public SdpMessage(String type, String from, String to, String sdp) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.sdp = sdp;
    }

    public String getType() { return type; }
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public String getSdp() { return sdp; }
}

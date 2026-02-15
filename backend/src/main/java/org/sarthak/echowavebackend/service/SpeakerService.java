package org.sarthak.echowavebackend.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class SpeakerService {

    private final StringRedisTemplate redis;

    private static final Duration SPEAKER_TTL = Duration.ofMinutes(5);

    public SpeakerService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String speakerKey(String channelId) {
        return "channel:" + channelId + ":speaker";
    }

    private String userChannelKey(String user) {
        return "user:" + user + ":channel";
    }

    /* ================= SPEAKER LOCK ================= */

    public boolean requestSpeaker(String channelId, String user) {

        Boolean ok = redis.opsForValue()
                .setIfAbsent(speakerKey(channelId), user, SPEAKER_TTL);

        if (Boolean.TRUE.equals(ok)) {
            redis.opsForValue().set(userChannelKey(user), channelId);
            return true;
        }
        return false;
    }

    public boolean releaseSpeaker(String channelId, String user) {

        String current = redis.opsForValue().get(speakerKey(channelId));

        if (user.equals(current)) {
            redis.delete(speakerKey(channelId));
            redis.delete(userChannelKey(user));
            return true;
        }
        return false;
    }

    /* ================= LOOKUPS ================= */

    public String getCurrentSpeaker(String channelId) {
        return redis.opsForValue().get(speakerKey(channelId));
    }

    public String getChannelOfSpeaker(String user) {
        return redis.opsForValue().get(userChannelKey(user));
    }

    /* ================= SAFETY ================= */

    public void forceReleaseIfSpeaker(String user) {
        String channelId = getChannelOfSpeaker(user);
        if (channelId != null) {
            redis.delete(speakerKey(channelId));
            redis.delete(userChannelKey(user));
        }
    }

    /* ================= KEEP ALIVE ================= */

    public void refreshSpeakerTTL(String channelId, String user) {
        String current = redis.opsForValue().get(speakerKey(channelId));
        if (user.equals(current)) {
            redis.expire(speakerKey(channelId), SPEAKER_TTL);
        }
    }
}
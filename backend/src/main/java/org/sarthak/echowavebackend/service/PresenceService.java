package org.sarthak.echowavebackend.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class PresenceService {

    private final StringRedisTemplate redis;

    public PresenceService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /* -------------------- Keys -------------------- */

    private String sessionUserKey(String sessionId) {
        return "session:" + sessionId + ":user";
    }

    private String sessionChannelsKey(String sessionId) {
        return "session:" + sessionId + ":channels";
    }

    private String channelSessionsKey(String channelId) {
        return "channel:" + channelId + ":sessions";
    }

    /* -------------------- JOIN -------------------- */

    public void userJoined(String channelId,
                           String sessionId,
                           String username) {

        redis.opsForValue()
                .set(sessionUserKey(sessionId), username);

        redis.opsForSet()
                .add(sessionChannelsKey(sessionId), channelId);

        redis.opsForSet()
                .add(channelSessionsKey(channelId), sessionId);
    }

    /* -------------------- LEAVE (explicit) -------------------- */

    public void userLeft(String channelId, String sessionId) {

        redis.opsForSet()
                .remove(sessionChannelsKey(sessionId), channelId);

        redis.opsForSet()
                .remove(channelSessionsKey(channelId), sessionId);

        cleanupChannel(channelId);
        cleanupSession(sessionId);
    }

    /* -------------------- DISCONNECT -------------------- */

    public Set<String> removeSession(String sessionId) {

        Set<String> channels =
                redis.opsForSet()
                        .members(sessionChannelsKey(sessionId));

        if (channels == null) {
            return Set.of();
        }

        for (String channelId : channels) {
            redis.opsForSet()
                    .remove(channelSessionsKey(channelId), sessionId);
            cleanupChannel(channelId);
        }

        redis.delete(sessionChannelsKey(sessionId));
        redis.delete(sessionUserKey(sessionId));

        return channels;
    }

    /* -------------------- QUERY -------------------- */

    public Set<String> getUsers(String channelId) {

        Set<String> sessions =
                redis.opsForSet()
                        .members(channelSessionsKey(channelId));

        if (sessions == null || sessions.isEmpty()) {
            return Set.of();
        }

        Set<String> users = new HashSet<>();

        for (String sessionId : sessions) {
            String user =
                    redis.opsForValue()
                            .get(sessionUserKey(sessionId));
            if (user != null) {
                users.add(user);
            }
        }

        return users;
    }

    /* -------------------- Cleanup helpers -------------------- */

    private void cleanupChannel(String channelId) {
        Long size =
                redis.opsForSet()
                        .size(channelSessionsKey(channelId));
        if (size != null && size == 0) {
            redis.delete(channelSessionsKey(channelId));
        }
    }

    private void cleanupSession(String sessionId) {
        Long size =
                redis.opsForSet()
                        .size(sessionChannelsKey(sessionId));
        if (size != null && size == 0) {
            redis.delete(sessionChannelsKey(sessionId));
            redis.delete(sessionUserKey(sessionId));
        }
    }
}

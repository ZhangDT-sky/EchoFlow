package org.echoflow.core.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.echoflow.core.chat.Message;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;

public class RedisChatMemory implements ChatMemory {

    private static final String KEY_PREFIX = "echoflow:chat:memory:";
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisChatMemory(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void addMessage(String sessionId, Message message) {
        String key = KEY_PREFIX + sessionId;
        try {
            String jsonEntry = objectMapper.writeValueAsString(message);
            // 将最新消息追加到 List 尾部，并设置个过期时间比如 7 天
            redisTemplate.opsForList().rightPush(key, jsonEntry);
            redisTemplate.expire(key, java.time.Duration.ofDays(7));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("RedisChatMemory 序列化失败", e);
        }
    }

    @Override
    public List<Message> getMessages(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);
        List<Message> messages = new ArrayList<>();
        if (jsonList == null || jsonList.isEmpty()) {
            return messages;
        }
        for (String json : jsonList) {
            try {
                messages.add(objectMapper.readValue(json, Message.class));
            } catch (JsonProcessingException e) {

            }
        }
        return messages;
    }

    @Override
    public void clear(String sessionId) {
        redisTemplate.delete(KEY_PREFIX + sessionId);
    }
}

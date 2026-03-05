package org.echoflow.core.context;

import org.echoflow.core.chat.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryChatMemory implements ChatMemory{

    private static final Logger log = LoggerFactory.getLogger(InMemoryChatMemory.class);
    private final Map<String,List<Message>> store = new ConcurrentHashMap<>();
    private static final int MAX_MESSAGES_PER_SESSION = 1000;

    @Override
    public void addMessage(String sessionId, Message message) {
        List<Message> messages = store.computeIfAbsent(sessionId, k->new ArrayList<>());
        synchronized (message){
            messages.add(message);
            if (messages.size()>MAX_MESSAGES_PER_SESSION){
                messages.remove(0);
                log.warn("[EchoFlow InMemory] Session [{}] 消息数超过上限 {}，已淘汰最早一条消息", sessionId, MAX_MESSAGES_PER_SESSION);
            }
        }
    }

    @Override
    public List<Message> getMessages(String sessionId) {
        List<Message> messages = store.get(sessionId);
        return messages != null ? new ArrayList<>(messages) : new ArrayList<>();
    }

    @Override
    public void clear(String sessionId) {
        store.remove(sessionId);
    }
}

package org.echoflow.core.context;

import org.echoflow.core.chat.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryChatMemory implements ChatMemory{

    private final Map<String,List<Message>> store = new ConcurrentHashMap<>();


    @Override
    public void addMessage(String sessionId, Message message) {
        store.computeIfAbsent(sessionId, k->new ArrayList<>()).add(message);
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

package org.echoflow.core.context;

import org.echoflow.core.chat.Message;

import java.util.List;

public interface ChatMemory {
    void addMessage(String sessionId, Message message);
    List<Message> getMessages(String sessionId);
    void clear(String sessionId);
}

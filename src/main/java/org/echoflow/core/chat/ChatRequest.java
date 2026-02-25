package org.echoflow.core.chat;

import java.util.List;

public class ChatRequest {
    private String model;
    private List<Message> messages;
    private Boolean stream;
    private Double temperature;

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
    public Boolean getStream() { return stream; }
    public void setStream(Boolean stream) { this.stream = stream; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public static ChatRequest of(String model, List<Message> messages){
        ChatRequest request = new ChatRequest();
        request.setModel(model);
        request.setMessages(messages);
        request.setStream(false);
        return request;
    }
}

package org.echoflow.core.chat;

import java.util.List;
import java.util.Map;

public class ChatRequest {
    private String model;
    private List<Message> messages;
    private Boolean stream;
    private Double temperature;

    // 发给大模型的工具列表
    public List<Tool> tools;

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
    public Boolean getStream() { return stream; }
    public void setStream(Boolean stream) { this.stream = stream; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public List<Tool> getTools() {return tools;}
    public void setTools(List<Tool> tools) {this.tools = tools;}

    public static ChatRequest of(String model, List<Message> messages){
        ChatRequest request = new ChatRequest();
        request.setModel(model);
        request.setMessages(messages);
        request.setStream(false);
        return request;
    }

    // 工具的实体定义结构
    public static class Tool {
        private String type = "function"; // 固定为 function
        private Function function;
        public Tool() {}
        public Tool(Function function) { this.function = function; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Function getFunction() { return function; }
        public void setFunction(Function function) { this.function = function; }
    }

    // 描述具体函数的结构
    public static class Function {
        private String name;
        private String description;

        private Map<String,Object> parameters;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }
}

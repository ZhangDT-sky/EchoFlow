package org.echoflow.core.chat;

import java.util.List;

public class Message {
    private String role;
    private String content;

    // 【新增】当角色是 assistant 时，如果它想调工具，这里会有值，content可能为null
    private List<ToolCall> tool_calls;

    // 【新增】当角色是 tool 时，它必须带上工具调用的唯一ID，作为反馈
    private String tool_call_id;

    public Message(){}

    public Message(String role, String content){
        this.role = role;
        this.content = content;
    }

    public static Message user(String content){
        return new Message("user", content);
    }

    public static Message system(String content){
        return new Message("system", content);
    }

    public static Message assistant(String content){
        return new Message("assistant", content);
    }

    public static Message tool(String toolCallId,String content) {
        Message msg = new Message("tool",content);
        msg.setTool_call_id(toolCallId);
        return msg;
    }

    public String getRole(){ return role; }
    public void setRole(String role){ this.role = role; }
    public String getContent(){ return content;}
    public void setContent(String content){ this.content = content; }
    public List<ToolCall> getTool_calls() { return tool_calls; }
    public void setTool_calls(List<ToolCall> tool_calls) { this.tool_calls = tool_calls; }
    public String getTool_call_id() { return tool_call_id; }
    public void setTool_call_id(String tool_call_id) { this.tool_call_id = tool_call_id; }

    // 【新增】模型要求调用工具时，底层的结构封装
    public static class ToolCall {
        private String id;
        private String type; // 一般是 function
        private CallFunction function;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public CallFunction getFunction() { return function; }
        public void setFunction(CallFunction function) { this.function = function; }
    }

    // 【新增】具体要求调用的函数名以及 JSON 入参字符串
    public static class CallFunction {
        private String name;
        private String arguments;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getArguments() { return arguments; }
        public void setArguments(String arguments) { this.arguments = arguments; }
    }

}

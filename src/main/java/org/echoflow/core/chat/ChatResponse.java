package org.echoflow.core.chat;

public class ChatResponse {
    private String content;
    private Usage usage;

    // 如果是流式返回的单个 Chunk 内容
    private String delta;

    public ChatResponse(){}

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Usage getUsage() { return usage; }
    public void setUsage(Usage usage) { this.usage = usage; }
    public String getDelta() { return delta; }
    public void setDelta(String delta) { this.delta = delta; }

    public static class Usage{
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;

        public int getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(int promptTokens) {
            this.promptTokens = promptTokens;
        }

        public int getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(int completionTokens) {
            this.completionTokens = completionTokens;
        }

        public int getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(int totalTokens) {
            this.totalTokens = totalTokens;
        }
    }
}

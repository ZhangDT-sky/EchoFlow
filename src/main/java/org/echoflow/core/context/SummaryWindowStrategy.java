package org.echoflow.core.context;

import org.echoflow.core.chat.ChatRequest;
import org.echoflow.core.chat.ChatResponse;
import org.echoflow.core.chat.Message;
import org.echoflow.core.provider.LLMProvider;
import org.echoflow.utils.TokenUtils;

import java.util.ArrayList;
import java.util.List;

public class SummaryWindowStrategy extends SlidingWindowStrategy{
    private final int maxTokens;
    private final LLMProvider llmProvider;
    private final int keepRecentN;

    public SummaryWindowStrategy(int maxTokens, LLMProvider llmProvider, int keepRecentN) {
        super(maxTokens);
        this.maxTokens = maxTokens;
        this.llmProvider = llmProvider;
        this.keepRecentN = keepRecentN;
    }

    @Override
    public List<Message> apply(List<Message> originalMessage){
        if (originalMessage == null || originalMessage.isEmpty()){
            return new ArrayList<>();
        }

        List<Message> result = new ArrayList<>(originalMessage);

        if (TokenUtils.estimateTokens(result)<=maxTokens){
            return result;
        }
        System.out.println("[EchoFlow Memory] 警告：Token 超限，触发后台大模型无损压缩摘要流程...");

        // 抽取最古老的 System Message 免遭破坏
        Message systemMsg = null;
        if (!result.isEmpty() && "system".equals(result.get(0).getRole())){
            systemMsg = result.remove(0);
        }

        // 抽取最近最活跃的 N 条记录免于被摘要
        List<Message> recentMsgs = new ArrayList<>();
        int toKeep = Math.min(keepRecentN,result.size());

        for (int i=0;i<toKeep;i++){
            // 一直从截断队伍尾部摘取，放入 recentMsgs 的头部以保证顺序
            recentMsgs.add(0, result.remove(result.size() - 1));
        }
        if(!result.isEmpty()){
            StringBuilder conversationToSummarize = new StringBuilder();
            for(Message msg : result){
                if(msg.getContent() != null) {
                    conversationToSummarize.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
                }
            }
            String prompt = "请用精炼的语言（不超过300字）准确总结以下多轮早期对话中用户的核心诉求、已提供的信息状态。不需要回复别的废话，直接给出总结要点。原对话如下：\\n"+conversationToSummarize.toString();
            List<Message> summaryRequestMessages = new ArrayList<>();
            summaryRequestMessages.add(Message.user(prompt));

            // 构建请求要求 AI 浓缩
            ChatRequest summaryRequest = ChatRequest.of(null,summaryRequestMessages);
            ChatResponse summaryResp = llmProvider.chat(summaryRequest);

            // 将压缩后的摘要变成一条新的 system
            Message summaryMessage = Message.system("这里有一份早期的对话记忆概要供你参考"+summaryResp.getContent());

            result.clear();
            if (systemMsg != null){
                result.add(systemMsg);
            }
            result.add(summaryMessage);
            result.addAll(recentMsgs);
            System.out.println("[EchoFlow Memory] 🎈 摘要完毕！");

        } else {
            // 极端情况：由于参数设置得不好，导致最近活跃的N条自身就已经超长超限，那就退回到暴力截取
            result.clear();
            if (systemMsg!=null){
                result.add(systemMsg);
            }
            result.addAll(recentMsgs);
            result = super.apply(result);
        }
        return result;
    }

}

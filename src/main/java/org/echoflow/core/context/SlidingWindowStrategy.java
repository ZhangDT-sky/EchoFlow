package org.echoflow.core.context;

import org.echoflow.core.chat.Message;
import org.echoflow.utils.TokenUtils;

import java.util.ArrayList;
import java.util.List;

public class SlidingWindowStrategy {

    private final int maxTokens;

    public SlidingWindowStrategy(int maxTokens) {
        this.maxTokens = maxTokens;
    }
    /**
     * 执行滑动窗口策略，截取掉最老的用户对话，直到不超过 maxTokens
     */
    public List<Message> apply(List<Message> originalMessages){
        if(originalMessages == null||originalMessages.isEmpty()){
            return new ArrayList<>();
        }

        List<Message> result = new ArrayList<>(originalMessages);

        while (result.size()>1 && TokenUtils.estimateTokens(result)>maxTokens){
            boolean removed = false;
            for (int i=0; i<result.size(); i++){
                if(!"system".equals(result.get(i).getRole())){
                    result.remove(i);
                    removed = true;
                    break;
                }
            }
            if (!removed) break;
        }
        return result;
    }
}

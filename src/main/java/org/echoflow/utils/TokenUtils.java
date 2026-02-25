package org.echoflow.utils;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import org.echoflow.core.chat.Message;

public class TokenUtils {
    private static final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

    private static final Encoding encoding = registry.getEncoding(EncodingType.CL100K_BASE);

    /**
     * 计算单段文本的 Token 数量
     */
    public static int estimateTokens(String text){
        if (text==null||text.isEmpty()){
            return 0;
        }
        return encoding.countTokens(text);
    }

    /**
     * 计算 Message 列表的总预估量 (粗略包含角色附加信息)
     */
    public static int estimateTokens(Iterable<Message> messages){
        int tokens = 0;
        for (Message msg:messages){
            tokens += estimateTokens(msg.getContent());
            tokens += 4;
        }
        tokens += 3;
        return tokens;
    }

}

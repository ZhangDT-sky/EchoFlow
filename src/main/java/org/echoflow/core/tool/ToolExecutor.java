package org.echoflow.core.tool;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 统一的大模型工具执行器接口。
 * 屏蔽了底层实现差异，不管是本地 Java 方法反射，还是远程 MCP Http 调用，均实现此接口。
 */
public interface ToolExecutor {
    Object execute(JsonNode args) throws Exception;
}

package org.example.config;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP 关闭时的兜底 ToolCallbackProvider。
 *
 * <p>背景：{@code ChatService} / {@code ChatController} 用 {@code @Autowired(required = true)}
 * 注入 {@link ToolCallbackProvider}，该 Bean 由 spring-ai-starter-mcp-client 的自动配置提供。
 * 一旦按"纯本地 mock"跑法把 {@code spring.ai.mcp.client.enabled} 设为 false，自动配置不再注册
 * 该 Bean，应用会以 NoSuchBeanDefinitionException 启动失败。
 *
 * <p>这里在 MCP 显式关闭时补一个空实现，使本地无 MCP 环境也能启动；MCP 打开时本类不生效，
 * 依然由 MCP 自动配置提供真实的 Provider，不存在 Bean 冲突。
 */
@Configuration
@ConditionalOnProperty(name = "spring.ai.mcp.client.enabled", havingValue = "false")
public class McpFallbackConfig {

    @Bean
    public ToolCallbackProvider emptyToolCallbackProvider() {
        return ToolCallbackProvider.from();
    }
}

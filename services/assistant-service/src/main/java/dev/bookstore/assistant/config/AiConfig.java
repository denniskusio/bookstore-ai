package dev.bookstore.assistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class AiConfig {

    /**
     * The one {@link ChatClient} of the service. Spring AI auto-configures only a prototype-scoped builder;
     * building the client once here keeps controllers and services free of builder boilerplate and makes them
     * trivial to test with a mock.
     */
    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}

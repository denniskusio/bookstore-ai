package dev.bookstore.assistant.web;

import dev.bookstore.assistant.web.dto.PingResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Smoke endpoint: sends one fixed prompt to the configured model. It proves the whole chain
 * (gateway -> assistant-service -> Ollama) without any search or RAG logic.
 */
@RestController
@RequestMapping("/assistant")
class PingController {

    static final String PROMPT = "Reply with exactly one word: pong";

    private final ChatClient chatClient;
    private final String model;

    PingController(ChatClient chatClient, @Value("${spring.ai.ollama.chat.model}") String model) {
        this.chatClient = chatClient;
        this.model = model;
    }

    @GetMapping("/ping")
    PingResponse ping() {
        String answer = chatClient.prompt().user(PROMPT).call().content();
        return new PingResponse(model, PROMPT, answer);
    }
}

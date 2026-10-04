package dev.bookstore.assistant.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** Web-layer slice test: the model is a mock, so no Ollama and no database are needed. */
@WebMvcTest(PingController.class)
@TestPropertySource(properties = "spring.ai.ollama.chat.model=test-model")
class PingControllerTest {

    @Autowired
    private MockMvcTester mvc;

    // Deep stubs let us stub the fluent chain chatClient.prompt().user(..).call().content() in one line.
    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Test
    void pingReturnsTheModelsAnswerTogetherWithPromptAndModelName() {
        given(chatClient.prompt().user(PingController.PROMPT).call().content()).willReturn("pong");

        assertThat(mvc.get().uri("/assistant/ping"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.model", model -> model.assertThat().isEqualTo("test-model"))
                .hasPathSatisfying("$.prompt", prompt -> prompt.assertThat().isEqualTo(PingController.PROMPT))
                .hasPathSatisfying("$.answer", answer -> answer.assertThat().isEqualTo("pong"));
    }

    @Test
    void modelFailureBecomesAProblemResponseWithoutLeakingDetails() {
        given(chatClient.prompt().user(PingController.PROMPT).call().content())
                .willThrow(new IllegalStateException("connection refused: http://ollama:11434"));

        assertThat(mvc.get().uri("/assistant/ping"))
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyText().doesNotContain("ollama:11434");
    }
}

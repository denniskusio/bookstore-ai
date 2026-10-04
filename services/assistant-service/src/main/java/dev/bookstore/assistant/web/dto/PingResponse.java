package dev.bookstore.assistant.web.dto;

/** Result of the smoke call to the language model. */
public record PingResponse(String model, String prompt, String answer) {
}

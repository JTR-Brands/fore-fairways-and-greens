package com.fore.game.application.npc;

/**
 * Interface for LLM API clients.
 * Implementations can target OpenAI, Anthropic, Ollama, etc.
 */
public interface LlmClient {

    /**
     * Send a prompt and get a completion.
     */
    String complete(String prompt);

    /**
     * Get the provider name for logging.
     */
    String getProviderName();
}

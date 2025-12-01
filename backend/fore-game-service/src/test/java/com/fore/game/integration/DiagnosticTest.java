package com.fore.game.integration;

import com.fore.game.application.dto.CreateGameRequest;
import com.fore.game.domain.model.enums.Difficulty;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

class DiagnosticTest extends BaseIntegrationTest {

    @Test
    void diagnoseCreateGame() throws Exception {
        CreateGameRequest request = CreateGameRequest.builder()
                .playerId(UUID.randomUUID())
                .playerName("TestPlayer")
                .vsNpc(true)
                .npcDifficulty(Difficulty.MEDIUM)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andReturn();

        System.out.println("=== RESPONSE STATUS: " + result.getResponse().getStatus() + " ===");
        System.out.println("=== RESPONSE BODY ===");
        System.out.println(result.getResponse().getContentAsString());
        System.out.println("=====================");
        
        // If there was an exception, print it
        if (result.getResolvedException() != null) {
            System.out.println("=== EXCEPTION ===");
            result.getResolvedException().printStackTrace();
            System.out.println("=================");
        }
    }
}

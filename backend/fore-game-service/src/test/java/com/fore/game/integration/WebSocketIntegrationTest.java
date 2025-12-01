package com.fore.game.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fore.game.api.websocket.dto.GameUpdateMessage;
import com.fore.game.api.websocket.dto.GameUpdateNotification;
import com.fore.game.application.dto.CreateGameRequest;
import com.fore.game.application.dto.GameStateResponse;
import com.fore.game.application.dto.PlayerActionRequest;
import com.fore.game.domain.model.enums.Difficulty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class WebSocketIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fore_test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setup() {
        // Configure ObjectMapper with Java 8 time support
        ObjectMapper wsObjectMapper = new ObjectMapper();
        wsObjectMapper.registerModule(new JavaTimeModule());
        wsObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
        messageConverter.setObjectMapper(wsObjectMapper);

        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(messageConverter);
    }

    @Test
    void shouldReceiveGameUpdateViaWebSocket() throws Exception {
        // 1. Create a game via REST API
        UUID playerId = UUID.randomUUID();
        CreateGameRequest createRequest = CreateGameRequest.builder()
                .playerId(playerId)
                .playerName("WebSocketTester")
                .vsNpc(true)
                .npcDifficulty(Difficulty.EASY)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        GameStateResponse createdGame = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                GameStateResponse.class
        );
        UUID gameId = createdGame.getGameId();

        // 2. Connect to WebSocket
        BlockingQueue<GameUpdateNotification> messageQueue = new LinkedBlockingQueue<>();
        CountDownLatch subscriptionLatch = new CountDownLatch(1);
        
        String wsUrl = "ws://localhost:" + port + "/ws";
        
        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                session.subscribe("/topic/game/" + gameId, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return GameUpdateNotification.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        if (payload instanceof GameUpdateNotification notification) {
                            messageQueue.offer(notification);
                        }
                    }
                });
                subscriptionLatch.countDown();
            }

            @Override
            public void handleException(StompSession session, StompCommand command, 
                                        StompHeaders headers, byte[] payload, Throwable exception) {
                System.err.println("STOMP Exception: " + exception.getMessage());
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                System.err.println("Transport Error: " + exception.getMessage());
            }
        };

        StompSession session = stompClient.connectAsync(wsUrl, new WebSocketHttpHeaders(), sessionHandler)
                .get(10, TimeUnit.SECONDS);

        boolean subscribed = subscriptionLatch.await(5, TimeUnit.SECONDS);
        assertThat(subscribed).as("Should subscribe to game topic").isTrue();
        
        Thread.sleep(500);

        // 3. Execute an action via REST API
        PlayerActionRequest rollRequest = PlayerActionRequest.builder()
                .playerId(playerId)
                .actionType(PlayerActionRequest.ActionType.ROLL_DICE)
                .build();

        mockMvc.perform(post("/api/v1/games/{gameId}/actions", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rollRequest)))
                .andExpect(status().isOk());

        // 4. Verify notification received
        GameUpdateNotification notification = messageQueue.poll(10, TimeUnit.SECONDS);
        
        assertThat(notification)
                .as("Should receive WebSocket notification after action")
                .isNotNull();
        assertThat(notification.getGameId()).isEqualTo(gameId);
        assertThat(notification.getUpdateType()).isEqualTo(GameUpdateMessage.UpdateType.DICE_ROLLED);
        assertThat(notification.getDiceRoll()).isNotNull();
        assertThat(notification.getCurrentPlayerId()).isNotNull();

        session.disconnect();
    }

    @Test
    void shouldConnectToWebSocket() throws Exception {
        CountDownLatch connectionLatch = new CountDownLatch(1);

        String wsUrl = "ws://localhost:" + port + "/ws";

        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                connectionLatch.countDown();
            }
        };

        StompSession session = stompClient.connectAsync(wsUrl, new WebSocketHttpHeaders(), sessionHandler)
                .get(10, TimeUnit.SECONDS);

        assertThat(connectionLatch.await(5, TimeUnit.SECONDS))
                .as("Should connect to WebSocket")
                .isTrue();
        assertThat(session.isConnected()).isTrue();

        session.disconnect();
    }
}

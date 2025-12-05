package com.fore.game.application.npc;

import com.fore.game.application.dto.ActionResultResponse;
import com.fore.game.application.dto.PlayerActionRequest;
import com.fore.game.application.usecases.ExecuteActionUseCase;
import com.fore.game.application.usecases.GetGameUseCase;
import com.fore.game.domain.model.GameSession;
import com.fore.game.domain.model.enums.GameStatus;
import com.fore.game.domain.model.enums.TurnPhase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.UUID;

/**
 * Orchestrates NPC turns by:
 * 1. Detecting when it's NPC's turn
 * 2. Building game context
 * 3. Asking NpcDecisionEngine for action
 * 4. Executing action via use case
 * 5. Repeating until turn ends
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NpcTurnService {

    private final NpcDecisionEngine decisionEngine;
    private final ExecuteActionUseCase executeActionUseCase;
    private final GetGameUseCase getGameUseCase;

    private static final int MAX_ACTIONS_PER_TURN = 20; // Safety limit

    /**
     * Execute the NPC's turn if it's their turn.
     * Returns when the turn is complete or it's no longer NPC's turn.
     */
    public void executeNpcTurnIfNeeded(UUID gameId) {
        log.debug("Checking if NPC turn needed for game {}", gameId);

        int actionCount = 0;
        
        while (actionCount < MAX_ACTIONS_PER_TURN) {
            // Fetch fresh game state
            var gameState = getGameUseCase.getById(gameId);
            
            // Check if game is still in progress
            if (!"IN_PROGRESS".equals(gameState.getStatus())) {
                log.debug("Game {} is not in progress, stopping NPC turn", gameId);
                return;
            }

            // Find NPC player
            var npcPlayer = gameState.getPlayers().stream()
                    .filter(p -> p.isNpc())
                    .findFirst()
                    .orElse(null);

            if (npcPlayer == null) {
                log.debug("No NPC player in game {}", gameId);
                return;
            }

            // Check if it's NPC's turn
            if (!npcPlayer.getPlayerId().equals(gameState.getCurrentPlayerId())) {
                log.debug("Not NPC's turn in game {}", gameId);
                return;
            }

            // Build context and decide action
            GameContext context = buildContext(gameId, npcPlayer.getPlayerId());
            NpcAction action = decisionEngine.decideAction(context);

            log.info("NPC {} decides: {} (reason: {})", 
                    npcPlayer.getDisplayName(), 
                    action.getActionType(), 
                    action.getReasoning());

            // Execute action
            PlayerActionRequest request = mapToRequest(npcPlayer.getPlayerId(), action);
            ActionResultResponse result = executeActionUseCase.execute(gameId, request);

            actionCount++;

            // If turn ended, we're done
            if (action.getActionType() == NpcAction.ActionType.END_TURN) {
                log.info("NPC {} ended turn after {} actions", npcPlayer.getDisplayName(), actionCount);
                return;
            }

            // Small delay to avoid overwhelming the system
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        log.warn("NPC turn exceeded max actions ({}), forcing end turn", MAX_ACTIONS_PER_TURN);
    }

    private GameContext buildContext(UUID gameId, UUID npcPlayerId) {
        // We need to load the full domain object for context building
        // This is a bit of a workaround - ideally we'd have a read model
        var dto = getGameUseCase.getById(gameId);
        
        // Build a simplified context from DTO
        // In a real implementation, you might load the domain object directly
        return buildContextFromDto(dto, npcPlayerId);
    }

    private GameContext buildContextFromDto(
            com.fore.game.application.dto.GameStateResponse dto, 
            UUID npcPlayerId) {
        
        var npc = dto.getPlayers().stream()
                .filter(p -> p.getPlayerId().equals(npcPlayerId))
                .findFirst()
                .orElseThrow();

        var opponent = dto.getPlayers().stream()
                .filter(p -> !p.getPlayerId().equals(npcPlayerId))
                .findFirst()
                .orElseThrow();

        // Map properties
        var allProperties = dto.getBoard().getTiles().stream()
                .filter(t -> t.getProperty() != null)
                .map(t -> {
                    var p = t.getProperty();
                    return GameContext.PropertyInfo.builder()
                            .propertyId(p.getPropertyId())
                            .name(p.getName())
                            .courseGroup(p.getCourseGroup())
                            .position(t.getPosition())
                            .purchasePrice(com.fore.common.types.Money.ofCents(p.getPurchasePriceCents()))
                            .baseRent(com.fore.common.types.Money.ofCents(p.getBaseRentCents()))
                            .currentRent(com.fore.common.types.Money.ofCents(p.getCurrentRentCents()))
                            .improvementCost(com.fore.common.types.Money.ofCents(p.getImprovementCostCents()))
                            .ownerId(p.getOwnerId())
                            .improvementLevel(p.getImprovementLevel())
                            .mortgaged(p.isMortgaged())
                            .canBePurchased(p.isCanBePurchased())
                            .canBeImproved(p.isCanBeImproved())
                            .build();
                })
                .toList();

        // Find current tile property
        var currentTileProperty = allProperties.stream()
                .filter(p -> p.getPosition() == npc.getPosition())
                .findFirst()
                .orElse(null);

        return GameContext.builder()
                .gameId(dto.getGameId())
                .turnPhase(TurnPhase.valueOf(dto.getTurnPhase()))
                .turnNumber(dto.getTurnNumber())
                .npcPlayerId(npcPlayerId)
                .npcName(npc.getDisplayName())
                .difficulty(npc.getNpcDifficulty() != null 
                        ? com.fore.game.domain.model.enums.Difficulty.valueOf(npc.getNpcDifficulty())
                        : com.fore.game.domain.model.enums.Difficulty.MEDIUM)
                .npcPosition(npc.getPosition())
                .npcCurrency(com.fore.common.types.Money.ofCents(npc.getCurrencyCents()))
                .npcOwnedPropertyIds(new HashSet<>(npc.getOwnedPropertyIds()))
                .npcInSandTrap(npc.isInSandTrap())
                .opponentPlayerId(opponent.getPlayerId())
                .opponentName(opponent.getDisplayName())
                .opponentPosition(opponent.getPosition())
                .opponentCurrency(com.fore.common.types.Money.ofCents(opponent.getCurrencyCents()))
                .opponentOwnedPropertyIds(new HashSet<>(opponent.getOwnedPropertyIds()))
                .allProperties(allProperties)
                .currentTileProperty(currentTileProperty)
                .pendingTrade(null) // TODO: Map from DTO if needed
                .npcCompleteGroups(new HashSet<>()) // TODO: Calculate
                .opponentCompleteGroups(new HashSet<>()) // TODO: Calculate
                .build();
    }

    private PlayerActionRequest mapToRequest(UUID playerId, NpcAction action) {
        var builder = PlayerActionRequest.builder()
                .playerId(playerId)
                .actionType(mapActionType(action.getActionType()));

        if (action.getTargetPropertyId() != null) {
            builder.targetPropertyId(action.getTargetPropertyId());
        }

        return builder.build();
    }

    private PlayerActionRequest.ActionType mapActionType(NpcAction.ActionType actionType) {
        return switch (actionType) {
            case ROLL_DICE -> PlayerActionRequest.ActionType.ROLL_DICE;
            case PURCHASE_PROPERTY -> PlayerActionRequest.ActionType.PURCHASE_PROPERTY;
            case IMPROVE_PROPERTY -> PlayerActionRequest.ActionType.IMPROVE_PROPERTY;
            case PROPOSE_TRADE -> PlayerActionRequest.ActionType.PROPOSE_TRADE;
            case ACCEPT_TRADE -> PlayerActionRequest.ActionType.ACCEPT_TRADE;
            case REJECT_TRADE -> PlayerActionRequest.ActionType.REJECT_TRADE;
            case END_TURN -> PlayerActionRequest.ActionType.END_TURN;
        };
    }
}

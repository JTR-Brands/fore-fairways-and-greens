package com.fore.game.application.usecases;

import com.fore.common.types.Money;
import com.fore.game.application.dto.ActionResultResponse;
import com.fore.game.application.dto.GameStateDtoMapper;
import com.fore.game.application.dto.PlayerActionRequest;
import com.fore.game.application.dto.PlayerActionRequest.ActionType;
import com.fore.game.application.ports.outbound.GameEventRepository;
import com.fore.game.application.ports.outbound.GameRepository;
import com.fore.game.domain.events.GameEvent;
import com.fore.game.domain.exceptions.GameNotFoundException;
import com.fore.game.domain.exceptions.InvalidActionException;
import com.fore.game.domain.model.DiceRoll;
import com.fore.game.domain.model.GameSession;
import com.fore.game.domain.model.TradeOffer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecuteActionUseCase {

    private final GameRepository gameRepository;
    private final GameEventRepository eventRepository;
    private final GameStateDtoMapper dtoMapper;

    @Transactional
    public ActionResultResponse execute(UUID gameId, PlayerActionRequest request) {
        log.info("Executing action {} for player {} in game {}", 
                request.getActionType(), request.getPlayerId(), gameId);

        GameSession game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        DiceRoll diceRoll = null;

        try {
            diceRoll = executeAction(game, request);
        } catch (IllegalStateException e) {
            throw new InvalidActionException(e.getMessage());
        }

        // Drain and persist events
        List<GameEvent> events = game.drainEvents();
        
        // Save game state
        GameSession savedGame = gameRepository.save(game);
        
        // Persist events
        eventRepository.appendEvents(savedGame.getGameId(), events);

        log.info("Action {} completed for game {}", request.getActionType(), gameId);

        ActionResultResponse.ActionResultResponseBuilder responseBuilder = ActionResultResponse.builder()
                .success(true)
                .actionType(request.getActionType().name())
                .events(dtoMapper.toEventDtos(events))
                .gameState(dtoMapper.toGameStateResponse(savedGame));

        if (diceRoll != null) {
            responseBuilder.diceRoll(dtoMapper.toDiceRollDto(diceRoll));
        }

        return responseBuilder.build();
    }

    private DiceRoll executeAction(GameSession game, PlayerActionRequest request) {
        UUID playerId = request.getPlayerId();
        ActionType actionType = request.getActionType();

        return switch (actionType) {
            case ROLL_DICE -> game.rollDice(playerId);
            
            case PURCHASE_PROPERTY -> {
                if (request.getTargetPropertyId() == null) {
                    throw new InvalidActionException("Target property ID is required for purchase");
                }
                game.purchaseProperty(playerId, request.getTargetPropertyId());
                yield null;
            }
            
            case IMPROVE_PROPERTY -> {
                if (request.getTargetPropertyId() == null) {
                    throw new InvalidActionException("Target property ID is required for improvement");
                }
                game.improveProperty(playerId, request.getTargetPropertyId());
                yield null;
            }
            
            case PROPOSE_TRADE -> {
                if (request.getTradeOffer() == null) {
                    throw new InvalidActionException("Trade offer details are required");
                }
                game.proposeTrade(playerId, buildTradeOffer(playerId, request.getTradeOffer()));
                yield null;
            }
            
            case ACCEPT_TRADE -> {
                game.respondToTrade(playerId, true);
                yield null;
            }
            
            case REJECT_TRADE -> {
                game.respondToTrade(playerId, false);
                yield null;
            }
            
            case END_TURN -> {
                game.endTurn(playerId);
                yield null;
            }
        };
    }

    private TradeOffer buildTradeOffer(UUID offeringPlayerId, PlayerActionRequest.TradeOfferRequest request) {
        return TradeOffer.builder()
                .offerId(UUID.randomUUID())
                .offeringPlayerId(offeringPlayerId)
                .receivingPlayerId(request.getReceivingPlayerId())
                .offeredPropertyIds(request.getOfferedPropertyIds() != null 
                        ? request.getOfferedPropertyIds() 
                        : new HashSet<>())
                .offeredCurrency(Money.ofCents(request.getOfferedCurrencyCents()))
                .requestedPropertyIds(request.getRequestedPropertyIds() != null 
                        ? request.getRequestedPropertyIds() 
                        : new HashSet<>())
                .requestedCurrency(Money.ofCents(request.getRequestedCurrencyCents()))
                .status(TradeOffer.TradeStatus.PENDING)
                .build();
    }
}

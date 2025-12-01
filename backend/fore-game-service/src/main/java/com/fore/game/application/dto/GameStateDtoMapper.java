package com.fore.game.application.dto;

import com.fore.game.application.dto.ActionResultResponse.DiceRollDto;
import com.fore.game.application.dto.ActionResultResponse.GameEventDto;
import com.fore.game.application.dto.GameStateResponse.*;
import com.fore.game.domain.events.*;
import com.fore.game.domain.model.*;
import com.fore.game.domain.model.enums.CourseGroup;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Maps domain objects to API DTOs.
 */
@Component
public class GameStateDtoMapper {

    public GameStateResponse toGameStateResponse(GameSession game) {
        Map<UUID, String> playerNames = game.getPlayers().values().stream()
                .collect(Collectors.toMap(PlayerState::getPlayerId, PlayerState::getDisplayName));

        return GameStateResponse.builder()
                .gameId(game.getGameId())
                .status(game.getStatus().name())
                .currentPlayerId(game.getCurrentPlayerId())
                .turnPhase(game.getTurnPhase().name())
                .turnNumber(game.getTurnNumber())
                .winnerId(game.getWinnerId())
                .players(mapPlayers(game))
                .board(mapBoard(game, playerNames))
                .pendingTrade(mapTrade(game.getPendingTrade(), playerNames))
                .createdAt(game.getCreatedAt())
                .updatedAt(game.getUpdatedAt())
                .build();
    }

    private List<PlayerStateDto> mapPlayers(GameSession game) {
        return game.getPlayers().values().stream()
                .map(player -> PlayerStateDto.builder()
                        .playerId(player.getPlayerId())
                        .displayName(player.getDisplayName())
                        .npc(player.isNpc())
                        .npcDifficulty(player.getNpcDifficulty() != null 
                                ? player.getNpcDifficulty().name() 
                                : null)
                        .position(player.getPosition())
                        .currencyCents(player.getCurrency().toCents())
                        .ownedPropertyIds(new ArrayList<>(player.getOwnedPropertyIds()))
                        .bankrupt(player.isBankrupt())
                        .inSandTrap(player.isInSandTrap())
                        .turnsInSandTrap(player.getTurnsInSandTrap())
                        .build())
                .collect(Collectors.toList());
    }

    private BoardDto mapBoard(GameSession game, Map<UUID, String> playerNames) {
        UUID currentPlayerId = game.getCurrentPlayerId();
        PlayerState currentPlayer = currentPlayerId != null 
                ? game.getPlayers().get(currentPlayerId) 
                : null;

        List<TileDto> tiles = game.getBoard().getTiles().stream()
                .map(tile -> mapTile(tile, game, playerNames, currentPlayer))
                .collect(Collectors.toList());

        return BoardDto.builder()
                .tiles(tiles)
                .build();
    }

    private TileDto mapTile(Tile tile, GameSession game, Map<UUID, String> playerNames, PlayerState currentPlayer) {
        PropertyDto propertyDto = tile.getProperty()
                .map(prop -> mapProperty(prop, game, playerNames, currentPlayer))
                .orElse(null);

        return TileDto.builder()
                .tileId(tile.getTileId())
                .position(tile.getPosition())
                .type(tile.getType().name())
                .name(tile.getName())
                .property(propertyDto)
                .build();
    }

    private PropertyDto mapProperty(Property property, GameSession game, 
                                    Map<UUID, String> playerNames, PlayerState currentPlayer) {
        
        boolean canBePurchased = !property.isOwned() 
                && currentPlayer != null 
                && currentPlayer.getPosition() == property.getTilePosition()
                && currentPlayer.canAfford(property.getPurchasePrice());

        boolean canBeImproved = property.isOwned()
                && currentPlayer != null
                && property.isOwnedBy(currentPlayer.getPlayerId())
                && property.canBeImproved()
                && game.getBoard().ownsCompleteGroup(currentPlayer.getPlayerId(), property.getCourseGroup())
                && currentPlayer.canAfford(property.getImprovementCost());

        // Calculate current rent
        long currentRentCents = 0;
        if (property.isOwned()) {
            boolean ownerHasGroup = game.getBoard().ownsCompleteGroup(
                    property.getOwnerId(), 
                    property.getCourseGroup()
            );
            currentRentCents = property.calculateRent(ownerHasGroup).toCents();
        }

        return PropertyDto.builder()
                .propertyId(property.getPropertyId())
                .name(property.getName())
                .courseGroup(property.getCourseGroup().name())
                .courseGroupColor(property.getCourseGroup().getHexColor())
                .purchasePriceCents(property.getPurchasePrice().toCents())
                .baseRentCents(property.getBaseRent().toCents())
                .currentRentCents(currentRentCents)
                .improvementCostCents(property.getImprovementCost().toCents())
                .ownerId(property.getOwnerId())
                .ownerName(property.getOwnerId() != null ? playerNames.get(property.getOwnerId()) : null)
                .improvementLevel(property.getImprovementLevel().name())
                .mortgaged(property.isMortgaged())
                .canBePurchased(canBePurchased)
                .canBeImproved(canBeImproved)
                .build();
    }

    private TradeOfferDto mapTrade(TradeOffer trade, Map<UUID, String> playerNames) {
        if (trade == null) return null;

        return TradeOfferDto.builder()
                .offerId(trade.getOfferId())
                .offeringPlayerId(trade.getOfferingPlayerId())
                .offeringPlayerName(playerNames.get(trade.getOfferingPlayerId()))
                .receivingPlayerId(trade.getReceivingPlayerId())
                .receivingPlayerName(playerNames.get(trade.getReceivingPlayerId()))
                .offeredPropertyIds(new ArrayList<>(trade.getOfferedPropertyIds()))
                .offeredCurrencyCents(trade.getOfferedCurrency().toCents())
                .requestedPropertyIds(new ArrayList<>(trade.getRequestedPropertyIds()))
                .requestedCurrencyCents(trade.getRequestedCurrency().toCents())
                .status(trade.getStatus().name())
                .build();
    }

    public DiceRollDto toDiceRollDto(DiceRoll roll) {
        return DiceRollDto.builder()
                .die1(roll.getDie1())
                .die2(roll.getDie2())
                .total(roll.getTotal())
                .doubles(roll.isDoubles())
                .build();
    }

    public List<GameEventDto> toEventDtos(List<GameEvent> events) {
        return events.stream()
                .map(this::toEventDto)
                .collect(Collectors.toList());
    }

    private GameEventDto toEventDto(GameEvent event) {
        return GameEventDto.builder()
                .eventType(event.getEventType())
                .description(describeEvent(event))
                .details(extractEventDetails(event))
                .build();
    }

    private String describeEvent(GameEvent event) {
        return switch (event) {
            case DiceRolledEvent e -> String.format("Rolled %d and %d (total: %d)%s",
                    e.getRoll().getDie1(), e.getRoll().getDie2(), e.getRoll().getTotal(),
                    e.getRoll().isDoubles() ? " - Doubles!" : "");
            case PlayerMovedEvent e -> String.format("Moved from tile %d to tile %d%s",
                    e.getFromPosition(), e.getToPosition(),
                    e.isPassedStart() ? " - Passed Start!" : "");
            case PropertyPurchasedEvent e -> String.format("Purchased %s for %s",
                    e.getPropertyName(), e.getPrice());
            case PropertyImprovedEvent e -> String.format("Improved %s to %s",
                    e.getPropertyName(), e.getNewLevel().getDisplayName());
            case RentPaidEvent e -> String.format("Paid %s rent", e.getAmount());
            case SalaryCollectedEvent e -> String.format("Collected %s salary", e.getAmount());
            case PlayerBankruptEvent e -> "Declared bankruptcy";
            case GameEndedEvent e -> String.format("Game ended: %s", e.getReason());
            case TurnStartedEvent e -> String.format("Turn %d started", e.getTurnNumber());
            case TurnEndedEvent e -> "Turn ended";
            default -> event.getEventType();
        };
    }

    private Object extractEventDetails(GameEvent event) {
        // Return relevant details as a map for frontend consumption
        return switch (event) {
            case DiceRolledEvent e -> Map.of(
                    "die1", e.getRoll().getDie1(),
                    "die2", e.getRoll().getDie2(),
                    "total", e.getRoll().getTotal(),
                    "doubles", e.getRoll().isDoubles()
            );
            case PlayerMovedEvent e -> Map.of(
                    "from", e.getFromPosition(),
                    "to", e.getToPosition(),
                    "passedStart", e.isPassedStart()
            );
            case PropertyPurchasedEvent e -> Map.of(
                    "propertyId", e.getPropertyId(),
                    "propertyName", e.getPropertyName(),
                    "price", e.getPrice().toCents()
            );
            case RentPaidEvent e -> Map.of(
                    "amount", e.getAmount().toCents(),
                    "receiverId", e.getReceiverId()
            );
            default -> null;
        };
    }

    public AvailableGamesResponse.GameSummary toGameSummary(GameSession game) {
        String creatorName = game.getPlayers().values().stream()
                .filter(p -> !p.isNpc())
                .findFirst()
                .map(PlayerState::getDisplayName)
                .orElse("Unknown");

        return AvailableGamesResponse.GameSummary.builder()
                .gameId(game.getGameId())
                .status(game.getStatus().name())
                .creatorName(creatorName)
                .playerCount(game.getPlayers().size())
                .maxPlayers(2)
                .createdAt(game.getCreatedAt())
                .build();
    }
}

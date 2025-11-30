package com.fore.game.infrastructure.persistence.mapper;

import com.fore.common.types.Money;
import com.fore.game.domain.model.*;
import com.fore.game.domain.model.enums.*;
import com.fore.game.infrastructure.persistence.entity.*;
import com.fore.game.infrastructure.persistence.entity.GameStateSnapshot.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Maps between domain GameSession and JPA GameSessionEntity.
 */
@Component
public class GameSessionMapper {

    /**
     * Convert domain model to JPA entity for persistence.
     */
    public GameSessionEntity toEntity(GameSession domain) {
        GameSessionEntity entity = GameSessionEntity.builder()
                .gameId(domain.getGameId())
                .status(domain.getStatus().name())
                .currentPlayerId(domain.getCurrentPlayerId())
                .turnPhase(domain.getTurnPhase().name())
                .turnNumber(domain.getTurnNumber())
                .winnerId(domain.getWinnerId())
                .gameStateSnapshot(buildSnapshot(domain))
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .lastActivityAt(domain.getLastActivityAt())
                .build();

        // Add participants
        domain.getPlayers().values().forEach(player -> {
            GameParticipantEntity participant = GameParticipantEntity.builder()
                    .gameId(domain.getGameId())
                    .playerId(player.getPlayerId())
                    .npc(player.isNpc())
                    .npcDifficulty(player.getNpcDifficulty() != null ? player.getNpcDifficulty().name() : null)
                    .joinedAt(domain.getCreatedAt())
                    .build();
            entity.addParticipant(participant);
        });

        return entity;
    }

    /**
     * Convert JPA entity back to domain model.
     */
    public GameSession toDomain(GameSessionEntity entity) {
        GameStateSnapshot snapshot = entity.getGameStateSnapshot();

        // Reconstruct board with property state from snapshot
        Board board = reconstructBoard(snapshot.getTiles());

        // Reconstruct player states
        Map<UUID, PlayerState> players = reconstructPlayers(snapshot.getPlayers());

        return GameSession.reconstitute(
                entity.getGameId(),
                GameStatus.valueOf(entity.getStatus()),
                entity.getCurrentPlayerId(),
                TurnPhase.valueOf(entity.getTurnPhase()),
                entity.getTurnNumber(),
                entity.getWinnerId(),
                board,
                players,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private GameStateSnapshot buildSnapshot(GameSession domain) {
        return GameStateSnapshot.builder()
                .players(buildPlayerSnapshots(domain))
                .tiles(buildTileSnapshots(domain.getBoard()))
                .pendingTrade(buildTradeSnapshot(domain.getPendingTrade()))
                .build();
    }

    private List<PlayerSnapshot> buildPlayerSnapshots(GameSession domain) {
        return domain.getPlayers().values().stream()
                .map(player -> PlayerSnapshot.builder()
                        .playerId(player.getPlayerId())
                        .displayName(player.getDisplayName())
                        .npc(player.isNpc())
                        .npcDifficulty(player.getNpcDifficulty() != null ? player.getNpcDifficulty().name() : null)
                        .position(player.getPosition())
                        .currencyCents(player.getCurrency().toCents())
                        .ownedPropertyIds(new ArrayList<>(player.getOwnedPropertyIds()))
                        .bankrupt(player.isBankrupt())
                        .turnsInSandTrap(player.getTurnsInSandTrap())
                        .consecutiveDoubles(player.getConsecutiveDoubles())
                        .build())
                .collect(Collectors.toList());
    }

    private List<TileSnapshot> buildTileSnapshots(Board board) {
        return board.getTiles().stream()
                .map(tile -> TileSnapshot.builder()
                        .tileId(tile.getTileId())
                        .position(tile.getPosition())
                        .type(tile.getType().name())
                        .name(tile.getName())
                        .property(tile.getProperty().map(this::buildPropertySnapshot).orElse(null))
                        .build())
                .collect(Collectors.toList());
    }

    private PropertySnapshot buildPropertySnapshot(Property property) {
        return PropertySnapshot.builder()
                .propertyId(property.getPropertyId())
                .name(property.getName())
                .courseGroup(property.getCourseGroup().name())
                .tilePosition(property.getTilePosition())
                .purchasePriceCents(property.getPurchasePrice().toCents())
                .baseRentCents(property.getBaseRent().toCents())
                .rentWithClubhouseCents(property.getRentWithClubhouse().toCents())
                .rentWithResortCents(property.getRentWithResort().toCents())
                .improvementCostCents(property.getImprovementCost().toCents())
                .ownerId(property.getOwnerId())
                .improvementLevel(property.getImprovementLevel().name())
                .mortgaged(property.isMortgaged())
                .build();
    }

    private TradeOfferSnapshot buildTradeSnapshot(TradeOffer trade) {
        if (trade == null) return null;
        return TradeOfferSnapshot.builder()
                .offerId(trade.getOfferId())
                .offeringPlayerId(trade.getOfferingPlayerId())
                .receivingPlayerId(trade.getReceivingPlayerId())
                .offeredPropertyIds(new ArrayList<>(trade.getOfferedPropertyIds()))
                .offeredCurrencyCents(trade.getOfferedCurrency().toCents())
                .requestedPropertyIds(new ArrayList<>(trade.getRequestedPropertyIds()))
                .requestedCurrencyCents(trade.getRequestedCurrency().toCents())
                .status(trade.getStatus().name())
                .build();
    }

    private Board reconstructBoard(List<TileSnapshot> tileSnapshots) {
        List<Tile> tiles = tileSnapshots.stream()
                .map(this::reconstructTile)
                .sorted(Comparator.comparingInt(Tile::getPosition))
                .collect(Collectors.toList());
        return new Board(tiles);
    }

    private Tile reconstructTile(TileSnapshot snapshot) {
        Property property = null;
        if (snapshot.getProperty() != null) {
            property = reconstructProperty(snapshot.getProperty());
        }

        return Tile.builder()
                .tileId(snapshot.getTileId())
                .position(snapshot.getPosition())
                .type(TileType.valueOf(snapshot.getType()))
                .name(snapshot.getName())
                .property(property)
                .build();
    }

    private Property reconstructProperty(PropertySnapshot snapshot) {
        Property property = Property.builder()
                .propertyId(snapshot.getPropertyId())
                .name(snapshot.getName())
                .courseGroup(CourseGroup.valueOf(snapshot.getCourseGroup()))
                .tilePosition(snapshot.getTilePosition())
                .purchasePrice(Money.ofCents(snapshot.getPurchasePriceCents()))
                .baseRent(Money.ofCents(snapshot.getBaseRentCents()))
                .rentWithClubhouse(Money.ofCents(snapshot.getRentWithClubhouseCents()))
                .rentWithResort(Money.ofCents(snapshot.getRentWithResortCents()))
                .improvementCost(Money.ofCents(snapshot.getImprovementCostCents()))
                .build();

        // Apply mutable state
        if (snapshot.getOwnerId() != null) {
            property.purchase(snapshot.getOwnerId());
        }
        
        // Apply improvements
        ImprovementLevel targetLevel = ImprovementLevel.valueOf(snapshot.getImprovementLevel());
        while (property.getImprovementLevel() != targetLevel) {
            property.improve();
        }
        
        // Apply mortgage
        if (snapshot.isMortgaged()) {
            property.mortgage();
        }

        return property;
    }

    private Map<UUID, PlayerState> reconstructPlayers(List<PlayerSnapshot> playerSnapshots) {
        Map<UUID, PlayerState> players = new LinkedHashMap<>();
        
        for (PlayerSnapshot snapshot : playerSnapshots) {
            // Build with zero starting currency, then set the actual amount
            PlayerState player = PlayerState.builder()
                    .playerId(snapshot.getPlayerId())
                    .displayName(snapshot.getDisplayName())
                    .npc(snapshot.isNpc())
                    .npcDifficulty(snapshot.getNpcDifficulty() != null 
                            ? Difficulty.valueOf(snapshot.getNpcDifficulty()) 
                            : null)
                    .startingCurrency(Money.zero())
                    .build();

            // Set actual currency
            player.setCurrency(Money.ofCents(snapshot.getCurrencyCents()));

            // Restore position
            player.moveTo(snapshot.getPosition());

            // Restore owned properties
            snapshot.getOwnedPropertyIds().forEach(player::addProperty);

            // Restore bankruptcy
            if (snapshot.isBankrupt()) {
                player.declareBankrupt();
            }

            // Restore sand trap state
            player.setTurnsInSandTrap(snapshot.getTurnsInSandTrap());

            // Restore consecutive doubles
            player.setConsecutiveDoubles(snapshot.getConsecutiveDoubles());

            players.put(player.getPlayerId(), player);
        }

        return players;
    }
}

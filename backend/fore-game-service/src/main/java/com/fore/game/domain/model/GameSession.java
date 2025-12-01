package com.fore.game.domain.model;

import com.fore.common.types.Money;
import com.fore.game.domain.events.*;
import com.fore.game.domain.model.enums.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;

/**
 * Aggregate root for a game session.
 * All game mutations must go through this class to ensure invariants.
 */
@Slf4j
@Getter
public class GameSession {

    private final UUID gameId;
    private GameStatus status;
    private UUID currentPlayerId;
    private TurnPhase turnPhase;
    private int turnNumber;
    private UUID winnerId;

    private final Board board;
    private final Map<UUID, PlayerState> players;
    private TradeOffer pendingTrade;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastActivityAt;

    // Event accumulator for domain events
    private final List<GameEvent> pendingEvents = new ArrayList<>();

    /**
     * Private constructor - use factory methods.
     */
    private GameSession(UUID gameId, Board board) {
        this.gameId = gameId;
        this.board = board;
        this.players = new LinkedHashMap<>(); // Preserve insertion order
        this.status = GameStatus.WAITING;
        this.turnPhase = TurnPhase.ROLL;
        this.turnNumber = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.lastActivityAt = Instant.now();
    }

    /**
     * Private constructor for reconstitution.
     */
    private GameSession(
            UUID gameId,
            GameStatus status,
            UUID currentPlayerId,
            TurnPhase turnPhase,
            int turnNumber,
            UUID winnerId,
            Board board,
            Map<UUID, PlayerState> players,
            Instant createdAt,
            Instant updatedAt) {
        this.gameId = gameId;
        this.status = status;
        this.currentPlayerId = currentPlayerId;
        this.turnPhase = turnPhase;
        this.turnNumber = turnNumber;
        this.winnerId = winnerId;
        this.board = board;
        this.players = new LinkedHashMap<>(players);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastActivityAt = updatedAt;
    }

    // ==================== Factory Methods ====================

    public static GameSession create(UUID creatorId, String creatorName, boolean vsNpc, Difficulty npcDifficulty) {
        UUID gameId = UUID.randomUUID();
        Board board = BoardFactory.createStandardBoard();
        GameSession session = new GameSession(gameId, board);

        // Add creator as first player
        PlayerState creator = PlayerState.builder()
                .playerId(creatorId)
                .displayName(creatorName)
                .npc(false)
                .startingCurrency(GameConstants.STARTING_CURRENCY)
                .build();
        session.players.put(creatorId, creator);

        // If vs NPC, add NPC player immediately
        if (vsNpc) {
            UUID npcId = UUID.randomUUID();
            Difficulty difficulty = npcDifficulty != null ? npcDifficulty : Difficulty.MEDIUM;
            PlayerState npc = PlayerState.builder()
                    .playerId(npcId)
                    .displayName(difficulty.getDisplayName())
                    .npc(true)
                    .npcDifficulty(difficulty)
                    .startingCurrency(GameConstants.STARTING_CURRENCY)
                    .build();
            session.players.put(npcId, npc);
            session.startGame(creatorId);
        }

        session.addEvent(GameCreatedEvent.builder()
                .gameId(gameId)
                .creatorId(creatorId)
                .vsNpc(vsNpc)
                .build());

        return session;
    }

    public static GameSession reconstitute(
            UUID gameId,
            GameStatus status,
            UUID currentPlayerId,
            TurnPhase turnPhase,
            int turnNumber,
            UUID winnerId,
            Board board,
            Map<UUID, PlayerState> players,
            Instant createdAt,
            Instant updatedAt) {

        return new GameSession(
                gameId,
                status,
                currentPlayerId,
                turnPhase,
                turnNumber,
                winnerId,
                board,
                players,
                createdAt,
                updatedAt
        );
    }

    // ==================== Commands ====================

    public void joinGame(UUID playerId, String playerName) {
        validateStatus(GameStatus.WAITING);

        if (players.size() >= GameConstants.MAX_PLAYERS) {
            throw new IllegalStateException("Game is full");
        }
        if (players.containsKey(playerId)) {
            throw new IllegalStateException("Player already in game");
        }

        PlayerState player = PlayerState.builder()
                .playerId(playerId)
                .displayName(playerName)
                .npc(false)
                .startingCurrency(GameConstants.STARTING_CURRENCY)
                .build();
        players.put(playerId, player);

        addEvent(PlayerJoinedEvent.builder()
                .gameId(gameId)
                .playerId(playerId)
                .playerName(playerName)
                .build());

        // Auto-start when we have enough players
        if (players.size() == GameConstants.MAX_PLAYERS) {
            UUID firstPlayerId = players.keySet().iterator().next();
            startGame(firstPlayerId);
        }
    }

    public DiceRoll rollDice(UUID playerId) {
        validatePlayerTurn(playerId);
        validatePhase(TurnPhase.ROLL);

        PlayerState player = getPlayer(playerId);

        // Check if stuck in sand trap
        if (player.isInSandTrap()) {
            return handleSandTrapRoll(player);
        }

        DiceRoll roll = DiceRoll.roll();
        processMovement(player, roll);

        return roll;
    }

    public void purchaseProperty(UUID playerId, UUID propertyId) {
        validatePlayerTurn(playerId);
        validatePhase(TurnPhase.ACTION);

        PlayerState player = getPlayer(playerId);
        Property property = board.getProperty(propertyId);

        // Validations
        if (property.isOwned()) {
            throw new IllegalStateException("Property is already owned");
        }
        if (player.getPosition() != property.getTilePosition()) {
            throw new IllegalStateException("Player must be on the property tile to purchase");
        }
        if (!player.canAfford(property.getPurchasePrice())) {
            throw new IllegalStateException("Insufficient funds to purchase property");
        }

        // Execute purchase
        player.subtractCurrency(property.getPurchasePrice());
        property.purchase(playerId);
        player.addProperty(propertyId);

        addEvent(PropertyPurchasedEvent.builder()
                .gameId(gameId)
                .playerId(playerId)
                .propertyId(propertyId)
                .propertyName(property.getName())
                .price(property.getPurchasePrice())
                .build());

        touch();
    }

    public void improveProperty(UUID playerId, UUID propertyId) {
        validatePlayerTurn(playerId);
        validatePhase(TurnPhase.ACTION);

        PlayerState player = getPlayer(playerId);
        Property property = board.getProperty(propertyId);

        // Validations
        if (!property.isOwnedBy(playerId)) {
            throw new IllegalStateException("Player does not own this property");
        }
        if (!property.canBeImproved()) {
            throw new IllegalStateException("Property cannot be improved");
        }
        if (!board.ownsCompleteGroup(playerId, property.getCourseGroup())) {
            throw new IllegalStateException("Must own complete course group to improve");
        }
        if (!player.canAfford(property.getImprovementCost())) {
            throw new IllegalStateException("Insufficient funds for improvement");
        }

        // Execute improvement
        player.subtractCurrency(property.getImprovementCost());
        ImprovementLevel previousLevel = property.getImprovementLevel();
        property.improve();

        addEvent(PropertyImprovedEvent.builder()
                .gameId(gameId)
                .playerId(playerId)
                .propertyId(propertyId)
                .propertyName(property.getName())
                .previousLevel(previousLevel)
                .newLevel(property.getImprovementLevel())
                .cost(property.getImprovementCost())
                .build());

        touch();
    }

    public void proposeTrade(UUID playerId, TradeOffer offer) {
        validatePlayerTurn(playerId);
        validatePhase(TurnPhase.ACTION);

        if (!offer.getOfferingPlayerId().equals(playerId)) {
            throw new IllegalStateException("Trade offer must be from current player");
        }
        if (!players.containsKey(offer.getReceivingPlayerId())) {
            throw new IllegalStateException("Trade recipient not in game");
        }
        if (pendingTrade != null && pendingTrade.isPending()) {
            throw new IllegalStateException("There is already a pending trade");
        }

        // Validate offering player owns offered properties
        PlayerState offeringPlayer = getPlayer(playerId);
        for (UUID propId : offer.getOfferedPropertyIds()) {
            if (!offeringPlayer.ownsProperty(propId)) {
                throw new IllegalStateException("Cannot offer property you don't own");
            }
        }

        // Validate receiving player owns requested properties
        PlayerState receivingPlayer = getPlayer(offer.getReceivingPlayerId());
        for (UUID propId : offer.getRequestedPropertyIds()) {
            if (!receivingPlayer.ownsProperty(propId)) {
                throw new IllegalStateException("Cannot request property opponent doesn't own");
            }
        }

        this.pendingTrade = offer;
        this.turnPhase = TurnPhase.TRADE;

        addEvent(TradeProposedEvent.builder()
                .gameId(gameId)
                .offer(offer)
                .build());

        touch();
    }

    public void respondToTrade(UUID playerId, boolean accept) {
        validatePhase(TurnPhase.TRADE);

        if (pendingTrade == null || !pendingTrade.isPending()) {
            throw new IllegalStateException("No pending trade to respond to");
        }
        if (!pendingTrade.getReceivingPlayerId().equals(playerId)) {
            throw new IllegalStateException("Only the trade recipient can respond");
        }

        if (accept) {
            executeTrade(pendingTrade);
            pendingTrade = pendingTrade.accept();

            addEvent(TradeAcceptedEvent.builder()
                    .gameId(gameId)
                    .offer(pendingTrade)
                    .build());
        } else {
            pendingTrade = pendingTrade.reject();

            addEvent(TradeRejectedEvent.builder()
                    .gameId(gameId)
                    .offer(pendingTrade)
                    .build());
        }

        this.turnPhase = TurnPhase.ACTION;
        touch();
    }

    public void endTurn(UUID playerId) {
        validatePlayerTurn(playerId);
        
        // Can only end turn from ACTION phase
        if (turnPhase != TurnPhase.ACTION) {
            throw new IllegalStateException("Can only end turn from ACTION phase, current phase: " + turnPhase);
        }

        // Cancel any pending trade
        if (pendingTrade != null && pendingTrade.isPending()) {
            pendingTrade = pendingTrade.cancel();
        }

        PlayerState currentPlayer = getPlayer(playerId);
        currentPlayer.resetConsecutiveDoubles();

        addEvent(TurnEndedEvent.builder()
                .gameId(gameId)
                .playerId(playerId)
                .turnNumber(turnNumber)
                .build());

        // Switch to next player
        advanceToNextPlayer();

        // Check for game end
        if (checkGameEnd()) {
            return;
        }

        addEvent(TurnStartedEvent.builder()
                .gameId(gameId)
                .playerId(currentPlayerId)
                .turnNumber(turnNumber)
                .build());

        touch();
    }

    // ==================== Private Helpers ====================

    private void startGame(UUID firstPlayerId) {
        this.status = GameStatus.IN_PROGRESS;
        this.currentPlayerId = firstPlayerId;
        this.turnPhase = TurnPhase.ROLL;
        this.turnNumber = 1;

        addEvent(GameStartedEvent.builder()
                .gameId(gameId)
                .firstPlayerId(firstPlayerId)
                .build());
    }

    private DiceRoll handleSandTrapRoll(PlayerState player) {
        DiceRoll roll = DiceRoll.roll();

        addEvent(DiceRolledEvent.builder()
                .gameId(gameId)
                .playerId(player.getPlayerId())
                .roll(roll)
                .build());

        if (roll.isDoubles()) {
            // Escape with doubles!
            player.escapeSandTrap();
            processMovement(player, roll);
        } else {
            player.decrementSandTrapTurns();
            if (!player.isInSandTrap()) {
                // Served full sentence, can move
                processMovement(player, roll);
            } else {
                // Still stuck, end turn
                this.turnPhase = TurnPhase.ACTION;
            }
        }

        return roll;
    }

    private void processMovement(PlayerState player, DiceRoll roll) {
        addEvent(DiceRolledEvent.builder()
                .gameId(gameId)
                .playerId(player.getPlayerId())
                .roll(roll)
                .build());

        // Track doubles
        if (roll.isDoubles()) {
            player.incrementConsecutiveDoubles();
            if (player.hasRolledThreeDoubles()) {
                // Three doubles = go to sand trap
                sendToSandTrap(player);
                return;
            }
        } else {
            player.resetConsecutiveDoubles();
        }

        int oldPosition = player.getPosition();
        int newPosition = board.calculateNewPosition(oldPosition, roll.getTotal());
        player.moveTo(newPosition);

        // Check if passed start
        boolean passedStart = board.passedStart(oldPosition, newPosition) && newPosition != 0;
        if (passedStart) {
            player.addCurrency(GameConstants.PASSING_SALARY);
            addEvent(SalaryCollectedEvent.builder()
                    .gameId(gameId)
                    .playerId(player.getPlayerId())
                    .amount(GameConstants.PASSING_SALARY)
                    .build());
        }

        addEvent(PlayerMovedEvent.builder()
                .gameId(gameId)
                .playerId(player.getPlayerId())
                .fromPosition(oldPosition)
                .toPosition(newPosition)
                .passedStart(passedStart)
                .build());

        // Handle tile effect
        handleLandedTile(player, board.getTileAt(newPosition));

        // If rolled doubles and not in sand trap, player rolls again
        if (roll.isDoubles() && !player.isInSandTrap() && status == GameStatus.IN_PROGRESS) {
            this.turnPhase = TurnPhase.ROLL;
        }
    }

    private void handleLandedTile(PlayerState player, Tile tile) {
        switch (tile.getType()) {
            case PROPERTY -> handlePropertyTile(player, tile);
            case SAND_TRAP -> sendToSandTrap(player);
            case WATER_HAZARD -> handleWaterHazard(player);
            case PRO_SHOP -> handleProShop(player);
            case CLUBHOUSE_HQ, MEMBERS_LOUNGE -> {
                // Safe tiles - no action, proceed to ACTION phase
                this.turnPhase = TurnPhase.ACTION;
            }
        }
    }

    private void handlePropertyTile(PlayerState player, Tile tile) {
        Property property = tile.getProperty().orElseThrow();

        if (property.isOwned() && !property.isOwnedBy(player.getPlayerId())) {
            // Must pay rent
            Money rent = property.calculateRent(
                    board.ownsCompleteGroup(property.getOwnerId(), property.getCourseGroup())
            );

            if (!property.isMortgaged()) {
                processRentPayment(player, property.getOwnerId(), property, rent);
            }
        }

        this.turnPhase = TurnPhase.ACTION;
    }

    private void processRentPayment(PlayerState payer, UUID receiverId, Property property, Money rent) {
        PlayerState receiver = getPlayer(receiverId);

        if (payer.canAfford(rent)) {
            payer.subtractCurrency(rent);
            receiver.addCurrency(rent);

            addEvent(RentPaidEvent.builder()
                    .gameId(gameId)
                    .payerId(payer.getPlayerId())
                    .receiverId(receiverId)
                    .propertyId(property.getPropertyId())
                    .amount(rent)
                    .build());
        } else {
            // Bankruptcy
            handleBankruptcy(payer, receiver);
        }
    }

    private void handleBankruptcy(PlayerState bankruptPlayer, PlayerState creditor) {
        bankruptPlayer.declareBankrupt();

        // Transfer all assets to creditor
        for (UUID propertyId : new HashSet<>(bankruptPlayer.getOwnedPropertyIds())) {
            Property property = board.getProperty(propertyId);
            property.transferTo(creditor.getPlayerId());
            bankruptPlayer.removeProperty(propertyId);
            creditor.addProperty(propertyId);
        }

        // Transfer remaining currency
        creditor.addCurrency(bankruptPlayer.getCurrency());
        // Set bankrupt player currency to zero
        bankruptPlayer.setCurrency(Money.zero());

        addEvent(PlayerBankruptEvent.builder()
                .gameId(gameId)
                .playerId(bankruptPlayer.getPlayerId())
                .creditorId(creditor.getPlayerId())
                .build());
    }

    private void sendToSandTrap(PlayerState player) {
        int sandTrapPosition = 8; // Position of sand trap
        player.moveTo(sandTrapPosition);
        player.enterSandTrap();
        player.resetConsecutiveDoubles();

        addEvent(PlayerSentToSandTrapEvent.builder()
                .gameId(gameId)
                .playerId(player.getPlayerId())
                .build());

        this.turnPhase = TurnPhase.ACTION;
    }

    private void handleWaterHazard(PlayerState player) {
        if (player.canAfford(GameConstants.WATER_HAZARD_PENALTY)) {
            player.subtractCurrency(GameConstants.WATER_HAZARD_PENALTY);

            addEvent(PenaltyPaidEvent.builder()
                    .gameId(gameId)
                    .playerId(player.getPlayerId())
                    .amount(GameConstants.WATER_HAZARD_PENALTY)
                    .reason("Water Hazard")
                    .build());
        }
        // If can't afford, nothing happens (for MVP simplicity)
        this.turnPhase = TurnPhase.ACTION;
    }

    private void handleProShop(PlayerState player) {
        // TODO: Implement card draw mechanic in future iteration
        // For MVP, pro shop is just a safe tile
        this.turnPhase = TurnPhase.ACTION;
    }

    private void executeTrade(TradeOffer offer) {
        PlayerState offering = getPlayer(offer.getOfferingPlayerId());
        PlayerState receiving = getPlayer(offer.getReceivingPlayerId());

        // Transfer properties from offering to receiving
        for (UUID propId : offer.getOfferedPropertyIds()) {
            Property property = board.getProperty(propId);
            property.transferTo(receiving.getPlayerId());
            offering.removeProperty(propId);
            receiving.addProperty(propId);
        }

        // Transfer properties from receiving to offering
        for (UUID propId : offer.getRequestedPropertyIds()) {
            Property property = board.getProperty(propId);
            property.transferTo(offering.getPlayerId());
            receiving.removeProperty(propId);
            offering.addProperty(propId);
        }

        // Transfer currency
        if (offer.getOfferedCurrency().isPositive()) {
            offering.subtractCurrency(offer.getOfferedCurrency());
            receiving.addCurrency(offer.getOfferedCurrency());
        }
        if (offer.getRequestedCurrency().isPositive()) {
            receiving.subtractCurrency(offer.getRequestedCurrency());
            offering.addCurrency(offer.getRequestedCurrency());
        }
    }

    private void advanceToNextPlayer() {
        List<UUID> playerIds = players.keySet().stream()
                .filter(id -> !players.get(id).isBankrupt())
                .toList();

        int currentIndex = playerIds.indexOf(currentPlayerId);
        int nextIndex = (currentIndex + 1) % playerIds.size();

        this.currentPlayerId = playerIds.get(nextIndex);
        this.turnPhase = TurnPhase.ROLL;
        this.turnNumber++;
    }

    private boolean checkGameEnd() {
        List<PlayerState> activePlayers = players.values().stream()
                .filter(p -> !p.isBankrupt())
                .toList();

        if (activePlayers.size() == 1) {
            // One player left standing
            this.winnerId = activePlayers.get(0).getPlayerId();
            this.status = GameStatus.COMPLETED;

            addEvent(GameEndedEvent.builder()
                    .gameId(gameId)
                    .winnerId(winnerId)
                    .reason("Opponent bankrupt")
                    .build());
            return true;
        }

        return false;
    }

    // ==================== Validation Helpers ====================

    private void validatePlayerTurn(UUID playerId) {
        if (status != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("Game is not in progress");
        }
        if (!currentPlayerId.equals(playerId)) {
            throw new IllegalStateException("It is not your turn");
        }
    }

    private void validatePhase(TurnPhase expectedPhase) {
        if (this.turnPhase != expectedPhase) {
            throw new IllegalStateException(
                    "Invalid turn phase. Expected " + expectedPhase + " but was " + turnPhase);
        }
    }

    private void validateStatus(GameStatus expectedStatus) {
        if (this.status != expectedStatus) {
            throw new IllegalStateException(
                    "Invalid game status. Expected " + expectedStatus + " but was " + status);
        }
    }

    // ==================== Query Methods ====================

    public PlayerState getPlayer(UUID playerId) {
        PlayerState player = players.get(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Player not found: " + playerId);
        }
        return player;
    }

    public PlayerState getCurrentPlayer() {
        if (currentPlayerId == null) {
            return null;
        }
        return getPlayer(currentPlayerId);
    }

    public Optional<PlayerState> getNpcPlayer() {
        return players.values().stream()
                .filter(PlayerState::isNpc)
                .findFirst();
    }

    public PlayerState getOpponent(UUID playerId) {
        return players.values().stream()
                .filter(p -> !p.getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No opponent found"));
    }

    public boolean isPlayerTurn(UUID playerId) {
        return currentPlayerId != null && currentPlayerId.equals(playerId);
    }

    public boolean isCurrentPlayerNpc() {
        PlayerState current = players.get(currentPlayerId);
        return current != null && current.isNpc();
    }

    public List<PlayerState> getActivePlayers() {
        return players.values().stream()
                .filter(p -> !p.isBankrupt())
                .toList();
    }

    // ==================== Event Management ====================

    private void addEvent(GameEvent event) {
        pendingEvents.add(event);
    }

    public List<GameEvent> drainEvents() {
        List<GameEvent> events = new ArrayList<>(pendingEvents);
        pendingEvents.clear();
        return events;
    }

    private void touch() {
        this.updatedAt = Instant.now();
        this.lastActivityAt = Instant.now();
    }
}

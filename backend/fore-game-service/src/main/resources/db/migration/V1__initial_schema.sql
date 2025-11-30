-- FORE: Fairways & Greens - Initial Schema
-- Version: 1
-- Description: Core tables for game state management

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

------------------------------------------------------------
-- PLAYER ACCOUNTS
------------------------------------------------------------
CREATE TABLE players (
    player_id       UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    display_name    VARCHAR(50) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_players_email ON players(email);

------------------------------------------------------------
-- GAME SESSIONS
------------------------------------------------------------
CREATE TABLE game_sessions (
    game_id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    status              VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    current_player_id   UUID,
    turn_phase          VARCHAR(20) NOT NULL DEFAULT 'ROLL',
    turn_number         INT NOT NULL DEFAULT 0,
    winner_id           UUID,
    game_state_snapshot JSONB NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_activity_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_status CHECK (status IN ('WAITING', 'IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT chk_turn_phase CHECK (turn_phase IN ('ROLL', 'MOVE', 'ACTION', 'TRADE', 'END_TURN'))
);

CREATE INDEX idx_game_sessions_status ON game_sessions(status);
CREATE INDEX idx_game_sessions_last_activity ON game_sessions(last_activity_at);

------------------------------------------------------------
-- GAME PARTICIPANTS
------------------------------------------------------------
CREATE TABLE game_participants (
    game_id     UUID NOT NULL REFERENCES game_sessions(game_id) ON DELETE CASCADE,
    player_id   UUID NOT NULL REFERENCES players(player_id),
    is_npc      BOOLEAN NOT NULL DEFAULT FALSE,
    npc_difficulty VARCHAR(20),
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (game_id, player_id),
    CONSTRAINT chk_npc_difficulty CHECK (
        (is_npc = FALSE AND npc_difficulty IS NULL) OR
        (is_npc = TRUE AND npc_difficulty IN ('EASY', 'MEDIUM', 'HARD', 'RUTHLESS'))
    )
);

CREATE INDEX idx_game_participants_player ON game_participants(player_id);

------------------------------------------------------------
-- GAME EVENTS (Append-only log)
------------------------------------------------------------
CREATE TABLE game_events (
    event_id        UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    game_id         UUID NOT NULL REFERENCES game_sessions(game_id) ON DELETE CASCADE,
    sequence_num    BIGINT NOT NULL,
    event_type      VARCHAR(50) NOT NULL,
    actor_id        UUID,
    event_payload   JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_game_event_sequence UNIQUE (game_id, sequence_num)
);

CREATE INDEX idx_game_events_game_id ON game_events(game_id);
CREATE INDEX idx_game_events_game_sequence ON game_events(game_id, sequence_num);
CREATE INDEX idx_game_events_type ON game_events(event_type);

------------------------------------------------------------
-- NPC PROFILES
------------------------------------------------------------
CREATE TABLE npc_profiles (
    profile_id      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_name    VARCHAR(50) NOT NULL UNIQUE,
    difficulty      VARCHAR(20) NOT NULL,
    prompt_template TEXT NOT NULL DEFAULT '',
    config_json     JSONB NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_difficulty CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD', 'RUTHLESS'))
);

-- Seed default NPC profiles
INSERT INTO npc_profiles (profile_name, difficulty, config_json) VALUES
    ('Casual Caddie', 'EASY', '{"aggression": 0.2, "tradeWillingness": 0.8, "mistakeRate": 0.3}'),
    ('Club Pro', 'MEDIUM', '{"aggression": 0.5, "tradeWillingness": 0.5, "mistakeRate": 0.1}'),
    ('Tour Veteran', 'HARD', '{"aggression": 0.7, "tradeWillingness": 0.3, "mistakeRate": 0.0}'),
    ('Championship Mind', 'RUTHLESS', '{"aggression": 0.95, "tradeWillingness": 0.1, "mistakeRate": 0.0}');

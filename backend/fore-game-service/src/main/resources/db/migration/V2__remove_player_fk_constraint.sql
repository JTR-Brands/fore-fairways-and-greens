-- Remove foreign key constraint on game_participants
-- For MVP, we allow game participants without requiring player accounts

ALTER TABLE game_participants 
    DROP CONSTRAINT IF EXISTS game_participants_player_id_fkey;

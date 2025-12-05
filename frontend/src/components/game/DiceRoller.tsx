import React, { useState, useEffect } from 'react';
import type { DiceRoll } from '../../types/game';
import './DiceRoller.css';

interface DiceRollerProps {
  diceRoll: DiceRoll | null;
  canRoll: boolean;
  isRolling?: boolean;
  onRoll: () => void;
}

export const DiceRoller: React.FC<DiceRollerProps> = ({
  diceRoll,
  canRoll,
  isRolling = false,
  onRoll,
}) => {
  const [animating, setAnimating] = useState(false);
  const [displayDice, setDisplayDice] = useState<{ die1: number; die2: number }>({
    die1: 1,
    die2: 1,
  });

  useEffect(() => {
    if (diceRoll) {
      // Animate dice rolling
      setAnimating(true);
      const interval = setInterval(() => {
        setDisplayDice({
          die1: Math.floor(Math.random() * 6) + 1,
          die2: Math.floor(Math.random() * 6) + 1,
        });
      }, 100);

      // Stop animation and show result
      setTimeout(() => {
        clearInterval(interval);
        setDisplayDice({ die1: diceRoll.die1, die2: diceRoll.die2 });
        setAnimating(false);
      }, 800);

      return () => clearInterval(interval);
    }
  }, [diceRoll]);

  const getDieFace = (value: number): string => {
    const faces = ['⚀', '⚁', '⚂', '⚃', '⚄', '⚅'];
    return faces[value - 1] || '⚀';
  };

  return (
    <div className="dice-roller">
      <div className={`dice-container ${animating ? 'rolling' : ''}`}>
        <div className={`die ${diceRoll?.isDoubles ? 'doubles' : ''}`}>
          {getDieFace(displayDice.die1)}
        </div>
        <div className={`die ${diceRoll?.isDoubles ? 'doubles' : ''}`}>
          {getDieFace(displayDice.die2)}
        </div>
      </div>

      {diceRoll && !animating && (
        <div className="dice-result">
          <span className="total">Total: {diceRoll.total}</span>
          {diceRoll.isDoubles && <span className="doubles-badge">DOUBLES! 🎲</span>}
        </div>
      )}

      <button
        className="roll-button"
        onClick={onRoll}
        disabled={!canRoll || isRolling || animating}
      >
        {animating || isRolling ? 'Rolling...' : 'Roll Dice 🎲'}
      </button>
    </div>
  );
};

export default DiceRoller;

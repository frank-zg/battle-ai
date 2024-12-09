package com.battle.ai.battle;


import com.battle.ai.Node;

public interface BattleEventListener {
    void onUnitMoved(Unit unit, Node from, Node to);
    void onUnitAttacked(Unit attacker, Unit target, double damage);
    void onBattleEnd(int winningTeam);
} 
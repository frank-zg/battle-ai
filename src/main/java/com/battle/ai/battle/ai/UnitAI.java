package com.battle.ai.battle.ai;

import com.battle.ai.battle.Unit;

import java.util.List;

public interface UnitAI {
    void makeDecision(Unit unit, List<Unit> allies, List<Unit> enemies);
} 
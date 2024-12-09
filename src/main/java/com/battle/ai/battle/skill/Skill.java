package com.battle.ai.battle.skill;

import com.battle.ai.battle.Unit;

import java.util.List;

public interface Skill {
    void use(Unit caster, Unit target, List<Unit> allUnits);
    String getName();
    void resetCooldown();
    long getCooldownTime();
} 
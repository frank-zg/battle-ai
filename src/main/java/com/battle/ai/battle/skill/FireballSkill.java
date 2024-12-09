package com.battle.ai.battle.skill;

import com.battle.ai.battle.Unit;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class FireballSkill implements Skill {
    private static final double DAMAGE = 30.0;
    private static final double RANGE = 128.0; // 4个格子
    private static final double SPLASH_RADIUS = 64.0; // 2个格子的溅射范围
    private static final long COOLDOWN_TIME = 3000; // 3秒冷却时间
    private boolean isOnCooldown = false;
    
    @Override
    public void use(Unit caster, Unit target, List<Unit> allUnits) {
        if (isOnCooldown || caster.distanceTo(target.getPosition()) > RANGE) {
            return;
        }
        
        // 获取溅射范围内的所有敌方单位
        List<Unit> targetsInRange = allUnits.stream()
            .filter(unit -> unit.getTeam() != caster.getTeam() // 不同队伍
                       && unit.isAlive() // 存活
                       && unit.getPosition().distanceTo(target.getPosition()) <= SPLASH_RADIUS) // 在溅射范围内
            .collect(Collectors.toList());
        
        // 对范围内所有目标造成伤害
        for (Unit splashTarget : targetsInRange) {
            double distance = splashTarget.getPosition().distanceTo(target.getPosition());
            // 根据距离计算衰减伤害
            double damageMultiplier = 1.0 - (distance / SPLASH_RADIUS) * 0.5; // 最远处伤害衰减50%
            double actualDamage = DAMAGE * damageMultiplier;
            
            splashTarget.takeDamage(actualDamage);
            log.debug("[技能溅射] %s 的火球术对 %s 造成 %.1f 伤害 (距离: %.1f, 衰减: %.1f%%)%n",
                caster.getId(), splashTarget.getId(), actualDamage, 
                distance, (1 - damageMultiplier) * 100);
        }
        
        isOnCooldown = true;
        log.debug("[技能] %s 释放火球术，影响 %d 个目标%n",
            caster.getId(), targetsInRange.size());
    }
    
    @Override
    public String getName() {
        return "Fireball";
    }
    
    @Override
    public void resetCooldown() {
        isOnCooldown = false;
    }
    
    @Override
    public long getCooldownTime() {
        return COOLDOWN_TIME;
    }
} 
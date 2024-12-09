package com.battle.ai.battle.stats;

import com.battle.ai.battle.Unit;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class BattleStats {
    private Map<String, UnitStats> unitStats = new HashMap<>();
    private long battleStartTime;
    private long battleEndTime;
    
    public BattleStats() {
        battleStartTime = System.currentTimeMillis();
    }
    
    public void recordDamageDealt(Unit attacker, Unit target, double damage) {
        getOrCreateUnitStats(attacker.getId()).addDamageDealt(damage);
        getOrCreateUnitStats(target.getId()).addDamageTaken(damage);
    }
    
    public void recordKill(Unit killer, Unit victim) {
        getOrCreateUnitStats(killer.getId()).incrementKills();
        getOrCreateUnitStats(victim.getId()).incrementDeaths();
    }
    
    public void recordMovement(Unit unit, double distance) {
        getOrCreateUnitStats(unit.getId()).addDistanceMoved(distance);
    }
    
    public void endBattle() {
        battleEndTime = System.currentTimeMillis();
    }
    
    public void printBattleReport() {
        System.out.println("\n========= 战斗统计 =========");
        log.debug("战斗时长: %.2f秒%n", (battleEndTime - battleStartTime) / 1000.0);
        
        unitStats.forEach((id, stats) -> {
            log.debug("\n单位 %s 统计:%n", id);
            log.debug("- 造成伤害: %.1f%n", stats.getDamageDealt());
            log.debug("- 承受伤害: %.1f%n", stats.getDamageTaken());
            log.debug("- 击杀数: %d%n", stats.getKills());
            log.debug("- 死亡数: %d%n", stats.getDeaths());
            log.debug("- 移动距离: %.1f%n", stats.getDistanceMoved());
        });
    }
    
    private UnitStats getOrCreateUnitStats(String unitId) {
        return unitStats.computeIfAbsent(unitId, k -> new UnitStats());
    }
    
    private static class UnitStats {
        private double damageDealt;
        private double damageTaken;
        private int kills;
        private int deaths;
        private double distanceMoved;
        
        public void addDamageDealt(double damage) { damageDealt += damage; }
        public void addDamageTaken(double damage) { damageTaken += damage; }
        public void incrementKills() { kills++; }
        public void incrementDeaths() { deaths++; }
        public void addDistanceMoved(double distance) { distanceMoved += distance; }
        
        public double getDamageDealt() { return damageDealt; }
        public double getDamageTaken() { return damageTaken; }
        public int getKills() { return kills; }
        public int getDeaths() { return deaths; }
        public double getDistanceMoved() { return distanceMoved; }
    }
} 
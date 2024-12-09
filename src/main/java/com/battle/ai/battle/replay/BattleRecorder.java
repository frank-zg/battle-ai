package com.battle.ai.battle.replay;

import com.battle.ai.Node;
import com.battle.ai.battle.Unit;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class BattleRecorder {
    private List<BattleAction> actions = new ArrayList<>();
    private long startTime;
    
    public BattleRecorder() {
        startTime = System.currentTimeMillis();
    }
    
    public void recordMovement(Unit unit, Node from, Node to) {
        actions.add(new BattleAction(
            System.currentTimeMillis() - startTime,
            ActionType.MOVE,
            unit.getId(),
            String.format("从(%.1f,%.1f)移动到(%.1f,%.1f)", 
                from.getX(), from.getY(), to.getX(), to.getY())
        ));
    }
    
    public void recordAttack(Unit attacker, Unit target, double damage) {
        actions.add(new BattleAction(
            System.currentTimeMillis() - startTime,
            ActionType.ATTACK,
            attacker.getId(),
            String.format("攻击%s造成%.1f伤害", target.getId(), damage)
        ));
    }
    
    public void playback(double speedMultiplier) {
        long lastActionTime = 0;
        for (BattleAction action : actions) {
            try {
                Thread.sleep((long)((action.timestamp - lastActionTime) / speedMultiplier));
                log.debug("[%.2f秒] %s %s: %s%n", 
                    action.timestamp/1000.0,
                    action.type,
                    action.unitId,
                    action.description);
                lastActionTime = action.timestamp;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private static class BattleAction {
        long timestamp;
        ActionType type;
        String unitId;
        String description;
        
        BattleAction(long timestamp, ActionType type, String unitId, String description) {
            this.timestamp = timestamp;
            this.type = type;
            this.unitId = unitId;
            this.description = description;
        }
    }
    
    private enum ActionType {
        MOVE("移动"),
        ATTACK("攻击"),
        SKILL("技能"),
        DEATH("死亡");
        
        private final String description;
        
        ActionType(String description) {
            this.description = description;
        }
        
        @Override
        public String toString() {
            return description;
        }
    }
} 
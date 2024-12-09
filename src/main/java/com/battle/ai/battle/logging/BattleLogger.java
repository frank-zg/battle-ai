package com.battle.ai.battle.logging;

import com.battle.ai.battle.Unit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BattleLogger {
    public static void logUnitAction(Unit unit, String action) {
        log.debug("[日志] %s: %s%n", unit.getId(), action);
    }
    
    public static void logBattleEvent(String event) {
        log.debug("[战斗事件] %s%n", event);
    }
    
    public static void clear() {
        // 清理日志的逻辑
    }
} 
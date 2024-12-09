package com.battle.ai.battle.event;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class BattleEventQueue {
    private static final ConcurrentLinkedQueue<BattleEvent> eventQueue = new ConcurrentLinkedQueue<>();
    
    public static void enqueue(BattleEvent event) {
        eventQueue.offer(event);
    }
    
    public static void processEvents(Consumer<BattleEvent> handler) {
        BattleEvent event;
        while ((event = eventQueue.poll()) != null) {
            handler.accept(event);
        }
    }
    
    public static void clear() {
        eventQueue.clear();
    }
    
    public static class BattleEvent {
        private final EventType type;
        private final Object[] params;
        
        public BattleEvent(EventType type, Object... params) {
            this.type = type;
            this.params = params;
        }
        
        public EventType getType() { return type; }
        public Object[] getParams() { return params; }
    }
    
    public enum EventType {
        UNIT_MOVED,
        UNIT_ATTACKED,
        UNIT_DIED,
        BATTLE_ENDED,
        SKILL_USED
    }
} 
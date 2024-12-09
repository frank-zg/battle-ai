package com.battle.ai.battle.exception;

public class BattleException extends RuntimeException {
    public BattleException(String message) {
        super(message);
    }
    
    public BattleException(String message, Throwable cause) {
        super(message, cause);
    }
} 
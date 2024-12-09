package com.battle.ai.battle.config;

public class BattleConfig {
    private static BattleConfig instance;
    
    private int maxTurns = 100;
    private long updateInterval = 100;
    private int aiUpdateInterval = 5;
    private boolean debugMode = false;
    private double gameSpeed = 1.0;
    
    private BattleConfig() {}
    
    public static BattleConfig getInstance() {
        if (instance == null) {
            instance = new BattleConfig();
        }
        return instance;
    }
    
    // Getters and Setters
    public int getMaxTurns() { return maxTurns; }
    public void setMaxTurns(int maxTurns) { this.maxTurns = maxTurns; }
    
    public long getUpdateInterval() { return updateInterval; }
    public void setUpdateInterval(long updateInterval) { this.updateInterval = updateInterval; }
    
    public int getAiUpdateInterval() { return aiUpdateInterval; }
    public void setAiUpdateInterval(int aiUpdateInterval) { this.aiUpdateInterval = aiUpdateInterval; }
    
    public boolean isDebugMode() { return debugMode; }
    public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }
    
    public double getGameSpeed() { return gameSpeed; }
    public void setGameSpeed(double gameSpeed) { this.gameSpeed = gameSpeed; }
} 
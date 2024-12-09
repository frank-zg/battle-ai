package com.battle.ai.battle.terrain;

public enum TerrainType {
    PLAIN(true),    // 平地
    WATER(false),   // 水域 
    MOUNTAIN(false), // 山地
    WALL(false),    // 墙壁
    NORMAL(true);   // 普通地形
    
    private final boolean walkable;
    
    TerrainType(boolean walkable) {
        this.walkable = walkable;
    }
    
    public boolean isWalkable() {
        return walkable;
    }
} 
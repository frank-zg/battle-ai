package com.battle.ai.battle;

public enum UnitState {
    IDLE(0),       // 空闲
    MOVING(1),     // 移动中
    ATTACKING(2),  // 攻击中
    CASTING(4),    // 施法中
    STUNNED(5),    // 眩晕
    DEAD (3)   ;    // 死亡 ; ;

    private int id;

    UnitState(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
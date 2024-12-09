package com.battle.ai.battle.config;

public class UnitConfig {
    private String type;
    private double maxHp;
    private double attackDamage;
    private double attackRange;
    private double moveSpeed;

    public UnitConfig(String type) {
        this.type = type;
        switch (type.toLowerCase()) {
            case "warrior":
                maxHp = 100;
                attackDamage = 15;
                attackRange = 32; // 一个格子的距离
                moveSpeed = 16;   // 半个格子的距离
                break;
            case "archer":
                maxHp = 80;
                attackDamage = 20;
                attackRange = 160; // 5个格子的距离
                moveSpeed = 24;    // 0.75个格子的距离
                break;
            default:
                throw new IllegalArgumentException("Unknown unit type: " + type);
        }
    }

    public UnitConfig() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(double maxHp) {
        this.maxHp = maxHp;
    }

    public double getAttackDamage() {
        return attackDamage;
    }

    public void setAttackDamage(double attackDamage) {
        this.attackDamage = attackDamage;
    }

    public double getAttackRange() {
        return attackRange;
    }

    public void setAttackRange(double attackRange) {
        this.attackRange = attackRange;
    }

    public double getMoveSpeed() {
        return moveSpeed;
    }

    public void setMoveSpeed(double moveSpeed) {
        this.moveSpeed = moveSpeed;
    }
}
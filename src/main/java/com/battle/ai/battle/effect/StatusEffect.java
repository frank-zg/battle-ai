package com.battle.ai.battle.effect;

import com.battle.ai.battle.Unit;

public abstract class StatusEffect {
    protected final long duration;
    protected long startTime;
    protected Unit target;
    
    public StatusEffect(long duration) {
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
    }
    
    public void apply(Unit target) {
        this.target = target;
        onApply();
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() - startTime >= duration;
    }
    
    public void update() {
        if (!isExpired()) {
            onUpdate();
        }
    }
    
    public void remove() {
        onRemove();
    }
    
    protected abstract void onApply();
    protected abstract void onUpdate();
    protected abstract void onRemove();
}

// 示例状态效果：减速
class SlowEffect extends StatusEffect {
    private final double slowFactor;
    private double originalSpeed;
    
    public SlowEffect(long duration, double slowFactor) {
        super(duration);
        this.slowFactor = slowFactor;
    }
    
    @Override
    protected void onApply() {
        originalSpeed = target.getMoveSpeed();
        target.setMoveSpeed(originalSpeed * (1 - slowFactor));
    }
    
    @Override
    protected void onUpdate() {
        // 持续效果可以在这里实现
    }
    
    @Override
    protected void onRemove() {
        target.setMoveSpeed(originalSpeed);
    }
} 
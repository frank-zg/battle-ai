package com.battle.ai.battle.ai;

import com.battle.ai.battle.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class AIManager {
    private final ExecutorService aiExecutor;
    private final Map<Unit, SimpleAI> unitAIs;
    
    public AIManager(Map<Unit, SimpleAI> unitAIs) {
        this.aiExecutor = Executors.newFixedThreadPool(2);
        this.unitAIs = unitAIs;
    }
    
    public void processAIDecisions(List<Unit> units, List<Unit> allies, List<Unit> enemies) {
        List<Future<?>> futures = new ArrayList<>();
        
        for (Unit unit : units) {
            if (unit.isAlive()) {
                futures.add(aiExecutor.submit(() -> {
                    SimpleAI ai = unitAIs.get(unit);
                    ai.makeDecision(unit, allies, enemies);
                }));
            }
        }
        
        // 等待所有AI决策完成
        for (Future<?> future : futures) {
            try {
                future.get(100, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                // 处理超时
                System.out.println("AI决策超时");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public void shutdown() {
        aiExecutor.shutdown();
        try {
            if (!aiExecutor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                aiExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            aiExecutor.shutdownNow();
        }
    }
} 
package com.test;

import com.battle.ai.Node;
import com.battle.ai.battle.BattleEventListener;
import com.battle.ai.battle.BattleSystem;
import com.battle.ai.battle.Unit;
import com.battle.ai.battle.ai.SimpleAI;
import com.battle.ai.battle.config.UnitConfig;
import com.battle.ai.battle.logging.BattleLogger;
import com.battle.ai.battle.memory.MemoryMonitor;
import com.battle.ai.battle.monitoring.PerformanceMonitor;
import com.battle.ai.battle.monitoring.SpatialMonitor;
import com.battle.ai.battle.replay.BattleRecorder;
import com.battle.ai.battle.skill.FireballSkill;
import com.battle.ai.battle.stats.BattleStats;
import com.battle.ai.battle.terrain.Grid;
import com.battle.ai.battle.terrain.TerrainType;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BattleTest {
    private static final int AI_UPDATE_INTERVAL = 5; // 每5回合才进行AI决策

    @Test
    public void testBattle() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        int gcCount = 0;
        
        // 初始化战斗系统
        Grid grid = Grid.getInstance();
        BattleSystem battleSystem = new BattleSystem(grid);
        
        // 添加一些地形变化
        grid.setTerrain(5, 5, TerrainType.WALL);
        grid.setTerrain(4, 5, TerrainType.WATER);
        
        // 创建多个单位 - 使用像素坐标
        List<Unit> team1Units = new ArrayList<>();
        List<Unit> team2Units = new ArrayList<>();
        
        // 队伍1的单位 (在左侧)
        /*team1Units.add(new Unit("warrior1",
            new Node(Grid.gridToPixel(0), Grid.gridToPixel(0)), // 修改：移除 false/true 参数
            new UnitConfig("warrior")));
        team1Units.add(new Unit("warrior2", 
            new Node(Grid.gridToPixel(0), Grid.gridToPixel(2)), 
            new UnitConfig("warrior")));
        team1Units.add(new Unit("archer1", 
            new Node(Grid.gridToPixel(1), Grid.gridToPixel(1)), 
            new UnitConfig("archer")));
        
        // 队伍2的单位 (在右侧)
        team2Units.add(new Unit("warrior3", 
            new Node(Grid.gridToPixel(9), Grid.gridToPixel(9)), 
            new UnitConfig("warrior")));
        team2Units.add(new Unit("warrior4", 
            new Node(Grid.gridToPixel(9), Grid.gridToPixel(7)), 
            new UnitConfig("warrior")));
        team2Units.add(new Unit("archer2", 
            new Node(Grid.gridToPixel(8), Grid.gridToPixel(8)), 
            new UnitConfig("archer")));*/
        
        // 设置队伍和技能
        for (Unit unit : team1Units) {
            unit.setTeam(1);
            if (unit.getId().toLowerCase().contains("warrior")) {
                unit.addSkill(new FireballSkill());
            }
            battleSystem.addUnit(unit, 1);
            System.out.printf("[初始化] %s 初始位置: (%.2f, %.2f), 攻击范围: %.2f%n", 
                unit.getId(), unit.getPosition().getX(), unit.getPosition().getY(), unit.getAttackRange());
        }
        
        for (Unit unit : team2Units) {
            unit.setTeam(2);
            if (unit.getId().toLowerCase().contains("warrior")) {
                unit.addSkill(new FireballSkill());
            }
            battleSystem.addUnit(unit, 2);
            System.out.printf("[初始化] %s 初始位置: (%.2f, %.2f), 攻击范围: %.2f%n", 
                unit.getId(), unit.getPosition().getX(), unit.getPosition().getY(), unit.getAttackRange());
        }

        // 设置AI
        Map<Unit, SimpleAI> unitAIs = new HashMap<>();
        for (Unit unit : team1Units) {
            unitAIs.put(unit, new SimpleAI());
        }
        for (Unit unit : team2Units) {
            unitAIs.put(unit, new SimpleAI());
        }

        // 添加战斗统计
        BattleStats battleStats = new BattleStats();
        
        // 添加战斗回放记录
        BattleRecorder battleRecorder = new BattleRecorder();
        
        // 修改事件监听器，记录战斗数据
        battleSystem.addListener(new BattleEventListener() {
            @Override
            public void onUnitMoved(Unit unit, Node from, Node to) {
                System.out.printf("[移动] %s: (%d,%d) -> (%d,%d)%n", 
                    unit.getId(), 
                    Grid.pixelToGrid(from.getX()), Grid.pixelToGrid(from.getY()),
                    Grid.pixelToGrid(to.getX()), Grid.pixelToGrid(to.getY()));
                    
                battleRecorder.recordMovement(unit, from, to);
                battleStats.recordMovement(unit, from.distanceTo(to));
                BattleLogger.logUnitAction(unit, String.format("moved from (%.2f,%.2f) to (%.2f,%.2f)", 
                    from.getX(), from.getY(), to.getX(), to.getY()));
            }
            
            @Override
            public void onUnitAttacked(Unit attacker, Unit target, double damage) {
                System.out.printf("[攻击] %s 攻击 %s, 造成 %.1f 伤害, 目标剩余血量: %.1f%n",
                    attacker.getId(), target.getId(), damage, target.getHp());
                    
                battleRecorder.recordAttack(attacker, target, damage);
                battleStats.recordDamageDealt(attacker, target, damage);
                
                if (!target.isAlive()) {
                    battleStats.recordKill(attacker, target);
                }
                
                BattleLogger.logUnitAction(attacker, String.format("attacked %s for %.1f damage", 
                    target.getId(), damage));
            }
            
            @Override
            public void onBattleEnd(int winningTeam) {
                System.out.println("==================");
                System.out.printf("[战斗结束] 队伍 %d 获胜!%n", winningTeam);
                System.out.println("==================");
                
                // 输出战斗统计
                battleStats.endBattle();
                battleStats.printBattleReport();
                
                // 播放战斗回放
                System.out.println("\n========= 战斗回放 =========");
                battleRecorder.playback(2.0); // 2倍速回放
                
                BattleLogger.logBattleEvent("Battle ended. Team " + winningTeam + " won!");
            }
        });
        
        System.out.println("========= 战斗开始 =========");
        
        // 运行战斗
        int maxTurns = 100; // 设置最大回合数
        int currentTurn = 0;
        
        while (battleSystem.isRunning() && currentTurn < maxTurns) {
            PerformanceMonitor.update();
            MemoryMonitor.checkMemory();
            
            long turnStartTime = System.currentTimeMillis(); // 每回合开始时初始化
            
            if (currentTurn % AI_UPDATE_INTERVAL == 0) {
                // AI决策
                for (Unit unit : team1Units) {
                    if (unit.isAlive()) {
                        System.out.printf("[AI决策] 队伍1 %s 当前位置: (%.2f, %.2f)%n", 
                            unit.getId(), unit.getPosition().getX(), unit.getPosition().getY());
                        Unit target = unitAIs.get(unit).findNearestTarget(unit, team2Units);
                        if (target != null) {
                            System.out.printf("[AI决策] %s 的目标是 %s，目标位置: (%.2f, %.2f)%n",
                                unit.getId(), target.getId(), 
                                target.getPosition().getX(), target.getPosition().getY());
                        }
                        unitAIs.get(unit).makeDecision(unit, team1Units, team2Units);
                    }
                }
                for (Unit unit : team2Units) {
                    if (unit.isAlive()) {
                        System.out.printf("[AI决策] 队伍2 %s 当前位置: (%.2f, %.2f)%n", 
                            unit.getId(), unit.getPosition().getX(), unit.getPosition().getY());
                        Unit target = unitAIs.get(unit).findNearestTarget(unit, team1Units);
                        if (target != null) {
                            System.out.printf("[AI决策] %s 的目标是 %s，目标位置: (%.2f, %.2f)%n",
                                unit.getId(), target.getId(), 
                                target.getPosition().getX(), target.getPosition().getY());
                        }
                        unitAIs.get(unit).makeDecision(unit, team2Units, team1Units);
                    }
                }
            }
            
            battleSystem.update();
            
            // 动态调整休眠时间
            long processTime = System.currentTimeMillis() - turnStartTime;
            if (processTime < 100) {
                Thread.sleep(100 - processTime);
            }
            
            currentTurn++;
        }
        
        if (currentTurn >= maxTurns) {
            System.out.println("========= 达到最大回合数限制 =========");
        }
    }

    @Test
    public void testLargeScaleBattle() throws InterruptedException {
        Grid grid = Grid.getInstance();
        BattleSystem battleSystem = new BattleSystem(grid);
        
        // 创建更多单位进行测试
        List<Unit> team1Units = new ArrayList<>();
        List<Unit> team2Units = new ArrayList<>();
        
        // 创建100个单位
        for (int i = 0; i < 5000; i++) {
            // 队伍1的单位 (在左侧)
            Unit unit1 = new Unit("warrior1_" + i,2,
                new Node(Grid.gridToPixel(i % 10), Grid.gridToPixel(i / 10)),
                new UnitConfig("warrior"));
            unit1.setTeam(1);
            if (i % 3 == 0) { // 每三个单位添加一个火球技能
                unit1.addSkill(new FireballSkill());
            }
            team1Units.add(unit1);
            battleSystem.addUnit(unit1, 1);
            
            // 队伍2的单位 (在右侧)
            Unit unit2 = new Unit("warrior2_" + i,1,
                new Node(Grid.gridToPixel(19 - i % 10), Grid.gridToPixel(19 - i / 10)),
                new UnitConfig("warrior"));
            unit2.setTeam(2);
            if (i % 3 == 0) {
                unit2.addSkill(new FireballSkill());
            }
            team2Units.add(unit2);
            battleSystem.addUnit(unit2, 2);
        }
        
        // 设置AI
        Map<Unit, SimpleAI> unitAIs = new HashMap<>();
        for (Unit unit : team1Units) {
            unitAIs.put(unit, new SimpleAI());
        }
        for (Unit unit : team2Units) {
            unitAIs.put(unit, new SimpleAI());
        }
        
        // 添加战斗统计和回放记录
        BattleStats battleStats = new BattleStats();
        BattleRecorder battleRecorder = new BattleRecorder();
        
        // 添加事件监听器
        battleSystem.addListener(new BattleEventListener() {
            @Override
            public void onUnitMoved(Unit unit, Node from, Node to) {
                battleRecorder.recordMovement(unit, from, to);
                battleStats.recordMovement(unit, from.distanceTo(to));
            }
            
            @Override
            public void onUnitAttacked(Unit attacker, Unit target, double damage) {
                battleRecorder.recordAttack(attacker, target, damage);
                battleStats.recordDamageDealt(attacker, target, damage);
                if (!target.isAlive()) {
                    battleStats.recordKill(attacker, target);
                }
            }
            
            @Override
            public void onBattleEnd(int winningTeam) {
                battleStats.endBattle();
                battleStats.printBattleReport();
                System.out.println("\n========= 战斗回放 =========");
                battleRecorder.playback(2.0);
            }
        });
        
        System.out.println("========= 大规模战斗开始 =========");
        
        int maxTurns = 1000; // 增加最大回合数
        int currentTurn = 0;
        
        while (battleSystem.isRunning() && currentTurn < maxTurns) {
            PerformanceMonitor.update();
            MemoryMonitor.checkMemory();

            team1Units = clearDeadUnits(team1Units);
            team2Units = clearDeadUnits(team2Units);
            long startTime = System.currentTimeMillis();

            if (currentTurn % AI_UPDATE_INTERVAL == 0) {
                // 批量更新AI决策
                // 使用并行流批量处理AI决策
                final List<Unit> team1UnitsCopy = new ArrayList<>(team1Units);
                final List<Unit> team2UnitsCopy = new ArrayList<>(team2Units);
                team1UnitsCopy.parallelStream()
                                        .filter(Unit::isAlive)
                                        .forEach(unit -> unitAIs.get(unit).makeDecision(unit, team1UnitsCopy, team2UnitsCopy));
                team2UnitsCopy.parallelStream()
                                        .filter(Unit::isAlive)
                                        .forEach(unit -> unitAIs.get(unit).makeDecision(unit, team2UnitsCopy, team1UnitsCopy));
            }
            System.out.println("AI决策耗时: "+(System.currentTimeMillis()-startTime));
            
            battleSystem.update();
            System.out.println("战斗系统更新耗时: "+(System.currentTimeMillis()-startTime));
            Thread.sleep(25); // 降低更新频率
            currentTurn++;
            
            if (currentTurn % 10 == 0) {
                System.out.printf("[进度] 当前回合: %d/%d%n", currentTurn, maxTurns);
            }
        }
        System.out.println("team1Units size: "+team1Units.size());
        System.out.println("team2Units size: "+team2Units.size());
        System.out.println("========= 大规模战斗结束 =========");
    }


    private List<Unit> clearDeadUnits(List<Unit> units) {
        return units.stream().filter(Unit::isAlive).collect(Collectors.toList());
    }

    @After
    public void cleanup() {
        // 清理资源
        BattleLogger.clear();
        Grid.reset();
        System.gc(); // 建议GC回收
        SpatialMonitor.printStats();
        SpatialMonitor.reset();
    }
}
package com.battle.ai.battle;

import com.battle.ai.Node;
import com.battle.ai.battle.memory.MemoryMonitor;
import com.battle.ai.battle.pool.ObjectPool;
import com.battle.ai.battle.spatial.QuadTree;
import com.battle.ai.battle.spatial.SpatialHashGrid;
import com.battle.ai.battle.terrain.Grid;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class BattleSystem {
    private final Grid grid;
    private final Map<Integer, List<Unit>> teams;
    private final List<BattleEventListener> listeners;
    private boolean running;
    private long lastUpdateTime;
    private static final long UPDATE_INTERVAL = 100; // 100ms更新一次

    private final ObjectPool<Node> nodePool;
    private long updateCount = 0;
    private final SpatialHashGrid spatialGrid;
    private final QuadTree quadTree;
    private static final int WORLD_SIZE = 640; // 20格 * 32像素

    public BattleSystem(Grid grid) {
        this.grid = grid;
        this.teams = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
        this.running = true;
        this.lastUpdateTime = System.currentTimeMillis();
        this.nodePool = new ObjectPool<>(() -> new Node(0, 0), 1000);
        this.spatialGrid = new SpatialHashGrid();
        this.quadTree = new QuadTree(0, new QuadTree.Rectangle(0, 0, WORLD_SIZE, WORLD_SIZE));
    }

    public void addUnit(Unit unit, int team) {
        teams.computeIfAbsent(team, k -> new ArrayList<>()).add(unit);
        unit.setTeam(team);
        spatialGrid.addUnit(unit);
    }

    public void addListener(BattleEventListener listener) {
        listeners.add(listener);
    }

    public boolean isRunning() {
        return running;
    }

    public boolean update() {
        if (!shouldUpdate()) return false;

        long startTime = System.currentTimeMillis();
        
        // 1. 四叉树更新
        long quadTreeStart = System.currentTimeMillis();
        quadTree.clear();
        teams.values().parallelStream()
                .flatMap(List::stream)
                .filter(Unit::isAlive)
                .forEach(quadTree::insert);
        long quadTreeTime = System.currentTimeMillis() - quadTreeStart;

        // 2. 单位更新
        long unitsStart = System.currentTimeMillis();
        updateUnits();
        long unitsTime = System.currentTimeMillis() - unitsStart;

        // 3. 战斗结束检查
        long battleEndStart = System.currentTimeMillis();
        checkBattleEnd();
        long battleEndTime = System.currentTimeMillis() - battleEndStart;

        long totalTime = System.currentTimeMillis() - startTime;
        
        // 每100次更新输出一次性能日志
       /* if (updateCount % 100 == 0) {
            System.out.println("[性能统计] 总耗时: %s ms, 四叉树更新: %s ms, 单位更新: %s ms, 战斗检查: %s ms"+
                    totalTime+","+ quadTreeTime+","+ unitsTime+","+ battleEndTime);
        }*/

        return true;
    }

    private boolean shouldUpdate() {
        updateCount++;
        if (updateCount % 100 == 0) {
            MemoryMonitor.checkMemory();
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL) {
            return false;
        }
        lastUpdateTime = currentTime;
        return true;
    }

    private void updateUnits() {
        long startTime = System.currentTimeMillis();
        
        // 使用分片处理来优化并行处理
        int processors = Runtime.getRuntime().availableProcessors();
        
        teams.values().stream()
                .flatMap(List::stream)
                .filter(Unit::isAlive)
                .collect(Collectors.groupingBy(unit -> 
                        Math.abs(unit.hashCode() % processors)))
                .values()
                .parallelStream()
                .forEach(unitGroup -> {
                    // 预先获取共享资源，减少锁竞争
                    List<Unit> allUnits = teams.values().stream()
                            .flatMap(List::stream)
                            .filter(Unit::isAlive)
                            .collect(Collectors.toList());
                            
                    for (Unit unit : unitGroup) {
                        updateUnitEfficiently(unit, allUnits);
                    }
                });
        
        long endTime = System.currentTimeMillis();
        if (updateCount % 100 == 0) {
            System.out.println("[单位更新性能] 更新 {} 个单位耗时: {}ms"+ 
                    teams.values().stream().mapToInt(List::size).sum()+","+
                    (endTime - startTime));
        }
    }

    private void updateUnitEfficiently(Unit unit, List<Unit> allUnits) {
        // 缓存常用计算结果
        Node oldPos = unit.getPosition();
        boolean needsMovement = unit.getPath() != null && !unit.getPath().isEmpty();
        boolean canAttack = unit.canAttack();
        
        // 只在必要时更新移动
        if (needsMovement) {
            unit.updatePathMovement();
            // 只在位置确实改变时更新空间网格
            if (!oldPos.equals(unit.getPosition())) {
                spatialGrid.updateUnit(unit, oldPos);
                notifyUnitMoved(unit, oldPos, unit.getPosition());
            }
        }
        
        // 只在可以攻击时更新战斗
        if (canAttack) {
            updateUnitCombatEfficiently(unit, allUnits);
        }
        
        unit.updateState();
    }

    private void updateUnitCombatEfficiently(Unit unit, List<Unit> allUnits) {
        double range = unit.getAttackRange();
        QuadTree.Rectangle searchArea = new QuadTree.Rectangle(
                unit.getPosition().getX() - range,
                unit.getPosition().getY() - range,
                range * 2,
                range * 2
        );

        // 使用空间索引快速查找范围内的单位
        List<Unit> nearbyUnits = new ArrayList<>();
        quadTree.retrieve(nearbyUnits, searchArea);

        // 使用快速过滤找到第一个可攻击目标
        nearbyUnits.stream()
                .filter(target -> target.getTeam() != unit.getTeam()
                        && target.isAlive()
                        && unit.isInAttackRange(target))
                .findFirst()
                .ifPresent(target -> {
                    double damage = unit.getAttackDamage();
                    unit.attack(target, allUnits);
                    notifyUnitAttacked(unit, target, damage);
                });
    }

    private void notifyUnitMoved(Unit unit, Node from, Node to) {
        for (BattleEventListener listener : listeners) {
            listener.onUnitMoved(unit, from, to);
        }
    }

    private void notifyUnitAttacked(Unit attacker, Unit target, double damage) {
        for (BattleEventListener listener : listeners) {
            listener.onUnitAttacked(attacker, target, damage);
        }
    }

    private void checkBattleEnd() {
        int aliveTeams = 0;
        int winningTeam = -1;

        for (Map.Entry<Integer, List<Unit>> entry : teams.entrySet()) {
            if (entry.getValue().stream().anyMatch(Unit::isAlive)) {
                aliveTeams++;
                winningTeam = entry.getKey();
            }
        }

        if (aliveTeams == 1) {
            running = false;
            notifyBattleEnd(winningTeam);
        }
    }

    private void notifyBattleEnd(int winningTeam) {
        for (BattleEventListener listener : listeners) {
            listener.onBattleEnd(winningTeam);
        }
    }
} 
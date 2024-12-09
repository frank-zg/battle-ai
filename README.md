# AI 战斗系统

## 简介
本项目实现了一个基于A*算法的AI战斗系统，结合了四叉树和空间哈希网格用于高效的空间管理。系统支持单位的自动寻路、攻击决策和战斗模拟，适用于游戏开发和AI研究。

## 项目结构

### 核心文件
src/  
├── main  
│ └── java/  
│ └── com/  
│ └── battle/  
│ └── ai/  
│ ├── AStar.java // A 寻路算法实现  
│ ├── Node.java // 节点基础类  
│ ├── battle/  
│ │ ├── BattleSystem.java // 战斗系统  
│ │ ├── Unit.java // 单位类  
│ │ ├── ai/  
│ │ │ ├── SimpleAI.java // AI 决策实现  
│ │ │ └── AIManager.java // AI 管理器  
│ │ ├── spatial/  
│ │ │ ├── QuadTree.java // 四叉树实现  
│ │ │ └── SpatialHashGrid.java // 空间哈希网格  
│ │ └── terrain/  
│ │ └── Grid.java // 地图网格系统  
│ └── cache/  
│ └── PathCache.java // 路径缓存  

## 核心功能

### 1. A*寻路算法
- **启发式函数**: 使用欧几里得距离计算节点间的估计代价。
```java
private double heuristic(Node a, Node b) {
    return Math.sqrt(Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getY() - b.getY(), 2));
}
```


- **路径缓存**: 利用`PathCache`缓存常用路径，减少重复计算。
```java
// 检查缓存中是否存在路径
List<Node> cachedPath = PathCache.getPath(start, end);
if (cachedPath != null) {
    return new ArrayList<>(cachedPath);
}
// 缓存新的路径
PathCache.cachePath(start, end, path);
```


- **分段寻路**: 对于长距离路径，自动分段处理以提高效率。
```java
// 如果距离太远，分段寻路
if (start.distanceTo(end) > 320) { // 10个格子以上
    return findSegmentedPath(start, end);
}
```


### 2. 空间管理系统

#### 四叉树实现
```java
// 创建四叉树
QuadTree quadTree = new QuadTree(0, new QuadTree.Rectangle(0, 0, WORLD_SIZE, WORLD_SIZE));
// 插入单位
quadTree.insert(unit);
// 范围查询
List<Unit> unitsInRange = new ArrayList<>();
quadTree.retrieve(unitsInRange, searchArea);
```

#### 空间哈希网格
```java
// 创建空间哈希网格
SpatialHashGrid spatialGrid = new SpatialHashGrid();
// 添加和更新单位
spatialGrid.addUnit(unit);
spatialGrid.updateUnit(unit, oldPosition);
// 获取邻近单位
List<Unit> nearbyUnits = spatialGrid.getNearbyUnits(position, radius);
```


### 3. AI决策系统

#### 简单AI实现
```java
public void makeDecision(Unit unit, List<Unit> allies, List<Unit> enemies) {
// 寻找最近的目标
    Unit target = findNearestTarget(unit, enemies);
    if (target != null) {
        if (unit.isInAttackRange(target) && unit.canAttack()) {
            // 在攻击范围内则攻击
            unit.attack(target, allUnits);
        } else {
        // 否则移动向目标
            moveTowardsTarget(unit, target);
        }
    }
}
```


#### AI管理器
```java
public class AIManager {
    private final ExecutorService aiExecutor;
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
    }
}
```


### 4. 战斗系统
```java
public class BattleSystem {
    public void update() {
        // 更新四叉树
        quadTree.clear();
        teams.values().parallelStream()
        .flatMap(List::stream)
        .filter(Unit::isAlive)
        .forEach(quadTree::insert);
        // 更新单位
        updateUnits();
        // 检查战斗结束
        checkBattleEnd();
    }
}
```

## 使用示例
```java
// 初始化系统
Grid grid = Grid.getInstance();
BattleSystem battleSystem = new BattleSystem(grid);
// 创建单位
Unit unit1 = new Unit("unit1", 1, new Node(0, 0), config);
Unit unit2 = new Unit("unit2", 2, new Node(100, 100), config);
// 添加到战斗系统
battleSystem.addUnit(unit1, 1);
battleSystem.addUnit(unit2, 2);
// 战斗主循环
while (battleSystem.isRunning()) {
    battleSystem.update();
    Thread.sleep(100); // 控制更新频率
}
```


## 总结
本项目通过A*算法实现了高效的寻路系统，结合四叉树和空间哈希网格优化了空间管理，并实现了简单的AI决策和战斗系统。系统具有良好的扩展性和可维护性，适用于需要复杂AI决策和战斗模拟的游戏开发。

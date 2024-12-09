package com.test;

import com.battle.ai.AStar;
import com.battle.ai.Node;
import com.battle.ai.battle.Unit;
import com.battle.ai.battle.config.UnitConfig;
import com.battle.ai.battle.spatial.SpatialHashGrid;
import com.battle.ai.battle.terrain.Grid;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

public class BattlePerformanceTest {
    private static final int MAP_WIDTH = 1000;
    private static final int MAP_HEIGHT = 1000;
    private static final int UNIT_COUNT = 1000;
    private static final double NEARBY_THRESHOLD = 50.0;
    
    @Test
    public void testPathFindingPerformance() {
        // 重置并获取Grid实例
        Grid.reset();
        Grid grid = Grid.getInstance();
        
        // 创建A*寻路器
        AStar astar = new AStar(grid);
        
        // 生成随机单位和目标点
        List<Node> units = createRandomNodes(UNIT_COUNT);
        List<Node> targets = createRandomNodes(UNIT_COUNT);
        
        // 性能测试
        long startTime = System.currentTimeMillis();
        int successPaths = 0;
        int failedPaths = 0;
        
        for (int i = 0; i < UNIT_COUNT; i++) {
            List<Node> path = astar.findPath(units.get(i), targets.get(i));
            if (path != null) {
                successPaths++;
            } else {
                failedPaths++;
            }
        }
        
        long endTime = System.currentTimeMillis();
        System.out.printf("A* 寻路性能测试结果:%n");
        System.out.printf("总单位数: %d%n", UNIT_COUNT);
        System.out.printf("成功寻路: %d%n", successPaths);
        System.out.printf("失败寻路: %d%n", failedPaths);
        System.out.printf("总耗时: %d ms%n", (endTime - startTime));
        System.out.printf("平均每次寻路耗时: %.2f ms%n", (endTime - startTime) / (double)UNIT_COUNT);
    }
    
    @Test
    public void testSpatialHashingPerformance() {
        SpatialHashGrid grid = new SpatialHashGrid();
        List<Unit> units = createUnits(UNIT_COUNT);
        
        // 添加单位到空间哈希网格
        long startTime = System.currentTimeMillis();
        
        for (Unit unit : units) {
            grid.addUnit(unit);
        }
        
        // 测试邻近单位查询
        for (Unit unit : units) {
            List<Unit> nearby = grid.getNearbyUnits(unit.getPosition(), NEARBY_THRESHOLD);
            // 可以添加断言验证结果
        }
        
        long endTime = System.currentTimeMillis();
        System.out.printf("空间哈希性能测试 - %d 个单位耗时: %d ms%n", 
            UNIT_COUNT, (endTime - startTime));
    }
    
    @Test
    public void testParallelPathFindingWithGuaranteedSuccess() {
        Grid.reset();
        Grid grid = Grid.getInstance();
        AStar astar = new AStar(grid);
        
        // 生成有效的起点和终点对
        List<PathPair> pathPairs = createGuaranteedPathPairs(UNIT_COUNT);
        
        long startTime = System.currentTimeMillis();
        
        // 使用并行流进行寻路
        long[] results = pathPairs.parallelStream()
            .mapToLong(pair -> {
                List<Node> path = findPathWithRetry(astar, pair.start, pair.end);
                return path != null ? 1 : 0;
            })
            .toArray();
        
        long successPaths = Arrays.stream(results).sum();
        long failedPaths = UNIT_COUNT - successPaths;
        
        long endTime = System.currentTimeMillis();
        System.out.printf("保证成功的并行A*寻路性能测试结果:%n");
        System.out.printf("总单位数: %d%n", UNIT_COUNT);
        System.out.printf("成功寻路: %d%n", successPaths);
        System.out.printf("失败寻路: %d%n", failedPaths);
        System.out.printf("总耗时: %d ms%n", (endTime - startTime));
        System.out.printf("平均每次寻路耗时: %.2f ms%n", (endTime - startTime) / (double)UNIT_COUNT);
    }
    
    @Test
    public void testBatchPathFinding() {
        Grid.reset();
        Grid grid = Grid.getInstance();
        AStar astar = new AStar(grid);
        
        List<Node> units = createRandomNodes(UNIT_COUNT);
        List<Node> targets = createRandomNodes(UNIT_COUNT);
        
        // 按距离对寻路请求进行分组
        Map<Integer, List<PathRequest>> batchedRequests = new HashMap<>();
        
        for (int i = 0; i < UNIT_COUNT; i++) {
            Node start = units.get(i);
            Node end = targets.get(i);
            int distance = (int)(start.distanceTo(end) / 32); // 转换为格子距离
            batchedRequests.computeIfAbsent(distance, k -> new ArrayList<>())
                .add(new PathRequest(start, end));
        }
        
        long startTime = System.currentTimeMillis();
        int successPaths = 0;
        int failedPaths = 0;
        
        // 按距离批量处理寻路请求
        for (List<PathRequest> batch : batchedRequests.values()) {
            List<List<Node>> paths = batch.parallelStream()
                .map(req -> astar.findPath(req.start, req.end))
                .collect(Collectors.toList());
            
            successPaths += paths.stream().filter(Objects::nonNull).count();
            failedPaths += paths.stream().filter(Objects::isNull).count();
        }
        
        long endTime = System.currentTimeMillis();
        System.out.printf("批量A*寻路性能测试结果:%n");
        System.out.printf("总单位数: %d%n", UNIT_COUNT);
        System.out.printf("成功寻路: %d%n", successPaths);
        System.out.printf("失败寻路: %d%n", failedPaths);
        System.out.printf("总耗时: %d ms%n", (endTime - startTime));
        System.out.printf("平均每次寻路耗时: %.2f ms%n", (endTime - startTime) / (double)UNIT_COUNT);
    }
    
    private List<Node> createRandomNodes(int count) {
        List<Node> nodes = new ArrayList<>();
        Random random = new Random();
        Grid grid = Grid.getInstance();
        
        for (int i = 0; i < count; i++) {
            int gridX, gridY;
            do {
                gridX = random.nextInt(grid.getWidth());
                gridY = random.nextInt(grid.getHeight());
            } while (!grid.isWalkable(gridX, gridY)); // 确保生成的位置是可行走的
            
            int pixelX = Grid.gridToPixel(gridX);
            int pixelY = Grid.gridToPixel(gridY);
            nodes.add(new Node(pixelX, pixelY));
        }
        return nodes;
    }
    
    private List<Unit> createUnits(int count) {
        List<Unit> units = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            Node pos = new Node(
                random.nextInt(MAP_WIDTH),
                random.nextInt(MAP_HEIGHT)
            );
            units.add(new Unit(UUID.randomUUID().toString(),i,pos,new UnitConfig("warrior")));
        }
        return units;
    }
    
    private static class PathRequest {
        final Node start;
        final Node end;
        
        PathRequest(Node start, Node end) {
            this.start = start;
            this.end = end;
        }
    }
    
    private boolean isLineOfSightClear(Node start, Node end) {
        Grid grid = Grid.getInstance();
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        int steps = (int) Math.max(Math.abs(dx), Math.abs(dy));
        
        if (steps == 0) return true;
        
        dx /= steps;
        dy /= steps;
        
        double x = start.getX();
        double y = start.getY();
        
        for (int i = 0; i < steps; i++) {
            int gridX = Grid.pixelToGrid(x);
            int gridY = Grid.pixelToGrid(y);
            
            if (!grid.isWalkable(gridX, gridY)) {
                return false;
            }
            
            x += dx;
            y += dy;
        }
        
        return true;
    }
    
    private List<PathPair> createGuaranteedPathPairs(int count) {
        List<PathPair> pairs = new ArrayList<>();
        Grid grid = Grid.getInstance();
        Random random = new Random();
        
        for (int i = 0; i < count; i++) {
            Node start = null;
            Node end = null;
            boolean validPairFound = false;
            
            while (!validPairFound) {
                // 生成起点
                start = createValidNode(grid, random);
                
                // 在起点周围找到可达的终点
                for (int attempt = 0; attempt < 10; attempt++) {
                    // 在较小范围内生成终点（比如5-10个格子的距离）
                    int distance = random.nextInt(6) + 5; // 5-10格子距离
                    double angle = random.nextDouble() * 2 * Math.PI;
                    
                    int endGridX = Grid.pixelToGrid(start.getX()) + (int)(Math.cos(angle) * distance);
                    int endGridY = Grid.pixelToGrid(start.getY()) + (int)(Math.sin(angle) * distance);
                    
                    // 确保终点在地图范围内且可行走
                    if (isValidGridPosition(grid, endGridX, endGridY)) {
                        end = new Node(Grid.gridToPixel(endGridX), Grid.gridToPixel(endGridY));
                        if (hasValidPath(grid, start, end)) {
                            validPairFound = true;
                            break;
                        }
                    }
                }
            }
            
            pairs.add(new PathPair(start, end));
        }
        return pairs;
    }
    
    private Node createValidNode(Grid grid, Random random) {
        int gridX, gridY;
        do {
            gridX = random.nextInt(grid.getWidth());
            gridY = random.nextInt(grid.getHeight());
        } while (!grid.isWalkable(gridX, gridY));
        
        return new Node(Grid.gridToPixel(gridX), Grid.gridToPixel(gridY));
    }
    
    private boolean isValidGridPosition(Grid grid, int gridX, int gridY) {
        return gridX >= 0 && gridX < grid.getWidth() && 
               gridY >= 0 && gridY < grid.getHeight() && 
               grid.isWalkable(gridX, gridY);
    }
    
    private boolean hasValidPath(Grid grid, Node start, Node end) {
        // 首先检查直线可达性
        if (isLineOfSightClear(start, end)) {
            return true;
        }
        
        // 如果直线不可达，检查周围是否有可能的路径
        int startGridX = Grid.pixelToGrid(start.getX());
        int startGridY = Grid.pixelToGrid(start.getY());
        int endGridX = Grid.pixelToGrid(end.getX());
        int endGridY = Grid.pixelToGrid(end.getY());
        
        // 检查起点和终点周围的格子是否都有可行走的相邻格子
        return hasWalkableNeighbor(grid, startGridX, startGridY) && 
               hasWalkableNeighbor(grid, endGridX, endGridY);
    }
    
    private boolean hasWalkableNeighbor(Grid grid, int gridX, int gridY) {
        int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}};
        for (int[] dir : directions) {
            int newX = gridX + dir[0];
            int newY = gridY + dir[1];
            if (isValidGridPosition(grid, newX, newY)) {
                return true;
            }
        }
        return false;
    }
    
    private List<Node> findPathWithRetry(AStar astar, Node start, Node end) {
        List<Node> path = astar.findPath(start, end);
        if (path != null) {
            return path;
        }
        
        // 如果直接寻路失败，尝试找到一个中间点
        Grid grid = Grid.getInstance();
        int startGridX = Grid.pixelToGrid(start.getX());
        int startGridY = Grid.pixelToGrid(start.getY());
        int endGridX = Grid.pixelToGrid(end.getX());
        int endGridY = Grid.pixelToGrid(end.getY());
        
        // 在起点和终点之间找一个可行走的中间点
        int midGridX = (startGridX + endGridX) / 2;
        int midGridY = (startGridY + endGridY) / 2;
        
        // 在中间点周围寻找可行走的位置
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                int newMidX = midGridX + dx;
                int newMidY = midGridY + dy;
                
                if (isValidGridPosition(grid, newMidX, newMidY)) {
                    Node midPoint = new Node(Grid.gridToPixel(newMidX), Grid.gridToPixel(newMidY));
                    List<Node> firstHalf = astar.findPath(start, midPoint);
                    if (firstHalf != null) {
                        List<Node> secondHalf = astar.findPath(midPoint, end);
                        if (secondHalf != null) {
                            // 合并路径
                            List<Node> completePath = new ArrayList<>(firstHalf);
                            completePath.addAll(secondHalf.subList(1, secondHalf.size()));
                            return completePath;
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    private static class PathPair {
        final Node start;
        final Node end;
        
        PathPair(Node start, Node end) {
            this.start = start;
            this.end = end;
        }
    }
} 
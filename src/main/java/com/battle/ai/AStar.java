package com.battle.ai;

import com.battle.ai.battle.terrain.Grid;
import com.battle.ai.cache.PathCache;

import java.util.*;

public class AStar {
    private final Grid grid;
    
    public AStar(Grid grid) {
        this.grid = grid;
    }
    
    public List<Node> findPath(Node start, Node end) {
        // 先检查缓存
        List<Node> cachedPath = PathCache.getPath(start, end);
        if (cachedPath != null) {
            return new ArrayList<>(cachedPath);
        }
        
        // 转换为网格坐标
        int startGridX = Grid.pixelToGrid(start.getX());
        int startGridY = Grid.pixelToGrid(start.getY());
        int endGridX = Grid.pixelToGrid(end.getX());
        int endGridY = Grid.pixelToGrid(end.getY());
        
        // 检查起点和终点是否可通行
        if (!grid.isWalkable(startGridX, startGridY) || !grid.isWalkable(endGridX, endGridY)) {
            return null;
        }
        
        // 如果距离太远，分段寻路
        if (start.distanceTo(end) > 320) { // 10个格子以上
            return findSegmentedPath(start, end);
        }
        
        List<Node> path = originalFindPath(start, end);
        if (path != null) {
            PathCache.cachePath(start, end, path);
        }
        return path;
    }
    
    private List<Node> findSegmentedPath(Node start, Node end) {
        List<Node> completePath = new ArrayList<>();
        Node current = start;
        Node target = end;
        
        while (current.distanceTo(end) > 160) { // 5个格子的距离
            // 计算中间点
            double ratio = 160.0 / current.distanceTo(end);
            double midX = current.getX() + (end.getX() - current.getX()) * ratio;
            double midY = current.getY() + (end.getY() - current.getY()) * ratio;
            Node midPoint = new Node(midX, midY);
            
            // 寻找到中间点的路径
            List<Node> segment = originalFindPath(current, midPoint);
            if (segment == null) {
                return null; // 如果找不到路径，返回null
            }
            
            completePath.addAll(segment);
            current = midPoint;
        }
        
        // 添加最后一段路径
        List<Node> finalSegment = originalFindPath(current, end);
        if (finalSegment != null) {
            completePath.addAll(finalSegment);
        }
        
        return completePath;
    }
    
    private double heuristic(Node a, Node b) {
        return Math.sqrt(Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getY() - b.getY(), 2));
    }
    
    private List<Node> reconstructPath(Map<Node, Node> cameFrom, Node current) {
        List<Node> path = new ArrayList<>();
        path.add(current);
        
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(0, current);
        }
        
        return path;
    }
    
    private List<Node> originalFindPath(Node start, Node end) {
        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        Set<Node> closedSet = new HashSet<>();
        Map<Node, Node> cameFrom = new HashMap<>();
        Map<Node, Double> gScore = new HashMap<>();
        
        PathNode startNode = new PathNode(start, 0, heuristic(start, end));
        openSet.add(startNode);
        gScore.put(start, 0.0);
        
        while (!openSet.isEmpty()) {
            PathNode current = openSet.poll();
            
            if (current.node.equals(end)) {
                return reconstructPath(cameFrom, current.node);
            }
            
            closedSet.add(current.node);
            
            for (Node neighbor : grid.getNeighbors(current.node)) {
                if (closedSet.contains(neighbor)) {
                    continue;
                }
                
                double tentativeGScore = gScore.get(current.node) + 1;
                
                if (!gScore.containsKey(neighbor) || tentativeGScore < gScore.get(neighbor)) {
                    cameFrom.put(neighbor, current.node);
                    gScore.put(neighbor, tentativeGScore);
                    double fScore = tentativeGScore + heuristic(neighbor, end);
                    openSet.add(new PathNode(neighbor, tentativeGScore, fScore));
                }
            }
        }
        
        return null;
    }
    
    private static class PathNode implements Comparable<PathNode> {
        Node node;
        double gScore;
        double fScore;
        
        PathNode(Node node, double gScore, double fScore) {
            this.node = node;
            this.gScore = gScore;
            this.fScore = fScore;
        }
        
        @Override
        public int compareTo(PathNode other) {
            return Double.compare(this.fScore, other.fScore);
        }
    }
} 